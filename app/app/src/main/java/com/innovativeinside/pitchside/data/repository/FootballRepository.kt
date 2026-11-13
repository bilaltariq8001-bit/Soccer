package com.innovativeinside.pitchside.data.repository

import com.innovativeinside.pitchside.core.AppClock
import com.innovativeinside.pitchside.core.AppError
import com.innovativeinside.pitchside.core.DispatcherProvider
import com.innovativeinside.pitchside.data.model.League
import com.innovativeinside.pitchside.data.model.Match
import com.innovativeinside.pitchside.data.model.MatchEvent
import com.innovativeinside.pitchside.data.model.MatchLineups
import com.innovativeinside.pitchside.data.model.NewsCategory
import com.innovativeinside.pitchside.data.model.NewsEdition
import com.innovativeinside.pitchside.data.model.NewsItem
import com.innovativeinside.pitchside.data.model.PlayerProfile
import com.innovativeinside.pitchside.data.model.PlayerSummary
import com.innovativeinside.pitchside.data.model.Scorer
import com.innovativeinside.pitchside.data.model.SearchResults
import com.innovativeinside.pitchside.data.model.StandingRow
import com.innovativeinside.pitchside.data.model.StatRow
import com.innovativeinside.pitchside.data.model.Team
import com.innovativeinside.pitchside.data.model.TeamDetail
import com.innovativeinside.pitchside.data.remote.FootballApi
import com.innovativeinside.pitchside.data.remote.Mappers
import com.innovativeinside.pitchside.data.news.NewsFeedSource
import com.innovativeinside.pitchside.data.remote.dto.AskRequest
import java.time.Instant
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.concurrent.ConcurrentHashMap
import kotlin.coroutines.cancellation.CancellationException
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.withContext

data class MatchDetail(
    val match: Match,
    val events: List<MatchEvent>,
    val stats: List<StatRow>,
    val lineups: MatchLineups?,
    val table: List<StandingRow>,
)

data class LeagueDetail(val league: League, val standings: List<StandingRow>, val fixtures: List<Match>, val scorers: List<Scorer>)

data class TeamPage(val detail: TeamDetail, val fixtures: List<Match>, val standings: List<StandingRow>)

data class NewsPage(val items: List<NewsItem>, val nextCursor: String?)

/**
 * The single seam between UI and data. ViewModels only talk to this. Every call returns a [Result] whose
 * failure is an [AppError]; nothing here throws, except coroutine cancellation.
 */
interface FootballRepository {
    suspend fun matchesOn(date: LocalDate, refresh: Boolean = false): Result<List<Match>>
    suspend fun liveMatches(refresh: Boolean = false): Result<List<Match>>
    suspend fun matchDetail(id: String, refresh: Boolean = false): Result<MatchDetail>
    suspend fun leagues(refresh: Boolean = false): Result<List<League>>
    suspend fun leagueDetail(id: String, refresh: Boolean = false): Result<LeagueDetail>
    suspend fun standings(leagueId: String, refresh: Boolean = false): Result<List<StandingRow>>
    suspend fun popularTeams(refresh: Boolean = false): Result<List<Team>>
    suspend fun teamPage(id: String, refresh: Boolean = false): Result<TeamPage>
    suspend fun player(id: String, refresh: Boolean = false): Result<PlayerProfile>
    suspend fun topPlayers(refresh: Boolean = false): Result<List<PlayerSummary>>
    suspend fun news(category: NewsCategory?, cursor: String? = null): Result<NewsPage>
    /**
     * Headlines from the news feed source (90min RSS), newest first. [edition] null merges every edition.
     * Paged client-side: [cursor] is the offset returned as [NewsPage.nextCursor].
     */
    suspend fun headlines(edition: NewsEdition?, cursor: String? = null, refresh: Boolean = false): Result<NewsPage>
    suspend fun article(id: String): Result<NewsItem>
    suspend fun search(query: String): Result<SearchResults>
    suspend fun ask(question: String, language: String): Result<String>
}

/**
 * Default implementation over a [FootballApi] (mock or HTTP). Keeps a small in-memory cache with a TTL
 * so tab switches and back navigation are instant; pass refresh = true for pull-to-refresh and retry.
 */
class DefaultFootballRepository(
    private val api: FootballApi,
    private val dispatchers: DispatcherProvider,
    private val clock: AppClock,
    private val ttlMs: Long = 60_000,
    private val liveTtlMs: Long = 20_000,
    /** Where headlines come from. Null falls back to the Pitchside API's /v1/news (mock data). */
    private val newsSource: NewsFeedSource? = null,
    private val newsTtlMs: Long = 10 * 60_000,
    private val newsPageSize: Int = 20,
) : FootballRepository {

    /** Every feed story seen this session, so the article screen opens without another download. */
    private val stories = ConcurrentHashMap<String, NewsItem>()

    private data class Entry(val value: Any, val atMs: Long)
    private val cache = ConcurrentHashMap<String, Entry>()

    private val timeFmt = DateTimeFormatter.ofPattern("HH:mm")
    private fun hhmm(ms: Long): String = Instant.ofEpochMilli(ms).atZone(clock.zone()).toLocalTime().format(timeFmt)
    private fun match(d: com.innovativeinside.pitchside.data.remote.dto.FixtureDto) = Mappers.match(d, ::hhmm)

    @Suppress("UNCHECKED_CAST")
    private suspend fun <T : Any> cached(key: String, refresh: Boolean, ttl: Long = ttlMs, load: suspend () -> T): Result<T> {
        val hit = cache[key]
        if (!refresh && hit != null && clock.nowMs() - hit.atMs < ttl) return Result.success(hit.value as T)
        return safe { load().also { cache[key] = Entry(it, clock.nowMs()) } }
    }

    private suspend fun <T> safe(block: suspend () -> T): Result<T> = withContext(dispatchers.io) {
        try {
            Result.success(block())
        } catch (c: CancellationException) {
            throw c
        } catch (t: Throwable) {
            Result.failure(AppError.from(t))
        }
    }

    fun clearCache() = cache.clear()

    override suspend fun matchesOn(date: LocalDate, refresh: Boolean) =
        cached("fixtures:$date", refresh, if (date == clock.today()) liveTtlMs else ttlMs) { api.fixtures(date.toString()).map(::match) }

    override suspend fun liveMatches(refresh: Boolean) = cached("live", refresh, liveTtlMs) { api.liveFixtures().map(::match) }

    override suspend fun matchDetail(id: String, refresh: Boolean) = cached("match:$id", refresh, liveTtlMs) {
        coroutineScope {
            val fixture = match(api.fixture(id))
            val events = async { api.events(id) }
            val stats = async { api.stats(id) }
            val lineups = async { api.lineups(id) }
            // The table is a nice-to-have on the match page: a failure there must not fail the whole page.
            val table = async { runCatching { api.standings(fixture.leagueId) }.getOrDefault(emptyList()) }
            val ev = Mappers.events(events.await(), fixture.home.id)
            val scorers = ev.filter { it.kind == com.innovativeinside.pitchside.data.model.EventKind.Goal }.map { it.player }.toSet()
            MatchDetail(
                match = fixture,
                events = ev,
                stats = stats.await().map(Mappers::stat),
                lineups = Mappers.lineups(lineups.await(), fixture.home.id, fixture.away.id, scorers),
                table = table.await().map(Mappers::standing),
            )
        }
    }

    override suspend fun leagues(refresh: Boolean) = cached("leagues", refresh) { api.leagues().map(Mappers::league) }

    override suspend fun leagueDetail(id: String, refresh: Boolean) = cached("league:$id", refresh) {
        coroutineScope {
            val league = async { Mappers.league(api.league(id)) }
            val table = async { api.standings(id).map(Mappers::standing) }
            val fixtures = async { api.leagueFixtures(id).map(::match) }
            val scorers = async { api.scorers(id).map(Mappers::scorer) }
            LeagueDetail(league.await(), table.await(), fixtures.await(), scorers.await())
        }
    }

    override suspend fun standings(leagueId: String, refresh: Boolean) = cached("standings:$leagueId", refresh) { api.standings(leagueId).map(Mappers::standing) }

    override suspend fun popularTeams(refresh: Boolean) = cached("popularTeams", refresh) { api.popularTeams().map(Mappers::team) }

    override suspend fun teamPage(id: String, refresh: Boolean) = cached("team:$id", refresh) {
        coroutineScope {
            val detail = Mappers.team(api.team(id))
            val fixtures = async { api.teamFixtures(id).map(::match) }
            val table = async { runCatching { api.standings(detail.team.leagueId).map(Mappers::standing) }.getOrDefault(emptyList()) }
            TeamPage(detail, fixtures.await(), table.await())
        }
    }

    override suspend fun player(id: String, refresh: Boolean) = cached("player:$id", refresh) { Mappers.player(api.player(id)) }

    override suspend fun topPlayers(refresh: Boolean) = cached("topPlayers", refresh) { api.topPlayers().map(Mappers::playerSummary) }

    override suspend fun news(category: NewsCategory?, cursor: String?) = safe {
        val key = when (category) { NewsCategory.Reports -> "reports"; NewsCategory.Transfers -> "transfers"; NewsCategory.Analysis -> "analysis"; null -> null }
        val page = api.news(key, cursor)
        NewsPage(page.items.map(Mappers::news), page.nextCursor)
    }

    private suspend fun edition(e: NewsEdition, refresh: Boolean): Result<List<NewsItem>> {
        val src = newsSource ?: return Result.success(emptyList())
        return cached("feed:${e.code}", refresh, newsTtlMs) { src.fetch(e).also { list -> list.forEach { stories[it.id] = it } } }
    }

    override suspend fun headlines(edition: NewsEdition?, cursor: String?, refresh: Boolean): Result<NewsPage> {
        if (newsSource == null) return news(null, cursor)
        val all: Result<List<NewsItem>> = if (edition != null) {
            edition(edition, refresh)
        } else {
            // One edition failing (or being slow to recover) should not blank the "All" view.
            val results = coroutineScope { NewsEdition.entries.map { e -> async { edition(e, refresh) } }.map { it.await() } }
            val ok = results.filter { it.isSuccess }
            if (ok.isEmpty()) Result.failure(results.first().exceptionOrNull() ?: AppError.Unknown())
            else Result.success(ok.flatMap { it.getOrThrow() }.sortedByDescending { it.publishedMs })
        }
        return all.map { list ->
            val start = cursor?.toIntOrNull()?.coerceIn(0, list.size) ?: 0
            val end = (start + newsPageSize).coerceAtMost(list.size)
            NewsPage(list.subList(start, end).toList(), if (end < list.size) end.toString() else null)
        }
    }

    override suspend fun article(id: String): Result<NewsItem> {
        stories[id]?.let { return Result.success(it) }
        // Feed ids are "<edition>-<guid>": after process death, reload that edition and look again.
        val edition = NewsEdition.fromCode(id.substringBefore('-', ""))
        if (newsSource != null && edition != null) {
            return edition(edition, refresh = false).fold(
                { list -> list.firstOrNull { it.id == id }?.let { Result.success(it) } ?: Result.failure(AppError.NotFound("This story")) },
                { Result.failure(it) },
            )
        }
        return cached("article:$id", false) { Mappers.news(api.article(id)) }
    }

    override suspend fun search(query: String): Result<SearchResults> {
        val q = query.trim()
        if (q.length < 2) return Result.success(SearchResults(emptyList(), emptyList(), emptyList()))
        return safe { Mappers.search(api.search(q)) }
    }

    override suspend fun ask(question: String, language: String): Result<String> {
        val q = question.trim()
        if (q.isEmpty()) return Result.failure(AppError.Validation("Type a question first."))
        if (q.length > 300) return Result.failure(AppError.Validation("Keep questions under 300 characters."))
        return safe { api.ask(AskRequest(q, language)).answer }
    }
}
