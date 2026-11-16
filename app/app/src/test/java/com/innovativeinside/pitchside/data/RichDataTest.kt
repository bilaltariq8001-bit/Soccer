package com.innovativeinside.pitchside.data

import com.innovativeinside.pitchside.data.model.FormText
import com.innovativeinside.pitchside.data.model.Match
import com.innovativeinside.pitchside.data.model.StandingRow
import com.innovativeinside.pitchside.data.model.Status
import com.innovativeinside.pitchside.data.model.Team
import com.innovativeinside.pitchside.data.model.WinProbability
import com.innovativeinside.pitchside.data.remote.ApiFootballParse
import com.innovativeinside.pitchside.data.remote.Mappers
import com.innovativeinside.pitchside.data.remote.dto.TeamRefDto
import com.innovativeinside.pitchside.ui.vm.countsByCompetition
import com.innovativeinside.pitchside.ui.vm.inForm
import com.innovativeinside.pitchside.ui.vm.pickSpotlight
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** Real-data enrichment: images, squad-based lineup shapes, season stats from events, form and Home picks. */
class RichDataTest {

    private fun obj(s: String) = Json.parseToJsonElement(s) as JsonObject

    private val a = Team("a", "Asante", "AS", 0xFF000000)
    private val b = Team("b", "Bechem", "BE", 0xFF000000)
    private val c = Team("c", "Cape", "CA", 0xFF000000)
    private val d = Team("d", "Dreams", "DR", 0xFF000000)

    private fun match(id: String, h: Team, aw: Team, status: Status, ko: Long, hs: Int? = null, as_: Int? = null, league: String = "177", probs: WinProbability? = null) =
        Match(id, league, "Premier League", "Round 9", "", null, ko, h, aw, hs, as_, status, if (status == Status.Finished) "FT" else "15:00", "", probs)

    private fun row(rank: Int, t: Team, pts: Int, form: String = "") = StandingRow(rank, t, 9, 0, 0, 0, 0, 0, 0, pts, form)

    @Test fun logosAndCountryFlowFromEventsToModels() {
        val e = obj("""{"match_id":"1","league_id":"177","league_name":"Premier League","country_name":"Ghana","match_date":"2026-11-15","match_time":"16:00",
            "match_status":"Finished","match_hometeam_id":"a","match_hometeam_name":"Asante","match_awayteam_id":"b","match_awayteam_name":"Bechem",
            "match_hometeam_score":"0","match_awayteam_score":"1","team_home_badge":"https://x/badges/a.jpg","team_away_badge":"","league_logo":"https://x/l.png"}""")
        val m = Mappers.match(ApiFootballParse.fixture(e)) { "" }
        assertEquals("https://x/badges/a.jpg", m.home.logo)
        assertNull(m.away.logo)
        assertEquals("https://x/l.png", m.leagueLogo)
        assertEquals("Ghana", m.country)
        assertNull(Mappers.team(TeamRefDto("z", "Zed", logo = "not-a-url")).logo)
    }

    @Test fun squadShapeBuildsFormationFromRoles() {
        val roles = listOf("Forward", "Defender", "Defender", "Midfielder", "Midfielder", "Forward", "Defender", "Goalkeeper", "Midfielder", null, "Defender")
        val (label, grids) = ApiFootballParse.squadShape(roles)!!
        assertEquals("4-4-2", label)
        assertEquals("1:1", grids[7])
        assertTrue(grids[0]!!.startsWith("4:"))
        // Two goalkeepers or too many unknowns: no honest shape.
        assertNull(ApiFootballParse.squadShape(roles.map { if (it == "Forward") "Goalkeeper" else it }))
        assertNull(ApiFootballParse.squadShape(List(11) { null }))
    }

    @Test fun lineupWithoutFormationUsesRosterPositionsAndPhotos() {
        val roles = listOf("Goalkeepers", "Defenders", "Defenders", "Defenders", "Defenders", "Midfielders", "Midfielders", "Midfielders", "Forwards", "Forwards", "Forwards")
        val side = obj("""{"starting_lineups":[${(1..11).joinToString(",") { """{"lineup_player":"P$it","lineup_number":"$it","lineup_position":"0","player_key":"k$it"}""" }}],"substitutes":[],"coach":[]}""")
        val roster = roles.mapIndexed { i, r -> "k${i + 1}" to ApiFootballParse.RosterEntry(ApiFootballParse.position(r), "https://x/p${i + 1}.jpg", i + 1, "", "a", 20) }.toMap()
        val dto = ApiFootballParse.lineup(side, "a", "", roster)
        assertTrue(dto.derived)
        assertEquals("4-3-3", dto.formation)
        assertEquals("https://x/p1.jpg", dto.startXI.first().photo)
        val l = Mappers.lineup(dto)
        assertTrue(l.hasPositions && l.shapeFromSquad)
        // Without a roster the lineup has no positions and says so.
        val bare = Mappers.lineup(ApiFootballParse.lineup(side, "a", ""))
        assertFalse(bare.hasPositions)
    }

    @Test fun playerSeasonIsCountedFromEvents() {
        val ev = obj("""{"match_id":"9","match_date":"2026-11-01","match_time":"15:00","match_status":"Finished","match_hometeam_id":"a","match_hometeam_name":"Asante",
            "match_awayteam_id":"b","match_awayteam_name":"Bechem","match_hometeam_score":"2","match_awayteam_score":"1",
            "goalscorer":[{"time":"10","home_scorer":"X","home_scorer_id":"p1","home_assist":"","home_assist_id":"","away_scorer":"","away_scorer_id":"","info":""},
                          {"time":"50","home_scorer":"Y","home_scorer_id":"p2","home_assist":"X","home_assist_id":"p1","away_scorer":"","away_scorer_id":"","info":""}],
            "cards":[{"time":"30","home_fault":"X","card":"yellow card","home_player_id":"p1","away_player_id":""}],
            "substitutions":{"home":[{"time":"70","substitution":"X | Z","substitution_player_id":"p1 | p3"}],"away":[]},
            "lineup":{"home":{"starting_lineups":[{"lineup_player":"X","player_key":"p1"}]},"away":{"starting_lineups":[]}}}""")
        val s = ApiFootballParse.playerSeason("p1", "a", listOf(ev))
        assertEquals(1, s.apps); assertEquals(1, s.starts); assertEquals(70, s.minutes)
        assertEquals(1, s.goals); assertEquals(1, s.assists); assertEquals(1, s.yellow)
        assertEquals("W", s.recent.single().result)
        val sub = ApiFootballParse.playerSeason("p3", "a", listOf(ev))
        assertEquals(20, sub.minutes); assertEquals(0, sub.starts)
        assertEquals(0, ApiFootballParse.playerSeason("nobody", "a", listOf(ev)).apps)
    }

    @Test fun untrackedZeroStatsAreDropped() {
        val rows = listOf(obj("""{"type":"Throw In","home":"0","away":"0"}"""), obj("""{"type":"Corners","home":"6","away":"2"}"""), obj("""{"type":"Red Cards","home":"0","away":"0"}"""))
        assertEquals(listOf("Corners", "Red cards"), ApiFootballParse.stats(rows).map { it.label })
    }

    @Test fun formAndOrdinalsFromRealResults() {
        val done = match("f", a, b, Status.Finished, 1_000, 2, 0)
        assertEquals("WL", FormText.formFrom("a", listOf(done, match("g", b, a, Status.Finished, 2_000, 3, 1))))
        // Upcoming games carry no result.
        assertEquals("", FormText.formFrom("c", listOf(match("x", c, d, Status.Upcoming, 1_000))))
        assertEquals(listOf("1st", "2nd", "3rd", "4th", "11th", "12th", "13th", "21st", "102nd"), listOf(1, 2, 3, 4, 11, 12, 13, 21, 102).map(FormText::ordinal))
    }

    @Test fun spotlightPrefersFollowedThenBiggestGame() {
        val now = 0L
        val tables = mapOf("177" to listOf(row(1, a, 20), row(2, b, 18), row(9, c, 8), row(10, d, 7)))
        val small = match("s", c, d, Status.Upcoming, 1_000)
        val big = match("b", a, b, Status.Upcoming, 5_000)
        assertEquals("b", pickSpotlight(listOf(small, big), emptyList(), emptyList(), tables, now)!!.id)
        assertEquals("s", pickSpotlight(listOf(small, big), emptyList(), listOf("d"), tables, now)!!.id)
        val result = match("r", a, c, Status.Finished, 500, 1, 0)
        assertEquals("r", pickSpotlight(emptyList(), listOf(result), emptyList(), tables, now)!!.id)
        assertNull(pickSpotlight(emptyList(), emptyList(), emptyList(), tables, now))
    }

    @Test fun competitionCountsAndFormGuide() {
        val ms = listOf(match("1", a, b, Status.Live, 0, 1, 0), match("2", c, d, Status.Upcoming, 0), match("3", a, c, Status.Upcoming, 0, league = "149"))
        val counts = countsByCompetition(ms)
        assertEquals("177", counts.first().leagueId); assertEquals(2, counts.first().count); assertEquals(1, counts.first().live)
        val hot = inForm(mapOf("177" to listOf(row(1, a, 20, "WWDWW"), row(2, b, 18, "LLLLL"), row(3, c, 15, "WWWWW"))), 2)
        assertEquals(listOf("c", "a"), hot.map { it.team.id })
    }
}
