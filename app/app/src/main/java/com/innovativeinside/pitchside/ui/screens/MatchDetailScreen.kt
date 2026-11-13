@file:OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class)

package com.innovativeinside.pitchside.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.outlined.Groups
import androidx.compose.material.icons.outlined.NotificationsNone
import androidx.compose.material.icons.outlined.SportsSoccer
import androidx.compose.material.icons.outlined.Style
import androidx.compose.material.icons.outlined.SwapVert
import androidx.compose.material.icons.outlined.TableChart
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalDensity
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
import com.innovativeinside.pitchside.core.UiState
import com.innovativeinside.pitchside.data.model.EventKind
import com.innovativeinside.pitchside.data.model.Lineup
import com.innovativeinside.pitchside.data.model.Match
import com.innovativeinside.pitchside.data.model.MatchEvent
import com.innovativeinside.pitchside.data.model.StatRow
import com.innovativeinside.pitchside.data.model.Status
import com.innovativeinside.pitchside.data.model.Team
import com.innovativeinside.pitchside.data.repository.MatchDetail
import com.innovativeinside.pitchside.ui.LocalApp
import com.innovativeinside.pitchside.ui.LocalPrefs
import com.innovativeinside.pitchside.ui.components.BackButton
import com.innovativeinside.pitchside.ui.components.BentoTile
import com.innovativeinside.pitchside.ui.components.BriefCard
import com.innovativeinside.pitchside.ui.components.CrestSize
import com.innovativeinside.pitchside.ui.components.DetailSkeleton
import com.innovativeinside.pitchside.ui.components.EmptyState
import com.innovativeinside.pitchside.ui.components.FormDots
import com.innovativeinside.pitchside.ui.components.Hairline
import com.innovativeinside.pitchside.ui.components.IconAction
import com.innovativeinside.pitchside.ui.components.LiveBadge
import com.innovativeinside.pitchside.ui.components.Overline
import com.innovativeinside.pitchside.ui.components.PcCard
import com.innovativeinside.pitchside.ui.components.PrimaryButton
import com.innovativeinside.pitchside.ui.components.ScoreText
import com.innovativeinside.pitchside.ui.components.SectionHeader
import com.innovativeinside.pitchside.ui.components.SegmentedTabs
import com.innovativeinside.pitchside.ui.components.StatBar
import com.innovativeinside.pitchside.ui.components.StateContent
import com.innovativeinside.pitchside.ui.components.TeamCrest
import com.innovativeinside.pitchside.ui.components.TextAction
import com.innovativeinside.pitchside.ui.components.UnderlineTabs
import com.innovativeinside.pitchside.ui.components.matchDescription
import com.innovativeinside.pitchside.ui.components.pcClickable
import com.innovativeinside.pitchside.ui.preview.PreviewData
import com.innovativeinside.pitchside.ui.preview.PreviewHost
import com.innovativeinside.pitchside.ui.theme.Pc
import com.innovativeinside.pitchside.ui.theme.PcShape
import com.innovativeinside.pitchside.ui.theme.PcSpace
import com.innovativeinside.pitchside.ui.theme.PcType
import com.innovativeinside.pitchside.ui.vm.MatchDetailViewModel

class MatchNav(
    val back: () -> Unit,
    val openLineup: (String) -> Unit,
    val openTeam: (String) -> Unit,
    val openPlayer: (String) -> Unit,
    val openLeague: (String) -> Unit,
)

private enum class MatchTab(val label: String) { Overview("Overview"), Timeline("Timeline"), Stats("Stats"), Lineups("Lineups"), Table("Table") }

/** Tabs backed by data for this match; Overview always exists. */
private fun tabsFor(d: MatchDetail): List<MatchTab> = buildList {
    add(MatchTab.Overview)
    if (d.events.isNotEmpty()) add(MatchTab.Timeline)
    if (d.stats.isNotEmpty()) add(MatchTab.Stats)
    if (d.lineups != null) add(MatchTab.Lineups)
    if (d.table.isNotEmpty()) add(MatchTab.Table)
}

@Composable
fun MatchRoute(nav: MatchNav, vm: MatchDetailViewModel = viewModel(factory = MatchDetailViewModel.Factory)) {
    val state by vm.state.collectAsStateWithLifecycle()
    val refreshing by vm.refreshing.collectAsStateWithLifecycle()
    val refreshError by vm.refreshError.collectAsStateWithLifecycle()
    val scroll = rememberScrollState()
    Box(Modifier.fillMaxSize()) {
        RefreshScreen(refreshing, vm::refresh, refreshError, vm::dismissRefreshError, maxWidth = 900, scrollState = scroll) {
            MatchContent(state, nav, vm::retry)
        }
        // Collapsing header: once the score stage scrolls away, a compact bar keeps the score in view.
        val density = LocalDensity.current
        val collapsed by remember { derivedStateOf { scroll.value > with(density) { 220.dp.toPx() } } }
        val match = (state as? UiState.Success)?.data?.match
        AnimatedVisibility(collapsed && match != null, enter = fadeIn(), exit = fadeOut()) {
            if (match != null) CompactMatchBar(match, nav)
        }
    }
}

@Composable
private fun CompactMatchBar(m: Match, nav: MatchNav) {
    val app = LocalApp.current
    val prefs = LocalPrefs.current
    val hidden = app.isHidden(prefs, m)
    Column(Modifier.fillMaxWidth().background(Pc.Hero).statusBarsPadding()) {
        Row(
            Modifier.fillMaxWidth().heightIn(min = 56.dp).padding(end = PcSpace.md).semantics(mergeDescendants = true) { contentDescription = matchDescription(m, hidden) },
            verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(PcSpace.xs),
        ) {
            IconAction(Icons.AutoMirrored.Outlined.ArrowBack, "Back", nav.back, tint = Pc.OnHero)
            TeamCrest(m.home, CrestSize.S.dp, onHero = true)
            Text(m.home.short, style = PcType.LabelStrong, color = Pc.OnHero)
            Text(if (m.hasScore && !hidden) "${m.homeScore} – ${m.awayScore}" else "v", style = PcType.ScoreRow, color = Pc.OnHero)
            Text(m.away.short, style = PcType.LabelStrong, color = Pc.OnHero)
            TeamCrest(m.away, CrestSize.S.dp, onHero = true)
            Spacer(Modifier.weight(1f))
            if (m.status == Status.Live) LiveBadge(m.label, onHero = true) else Text(if (m.status == Status.Finished) "FT" else m.label, style = PcType.LabelStrong, color = Pc.OnHero2)
        }
    }
}

@Composable
fun MatchContent(state: UiState<MatchDetail>, nav: MatchNav, onRetry: () -> Unit) {
    val app = LocalApp.current
    val prefs = LocalPrefs.current
    val match = (state as? UiState.Success)?.data?.match
    Row(Modifier.fillMaxWidth().padding(horizontal = PcSpace.xs, vertical = PcSpace.xxs), verticalAlignment = Alignment.CenterVertically) {
        BackButton(nav.back)
        Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
            if (match != null) {
                Text(
                    match.leagueName, style = PcType.LabelStrong,
                    modifier = Modifier.pcClickable(PcShape.Chip, "Open ${match.leagueName}") { nav.openLeague(match.leagueId) }.padding(horizontal = PcSpace.xs, vertical = 2.dp),
                    maxLines = 1, overflow = TextOverflow.Ellipsis,
                )
                Text(match.round, style = PcType.Caption, maxLines = 1)
            } else Text("Match", style = PcType.LabelStrong)
        }
        if (match != null && match.status != Status.Finished) {
            val on = match.id in prefs.matchAlerts
            IconAction(if (on) Icons.Filled.Notifications else Icons.Outlined.NotificationsNone, if (on) "Turn off match alerts" else "Turn on match alerts", { app.toggleMatchAlert(match.id) }, tint = if (on) Pc.Accent else Pc.Text2)
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
    val tabs = tabsFor(d)
    // Stats Nerd persona opens on Stats when there are stats; everyone else on Overview.
    var tabName by rememberSaveable { mutableStateOf(if (prefs.persona?.name == "Nerd" && d.stats.isNotEmpty()) MatchTab.Stats.name else MatchTab.Overview.name) }
    val tab = tabs.firstOrNull { it.name == tabName } ?: MatchTab.Overview
    MatchHeader(d, hidden, nav)
    Spacer(Modifier.height(PcSpace.xs))
    UnderlineTabs(tabs.map { it.label }, tabs.indexOf(tab), { tabName = tabs[it].name })
    Column(Modifier.padding(PcSpace.screen), verticalArrangement = Arrangement.spacedBy(PcSpace.gutter)) {
        when (tab) {
            MatchTab.Overview -> OverviewTab(d, hidden, onStats = { tabName = MatchTab.Stats.name }) { app.reveal(match.id) }
            MatchTab.Timeline -> TimelineTab(d, hidden) { app.reveal(match.id) }
            MatchTab.Stats -> StatsTab(d, hidden) { app.reveal(match.id) }
            MatchTab.Lineups -> LineupsTab(d, nav)
            MatchTab.Table -> {
                StandingsTable(d.table, nav.openTeam, highlight = setOf(match.home.id, match.away.id), compact = true, showForm = true)
                Text("Both clubs in this match are highlighted. Tap a club to open it.", style = PcType.Caption)
            }
        }
    }
}

/** The score stage: deep green, both clubs (tap to open), score with flash on change, status and venue. */
@Composable
private fun MatchHeader(d: MatchDetail, hidden: Boolean, nav: MatchNav) {
    val app = LocalApp.current
    val match = d.match
    PcCard(Modifier.padding(horizontal = PcSpace.screen).fillMaxWidth(), container = Pc.Hero, border = Pc.HeroOutline) {
        Column(Modifier.padding(PcSpace.md), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(PcSpace.xs)) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.Top) {
                HeaderTeam(match.home) { nav.openTeam(match.home.id) }
                Column(Modifier.widthIn(min = 120.dp).padding(top = PcSpace.xs), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(PcSpace.xs)) {
                    if (hidden) {
                        Text("– –", style = PcType.ScoreDisplay, color = Pc.OnHero2)
                        TextAction("Reveal score", { app.reveal(match.id) }, color = Pc.OnHero)
                    } else {
                        if (match.hasScore) ScoreText(
                            match.homeScore, match.awayScore, color = Pc.OnHero, flashKey = "${match.homeScore}-${match.awayScore}", flashColor = Pc.OnHero,
                            modifier = Modifier.semantics { contentDescription = "Score ${match.homeScore} to ${match.awayScore}" },
                        ) else Text(match.label, style = PcType.ScoreCompact, color = Pc.OnHero)
                        when (match.status) {
                            Status.Live -> LiveBadge(match.label, onHero = true)
                            Status.Finished -> Text("Full time", style = PcType.LabelStrong, color = Pc.OnHero2)
                            Status.Postponed -> Text("Postponed", style = PcType.LabelStrong, color = Pc.OnHero2)
                            Status.Upcoming -> Text(formatDay(match.kickoffMs), style = PcType.LabelStrong, color = Pc.OnHero2)
                        }
                    }
                }
                HeaderTeam(match.away) { nav.openTeam(match.away.id) }
            }
            if (!hidden && match.hasScore) {
                val goals = d.events.filter { it.kind == EventKind.Goal }.sortedBy { it.minute }
                if (goals.isNotEmpty()) Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(PcSpace.md)) {
                    Text(goals.filter { it.isHome }.joinToString("\n") { "${it.player.substringAfterLast(' ')} ${it.minuteLabel}" }, style = PcType.Caption, color = Pc.OnHero2, modifier = Modifier.weight(1f), textAlign = TextAlign.Center)
                    Icon(Icons.Outlined.SportsSoccer, null, tint = Pc.OnHero2, modifier = Modifier.size(14.dp))
                    Text(goals.filter { !it.isHome }.joinToString("\n") { "${it.player.substringAfterLast(' ')} ${it.minuteLabel}" }, style = PcType.Caption, color = Pc.OnHero2, modifier = Modifier.weight(1f), textAlign = TextAlign.Center)
                }
            }
            Text(listOf(match.venue, formatKickoff(match.kickoffMs)).filter { it.isNotBlank() }.joinToString(" · "), style = PcType.Caption, color = Pc.OnHero2, textAlign = TextAlign.Center)
        }
    }
}

@Composable
private fun RowScope.HeaderTeam(team: Team, onClick: () -> Unit) {
    Column(
        Modifier.weight(1f).pcClickable(PcShape.Control, "Open ${team.name}", onClick = onClick).padding(vertical = PcSpace.xxs),
        horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(PcSpace.xs),
    ) {
        TeamCrest(team, CrestSize.XL.dp, onHero = true)
        Text(team.name, style = PcType.BodyStrong, color = Pc.OnHero, maxLines = 2, overflow = TextOverflow.Ellipsis, textAlign = TextAlign.Center)
    }
}

@Composable
private fun SpoilerGate(what: String, onReveal: () -> Unit) =
    EmptyState("$what hidden by Spoiler Shield", "Reveal the score to see it. Your other shielded matches stay hidden.", actionLabel = "Reveal score", onAction = onReveal)

/** Overview bento: AI brief, key events, key stats, form, and match facts. */
@Composable
private fun OverviewTab(d: MatchDetail, hidden: Boolean, onStats: () -> Unit, onReveal: () -> Unit) {
    val m = d.match
    if (hidden) { SpoilerGate("Match overview", onReveal); return }
    if (m.brief.isNotBlank()) BriefCard(
        m.brief,
        title = when (m.status) { Status.Live -> "AI insight · live"; Status.Finished -> "AI insight · recap"; else -> "AI insight · preview" },
        updated = if (m.status == Status.Live) "Updates every 30 s" else "",
    )
    val key = d.events.filter { it.kind == EventKind.Goal || it.kind == EventKind.RedCard }
    val homeForm = d.table.firstOrNull { it.team.id == m.home.id }?.form.orEmpty()
    val awayForm = d.table.firstOrNull { it.team.id == m.away.id }?.form.orEmpty()
    val showForm = homeForm.isNotBlank() || awayForm.isNotBlank()
    if (key.isNotEmpty() || showForm) Row(Modifier.fillMaxWidth().height(IntrinsicSize.Min), horizontalArrangement = Arrangement.spacedBy(PcSpace.gutter)) {
        if (key.isNotEmpty()) BentoTile("Key events", Modifier.weight(1f).fillMaxHeight()) {
            key.take(5).forEach { e ->
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(e.minuteLabel, style = PcType.Numeric.copy(fontWeight = FontWeight.Bold), modifier = Modifier.width(36.dp))
                    EventIcon(e.kind, 16)
                    Text(e.player.substringAfterLast(' '), style = PcType.Label, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
            }
        }
        if (showForm) BentoTile("Form", Modifier.weight(1f).fillMaxHeight()) {
            listOf(m.home to homeForm, m.away to awayForm).forEach { (t, f) ->
                Text(t.name, style = PcType.LabelStrong, maxLines = 1, overflow = TextOverflow.Ellipsis)
                if (f.isNotBlank()) FormDots(f) else Text("No recent results", style = PcType.Caption)
            }
        }
    }
    if (d.stats.isNotEmpty()) {
        SectionHeader("Key numbers", "All stats", onStats)
        PcCard(Modifier.fillMaxWidth()) { Column(Modifier.padding(PcSpace.md), verticalArrangement = Arrangement.spacedBy(PcSpace.sm)) { d.stats.take(3).forEach { StatBar(it) } } }
    }
    PcCard(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(horizontal = PcSpace.md, vertical = PcSpace.xxs)) {
            InfoLine("Kickoff", formatKickoff(m.kickoffMs))
            InfoLine("Venue", m.venue.ifBlank { "To be confirmed" })
            InfoLine("Referee", m.referee ?: "Not announced yet")
            InfoLine("Round", m.round.ifBlank { "-" }, last = true)
        }
    }
}

@Composable
private fun InfoLine(label: String, value: String, last: Boolean = false) {
    Row(Modifier.fillMaxWidth().heightIn(min = 48.dp).semantics(mergeDescendants = true) {}, verticalAlignment = Alignment.CenterVertically) {
        Text(label, style = PcType.BodyMuted, modifier = Modifier.weight(1f))
        Text(value, style = PcType.BodyStrong, textAlign = TextAlign.End, maxLines = 2, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1.4f))
    }
    if (!last) Hairline()
}

@Composable
private fun EventIcon(kind: EventKind, size: Int = 18) {
    val (icon, tint) = when (kind) {
        EventKind.Goal -> Icons.Outlined.SportsSoccer to Pc.Accent
        EventKind.YellowCard -> Icons.Outlined.Style to Pc.Warning
        EventKind.RedCard -> Icons.Outlined.Style to Pc.Error
        EventKind.Sub -> Icons.Outlined.SwapVert to Pc.Text2
    }
    Icon(icon, null, tint = tint, modifier = Modifier.size(size.dp))
}

/** Vertical timeline: a centre rail with minute markers; home events on the left, away on the right. */
@Composable
private fun TimelineTab(d: MatchDetail, hidden: Boolean, onReveal: () -> Unit) {
    if (hidden) { SpoilerGate("Timeline", onReveal); return }
    PcCard(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(vertical = PcSpace.xs)) {
            d.events.sortedWith(compareBy({ it.minute }, { it.extra ?: 0 })).forEach { e -> EventRow(e) }
        }
    }
}

@Composable
private fun EventRow(e: MatchEvent) {
    val title = when (e.kind) { EventKind.Goal -> "Goal"; EventKind.YellowCard -> "Yellow card"; EventKind.RedCard -> "Red card"; EventKind.Sub -> "Substitution" }
    Row(
        Modifier.fillMaxWidth().height(IntrinsicSize.Min).heightIn(min = 60.dp).semantics(mergeDescendants = true) { contentDescription = "${e.minuteLabel}, $title, ${e.player}, ${e.detail}" },
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f).padding(horizontal = PcSpace.sm), horizontalAlignment = Alignment.End) { if (e.isHome) EventText(title, e.player, e.detail, true) }
        Box(Modifier.width(56.dp).fillMaxHeight(), contentAlignment = Alignment.Center) {
            Box(Modifier.width(2.dp).fillMaxHeight().background(Pc.Outline))
            Column(Modifier.clip(PcShape.Chip).background(Pc.SurfaceVariant).padding(horizontal = 6.dp, vertical = 4.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Text(e.minuteLabel, style = PcType.Numeric.copy(fontWeight = FontWeight.Bold))
                EventIcon(e.kind, 14)
            }
        }
        Column(Modifier.weight(1f).padding(horizontal = PcSpace.sm)) { if (!e.isHome) EventText(title, e.player, e.detail, false) }
    }
}

@Composable
private fun EventText(title: String, player: String, sub: String, end: Boolean) {
    Column(horizontalAlignment = if (end) Alignment.End else Alignment.Start) {
        Text(player, style = PcType.LabelStrong, textAlign = if (end) TextAlign.End else TextAlign.Start, maxLines = 2, overflow = TextOverflow.Ellipsis)
        Text(listOf(title, sub).filter { it.isNotBlank() }.joinToString(" · "), style = PcType.Caption, textAlign = if (end) TextAlign.End else TextAlign.Start, maxLines = 2)
    }
}

/** Paired bars, home (accent) against away (neutral). */
@Composable
private fun StatsTab(d: MatchDetail, hidden: Boolean, onReveal: () -> Unit) {
    if (hidden) { SpoilerGate("Stats", onReveal); return }
    PcCard(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(PcSpace.md), verticalArrangement = Arrangement.spacedBy(PcSpace.md)) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Row(Modifier.weight(1f), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Box(Modifier.size(10.dp).clip(CircleShape).background(Pc.Accent)); Text(d.match.home.name, style = PcType.LabelStrong, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
                Row(Modifier.weight(1f), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp, Alignment.End)) {
                    Text(d.match.away.name, style = PcType.LabelStrong, maxLines = 1, overflow = TextOverflow.Ellipsis); Box(Modifier.size(10.dp).clip(CircleShape).background(Pc.Neutral))
                }
            }
            d.stats.forEach { StatBar(it) }
        }
    }
}

@Composable
private fun LineupsTab(d: MatchDetail, nav: MatchNav) {
    val l = d.lineups ?: return
    Row(Modifier.fillMaxWidth().height(IntrinsicSize.Min), horizontalArrangement = Arrangement.spacedBy(PcSpace.gutter)) {
        listOf(d.match.home to l.home, d.match.away to l.away).forEach { (team, lineup) ->
            PcCard(Modifier.weight(1f).fillMaxHeight()) {
                Column(Modifier.padding(PcSpace.sm), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        TeamCrest(team, CrestSize.S.dp)
                        Text(team.short, style = PcType.LabelStrong)
                        Text(lineup.formation, style = PcType.Numeric, color = Pc.Text2)
                    }
                    Text("Coach ${lineup.coach}", style = PcType.Caption, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    lineup.starters.sortedBy { it.number }.forEach { p ->
                        Row(
                            Modifier.fillMaxWidth().heightIn(min = 40.dp).pcClickable(PcShape.Small, "Open ${p.name}") { nav.openPlayer(p.playerId) },
                            verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(PcSpace.xs),
                        ) {
                            Text(p.number.toString(), style = PcType.Numeric, color = Pc.Text2, modifier = Modifier.width(22.dp), textAlign = TextAlign.End)
                            Text(p.name, style = PcType.Label, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f))
                            if (p.scored) Icon(Icons.Outlined.SportsSoccer, "Scored", tint = Pc.Accent, modifier = Modifier.size(14.dp))
                        }
                    }
                }
            }
        }
    }
    PrimaryButton("Open pitch view", { nav.openLineup(d.match.id) }, Modifier.fillMaxWidth(), icon = Icons.Outlined.Groups)
}

// ---------------- Pitch view ----------------

@Composable
fun LineupRoute(back: () -> Unit, openPlayer: (String) -> Unit, vm: MatchDetailViewModel = viewModel(factory = MatchDetailViewModel.Factory)) {
    val state by vm.state.collectAsStateWithLifecycle()
    Screen(maxWidth = 640) {
        Row(Modifier.fillMaxWidth().padding(PcSpace.xs), verticalAlignment = Alignment.CenterVertically) {
            BackButton(back)
            Text("Lineups", style = PcType.Subtitle, modifier = Modifier.weight(1f).semantics { heading() })
        }
        StateContent(state, vm::retry, Modifier.padding(horizontal = PcSpace.screen), loading = { DetailSkeleton() }) { d ->
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
    val line = Pc.PitchLine
    Column(verticalArrangement = Arrangement.spacedBy(PcSpace.sm)) {
        SegmentedTabs(listOf("${d.match.home.short} · ${home.formation}", "${d.match.away.short} · ${away.formation}"), if (showHome) 0 else 1, { showHome = it == 0 })
        BoxWithConstraints(Modifier.fillMaxWidth().height(470.dp).clip(PcShape.Tile).background(Pc.Pitch).border(1.dp, Pc.HeroOutline, PcShape.Tile)) {
            val w = maxWidth
            val h = maxHeight
            Canvas(Modifier.fillMaxSize()) {
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
                    Modifier.offset(x = w * p.x - 32.dp, y = h * p.y - 26.dp).width(64.dp).pcClickable(PcShape.Small, "Open ${p.name}") { openPlayer(p.playerId) },
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    val gk = p.position.startsWith("G")
                    Box(
                        Modifier.size(34.dp).clip(CircleShape).background(if (gk) Pc.Warning else Pc.team(team.color)).border(2.dp, if (p.scored) Pc.AccentContainer else Pc.OnHero, CircleShape),
                        contentAlignment = Alignment.Center,
                    ) { Text(p.number.toString(), style = PcType.LabelStrong, color = Pc.OnTeam) }
                    Text(p.name.substringAfterLast(' '), style = PcType.Caption.copy(fontWeight = if (p.scored) FontWeight.Bold else FontWeight.Medium), color = Pc.OnHero, maxLines = 1)
                }
            }
        }
        Overline("Bench · coach ${l.coach}")
        FlowRow(horizontalArrangement = Arrangement.spacedBy(PcSpace.xs), verticalArrangement = Arrangement.spacedBy(PcSpace.xs)) {
            l.bench.forEach { b ->
                Box(Modifier.heightIn(min = 40.dp).clip(PcShape.Chip).background(Pc.Surface).border(1.dp, Pc.Outline, PcShape.Chip).pcClickable(PcShape.Chip, "Open ${b.name}") { openPlayer(b.playerId) }.padding(horizontal = PcSpace.sm), contentAlignment = Alignment.Center) {
                    Text("${b.number}  ${b.name}", style = PcType.Label)
                }
            }
        }
    }
}

// ---------------- Previews ----------------

private val previewDetail = MatchDetail(
    PreviewData.live,
    listOf(MatchEvent(23, null, true, EventKind.Goal, "Leo Marsh", "Assist T. Ellis"), MatchEvent(51, null, false, EventKind.Goal, "Sam Okafor", ""), MatchEvent(67, null, true, EventKind.Goal, "Theo Ellis", "Header")),
    listOf(StatRow("Possession", 58, 42, "58%", "42%"), StatRow("Shots", 8, 5, "8", "5"), StatRow("On target", 4, 2, "4", "2")),
    null, PreviewData.table,
)
private val previewNav = MatchNav({}, {}, {}, {}, {})

@Preview(name = "Match · light", showBackground = true, widthDp = 380, heightDp = 1200)
@Composable
private fun MatchPreview() = PreviewHost(dark = false) { Column { MatchContent(UiState.Success(previewDetail), previewNav) {} } }

@Preview(name = "Match · dark", showBackground = true, widthDp = 380, heightDp = 1200, backgroundColor = 0xFF0C0F0D)
@Composable
private fun MatchDarkPreview() = PreviewHost(dark = true) { Column { MatchContent(UiState.Success(previewDetail), previewNav) {} } }

@Preview(name = "Match · loading", showBackground = true, widthDp = 360)
@Composable
private fun MatchLoadingPreview() = PreviewHost(dark = false) { Column { MatchContent(UiState.Loading, previewNav) {} } }
