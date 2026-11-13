@file:OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class)

package com.innovativeinside.pitchside.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.EmojiEvents
import androidx.compose.material.icons.outlined.Groups
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.SearchOff
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.Visibility
import androidx.compose.material.icons.outlined.VisibilityOff
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.innovativeinside.pitchside.R
import com.innovativeinside.pitchside.data.model.Team
import com.innovativeinside.pitchside.data.prefs.AlertLevel
import com.innovativeinside.pitchside.data.prefs.UserPrefs
import com.innovativeinside.pitchside.ui.LocalApp
import com.innovativeinside.pitchside.ui.LocalPrefs
import com.innovativeinside.pitchside.ui.components.AppTopBar
import com.innovativeinside.pitchside.ui.components.BackButton
import com.innovativeinside.pitchside.ui.components.Chip
import com.innovativeinside.pitchside.ui.components.ChipRow
import com.innovativeinside.pitchside.ui.components.CrestSize
import com.innovativeinside.pitchside.ui.components.EmptyState
import com.innovativeinside.pitchside.ui.components.ErrorState
import com.innovativeinside.pitchside.ui.components.FlowChips
import com.innovativeinside.pitchside.ui.components.Hairline
import com.innovativeinside.pitchside.ui.components.IconAction
import com.innovativeinside.pitchside.ui.components.ListSkeleton
import com.innovativeinside.pitchside.ui.components.Overline
import com.innovativeinside.pitchside.ui.components.PcCard
import com.innovativeinside.pitchside.ui.components.PcDialog
import com.innovativeinside.pitchside.ui.components.PlayerAvatar
import com.innovativeinside.pitchside.ui.components.SearchField
import com.innovativeinside.pitchside.ui.components.SectionHeader
import com.innovativeinside.pitchside.ui.components.Segment
import com.innovativeinside.pitchside.ui.components.SegmentedControl
import com.innovativeinside.pitchside.ui.components.StateContent
import com.innovativeinside.pitchside.ui.components.TeamCrest
import com.innovativeinside.pitchside.ui.components.TextAction
import com.innovativeinside.pitchside.ui.components.pcClickable
import com.innovativeinside.pitchside.ui.theme.Pc
import com.innovativeinside.pitchside.ui.theme.PcShape
import com.innovativeinside.pitchside.ui.theme.PcSpace
import com.innovativeinside.pitchside.ui.theme.PcType
import com.innovativeinside.pitchside.ui.vm.FavoritesViewModel
import com.innovativeinside.pitchside.ui.vm.SearchState
import com.innovativeinside.pitchside.ui.vm.SearchTab
import com.innovativeinside.pitchside.ui.vm.SearchViewModel

// ---------------- Favorites ----------------

class FavoritesNav(
    val openSearch: () -> Unit,
    val openSettings: () -> Unit,
    val openTeam: (String) -> Unit,
    val openLeague: (String) -> Unit,
    val openPlayer: (String) -> Unit,
    val openPaywall: () -> Unit,
    val openMatch: (String) -> Unit = {},
)

@Composable
fun FavoritesRoute(nav: FavoritesNav, vm: FavoritesViewModel = viewModel(factory = FavoritesViewModel.Factory)) {
    val state by vm.state.collectAsStateWithLifecycle()
    val refreshing by vm.refreshing.collectAsStateWithLifecycle()
    val refreshError by vm.refreshError.collectAsStateWithLifecycle()
    val app = LocalApp.current
    val prefs = LocalPrefs.current
    var tab by rememberSaveable { mutableIntStateOf(0) }
    var unfollow by remember { mutableStateOf<Team?>(null) }
    RefreshScreen(refreshing, vm::refresh, refreshError, vm::dismissRefreshError) {
        AppTopBar("Favorites", subtitle = "Your teams, leagues and players", actions = {
            IconAction(Icons.Outlined.Settings, "Settings", nav.openSettings)
            IconAction(Icons.Outlined.Add, "Add favorites", nav.openSearch, tint = Pc.Accent)
        })
        StateContent(state, vm::retry, Modifier.padding(horizontal = PcSpace.screen)) { d ->
            Column(Modifier.padding(top = PcSpace.xs), verticalArrangement = Arrangement.spacedBy(PcSpace.gutter)) {
                // My matches today
                val mine = d.today.filter { it.involves(prefs.teams) }.sortedBy { it.kickoffMs }
                if (mine.isNotEmpty()) {
                    SectionHeader("My matches today")
                    MatchList(mine, nav.openMatch)
                }
                SegmentedControl(
                    listOf(Segment("Teams", prefs.teams.size), Segment("Leagues", prefs.leagues.size), Segment("Players", prefs.players.size)),
                    tab, { tab = it },
                )
                when (tab) {
                    0 -> {
                        if (!prefs.isPro) FollowMeter(prefs)
                        val teams = d.teams.filter { it.id in prefs.teams }
                        if (teams.isEmpty()) {
                            EmptyState("No teams yet", "Follow teams to see their matches first and get alerts.", actionLabel = "Find teams", onAction = nav.openSearch, icon = Icons.Outlined.Groups)
                            val suggested = d.teams.filter { it.id !in prefs.teams }.take(4)
                            if (suggested.isNotEmpty()) {
                                Overline("Suggested teams")
                                PcCard(Modifier.fillMaxWidth()) {
                                    Column { suggested.forEachIndexed { i, t ->
                                        FollowRow({ TeamCrest(t, CrestSize.M.dp) }, t.name, "Popular club", false, { app.toggleTeam(t.id, t.name) }) { nav.openTeam(t.id) }
                                        if (i < suggested.lastIndex) Hairline()
                                    } }
                                }
                            }
                        }
                        teams.forEach { t -> FollowedTeamCard(t, prefs, nav, onUnfollow = { unfollow = t }) }
                    }
                    1 -> {
                        val leagues = d.leagues.filter { it.id in prefs.leagues }
                        if (leagues.isEmpty()) EmptyState("No leagues yet", "Follow a league to pin its matches and table.", actionLabel = "Browse leagues", onAction = nav.openSearch, icon = Icons.Outlined.EmojiEvents)
                        else PcCard(Modifier.fillMaxWidth()) {
                            Column { leagues.forEachIndexed { i, l ->
                                FollowRow({ LeagueBadge(l.short) }, l.name, "${l.country} · ${l.currentRound}", true, { app.toggleLeague(l.id, l.name) }) { nav.openLeague(l.id) }
                                if (i < leagues.lastIndex) Hairline()
                            } }
                        }
                    }
                    else -> {
                        if (d.players.isEmpty()) EmptyState("No players yet", "Follow players to track their form and goals.", actionLabel = "Find players", onAction = nav.openSearch, icon = Icons.Outlined.Person)
                        else PcCard(Modifier.fillMaxWidth()) {
                            Column { d.players.forEachIndexed { i, p ->
                                FollowRow({ PlayerAvatar(p.name, p.team.color, 40.dp) }, p.name, "${p.position} · ${p.team.name}", true, { app.togglePlayer(p.id, p.name) }) { nav.openPlayer(p.id) }
                                if (i < d.players.lastIndex) Hairline()
                            } }
                        }
                    }
                }
            }
        }
    }
    unfollow?.let { t ->
        PcDialog(
            "Unfollow ${t.name}?", "You'll stop getting alerts and its matches leave your home.", "Unfollow", { app.toggleTeam(t.id, t.name); unfollow = null },
            "Keep following", { unfollow = null }, danger = true,
        )
    }
}

@Composable
private fun FollowMeter(prefs: UserPrefs) {
    val limit = UserPrefs.FREE_TEAM_LIMIT
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(PcSpace.sm)) {
        Text("${prefs.teams.size} of $limit free follows", style = PcType.Caption)
        Box(Modifier.weight(1f).height(6.dp).clip(CircleShape).background(Pc.SurfaceVariant)) {
            Box(Modifier.fillMaxWidth((prefs.teams.size / limit.toFloat()).coerceAtMost(1f)).height(6.dp).clip(CircleShape).background(Pc.Accent))
        }
    }
}

@Composable
private fun FollowedTeamCard(t: Team, prefs: UserPrefs, nav: FavoritesNav, onUnfollow: () -> Unit) {
    val app = LocalApp.current
    val level = prefs.teamAlerts[t.id] ?: prefs.alertLevel
    PcCard(Modifier.fillMaxWidth(), onClick = { nav.openTeam(t.id) }) {
        Column(Modifier.padding(start = PcSpace.md, end = PcSpace.xs, top = PcSpace.sm, bottom = PcSpace.xs), verticalArrangement = Arrangement.spacedBy(PcSpace.xs)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(PcSpace.sm)) {
                TeamCrest(t, CrestSize.L.dp)
                Text(t.name, style = PcType.Subtitle, modifier = Modifier.weight(1f), maxLines = 1, overflow = TextOverflow.Ellipsis)
                val on = t.id in prefs.shielded
                IconAction(if (on) Icons.Outlined.VisibilityOff else Icons.Outlined.Visibility, if (on) "Spoiler Shield on for ${t.name}" else "Spoiler Shield off for ${t.name}", { app.toggleShield(t.id) }, tint = if (on) Pc.Accent else Pc.Text2)
            }
            Overline("Alerts")
            FlowChips(AlertLevel.entries.map { it.label }, level.label, { label -> app.setTeamAlert(t.id, AlertLevel.entries.first { it.label == label }) })
            TextAction("Unfollow", onUnfollow, color = Pc.Error)
        }
    }
}

/** A list row with a leading visual, title/subtitle and a Follow/Following chip. */
@Composable
fun FollowRow(leading: @Composable () -> Unit, title: String, sub: String, following: Boolean, onToggle: () -> Unit, onOpen: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().heightIn(min = 64.dp).pcClickable(PcShape.Control, "Open $title", onClick = onOpen).padding(horizontal = PcSpace.sm, vertical = PcSpace.xs),
        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(PcSpace.sm),
    ) {
        leading()
        Column(Modifier.weight(1f)) {
            Text(title, style = PcType.BodyStrong, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(sub, style = PcType.Caption, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
        Chip(if (following) stringResource(R.string.action_following) else stringResource(R.string.action_follow), following, onToggle)
    }
}

// ---------------- Search ----------------

@Composable
fun SearchRoute(
    back: () -> Unit,
    openTeam: (String) -> Unit,
    openLeague: (String) -> Unit,
    openPlayer: (String) -> Unit,
    openMatch: (String) -> Unit = {},
    vm: SearchViewModel = viewModel(factory = SearchViewModel.Factory),
) {
    val app = LocalApp.current
    val prefs = LocalPrefs.current
    val query by vm.query.collectAsStateWithLifecycle()
    val tab by vm.tab.collectAsStateWithLifecycle()
    val state by vm.state.collectAsStateWithLifecycle()
    val saveQuery: () -> Unit = { app.addRecentSearch(query) }
    Screen {
        Row(Modifier.fillMaxWidth().padding(start = PcSpace.xs, end = PcSpace.xxs, top = PcSpace.xs), verticalAlignment = Alignment.CenterVertically) {
            BackButton(back)
            SearchField(query, vm::onQuery, "Teams, leagues, players, matches", Modifier.weight(1f), autoFocus = true, onSearch = saveQuery)
            TextAction(stringResource(R.string.action_cancel), { if (query.isEmpty()) back() else vm.onQuery("") }, color = Pc.Text2)
        }
        Spacer(Modifier.height(PcSpace.xs))
        if (state !is SearchState.Idle) ChipRow(SearchTab.entries.map { it.name }, tab.name, { vm.setTab(SearchTab.valueOf(it)) })
        Spacer(Modifier.height(PcSpace.sm))
        Column(Modifier.padding(horizontal = PcSpace.screen), verticalArrangement = Arrangement.spacedBy(PcSpace.xs)) {
            when (val s = state) {
                SearchState.Idle -> {
                    if (prefs.recentSearches.isNotEmpty()) {
                        SectionHeader("Recent searches", "Clear", { app.clearRecentSearches() })
                        PcCard(Modifier.fillMaxWidth()) {
                            Column { prefs.recentSearches.forEachIndexed { i, q ->
                                Row(Modifier.fillMaxWidth().heightIn(min = 52.dp).pcClickable(PcShape.Control, "Search $q") { vm.onQuery(q) }.padding(start = PcSpace.md), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(PcSpace.sm)) {
                                    Icon(Icons.Outlined.History, null, tint = Pc.Text2, modifier = Modifier.size(20.dp))
                                    Text(q, style = PcType.Body, modifier = Modifier.weight(1f), maxLines = 1, overflow = TextOverflow.Ellipsis)
                                    IconAction(Icons.Outlined.Close, "Remove $q from recent searches", { app.removeRecentSearch(q) }, tint = Pc.Text2)
                                }
                                if (i < prefs.recentSearches.lastIndex) Hairline()
                            } }
                        }
                    }
                    EmptyState("Search everything", "Type at least two letters to find a club, a league, a player or one of today's matches.", icon = Icons.Outlined.Search)
                }
                SearchState.Loading -> ListSkeleton(3)
                is SearchState.Error -> ErrorState(s.error, vm::retry)
                is SearchState.Empty -> EmptyState("No results for \"${s.query}\"", "Check the spelling or try a club or league name.", actionLabel = "Clear search", onAction = { vm.onQuery("") }, icon = Icons.Outlined.SearchOff)
                is SearchState.Results -> {
                    val r = s.results
                    val all = tab == SearchTab.All
                    if ((all || tab == SearchTab.Teams) && r.teams.isNotEmpty()) ResultGroup("Teams") {
                        r.teams.forEachIndexed { i, t ->
                            FollowRow({ TeamCrest(t, CrestSize.M.dp) }, t.name, "Club", t.id in prefs.teams, { app.toggleTeam(t.id, t.name) }) { saveQuery(); openTeam(t.id) }
                            if (i < r.teams.lastIndex) Hairline()
                        }
                    }
                    if ((all || tab == SearchTab.Leagues) && r.leagues.isNotEmpty()) ResultGroup("Competitions") {
                        r.leagues.forEachIndexed { i, l ->
                            FollowRow({ LeagueBadge(l.short) }, l.name, l.country, l.id in prefs.leagues, { app.toggleLeague(l.id, l.name) }) { saveQuery(); openLeague(l.id) }
                            if (i < r.leagues.lastIndex) Hairline()
                        }
                    }
                    if ((all || tab == SearchTab.Players) && r.players.isNotEmpty()) ResultGroup("Players") {
                        r.players.forEachIndexed { i, p ->
                            FollowRow({ PlayerAvatar(p.name, p.team.color, 40.dp) }, p.name, "${p.position} · ${p.team.name}", p.id in prefs.players, { app.togglePlayer(p.id, p.name) }) { saveQuery(); openPlayer(p.id) }
                            if (i < r.players.lastIndex) Hairline()
                        }
                    }
                    if (all && s.matches.isNotEmpty()) {
                        Overline("Matches today", Modifier.padding(top = PcSpace.xs).semantics { heading() })
                        MatchList(s.matches, { id -> saveQuery(); openMatch(id) })
                    }
                    val shownNone = when (tab) { SearchTab.Teams -> r.teams.isEmpty(); SearchTab.Leagues -> r.leagues.isEmpty(); SearchTab.Players -> r.players.isEmpty(); else -> false }
                    if (shownNone) EmptyState("No ${tab.name.lowercase()} match \"${s.query}\"", "Other results are under All.", actionLabel = "Show all", onAction = { vm.setTab(SearchTab.All) }, icon = Icons.Outlined.SearchOff)
                }
            }
        }
    }
}

@Composable
private fun ResultGroup(title: String, content: @Composable () -> Unit) {
    Overline(title, Modifier.padding(top = PcSpace.xs).semantics { heading() })
    PcCard(Modifier.fillMaxWidth()) { Column { content() } }
}
