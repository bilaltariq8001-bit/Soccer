package com.innovativeinside.pitchside.data

import com.innovativeinside.pitchside.core.AppError
import com.innovativeinside.pitchside.data.remote.ApiFootballComApi
import com.innovativeinside.pitchside.data.remote.ApiFootballParse
import com.innovativeinside.pitchside.data.remote.ApiFootballParse.objects
import com.innovativeinside.pitchside.data.remote.dto.AskRequest
import java.time.LocalDate
import java.time.ZoneOffset
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import okhttp3.mockwebserver.Dispatcher
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import okhttp3.mockwebserver.RecordedRequest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

private fun res(name: String): String = ApiFootballTest::class.java.getResource("/afl/$name")!!.readText()
private fun rows(name: String): List<JsonObject> = Json.parseToJsonElement(res(name)).objects()

/** apifootball.com parsing (pure) and the API client against trimmed real responses served by MockWebServer. */
class ApiFootballTest {

    // ---------------- pure parsing ----------------

    @Test fun statusMapping() {
        assertEquals("finished", ApiFootballParse.status("Finished").status)
        assertEquals("AET", ApiFootballParse.status("After ET").label)
        assertEquals("Pens", ApiFootballParse.status("After Pen.").label)
        assertEquals("postponed", ApiFootballParse.status("Postponed").status)
        assertEquals("postponed", ApiFootballParse.status("Cancelled").status)
        assertEquals("upcoming", ApiFootballParse.status("").status)
        assertEquals("upcoming", ApiFootballParse.status("Not Started").status)
        val ht = ApiFootballParse.status("Half Time")
        assertEquals("live", ht.status); assertEquals("HT", ht.short)
        val m = ApiFootballParse.status("67")
        assertEquals("live", m.status); assertEquals(67, m.minute); assertEquals("67'", m.label)
        assertEquals("45+2'", ApiFootballParse.status("45+2").label)
        assertEquals("live", ApiFootballParse.status("", live = true).status)
    }

    @Test fun finishedFixtureMapsScoresTimesAndCrests() {
        val e = rows("events_mixed.json").first { ApiFootballParse.run { it.str("match_id") } == "824681" }
        val f = ApiFootballParse.fixture(e)
        assertEquals("finished", f.status)
        assertEquals("Bechem United", f.home.name)
        assertEquals(2, f.homeScore); assertEquals(1, f.awayScore)
        assertEquals(LocalDate.of(2026, 11, 8).atTime(15, 0).toInstant(ZoneOffset.UTC).toEpochMilli(), f.kickoffMs)
        assertTrue(f.round.startsWith("Round "))
        assertTrue(f.home.logo!!.startsWith("https://"))
        assertEquals("177", f.leagueId)
    }

    @Test fun upcomingFixtureHasNoScore() {
        val e = rows("events_mixed.json").first { ApiFootballParse.run { it.str("match_id") } == "813780" }
        val f = ApiFootballParse.fixture(e)
        assertEquals("upcoming", f.status)
        assertNull(f.homeScore)
        assertEquals("", f.label)
    }

    @Test fun timelineStatsAndLineupFromRealEvent() {
        val e = rows("events_mixed.json").first { ApiFootballParse.run { it.str("match_id") } == "824681" }
        val tl = ApiFootballParse.timeline(e)
        assertEquals(3, tl.count { it.kind == "goal" })
        assertTrue(tl.filter { it.kind == "goal" }.all { it.playerName.isNotBlank() && it.teamId.isNotBlank() })
        val stats = ApiFootballParse.stats(ApiFootballParse.run { e.arr("statistics") })
        assertTrue(stats.isNotEmpty())
        assertEquals(stats.map { it.label }.distinct(), stats.map { it.label })
        assertTrue(stats.any { it.label == "Corners" })
        val lineup = ApiFootballParse.lineup(ApiFootballParse.run { e.obj("lineup")!!.obj("home") }, "h", "")
        assertEquals(11, lineup.startXI.size)
        assertTrue(lineup.startXI.all { it.grid == null }) // no formation published for this match
    }

    @Test fun cardsAndRedCardsAreRead() {
        val e = rows("events_mixed.json").first { ApiFootballParse.run { it.str("match_id") } == "813760" }
        val cards = ApiFootballParse.timeline(e).filter { it.kind == "redCard" }
        assertEquals(2, cards.size)
        assertEquals(62, cards.first().minute)
    }

    @Test fun lineupGridFollowsFormation() {
        val side = Json.parseToJsonElement(
            """{"starting_lineups":[${(1..11).joinToString(",") { """{"lineup_player":"P$it","lineup_number":"$it","lineup_position":"$it","player_key":"$it"}""" }}],
               "substitutes":[],"coach":[{"lineup_player":"C. Coach"}]}""",
        ) as JsonObject
        val l = ApiFootballParse.lineup(side, "h", "4-3-3")
        assertEquals("1:1", l.startXI[0].grid)
        assertEquals("2:4", l.startXI[4].grid)
        assertEquals("4:3", l.startXI[10].grid)
        assertEquals("C. Coach", l.coach)
        assertEquals("45+2", ApiFootballParse.minute("45+2").let { "${it.first}+${it.second}" })
    }

    @Test fun oddsPreferBet365AndPredictionsParse() {
        val o = ApiFootballParse.odds(rows("odds.json"))!!
        assertEquals("bet365", o.bookmaker)
        assertEquals(1.55, o.home, 0.001)
        val p = ApiFootballParse.prediction(rows("predictions.json").first())!!
        assertEquals(90.0, p.home, 0.01); assertEquals(3.0, p.draw, 0.01); assertEquals(2.0, p.away, 0.01)
    }

    @Test fun scorersCountedFromGoalEvents() {
        val s = ApiFootballParse.scorersFromEvents(rows("events_mixed.json"), "x")
        assertTrue(s.isNotEmpty())
        assertEquals(1, s.first().rank)
        assertTrue(s.zipWithNext().all { (a, b) -> a.goals >= b.goals })
    }

    @Test fun liveCommentaryParse() {
        val root = Json.parseToJsonElement(
            """{"55":{"match_id":"55","live_odds":[
                 {"odd_name":"Fulltime Result","suspended":"No","type":"1","value":"2.1","upd":"2026-11-14 15:10:00"},
                 {"odd_name":"Fulltime Result","suspended":"No","type":"X","value":"3.2","upd":"2026-11-14 15:10:00"},
                 {"odd_name":"Fulltime Result","suspended":"No","type":"2","value":"3.9","upd":"2026-11-14 15:10:00"}],
               "live_comments":[{"time":"12:01","text":"Corner for the home side","state":""},{"time":"13:00","text":"","state":""}]}}""",
        )
        val d = ApiFootballParse.liveData(root, "55")!!
        assertEquals(1, d.comments.size)
        assertEquals(3.2, d.liveOdds!!.draw, 0.001)
        assertTrue(d.liveOdds!!.live)
        assertNull(ApiFootballParse.liveData(root, "56"))
    }

    @Test fun socketPushPatchesFixturesAndPlanNoticeStopsRetries() = runTest {
        val api = ApiFootballComApi("k", "http://localhost/", nowMs = { 0L })
        val socket = com.innovativeinside.pitchside.data.remote.LiveScoreSocket("k", api)
        val e = rows("events_mixed.json").first { ApiFootballParse.run { it.str("match_id") } == "813780" }
        val live = JsonObject(e + mapOf("match_status" to kotlinx.serialization.json.JsonPrimitive("23"), "match_live" to kotlinx.serialization.json.JsonPrimitive("1"),
            "match_hometeam_score" to kotlinx.serialization.json.JsonPrimitive("1"), "match_awayteam_score" to kotlinx.serialization.json.JsonPrimitive("0")))
        socket.handle("[$live]") // stored in the API's live overlay (covered by pushedLiveRowShowsInLiveFixtures)
        assertFalse(socket.planGated)
        socket.handle("Your plan is expired or you don't have access to websocket!")
        assertTrue(socket.planGated)
    }

    // ---------------- client against MockWebServer ----------------

    private lateinit var server: MockWebServer
    private val seen = mutableListOf<RecordedRequest>()
    private val now = LocalDate.of(2026, 11, 14).atTime(12, 0).toInstant(ZoneOffset.UTC).toEpochMilli()

    private fun body(text: String) = MockResponse().setBody(text).setHeader("Content-Type", "application/json")
    private val noEvent = """{"error":404,"message":"No event found (please check your plan)!"}"""

    @Before fun setUp() {
        server = MockWebServer()
        server.dispatcher = object : Dispatcher() {
            override fun dispatch(request: RecordedRequest): MockResponse {
                synchronized(seen) { seen += request }
                val url = request.requestUrl!!
                if (url.queryParameter("APIkey") == "bad") return body("""{"error":404,"message":"Authentification failed!"}""")
                return when (url.queryParameter("action")) {
                    "get_leagues" -> body(res("leagues.json"))
                    "get_countries" -> body(res("countries.json"))
                    "get_standings" -> body(res(if (url.queryParameter("league_id") == "149") "standings_149.json" else "standings_177.json"))
                    "get_events" -> when {
                        url.queryParameter("match_live") == "1" -> body(noEvent)
                        url.queryParameter("league_id") == "149" -> body(res("events_149_window.json"))
                        url.queryParameter("league_id") == "177" -> body(res("events_mixed.json").let { t -> "[" + Json.parseToJsonElement(t).objects().filter { ApiFootballParse.run { it.str("league_id") } == "177" }.joinToString(",") + "]" })
                        url.queryParameter("match_id") == "824681" -> body("[" + rows("events_mixed.json").first { ApiFootballParse.run { it.str("match_id") } == "824681" } + "]")
                        url.queryParameter("match_id") != null -> body(noEvent)
                        else -> body(res("events_mixed.json"))
                    }
                    "get_predictions" -> body(res("predictions.json"))
                    "get_topscorers" -> body("""{"error":404,"message":"No country found (please check your plan)!"}""")
                    "get_odds" -> body("""{"error":1005,"message":"You do not have access to this method."}""")
                    "get_H2H" -> body(res("h2h.json"))
                    "get_videos" -> body(noEvent)
                    else -> body(noEvent)
                }
            }
        }
        server.start()
    }

    @After fun tearDown() = server.shutdown()

    private fun api(key: String = "test-key") = ApiFootballComApi(key, server.url("/").toString(), nowMs = { now }, zone = { ZoneOffset.UTC }, retryDelayMs = 1)

    @Test fun fixturesForADayUseUtcAndAttachPredictions() = runTest {
        val list = api().fixtures("2026-11-14")
        assertEquals(listOf("813780"), list.map { it.id })
        assertNotNull(list.single().prediction)
        val req = seen.first { it.requestUrl!!.queryParameter("action") == "get_events" }.requestUrl!!
        assertEquals("UTC", req.queryParameter("timezone"))
        assertEquals("test-key", req.queryParameter("APIkey"))
    }

    @Test fun splitLeagueBecomesDivisionsAndKeepsSingleLeague() = runTest {
        val leagues = api().leagues()
        assertTrue(leagues.any { it.id == "177" && it.country == "Ghana" })
        val divisions = leagues.filter { it.id.startsWith("149.") }
        assertEquals(3, divisions.size)
        assertTrue(divisions.all { it.country == "England" && it.teamCount > 0 })
        assertTrue(divisions.any { it.name == "Southern League Premier Central" })
        // England comes before Ghana: get_countries order.
        assertTrue(leagues.indexOfFirst { it.country == "England" } < leagues.indexOfFirst { it.country == "Ghana" })
        val table = api().standings(divisions.first().id)
        assertTrue(table.isNotEmpty())
        assertEquals(table.sortedBy { it.rank }, table)
        assertTrue(table.all { it.team.leagueId == divisions.first().id })
    }

    @Test fun standingsCarryTheSourceZoneLabelForTableMarkers() {
        val parsed = rows("standings_177.json").map { ApiFootballParse.standingRow(it, "177") }
        assertEquals(1, parsed.count { it.zone == "Promotion - CAF Champions League (Qualification)" })
        assertEquals(parsed.size - 1, parsed.count { it.zone == null })
        val mapped = parsed.map { com.innovativeinside.pitchside.data.remote.Mappers.standing(it) }
        assertTrue(mapped.single { it.zone.isNotBlank() }.zone.startsWith("Promotion"))
        assertTrue(rows("standings_149.json").map { ApiFootballParse.standingRow(it, "149") }.any { it.zone == "Relegation" })
    }

    @Test fun noLiveMatchesIsEmptyNotAnError() = runTest {
        assertTrue(api().liveFixtures().isEmpty())
    }

    @Test fun pushedLiveRowShowsInLiveFixtures() = runTest {
        val a = api()
        val e = rows("events_mixed.json").first { ApiFootballParse.run { it.str("match_id") } == "813780" }
        val p = kotlinx.serialization.json.JsonPrimitive("23")
        a.applyLivePush(listOf(JsonObject(e + mapOf("match_status" to p, "match_live" to kotlinx.serialization.json.JsonPrimitive("1")))))
        val live = a.liveFixtures()
        assertEquals(listOf("813780"), live.map { it.id })
        assertEquals(23, live.single().minute)
    }

    @Test fun topScorersFallBackToGoalEventsWhenNotOnPlan() = runTest {
        val s = api().scorers("177")
        assertTrue(s.isNotEmpty())
        assertTrue(s.all { it.goals > 0 })
    }

    @Test fun planGatedMethodIsForbiddenAndCached() = runTest {
        val a = api()
        repeat(2) {
            val err = runCatching { a.odds("813780") }.exceptionOrNull()
            assertTrue(err is AppError.Forbidden)
        }
        assertEquals(1, seen.count { it.requestUrl!!.queryParameter("action") == "get_odds" })
    }

    @Test fun badKeySurfacesAsForbidden() = runTest {
        assertTrue(runCatching { api("bad").leagues() }.exceptionOrNull() is AppError.Forbidden)
    }

    @Test fun matchDetailPiecesAndH2h() = runTest {
        val a = api()
        val f = a.fixture("824681")
        assertEquals("finished", f.status)
        assertEquals(3, a.events("824681").count { it.kind == "goal" })
        assertTrue(a.stats("824681").isNotEmpty())
        assertEquals(2, a.lineups("824681").teams.size)
        assertTrue(a.videos("824681").isEmpty())
        // The trimmed H2H sample only has meetings of other teams' fixture ids, so they are kept (finished, not this match).
        assertFalse(a.headToHead("824681").any { it.id == "824681" })
        assertTrue(runCatching { a.fixture("999") }.exceptionOrNull() is AppError.NotFound)
    }

    @Test fun cachingAvoidsRepeatCalls() = runTest {
        val a = api()
        a.fixtures("2026-11-14"); a.fixtures("2026-11-14")
        assertEquals(1, seen.count { it.requestUrl!!.queryParameter("action") == "get_events" && it.requestUrl!!.queryParameter("from") != null && it.requestUrl!!.queryParameter("league_id") == null })
    }

    @Test fun askAnswersFromTables() = runTest {
        val answer = api().ask(AskRequest("Who leads the table?", "en")).answer
        assertTrue(answer, answer.contains("lead"))
        assertTrue(api().news(null, null).items.isEmpty())
    }
}
