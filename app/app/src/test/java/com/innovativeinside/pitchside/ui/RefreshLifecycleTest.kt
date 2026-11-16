package com.innovativeinside.pitchside.ui

import com.innovativeinside.pitchside.core.AppError
import com.innovativeinside.pitchside.core.UiState
import com.innovativeinside.pitchside.data.model.Match
import com.innovativeinside.pitchside.data.model.Status
import com.innovativeinside.pitchside.data.model.Team
import com.innovativeinside.pitchside.data.repository.FootballRepository
import com.innovativeinside.pitchside.testutil.MainDispatcherRule
import com.innovativeinside.pitchside.ui.vm.LOAD_TIMEOUT_MS
import com.innovativeinside.pitchside.ui.vm.LoadViewModel
import com.innovativeinside.pitchside.ui.vm.MatchesViewModel
import com.innovativeinside.pitchside.ui.vm.findNextMatchDay
import java.time.LocalDate
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

/**
 * The pull-to-refresh indicator must always end: on success, error, empty, timeout, exceptions and when a newer load
 * or a background (socket/poll) refresh comes along. Regression tests for the stuck spinner on the Matches tab (0.5.0).
 */
@OptIn(ExperimentalCoroutinesApi::class)
class RefreshLifecycleTest {
    @get:Rule val main = MainDispatcherRule()

    /** A screen whose next answers are scripted. Each call takes 100 ms unless the script says otherwise. */
    private class ScriptedVm(
        updates: kotlinx.coroutines.flow.Flow<Set<String>> = kotlinx.coroutines.flow.emptyFlow(),
        private val poll: Long? = null,
    ) : LoadViewModel<List<String>>() {
        val script = ArrayDeque<suspend () -> Result<List<String>>>()
        var calls = 0
        init {
            reloadOn(updates, minGapMs = 0) { _, _ -> true }
        }
        override suspend fun fetch(refresh: Boolean): Result<List<String>> {
            calls++
            val step = script.removeFirstOrNull()
            if (step != null) return step()
            delay(100)
            return Result.success(listOf("a"))
        }
        override fun pollIntervalMs(data: List<String>) = poll
        // Exercise the optional whole-load limit too (virtual time in tests).
        override val loadTimeoutMs: Long? = LOAD_TIMEOUT_MS
    }

    private fun loadedVm(updates: kotlinx.coroutines.flow.Flow<Set<String>> = kotlinx.coroutines.flow.emptyFlow()) = ScriptedVm(updates)

    @Test fun firstLoadUsesSkeletonNotIndicator() = runTest(main.dispatcher) {
        val vm = ScriptedVm()
        vm.load()
        assertEquals(UiState.Loading, vm.state.value)
        assertFalse(vm.refreshing.value)
        // Pulling while the first load runs still shows the skeleton, never the indicator.
        vm.refresh()
        assertFalse(vm.refreshing.value)
        advanceUntilIdle()
        assertTrue(vm.state.value is UiState.Success)
        assertFalse(vm.refreshing.value)
    }

    @Test fun refreshEndsOnSuccess() = runTest(main.dispatcher) {
        val vm = loadedVm(); vm.load(); advanceUntilIdle()
        vm.script.addLast { delay(100); Result.success(listOf("a", "b")) }
        vm.refresh()
        assertTrue("indicator shows over existing data", vm.refreshing.value)
        advanceUntilIdle()
        assertFalse(vm.refreshing.value)
        assertEquals(listOf("a", "b"), (vm.state.value as UiState.Success).data)
    }

    @Test fun refreshEndsOnErrorAndKeepsData() = runTest(main.dispatcher) {
        val vm = loadedVm(); vm.load(); advanceUntilIdle()
        vm.script.addLast { delay(100); Result.failure(AppError.Network()) }
        vm.refresh()
        advanceUntilIdle()
        assertFalse(vm.refreshing.value)
        assertTrue(vm.state.value is UiState.Success)
        assertTrue(vm.refreshError.value is AppError.Network)
    }

    @Test fun refreshEndsOnEmpty() = runTest(main.dispatcher) {
        val vm = loadedVm(); vm.load(); advanceUntilIdle()
        vm.script.addLast { delay(100); Result.success(emptyList()) }
        vm.refresh()
        advanceUntilIdle()
        assertFalse(vm.refreshing.value)
        assertEquals(UiState.Empty, vm.state.value)
    }

    @Test fun refreshEndsWhenFetchThrows() = runTest(main.dispatcher) {
        val vm = loadedVm(); vm.load(); advanceUntilIdle()
        vm.script.addLast { delay(10); throw IllegalStateException("boom") }
        vm.refresh()
        advanceUntilIdle()
        assertFalse(vm.refreshing.value)
        assertTrue(vm.refreshError.value != null)
    }

    @Test fun hangingRequestTimesOut() = runTest(main.dispatcher) {
        val vm = loadedVm(); vm.load(); advanceUntilIdle()
        vm.script.addLast { awaitCancellation() }
        vm.refresh()
        advanceTimeBy(LOAD_TIMEOUT_MS - 1); runCurrent()
        assertTrue(vm.refreshing.value)
        advanceTimeBy(2); runCurrent()
        assertFalse("indicator ends at the time limit", vm.refreshing.value)
        assertTrue(vm.refreshError.value is AppError.Timeout)
        // A first load that hangs ends in a Timeout error, not an endless skeleton.
        val fresh = ScriptedVm().apply { script.addLast { awaitCancellation() } }
        fresh.load()
        advanceTimeBy(LOAD_TIMEOUT_MS + 1); runCurrent()
        assertTrue((fresh.state.value as UiState.Error).error is AppError.Timeout)
    }

    @Test fun refreshEndsWhenANewerLoadReplacesIt() = runTest(main.dispatcher) {
        val vm = loadedVm(); vm.load(); advanceUntilIdle()
        vm.script.addLast { awaitCancellation() }
        vm.refresh()
        runCurrent() // the refresh is now in flight, waiting on the network
        assertTrue(vm.refreshing.value)
        // e.g. the user picks another date while the pull runs: the old load is cancelled.
        vm.load()
        assertFalse(vm.refreshing.value)
        advanceUntilIdle()
        assertFalse(vm.refreshing.value)
        assertTrue(vm.state.value is UiState.Success)
    }

    @Test fun liveSocketPushDoesNotStrandTheIndicator() = runTest(main.dispatcher) {
        val updates = MutableSharedFlow<Set<String>>(extraBufferCapacity = 4)
        val vm = loadedVm(updates); vm.load(); advanceUntilIdle()
        val gate = CompletableDeferred<Unit>()
        vm.script.addLast { gate.await(); Result.success(listOf("x")) }
        vm.refresh()
        runCurrent()
        // In 0.5.0 a socket push here cancelled the user's refresh and left the indicator on forever.
        updates.tryEmit(setOf("m1")); runCurrent()
        assertTrue(vm.refreshing.value)
        gate.complete(Unit)
        advanceUntilIdle()
        assertFalse(vm.refreshing.value)
        assertEquals(listOf("x"), (vm.state.value as UiState.Success).data)
    }

    @Test fun backgroundRefreshNeverPublishesOverANewerLoad() = runTest(main.dispatcher) {
        val updates = MutableSharedFlow<Set<String>>(extraBufferCapacity = 4)
        val vm = loadedVm(updates); vm.load(); advanceUntilIdle()
        vm.script.addLast { delay(1_000); Result.success(listOf("stale")) } // silent refresh, slow
        vm.script.addLast { delay(100); Result.success(listOf("new")) } // user load
        updates.tryEmit(setOf("m1")); runCurrent()
        vm.load()
        advanceUntilIdle()
        assertEquals(listOf("new"), (vm.state.value as UiState.Success).data)
        assertFalse(vm.refreshing.value)
    }

    @Test fun hangingNetworkCallEndsTheRefreshThroughTheRepositoryLimit() = runTest(main.dispatcher) {
        val clock = com.innovativeinside.pitchside.testutil.FixedClock()
        val behavior = com.innovativeinside.pitchside.data.mock.MockBehavior().apply { latencyMs = 50 }
        val repo = com.innovativeinside.pitchside.data.repository.DefaultFootballRepository(
            com.innovativeinside.pitchside.data.mock.MockFootballApi(clock, behavior), com.innovativeinside.pitchside.testutil.TestDispatchers(main.dispatcher), clock,
        )
        val vm = MatchesViewModel(repo) { clock.today() }
        advanceTimeBy(1_000); runCurrent()
        assertTrue(vm.state.value is UiState.Success)
        behavior.latencyMs = 10 * 60_000L // the server never answers in time
        vm.refresh()
        runCurrent()
        assertTrue(vm.refreshing.value)
        advanceTimeBy(com.innovativeinside.pitchside.data.repository.CALL_TIMEOUT_MS + 1); runCurrent()
        assertFalse("indicator ends when the call times out", vm.refreshing.value)
        assertTrue(vm.refreshError.value is AppError.Timeout)
        assertTrue("data stays on screen", vm.state.value is UiState.Success)
        androidx.lifecycle.ViewModelStore().apply { put("vm", vm) }.clear()
    }

    // ---------------- Matches: empty day and next day with matches ----------------

    private val t = Team("t1", "Asante", "ASA", 0xFF2E7D32, "177")
    private fun match(id: String, day: LocalDate) = Match(
        id = id, leagueId = "177", leagueName = "Premier League", round = "", venue = "", referee = null,
        kickoffMs = day.toEpochDay() * 86_400_000L, home = t, away = t.copy(id = "t2"), homeScore = null, awayScore = null,
        status = Status.Upcoming, label = "15:00", brief = "",
    )

    /** Only matchesOn is used by MatchesViewModel; everything else fails loudly. */
    private class DaysRepo(private val days: Map<LocalDate, List<Match>>) : FootballRepository {
        override suspend fun matchesOn(date: LocalDate, refresh: Boolean): Result<List<Match>> = Result.success(days[date].orEmpty())
        private fun no(): Nothing = throw UnsupportedOperationException()
        override suspend fun liveMatches(refresh: Boolean) = no()
        override suspend fun matchDetail(id: String, refresh: Boolean) = no()
        override suspend fun leagues(refresh: Boolean) = no()
        override suspend fun leagueDetail(id: String, refresh: Boolean) = no()
        override suspend fun standings(leagueId: String, refresh: Boolean) = no()
        override suspend fun popularTeams(refresh: Boolean) = no()
        override suspend fun teamPage(id: String, refresh: Boolean) = no()
        override suspend fun player(id: String, refresh: Boolean) = no()
        override suspend fun topPlayers(refresh: Boolean) = no()
        override suspend fun news(category: com.innovativeinside.pitchside.data.model.NewsCategory?, cursor: String?) = no()
        override suspend fun headlines(edition: com.innovativeinside.pitchside.data.model.NewsEdition?, cursor: String?, refresh: Boolean) = no()
        override suspend fun article(id: String) = no()
        override suspend fun search(query: String) = no()
        override suspend fun ask(question: String, language: String) = no()
    }
    private fun repoWith(days: Map<LocalDate, List<Match>>): FootballRepository = DaysRepo(days)

    @Test fun emptyDayOffersTheNextDayWithMatches() = runTest(main.dispatcher) {
        val today = LocalDate.of(2026, 11, 15)
        val vm = MatchesViewModel(repoWith(mapOf(today.plusDays(6) to listOf(match("m", today.plusDays(6)))))) { today }
        advanceUntilIdle()
        assertEquals(UiState.Empty, vm.state.value)
        assertFalse(vm.refreshing.value)
        assertEquals(today.plusDays(6), vm.nextMatchDay.value)
        vm.selectDate(today.plusDays(6))
        assertNull(vm.nextMatchDay.value)
        advanceUntilIdle()
        assertTrue(vm.state.value is UiState.Success)
    }

    @Test fun findNextMatchDayStopsAtFirstHitAndSkipsFailures() = runTest {
        val from = LocalDate.of(2026, 11, 15)
        val asked = mutableListOf<LocalDate>()
        val next = findNextMatchDay(from, 7) { d ->
            asked += d
            when (d) {
                from.plusDays(1) -> Result.failure(AppError.Network())
                from.plusDays(3) -> Result.success(listOf(match("m", d)))
                else -> Result.success(emptyList())
            }
        }
        assertEquals(from.plusDays(3), next)
        assertEquals(3, asked.size)
        assertNull(findNextMatchDay(from, 7) { Result.success(emptyList()) })
    }
}
