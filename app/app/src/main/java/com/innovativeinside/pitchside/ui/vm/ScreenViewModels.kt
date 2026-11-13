package com.innovativeinside.pitchside.ui.vm

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import com.innovativeinside.pitchside.AppContainer
import com.innovativeinside.pitchside.core.AppError
import com.innovativeinside.pitchside.data.model.League
import com.innovativeinside.pitchside.data.model.Match
import com.innovativeinside.pitchside.data.model.NewsItem
import com.innovativeinside.pitchside.data.model.PlayerProfile
import com.innovativeinside.pitchside.data.model.PlayerSummary
import com.innovativeinside.pitchside.data.model.Scorer
import com.innovativeinside.pitchside.data.model.StandingRow
import com.innovativeinside.pitchside.data.model.Status
import com.innovativeinside.pitchside.data.model.Team
import com.innovativeinside.pitchside.data.repository.FootballRepository
import com.innovativeinside.pitchside.data.repository.LeagueDetail
import com.innovativeinside.pitchside.data.repository.MatchDetail
import com.innovativeinside.pitchside.data.repository.TeamPage
import java.time.LocalDate
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

private const val LIVE_POLL_MS = 30_000L

private fun SavedStateHandle.requireId(): String = get<String>("id")?.takeIf { it.isNotBlank() } ?: ""

private fun missingId(): Result<Nothing> = Result.failure(AppError.NotFound("This page"))

// ---------------- Home ----------------

data class HomeData(
    val matches: List<Match>,
    val news: List<NewsItem>,
    val table: List<StandingRow>,
    val leagues: List<League>,
    /** League the standings/top-scorer tiles come from (a followed league with a table, else the main one). */
    val tableLeague: League? = null,
    val scorers: List<Scorer> = emptyList(),
    /** Upcoming fixtures today and the next few days, for the next-kickoff tile. */
    val upcoming: List<Match> = emptyList(),
) {
    val live: List<Match> get() = matches.filter { it.status == Status.Live }
}

const val MAIN_LEAGUE = "pd"

class HomeViewModel(
    private val repo: FootballRepository,
    /** Followed league ids, in the user's order; the first one with a table feeds the standings tile. */
    private val followedLeagues: suspend () -> List<String> = { emptyList() },
    private val today: () -> LocalDate,
) : LoadViewModel<HomeData>() {
    init { load() }

    override suspend fun fetch(refresh: Boolean): Result<HomeData> = coroutineScope {
        val matches = async { repo.matchesOn(today(), refresh) }
        val news = async { repo.headlines(null, refresh = refresh) }
        val leagues = async { repo.leagues(refresh) }
        val ahead = (1..3L).map { d -> async { repo.matchesOn(today().plusDays(d), refresh).getOrDefault(emptyList()) } }
        val table = async {
            val candidates = (followedLeagues() + MAIN_LEAGUE).distinct().take(3)
            candidates.firstNotNullOfOrNull { id -> repo.leagueDetail(id, refresh).getOrNull()?.takeIf { it.standings.isNotEmpty() } }
        }
        // Matches are the point of the page: if they fail, the page fails. The rest degrade to empty.
        matches.await().map { m ->
            val detail = table.await()
            HomeData(
                matches = m,
                news = news.await().getOrNull()?.items.orEmpty().take(3),
                table = detail?.standings.orEmpty(),
                leagues = leagues.await().getOrDefault(emptyList()),
                tableLeague = detail?.league,
                scorers = detail?.scorers.orEmpty(),
                upcoming = (m + ahead.flatMap { it.await() }).filter { it.status == Status.Upcoming }.sortedBy { it.kickoffMs },
            )
        }
    }

    override fun isEmpty(data: HomeData) = false // Home always has something to show (empty matches has its own card).
    override fun pollIntervalMs(data: HomeData) = if (data.live.isNotEmpty()) LIVE_POLL_MS else null

    private val _heroStats = MutableStateFlow<Map<String, List<com.innovativeinside.pitchside.data.model.StatRow>>>(emptyMap())
    /** Possession/shots for the hero tile's stat strip, loaded for whichever live match is the hero. */
    val heroStats: StateFlow<Map<String, List<com.innovativeinside.pitchside.data.model.StatRow>>> = _heroStats.asStateFlow()

    fun loadStats(matchId: String) {
        viewModelScope.launch {
            repo.matchDetail(matchId).onSuccess { d -> _heroStats.value = _heroStats.value + (matchId to d.stats) }
        }
    }

    companion object {
        val Factory = containerFactory { c, _ -> HomeViewModel(c.football, { c.prefs.prefs.first().leagues }) { c.clock.today() } }
    }
}

// ---------------- Live hero selection (pure, unit tested) ----------------

/**
 * Orders live matches for the Home hero: a followed team's match first, then the closest game
 * (smallest goal gap), then the most active (more goals), then the earliest kickoff.
 */
fun rankLiveMatches(live: List<Match>, followedTeams: Collection<String>): List<Match> =
    live.filter { it.status == Status.Live }.sortedWith(
        compareBy<Match>(
            { if (it.involves(followedTeams)) 0 else 1 },
            { if (it.hasScore) kotlin.math.abs(it.homeScore!! - it.awayScore!!) else 0 },
            { -((it.homeScore ?: 0) + (it.awayScore ?: 0)) },
            { it.kickoffMs },
        ),
    )

/** The hero is the match the user promoted (while it is still live), otherwise the top-ranked one. */
fun pickHero(live: List<Match>, followedTeams: Collection<String>, promotedId: String?): Match? =
    live.firstOrNull { it.id == promotedId && it.status == Status.Live } ?: rankLiveMatches(live, followedTeams).firstOrNull()

/** Next upcoming kickoff for a followed team, or the next kickoff of any match when nothing is followed. */
fun nextKickoff(upcoming: List<Match>, followedTeams: Collection<String>, nowMs: Long): Match? {
    val future = upcoming.filter { it.status == Status.Upcoming && it.kickoffMs >= nowMs }.sortedBy { it.kickoffMs }
    return if (followedTeams.isNotEmpty()) future.firstOrNull { it.involves(followedTeams) } else future.firstOrNull()
}

/** How far a live match is through regulation time, from its badge label ("74'", "45+2'", "HT"). */
fun liveProgress(label: String): Float? {
    val l = label.trim()
    if (l.equals("HT", true)) return 0.5f
    val minute = Regex("^(\\d+)").find(l)?.groupValues?.get(1)?.toIntOrNull() ?: return null
    return (minute / 90f).coerceIn(0f, 1f)
}

// ---------------- Matches ----------------

enum class MatchFilter { All, Live, MyTeams, Upcoming, Finished }

/** Pure filter used by the Matches screen and Home, unit tested. Postponed games sit with Upcoming. */
fun filterMatches(list: List<Match>, filter: MatchFilter, followedTeams: Collection<String>, followedLeagues: Collection<String>): List<Match> = when (filter) {
    MatchFilter.All -> list
    MatchFilter.Live -> list.filter { it.status == Status.Live }
    MatchFilter.MyTeams -> list.filter { it.involves(followedTeams) || it.leagueId in followedLeagues }
    MatchFilter.Upcoming -> list.filter { it.status == Status.Upcoming || it.status == Status.Postponed }
    MatchFilter.Finished -> list.filter { it.status == Status.Finished }
}

/** Competition chip filter shared by Home and the Matches tab. Null means every competition. */
fun filterByCompetition(list: List<Match>, leagueId: String?): List<Match> = if (leagueId == null) list else list.filter { it.leagueId == leagueId }

/** Competitions present in a list of matches, as (id, name), in first-seen order with followed leagues first. */
fun competitionsOf(list: List<Match>, followedLeagues: Collection<String>): List<Pair<String, String>> =
    list.map { it.leagueId to it.leagueName }.distinct().sortedBy { if (it.first in followedLeagues) 0 else 1 }

/** Counts per segment for the Live / Upcoming / Finished control. */
fun statusCounts(list: List<Match>): Map<MatchFilter, Int> = mapOf(
    MatchFilter.Live to list.count { it.status == Status.Live },
    MatchFilter.Upcoming to list.count { it.status == Status.Upcoming || it.status == Status.Postponed },
    MatchFilter.Finished to list.count { it.status == Status.Finished },
)

/** Groups by competition, followed leagues first, then by kickoff. */
fun groupByLeague(list: List<Match>, followedLeagues: Collection<String>): List<Pair<String, List<Match>>> =
    list.groupBy { it.leagueName }
        .toList()
        .sortedWith(compareBy<Pair<String, List<Match>>> { (_, ms) -> if (ms.first().leagueId in followedLeagues) 0 else 1 }.thenBy { it.first })
        .map { (name, ms) -> name to ms.sortedBy { it.kickoffMs } }

class MatchesViewModel(private val repo: FootballRepository, private val todayFn: () -> LocalDate) : LoadViewModel<List<Match>>() {
    private val _date = MutableStateFlow(todayFn())
    val date: StateFlow<LocalDate> = _date.asStateFlow()
    private val _filter = MutableStateFlow(MatchFilter.All)
    val filter: StateFlow<MatchFilter> = _filter.asStateFlow()
    private val _myTeams = MutableStateFlow(false)
    /** "My teams" chip: followed teams and leagues only, combined with the status segment. */
    val myTeamsOnly: StateFlow<Boolean> = _myTeams.asStateFlow()

    val today: LocalDate get() = todayFn()

    init { load() }

    fun selectDate(d: LocalDate) { if (d != _date.value) { _date.value = d; load() } }
    fun setFilter(f: MatchFilter) { if (f == MatchFilter.MyTeams) _myTeams.value = true else _filter.value = f }
    fun setMyTeamsOnly(on: Boolean) { _myTeams.value = on }

    override suspend fun fetch(refresh: Boolean) = repo.matchesOn(_date.value, refresh)
    override fun pollIntervalMs(data: List<Match>) = if (data.any { it.status == Status.Live }) LIVE_POLL_MS else null

    companion object { val Factory = containerFactory { c, _ -> MatchesViewModel(c.football) { c.clock.today() } } }
}

// ---------------- Match detail ----------------

class MatchDetailViewModel(private val repo: FootballRepository, private val id: String) : LoadViewModel<MatchDetail>() {
    init { load() }
    override suspend fun fetch(refresh: Boolean) = if (id.isBlank()) missingId() else repo.matchDetail(id, refresh)
    override fun isEmpty(data: MatchDetail) = false
    override fun pollIntervalMs(data: MatchDetail) = if (data.match.status == Status.Live) LIVE_POLL_MS else null

    companion object { val Factory = containerFactory { c, s -> MatchDetailViewModel(c.football, s.requireId()) } }
}

// ---------------- Leagues ----------------

data class LeaguesData(val leagues: List<League>, val topPlayers: List<PlayerSummary>)

class LeaguesViewModel(private val repo: FootballRepository, private val followedLeagues: suspend () -> List<String> = { emptyList() }) : LoadViewModel<LeaguesData>() {
    private val _selected = MutableStateFlow<String?>(null)
    /** Competition picked with the chips; defaults to the first followed league, else the first league. */
    val selected: StateFlow<String?> = _selected.asStateFlow()
    private val _detail = MutableStateFlow<com.innovativeinside.pitchside.core.UiState<LeagueDetail>>(com.innovativeinside.pitchside.core.UiState.Loading)
    val detail: StateFlow<com.innovativeinside.pitchside.core.UiState<LeagueDetail>> = _detail.asStateFlow()
    private var detailJob: kotlinx.coroutines.Job? = null

    init { load() }
    override suspend fun fetch(refresh: Boolean): Result<LeaguesData> = coroutineScope {
        val players = async { repo.topPlayers(refresh) }
        repo.leagues(refresh).map { LeaguesData(it, players.await().getOrDefault(emptyList())) }.onSuccess { d ->
            val followed = followedLeagues()
            val pick = _selected.value?.takeIf { id -> d.leagues.any { it.id == id } }
                ?: d.leagues.firstOrNull { it.id in followed }?.id ?: d.leagues.firstOrNull()?.id
            if (pick != null) loadDetail(pick, refresh)
        }
    }
    override fun isEmpty(data: LeaguesData) = data.leagues.isEmpty()

    fun select(id: String) { if (id != _selected.value) loadDetail(id, false) }
    fun retryDetail() { _selected.value?.let { loadDetail(it, true) } }

    private fun loadDetail(id: String, refresh: Boolean) {
        _selected.value = id
        detailJob?.cancel()
        if (_detail.value !is com.innovativeinside.pitchside.core.UiState.Success || refresh.not()) _detail.value = com.innovativeinside.pitchside.core.UiState.Loading
        detailJob = viewModelScope.launch {
            _detail.value = repo.leagueDetail(id, refresh).fold(
                { com.innovativeinside.pitchside.core.UiState.Success(it) },
                { com.innovativeinside.pitchside.core.UiState.Error(AppError.from(it)) },
            )
        }
    }
    companion object { val Factory = containerFactory { c, _ -> LeaguesViewModel(c.football) { c.prefs.prefs.first().leagues } } }
}

class LeagueDetailViewModel(private val repo: FootballRepository, private val id: String) : LoadViewModel<LeagueDetail>() {
    init { load() }
    override suspend fun fetch(refresh: Boolean) = if (id.isBlank()) missingId() else repo.leagueDetail(id, refresh)
    override fun isEmpty(data: LeagueDetail) = false
    companion object { val Factory = containerFactory { c, s -> LeagueDetailViewModel(c.football, s.requireId()) } }
}

// ---------------- Team and player ----------------

class TeamViewModel(private val repo: FootballRepository, private val id: String) : LoadViewModel<TeamPage>() {
    init { load() }
    override suspend fun fetch(refresh: Boolean) = if (id.isBlank()) missingId() else repo.teamPage(id, refresh)
    override fun isEmpty(data: TeamPage) = false
    companion object { val Factory = containerFactory { c, s -> TeamViewModel(c.football, s.requireId()) } }
}

class PlayerViewModel(private val repo: FootballRepository, private val id: String) : LoadViewModel<PlayerProfile>() {
    init { load() }
    override suspend fun fetch(refresh: Boolean) = if (id.isBlank()) missingId() else repo.player(id, refresh)
    override fun isEmpty(data: PlayerProfile) = false
    companion object { val Factory = containerFactory { c, s -> PlayerViewModel(c.football, s.requireId()) } }
}

// ---------------- Article ----------------

class ArticleViewModel(private val repo: FootballRepository, private val id: String) : LoadViewModel<NewsItem>() {
    init { load() }
    override suspend fun fetch(refresh: Boolean) = if (id.isBlank()) missingId() else repo.article(id)
    override fun isEmpty(data: NewsItem) = false
    companion object { val Factory = containerFactory { c, s -> ArticleViewModel(c.football, s.requireId()) } }
}

// ---------------- Favorites ----------------

data class FavoritesData(
    val teams: List<Team>,
    val leagues: List<League>,
    val players: List<PlayerSummary>,
    /** Today's matches; the screen keeps the ones involving followed teams ("My matches today"). */
    val today: List<Match> = emptyList(),
)

/** Resolves followed ids into names and crests. Reloads when the followed players change. */
class FavoritesViewModel(private val c: AppContainer) : LoadViewModel<FavoritesData>() {
    private var playerIds: List<String> = emptyList()

    init {
        viewModelScope.launch {
            c.prefs.prefs.map { it.players }.distinctUntilChanged().collect { ids -> playerIds = ids; load() }
        }
    }

    override suspend fun fetch(refresh: Boolean): Result<FavoritesData> = coroutineScope {
        val teams = async { c.football.popularTeams(refresh) }
        val leagues = async { c.football.leagues(refresh) }
        val players = playerIds.map { id -> async { c.football.player(id, refresh) } }
        val today = async { c.football.matchesOn(c.clock.today(), refresh).getOrDefault(emptyList()) }
        runCatching {
            FavoritesData(
                teams.await().getOrThrow(),
                leagues.await().getOrThrow(),
                players.mapNotNull { it.await().getOrNull()?.summary },
                today.await(),
            )
        }
    }

    override fun isEmpty(data: FavoritesData) = false
    companion object { val Factory = containerFactory { c, _ -> FavoritesViewModel(c) } }
}

// ---------------- Onboarding ----------------

class OnboardingTeamsViewModel(private val repo: FootballRepository) : LoadViewModel<List<Team>>() {
    init { load() }
    override suspend fun fetch(refresh: Boolean) = repo.popularTeams(refresh)
    companion object { val Factory = containerFactory { c, _ -> OnboardingTeamsViewModel(c.football) } }
}

// ---------------- Onboarding: building your home (real work, not a fake loader) ----------------

enum class StepState { Waiting, Running, Done, Skipped }
data class BuildStep(val label: String, val state: StepState = StepState.Waiting)

class BuildHomeViewModel(private val c: AppContainer) : androidx.lifecycle.ViewModel() {
    private val _steps = MutableStateFlow(
        listOf(BuildStep("Saving your choices"), BuildStep("Loading today's fixtures"), BuildStep("Loading leagues and tables"), BuildStep("Preparing your Match Briefs")),
    )
    val steps: StateFlow<List<BuildStep>> = _steps.asStateFlow()
    private val _finished = MutableStateFlow(false)
    val finished: StateFlow<Boolean> = _finished.asStateFlow()
    private var started = false

    fun start() {
        if (started) return
        started = true
        viewModelScope.launch {
            run(0) { true }
            run(1) { c.football.matchesOn(c.clock.today()).isSuccess }
            run(2) { c.football.leagues().isSuccess && c.football.standings("pd").isSuccess }
            run(3) { c.football.liveMatches().isSuccess }
            // Persist last, so a crash mid-way re-runs onboarding instead of leaving a half-built home.
            c.prefs.update(com.innovativeinside.pitchside.data.prefs.PrefsReducer::completeOnboarding)
            _finished.value = true
        }
    }

    private suspend fun run(i: Int, work: suspend () -> Boolean) {
        set(i, StepState.Running)
        val ok = runCatching { work() }.getOrDefault(false)
        // Offline is fine: the step is skipped and the data loads when the app opens.
        set(i, if (ok) StepState.Done else StepState.Skipped)
    }

    private fun set(i: Int, s: StepState) { _steps.value = _steps.value.mapIndexed { j, st -> if (j == i) st.copy(state = s) else st } }

    companion object { val Factory = containerFactory { c, _ -> BuildHomeViewModel(c) } }
}
