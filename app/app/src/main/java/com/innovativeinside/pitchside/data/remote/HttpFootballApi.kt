package com.innovativeinside.pitchside.data.remote

import com.innovativeinside.pitchside.core.AppError
import com.innovativeinside.pitchside.data.remote.dto.AskRequest
import com.innovativeinside.pitchside.data.remote.dto.AskResponse
import com.innovativeinside.pitchside.data.remote.dto.ErrorBody
import com.innovativeinside.pitchside.data.remote.dto.EventDto
import com.innovativeinside.pitchside.data.remote.dto.FixtureDto
import com.innovativeinside.pitchside.data.remote.dto.LeagueDto
import com.innovativeinside.pitchside.data.remote.dto.LineupsDto
import com.innovativeinside.pitchside.data.remote.dto.ListEnvelope
import com.innovativeinside.pitchside.data.remote.dto.NewsDto
import com.innovativeinside.pitchside.data.remote.dto.PlayerDto
import com.innovativeinside.pitchside.data.remote.dto.PlayerSummaryDto
import com.innovativeinside.pitchside.data.remote.dto.ScorerDto
import com.innovativeinside.pitchside.data.remote.dto.SearchDto
import com.innovativeinside.pitchside.data.remote.dto.StandingRowDto
import com.innovativeinside.pitchside.data.remote.dto.StatLineDto
import com.innovativeinside.pitchside.data.remote.dto.TeamDto
import com.innovativeinside.pitchside.data.remote.dto.TeamRefDto
import java.io.IOException
import java.util.concurrent.TimeUnit
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlinx.coroutines.delay
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.serialization.KSerializer
import kotlinx.serialization.json.Json
import okhttp3.Call
import okhttp3.Callback
import okhttp3.HttpUrl
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response

/**
 * [FootballApi] over HTTPS with OkHttp and kotlinx.serialization.
 *
 * - Adds `Authorization: Bearer <token>` when [tokenProvider] returns a token, never logs it.
 * - GET requests are retried up to [maxRetries] times with backoff on network errors, timeouts, 429 and 5xx.
 * - Every failure surfaces as an [AppError]; a 401 also calls [onUnauthorized] so the session can end cleanly.
 */
class HttpFootballApi(
    baseUrl: String,
    private val client: OkHttpClient = defaultClient(),
    private val tokenProvider: suspend () -> String? = { null },
    private val onUnauthorized: suspend () -> Unit = {},
    private val maxRetries: Int = 2,
    private val retryDelayMs: Long = 400,
    private val clientVersion: String = "android",
) : FootballApi {

    private val base: HttpUrl = baseUrl.trimEnd('/').plus("/").toHttpUrl()

    internal val json = Json { ignoreUnknownKeys = true; explicitNulls = false; coerceInputValues = true }

    override suspend fun fixtures(date: String) = getList("v1/fixtures", FixtureDto.serializer(), "date" to date)
    override suspend fun liveFixtures() = getList("v1/fixtures/live", FixtureDto.serializer())
    override suspend fun fixture(id: String) = get("v1/fixtures/${seg(id)}", FixtureDto.serializer())
    override suspend fun events(fixtureId: String) = getList("v1/fixtures/${seg(fixtureId)}/events", EventDto.serializer())
    override suspend fun stats(fixtureId: String) = getList("v1/fixtures/${seg(fixtureId)}/stats", StatLineDto.serializer())
    override suspend fun lineups(fixtureId: String) = get("v1/fixtures/${seg(fixtureId)}/lineups", LineupsDto.serializer())
    override suspend fun leagues() = getList("v1/leagues", LeagueDto.serializer())
    override suspend fun league(id: String) = get("v1/leagues/${seg(id)}", LeagueDto.serializer())
    override suspend fun leagueFixtures(leagueId: String) = getList("v1/leagues/${seg(leagueId)}/fixtures", FixtureDto.serializer())
    override suspend fun standings(leagueId: String) = getList("v1/leagues/${seg(leagueId)}/standings", StandingRowDto.serializer())
    override suspend fun scorers(leagueId: String) = getList("v1/leagues/${seg(leagueId)}/scorers", ScorerDto.serializer())
    override suspend fun popularTeams() = getList("v1/teams", TeamRefDto.serializer(), "popular" to "true")
    override suspend fun team(id: String) = get("v1/teams/${seg(id)}", TeamDto.serializer())
    override suspend fun teamFixtures(teamId: String) = getList("v1/teams/${seg(teamId)}/fixtures", FixtureDto.serializer())
    override suspend fun player(id: String) = get("v1/players/${seg(id)}", PlayerDto.serializer())
    override suspend fun topPlayers() = getList("v1/players/top", PlayerSummaryDto.serializer())

    override suspend fun news(category: String?, cursor: String?): Page<NewsDto> {
        val env = get("v1/news", ListEnvelope.serializer(NewsDto.serializer()), "category" to category, "cursor" to cursor)
        return Page(env.items, env.nextCursor)
    }

    override suspend fun article(id: String) = get("v1/news/${seg(id)}", NewsDto.serializer())
    override suspend fun search(query: String) = get("v1/search", SearchDto.serializer(), "q" to query.trim())

    override suspend fun ask(request: AskRequest): AskResponse {
        val body = json.encodeToString(AskRequest.serializer(), request).toRequestBody(JSON)
        return execute(Request.Builder().url(base.resolve("v1/ask")!!).post(body), AskResponse.serializer(), retry = false)
    }

    // ---- plumbing ----

    private fun seg(s: String): String = java.net.URLEncoder.encode(s, "UTF-8")

    private suspend fun <T> getList(path: String, item: KSerializer<T>, vararg query: Pair<String, String?>): List<T> =
        get(path, ListEnvelope.serializer(item), *query).items

    private suspend fun <T> get(path: String, serializer: KSerializer<T>, vararg query: Pair<String, String?>): T {
        val url = base.resolve(path)!!.newBuilder().apply { query.forEach { (k, v) -> if (v != null) addQueryParameter(k, v) } }.build()
        return execute(Request.Builder().url(url).get(), serializer, retry = true)
    }

    private suspend fun <T> execute(builder: Request.Builder, serializer: KSerializer<T>, retry: Boolean): T {
        var attempt = 0
        while (true) {
            try {
                val token = tokenProvider()
                val req = builder
                    .header("Accept", "application/json")
                    .header("X-Client", clientVersion)
                    .apply { if (token != null) header("Authorization", "Bearer $token") else removeHeader("Authorization") }
                    .build()
                val text = client.newCall(req).await().use { resp -> readOrThrow(resp) }
                return try {
                    json.decodeFromString(serializer, text)
                } catch (e: Exception) {
                    throw AppError.Parse(e)
                }
            } catch (t: Throwable) {
                val err = AppError.from(t)
                if (err is AppError.Unauthorized) onUnauthorized()
                val canRetry = retry && attempt < maxRetries && (err is AppError.Network || err is AppError.Timeout || err is AppError.Server || err is AppError.RateLimited)
                if (!canRetry) throw err
                attempt++
                delay(retryDelayMs * attempt)
            }
        }
    }

    private fun readOrThrow(resp: Response): String {
        val text = resp.body?.string().orEmpty()
        if (resp.isSuccessful) return text
        val serverMessage = runCatching { json.decodeFromString(ErrorBody.serializer(), text).message }.getOrNull()
        throw AppError.fromHttp(resp.code, serverMessage)
    }

    companion object {
        private val JSON = "application/json; charset=utf-8".toMediaType()

        fun defaultClient(): OkHttpClient = OkHttpClient.Builder()
            .connectTimeout(10, TimeUnit.SECONDS)
            .readTimeout(15, TimeUnit.SECONDS)
            .writeTimeout(15, TimeUnit.SECONDS)
            .callTimeout(30, TimeUnit.SECONDS)
            .build()
    }
}

/** Cancellable bridge from OkHttp's async call to a coroutine. */
internal suspend fun Call.await(): Response = suspendCancellableCoroutine { cont ->
    cont.invokeOnCancellation { runCatching { cancel() } }
    enqueue(object : Callback {
        override fun onFailure(call: Call, e: IOException) { if (cont.isActive) cont.resumeWithException(e) }
        override fun onResponse(call: Call, response: Response) { cont.resume(response) }
    })
}

