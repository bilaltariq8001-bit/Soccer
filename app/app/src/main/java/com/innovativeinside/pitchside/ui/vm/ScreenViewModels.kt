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
) {
    val live: List<Match> get() = matches.filter { it.status == Status.Live }
}

class HomeViewModel(private val repo: FootballRepository, private val today: () -> LocalDate) : LoadViewModel<HomeData>() {
    init { load() }

    override suspend fun fetch(refresh: Boolean): Result<HomeData> = coroutineScope {
        val matches = async { repo.matchesOn(today(), refresh) }
        val news = async { repo.news(null) }
        val table = async { repo.standings("pd", refresh) }
        val leagues = async { repo.leagues(refresh) }
        // Matches are the point of the page: if they fail, the page fails. The rest degrade to empty.
        matches.await().map { m ->
            HomeData(m, news.await().getOrNull()?.items.orEmpty().take(3), table.await().getOrDefault(emptyList()), leagues.await().getOrDefault(emptyList()))
        }
    }

    override fun isEmpty(data: HomeData) = false // Home always has something to show (empty matches has its own card).
    override fun pollIntervalMs(data: HomeData) = if (data.live.isNotEmpty()) LIVE_POLL_MS else null

    companion object { val Factory = containerFactory { c, _ -> HomeViewModel(c.football) { c.clock.today() } } }
}

// ---------------- Matches ----------------

enum class MatchFilter { All, Live, MyTeams }

/** Pure filter used by the Matches screen, unit tested. */
fun filterMatches(list: List<Match>, filter: MatchFilter, followedTeams: Collection<String>, followedLeagues: Collection<String>): List<Match> = when (filter) {
    MatchFilter.All -> list
    MatchFilter.Live -> list.filter { it.status == Status.Live }
    MatchFilter.MyTeams -> list.filter { it.involves(followedTeams) || it.leagueId in followedLeagues }
}

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

    val today: LocalDate get() = todayFn()

    init { load() }

    fun selectDate(d: LocalDate) { if (d != _date.value) { _date.value = d; load() } }
    fun setFilter(f: MatchFilter) { _filter.value = f }

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

class LeaguesViewModel(private val repo: FootballRepository) : LoadViewModel<LeaguesData>() {
    init { load() }
    override suspend fun fetch(refresh: Boolean): Result<LeaguesData> = coroutineScope {
        val players = async { repo.topPlayers(refresh) }
        repo.leagues(refresh).map { LeaguesData(it, players.await().getOrDefault(emptyList())) }
    }
    override fun isEmpty(data: LeaguesData) = data.leagues.isEmpty()
    companion object { val Factory = containerFactory { c, _ -> LeaguesViewModel(c.football) } }
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

data class FavoritesData(val teams: List<Team>, val leagues: List<League>, val players: List<PlayerSummary>)

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
        runCatching {
            FavoritesData(
                teams.await().getOrThrow(),
                leagues.await().getOrThrow(),
                players.mapNotNull { it.await().getOrNull()?.summary },
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
