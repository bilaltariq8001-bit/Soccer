@file:OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class, androidx.compose.material3.ExperimentalMaterial3Api::class, androidx.compose.foundation.ExperimentalFoundationApi::class)

package com.innovativeinside.pitchside.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.KeyboardArrowRight
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.EmojiEvents
import androidx.compose.material.icons.outlined.EventBusy
import androidx.compose.material.icons.outlined.Groups
import androidx.compose.material.icons.outlined.StarBorder
import androidx.compose.material.icons.outlined.TableChart
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.innovativeinside.pitchside.R
import com.innovativeinside.pitchside.core.UiState
import com.innovativeinside.pitchside.data.model.League
import com.innovativeinside.pitchside.data.model.PlayerProfile
import com.innovativeinside.pitchside.data.model.PlayerSummary
import com.innovativeinside.pitchside.data.model.Position
import com.innovativeinside.pitchside.data.model.Scorer
import com.innovativeinside.pitchside.data.repository.LeagueDetail
import com.innovativeinside.pitchside.data.repository.TeamPage
import com.innovativeinside.pitchside.ui.LocalApp
import com.innovativeinside.pitchside.ui.LocalPrefs
import com.innovativeinside.pitchside.ui.components.AppTopBar
import com.innovativeinside.pitchside.ui.components.BackButton
import com.innovativeinside.pitchside.ui.components.BentoTile
import com.innovativeinside.pitchside.ui.components.BriefCard
import com.innovativeinside.pitchside.ui.components.Chip
import com.innovativeinside.pitchside.ui.components.CrestSize
import com.innovativeinside.pitchside.ui.components.DetailSkeleton
import com.innovativeinside.pitchside.ui.components.EmptyState
import com.innovativeinside.pitchside.ui.components.ErrorState
import com.innovativeinside.pitchside.ui.components.FilterChipRow
import com.innovativeinside.pitchside.ui.components.Hairline
import com.innovativeinside.pitchside.ui.components.IconAction
import com.innovativeinside.pitchside.ui.components.ListSkeleton
import com.innovativeinside.pitchside.ui.components.Overline
import com.innovativeinside.pitchside.ui.components.PcCard
import com.innovativeinside.pitchside.ui.components.PlayerAvatar
import com.innovativeinside.pitchside.ui.components.PrimaryButton
import com.innovativeinside.pitchside.ui.components.SecondaryButton
import com.innovativeinside.pitchside.ui.components.SectionHeader
import com.innovativeinside.pitchside.ui.components.SegmentedTabs
import com.innovativeinside.pitchside.ui.components.StandingsHeader
import com.innovativeinside.pitchside.ui.components.StandingsRow
import com.innovativeinside.pitchside.ui.components.StateContent
import com.innovativeinside.pitchside.ui.components.TeamCrest
import com.innovativeinside.pitchside.ui.components.UnderlineTabs
import com.innovativeinside.pitchside.ui.components.pcClickable
import com.innovativeinside.pitchside.ui.preview.PreviewData
import com.innovativeinside.pitchside.ui.preview.PreviewHost
import com.innovativeinside.pitchside.ui.theme.Pc
import com.innovativeinside.pitchside.ui.theme.PcShape
import com.innovativeinside.pitchside.ui.theme.PcSpace
import com.innovativeinside.pitchside.ui.theme.PcType
import com.innovativeinside.pitchside.ui.vm.LeagueDetailViewModel
import com.innovativeinside.pitchside.ui.vm.LeaguesData
import com.innovativeinside.pitchside.ui.vm.LeaguesViewModel
import com.innovativeinside.pitchside.ui.vm.PlayerViewModel
import com.innovativeinside.pitchside.ui.vm.TeamViewModel
import com.innovativeinside.pitchside.ui.vm.groupByLeague

// ---------------- Leagues tab ----------------

@Composable
fun LeaguesRoute(openLeague: (String) -> Unit, openPlayer: (String) -> Unit, openSearch: () -> Unit, openTeam: (String) -> Unit = {}, openMatch: (String) -> Unit = {}, vm: LeaguesViewModel = viewModel(factory = LeaguesViewModel.Factory)) {
    val state by vm.state.collectAsStateWithLifecycle()
    val refreshing by vm.refreshing.collectAsStateWithLifecycle()
    val selected by vm.selected.collectAsStateWithLifecycle()
    val detail by vm.detail.collectAsStateWithLifecycle()
    PullToRefreshBox(isRefreshing = refreshing, onRefresh = vm::refresh, modifier = Modifier.fillMaxSize().background(Pc.Bg)) {
        LeaguesContent(state, vm::retry, openLeague, openPlayer, openSearch, selected, detail, vm::select, vm::retryDetail, openTeam, openMatch)
    }
}

/**
 * Leagues tab: competition chips, then Table / Fixtures / Top scorers for the picked competition (the
 * table header sticks while scrolling), then every competition with a follow toggle and the top players.
 */
@Composable
fun LeaguesContent(
    state: UiState<LeaguesData>,
    onRetry: () -> Unit,
    openLeague: (String) -> Unit,
    openPlayer: (String) -> Unit,
    openSearch: () -> Unit,
    selected: String? = null,
    detail: UiState<LeagueDetail> = UiState.Loading,
    onSelect: (String) -> Unit = {},
    onRetryDetail: () -> Unit = {},
    openTeam: (String) -> Unit = {},
    openMatch: (String) -> Unit = {},
) {
    val app = LocalApp.current
    val prefs = LocalPrefs.current
    var view by rememberSaveable { mutableIntStateOf(0) }
    Box(Modifier.fillMaxSize().background(Pc.Bg).statusBarsPadding(), contentAlignment = Alignment.TopCenter) {
        LazyColumn(Modifier.widthIn(max = 760.dp).fillMaxSize(), contentPadding = PaddingValues(bottom = PcSpace.lg)) {
            item { AppTopBar("Leagues", subtitle = "Tables, fixtures and top scorers", onSearch = openSearch) }
            when (state) {
                UiState.Loading -> item { ListSkeleton(5, Modifier.padding(PcSpace.screen)) }
                UiState.Empty -> item { EmptyState("No leagues available", "Competitions appear here once the season data is loaded.", Modifier.padding(PcSpace.screen), icon = Icons.Outlined.EmojiEvents) }
                is UiState.Error -> item { ErrorState(state.error, onRetry, Modifier.padding(PcSpace.screen).testStateError()) }
                is UiState.Success -> {
                    val leagues = state.data.leagues.sortedBy { if (it.id in prefs.leagues) 0 else 1 }
                    val current = leagues.firstOrNull { it.id == selected } ?: leagues.first()
                    item {
                        FilterChipRow(leagues.map { it.name }, current.name, { name -> leagues.firstOrNull { it.name == name }?.let { onSelect(it.id) } }, Modifier.padding(vertical = PcSpace.xs))
                    }
                    item {
                        val following = current.id in prefs.leagues
                        Row(
                            Modifier.fillMaxWidth().padding(horizontal = PcSpace.screen, vertical = PcSpace.xs),
                            verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(PcSpace.sm),
                        ) {
                            LeagueBadge(current.short, 48)
                            Column(Modifier.weight(1f)) {
                                Text(current.name, style = PcType.Subtitle, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.semantics { heading() })
                                Text("${current.country} · ${current.season} · ${current.currentRound}", style = PcType.Caption, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            }
                            IconAction(if (following) Icons.Filled.Star else Icons.Outlined.StarBorder, if (following) "Unfollow ${current.name}" else "Follow ${current.name}", { app.toggleLeague(current.id, current.name) }, tint = if (following) Pc.Accent else Pc.Text2)
                            IconAction(Icons.AutoMirrored.Outlined.KeyboardArrowRight, "Open ${current.name}", { openLeague(current.id) }, tint = Pc.Text2)
                        }
                    }
                    item { SegmentedTabs(listOf("Table", "Fixtures", "Top scorers"), view, { view = it }, Modifier.padding(horizontal = PcSpace.screen, vertical = PcSpace.xs)) }
                    when (detail) {
                        UiState.Loading, UiState.Empty -> item { ListSkeleton(5, Modifier.padding(PcSpace.screen)) }
                        is UiState.Error -> item { ErrorState(detail.error, onRetryDetail, Modifier.padding(PcSpace.screen)) }
                        is UiState.Success -> leagueSection(detail.data, view, openTeam, openMatch, openPlayer)
                    }
                    item { SectionHeader("All competitions", modifier = Modifier.padding(start = PcSpace.screen, end = PcSpace.screen, top = PcSpace.md)) }
                    item {
                        PcCard(Modifier.padding(horizontal = PcSpace.screen).fillMaxWidth()) {
                            Column { leagues.forEachIndexed { i, l ->
                                FollowRow({ LeagueBadge(l.short) }, l.name, "${l.country} · ${l.currentRound}", l.id in prefs.leagues, { app.toggleLeague(l.id, l.name) }) { openLeague(l.id) }
                                if (i < leagues.lastIndex) Hairline()
                            } }
                        }
                    }
                    if (state.data.topPlayers.isNotEmpty()) {
                        item { SectionHeader("Top-rated players", modifier = Modifier.padding(start = PcSpace.screen, end = PcSpace.screen, top = PcSpace.md)) }
                        item {
                            PcCard(Modifier.padding(horizontal = PcSpace.screen).fillMaxWidth()) {
                                Column { state.data.topPlayers.forEachIndexed { i, p -> PlayerRow(p, { openPlayer(p.id) }); if (i < state.data.topPlayers.lastIndex) Hairline() } }
                            }
                        }
                    }
                }
            }
        }
    }
}

private fun Modifier.testStateError() = this.testTag("state_error")

private fun androidx.compose.foundation.lazy.LazyListScope.leagueSection(d: LeagueDetail, view: Int, openTeam: (String) -> Unit, openMatch: (String) -> Unit, openPlayer: (String) -> Unit) {
    when (view) {
        0 -> if (d.standings.isEmpty()) item {
            EmptyState("Knockout competition", "${d.league.name} has no league table. Check Fixtures for the draw and results.", Modifier.padding(PcSpace.screen), icon = Icons.Outlined.TableChart)
        } else {
            stickyHeader(key = "table_header") { Box(Modifier.padding(horizontal = PcSpace.screen).clip(PcShape.Small)) { StandingsHeader(showForm = true) } }
            itemsIndexed(d.standings, key = { _, r -> "row-${r.team.id}" }) { i, r ->
                val followed = LocalPrefs.current.teams.contains(r.team.id)
                Column(Modifier.padding(horizontal = PcSpace.screen)) {
                    StandingsRow(r, { openTeam(r.team.id) }, followed = followed, showForm = true)
                    if (i < d.standings.lastIndex) Hairline()
                }
            }
            item { Text("Form shows the last five results, newest on the right. Followed clubs are tinted.", style = PcType.Caption, modifier = Modifier.padding(horizontal = PcSpace.screen, vertical = PcSpace.xs)) }
        }
        1 -> if (d.fixtures.isEmpty()) item {
            EmptyState("No fixtures this week", "Fixtures for the next round appear once they're scheduled.", Modifier.padding(PcSpace.screen), icon = Icons.Outlined.EventBusy)
        } else d.fixtures.groupBy { formatDay(it.kickoffMs) }.forEach { (day, ms) ->
            item(key = "fx-$day") { LeagueGroup(day, ms, openMatch, Modifier.padding(horizontal = PcSpace.screen, vertical = PcSpace.xxs), short = day.take(3)) }
        }
        else -> if (d.scorers.isEmpty()) item {
            EmptyState("No goals yet", "Top scorers appear after the first round.", Modifier.padding(PcSpace.screen), icon = Icons.Outlined.EmojiEvents)
        } else item {
            PcCard(Modifier.padding(horizontal = PcSpace.screen).fillMaxWidth()) {
                Column { d.scorers.forEachIndexed { i, s -> ScorerRow(s) { openPlayer(s.playerId) }; if (i < d.scorers.lastIndex) Hairline() } }
            }
        }
    }
}

@Composable
fun LeagueRow(l: League, following: Boolean, onOpen: () -> Unit, onToggle: () -> Unit) {
    PcCard(Modifier.fillMaxWidth()) { FollowRow({ LeagueBadge(l.short) }, l.name, "${l.country} · ${l.currentRound}", following, onToggle, onOpen) }
}

/** Player row: avatar, name, position · club · nationality, and the rating pill. */
@Composable
fun PlayerRow(p: PlayerSummary, onClick: () -> Unit, trailing: @Composable () -> Unit = { RatingPill(p.rating) }) {
    Row(
        Modifier.fillMaxWidth().heightIn(min = 64.dp).pcClickable(PcShape.Control, "Open ${p.name}", onClick = onClick).padding(horizontal = PcSpace.sm, vertical = PcSpace.xs),
        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(PcSpace.sm),
    ) {
        PlayerAvatar(p.name, p.team.color, 40.dp)
        Column(Modifier.weight(1f)) {
            Text(p.name, style = PcType.BodyStrong, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text("${p.position} · ${p.team.name} · ${p.nationality}", style = PcType.Caption, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
        trailing()
    }
}

/** Rating pill; the number is always shown, colour only reinforces it. */
@Composable
fun RatingPill(r: Double) {
    val (bg, fg) = when { r >= 8.0 -> Pc.Accent to Pc.OnAccent; r >= 7.0 -> Pc.AccentContainer to Pc.Accent; else -> Pc.SurfaceVariant to Pc.Text }
    Box(Modifier.heightIn(min = 26.dp).clip(PcShape.Chip).background(bg).padding(horizontal = 10.dp).semantics { contentDescription = "Rating %.1f".format(r) }, contentAlignment = Alignment.Center) {
        Text("%.1f".format(r), style = PcType.Numeric.copy(fontWeight = FontWeight.Bold), color = fg)
    }
}

@Composable
fun ratingColor(r: Double): Color = when { r >= 8.0 -> Pc.Accent; r >= 7.0 -> Pc.Warning; else -> Pc.Neutral }

// ---------------- Shared detail header ----------------

/** Header for team, player and competition pages: visual, name, facts line, follow button. */
@Composable
private fun EntityHeader(visual: @Composable () -> Unit, title: String, facts: String, following: Boolean, onFollow: () -> Unit, extra: @Composable () -> Unit = {}) {
    Column(Modifier.padding(horizontal = PcSpace.screen), verticalArrangement = Arrangement.spacedBy(PcSpace.sm)) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(PcSpace.md)) {
            visual()
            Column(Modifier.weight(1f)) {
                Text(title, style = PcType.Title, maxLines = 2, overflow = TextOverflow.Ellipsis, modifier = Modifier.semantics { heading() })
                Text(facts, style = PcType.BodyMuted, maxLines = 2, overflow = TextOverflow.Ellipsis)
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(PcSpace.xs), verticalAlignment = Alignment.CenterVertically) {
            if (following) SecondaryButton(stringResource(R.string.action_following), onFollow, icon = Icons.Filled.Star)
            else PrimaryButton(stringResource(R.string.action_follow), onFollow, icon = Icons.Outlined.StarBorder)
            extra()
        }
    }
}

// ---------------- League detail ----------------

@Composable
fun LeagueRoute(back: () -> Unit, openMatch: (String) -> Unit, openTeam: (String) -> Unit, openPlayer: (String) -> Unit, vm: LeagueDetailViewModel = viewModel(factory = LeagueDetailViewModel.Factory)) {
    val state by vm.state.collectAsStateWithLifecycle()
    val refreshing by vm.refreshing.collectAsStateWithLifecycle()
    val refreshError by vm.refreshError.collectAsStateWithLifecycle()
    RefreshScreen(refreshing, vm::refresh, refreshError, vm::dismissRefreshError) {
        Row(Modifier.fillMaxWidth().padding(PcSpace.xs), verticalAlignment = Alignment.CenterVertically) { BackButton(back) }
        StateContent(state, vm::retry, loading = { DetailSkeleton() }) { d -> LeagueBody(d, openMatch, openTeam, openPlayer) }
    }
}

@Composable
fun LeagueBody(d: LeagueDetail, openMatch: (String) -> Unit, openTeam: (String) -> Unit, openPlayer: (String) -> Unit) {
    val app = LocalApp.current
    val prefs = LocalPrefs.current
    val l = d.league
    var tab by rememberSaveable { mutableIntStateOf(0) }
    EntityHeader({ LeagueBadge(l.short, 64) }, l.name, "${l.country} · ${l.season} · ${l.currentRound}", l.id in prefs.leagues, { app.toggleLeague(l.id, l.name) })
    Spacer(Modifier.height(PcSpace.sm))
    val tabs = buildList { if (d.standings.isNotEmpty()) add("Table"); add("Matches"); if (d.scorers.isNotEmpty()) add("Top scorers"); add("Teams") }
    val current = tabs.getOrElse(tab) { tabs.first() }
    UnderlineTabs(tabs, tabs.indexOf(current), { tab = it })
    Column(Modifier.padding(PcSpace.screen), verticalArrangement = Arrangement.spacedBy(PcSpace.sm)) {
        when (current) {
            "Table" -> {
                var view by rememberSaveable { mutableIntStateOf(0) }
                SegmentedTabs(listOf("Short", "Full", "Form"), view, { view = it })
                StandingsTable(d.standings, openTeam, compact = view != 1, showForm = view == 2)
            }
            "Matches" -> if (d.fixtures.isEmpty()) EmptyState("No fixtures this week", "Fixtures for the next round appear once they're scheduled.", icon = Icons.Outlined.EventBusy)
            else d.fixtures.groupBy { formatDay(it.kickoffMs) }.forEach { (day, ms) -> LeagueGroup(day, ms, openMatch, short = day.take(3)) }
            "Top scorers" -> PcCard(Modifier.fillMaxWidth()) { Column { d.scorers.forEachIndexed { i, s -> ScorerRow(s) { openPlayer(s.playerId) }; if (i < d.scorers.lastIndex) Hairline() } } }
            else -> {
                val teams = d.standings.map { it.team }.ifEmpty { d.fixtures.flatMap { listOf(it.home, it.away) }.distinctBy { it.id } }
                if (teams.isEmpty()) EmptyState("No teams listed", "Team lists appear when the season starts.", icon = Icons.Outlined.Groups)
                else PcCard(Modifier.fillMaxWidth()) {
                    Column { teams.forEachIndexed { i, t ->
                        FollowRow({ TeamCrest(t, CrestSize.M.dp) }, t.name, l.name, t.id in prefs.teams, { app.toggleTeam(t.id, t.name) }) { openTeam(t.id) }
                        if (i < teams.lastIndex) Hairline()
                    } }
                }
            }
        }
    }
}

@Composable
private fun ScorerRow(s: Scorer, onClick: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().heightIn(min = 64.dp).pcClickable(PcShape.Control, "Open ${s.name}", onClick = onClick).padding(horizontal = PcSpace.md)
            .semantics(mergeDescendants = true) { contentDescription = "${s.rank}, ${s.name}, ${s.team.name}, ${s.goals} goals, ${s.assists} assists" },
        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(PcSpace.sm),
    ) {
        Text(s.rank.toString(), style = PcType.Numeric.copy(fontWeight = FontWeight.Bold), color = Pc.Text2, modifier = Modifier.width(20.dp))
        PlayerAvatar(s.name, s.team.color, 40.dp)
        Column(Modifier.weight(1f)) {
            Text(s.name, style = PcType.BodyStrong, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text("${s.team.name} · ${s.assists} assists", style = PcType.Caption, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
        Text(s.goals.toString(), style = PcType.ScoreCompact)
    }
}

// ---------------- Team ----------------

@Composable
fun TeamRoute(back: () -> Unit, openMatch: (String) -> Unit, openTeam: (String) -> Unit, openPlayer: (String) -> Unit, vm: TeamViewModel = viewModel(factory = TeamViewModel.Factory)) {
    val state by vm.state.collectAsStateWithLifecycle()
    val refreshing by vm.refreshing.collectAsStateWithLifecycle()
    val refreshError by vm.refreshError.collectAsStateWithLifecycle()
    RefreshScreen(refreshing, vm::refresh, refreshError, vm::dismissRefreshError) {
        Row(Modifier.fillMaxWidth().padding(PcSpace.xs), verticalAlignment = Alignment.CenterVertically) { BackButton(back) }
        StateContent(state, vm::retry, loading = { DetailSkeleton() }) { d -> TeamBody(d, openMatch, openTeam, openPlayer) }
    }
}

@Composable
fun TeamBody(page: TeamPage, openMatch: (String) -> Unit, openTeam: (String) -> Unit, openPlayer: (String) -> Unit) {
    val app = LocalApp.current
    val prefs = LocalPrefs.current
    val d = page.detail
    val t = d.team
    val following = t.id in prefs.teams
    var tab by rememberSaveable { mutableIntStateOf(0) }
    EntityHeader({ TeamCrest(t, CrestSize.XL.dp) }, t.name, listOf(d.leagueName, d.city).filter { it.isNotBlank() }.joinToString(" · "), following, { app.toggleTeam(t.id, t.name) }) {
        if (following) {
            val shield = t.id in prefs.shielded
            Chip(if (shield) "Spoiler Shield on" else "Spoiler Shield off", shield, { app.toggleShield(t.id) })
        }
    }
    Spacer(Modifier.height(PcSpace.sm))
    val tabs = buildList { add("Overview"); add("Fixtures"); if (d.squad.isNotEmpty()) add("Squad"); if (page.standings.isNotEmpty()) add("Table") }
    val current = tabs.getOrElse(tab) { tabs.first() }
    UnderlineTabs(tabs, tabs.indexOf(current), { tab = it })
    Column(Modifier.padding(PcSpace.screen), verticalArrangement = Arrangement.spacedBy(PcSpace.gutter)) {
        when (current) {
            "Overview" -> {
                val next = page.fixtures.filter { it.status == com.innovativeinside.pitchside.data.model.Status.Upcoming }.minByOrNull { it.kickoffMs }
                val row = page.standings.firstOrNull { it.team.id == t.id }
                Row(Modifier.fillMaxWidth().height(IntrinsicSize.Min), horizontalArrangement = Arrangement.spacedBy(PcSpace.gutter)) {
                    if (row != null) BentoTile("League position", Modifier.weight(1f).fillMaxHeight(), onClick = { tab = tabs.indexOf("Table") }) {
                        Text("${row.rank}", style = PcType.ScoreCompact)
                        Text("${row.points} pts · ${row.played} played", style = PcType.Caption)
                        com.innovativeinside.pitchside.ui.components.FormDots(row.form)
                    }
                    if (next != null) BentoTile("Next match", Modifier.weight(1f).fillMaxHeight(), onClick = { openMatch(next.id) }) {
                        val opp = if (next.home.id == t.id) next.away else next.home
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(PcSpace.xs)) {
                            TeamCrest(opp, CrestSize.M.dp)
                            Text("${if (next.home.id == t.id) "vs" else "at"} ${opp.name}", style = PcType.LabelStrong, maxLines = 2, overflow = TextOverflow.Ellipsis)
                        }
                        Text(formatKickoff(next.kickoffMs), style = PcType.Caption)
                    }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(PcSpace.xs)) {
                    StatTile(if (d.capacity > 0) "%,d".format(d.capacity) else "-", "Capacity", Modifier.weight(1f))
                    StatTile(if (d.founded > 0) d.founded.toString() else "-", "Founded", Modifier.weight(1f))
                    StatTile(d.squad.size.toString(), "Squad", Modifier.weight(1f))
                }
                PcCard(Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(horizontal = PcSpace.md)) {
                        KeyValue("Coach", d.coach.ifBlank { "Not listed" })
                        Hairline()
                        KeyValue("Stadium", listOf(d.stadium, d.city).filter { it.isNotBlank() }.joinToString(", ").ifBlank { "Not listed" })
                    }
                }
            }
            "Fixtures" -> if (page.fixtures.isEmpty()) EmptyState("No matches this fortnight", "Upcoming fixtures appear once they're scheduled.", icon = Icons.Outlined.EventBusy)
            else groupByLeague(page.fixtures, prefs.leagues).forEach { (name, ms) -> LeagueGroup(name, ms, openMatch) }
            "Squad" -> Position.entries.forEach { pos ->
                val group = d.squad.filter { it.position == pos }.sortedBy { it.number }
                if (group.isNotEmpty()) {
                    Overline(pos.label)
                    PcCard(Modifier.fillMaxWidth()) {
                        Column { group.forEachIndexed { i, p ->
                            Row(
                                Modifier.fillMaxWidth().heightIn(min = 56.dp).pcClickable(PcShape.Control, "Open ${p.name}") { openPlayer(p.playerId) }.padding(horizontal = PcSpace.sm),
                                verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(PcSpace.sm),
                            ) {
                                Box(Modifier.size(32.dp).clip(CircleShape).background(Pc.SurfaceVariant), contentAlignment = Alignment.Center) { Text(p.number.toString(), style = PcType.Numeric.copy(fontWeight = FontWeight.Bold)) }
                                Text(p.name, style = PcType.BodyStrong, modifier = Modifier.weight(1f), maxLines = 1, overflow = TextOverflow.Ellipsis)
                                p.rating?.let { RatingPill(it) }
                                Icon(Icons.AutoMirrored.Outlined.KeyboardArrowRight, null, tint = Pc.Text3, modifier = Modifier.size(18.dp))
                            }
                            if (i < group.lastIndex) Hairline()
                        } }
                    }
                }
            }
            else -> StandingsTable(page.standings, openTeam, highlight = setOf(t.id), compact = true, showForm = true)
        }
    }
}

@Composable
private fun KeyValue(k: String, v: String) {
    Row(Modifier.fillMaxWidth().heightIn(min = 48.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(k, style = PcType.BodyMuted, modifier = Modifier.weight(1f))
        Text(v, style = PcType.BodyStrong, textAlign = TextAlign.End, maxLines = 2, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1.5f))
    }
}

@Composable
fun StatTile(value: String, label: String, modifier: Modifier, color: Color = Pc.Text) {
    PcCard(modifier.semantics(mergeDescendants = true) { contentDescription = "$label $value" }) {
        Column(Modifier.fillMaxWidth().padding(vertical = PcSpace.sm, horizontal = PcSpace.xs), horizontalAlignment = Alignment.CenterHorizontally) {
            Text(value, style = PcType.ScoreCompact, color = color, maxLines = 1)
            Text(label.uppercase(), style = PcType.Overline, maxLines = 1)
        }
    }
}

// ---------------- Player ----------------

@Composable
fun PlayerRoute(back: () -> Unit, openTeam: (String) -> Unit, vm: PlayerViewModel = viewModel(factory = PlayerViewModel.Factory)) {
    val state by vm.state.collectAsStateWithLifecycle()
    val refreshing by vm.refreshing.collectAsStateWithLifecycle()
    val refreshError by vm.refreshError.collectAsStateWithLifecycle()
    RefreshScreen(refreshing, vm::refresh, refreshError, vm::dismissRefreshError) {
        Row(Modifier.fillMaxWidth().padding(PcSpace.xs), verticalAlignment = Alignment.CenterVertically) { BackButton(back) }
        StateContent(state, vm::retry, loading = { DetailSkeleton() }) { p -> PlayerBody(p, openTeam) }
    }
}

@Composable
fun PlayerBody(p: PlayerProfile, openTeam: (String) -> Unit) {
    val app = LocalApp.current
    val prefs = LocalPrefs.current
    val s = p.summary
    var tab by rememberSaveable { mutableIntStateOf(0) }
    EntityHeader({ PlayerAvatar(s.name, s.team.color, 72.dp) }, s.name, "${s.position} · #${s.number} · ${s.nationality}", s.id in prefs.players, { app.togglePlayer(s.id, s.name) }) {
        Chip(s.team.name, false, { openTeam(s.team.id) })
    }
    Spacer(Modifier.height(PcSpace.sm))
    val tabs = buildList { add("Profile"); if (p.recentMatches.isNotEmpty()) add("Matches"); add("Stats"); if (p.career.isNotEmpty()) add("Career") }
    val current = tabs.getOrElse(tab) { tabs.first() }
    UnderlineTabs(tabs, tabs.indexOf(current), { tab = it })
    Column(Modifier.padding(PcSpace.screen), verticalArrangement = Arrangement.spacedBy(PcSpace.gutter)) {
        when (current) {
            "Profile" -> {
                FlowRow(horizontalArrangement = Arrangement.spacedBy(PcSpace.xs), verticalArrangement = Arrangement.spacedBy(PcSpace.xs)) {
                    listOf("Age ${p.age}", "${p.heightCm} cm", "${p.foot} foot", s.nationality, "Value ${p.marketValue}").forEach { Tag(it) }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(PcSpace.xs)) {
                    StatTile(p.goals.toString(), "Goals", Modifier.weight(1f)); StatTile(p.assists.toString(), "Assists", Modifier.weight(1f))
                    StatTile("%.1f".format(s.rating), "Rating", Modifier.weight(1f), Pc.Accent); StatTile(p.matches.toString(), "Apps", Modifier.weight(1f))
                }
                if (p.scoutingNote.isNotBlank()) BriefCard(p.scoutingNote, title = "Scouting note", compactMeta = true)
            }
            "Matches" -> PcCard(Modifier.fillMaxWidth()) {
                Column { p.recentMatches.forEachIndexed { i, m ->
                    Row(Modifier.fillMaxWidth().heightIn(min = 60.dp).padding(horizontal = PcSpace.sm), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(PcSpace.sm)) {
                        TeamCrest(m.opponent, CrestSize.M.dp)
                        Column(Modifier.weight(1f)) {
                            Text("${if (m.home) "vs" else "at"} ${m.opponent.name}", style = PcType.BodyStrong, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            Text("${formatDay(m.dateMs)} · ${m.score} · ${m.minutes}'", style = PcType.Caption)
                        }
                        RatingPill(m.rating)
                    }
                    if (i < p.recentMatches.lastIndex) Hairline()
                } }
            }
            "Stats" -> {
                var per90 by rememberSaveable { mutableIntStateOf(0) }
                SegmentedTabs(listOf("Season", "Per 90"), per90, { per90 = it })
                val mins = p.minutes.coerceAtLeast(1)
                PcCard(Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(PcSpace.md), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                        if (per90 == 0) {
                            Bar("Goals", p.goals.toDouble(), 15.0, "%.0f")
                            Bar("Assists", p.assists.toDouble(), 10.0, "%.0f")
                            Bar("xG", p.xg, 12.0, "%.1f")
                            Bar("Minutes", p.minutes.toDouble(), 1000.0, "%.0f")
                        } else {
                            Bar("Goals", p.goals * 90.0 / mins, 1.2, "%.2f")
                            Bar("Shots", p.shotsPer90, 5.0, "%.1f")
                            Bar("Key passes", p.keyPassesPer90, 4.0, "%.1f")
                            Bar("xG", p.xg * 90.0 / mins, 1.0, "%.2f")
                        }
                    }
                }
            }
            else -> PcCard(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(horizontal = PcSpace.md, vertical = PcSpace.xs)) {
                    p.career.forEachIndexed { i, c ->
                        Row(Modifier.fillMaxWidth().heightIn(min = 60.dp).padding(vertical = PcSpace.xs), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(PcSpace.sm)) {
                            Box(Modifier.size(10.dp).clip(CircleShape).background(if (c.current) Pc.Accent else Pc.OutlineStrong))
                            Column(Modifier.weight(1f)) { Text(c.club, style = PcType.BodyStrong); Text(c.years, style = PcType.Caption) }
                            Column(horizontalAlignment = Alignment.End) { Text("${c.apps} apps", style = PcType.Numeric, color = Pc.Text2); Text("${c.goals} goals", style = PcType.Numeric.copy(fontWeight = FontWeight.Bold), color = Pc.Accent) }
                        }
                        if (i < p.career.lastIndex) Hairline()
                    }
                }
            }
        }
    }
}

@Composable
private fun Tag(text: String) {
    Box(Modifier.heightIn(min = 32.dp).clip(PcShape.Chip).background(Pc.Surface).border(1.dp, Pc.Outline, PcShape.Chip).padding(horizontal = PcSpace.sm), contentAlignment = Alignment.Center) {
        Text(text, style = PcType.Label)
    }
}

@Composable
private fun Bar(label: String, value: Double, max: Double, fmt: String) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.semantics(mergeDescendants = true) { contentDescription = "$label ${fmt.format(value)}" }) {
        Text(label, style = PcType.Label, color = Pc.Text2, modifier = Modifier.width(88.dp))
        Box(Modifier.weight(1f).height(6.dp).clip(CircleShape).background(Pc.SurfaceVariant)) { Box(Modifier.fillMaxWidth((value / max).coerceIn(0.03, 1.0).toFloat()).height(6.dp).clip(CircleShape).background(Pc.Accent)) }
        Text(fmt.format(value), style = PcType.Numeric.copy(fontWeight = FontWeight.Bold), modifier = Modifier.width(48.dp), textAlign = TextAlign.End)
    }
}

@Preview(name = "Leagues · success", showBackground = true, widthDp = 380, heightDp = 1200)
@Composable
private fun LeaguesPreview() = PreviewHost(dark = false) {
    LeaguesContent(
        UiState.Success(LeaguesData(PreviewData.leagues, emptyList())), {}, {}, {}, {},
        selected = "pd", detail = UiState.Success(LeagueDetail(PreviewData.leagues.first(), PreviewData.table, PreviewData.matches, PreviewData.scorers)),
    )
}

@Preview(name = "Leagues · empty · dark", showBackground = true, widthDp = 380, backgroundColor = 0xFF0C0F0D)
@Composable
private fun LeaguesEmptyPreview() = PreviewHost(dark = true) { LeaguesContent(UiState.Empty, {}, {}, {}, {}) }
