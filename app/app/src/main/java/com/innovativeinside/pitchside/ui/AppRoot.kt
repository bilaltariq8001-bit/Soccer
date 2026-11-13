package com.innovativeinside.pitchside.ui

import android.app.Activity
import android.net.ConnectivityManager
import android.net.Network
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import androidx.navigation.navDeepLink
import com.innovativeinside.pitchside.R
import com.innovativeinside.pitchside.data.prefs.UserPrefs
import com.innovativeinside.pitchside.ui.components.BannerKind
import com.innovativeinside.pitchside.ui.components.BottomNavBar
import com.innovativeinside.pitchside.ui.components.OfflineBanner
import com.innovativeinside.pitchside.ui.screens.NotificationsScreen
import com.innovativeinside.pitchside.ui.theme.PcSpace
import com.innovativeinside.pitchside.ui.components.InfoBanner
import com.innovativeinside.pitchside.ui.components.PcDialog
import com.innovativeinside.pitchside.ui.components.SideRail
import com.innovativeinside.pitchside.ui.components.navItems
import com.innovativeinside.pitchside.ui.screens.AccountRoute
import com.innovativeinside.pitchside.ui.screens.AlertsScreen
import com.innovativeinside.pitchside.ui.screens.ArticleRoute
import com.innovativeinside.pitchside.ui.screens.AskRoute
import com.innovativeinside.pitchside.ui.screens.AuthRoute
import com.innovativeinside.pitchside.ui.screens.BuildingScreen
import com.innovativeinside.pitchside.ui.screens.FavoritesNav
import com.innovativeinside.pitchside.ui.screens.FavoritesRoute
import com.innovativeinside.pitchside.ui.screens.HomeActions
import com.innovativeinside.pitchside.ui.screens.HomeRoute
import com.innovativeinside.pitchside.ui.screens.LanguageScreen
import com.innovativeinside.pitchside.ui.screens.LeagueRoute
import com.innovativeinside.pitchside.ui.screens.LeaguesRoute
import com.innovativeinside.pitchside.ui.screens.LineupRoute
import com.innovativeinside.pitchside.ui.screens.MatchNav
import com.innovativeinside.pitchside.ui.screens.MatchRoute
import com.innovativeinside.pitchside.ui.screens.MatchesRoute
import com.innovativeinside.pitchside.ui.screens.NewsRoute
import com.innovativeinside.pitchside.ui.screens.PaywallScreen
import com.innovativeinside.pitchside.ui.screens.PersonaScreen
import com.innovativeinside.pitchside.ui.screens.PlayerRoute
import com.innovativeinside.pitchside.ui.screens.SearchRoute
import com.innovativeinside.pitchside.ui.screens.SettingsNav
import com.innovativeinside.pitchside.ui.screens.SettingsScreen
import com.innovativeinside.pitchside.ui.screens.TeamRoute
import com.innovativeinside.pitchside.ui.screens.TeamsScreen
import com.innovativeinside.pitchside.ui.screens.WatchScreen
import com.innovativeinside.pitchside.ui.screens.WelcomeScreen
import com.innovativeinside.pitchside.ui.screens.WidthClass
import com.innovativeinside.pitchside.ui.screens.widthClass
import com.innovativeinside.pitchside.ui.theme.Pc
import com.innovativeinside.pitchside.ui.vm.AppEvent
import com.innovativeinside.pitchside.ui.vm.AppViewModel
import com.innovativeinside.pitchside.ui.vm.AuthMode
import kotlinx.coroutines.delay

object Routes {
    const val Welcome = "onb_welcome"
    const val Language = "onb_language"
    const val Teams = "onb_teams"
    const val Watch = "onb_watch"
    const val Alerts = "onb_alerts"
    const val Persona = "onb_persona"
    const val Building = "onb_building"
    const val Home = "home"
    const val Matches = "matches"
    const val Leagues = "leagues"
    const val News = "news"
    const val Following = "following"
    const val Match = "match/{id}"
    const val Lineup = "lineup/{id}"
    const val League = "league/{id}"
    const val Team = "team/{id}"
    const val Player = "player/{id}"
    const val Article = "article/{id}"
    const val Search = "search"
    const val Ask = "ask"
    const val Paywall = "paywall"
    const val Settings = "settings"
    const val Auth = "auth/{mode}"
    const val Account = "account"
    const val Notifications = "notifications"

    fun match(id: String) = "match/$id"
    fun lineup(id: String) = "lineup/$id"
    fun league(id: String) = "league/$id"
    fun team(id: String) = "team/$id"
    fun player(id: String) = "player/$id"
    fun article(id: String) = "article/$id"
    fun auth(mode: AuthMode) = "auth/${mode.name}"

    const val DEEP_LINK = "pitchside://"
}

private val idArg = listOf(navArgument("id") { type = NavType.StringType })

@Composable
private fun rememberOnline(): Boolean {
    val ctx = LocalContext.current
    var online by remember { mutableStateOf(true) }
    DisposableEffect(ctx) {
        val cm = ctx.getSystemService(ConnectivityManager::class.java)
        online = cm.activeNetwork != null
        val cb = object : ConnectivityManager.NetworkCallback() {
            override fun onAvailable(network: Network) { online = true }
            override fun onLost(network: Network) { online = cm.activeNetwork != null }
        }
        cm.registerDefaultNetworkCallback(cb)
        onDispose { cm.unregisterNetworkCallback(cb) }
    }
    return online
}

/** Tab switches keep each tab's own back stack and scroll state. */
private fun NavHostController.goTab(route: String) = navigate(route) {
    popUpTo(Routes.Home) { saveState = true }
    launchSingleTop = true
    restoreState = true
}

@Composable
fun AppRoot(app: AppViewModel, prefs: UserPrefs) {
    val nav = rememberNavController()
    val entry by nav.currentBackStackEntryAsState()
    val route = entry?.destination?.route
    val showNav = route in navItems.map { it.route }
    val width = widthClass()
    val online = rememberOnline()
    val liveCount by app.liveCount.collectAsStateWithLifecycle()
    val badges = mapOf(Routes.Matches to liveCount)
    var toast by remember { mutableStateOf<String?>(null) }
    var exitDialog by remember { mutableStateOf(false) }
    val activity = LocalContext.current as? Activity

    LaunchedEffect(Unit) {
        app.events.collect { e ->
            when (e) {
                is AppEvent.Message -> toast = e.text
                AppEvent.OpenPaywall -> nav.navigate(Routes.Paywall)
            }
        }
    }
    LaunchedEffect(toast) { if (toast != null) { delay(2500); toast = null } }

    // Back on Home asks before leaving, so a stray swipe doesn't close the app mid-match.
    BackHandler(enabled = route == Routes.Home) { exitDialog = true }

    val openMatch: (String) -> Unit = { nav.navigate(Routes.match(it)) }
    val openTeam: (String) -> Unit = { nav.navigate(Routes.team(it)) }
    val openLeague: (String) -> Unit = { nav.navigate(Routes.league(it)) }
    val openPlayer: (String) -> Unit = { nav.navigate(Routes.player(it)) }
    val openArticle: (String) -> Unit = { nav.navigate(Routes.article(it)) }
    val openSearch: () -> Unit = { nav.navigate(Routes.Search) }
    val back: () -> Unit = { if (!nav.popBackStack()) nav.goTab(Routes.Home) }

    Row(Modifier.fillMaxSize().background(Pc.Bg)) {
        if (showNav && width != WidthClass.Compact) SideRail(route, { nav.goTab(it) }, { nav.navigate(Routes.Ask) }, wide = width == WidthClass.Expanded, badges = badges)
        Column(Modifier.weight(1f).fillMaxHeight()) {
            Box(Modifier.weight(1f).fillMaxWidth()) {
                NavHost(nav, startDestination = if (prefs.onboarded) Routes.Home else Routes.Welcome) {
                    // Onboarding
                    composable(Routes.Welcome) { WelcomeScreen({ nav.navigate(Routes.Language) }, { nav.navigate(Routes.auth(AuthMode.SignIn)) }) }
                    composable(Routes.Language) { LanguageScreen(back) { nav.navigate(Routes.Teams) } }
                    composable(Routes.Teams) { TeamsScreen(back, { nav.navigate(Routes.Watch) }) }
                    composable(Routes.Watch) { WatchScreen(back) { nav.navigate(Routes.Alerts) } }
                    composable(Routes.Alerts) { AlertsScreen(back) { nav.navigate(Routes.Persona) } }
                    composable(Routes.Persona) { PersonaScreen(back) { nav.navigate(Routes.Building) } }
                    composable(Routes.Building) { BuildingScreen({ nav.navigate(Routes.Home) { popUpTo(0) { inclusive = true } } }) }

                    // Tabs
                    composable(Routes.Home) {
                        HomeRoute(HomeActions(openMatch, openSearch, { nav.navigate(Routes.Ask) }, { nav.goTab(Routes.News) }, openArticle, openLeague, openTeam, { nav.goTab(Routes.Matches) }, { nav.navigate(Routes.Notifications) }, openPlayer))
                    }
                    composable(Routes.Matches) { MatchesRoute(openMatch, openLeague, openSearch) }
                    composable(Routes.Leagues) { LeaguesRoute(openLeague, openPlayer, openSearch, openTeam, openMatch) }
                    composable(Routes.News) { NewsRoute(openArticle) }
                    composable(Routes.Following) {
                        FavoritesRoute(FavoritesNav(openSearch, { nav.navigate(Routes.Settings) }, openTeam, openLeague, openPlayer, { nav.navigate(Routes.Paywall) }, openMatch))
                    }

                    // Details
                    composable(Routes.Match, idArg, deepLinks = listOf(navDeepLink { uriPattern = "${Routes.DEEP_LINK}match/{id}" })) {
                        MatchRoute(MatchNav(back, { nav.navigate(Routes.lineup(it)) }, openTeam, openPlayer, openLeague))
                    }
                    composable(Routes.Lineup, idArg) { LineupRoute(back, openPlayer) }
                    composable(Routes.League, idArg, deepLinks = listOf(navDeepLink { uriPattern = "${Routes.DEEP_LINK}league/{id}" })) { LeagueRoute(back, openMatch, openTeam, openPlayer) }
                    composable(Routes.Team, idArg, deepLinks = listOf(navDeepLink { uriPattern = "${Routes.DEEP_LINK}team/{id}" })) { TeamRoute(back, openMatch, openTeam, openPlayer) }
                    composable(Routes.Player, idArg) { PlayerRoute(back, openTeam) }
                    composable(Routes.Article, idArg) { ArticleRoute(back) }
                    composable(Routes.Search) { SearchRoute(back, openTeam, openLeague, openPlayer, openMatch) }
                    composable(Routes.Notifications) { NotificationsScreen(back) { nav.navigate(Routes.Settings) } }
                    composable(Routes.Ask) { AskRoute(back, openPaywall = { nav.navigate(Routes.Paywall) }) }
                    composable(Routes.Paywall) { PaywallScreen(back) }
                    composable(Routes.Settings) {
                        SettingsScreen(SettingsNav(back, { nav.navigate(Routes.Paywall) }, { nav.navigate(Routes.auth(AuthMode.SignIn)) }, { nav.navigate(Routes.Account) }) {
                            nav.navigate(Routes.Welcome) { popUpTo(0) { inclusive = true } }
                        })
                    }
                    composable(Routes.Auth, listOf(navArgument("mode") { type = NavType.StringType })) { e ->
                        val mode = runCatching { AuthMode.valueOf(e.arguments?.getString("mode").orEmpty()) }.getOrDefault(AuthMode.SignIn)
                        AuthRoute(mode, back, onDone = {
                            // From onboarding's "I already have an account", signing in skips the quiz.
                            if (!prefs.onboarded) nav.navigate(Routes.Language) { popUpTo(Routes.Welcome) } else back()
                        })
                    }
                    composable(Routes.Account) {
                        AccountRoute(back) { nav.navigate(Routes.auth(AuthMode.SignIn)) { popUpTo(Routes.Account) { inclusive = true } } }
                    }
                }
                toast?.let { t -> Box(Modifier.align(Alignment.BottomCenter).padding(PcSpace.md).testTag("toast")) { InfoBanner(t, BannerKind.Success) } }
            }
            if (!online) OfflineBanner(Modifier.padding(horizontal = PcSpace.screen, vertical = PcSpace.xs).testTag("offline_banner"))
            if (showNav && width == WidthClass.Compact) BottomNavBar(route, { nav.goTab(it) }, badges) else Spacer(Modifier.navigationBarsPadding())
        }
    }
    if (exitDialog) PcDialog(
        stringResource(R.string.exit_title), stringResource(R.string.exit_body), stringResource(R.string.action_exit), { exitDialog = false; activity?.finish() },
        stringResource(R.string.action_cancel), { exitDialog = false },
    )
}
