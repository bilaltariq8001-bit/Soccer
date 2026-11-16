package com.innovativeinside.pitchside.data

import com.innovativeinside.pitchside.data.model.EventKind
import com.innovativeinside.pitchside.data.model.NewsCategory
import com.innovativeinside.pitchside.data.model.Position
import com.innovativeinside.pitchside.data.model.Status
import com.innovativeinside.pitchside.data.remote.Mappers
import com.innovativeinside.pitchside.data.remote.dto.EventDto
import com.innovativeinside.pitchside.data.remote.dto.FixtureDto
import com.innovativeinside.pitchside.data.remote.dto.LineupDto
import com.innovativeinside.pitchside.data.remote.dto.LineupPlayerDto
import com.innovativeinside.pitchside.data.remote.dto.LineupsDto
import com.innovativeinside.pitchside.data.remote.dto.NewsDto
import com.innovativeinside.pitchside.data.remote.dto.StandingRowDto
import com.innovativeinside.pitchside.data.remote.dto.TeamRefDto
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class MappersTest {
    private val home = TeamRefDto("1", "Northgate FC", null, "#1F6F6B")
    private val away = TeamRefDto("2", "Riverside", "RU", null)
    private fun fx(status: String, label: String = "", minute: Int? = null, short: String = "", hs: Int? = 1, aw: Int? = 0) =
        FixtureDto("f1", "pd", "Premier", "R1", 0L, status, short, minute, label, "Park", null, home, away, hs, aw)

    @Test fun teamColorAndMonogram() {
        val t = Mappers.team(home)
        assertEquals(0xFF1F6F6BL, t.color)
        assertEquals("NF", t.short)
        assertEquals("RU", Mappers.team(away).short)
        assertEquals(0xFF34495EL, Mappers.parseColor("not-a-colour"))
        assertEquals(0x801F6F6BL, Mappers.parseColor("#801F6F6B"))
        assertEquals("?", Mappers.monogram("  "))
    }

    @Test fun statusMapping() {
        assertEquals(Status.Live, Mappers.status("LIVE"))
        assertEquals(Status.Finished, Mappers.status("finished"))
        assertEquals(Status.Postponed, Mappers.status("cancelled"))
        assertEquals(Status.Upcoming, Mappers.status("anything-else"))
    }

    @Test fun matchLabels() {
        assertEquals("74'", Mappers.match(fx("live", minute = 74)) { "x" }.label)
        assertEquals("HT", Mappers.match(fx("live", short = "HT")) { "x" }.label)
        assertEquals("FT", Mappers.match(fx("finished")) { "x" }.label)
        assertEquals("Postponed", Mappers.match(fx("postponed")) { "x" }.label)
        val up = Mappers.match(fx("upcoming", hs = 0, aw = 0)) { "20:45" }
        assertEquals("20:45", up.label)
        // An upcoming match never shows a score, even if the source sends 0-0.
        assertNull(up.homeScore)
        assertEquals("Custom", Mappers.match(fx("live", label = "Custom")) { "x" }.label)
    }

    @Test fun eventsDropUnknownAndSortNewestFirst() {
        val ev = Mappers.events(listOf(
            EventDto("a", "goal", 10, null, "1", "A", "B"),
            EventDto("b", "var", 20, null, "1", "X"),
            EventDto("c", "redCard", 45, 2, "2", "C"),
        ), homeTeamId = "1")
        assertEquals(2, ev.size)
        assertEquals(EventKind.RedCard, ev[0].kind)
        assertEquals("45+2'", ev[0].minuteLabel)
        assertTrue(ev[1].isHome)
        assertEquals("Assist B", ev[1].detail)
    }

    @Test fun lineupGridToPitchFractions() {
        val dto = LineupDto("1", "Coach", "4-4-2", listOf(
            LineupPlayerDto("p1", "Keeper", 1, "G", "1:1"),
            LineupPlayerDto("p2", "Left", 3, "D", "2:1"),
            LineupPlayerDto("p3", "Right", 2, "D", "2:2"),
            LineupPlayerDto("p9", "Striker", 9, "F", "3:1"),
        ))
        val l = Mappers.lineup(dto, scorers = setOf("Striker"))
        val gk = l.starters.first { it.number == 1 }
        val st = l.starters.first { it.number == 9 }
        assertEquals(0.5f, gk.x, 0.001f)
        assertTrue("keeper sits deepest", gk.y > st.y)
        assertTrue(st.scored)
        assertNull(Mappers.lineups(LineupsDto(listOf(dto)), "1", "2"))
    }

    @Test fun standingFormIsSanitised() {
        val row = Mappers.standing(StandingRowDto(1, home, 9, 7, 1, 1, 20, 7, 13, 22, "WW?DLLW"))
        assertEquals("WDLLW", row.form)
    }

    @Test fun newsMojibakeIsFixed() {
        val n = Mappers.news(NewsDto("n", "transfers", "Men,Äôs final", publishedMs = 0, readMinutes = 0))
        assertEquals("Men’s final", n.title)
        assertEquals(NewsCategory.Transfers, n.category)
        assertEquals(1, n.readMinutes)
    }

    @Test fun positions() {
        assertEquals(Position.Goalkeeper, Mappers.position("G"))
        assertEquals(Position.Defender, Mappers.position("Defender"))
        assertEquals(Position.Forward, Mappers.position("?"))
    }
}
