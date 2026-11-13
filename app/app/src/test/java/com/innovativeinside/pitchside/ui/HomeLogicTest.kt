package com.innovativeinside.pitchside.ui

import com.innovativeinside.pitchside.data.model.Match
import com.innovativeinside.pitchside.data.model.Status
import com.innovativeinside.pitchside.data.model.Team
import com.innovativeinside.pitchside.data.prefs.PrefsReducer
import com.innovativeinside.pitchside.data.prefs.UserPrefs
import com.innovativeinside.pitchside.ui.components.countdown
import com.innovativeinside.pitchside.ui.components.matchDescription
import com.innovativeinside.pitchside.ui.components.newsDate
import com.innovativeinside.pitchside.ui.vm.MatchFilter
import com.innovativeinside.pitchside.ui.vm.competitionsOf
import com.innovativeinside.pitchside.ui.vm.filterByCompetition
import com.innovativeinside.pitchside.ui.vm.filterMatches
import com.innovativeinside.pitchside.ui.vm.liveProgress
import com.innovativeinside.pitchside.ui.vm.nextKickoff
import com.innovativeinside.pitchside.ui.vm.pickHero
import com.innovativeinside.pitchside.ui.vm.rankLiveMatches
import com.innovativeinside.pitchside.ui.vm.statusCounts
import java.time.ZoneOffset
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

private fun team(id: String) = Team(id, id.uppercase(), id.uppercase(), 0xFF1F4D35, "pd")

private fun m(id: String, home: String, away: String, hs: Int?, aw: Int?, status: Status = Status.Live, ko: Long = 0, league: String = "pd", label: String = "60'") =
    Match(id, league, if (league == "pd") "Premier Division" else "Continental Cup", "R1", "Park", null, ko, team(home), team(away), hs, aw, status, label, "")

/** Hero selection, filters and small formatting helpers behind the Home redesign. */
class HomeLogicTest {
    private val tight = m("tight", "a", "b", 1, 1, ko = 300)
    private val busy = m("busy", "c", "d", 3, 3, ko = 200)
    private val blowout = m("blowout", "e", "f", 4, 0, ko = 100)
    private val followed = m("mine", "g", "h", 0, 3, ko = 400)

    @Test fun heroPrefersFollowedThenClosestThenMostActiveThenEarliest() {
        assertEquals(listOf("mine", "busy", "tight", "blowout"), rankLiveMatches(listOf(tight, busy, blowout, followed), listOf("h")).map { it.id })
        // Nothing followed: closest game first; among equal gaps the one with more goals; then kickoff.
        assertEquals(listOf("busy", "tight", "blowout"), rankLiveMatches(listOf(blowout, tight, busy), emptyList()).map { it.id })
        val sameGap = m("early", "i", "j", 1, 1, ko = 50)
        assertEquals("early", rankLiveMatches(listOf(tight, sameGap), emptyList()).first().id)
    }

    @Test fun promotedMatchWinsWhileLiveAndNonLiveAreIgnored() {
        val live = listOf(tight, busy, blowout)
        assertEquals("blowout", pickHero(live, emptyList(), "blowout")!!.id)
        assertEquals("busy", pickHero(live, emptyList(), "gone")!!.id)
        assertEquals("busy", pickHero(live + m("ft", "x", "y", 9, 0, Status.Finished), emptyList(), "ft")!!.id)
        assertNull(pickHero(emptyList(), listOf("a"), null))
    }

    @Test fun nextKickoffForFollowedTeamsElseAnyUpcoming() {
        val now = 1_000L
        val u1 = m("u1", "a", "b", null, null, Status.Upcoming, ko = 5_000)
        val u2 = m("u2", "c", "h", null, null, Status.Upcoming, ko = 9_000)
        val past = m("past", "h", "z", null, null, Status.Upcoming, ko = 500)
        assertEquals("u2", nextKickoff(listOf(u1, u2, past), listOf("h"), now)!!.id)
        assertEquals("u1", nextKickoff(listOf(u2, u1), emptyList(), now)!!.id)
        assertNull(nextKickoff(listOf(u1), listOf("nobody"), now))
    }

    @Test fun liveProgressFromLabels() {
        assertEquals(0.5f, liveProgress("HT"))
        assertEquals(74 / 90f, liveProgress("74'")!!, 0.001f)
        assertEquals(1f, liveProgress("90+3'"))
        assertNull(liveProgress("Postponed"))
    }

    @Test fun competitionAndStatusFilters() {
        val cup = m("cup", "k", "l", 0, 0, league = "cc")
        val up = m("up", "a", "h", null, null, Status.Upcoming)
        val ppd = m("ppd", "a", "b", null, null, Status.Postponed, label = "Postponed")
        val ft = m("ft", "a", "b", 2, 0, Status.Finished, label = "FT")
        val all = listOf(tight, cup, up, ppd, ft)
        assertEquals(listOf("cup"), filterByCompetition(all, "cc").map { it.id })
        assertEquals(all, filterByCompetition(all, null))
        assertEquals(listOf("cc" to "Continental Cup", "pd" to "Premier Division"), competitionsOf(all, listOf("cc")))
        assertEquals(listOf("up", "ppd"), filterMatches(all, MatchFilter.Upcoming, emptyList(), emptyList()).map { it.id })
        assertEquals(listOf("ft"), filterMatches(all, MatchFilter.Finished, emptyList(), emptyList()).map { it.id })
        assertEquals(mapOf(MatchFilter.Live to 2, MatchFilter.Upcoming to 2, MatchFilter.Finished to 1), statusCounts(all))
    }

    @Test fun spokenDescriptions() {
        assertEquals("A 1, B 1, 60th minute, live", matchDescription(tight))
        assertEquals("A, B, half time, live, score hidden by Spoiler Shield", matchDescription(m("x", "a", "b", 1, 0, label = "HT"), hidden = true))
        assertEquals("A, B, postponed", matchDescription(m("p", "a", "b", null, null, Status.Postponed, label = "Postponed")))
        assertEquals("A 0, B 0, full time", matchDescription(m("f", "a", "b", 0, 0, Status.Finished, label = "FT")))
    }

    @Test fun countdownAndNewsDates() {
        val min = 60_000L
        assertEquals("1d 04h 12m", countdown(0, (24 * 60 + 4 * 60 + 12) * min))
        assertEquals("2h 05m", countdown(0, 125 * min))
        assertEquals("9m", countdown(0, 9 * min))
        assertEquals("Starting now", countdown(10 * min, 0))
        val now = 1_794_508_200_000L // 2026-11-12
        assertEquals("3 h ago", newsDate(now - 3 * 60 * min, now, ZoneOffset.UTC))
        assertEquals("Yesterday", newsDate(now - 30 * 60 * min, now, ZoneOffset.UTC))
        assertEquals("13 Jul", newsDate(1_783_980_177_000L, now, ZoneOffset.UTC))
        assertEquals("11 Sep 2025", newsDate(1_757_581_200_000L, now, ZoneOffset.UTC))
    }

    @Test fun recentSearchesAreDedupedAndCapped() {
        var p = UserPrefs()
        listOf("Northgate", "marsh", "NORTHGATE", "x", "Premier").forEach { p = PrefsReducer.addRecentSearch(p, it) }
        assertEquals(listOf("Premier", "NORTHGATE", "marsh"), p.recentSearches)
        repeat(12) { p = PrefsReducer.addRecentSearch(p, "query $it") }
        assertEquals(UserPrefs.RECENT_SEARCH_LIMIT, p.recentSearches.size)
        assertEquals("query 11", p.recentSearches.first())
        p = PrefsReducer.removeRecentSearch(p, "QUERY 11")
        assertEquals("query 10", p.recentSearches.first())
        assertEquals(emptyList<String>(), PrefsReducer.clearRecentSearches(p).recentSearches)
    }
}
