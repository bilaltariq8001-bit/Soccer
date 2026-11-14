package com.innovativeinside.pitchside.data.remote

import com.innovativeinside.pitchside.core.AppError
import com.innovativeinside.pitchside.data.remote.ApiFootballParse.arr
import com.innovativeinside.pitchside.data.remote.ApiFootballParse.obj
import com.innovativeinside.pitchside.data.remote.ApiFootballParse.objects
import com.innovativeinside.pitchside.data.remote.ApiFootballParse.str
import com.innovativeinside.pitchside.data.remote.dto.AskRequest
import com.innovativeinside.pitchside.data.remote.dto.AskResponse
import com.innovativeinside.pitchside.data.remote.dto.EventDto
import com.innovativeinside.pitchside.data.remote.dto.FixtureDto
import com.innovativeinside.pitchside.data.remote.dto.LeagueDto
import com.innovativeinside.pitchside.data.remote.dto.LineupsDto
import com.innovativeinside.pitchside.data.remote.dto.LiveDataDto
import com.innovativeinside.pitchside.data.remote.dto.NewsDto
import com.innovativeinside.pitchside.data.remote.dto.OddsDto
import com.innovativeinside.pitchside.data.remote.dto.PlayerDto
import com.innovativeinside.pitchside.data.remote.dto.PlayerSummaryDto
import com.innovativeinside.pitchside.data.remote.dto.PredictionDto
import com.innovativeinside.pitchside.data.remote.dto.ScorerDto
import com.innovativeinside.pitchside.data.remote.dto.SearchDto
import com.innovativeinside.pitchside.data.remote.dto.StandingRowDto
import com.innovativeinside.pitchside.data.remote.dto.StatLineDto
import com.innovativeinside.pitchside.data.remote.dto.TeamDto
import com.innovativeinside.pitchside.data.remote.dto.TeamRefDto
import com.innovativeinside.pitchside.data.remote.dto.VideoDto
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZoneOffset
import java.util.concurrent.ConcurrentHashMap
import kotlinx.coroutines.delay
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import okhttp3.HttpUrl
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.OkHttpClient
import okhttp3.Request

/**
 * [FootballApi] backed directly by apifootball.com v3 (https://apifootball.com/documentation/).
 *
 * - Coverage follows the key's plan: leagues come from get_leagues, so an upgraded plan shows more without code changes.
 * - Every call is cached in memory (live data ~30-60 s, tables/teams 10-60 min) because the API allows 180 calls/hour per
 *   endpoint. Concurrent requests for the same URL share one network call.
 * - "No event found" (error 404) on a list endpoint means "nothing", not a failure. Plan-gated methods (error 1005) and a
 *   bad key surface as [AppError.Forbidden]; the screens that use optional extras (odds, predictions, videos) hide them.
 * - Leagues that group several divisions under one league id (e.g. England "Non League Premier": Isthmian, NPL, Southern
 *   Central, Southern South) are split into one competition per division, with ids "<leagueId>.<stageKey>".
 * - All times are requested with timezone=UTC and converted on device.
 * - The API key is never logged and never part of a cache key.
 */
class ApiFootballComApi(
    private val apiKey: String,
    baseUrl: String = "https://apiv3.apifootball.com/",
    private val client: OkHttpClient = HttpFootballApi.defaultClient(),
    private val nowMs: () -> Long = { System.currentTimeMillis() },
    private val zone: () -> ZoneId = { ZoneId.systemDefault() },
    private val maxRetries: Int = 2,
    private val retryDelayMs: Long = 400,
) : FootballApi {

    private val base: HttpUrl = baseUrl.trimEnd('/').plus("/").toHttpUrl()
    private val json = Json { ignoreUnknownKeys = true; isLenient = true }

    // ---------------- cache + transport ----------------

    private class Entry(val value: Any?, val error: Throwable?, val atMs: Long, val ttlMs: Long)
    private val cache = ConcurrentHashMap<String, Entry>()
    private val locks = ConcurrentHashMap<String, Mutex>()

    /** The API answered with an error object ({"error":404,"message":"No event found ..."}). */
    internal class ApiError(val code: Int, override val message: String) : Exception(message) {
        val noData: Boolean get() = code == 404 && message.contains("found", ignoreCase = true) && !message.contains("Authentification", true)
        val planGated: Boolean get() = code == 1005
        val badKey: Boolean get() = message.contains("Authentification failed", ignoreCase = true)
    }

    fun clearCache() = cache.clear()

    @Suppress("UNCHECKED_CAST")
    private suspend fun <T> memo(key: String, ttlMs: Long, load: suspend () -> T): T {
        fun fresh(): Entry? = cache[key]?.takeIf { nowMs() - it.atMs < it.ttlMs }
        fresh()?.let { e -> e.error?.let { throw it }; return e.value as T }
        return locks.getOrPut(key) { Mutex() }.withLock {
            fresh()?.let { e -> e.error?.let { throw it }; return@withLock e.value as T }
            try {
                load().also { cache[key] = Entry(it, null, nowMs(), ttlMs) }
            } catch (e: ApiError) {
                // Remember "no data" and "not on your plan" answers too, so a gated method costs one call per TTL, not per screen.
                if (e.noData || e.planGated) cache[key] = Entry(null, e, nowMs(), ttlMs.coerceAtLeast(MIN))
                throw e
            }
        }
    }

    /** One GET; returns the parsed JSON or throws [ApiError] / [AppError]. */
    private suspend fun call(action: String, params: Map<String, String>, ttlMs: Long): JsonElement {
        val key = action + "?" + params.entries.sortedBy { it.key }.joinToString("&") { "${it.key}=${it.value}" }
        return memo(key, ttlMs) { fetch(action, params) }
    }

    private suspend fun fetch(action: String, params: Map<String, String>): JsonElement {
        val url = base.newBuilder().addQueryParameter("action", action).apply {
            params.forEach { (k, v) -> addQueryParameter(k, v) }
        }.addQueryParameter("APIkey", apiKey).build()
        var attempt = 0
        while (true) {
            try {
                val req = Request.Builder().url(url).get().header("Accept", "application/json").build()
                val text = client.newCall(req).await().use { resp ->
                    val body = resp.body?.string().orEmpty()
                    if (!resp.isSuccessful) throw AppError.fromHttp(resp.code)
                    body
                }
                val el = try { json.parseToJsonElement(text) } catch (e: Exception) { throw AppError.Parse(e) }
                (el as? JsonObject)?.let { o ->
                    val code = o.str("error").toIntOrNull()
                    if (code != null) throw ApiError(code, o.str("message"))
                }
                return el
            } catch (e: ApiError) {
                throw e
            } catch (t: Throwable) {
                val err = AppError.from(t)
                val canRetry = attempt < maxRetries && (err is AppError.Network || err is AppError.Timeout || err is AppError.Server || err is AppError.RateLimited)
                if (!canRetry) throw err
                attempt++
                delay(retryDelayMs * attempt)
            }
        }
    }

    /** A list endpoint: "No ... found" is an empty list. */
    private suspend fun list(action: String, params: Map<String, String>, ttlMs: Long): List<JsonObject> = try {
        call(action, params, ttlMs).objects()
    } catch (e: ApiError) {
        if (e.noData) emptyList() else throw toAppError(e)
    }

    /** An object endpoint (get_H2H, get_lineups, ...); null when the API has no data. */
    private suspend fun objectOrNull(action: String, params: Map<String, String>, ttlMs: Long): JsonElement? = try {
        call(action, params, ttlMs)
    } catch (e: ApiError) {
        if (e.noData) null else throw toAppError(e)
    }

    private fun toAppError(e: ApiError): AppError = when {
        e.badKey || e.planGated -> AppError.Forbidden()
        e.code == 404 -> AppError.NotFound()
        e.code == 429 -> AppError.RateLimited()
        else -> AppError.Unknown(e)
    }

    // ---------------- dates ----------------

    private fun todayUtc(): LocalDate = Instant.ofEpochMilli(nowMs()).atZone(ZoneOffset.UTC).toLocalDate()
    private fun range(from: LocalDate, to: LocalDate) = mapOf("from" to from.toString(), "to" to to.toString(), "timezone" to "UTC")

    // ---------------- live overlay (fed by the livescore WebSocket) ----------------

    private class Live(val row: JsonObject, val atMs: Long)
    private val live = ConcurrentHashMap<String, Live>()

    /** Stores pushed event rows; newer pushes win over cached REST rows when fixtures are mapped. Returns changed ids. */
    fun applyLivePush(rows: List<JsonObject>): Set<String> {
        val now = nowMs()
        live.entries.removeIf { now - it.value.atMs > 3 * 3_600_000L }
        return rows.mapNotNull { r -> r.str("match_id").takeIf { it.isNotBlank() }?.also { live[it] = Live(r, now) } }.toSet()
    }

    /** Pushed rows can be partial; merge them over the REST row so lineups and stats are kept. */
    private fun patch(rows: List<JsonObject>): List<JsonObject> = if (live.isEmpty()) rows else rows.map { r ->
        val p = live[r.str("match_id")]?.row ?: return@map r
        JsonObject(r + p.filterValues { v -> !(v is JsonArray && v.isEmpty()) })
    }

    // ---------------- leagues and divisions ----------------

    private data class Division(
        val id: String,
        val baseId: String,
        val stageKey: String?,
        val name: String,
        val teamIds: Set<String>,
        val rows: List<JsonObject>,
    )

    private suspend fun rawLeagues() = list("get_leagues", emptyMap(), HOUR)
    private suspend fun rawStandings(baseId: String) = list("get_standings", mapOf("league_id" to baseId), TEN_MIN)

    /** About two months back and one forward: enough to place every team of a division and to build form. */
    private suspend fun leagueWindow(baseId: String): List<JsonObject> =
        patch(list("get_events", range(todayUtc().minusDays(60), todayUtc().plusDays(30)) + ("league_id" to baseId), FIVE_MIN))

    private suspend fun divisions(baseId: String): List<Division> = memo("div:$baseId", TEN_MIN) {
        val rows = rawStandings(baseId)
        val groups = rows.groupBy { it.str("fk_stage_key") + "|" + it.str("stage_name") }
        if (groups.size <= 1) return@memo emptyList<Division>()
        val events = runCatching { leagueWindow(baseId) }.getOrDefault(emptyList())
        val stages = events.groupBy { it.str("fk_stage_key") }.mapValues { (_, es) ->
            es.first().str("stage_name") to es.flatMap { listOf(it.str("match_hometeam_id"), it.str("match_awayteam_id")) }.toSet()
        }
        val taken = mutableSetOf<String>()
        groups.values.mapIndexed { i, g ->
            val teams = g.map { it.str("team_id") }.toSet()
            val best = stages.filterKeys { it !in taken && it.isNotBlank() }
                .maxByOrNull { (_, v) -> v.second.intersect(teams).size }
                ?.takeIf { (_, v) -> v.second.intersect(teams).isNotEmpty() }
            best?.let { taken += it.key }
            val stageName = g.first().str("stage_name").takeIf { it.isNotBlank() && !it.equals("Current", true) }
            val name = best?.value?.first?.takeIf { it.isNotBlank() } ?: stageName ?: "Group ${i + 1}"
            val key = best?.key ?: "g${i + 1}"
            Division("$baseId.$key", baseId, best?.key, name, teams, g.sortedBy { it.str("overall_league_position").toIntOrNull() ?: 99 })
        }.sortedBy { it.name }
    }

    private fun baseOf(id: String) = id.substringBefore('.')

    private suspend fun division(id: String): Division? {
        if (!id.contains('.')) return null
        return divisions(baseOf(id)).firstOrNull { it.id == id }
    }

    /** The competition id a fixture belongs to: its division when the league is split, else the API league id. */
    private suspend fun competitionIdFor(e: JsonObject): String {
        val baseId = e.str("league_id")
        val divs = runCatching { divisions(baseId) }.getOrDefault(emptyList())
        if (divs.isEmpty()) return baseId
        val stage = e.str("fk_stage_key")
        divs.firstOrNull { it.stageKey != null && it.stageKey == stage }?.let { return it.id }
        val home = e.str("match_hometeam_id")
        return divs.firstOrNull { home in it.teamIds }?.id ?: baseId
    }

    private suspend fun mapFixtures(rows: List<JsonObject>, predictions: Map<String, PredictionDto> = emptyMap()): List<FixtureDto> =
        patch(rows).map { ApiFootballParse.fixture(it, competitionIdFor(it), predictions[it.str("match_id")]) }

    private suspend fun predictionsFor(from: LocalDate, to: LocalDate): Map<String, PredictionDto> = runCatching {
        list("get_predictions", range(from, to), HALF_HOUR).mapNotNull { p -> ApiFootballParse.prediction(p)?.let { p.str("match_id") to it } }.toMap()
    }.getOrDefault(emptyMap())

    private fun roundOf(rows: List<JsonObject>): String = rows
        .filter { ApiFootballParse.status(it.str("match_status")).status == "finished" }
        .maxByOrNull { ApiFootballParse.kickoffMs(it.str("match_date"), it.str("match_time")) }
        ?.str("match_round")?.let { r -> if (r.toIntOrNull() != null) "Round $r" else r }.orEmpty()

    override suspend fun leagues(): List<LeagueDto> {
        val raw = rawLeagues()
        val countryOrder = runCatching { list("get_countries", emptyMap(), DAY) }.getOrDefault(emptyList())
            .mapIndexed { i, c -> c.str("country_id") to (i to c.str("country_name")) }.toMap()
        return raw.sortedBy { countryOrder[it.str("country_id")]?.first ?: Int.MAX_VALUE }.flatMap { l ->
            val baseId = l.str("league_id")
            val country = l.str("country_name").ifBlank { countryOrder[l.str("country_id")]?.second.orEmpty() }
            val season = l.str("league_season")
            val divs = runCatching { divisions(baseId) }.getOrDefault(emptyList())
            val events = runCatching { leagueWindow(baseId) }.getOrDefault(emptyList())
            if (divs.isEmpty()) {
                val teams = runCatching { rawStandings(baseId).size }.getOrDefault(0)
                listOf(LeagueDto(baseId, l.str("league_name"), null, country, teams, season, roundOf(events)))
            } else divs.map { d ->
                val dEvents = events.filter { (d.stageKey != null && it.str("fk_stage_key") == d.stageKey) || (d.stageKey == null && it.str("match_hometeam_id") in d.teamIds) }
                LeagueDto(d.id, d.name, null, country, d.teamIds.size, season, roundOf(dEvents))
            }
        }
    }

    override suspend fun league(id: String): LeagueDto = leagues().firstOrNull { it.id == id } ?: throw AppError.NotFound("This competition")

    override suspend fun standings(leagueId: String): List<StandingRowDto> {
        val baseId = baseOf(leagueId)
        val div = division(leagueId)
        val rows = div?.rows ?: divisions(baseId).firstOrNull()?.rows ?: rawStandings(baseId)
        val fixtures = runCatching { leagueWindow(baseId).map { ApiFootballParse.fixture(it) } }.getOrDefault(emptyList())
        return rows.map { r -> ApiFootballParse.standingRow(r, leagueId, ApiFootballParse.form(r.str("team_id"), fixtures).ifBlank { null }) }
            .sortedBy { it.rank }
    }

    override suspend fun leagueFixtures(leagueId: String): List<FixtureDto> {
        val baseId = baseOf(leagueId)
        val div = division(leagueId)
        val rows = leagueWindow(baseId).filter { e ->
            div == null || (div.stageKey != null && e.str("fk_stage_key") == div.stageKey) || (div.stageKey == null && e.str("match_hometeam_id") in div.teamIds)
        }
        return mapFixtures(rows).sortedBy { it.kickoffMs }
    }

    /** Season start from "2026/2027" (1 July 2026) or "2026" (1 January 2026); about six months back otherwise. */
    private suspend fun seasonStart(baseId: String): LocalDate {
        val season = runCatching { rawLeagues().firstOrNull { it.str("league_id") == baseId }?.str("league_season") }.getOrNull().orEmpty()
        val year = season.take(4).toIntOrNull() ?: return todayUtc().minusDays(180)
        return if ('/' in season) LocalDate.of(year, 7, 1) else LocalDate.of(year, 1, 1)
    }

    override suspend fun scorers(leagueId: String): List<ScorerDto> {
        val baseId = baseOf(leagueId)
        val div = division(leagueId)
        val official = runCatching { list("get_topscorers", mapOf("league_id" to baseId), HALF_HOUR) }.getOrDefault(emptyList())
            .map { ApiFootballParse.topScorer(it, leagueId) }
            .filter { div == null || it.team.id in div.teamIds }
        if (official.isNotEmpty()) return official.sortedBy { it.rank }.mapIndexed { i, s -> s.copy(rank = i + 1) }.take(20)
        // get_topscorers is not on every plan: count goals from this season's results instead.
        val season = runCatching {
            list("get_events", range(seasonStart(baseId), todayUtc()) + ("league_id" to baseId), HALF_HOUR)
        }.getOrDefault(emptyList()).filter { e ->
            div == null || (div.stageKey != null && e.str("fk_stage_key") == div.stageKey) || (div.stageKey == null && e.str("match_hometeam_id") in div.teamIds)
        }
        return ApiFootballParse.scorersFromEvents(season, leagueId)
    }

    // ---------------- fixtures ----------------

    override suspend fun fixtures(date: String): List<FixtureDto> {
        val day = LocalDate.parse(date)
        val z = zone()
        val start = day.atStartOfDay(z).toInstant().toEpochMilli()
        val end = day.plusDays(1).atStartOfDay(z).toInstant().toEpochMilli()
        val from = Instant.ofEpochMilli(start).atZone(ZoneOffset.UTC).toLocalDate()
        val to = Instant.ofEpochMilli(end - 1).atZone(ZoneOffset.UTC).toLocalDate()
        val today = todayUtc()
        val ttl = if (!today.isBefore(from) && !today.isAfter(to)) MINUTE else TEN_MIN
        val rows = list("get_events", range(from, to), ttl)
        return mapFixtures(rows, predictionsFor(from, to)).filter { it.kickoffMs in start until end }.sortedBy { it.kickoffMs }
    }

    override suspend fun liveFixtures(): List<FixtureDto> {
        val rows = list("get_events", mapOf("match_live" to "1", "timezone" to "UTC"), HALF_MINUTE)
        val ids = rows.map { it.str("match_id") }.toSet()
        // Matches the socket reported live that the last REST snapshot did not have yet.
        val pushed = live.values.map { it.row }.filter { it.str("match_id") !in ids && it.str("match_live") == "1" }
        return mapFixtures(rows + pushed).filter { it.status == "live" }
    }

    private suspend fun eventRow(id: String): JsonObject =
        patch(list("get_events", mapOf("match_id" to id, "timezone" to "UTC"), MINUTE)).firstOrNull() ?: throw AppError.NotFound("This match")

    override suspend fun fixture(id: String): FixtureDto {
        val e = eventRow(id)
        val pred = runCatching { prediction(id) }.getOrNull()
        return ApiFootballParse.fixture(e, competitionIdFor(e), pred)
    }

    override suspend fun events(fixtureId: String): List<EventDto> = ApiFootballParse.timeline(eventRow(fixtureId))

    override suspend fun stats(fixtureId: String): List<StatLineDto> {
        val e = eventRow(fixtureId)
        val inline = e.arr("statistics")
        if (inline.isNotEmpty()) return ApiFootballParse.stats(inline)
        if (ApiFootballParse.status(e.str("match_status")).status == "upcoming") return emptyList()
        // get_events can lag behind; get_statistics is the dedicated source.
        val root = runCatching { objectOrNull("get_statistics", mapOf("match_id" to fixtureId), TEN_MIN) }.getOrNull() as? JsonObject
        return ApiFootballParse.stats((root?.get(fixtureId) as? JsonObject)?.arr("statistics").orEmpty())
    }

    override suspend fun lineups(fixtureId: String): LineupsDto {
        val e = eventRow(fixtureId)
        var lineup = e.obj("lineup")
        val empty = lineup == null || listOf("home", "away").all { lineup?.obj(it)?.arr("starting_lineups").isNullOrEmpty() }
        if (empty) {
            val root = runCatching { objectOrNull("get_lineups", mapOf("match_id" to fixtureId), TEN_MIN) }.getOrNull() as? JsonObject
            (root?.get(fixtureId) as? JsonObject)?.obj("lineup")?.let { lineup = it }
        }
        return LineupsDto(
            listOf(
                ApiFootballParse.lineup(lineup?.obj("home"), e.str("match_hometeam_id"), e.str("match_hometeam_system")),
                ApiFootballParse.lineup(lineup?.obj("away"), e.str("match_awayteam_id"), e.str("match_awayteam_system")),
            ),
        )
    }

    override suspend fun prediction(fixtureId: String): PredictionDto? =
        list("get_predictions", mapOf("match_id" to fixtureId, "timezone" to "UTC"), HALF_HOUR).firstOrNull()?.let(ApiFootballParse::prediction)

    override suspend fun headToHead(fixtureId: String): List<FixtureDto> {
        val e = eventRow(fixtureId)
        val root = objectOrNull(
            "get_H2H",
            mapOf("firstTeamId" to e.str("match_hometeam_id"), "secondTeamId" to e.str("match_awayteam_id"), "timezone" to "UTC"),
            HOUR,
        ) as? JsonObject ?: return emptyList()
        return root.arr("firstTeam_VS_secondTeam").map { ApiFootballParse.fixture(it) }
            .filter { it.id != fixtureId && it.status == "finished" }
            .sortedByDescending { it.kickoffMs }
            .take(6)
    }

    override suspend fun odds(fixtureId: String): OddsDto? =
        ApiFootballParse.odds(list("get_odds", mapOf("match_id" to fixtureId), HALF_HOUR))

    override suspend fun liveCommentary(fixtureId: String): LiveDataDto? {
        // The action name really is spelled "commnets" in the API.
        val root = objectOrNull("get_live_odds_commnets", mapOf("match_id" to fixtureId), HALF_MINUTE) ?: return null
        return ApiFootballParse.liveData(root, fixtureId)
    }

    override suspend fun videos(fixtureId: String): List<VideoDto> =
        ApiFootballParse.videos(list("get_videos", mapOf("match_id" to fixtureId), HOUR))

    // ---------------- teams and players ----------------

    /** Every team the plan covers, with the competition it plays in (from the standings). */
    private suspend fun allTeams(): List<TeamRefDto> = memo("allTeams", TEN_MIN) {
        leagues().flatMap { l -> runCatching { standings(l.id) }.getOrDefault(emptyList()).map { it.team } }.distinctBy { it.id }
    }

    override suspend fun popularTeams(): List<TeamRefDto> =
        leagues().flatMap { l -> runCatching { standings(l.id).take(3).map { it.team } }.getOrDefault(emptyList()) }.distinctBy { it.id }.take(12)

    override suspend fun team(id: String): TeamDto {
        val t = list("get_teams", mapOf("team_id" to id), HOUR).firstOrNull() ?: throw AppError.NotFound("This team")
        val ref = runCatching { allTeams().firstOrNull { it.id == id } }.getOrNull()
        val leagueName = ref?.leagueId?.let { lid -> runCatching { leagues().firstOrNull { it.id == lid }?.name }.getOrNull() }.orEmpty()
        return ApiFootballParse.team(t, ref?.leagueId, leagueName)
    }

    override suspend fun teamFixtures(teamId: String): List<FixtureDto> =
        mapFixtures(list("get_events", range(todayUtc().minusDays(120), todayUtc().plusDays(45)) + ("team_id" to teamId), FIVE_MIN)).sortedBy { it.kickoffMs }

    override suspend fun player(id: String): PlayerDto {
        val rows = list("get_players", mapOf("player_id" to id), HOUR)
        val leagueId = rows.firstOrNull()?.str("team_key")?.let { tk -> runCatching { allTeams().firstOrNull { it.id == tk }?.leagueId }.getOrNull() }
        return ApiFootballParse.player(rows, leagueId) ?: throw AppError.NotFound("This player")
    }

    override suspend fun topPlayers(): List<PlayerSummaryDto> =
        leagues().flatMap { l -> runCatching { scorers(l.id).take(3) }.getOrDefault(emptyList()) }
            .sortedWith(compareByDescending<ScorerDto> { it.goals }.thenByDescending { it.assists })
            .distinctBy { it.playerId }
            .take(10)
            .map { s -> PlayerSummaryDto(s.playerId, s.name, s.team, position = "", number = 0) }

    // ---------------- news (not from this API) ----------------

    /** News comes from the 90min RSS feeds (see NinetyMinNewsSource); this API's get_news is not used. */
    override suspend fun news(category: String?, cursor: String?): Page<NewsDto> = Page(emptyList(), null)

    override suspend fun article(id: String): NewsDto = throw AppError.NotFound("This story")

    // ---------------- search and ask ----------------

    override suspend fun search(query: String): SearchDto {
        val q = query.trim()
        if (q.length < 2) return SearchDto()
        val leagues = runCatching { leagues() }.getOrDefault(emptyList())
        val teams = runCatching { allTeams() }.getOrDefault(emptyList()).filter { it.name.contains(q, true) }
        val players = runCatching { list("get_players", mapOf("player_name" to q), TEN_MIN) }.getOrDefault(emptyList())
            .map { ApiFootballParse.playerSummary(it) }.filter { it.id.isNotBlank() && it.id != "0" && it.name.isNotBlank() }.distinctBy { it.id }.take(20)
        return SearchDto(
            teams = teams.take(20),
            leagues = leagues.filter { it.name.contains(q, true) || it.country.contains(q, true) },
            players = players,
        )
    }

    /**
     * Answers from live standings and scorers only (no language model): table leaders, a named team's position and form,
     * and top scorers. Anything else gets an honest "can't answer that yet".
     */
    override suspend fun ask(request: AskRequest): AskResponse {
        val q = request.question.lowercase()
        val leagues = leagues()
        val tables = leagues.associateWith { l -> runCatching { standings(l.id) }.getOrDefault(emptyList()) }.filterValues { it.isNotEmpty() }
        val named = tables.flatMap { (l, rows) -> rows.map { l to it } }
            .filter { (_, r) -> r.team.name.length >= 3 && q.contains(r.team.name.lowercase()) }
            .maxByOrNull { (_, r) -> r.team.name.length }
        val answer = when {
            named != null -> {
                val (l, r) = named
                val form = r.form?.takeIf { it.isNotBlank() }?.let { ", recent form $it" }.orEmpty()
                "${r.team.name} are ${ordinal(r.rank)} in ${l.name} on ${r.points} points from ${r.played} games (${r.won}W ${r.drawn}D ${r.lost}L, goal difference ${signed(r.goalDiff)})$form."
            }
            "score" in q || "goal" in q -> {
                val top = leagues.mapNotNull { l -> runCatching { scorers(l.id).firstOrNull() }.getOrNull()?.let { l to it } }
                if (top.isEmpty()) "No scorer data is available for your competitions yet."
                else top.joinToString(" ") { (l, s) -> "${s.name} (${s.team.name}) leads ${l.name} with ${s.goals} goals." }
            }
            "form" in q -> {
                val best = tables.flatMap { (l, rows) -> rows.map { l to it } }.maxByOrNull { (_, r) -> r.form.orEmpty().count { it == 'W' } * 3 + r.form.orEmpty().count { it == 'D' } }
                if (best == null || best.second.form.isNullOrBlank()) "There are no recent results to judge form yet."
                else "${best.second.team.name} are in the best form in ${best.first.name}: ${best.second.form}."
            }
            tables.isNotEmpty() && ("table" in q || "top" in q || "lead" in q || "first" in q) ->
                tables.entries.joinToString(" ") { (l, rows) ->
                    val a = rows[0]; val b = rows.getOrNull(1)
                    "${a.team.name} lead ${l.name} on ${a.points} points" + (b?.let { ", ${a.points - it.points} clear of ${it.team.name}." } ?: ".")
                }
            else -> "I can answer from live tables and results: try \"Who leads the table?\", \"Who is the top scorer?\" or a team's name."
        }
        return AskResponse(answer)
    }

    private fun ordinal(n: Int): String = n.toString() + when {
        n % 100 in 11..13 -> "th"
        n % 10 == 1 -> "st"
        n % 10 == 2 -> "nd"
        n % 10 == 3 -> "rd"
        else -> "th"
    }

    private fun signed(n: Int) = if (n > 0) "+$n" else n.toString()

    companion object {
        private const val HALF_MINUTE = 30_000L
        private const val MINUTE = 60_000L
        private const val MIN = MINUTE
        private const val FIVE_MIN = 5 * MINUTE
        private const val TEN_MIN = 10 * MINUTE
        private const val HALF_HOUR = 30 * MINUTE
        private const val HOUR = 60 * MINUTE
        private const val DAY = 24 * HOUR
    }
}
