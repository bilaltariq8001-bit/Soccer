package com.innovativeinside.pitchside.data

import com.innovativeinside.pitchside.core.AppError
import com.innovativeinside.pitchside.data.mock.MockBehavior
import com.innovativeinside.pitchside.data.mock.MockFootballApi
import com.innovativeinside.pitchside.data.model.Status
import com.innovativeinside.pitchside.data.remote.FootballApi
import com.innovativeinside.pitchside.data.repository.DefaultFootballRepository
import com.innovativeinside.pitchside.testutil.FixedClock
import com.innovativeinside.pitchside.testutil.TestDispatchers
import java.lang.reflect.Proxy
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class RepositoryTest {
    private val clock = FixedClock()
    private val behavior = MockBehavior().apply { latencyMs = 0 }
    private val mock = MockFootballApi(clock, behavior)

    /** Wraps the mock and counts calls per method, to test caching. */
    private val calls = mutableMapOf<String, Int>()
    private val counting: FootballApi = Proxy.newProxyInstance(FootballApi::class.java.classLoader, arrayOf(FootballApi::class.java)) { _, m, args ->
        calls[m.name] = (calls[m.name] ?: 0) + 1
        m.invoke(mock, *(args ?: emptyArray()))
    } as FootballApi

    private fun repo(api: FootballApi = mock) = DefaultFootballRepository(api, TestDispatchers(StandardTestDispatcher()), clock)

    @Test fun todayHasLiveUpcomingFinishedAndPostponed() = runTest {
        val r = DefaultFootballRepository(mock, TestDispatchers(StandardTestDispatcher(testScheduler)), clock)
        val list = r.matchesOn(clock.today()).getOrThrow()
        assertEquals(setOf(Status.Live, Status.Upcoming, Status.Finished, Status.Postponed), list.map { it.status }.toSet())
        assertEquals(list.sortedBy { it.kickoffMs }, list)
        assertEquals("74'", list.first { it.id == "m1" }.label)
    }

    @Test fun farDatesAreEmpty() = runTest {
        val r = DefaultFootballRepository(mock, TestDispatchers(StandardTestDispatcher(testScheduler)), clock)
        assertTrue(r.matchesOn(clock.today().plusDays(30)).getOrThrow().isEmpty())
        assertTrue(r.matchesOn(clock.today().minusDays(2)).getOrThrow().all { it.status == Status.Finished })
    }

    @Test fun cacheServesRepeatsUntilRefreshOrExpiry() = runTest {
        val r = DefaultFootballRepository(counting, TestDispatchers(StandardTestDispatcher(testScheduler)), clock)
        r.leagues(); r.leagues()
        assertEquals(1, calls["leagues"])
        r.leagues(refresh = true)
        assertEquals(2, calls["leagues"])
        clock.now += 61_000
        r.leagues()
        assertEquals(3, calls["leagues"])
    }

    @Test fun failuresBecomeTypedResults() = runTest {
        val r = DefaultFootballRepository(mock, TestDispatchers(StandardTestDispatcher(testScheduler)), clock)
        behavior.offline = true
        assertTrue(r.leagues().exceptionOrNull() is AppError.Network)
        behavior.offline = false
        behavior.serverErrors = true
        assertTrue(r.leagues(refresh = true).exceptionOrNull() is AppError.Server)
        behavior.serverErrors = false
        assertTrue(r.player("nope").exceptionOrNull() is AppError.NotFound)
        assertTrue(r.matchDetail("missing").exceptionOrNull() is AppError.NotFound)
    }

    @Test fun matchDetailCombinesEndpoints() = runTest {
        val r = DefaultFootballRepository(mock, TestDispatchers(StandardTestDispatcher(testScheduler)), clock)
        val live = r.matchDetail("m1").getOrThrow()
        assertEquals(5, live.events.size)
        assertTrue(live.stats.isNotEmpty())
        assertNotNull(live.lineups)
        assertTrue(live.lineups!!.home.starters.any { it.scored })
        assertEquals(8, live.table.size)
        val later = r.matchDetail("m4").getOrThrow()
        assertTrue(later.events.isEmpty() && later.stats.isEmpty())
        assertNull("lineups only an hour before kickoff", later.lineups)
        assertTrue("knockout cup has no table", r.matchDetail("m3").getOrThrow().table.isEmpty())
    }

    @Test fun searchNeedsTwoCharactersAndFindsAcrossTypes() = runTest {
        val r = DefaultFootballRepository(counting, TestDispatchers(StandardTestDispatcher(testScheduler)), clock)
        assertTrue(r.search("n").getOrThrow().isEmpty)
        assertNull(calls["search"])
        val res = r.search("north").getOrThrow()
        assertTrue(res.teams.any { it.id == "ng" })
        assertTrue(res.leagues.any { it.id == "pd" }) // Northland
        assertTrue(r.search("Marsh").getOrThrow().players.any { it.id == "ng-9" })
    }

    @Test fun newsPaginates() = runTest {
        val r = DefaultFootballRepository(mock, TestDispatchers(StandardTestDispatcher(testScheduler)), clock)
        val p1 = r.news(null).getOrThrow()
        assertEquals(4, p1.items.size)
        val p2 = r.news(null, p1.nextCursor).getOrThrow()
        assertEquals(4, p2.items.size)
        assertNull(p2.nextCursor)
        assertFalse(p1.items.map { it.id }.intersect(p2.items.map { it.id }.toSet()).isNotEmpty())
    }

    @Test fun askValidatesInput() = runTest {
        val r = DefaultFootballRepository(mock, TestDispatchers(StandardTestDispatcher(testScheduler)), clock)
        assertTrue(r.ask("  ", "en").exceptionOrNull() is AppError.Validation)
        assertTrue(r.ask("x".repeat(301), "en").exceptionOrNull() is AppError.Validation)
        assertTrue(r.ask("Who leads the table?", "en").getOrThrow().contains("Northgate"))
    }

    @Test fun teamAndPlayerPages() = runTest {
        val r = DefaultFootballRepository(mock, TestDispatchers(StandardTestDispatcher(testScheduler)), clock)
        val t = r.teamPage("ng").getOrThrow()
        assertEquals(16, t.detail.squad.size)
        assertTrue(t.fixtures.isNotEmpty())
        val p = r.player("ng-9").getOrThrow()
        assertEquals("Leo Marsh", p.summary.name)
        assertEquals(7, p.goals)
        assertEquals(5, p.recentMatches.size)
    }
}
