@file:OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class)

package com.innovativeinside.pitchside.ui.screens

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.outlined.Groups
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material.icons.outlined.QueryStats
import androidx.compose.material.icons.outlined.SportsSoccer
import androidx.compose.material.icons.outlined.Style
import androidx.compose.material.icons.outlined.SwapVert
import androidx.compose.material.icons.outlined.TableChart
import androidx.compose.material.icons.outlined.Timeline
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.innovativeinside.pitchside.core.UiState
import com.innovativeinside.pitchside.data.model.EventKind
import com.innovativeinside.pitchside.data.model.Lineup
import com.innovativeinside.pitchside.data.model.MatchEvent
import com.innovativeinside.pitchside.data.model.Status
import com.innovativeinside.pitchside.data.repository.MatchDetail
import com.innovativeinside.pitchside.ui.LocalApp
import com.innovativeinside.pitchside.ui.LocalPrefs
import com.innovativeinside.pitchside.ui.components.BackButton
import com.innovativeinside.pitchside.ui.components.BriefCard
import com.innovativeinside.pitchside.ui.components.Crest
import com.innovativeinside.pitchside.ui.components.DetailSkeleton
import com.innovativeinside.pitchside.ui.components.EmptyState
import com.innovativeinside.pitchside.ui.components.IconAction
import com.innovativeinside.pitchside.ui.components.PcCard
import com.innovativeinside.pitchside.ui.components.PrimaryButton
import com.innovativeinside.pitchside.ui.components.SectionHeader
import com.innovativeinside.pitchside.ui.components.SegmentedTabs
import com.innovativeinside.pitchside.ui.components.StatBar
import com.innovativeinside.pitchside.ui.components.StateContent
import com.innovativeinside.pitchside.ui.components.StatusBadge
import com.innovativeinside.pitchside.ui.components.TextAction
import com.innovativeinside.pitchside.ui.components.UnderlineTabs
import com.innovativeinside.pitchside.ui.components.pcClickable
import com.innovativeinside.pitchside.ui.preview.PreviewData
import com.innovativeinside.pitchside.ui.preview.PreviewHost
import com.innovativeinside.pitchside.ui.theme.Pc
import com.innovativeinside.pitchside.ui.theme.PcShape
import com.innovativeinside.pitchside.ui.theme.PcType
import com.innovativeinside.pitchside.ui.vm.MatchDetailViewModel

class MatchNav(
    val back: () -> Unit,
    val openLineup: (String) -> Unit,
    val openTeam: (String) -> Unit,
    val openPlayer: (String) -> Unit,
    val openLeague: (String) -> Unit,
)

@Composable
fun MatchRoute(nav: MatchNav, vm: MatchDetailViewModel = viewModel(factory = MatchDetailViewModel.Factory)) {
    val state by vm.state.collectAsStateWithLifecycle()
    val refreshing by vm.refreshing.collectAsStateWithLifecycle()
    val refreshError by vm.refreshError.collectAsStateWithLifecycle()
    RefreshScreen(refreshing, vm::refresh, refreshError, vm::dismissRefreshError, maxWidth = 900) {
        MatchContent(state, nav, vm::retry)
    }
}

@Composable
fun MatchContent(state: UiState<MatchDetail>, nav: MatchNav, onRetry: () -> Unit) {
    val app = LocalApp.current
    val prefs = LocalPrefs.current
    val match = (state as? UiState.Success)?.data?.match
    Row(Modifier.fillMaxWidth().padding(start = 8.dp, end = 8.dp, top = 8.dp), verticalAlignment = Alignment.CenterVertically) {
        BackButton(nav.back)
        Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
            if (match != null) {
                Text(match.leagueName, style = PcType.Label.copy(fontSize = 15.sp, fontWeight = FontWeight.ExtraBold), modifier = Modifier.pcClickable(PcShape.Chip, "Open ${match.leagueName}") { nav.openLeague(match.leagueId) }.padding(horizontal = 6.dp))
                Text(listOf(match.round, match.venue).filter { it.isNotBlank() }.joinToString(" · "), style = PcType.Caption.copy(color = Pc.Text2), maxLines = 1)
            } else Text("Match", style = PcType.Label)
        }
        if (match != null && match.status != Status.Finished) {
            val on = match.id in prefs.matchAlerts
            IconAction(if (on) Icons.Filled.Notifications else Icons.Outlined.Notifications, if (on) "Turn off match alerts" else "Turn on match alerts", { app.toggleMatchAlert(match.id) }, tint = if (on) Pc.Accent else Pc.Text2)
        } else Spacer(Modifier.width(48.dp))
    }
    StateContent(state, onRetry, loading = { DetailSkeleton() }) { d -> MatchBody(d, nav) }
}

@Composable
private fun MatchBody(d: MatchDetail, nav: MatchNav) {
    val app = LocalApp.current
    val prefs = LocalPrefs.current
    val match = d.match
    val hidden = app.isHidden(prefs, match)
    // Stats Nerd persona opens on Stats, everyone else on the Brief.
    var tab by rememberSaveable { mutableIntStateOf(if (prefs.persona?.name == "Nerd" && match.status != Status.Upcoming) 2 else 0) }
    PcCard(Modifier.padding(horizontal = 16.dp, vertical = 8.dp).fillMaxWidth(), shape = PcShape.Hero) {
        Column(Modifier.padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.Top) {
                TeamHead(match.home, true) { nav.openTeam(match.home.id) }
                Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (hidden) {
                        Text("– –", style = PcType.ScoreDisplay.copy(fontSize = 56.sp, lineHeight = 56.sp, color = Pc.Text3))
                        TextAction("Reveal score", { app.reveal(match.id) })
                    } else {
                        Text(
                            if (match.hasScore) "${match.homeScore} – ${match.awayScore}" else match.label,
                            style = if (match.hasScore) PcType.ScoreDisplay.copy(fontSize = 56.sp, lineHeight = 56.sp) else PcType.ScoreCompact,
                            modifier = Modifier.semantics { contentDescription = if (match.hasScore) "Score ${match.homeScore} to ${match.awayScore}" else "Kicks off ${match.label}" },
                        )
                        StatusBadge(match.status, match.label)
                    }
                }
                TeamHead(match.away, false) { nav.openTeam(match.away.id) }
            }
            if (!hidden && match.hasScore) {
                val goals = d.events.filter { it.kind == EventKind.Goal }.sortedBy { it.minute }
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text(goals.filter { it.isHome }.joinToString(" · ") { "${it.player.substringAfterLast(' ')} ${it.minuteLabel}" }, style = PcType.Caption.copy(color = Pc.Text2), modifier = Modifier.weight(1f))
                    Text(goals.filter { !it.isHome }.joinToString(" · ") { "${it.player.substringAfterLast(' ')} ${it.minuteLabel}" }, style = PcType.Caption.copy(color = Pc.Text2), textAlign = TextAlign.End, modifier = Modifier.weight(1f))
                }
            }
            Text(formatKickoff(match.kickoffMs), style = PcType.Caption)
        }
    }
    UnderlineTabs(listOf("Brief", "Timeline", "Stats", "Lineups", "Table"), tab, { tab = it })
    Box(Modifier.fillMaxWidth().height(1.dp).background(Pc.Outline))
    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        when (tab) {
            0 -> BriefTab(d, hidden, onStats = { tab = 2 }) { app.reveal(match.id) }
            1 -> TimelineTab(d, hidden) { app.reveal(match.id) }
            2 -> StatsTab(d, hidden) { app.reveal(match.id) }
            3 -> LineupsTab(d, nav)
            else -> if (d.table.isEmpty()) EmptyState("No table for this competition", "${match.leagueName} is played as a knockout, so there is no league table.", icon = Icons.Outlined.TableChart)
            else {
                StandingsTable(d.table, nav.openTeam, highlight = setOf(match.home.id, match.away.id), compact = true)
                Text("Both clubs in this match are highlighted. Tap a club to open it.", style = PcType.Caption)
            }
        }
    }
}

@Composable
private fun androidx.compose.foundation.layout.RowScope.TeamHead(team: com.innovativeinside.pitchside.data.model.Team, home: Boolean, onClick: () -> Unit) {
    Column(
        Modifier.weight(1f).pcClickable(PcShape.Card, "Open ${team.name}", onClick = onClick).padding(vertical = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Crest(team, 60.dp)
        Text(team.name, style = PcType.Label.copy(fontSize = 15.sp, fontWeight = FontWeight.ExtraBold, color = if (home) Pc.Text else Pc.Text2), maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}

@Composable
private fun SpoilerGate(what: String, onReveal: () -> Unit) =
    EmptyState("$what hidden by Spoiler Shield", "Reveal the score to see it. Your other shielded matches stay hidden.", actionLabel = "Reveal score", onAction = onReveal)

@Composable
private fun BriefTab(d: MatchDetail, hidden: Boolean, onStats: () -> Unit, onReveal: () -> Unit) {
    val m = d.match
    if (hidden) { SpoilerGate("Match Brief", onReveal); return }
    if (m.brief.isNotBlank()) BriefCard(
        m.brief,
        title = when (m.status) { Status.Live -> "MATCH BRIEF · LIVE"; Status.Finished -> "MATCH BRIEF · RECAP"; else -> "MATCH BRIEF · PREVIEW" },
        updated = if (m.status == Status.Live) "Updates every 30 s" else "",
    )
    PcCard(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(horizontal = 16.dp, vertical = 4.dp)) {
            InfoLine("Kickoff", formatKickoff(m.kickoffMs))
            InfoLine("Venue", m.venue.ifBlank { "To be confirmed" })
            InfoLine("Referee", m.referee ?: "Not announced yet")
            InfoLine("Round", m.round.ifBlank { "-" }, last = true)
        }
    }
    if (d.stats.isNotEmpty()) {
        SectionHeader("Key numbers", "All stats", onStats)
        d.stats.take(3).forEach { StatBar(it) }
    }
}

@Composable
private fun InfoLine(label: String, value: String, last: Boolean = false) {
    Row(Modifier.fillMaxWidth().heightIn(min = 48.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(label, style = PcType.BodyMuted, modifier = Modifier.weight(1f))
        Text(value, style = PcType.Label.copy(fontSize = 14.sp), textAlign = TextAlign.End)
    }
    if (!last) Box(Modifier.fillMaxWidth().height(1.dp).background(Pc.Outline))
}

@Composable
private fun TimelineTab(d: MatchDetail, hidden: Boolean, onReveal: () -> Unit) {
    if (hidden) { SpoilerGate("Timeline", onReveal); return }
    if (d.events.isEmpty()) {
        EmptyState(
            if (d.match.status == Status.Upcoming || d.match.status == Status.Postponed) "No events yet" else "A quiet one so far",
            if (d.match.status == Status.Upcoming) "Goals, cards and substitutions appear here once the match kicks off." else "Goals, cards and substitutions appear here as they happen.",
            icon = Icons.Outlined.Timeline,
        )
        return
    }
    Column {
        d.events.forEach { e -> EventRow(e) ; Box(Modifier.fillMaxWidth().height(1.dp).background(Pc.Outline)) }
    }
}

@Composable
private fun EventRow(e: MatchEvent) {
    val (icon, tint, title) = when (e.kind) {
        EventKind.Goal -> Triple(Icons.Outlined.SportsSoccer, Pc.Accent, "Goal")
        EventKind.YellowCard -> Triple(Icons.Outlined.Style, Pc.Warning, "Yellow card")
        EventKind.RedCard -> Triple(Icons.Outlined.Style, Pc.Error, "Red card")
        EventKind.Sub -> Triple(Icons.Outlined.SwapVert, Pc.Text2, "Substitution")
    }
    Row(
        Modifier.fillMaxWidth().heightIn(min = 64.dp).padding(vertical = 4.dp).semantics(mergeDescendants = true) { contentDescription = "${e.minuteLabel}, $title, ${e.player}, ${e.detail}" },
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f).padding(end = 8.dp), horizontalAlignment = Alignment.End) { if (e.isHome) EventText("$title · ${e.player}", e.detail, true) }
        Column(Modifier.width(56.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Text(e.minuteLabel, style = PcType.ScoreRow)
            Box(Modifier.size(24.dp).clip(CircleShape).background(Pc.SurfaceVariant), contentAlignment = Alignment.Center) { Icon(icon, null, tint = tint, modifier = Modifier.size(14.dp)) }
        }
        Column(Modifier.weight(1f).padding(start = 8.dp)) { if (!e.isHome) EventText("$title · ${e.player}", e.detail, false) }
    }
}

@Composable
private fun EventText(title: String, sub: String, end: Boolean) {
    Column(horizontalAlignment = if (end) Alignment.End else Alignment.Start) {
        Text(title, style = PcType.Label.copy(fontSize = 14.sp, fontWeight = FontWeight.ExtraBold), textAlign = if (end) TextAlign.End else TextAlign.Start)
        if (sub.isNotBlank()) Text(sub, style = PcType.Caption.copy(color = Pc.Text2), textAlign = if (end) TextAlign.End else TextAlign.Start)
    }
}

@Composable
private fun StatsTab(d: MatchDetail, hidden: Boolean, onReveal: () -> Unit) {
    if (hidden) { SpoilerGate("Stats", onReveal); return }
    if (d.stats.isEmpty()) {
        EmptyState("Stats start at kickoff", "Possession, shots and xG appear here once the match begins.", icon = Icons.Outlined.QueryStats)
        return
    }
    Row(Modifier.fillMaxWidth()) {
        Row(Modifier.weight(1f), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) { Box(Modifier.size(10.dp).clip(RoundedCornerShape(3.dp)).background(Pc.Accent)); Text(d.match.home.name, style = PcType.Label) }
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) { Text(d.match.away.name, style = PcType.Label.copy(color = Pc.Text2)); Box(Modifier.size(10.dp).clip(RoundedCornerShape(3.dp)).background(Pc.Neutral)) }
    }
    d.stats.forEach { StatBar(it) }
}

@Composable
private fun LineupsTab(d: MatchDetail, nav: MatchNav) {
    val l = d.lineups
    if (l == null) {
        EmptyState("Lineups not announced", "Teams usually confirm their lineups about an hour before kickoff.", icon = Icons.Outlined.Groups)
        return
    }
    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        listOf(d.match.home to l.home, d.match.away to l.away).forEach { (team, lineup) ->
            PcCard(Modifier.weight(1f)) {
                Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text("${team.name} · ${lineup.formation}", style = PcType.Label.copy(color = if (team == d.match.home) Pc.Accent else Pc.Text2), maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Text("Coach ${lineup.coach}", style = PcType.Caption)
                    lineup.starters.sortedBy { it.number }.forEach { p ->
                        Row(
                            Modifier.fillMaxWidth().heightIn(min = 40.dp).pcClickable(PcShape.Chip, "Open ${p.name}") { nav.openPlayer(p.playerId) },
                            verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            Text(p.number.toString(), style = PcType.Label.copy(color = Pc.Text3), modifier = Modifier.width(22.dp))
                            Text(p.name, style = PcType.Label.copy(fontWeight = FontWeight.SemiBold), maxLines = 1, overflow = TextOverflow.Ellipsis)
                        }
                    }
                }
            }
        }
    }
    PrimaryButton("Open pitch view", { nav.openLineup(d.match.id) }, Modifier.fillMaxWidth())
}

// ---------------- Pitch view ----------------

@Composable
fun LineupRoute(back: () -> Unit, openPlayer: (String) -> Unit, vm: MatchDetailViewModel = viewModel(factory = MatchDetailViewModel.Factory)) {
    val state by vm.state.collectAsStateWithLifecycle()
    Screen(maxWidth = 640) {
        Row(Modifier.fillMaxWidth().padding(8.dp), verticalAlignment = Alignment.CenterVertically) {
            BackButton(back)
            Text("Lineups", style = PcType.Subtitle, modifier = Modifier.weight(1f))
        }
        StateContent(state, vm::retry, Modifier.padding(horizontal = 16.dp), loading = { DetailSkeleton() }) { d ->
            val l = d.lineups
            if (l == null) EmptyState("Lineups not announced", "Check back about an hour before kickoff.", icon = Icons.Outlined.Groups)
            else PitchView(d, l.home, l.away, openPlayer)
        }
    }
}

@Composable
private fun PitchView(d: MatchDetail, home: Lineup, away: Lineup, openPlayer: (String) -> Unit) {
    var showHome by rememberSaveable { mutableStateOf(true) }
    val l = if (showHome) home else away
    val team = if (showHome) d.match.home else d.match.away
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        SegmentedTabs(listOf("${d.match.home.name} · ${home.formation}", "${d.match.away.name} · ${away.formation}"), if (showHome) 0 else 1, { showHome = it == 0 })
        BoxWithConstraints(Modifier.fillMaxWidth().height(470.dp).clip(PcShape.Hero).background(Color(0xFF12301F)).border(1.dp, Color(0xFF1E4A31), PcShape.Hero)) {
            val w = maxWidth
            val h = maxHeight
            Canvas(Modifier.fillMaxSize()) {
                val line = Color.White.copy(alpha = 0.28f)
                val st = Stroke(width = 2.dp.toPx())
                val m = 14.dp.toPx()
                drawRoundRect(line, Offset(m, m), Size(size.width - 2 * m, size.height - 2 * m), CornerRadius(4.dp.toPx()), st)
                drawLine(line, Offset(m, size.height / 2), Offset(size.width - m, size.height / 2), st.width)
                drawCircle(line, 44.dp.toPx(), Offset(size.width / 2, size.height / 2), style = st)
                drawRect(line, Offset(size.width * 0.26f, m), Size(size.width * 0.48f, 70.dp.toPx()), style = st)
                drawRect(line, Offset(size.width * 0.26f, size.height - m - 70.dp.toPx()), Size(size.width * 0.48f, 70.dp.toPx()), style = st)
            }
            l.starters.forEach { p ->
                Column(
                    Modifier.offset(x = w * p.x - 32.dp, y = h * p.y - 26.dp).width(64.dp).pcClickable(PcShape.Chip, "Open ${p.name}") { openPlayer(p.playerId) },
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    val gk = p.position.startsWith("G")
                    Box(Modifier.size(34.dp).clip(CircleShape).background(if (gk) Pc.Warning else Color(team.color)).border(2.dp, if (p.scored) Pc.Accent else Color.White, CircleShape), contentAlignment = Alignment.Center) {
                        Text(p.number.toString(), color = if (gk) Color(0xFF07111F) else Color.White, fontWeight = FontWeight.ExtraBold, fontSize = 13.sp)
                    }
                    Text(p.name.substringAfterLast(' '), color = Color.White, fontSize = 12.sp, fontWeight = if (p.scored) FontWeight.Bold else FontWeight.Medium, maxLines = 1)
                }
            }
        }
        Text("BENCH · COACH ${l.coach.uppercase()}", style = PcType.Overline.copy(color = Pc.Text2))
        androidx.compose.foundation.layout.FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            l.bench.forEach { b ->
                Box(Modifier.heightIn(min = 40.dp).clip(PcShape.Chip).background(Pc.SurfaceVariant).pcClickable(PcShape.Chip, "Open ${b.name}") { openPlayer(b.playerId) }.padding(horizontal = 12.dp), contentAlignment = Alignment.Center) {
                    Text("${b.number} ${b.name}", style = PcType.Label.copy(fontSize = 13.sp))
                }
            }
        }
    }
}

// ---------------- Previews ----------------

private val previewDetail = MatchDetail(PreviewData.live, emptyList(), emptyList(), null, PreviewData.table)
private val previewNav = MatchNav({}, {}, {}, {}, {})

@Preview(name = "Match · success", showBackground = true, backgroundColor = 0xFF07111F)
@Composable
private fun MatchPreview() = PreviewHost { Column { MatchContent(UiState.Success(previewDetail), previewNav) {} } }

@Preview(name = "Match · loading", showBackground = true, backgroundColor = 0xFF07111F)
@Composable
private fun MatchLoadingPreview() = PreviewHost { Column { MatchContent(UiState.Loading, previewNav) {} } }
