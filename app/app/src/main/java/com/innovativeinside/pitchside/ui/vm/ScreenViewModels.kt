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
    /** Standings of each competition the source covers (by league id), for ranks, form and the spotlight pick. */
    val tables: Map<String, List<StandingRow>> = emptyMap(),
) {
    val live: List<Match> get() = matches.filter { it.status == Status.Live }
}

/**
 * The match Home features when nothing is live: a followed team's next match, else the upcoming match between the
 * best-placed sides (lowest combined table rank) in the next few days, earliest first on ties. Falls back to today's
 * latest result when nothing is scheduled.
 */
fun pickSpotlight(upcoming: List<Match>, today: List<Match>, followedTeams: Collection<String>, tables: Map<String, List<StandingRow>>, nowMs: Long): Match? {
    val future = upcoming.filter { it.status == Status.Upcoming && it.kickoffMs >= nowMs }
    future.filter { it.involves(followedTeams) }.minByOrNull { it.kickoffMs }?.let { return it }
    fun rank(m: Match, teamId: String) = tables[m.leagueId]?.firstOrNull { it.team.id == teamId }?.rank ?: 99
    val soon = future.filter { it.kickoffMs - nowMs <= 3L * 24 * 3_600_000 }.ifEmpty { future }
    soon.minWithOrNull(compareBy<Match>({ rank(it, it.home.id) + rank(it, it.away.id) }, { it.kickoffMs }))?.let { return it }
    return today.filter { it.status == Status.Finished }.maxByOrNull { it.kickoffMs }
}

/** Today's matches per competition, busiest first: (league id, name, logo, count). */
fun countsByCompetition(matches: List<Match>): List<CompetitionCount> =
    matches.groupBy { it.leagueId }.map { (id, ms) -> CompetitionCount(id, ms.first().leagueName, ms.firstNotNullOfOrNull { it.leagueLogo }, ms.size, ms.count { it.status == Status.Live }) }
        .sortedWith(compareByDescending<CompetitionCount> { it.count }.thenBy { it.name })

data class CompetitionCount(val leagueId: String, val name: String, val logo: String?, val count: Int, val live: Int)

/** Teams in the best recent form across [tables]: 3 points a win, 1 a draw, over the last five. */
fun inForm(tables: Map<String, List<StandingRow>>, max: Int = 3): List<StandingRow> =
    tables.values.flatten().filter { it.form.length >= 3 }
        .sortedWith(compareByDescending<StandingRow> { r -> r.form.count { it == 'W' } * 3 + r.form.count { it == 'D' } }.thenBy { it.rank })
        .take(max)

/**
 * Which competitions to try for the Home standings tile: followed ones the data source knows, then the source's own
 * leagues in its order. Followed ids the source doesn't know (e.g. a default from another data source) are skipped.
 */
fun tableCandidates(followed: List<String>, known: List<String>, max: Int = 3): List<String> =
    (if (known.isEmpty()) followed else followed.filter { it in known } + known).distinct().take(max)

class HomeViewModel(
    private val repo: FootballRepository,
    /** Followed league ids, in the user's order; the first one with a table feeds the standings tile. */
    private val followedLeagues: suspend () -> List<String> = { emptyList() },
    liveUpdates: kotlinx.coroutines.flow.Flow<Set<String>> = kotlinx.coroutines.flow.emptyFlow(),
    private val today: () -> LocalDate,
) : LoadViewModel<HomeData>() {
    init {
        load()
        reloadOn(liveUpdates) { d, ids -> d.matches.any { it.id in ids } || d.live.isNotEmpty() }
    }

    override suspend fun fetch(refresh: Boolean): Result<HomeData> = coroutineScope {
        val matches = async { repo.matchesOn(today(), refresh) }
        val news = async { repo.headlines(null, refresh = refresh) }
        val leagues = async { repo.leagues(refresh) }
        val ahead = (1..3L).map { d -> async { repo.matchesOn(today().plusDays(d), refresh).getOrDefault(emptyList()) } }
        val table = async {
            val candidates = tableCandidates(followedLeagues(), leagues.await().getOrDefault(emptyList()).map { it.id })
            candidates.firstNotNullOfOrNull { id -> repo.leagueDetail(id, refresh).getOrNull()?.takeIf { it.standings.isNotEmpty() } }
        }
        val tables = async {
            val ids = leagues.await().getOrDefault(emptyList()).map { it.id }.take(6)
            ids.map { id -> async { id to repo.standings(id, refresh).getOrDefault(emptyList()) } }.map { it.await() }.filter { it.second.isNotEmpty() }.toMap()
        }
        // Matches are the point of the page: if they fail, the page fails. The rest degrade to empty.
        matches.await().map { raw ->
            val detail = table.await()
            val t = tables.await()
            val m = raw
            HomeData(
                matches = m,
                news = news.await().getOrNull()?.items.orEmpty().take(3),
                table = detail?.standings.orEmpty(),
                leagues = leagues.await().getOrDefault(emptyList()),
                tableLeague = detail?.league,
                scorers = detail?.scorers.orEmpty(),
                upcoming = (m + ahead.flatMap { it.await() }).filter { it.status == Status.Upcoming }.distinctBy { it.id }.sortedBy { it.kickoffMs },
                tables = t,
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
        val Factory = containerFactory { c, _ -> HomeViewModel(c.football, { c.prefs.prefs.first().leagues }, c.liveUpdates) { c.clock.today() } }
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

/**
 * The first day after [from] (up to [maxDays] ahead) that has at least one match, or null. Days that fail to load are
 * skipped; stops at the first hit so it costs as few calls as possible.
 */
suspend fun findNextMatchDay(from: LocalDate, maxDays: Int, matchesOn: suspend (LocalDate) -> Result<List<Match>>): LocalDate? {
    for (i in 1..maxDays) {
        val d = from.plusDays(i.toLong())
        if (matchesOn(d).getOrNull()?.isNotEmpty() == true) return d
    }
    return null
}

class MatchesViewModel(
    private val repo: FootballRepository,
    liveUpdates: kotlinx.coroutines.flow.Flow<Set<String>> = kotlinx.coroutines.flow.emptyFlow(),
    private val todayFn: () -> LocalDate,
) : LoadViewModel<List<Match>>() {
    private val _date = MutableStateFlow(todayFn())
    val date: StateFlow<LocalDate> = _date.asStateFlow()
    private val _filter = MutableStateFlow(MatchFilter.All)
    val filter: StateFlow<MatchFilter> = _filter.asStateFlow()
    private val _myTeams = MutableStateFlow(false)
    /** "My teams" chip: followed teams and leagues only, combined with the status segment. */
    val myTeamsOnly: StateFlow<Boolean> = _myTeams.asStateFlow()
    private val _nextMatchDay = MutableStateFlow<LocalDate?>(null)
    /** When the picked day has no matches: the next day (within [NEXT_DAY_LOOKAHEAD] days) that has some, else null. */
    val nextMatchDay: StateFlow<LocalDate?> = _nextMatchDay.asStateFlow()
    private var nextJob: kotlinx.coroutines.Job? = null

    val today: LocalDate get() = todayFn()

    init {
        load()
        reloadOn(liveUpdates) { list, ids -> list.any { it.id in ids } }
    }

    fun selectDate(d: LocalDate) { if (d != _date.value) { nextJob?.cancel(); _nextMatchDay.value = null; _date.value = d; load() } }

    override fun onLoaded(state: com.innovativeinside.pitchside.core.UiState<List<Match>>) {
        nextJob?.cancel()
        _nextMatchDay.value = null
        if (state !is com.innovativeinside.pitchside.core.UiState.Empty) return
        val from = _date.value
        nextJob = viewModelScope.launch {
            _nextMatchDay.value = boundedFetch { Result.success(findNextMatchDay(from, NEXT_DAY_LOOKAHEAD) { d -> repo.matchesOn(d) }) }.getOrNull()
        }
    }
    fun setFilter(f: MatchFilter) { if (f == MatchFilter.MyTeams) _myTeams.value = true else _filter.value = f }
    fun setMyTeamsOnly(on: Boolean) { _myTeams.value = on }

    override suspend fun fetch(refresh: Boolean) = repo.matchesOn(_date.value, refresh)
    override fun pollIntervalMs(data: List<Match>) = if (data.any { it.status == Status.Live }) LIVE_POLL_MS else null

    companion object {
        const val NEXT_DAY_LOOKAHEAD = 7
        val Factory = containerFactory { c, _ -> MatchesViewModel(c.football, c.liveUpdates) { c.clock.today() } }
    }
}

// ---------------- Match detail ----------------

class MatchDetailViewModel(
    private val repo: FootballRepository,
    private val id: String,
    liveUpdates: kotlinx.coroutines.flow.Flow<Set<String>> = kotlinx.coroutines.flow.emptyFlow(),
) : LoadViewModel<MatchDetail>() {
    init {
        load()
        reloadOn(liveUpdates, minGapMs = 3_000) { _, ids -> id in ids }
    }
    override suspend fun fetch(refresh: Boolean) = if (id.isBlank()) missingId() else repo.matchDetail(id, refresh)
    override fun isEmpty(data: MatchDetail) = false
    override fun pollIntervalMs(data: MatchDetail) = if (data.match.status == Status.Live) LIVE_POLL_MS else null

    companion object { val Factory = containerFactory { c, s -> MatchDetailViewModel(c.football, s.requireId(), c.liveUpdates) } }
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
            _detail.value = boundedFetch { repo.leagueDetail(id, refresh) }.fold(
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
        listOf(BuildStep("Saving your choices"), BuildStep("Loading today's fixtures"), BuildStep("Loading leagues and tables"), BuildStep("Checking live matches")),
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
            run(2) {
                val first = c.football.leagues().getOrNull()?.firstOrNull()
                first != null && c.football.standings(first.id).isSuccess
            }
            run(3) { c.football.liveMatches().isSuccess }
            // Persist last, so a crash mid-way re-runs onboarding instead of leaving a half-built home.
            c.prefs.update(com.innovativeinside.pitchside.data.prefs.PrefsReducer::completeOnboarding)
            _finished.value = true
        }
    }

    private suspend fun run(i: Int, work: suspend () -> Boolean) {
        set(i, StepState.Running)
        // Each repository call is time-limited in the data layer, so a slow network cannot leave a step running forever.
        val ok = runCatching { work() }.getOrDefault(false)
        // Offline is fine: the step is skipped and the data loads when the app opens.
        set(i, if (ok) StepState.Done else StepState.Skipped)
    }

    private fun set(i: Int, s: StepState) { _steps.value = _steps.value.mapIndexed { j, st -> if (j == i) st.copy(state = s) else st } }

    companion object { val Factory = containerFactory { c, _ -> BuildHomeViewModel(c) } }
}
