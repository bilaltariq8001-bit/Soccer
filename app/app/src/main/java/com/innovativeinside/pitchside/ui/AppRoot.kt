package com.innovativeinside.pitchside.ui

import android.net.ConnectivityManager
import android.net.Network
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
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.innovativeinside.pitchside.AppViewModel
import com.innovativeinside.pitchside.ui.components.BannerKind
import com.innovativeinside.pitchside.ui.components.BottomNav
import com.innovativeinside.pitchside.ui.components.InfoBanner
import com.innovativeinside.pitchside.ui.components.SideRail
import com.innovativeinside.pitchside.ui.components.navItems
import com.innovativeinside.pitchside.ui.screens.AlertsScreen
import com.innovativeinside.pitchside.ui.screens.AskScreen
import com.innovativeinside.pitchside.ui.screens.BuildingScreen
import com.innovativeinside.pitchside.ui.screens.FollowingScreen
import com.innovativeinside.pitchside.ui.screens.HomeActions
import com.innovativeinside.pitchside.ui.screens.HomeScreen
import com.innovativeinside.pitchside.ui.screens.LanguageScreen
import com.innovativeinside.pitchside.ui.screens.LeagueScreen
import com.innovativeinside.pitchside.ui.screens.LineupScreen
import com.innovativeinside.pitchside.ui.screens.MatchScreen
import com.innovativeinside.pitchside.ui.screens.MatchesScreen
import com.innovativeinside.pitchside.ui.screens.NewsScreen
import com.innovativeinside.pitchside.ui.screens.PaywallScreen
import com.innovativeinside.pitchside.ui.screens.PersonaScreen
import com.innovativeinside.pitchside.ui.screens.PlayerScreen
import com.innovativeinside.pitchside.ui.screens.SearchScreen
import com.innovativeinside.pitchside.ui.screens.SettingsScreen
import com.innovativeinside.pitchside.ui.screens.TeamsScreen
import com.innovativeinside.pitchside.ui.screens.WatchScreen
import com.innovativeinside.pitchside.ui.screens.WelcomeScreen
import com.innovativeinside.pitchside.ui.screens.WidthClass
import com.innovativeinside.pitchside.ui.screens.widthClass
import com.innovativeinside.pitchside.ui.theme.Pc

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
    const val Player = "player/{id}"
    const val Search = "search"
    const val Ask = "ask"
    const val Paywall = "paywall"
    const val Settings = "settings"
}

@Composable
private fun rememberOnline(): State<Boolean> {
    val ctx = LocalContext.current
    val state = remember { mutableStateOf(true) }
    DisposableEffect(ctx) {
        val cm = ctx.getSystemService(ConnectivityManager::class.java)
        state.value = cm.activeNetwork != null
        val cb = object : ConnectivityManager.NetworkCallback() {
            override fun onAvailable(network: Network) { state.value = true }
            override fun onLost(network: Network) { state.value = cm.activeNetwork != null }
        }
        cm.registerDefaultNetworkCallback(cb)
        onDispose { cm.unregisterNetworkCallback(cb) }
    }
    return state
}

private fun NavHostController.goTab(route: String) = navigate(route) {
    popUpTo(Routes.Home) { saveState = true }
    launchSingleTop = true
    restoreState = true
}

@Composable
fun AppRoot(app: AppViewModel) {
    val nav = rememberNavController()
    val entry = nav.currentBackStackEntryAsState().value
    val route = entry?.destination?.route
    val tabRoutes = navItems.map { it.route }
    val showNav = route in tabRoutes
    val width = widthClass()
    val online by rememberOnline()

    LaunchedEffect(app.toastMessage) { if (app.toastMessage != null) { kotlinx.coroutines.delay(2500); app.toastMessage = null } }

    val home = HomeActions(
        openMatch = { nav.navigate("match/$it") },
        openSearch = { nav.navigate(Routes.Search) },
        openAsk = { nav.navigate(Routes.Ask) },
        openNews = { nav.goTab(Routes.News) },
        openLeagues = { nav.goTab(Routes.Leagues) },
        openMatches = { nav.goTab(Routes.Matches) },
    )

    Row(Modifier.fillMaxSize().background(Pc.Bg)) {
        if (showNav && width != WidthClass.Compact) SideRail(route, { nav.goTab(it) }, { nav.navigate(Routes.Ask) }, wide = width == WidthClass.Expanded)
        Column(Modifier.weight(1f).fillMaxHeight()) {
            Box(Modifier.weight(1f).fillMaxWidth()) {
                NavHost(nav, startDestination = if (app.onboarded) Routes.Home else Routes.Welcome) {
                    composable(Routes.Welcome) { WelcomeScreen({ nav.navigate(Routes.Language) }, { app.toastMessage = "Account sign-in arrives with the backend." }) }
                    composable(Routes.Language) { LanguageScreen({ nav.popBackStack() }, { nav.navigate(Routes.Teams) }) }
                    composable(Routes.Teams) { TeamsScreen({ nav.popBackStack() }, { nav.navigate(Routes.Watch) }) }
                    composable(Routes.Watch) { WatchScreen({ nav.popBackStack() }, { nav.navigate(Routes.Alerts) }) }
                    composable(Routes.Alerts) { AlertsScreen({ nav.popBackStack() }, { nav.navigate(Routes.Persona) }) }
                    composable(Routes.Persona) { PersonaScreen({ nav.popBackStack() }, { nav.navigate(Routes.Building) }) }
                    composable(Routes.Building) {
                        BuildingScreen { nav.navigate(Routes.Home) { popUpTo(Routes.Welcome) { inclusive = true } } }
                    }
                    composable(Routes.Home) { HomeScreen(home) }
                    composable(Routes.Matches) { MatchesScreen { nav.navigate("match/$it") } }
                    composable(Routes.Leagues) { LeagueScreen { nav.navigate("player/$it") } }
                    composable(Routes.News) { NewsScreen() }
                    composable(Routes.Following) {
                        FollowingScreen({ nav.navigate(Routes.Search) }, { nav.navigate(Routes.Paywall) }, { nav.navigate(Routes.Settings) }, { nav.navigate("player/$it") })
                    }
                    composable(Routes.Match, listOf(navArgument("id") { type = NavType.StringType })) {
                        MatchScreen(it.arguments?.getString("id").orEmpty(), { nav.popBackStack() }, { id -> nav.navigate("lineup/$id") })
                    }
                    composable(Routes.Lineup, listOf(navArgument("id") { type = NavType.StringType })) {
                        LineupScreen(it.arguments?.getString("id").orEmpty()) { nav.popBackStack() }
                    }
                    composable(Routes.Player, listOf(navArgument("id") { type = NavType.StringType })) {
                        PlayerScreen(it.arguments?.getString("id").orEmpty()) { nav.popBackStack() }
                    }
                    composable(Routes.Search) { SearchScreen({ nav.popBackStack() }, { nav.navigate(Routes.Paywall) }) { nav.navigate("player/$it") } }
                    composable(Routes.Ask) { AskScreen({ nav.popBackStack() }, { nav.navigate(Routes.Paywall) }) }
                    composable(Routes.Paywall) { PaywallScreen { nav.popBackStack() } }
                    composable(Routes.Settings) {
                        SettingsScreen({ nav.popBackStack() }, { nav.navigate(Routes.Paywall) }) {
                            nav.navigate(Routes.Welcome) { popUpTo(0) { inclusive = true } }
                        }
                    }
                }
                if (app.toastMessage != null) Box(Modifier.align(Alignment.BottomCenter).padding(16.dp)) {
                    InfoBanner(app.toastMessage.orEmpty(), BannerKind.Success)
                }
            }
            if (!online) InfoBanner("Offline. Showing saved scores", BannerKind.Warning, Modifier.padding(horizontal = 16.dp, vertical = 8.dp))
            if (showNav && width == WidthClass.Compact) BottomNav(route) { nav.goTab(it) } else Spacer(Modifier.navigationBarsPadding())
        }
    }
}

private operator fun <T> State<T>.getValue(thisRef: Any?, property: kotlin.reflect.KProperty<*>): T = this.value
