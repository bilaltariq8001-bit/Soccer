package com.innovativeinside.pitchside.data

import com.innovativeinside.pitchside.core.AppError
import com.innovativeinside.pitchside.data.remote.HttpFootballApi
import com.innovativeinside.pitchside.data.remote.dto.AskRequest
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.test.runTest
import okhttp3.OkHttpClient
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import okhttp3.mockwebserver.SocketPolicy
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Before
import org.junit.Test

class HttpFootballApiTest {
    private lateinit var server: MockWebServer
    private var token: String? = "secret-token"
    private var unauthorizedCalls = 0

    private fun api(retries: Int = 0, readTimeoutMs: Long = 2_000) = HttpFootballApi(
        baseUrl = server.url("/").toString(),
        client = OkHttpClient.Builder().readTimeout(readTimeoutMs, TimeUnit.MILLISECONDS).build(),
        tokenProvider = { token },
        onUnauthorized = { unauthorizedCalls++ },
        maxRetries = retries,
        retryDelayMs = 1,
        clientVersion = "android/test",
    )

    @Before fun setUp() { server = MockWebServer().apply { start() } }
    @After fun tearDown() { server.shutdown() }

    private val fixtureJson = """{"items":[{"id":"f1","leagueId":"pd","leagueName":"Premier","kickoffMs":1,"status":"live","minute":12,
        "home":{"id":"1","name":"Northgate"},"away":{"id":"2","name":"Riverside"},"homeScore":1,"awayScore":0,"unknownField":true}]}"""

    @Test fun successSendsHeadersAndQueryAndParses() = runTest {
        server.enqueue(MockResponse().setBody(fixtureJson))
        val list = api().fixtures("2026-11-12")
        assertEquals("f1", list.single().id)
        val req = server.takeRequest()
        assertEquals("/v1/fixtures?date=2026-11-12", req.path)
        assertEquals("Bearer secret-token", req.getHeader("Authorization"))
        assertEquals("application/json", req.getHeader("Accept"))
        assertEquals("android/test", req.getHeader("X-Client"))
    }

    @Test fun noTokenMeansNoAuthHeader() = runTest {
        token = null
        server.enqueue(MockResponse().setBody("""{"items":[]}"""))
        assertTrue(api().leagues().isEmpty())
        assertNull(server.takeRequest().getHeader("Authorization"))
    }

    @Test fun pathSegmentsAreEncoded() = runTest {
        server.enqueue(MockResponse().setBody("""{"id":"a b","name":"X"}"""))
        api().league("a b/c")
        assertEquals("/v1/leagues/a+b%2Fc", server.takeRequest().path)
    }

    private suspend fun expect(code: Int, check: (AppError) -> Boolean) {
        server.enqueue(MockResponse().setResponseCode(code).setBody("""{"code":"x","message":"Server says no"}"""))
        try { api().leagues(); fail("expected error") } catch (e: AppError) { assertTrue("$code -> $e", check(e)) }
    }

    @Test fun httpErrorsMapToTypedErrors() = runTest {
        expect(401) { it is AppError.Unauthorized }
        assertEquals(1, unauthorizedCalls)
        expect(403) { it is AppError.Forbidden }
        expect(404) { it is AppError.NotFound }
        expect(422) { it is AppError.Validation && it.message == "Server says no" }
        expect(500) { it is AppError.Server && it.code == 500 }
    }

    @Test fun malformedJsonIsParseError() = runTest {
        server.enqueue(MockResponse().setBody("{not json"))
        try { api().leagues(); fail() } catch (e: AppError) { assertTrue(e is AppError.Parse) }
    }

    @Test fun timeoutIsTimeoutError() = runTest {
        server.enqueue(MockResponse().setBody("""{"items":[]}""").setBodyDelay(2, TimeUnit.SECONDS))
        try { api(readTimeoutMs = 200).leagues(); fail() } catch (e: AppError) { assertTrue("$e", e is AppError.Timeout) }
    }

    @Test fun networkFailureIsNetworkError() = runTest {
        server.enqueue(MockResponse().setSocketPolicy(SocketPolicy.DISCONNECT_AT_START))
        try { api().leagues(); fail() } catch (e: AppError) { assertTrue("$e", e is AppError.Network) }
    }

    @Test fun getRetriesOnServerErrorThenSucceeds() = runTest {
        server.enqueue(MockResponse().setResponseCode(503))
        server.enqueue(MockResponse().setBody("""{"items":[{"id":"pd","name":"Premier"}]}"""))
        assertEquals("pd", api(retries = 2).leagues().single().id)
        assertEquals(2, server.requestCount)
    }

    @Test fun clientErrorsAreNotRetried() = runTest {
        server.enqueue(MockResponse().setResponseCode(404))
        try { api(retries = 2).leagues(); fail() } catch (e: AppError) { assertTrue(e is AppError.NotFound) }
        assertEquals(1, server.requestCount)
    }

    @Test fun askPostsJsonBodyAndIsNotRetried() = runTest {
        server.enqueue(MockResponse().setResponseCode(503))
        try { api(retries = 2).ask(AskRequest("Who leads?", "en")); fail() } catch (e: AppError) { assertTrue(e is AppError.Server) }
        val req = server.takeRequest()
        assertEquals("POST", req.method)
        assertEquals("""{"question":"Who leads?","language":"en"}""", req.body.readUtf8())
        assertEquals(1, server.requestCount)
    }

    @Test fun newsPagination() = runTest {
        server.enqueue(MockResponse().setBody("""{"items":[{"id":"n1","category":"analysis","title":"T","publishedMs":5}],"nextCursor":"4"}"""))
        val page = api().news("analysis", "0")
        assertEquals("4", page.nextCursor)
        assertEquals("/v1/news?category=analysis&cursor=0", server.takeRequest().path)
    }
}
