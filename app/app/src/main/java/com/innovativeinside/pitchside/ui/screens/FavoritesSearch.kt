@file:OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class)

package com.innovativeinside.pitchside.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.EmojiEvents
import androidx.compose.material.icons.outlined.Groups
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.innovativeinside.pitchside.R
import com.innovativeinside.pitchside.data.model.Team
import com.innovativeinside.pitchside.data.prefs.AlertLevel
import com.innovativeinside.pitchside.data.prefs.UserPrefs
import com.innovativeinside.pitchside.ui.LocalApp
import com.innovativeinside.pitchside.ui.LocalPrefs
import com.innovativeinside.pitchside.ui.components.BackButton
import com.innovativeinside.pitchside.ui.components.Chip
import com.innovativeinside.pitchside.ui.components.ChipRow
import com.innovativeinside.pitchside.ui.components.Crest
import com.innovativeinside.pitchside.ui.components.EmptyState
import com.innovativeinside.pitchside.ui.components.ErrorState
import com.innovativeinside.pitchside.ui.components.FlowChips
import com.innovativeinside.pitchside.ui.components.IconAction
import com.innovativeinside.pitchside.ui.components.ListSkeleton
import com.innovativeinside.pitchside.ui.components.PcCard
import com.innovativeinside.pitchside.ui.components.PcDialog
import com.innovativeinside.pitchside.ui.components.SearchField
import com.innovativeinside.pitchside.ui.components.SegmentedTabs
import com.innovativeinside.pitchside.ui.components.StateContent
import com.innovativeinside.pitchside.ui.components.TextAction
import com.innovativeinside.pitchside.ui.components.pcClickable
import com.innovativeinside.pitchside.ui.theme.Pc
import com.innovativeinside.pitchside.ui.theme.PcShape
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
        Row(Modifier.fillMaxWidth().padding(start = 16.dp, end = 8.dp, top = 16.dp), verticalAlignment = Alignment.CenterVertically) {
            Text("Favorites", style = PcType.Title.copy(fontSize = 28.sp, lineHeight = 34.sp), modifier = Modifier.weight(1f))
            IconAction(Icons.Outlined.Settings, "Settings", nav.openSettings, filled = true)
            Spacer(Modifier.width(8.dp))
            Box(Modifier.size(48.dp).clip(PcShape.Control).background(Pc.Lime).pcClickable(PcShape.Control, "Add favorites", onClick = nav.openSearch), contentAlignment = Alignment.Center) { Icon(Icons.Outlined.Add, null, tint = Pc.OnLime) }
        }
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            SegmentedTabs(listOf("Teams ${prefs.teams.size}", "Leagues ${prefs.leagues.size}", "Players ${prefs.players.size}"), tab, { tab = it })
            StateContent(state, vm::retry) { d ->
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    when (tab) {
                        0 -> {
                            if (!prefs.isPro) FollowMeter(prefs)
                            val teams = d.teams.filter { it.id in prefs.teams }
                            if (teams.isEmpty()) EmptyState("No teams yet", "Follow teams to see their matches first and get alerts.", actionLabel = "Find teams", onAction = nav.openSearch, icon = Icons.Outlined.Groups)
                            teams.forEach { t -> FollowedTeamCard(t, prefs, nav, onUnfollow = { unfollow = t }) }
                        }
                        1 -> {
                            val leagues = d.leagues.filter { it.id in prefs.leagues }
                            if (leagues.isEmpty()) EmptyState("No leagues yet", "Follow a league to pin its matches and table.", actionLabel = "Browse leagues", onAction = nav.openSearch, icon = Icons.Outlined.EmojiEvents)
                            leagues.forEach { l -> LeagueRow(l, true, { nav.openLeague(l.id) }) { app.toggleLeague(l.id, l.name) } }
                        }
                        else -> {
                            if (d.players.isEmpty()) EmptyState("No players yet", "Follow players to track their form and goals.", actionLabel = "Find players", onAction = nav.openSearch, icon = Icons.Outlined.Person)
                            d.players.forEach { p -> PlayerRow(p, { nav.openPlayer(p.id) }) { Chip(stringResource(R.string.action_following), true, { app.togglePlayer(p.id, p.name) }) } }
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
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        Text("${prefs.teams.size} of $limit free follows", style = PcType.Meta.copy(color = Pc.Text2))
        Box(Modifier.weight(1f).height(6.dp).clip(CircleShape).background(Pc.Border)) {
            Box(Modifier.fillMaxWidth((prefs.teams.size / limit.toFloat()).coerceAtMost(1f)).height(6.dp).clip(CircleShape).background(Pc.Lime))
        }
    }
}

@Composable
private fun FollowedTeamCard(t: Team, prefs: UserPrefs, nav: FavoritesNav, onUnfollow: () -> Unit) {
    val app = LocalApp.current
    val level = prefs.teamAlerts[t.id] ?: prefs.alertLevel
    PcCard(Modifier.fillMaxWidth(), onClick = { nav.openTeam(t.id) }) {
        Column(Modifier.padding(start = 16.dp, end = 12.dp, top = 12.dp, bottom = 12.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Crest(t, 40.dp)
                Text(t.name, style = PcType.Label.copy(fontSize = 15.sp), modifier = Modifier.weight(1f))
                val on = t.id in prefs.shielded
                IconAction(if (on) Icons.Outlined.VisibilityOff else Icons.Outlined.Visibility, if (on) "Spoiler Shield on for ${t.name}" else "Spoiler Shield off for ${t.name}", { app.toggleShield(t.id) }, tint = if (on) Pc.Lime else Pc.Text2, filled = true)
            }
            Text("Alerts", style = PcType.Meta)
            FlowChips(AlertLevel.entries.map { it.label }, level.label, { label -> app.setTeamAlert(t.id, AlertLevel.entries.first { it.label == label }) })
            TextAction("Unfollow", onUnfollow, color = Pc.Red)
        }
    }
}

// ---------------- Search ----------------

@Composable
fun SearchRoute(back: () -> Unit, openTeam: (String) -> Unit, openLeague: (String) -> Unit, openPlayer: (String) -> Unit, vm: SearchViewModel = viewModel(factory = SearchViewModel.Factory)) {
    val app = LocalApp.current
    val prefs = LocalPrefs.current
    val query by vm.query.collectAsStateWithLifecycle()
    val tab by vm.tab.collectAsStateWithLifecycle()
    val state by vm.state.collectAsStateWithLifecycle()
    Screen {
        Row(Modifier.fillMaxWidth().padding(start = 8.dp, end = 16.dp, top = 8.dp), verticalAlignment = Alignment.CenterVertically) {
            BackButton(back)
            SearchField(query, vm::onQuery, "Search teams, leagues, players", autoFocus = true)
        }
        Spacer(Modifier.height(8.dp))
        ChipRow(SearchTab.entries.map { it.name }, tab.name, { vm.setTab(SearchTab.valueOf(it)) })
        Spacer(Modifier.height(16.dp))
        Column(Modifier.padding(horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            when (val s = state) {
                SearchState.Idle -> {
                    Text("TRY", style = PcType.Overline.copy(color = Pc.Text3))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) { listOf("Northgate", "Premier", "Marsh").forEach { Chip(it, false, { vm.onQuery(it) }) } }
                    EmptyState("Search everything", "Type at least two letters to find a team, a league or a player.", icon = Icons.Outlined.Search)
                }
                SearchState.Loading -> ListSkeleton(3)
                is SearchState.Error -> ErrorState(s.error, vm::retry)
                is SearchState.Empty -> EmptyState("No results for \"${s.query}\"", "Check the spelling or try a club or league name.", actionLabel = "Clear search", onAction = { vm.onQuery("") }, icon = Icons.Outlined.SearchOff)
                is SearchState.Results -> {
                    val r = s.results
                    if ((tab == SearchTab.All || tab == SearchTab.Teams) && r.teams.isNotEmpty()) {
                        Text("TEAMS", style = PcType.Overline.copy(color = Pc.Text3))
                        r.teams.forEach { t ->
                            val on = t.id in prefs.teams
                            ResultRow({ Crest(t, 40.dp) }, t.name, "Club", on, { app.toggleTeam(t.id, t.name) }) { openTeam(t.id) }
                        }
                    }
                    if ((tab == SearchTab.All || tab == SearchTab.Leagues) && r.leagues.isNotEmpty()) {
                        Text("LEAGUES", style = PcType.Overline.copy(color = Pc.Text3))
                        r.leagues.forEach { l ->
                            val on = l.id in prefs.leagues
                            ResultRow({ LeagueBadge(l.short) }, l.name, l.country, on, { app.toggleLeague(l.id, l.name) }) { openLeague(l.id) }
                        }
                    }
                    if ((tab == SearchTab.All || tab == SearchTab.Players) && r.players.isNotEmpty()) {
                        Text("PLAYERS", style = PcType.Overline.copy(color = Pc.Text3))
                        r.players.forEach { p ->
                            val on = p.id in prefs.players
                            ResultRow({ PlayerAvatar(p.name, p.team.color) }, p.name, "${p.position} · ${p.team.name}", on, { app.togglePlayer(p.id, p.name) }) { openPlayer(p.id) }
                        }
                    }
                    val shownNone = when (tab) { SearchTab.Teams -> r.teams.isEmpty(); SearchTab.Leagues -> r.leagues.isEmpty(); SearchTab.Players -> r.players.isEmpty(); else -> false }
                    if (shownNone) EmptyState("No ${tab.name.lowercase()} match \"${s.query}\"", "Other results are under All.", actionLabel = "Show all", onAction = { vm.setTab(SearchTab.All) }, icon = Icons.Outlined.SearchOff)
                }
            }
        }
    }
}

@Composable
private fun ResultRow(leading: @Composable () -> Unit, title: String, sub: String, following: Boolean, onToggle: () -> Unit, onOpen: () -> Unit) {
    PcCard(Modifier.fillMaxWidth(), onClick = onOpen) {
        Row(Modifier.padding(horizontal = 12.dp, vertical = 8.dp).heightIn(min = 56.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            leading()
            Column(Modifier.weight(1f)) { Text(title, style = PcType.Label.copy(fontSize = 15.sp, fontWeight = FontWeight.Bold)); Text(sub, style = PcType.Meta.copy(color = Pc.Text2)) }
            Chip(if (following) stringResource(R.string.action_following) else stringResource(R.string.action_follow), following, onToggle)
        }
    }
}

