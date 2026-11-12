@file:OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class)

package com.innovativeinside.pitchside.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.EmojiEvents
import androidx.compose.material.icons.outlined.EventBusy
import androidx.compose.material.icons.outlined.Groups
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.StarBorder
import androidx.compose.material.icons.outlined.TableChart
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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
import com.innovativeinside.pitchside.ui.components.BackButton
import com.innovativeinside.pitchside.ui.components.BriefCard
import com.innovativeinside.pitchside.ui.components.Chip
import com.innovativeinside.pitchside.ui.components.Crest
import com.innovativeinside.pitchside.ui.components.DetailSkeleton
import com.innovativeinside.pitchside.ui.components.EmptyState
import com.innovativeinside.pitchside.ui.components.FormChip
import com.innovativeinside.pitchside.ui.components.IconAction
import com.innovativeinside.pitchside.ui.components.PcCard
import com.innovativeinside.pitchside.ui.components.SectionHeader
import com.innovativeinside.pitchside.ui.components.SegmentedTabs
import com.innovativeinside.pitchside.ui.components.StateContent
import com.innovativeinside.pitchside.ui.components.UnderlineTabs
import com.innovativeinside.pitchside.ui.components.pcClickable
import com.innovativeinside.pitchside.ui.preview.PreviewData
import com.innovativeinside.pitchside.ui.preview.PreviewHost
import com.innovativeinside.pitchside.ui.theme.Pc
import com.innovativeinside.pitchside.ui.theme.PcShape
import com.innovativeinside.pitchside.ui.theme.PcType
import com.innovativeinside.pitchside.ui.vm.LeagueDetailViewModel
import com.innovativeinside.pitchside.ui.vm.LeaguesData
import com.innovativeinside.pitchside.ui.vm.LeaguesViewModel
import com.innovativeinside.pitchside.ui.vm.PlayerViewModel
import com.innovativeinside.pitchside.ui.vm.TeamViewModel
import com.innovativeinside.pitchside.ui.vm.groupByLeague

// ---------------- Leagues tab ----------------

@Composable
fun LeaguesRoute(openLeague: (String) -> Unit, openPlayer: (String) -> Unit, openSearch: () -> Unit, vm: LeaguesViewModel = viewModel(factory = LeaguesViewModel.Factory)) {
    val state by vm.state.collectAsStateWithLifecycle()
    val refreshing by vm.refreshing.collectAsStateWithLifecycle()
    val refreshError by vm.refreshError.collectAsStateWithLifecycle()
    RefreshScreen(refreshing, vm::refresh, refreshError, vm::dismissRefreshError) {
        LeaguesContent(state, vm::retry, openLeague, openPlayer, openSearch)
    }
}

@Composable
fun LeaguesContent(state: UiState<LeaguesData>, onRetry: () -> Unit, openLeague: (String) -> Unit, openPlayer: (String) -> Unit, openSearch: () -> Unit) {
    val app = LocalApp.current
    val prefs = LocalPrefs.current
    Row(Modifier.fillMaxWidth().padding(start = 16.dp, end = 8.dp, top = 16.dp), verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            Text("Leagues", style = PcType.Title.copy(fontSize = 30.sp, lineHeight = 36.sp))
            Text("Tables, fixtures and top scorers", style = PcType.BodySmall)
        }
        IconAction(Icons.Outlined.Search, stringResource(R.string.action_search), openSearch, filled = true)
    }
    Spacer(Modifier.height(16.dp))
    StateContent(state, onRetry, Modifier.padding(horizontal = 16.dp), empty = { EmptyState("No leagues available", "Competitions appear here once the season data is loaded.", icon = Icons.Outlined.EmojiEvents) }) { d ->
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            d.leagues.sortedBy { if (it.id in prefs.leagues) 0 else 1 }.forEach { l ->
                LeagueRow(l, l.id in prefs.leagues, { openLeague(l.id) }) { app.toggleLeague(l.id, l.name) }
            }
            if (d.topPlayers.isNotEmpty()) {
                Spacer(Modifier.height(8.dp))
                SectionHeader("Top-rated players")
                d.topPlayers.forEach { p -> PlayerRow(p, { openPlayer(p.id) }) }
            }
        }
    }
}

@Composable
fun LeagueBadge(short: String, size: Int = 40) {
    Box(Modifier.size(size.dp).clip(PcShape.Control).background(Pc.Raised), contentAlignment = Alignment.Center) {
        Text(short, color = Pc.Lime, fontWeight = FontWeight.ExtraBold, fontSize = (size * 0.32f).sp)
    }
}

@Composable
fun LeagueRow(l: League, following: Boolean, onOpen: () -> Unit, onToggle: () -> Unit) {
    PcCard(Modifier.fillMaxWidth(), onClick = onOpen) {
        Row(Modifier.padding(12.dp).heightIn(min = 48.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            LeagueBadge(l.short)
            Column(Modifier.weight(1f)) { Text(l.name, style = PcType.Label.copy(fontSize = 15.sp)); Text("${l.country} · ${l.currentRound}", style = PcType.Meta.copy(color = Pc.Text2)) }
            Chip(if (following) stringResource(R.string.action_following) else stringResource(R.string.action_follow), following, onToggle)
        }
    }
}

@Composable
fun PlayerAvatar(name: String, color: Long, size: Int = 40) {
    val initials = name.split(" ").filter { it.isNotBlank() }.take(2).joinToString("") { it.first().uppercase() }
    Box(Modifier.size(size.dp).clip(CircleShape).background(Color(color)), contentAlignment = Alignment.Center) {
        Text(initials, color = Color.White, fontWeight = FontWeight.ExtraBold, fontSize = (size * 0.33f).sp)
    }
}

@Composable
fun PlayerRow(p: PlayerSummary, onClick: () -> Unit, trailing: @Composable () -> Unit = {
    Box(Modifier.heightIn(min = 28.dp).clip(PcShape.Badge).background(ratingColor(p.rating)).padding(horizontal = 8.dp), contentAlignment = Alignment.Center) {
        Text("%.1f".format(p.rating), color = Pc.OnLime, fontWeight = FontWeight.ExtraBold, fontSize = 13.sp)
    }
}) {
    PcCard(Modifier.fillMaxWidth(), onClick = onClick) {
        Row(Modifier.padding(12.dp).heightIn(min = 48.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            PlayerAvatar(p.name, p.team.color)
            Column(Modifier.weight(1f)) {
                Text(p.name, style = PcType.Label.copy(fontSize = 15.sp))
                Text("${p.position} · ${p.team.name} · ${p.nationality}", style = PcType.Meta.copy(color = Pc.Text2), maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
            trailing()
        }
    }
}

@Composable
fun ratingColor(r: Double): Color = when { r >= 8.0 -> Pc.Lime; r >= 7.0 -> Pc.Amber; else -> Pc.Neutral }

// ---------------- League detail ----------------

@Composable
fun LeagueRoute(back: () -> Unit, openMatch: (String) -> Unit, openTeam: (String) -> Unit, openPlayer: (String) -> Unit, vm: LeagueDetailViewModel = viewModel(factory = LeagueDetailViewModel.Factory)) {
    val state by vm.state.collectAsStateWithLifecycle()
    val refreshing by vm.refreshing.collectAsStateWithLifecycle()
    val refreshError by vm.refreshError.collectAsStateWithLifecycle()
    RefreshScreen(refreshing, vm::refresh, refreshError, vm::dismissRefreshError) {
        Row(Modifier.fillMaxWidth().padding(8.dp), verticalAlignment = Alignment.CenterVertically) { BackButton(back) }
        StateContent(state, vm::retry, loading = { DetailSkeleton() }) { d -> LeagueBody(d, openMatch, openTeam, openPlayer) }
    }
}

@Composable
fun LeagueBody(d: LeagueDetail, openMatch: (String) -> Unit, openTeam: (String) -> Unit, openPlayer: (String) -> Unit) {
    val app = LocalApp.current
    val prefs = LocalPrefs.current
    val l = d.league
    val following = l.id in prefs.leagues
    var tab by rememberSaveable { mutableIntStateOf(0) }
    Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        LeagueBadge(l.short, 52)
        Column(Modifier.weight(1f)) {
            Text(l.name, style = PcType.Title.copy(fontSize = 24.sp, lineHeight = 28.sp))
            Text("${l.country} · ${l.season} · ${l.currentRound}", style = PcType.BodySmall.copy(fontSize = 13.sp))
        }
        IconAction(if (following) Icons.Filled.Star else Icons.Outlined.StarBorder, if (following) "Unfollow ${l.name}" else "Follow ${l.name}", { app.toggleLeague(l.id, l.name) }, tint = if (following) Pc.Lime else Pc.Text2, filled = true)
    }
    Spacer(Modifier.height(12.dp))
    UnderlineTabs(listOf("Table", "Matches", "Top scorers", "Teams"), tab, { tab = it })
    Box(Modifier.fillMaxWidth().height(1.dp).background(Pc.Border))
    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        when (tab) {
            0 -> if (d.standings.isEmpty()) EmptyState("Knockout competition", "${l.name} has no league table. Check Matches for the draw and results.", icon = Icons.Outlined.TableChart)
            else {
                var view by rememberSaveable { mutableIntStateOf(0) }
                SegmentedTabs(listOf("Short", "Full", "Form"), view, { view = it })
                StandingsTable(d.standings, openTeam, compact = view == 0, showForm = view == 2)
                Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) { Legend(Pc.Lime, "Your club"); Legend(Pc.Red, "Negative goal difference") }
            }
            1 -> if (d.fixtures.isEmpty()) EmptyState("No fixtures this week", "Fixtures for the next round appear once they're scheduled.", icon = Icons.Outlined.EventBusy)
            else d.fixtures.groupBy { formatDay(it.kickoffMs) }.forEach { (day, ms) -> LeagueGroup(day, ms, openMatch) }
            2 -> if (d.scorers.isEmpty()) EmptyState("No goals yet", "Top scorers appear after the first round.", icon = Icons.Outlined.EmojiEvents)
            else d.scorers.forEach { s -> ScorerRow(s) { openPlayer(s.playerId) } }
            else -> {
                val teams = d.standings.map { it.team }.ifEmpty { d.fixtures.flatMap { listOf(it.home, it.away) }.distinctBy { it.id } }
                if (teams.isEmpty()) EmptyState("No teams listed", "Team lists appear when the season starts.", icon = Icons.Outlined.Groups)
                teams.forEach { t ->
                    val on = t.id in prefs.teams
                    PcCard(Modifier.fillMaxWidth(), onClick = { openTeam(t.id) }) {
                        Row(Modifier.padding(12.dp).heightIn(min = 48.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            Crest(t, 40.dp)
                            Text(t.name, style = PcType.Label.copy(fontSize = 15.sp), modifier = Modifier.weight(1f))
                            Chip(if (on) stringResource(R.string.action_following) else stringResource(R.string.action_follow), on, { app.toggleTeam(t.id, t.name) })
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ScorerRow(s: Scorer, onClick: () -> Unit) {
    PcCard(Modifier.fillMaxWidth(), onClick = onClick) {
        Row(Modifier.fillMaxWidth().heightIn(min = 64.dp).padding(horizontal = 16.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(s.rank.toString(), style = PcType.Label.copy(color = Pc.Text2), modifier = Modifier.width(20.dp))
            PlayerAvatar(s.name, s.team.color)
            Column(Modifier.weight(1f)) { Text(s.name, style = PcType.Label.copy(fontSize = 15.sp)); Text("${s.team.name} · ${s.assists} assists", style = PcType.BodySmall.copy(fontSize = 13.sp)) }
            Text(s.goals.toString(), style = PcType.ScoreL.copy(fontSize = 32.sp))
        }
    }
}

@Composable
private fun Legend(color: Color, label: String) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        Box(Modifier.size(12.dp).clip(PcShape.Badge).background(color))
        Text(label, style = PcType.Meta.copy(color = Pc.Text2))
    }
}

// ---------------- Team ----------------

@Composable
fun TeamRoute(back: () -> Unit, openMatch: (String) -> Unit, openTeam: (String) -> Unit, openPlayer: (String) -> Unit, vm: TeamViewModel = viewModel(factory = TeamViewModel.Factory)) {
    val state by vm.state.collectAsStateWithLifecycle()
    val refreshing by vm.refreshing.collectAsStateWithLifecycle()
    val refreshError by vm.refreshError.collectAsStateWithLifecycle()
    RefreshScreen(refreshing, vm::refresh, refreshError, vm::dismissRefreshError) {
        Row(Modifier.fillMaxWidth().padding(8.dp), verticalAlignment = Alignment.CenterVertically) { BackButton(back) }
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
    Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        Crest(t, 64.dp)
        Column(Modifier.weight(1f)) {
            Text(t.name, style = PcType.Title.copy(fontSize = 24.sp, lineHeight = 28.sp))
            Text(d.leagueName, style = PcType.BodySmall.copy(fontSize = 13.sp))
        }
    }
    Spacer(Modifier.height(12.dp))
    Row(Modifier.padding(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Chip(if (following) stringResource(R.string.action_following) else stringResource(R.string.action_follow), following, { app.toggleTeam(t.id, t.name) })
        if (following) {
            val shield = t.id in prefs.shielded
            Chip(if (shield) "Spoiler Shield on" else "Spoiler Shield off", shield, { app.toggleShield(t.id) })
        }
    }
    Spacer(Modifier.height(8.dp))
    UnderlineTabs(listOf("Matches", "Overview", "Squad", "Table"), tab, { tab = it })
    Box(Modifier.fillMaxWidth().height(1.dp).background(Pc.Border))
    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        when (tab) {
            0 -> if (page.fixtures.isEmpty()) EmptyState("No matches this fortnight", "Upcoming fixtures appear once they're scheduled.", icon = Icons.Outlined.EventBusy)
            else groupByLeague(page.fixtures, prefs.leagues).forEach { (name, ms) -> LeagueGroup(name, ms, openMatch) }
            1 -> PcCard(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    KeyValue("Coach", d.coach.ifBlank { "Not listed" })
                    KeyValue("Stadium", listOf(d.stadium, d.city).filter { it.isNotBlank() }.joinToString(", ").ifBlank { "Not listed" })
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        StatTile(if (d.capacity > 0) "%,d".format(d.capacity) else "-", "Capacity", Modifier.weight(1f))
                        StatTile(if (d.founded > 0) d.founded.toString() else "-", "Founded", Modifier.weight(1f))
                        StatTile(d.squad.size.toString(), "Squad", Modifier.weight(1f))
                    }
                }
            }
            2 -> if (d.squad.isEmpty()) EmptyState("Squad not published", "The squad list appears once the club registers it.", icon = Icons.Outlined.Groups)
            else Position.entries.forEach { pos ->
                val group = d.squad.filter { it.position == pos }.sortedBy { it.number }
                if (group.isNotEmpty()) {
                    Text(pos.label.uppercase(), style = PcType.Overline.copy(color = Pc.Text3))
                    group.forEach { p ->
                        PcCard(Modifier.fillMaxWidth(), onClick = { openPlayer(p.playerId) }) {
                            Row(Modifier.padding(12.dp).heightIn(min = 40.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                Box(Modifier.size(32.dp).clip(PcShape.Badge).background(Pc.Raised), contentAlignment = Alignment.Center) { Text(p.number.toString(), style = PcType.Label) }
                                Text(p.name, style = PcType.Label.copy(fontSize = 15.sp), modifier = Modifier.weight(1f))
                                p.rating?.let { r -> Box(Modifier.heightIn(min = 26.dp).clip(PcShape.Badge).background(ratingColor(r)).padding(horizontal = 8.dp), contentAlignment = Alignment.Center) { Text("%.1f".format(r), color = Pc.OnLime, fontWeight = FontWeight.ExtraBold, fontSize = 13.sp) } }
                                Icon(Icons.Filled.ChevronRight, null, tint = Pc.Text3)
                            }
                        }
                    }
                }
            }
            else -> if (page.standings.isEmpty()) EmptyState("No table", "This club's competition has no league table right now.", icon = Icons.Outlined.TableChart)
            else StandingsTable(page.standings, openTeam, highlight = setOf(t.id), compact = true)
        }
    }
}

@Composable
private fun KeyValue(k: String, v: String) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Text(k, style = PcType.BodySmall, modifier = Modifier.weight(1f))
        Text(v, style = PcType.Label, textAlign = TextAlign.End)
    }
}

@Composable
fun StatTile(value: String, label: String, modifier: Modifier, color: Color = Pc.Text) {
    PcCard(modifier) { Column(Modifier.fillMaxWidth().padding(vertical = 12.dp), horizontalAlignment = Alignment.CenterHorizontally) { Text(value, style = PcType.ScoreL.copy(fontSize = 28.sp, color = color), maxLines = 1); Text(label, style = PcType.Meta) } }
}

// ---------------- Player ----------------

@Composable
fun PlayerRoute(back: () -> Unit, openTeam: (String) -> Unit, vm: PlayerViewModel = viewModel(factory = PlayerViewModel.Factory)) {
    val state by vm.state.collectAsStateWithLifecycle()
    val refreshing by vm.refreshing.collectAsStateWithLifecycle()
    val refreshError by vm.refreshError.collectAsStateWithLifecycle()
    RefreshScreen(refreshing, vm::refresh, refreshError, vm::dismissRefreshError) {
        Row(Modifier.fillMaxWidth().padding(8.dp), verticalAlignment = Alignment.CenterVertically) { BackButton(back) }
        StateContent(state, vm::retry, loading = { DetailSkeleton() }) { p -> PlayerBody(p, openTeam) }
    }
}

@Composable
fun PlayerBody(p: PlayerProfile, openTeam: (String) -> Unit) {
    val app = LocalApp.current
    val prefs = LocalPrefs.current
    val s = p.summary
    val following = s.id in prefs.players
    var tab by rememberSaveable { mutableIntStateOf(0) }
    Row(Modifier.padding(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(16.dp), verticalAlignment = Alignment.CenterVertically) {
        PlayerAvatar(s.name, s.team.color, 80)
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(s.name, style = PcType.Title)
            Text("${s.position} · #${s.number}", style = PcType.BodySmall)
            Text(s.team.name, style = PcType.Label.copy(color = Pc.Lime), modifier = Modifier.pcClickable(PcShape.Chip, "Open ${s.team.name}") { openTeam(s.team.id) }.padding(vertical = 8.dp))
        }
        IconAction(if (following) Icons.Filled.Star else Icons.Outlined.StarBorder, if (following) "Unfollow ${s.name}" else "Follow ${s.name}", { app.togglePlayer(s.id, s.name) }, tint = if (following) Pc.Lime else Pc.Text2, filled = true)
    }
    Spacer(Modifier.height(8.dp))
    UnderlineTabs(listOf("Profile", "Matches", "Stats", "Career"), tab, { tab = it })
    Box(Modifier.fillMaxWidth().height(1.dp).background(Pc.Border))
    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        when (tab) {
            0 -> {
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf("Age ${p.age}", "${p.heightCm} cm", "${p.foot} foot", s.nationality, "Value ${p.marketValue}").forEach { Tag(it) }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    StatTile(p.goals.toString(), "Goals", Modifier.weight(1f)); StatTile(p.assists.toString(), "Assists", Modifier.weight(1f))
                    StatTile("%.1f".format(s.rating), "Rating", Modifier.weight(1f), Pc.Lime); StatTile(p.matches.toString(), "Matches", Modifier.weight(1f))
                }
                if (p.scoutingNote.isNotBlank()) BriefCard(p.scoutingNote, title = "SCOUTING NOTE", updated = "")
            }
            1 -> if (p.recentMatches.isEmpty()) EmptyState("No recent matches", "Matches appear after this player's first appearance.", icon = Icons.Outlined.EventBusy)
            else p.recentMatches.forEach { m ->
                PcCard(Modifier.fillMaxWidth()) {
                    Row(Modifier.padding(12.dp).heightIn(min = 48.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        Crest(m.opponent, 32.dp)
                        Column(Modifier.weight(1f)) {
                            Text("${if (m.home) "vs" else "at"} ${m.opponent.name}", style = PcType.Label)
                            Text("${formatDay(m.dateMs)} · ${m.score} · ${m.minutes}'", style = PcType.Meta.copy(color = Pc.Text2))
                        }
                        Box(Modifier.heightIn(min = 28.dp).clip(PcShape.Badge).background(ratingColor(m.rating)).padding(horizontal = 8.dp), contentAlignment = Alignment.Center) {
                            Text("%.1f".format(m.rating), color = Pc.OnLime, fontWeight = FontWeight.ExtraBold, fontSize = 13.sp)
                        }
                    }
                }
            }
            2 -> {
                var per90 by rememberSaveable { mutableIntStateOf(0) }
                SegmentedTabs(listOf("Season", "Per 90"), per90, { per90 = it })
                val mins = p.minutes.coerceAtLeast(1)
                PcCard(Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                        if (per90 == 0) {
                            Bar("Goals", p.goals.toDouble(), 15.0, Pc.Lime, "%.0f")
                            Bar("Assists", p.assists.toDouble(), 10.0, Pc.Live, "%.0f")
                            Bar("xG", p.xg, 12.0, Pc.Lime, "%.1f")
                            Bar("Minutes", p.minutes.toDouble(), 1000.0, Pc.Neutral, "%.0f")
                        } else {
                            Bar("Goals", p.goals * 90.0 / mins, 1.2, Pc.Lime, "%.2f")
                            Bar("Shots", p.shotsPer90, 5.0, Pc.Lime, "%.1f")
                            Bar("Key passes", p.keyPassesPer90, 4.0, Pc.Live, "%.1f")
                            Bar("xG", p.xg * 90.0 / mins, 1.0, Pc.Lime, "%.2f")
                        }
                    }
                }
            }
            else -> PcCard(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
                    p.career.forEachIndexed { i, c ->
                        Row(Modifier.fillMaxWidth().heightIn(min = 64.dp).padding(vertical = 8.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            Box(Modifier.size(12.dp).clip(CircleShape).background(if (c.current) Pc.Lime else Pc.BorderStrong))
                            Column(Modifier.weight(1f)) { Text(c.club, style = PcType.Label.copy(fontSize = 15.sp)); Text(c.years, style = PcType.BodySmall.copy(fontSize = 13.sp)) }
                            Column(horizontalAlignment = Alignment.End) { Text("${c.apps} apps", style = PcType.BodySmall.copy(fontSize = 13.sp)); Text("${c.goals} goals", style = PcType.Label.copy(color = Pc.Lime, fontSize = 13.sp)) }
                        }
                        if (i < p.career.lastIndex) Box(Modifier.fillMaxWidth().height(1.dp).background(Pc.Border))
                    }
                }
            }
        }
    }
}

@Composable
private fun Tag(text: String) {
    Box(Modifier.heightIn(min = 28.dp).clip(PcShape.Chip).background(Pc.Surface).border(1.dp, Pc.Border, PcShape.Chip).padding(horizontal = 10.dp), contentAlignment = Alignment.Center) { Text(text, style = PcType.Meta.copy(color = Pc.Text)) }
}

@Composable
private fun Bar(label: String, value: Double, max: Double, color: Color, fmt: String) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        Text(label, style = PcType.BodySmall, modifier = Modifier.width(92.dp))
        Box(Modifier.weight(1f).height(8.dp).clip(CircleShape).background(Pc.Border)) { Box(Modifier.fillMaxWidth((value / max).coerceIn(0.03, 1.0).toFloat()).height(8.dp).clip(CircleShape).background(color)) }
        Text(fmt.format(value), style = PcType.Label, modifier = Modifier.width(48.dp), textAlign = TextAlign.End)
    }
}

@Preview(name = "Leagues · success", showBackground = true, backgroundColor = 0xFF07111F)
@Composable
private fun LeaguesPreview() = PreviewHost { Column { LeaguesContent(UiState.Success(LeaguesData(PreviewData.leagues, emptyList())), {}, {}, {}, {}) } }

@Preview(name = "Leagues · empty", showBackground = true, backgroundColor = 0xFF07111F)
@Composable
private fun LeaguesEmptyPreview() = PreviewHost { Column { LeaguesContent(UiState.Empty, {}, {}, {}, {}) } }
