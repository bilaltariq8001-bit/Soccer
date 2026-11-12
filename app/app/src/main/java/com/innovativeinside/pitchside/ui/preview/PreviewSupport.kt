package com.innovativeinside.pitchside.ui.preview

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import com.innovativeinside.pitchside.AppContainer
import com.innovativeinside.pitchside.core.AppClock
import com.innovativeinside.pitchside.core.DefaultDispatchers
import com.innovativeinside.pitchside.core.DispatcherProvider
import com.innovativeinside.pitchside.core.SystemClock
import com.innovativeinside.pitchside.data.auth.AuthRepository
import com.innovativeinside.pitchside.data.auth.InMemoryAuthRepository
import com.innovativeinside.pitchside.data.mock.MockBehavior
import com.innovativeinside.pitchside.data.mock.MockFootballApi
import com.innovativeinside.pitchside.data.model.League
import com.innovativeinside.pitchside.data.model.Match
import com.innovativeinside.pitchside.data.model.NewsCategory
import com.innovativeinside.pitchside.data.model.NewsItem
import com.innovativeinside.pitchside.data.model.StandingRow
import com.innovativeinside.pitchside.data.model.Status
import com.innovativeinside.pitchside.data.model.Team
import com.innovativeinside.pitchside.data.prefs.InMemoryPrefsRepository
import com.innovativeinside.pitchside.data.prefs.PrefsRepository
import com.innovativeinside.pitchside.data.prefs.UserPrefs
import com.innovativeinside.pitchside.data.repository.DefaultFootballRepository
import com.innovativeinside.pitchside.data.repository.FootballRepository
import com.innovativeinside.pitchside.ui.LocalApp
import com.innovativeinside.pitchside.ui.LocalPrefs
import com.innovativeinside.pitchside.ui.theme.Pc
import com.innovativeinside.pitchside.ui.theme.PitchsideTheme
import com.innovativeinside.pitchside.ui.vm.AppViewModel

/** Container with in-memory prefs/auth and the mock API. Used by previews and by Robolectric UI tests. */
class InMemoryContainer(
    initialPrefs: UserPrefs = UserPrefs(onboarded = true, teams = listOf("ng")),
    override val clock: AppClock = SystemClock,
    override val dispatchers: DispatcherProvider = DefaultDispatchers,
    latencyMs: Long = 0,
) : AppContainer {
    override val prefs: PrefsRepository = InMemoryPrefsRepository(initialPrefs)
    override val auth: AuthRepository = InMemoryAuthRepository(clock)
    override val mockBehavior: MockBehavior = MockBehavior().apply { this.latencyMs = latencyMs }
    override val football: FootballRepository = DefaultFootballRepository(MockFootballApi(clock, mockBehavior), dispatchers, clock)
}

/** Wraps a preview in the theme and the locals every screen expects. */
@Composable
fun PreviewHost(dark: Boolean = true, prefs: UserPrefs = UserPrefs(onboarded = true, teams = listOf("ng")), content: @Composable () -> Unit) {
    val app = remember { AppViewModel(InMemoryContainer(prefs)) }
    PitchsideTheme(dark) {
        CompositionLocalProvider(LocalApp provides app, LocalPrefs provides prefs) {
            Box(Modifier.background(Pc.Bg)) { content() }
        }
    }
}

/** Sample domain objects for previews only. Real screens get data from the repository. */
object PreviewData {
    private val ng = Team("ng", "Northgate", "NG", 0xFF1F6F6B, "pd")
    private val ru = Team("ru", "Riverside", "RU", 0xFF7A3FBF, "pd")
    private val hc = Team("hc", "Harbor City", "HC", 0xFF1F5FBF, "pd")
    private val rr = Team("rr", "Redmont", "RR", 0xFFB0262F, "pd")
    private const val NOW = 1_800_000_000_000L

    val live = Match("m1", "pd", "Premier Division", "Round 9", "Northgate Park", "A. Calder", NOW, ng, ru, 2, 1, Status.Live, "74'",
        "Northgate lead 2-1, but Riverside have had 7 shots since half time.")
    val upcoming = Match("m4", "pd", "Premier Division", "Round 9", "Harbor Park", null, NOW + 3_600_000, hc, rr, null, null, Status.Upcoming, "20:45", "A win lifts Harbor City to second.")
    val finished = upcoming.copy(id = "m7", homeScore = 1, awayScore = 3, status = Status.Finished, label = "FT")
    val matches = listOf(live, upcoming, finished)

    val table = listOf(ng, ru, hc, rr).mapIndexed { i, t -> StandingRow(i + 1, t, 9, 7 - i, 1, 1 + i, 20 - i * 2, 8 + i, 12 - i * 3, 22 - i * 3, "WWDWL") }
    val news = listOf(NewsItem("n1", NewsCategory.Reports, "Northgate edge Riverside in a six-goal thriller", "Two late goals settle it.", "Body", "Pitchside Desk", NOW - 720_000, 4, null))
    val leagues = listOf(League("pd", "Premier Division", "PD", "Northland", 8, "2026/27", "Round 9"), League("cc", "Continental Cup", "CC", "International", 16, "2026/27", "Round of 16"))

    val home = com.innovativeinside.pitchside.ui.vm.HomeData(matches, news, table, leagues)
}
