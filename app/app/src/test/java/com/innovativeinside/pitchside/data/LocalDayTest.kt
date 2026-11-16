package com.innovativeinside.pitchside.data

import com.innovativeinside.pitchside.data.remote.ApiFootballComApi
import com.innovativeinside.pitchside.data.remote.LocalDayWindow
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.ZoneOffset
import kotlinx.coroutines.test.runTest
import okhttp3.mockwebserver.Dispatcher
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import okhttp3.mockwebserver.RecordedRequest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/** Fixtures are asked for by the device's local day, not the UTC day (bug: Matches showed nothing for "today" in Pakistan). */
class LocalDayTest {

    private val karachi = ZoneId.of("Asia/Karachi") // UTC+5, no DST
    private val sun15 = LocalDate.of(2026, 11, 15)
    private fun utcMs(y: Int, mo: Int, d: Int, h: Int, mi: Int = 0) = LocalDateTime.of(y, mo, d, h, mi).toInstant(ZoneOffset.UTC).toEpochMilli()

    // ---------------- pure window ----------------

    @Test fun pakistanDayCoversTwoUtcDays() {
        val w = LocalDayWindow.of(sun15, karachi)
        assertEquals(LocalDate.of(2026, 11, 14), w.utcFrom)
        assertEquals(LocalDate.of(2026, 11, 15), w.utcTo)
        assertEquals(utcMs(2026, 11, 14, 19), w.startMs)
        assertEquals(utcMs(2026, 11, 15, 19), w.endMs)
        assertTrue(w.contains(utcMs(2026, 11, 14, 20)))  // Sat 20:00 UTC = Sun 01:00 PKT
        assertTrue(w.contains(utcMs(2026, 11, 15, 15)))  // Sun 15:00 UTC = Sun 20:00 PKT
        assertFalse(w.contains(utcMs(2026, 11, 15, 19))) // Sun 19:00 UTC = Mon 00:00 PKT
        assertFalse(w.contains(utcMs(2026, 11, 14, 15))) // Sat 15:00 UTC = Sat 20:00 PKT
    }

    @Test fun utcAndWestOfUtcDays() {
        val utc = LocalDayWindow.of(sun15, ZoneOffset.UTC)
        assertEquals(sun15, utc.utcFrom); assertEquals(sun15, utc.utcTo)
        val la = LocalDayWindow.of(sun15, ZoneId.of("America/Los_Angeles"))
        assertEquals(sun15, la.utcFrom); assertEquals(LocalDate.of(2026, 11, 16), la.utcTo)
    }

    @Test fun dstDayIsTwentyFiveHours() {
        val w = LocalDayWindow.of(LocalDate.of(2026, 10, 25), ZoneId.of("Europe/London"))
        assertEquals(25 * 3_600_000L, w.endMs - w.startMs)
    }

    // ---------------- API client ----------------

    private lateinit var server: MockWebServer
    private val seen = mutableListOf<RecordedRequest>()
    private var eventsBody = ""

    private fun row(id: String, date: String, time: String) =
        """{"match_id":"$id","league_id":"177","league_name":"Premier League","country_name":"Ghana","match_date":"$date","match_time":"$time","match_status":"","match_hometeam_id":"1","match_hometeam_name":"Aduana","match_awayteam_id":"2","match_awayteam_name":"Bechem United","match_live":"0","match_round":"11"}"""

    @Before fun setUp() {
        server = MockWebServer()
        server.dispatcher = object : Dispatcher() {
            override fun dispatch(request: RecordedRequest): MockResponse {
                synchronized(seen) { seen += request }
                val action = request.requestUrl!!.queryParameter("action")
                val body = if (action == "get_events") eventsBody else """{"error":404,"message":"No event found (please check your plan)!"}"""
                return MockResponse().setBody(body).setHeader("Content-Type", "application/json")
            }
        }
        server.start()
    }

    @After fun tearDown() = server.shutdown()

    private fun api(zone: ZoneId) = ApiFootballComApi("k", server.url("/").toString(), nowMs = { utcMs(2026, 11, 15, 6) }, zone = { zone }, retryDelayMs = 1)

    @Test fun fixturesAreFilteredToTheLocalDay() = runTest {
        eventsBody = "[" + listOf(
            row("sat-afternoon", "2026-11-14", "15:00"), // Sat 20:00 PKT: not Sunday
            row("sat-night", "2026-11-14", "20:00"),     // Sun 01:00 PKT
            row("sun", "2026-11-15", "15:00"),           // Sun 20:00 PKT
            row("sun-late", "2026-11-15", "19:30"),      // Mon 00:30 PKT: not Sunday
        ).joinToString(",") + "]"
        val list = api(karachi).fixtures("2026-11-15")
        assertEquals(listOf("sat-night", "sun"), list.map { it.id })
        val req = seen.first { it.requestUrl!!.queryParameter("action") == "get_events" }.requestUrl!!
        assertEquals("2026-11-14", req.queryParameter("from"))
        assertEquals("2026-11-15", req.queryParameter("to"))
        assertEquals("UTC", req.queryParameter("timezone"))
    }

    @Test fun noEventFoundIsAnEmptyDayNotAnError() = runTest {
        eventsBody = """{"error":404,"message":"No event found (please check your plan)!"}"""
        assertTrue(api(karachi).fixtures("2026-11-15").isEmpty())
    }
}
