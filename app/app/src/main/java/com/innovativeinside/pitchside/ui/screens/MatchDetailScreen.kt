@file:OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class)

package com.innovativeinside.pitchside.ui.screens

import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.sp
import androidx.compose.ui.platform.testTag
import com.innovativeinside.pitchside.ui.components.GlowCard
import com.innovativeinside.pitchside.ui.components.PillTabs
import com.innovativeinside.pitchside.ui.components.ScoreHero
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
import androidx.compose.material.icons.outlined.PlayCircle
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
import androidx.compose.ui.platform.LocalUriHandler
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
import com.innovativeinside.pitchside.data.model.Highlight
import com.innovativeinside.pitchside.data.model.MatchOdds
import com.innovativeinside.pitchside.data.model.WinProbability
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
import com.innovativeinside.pitchside.ui.components.CrestSize
import com.innovativeinside.pitchside.ui.components.DetailSkeleton
import com.innovativeinside.pitchside.ui.components.EmptyState
import com.innovativeinside.pitchside.ui.components.FormDots
import com.innovativeinside.pitchside.ui.components.FormChip
import com.innovativeinside.pitchside.ui.components.StandingsHeader
import com.innovativeinside.pitchside.ui.components.StandingsRow
import com.innovativeinside.pitchside.ui.components.PlayerAvatar
import com.innovativeinside.pitchside.data.model.FormText
import com.innovativeinside.pitchside.ui.components.Hairline
import com.innovativeinside.pitchside.ui.components.IconAction
import com.innovativeinside.pitchside.ui.components.LiveBadge
import com.innovativeinside.pitchside.ui.components.Overline
import com.innovativeinside.pitchside.ui.components.PcCard
import com.innovativeinside.pitchside.ui.components.PrimaryButton
import com.innovativeinside.pitchside.ui.components.ProbabilityBar
import com.innovativeinside.pitchside.ui.components.ScoreText
import com.innovativeinside.pitchside.ui.components.SectionHeader
import com.innovativeinside.pitchside.ui.components.SegmentedTabs
import com.innovativeinside.pitchside.ui.components.StatBar
import com.innovativeinside.pitchside.ui.components.StateContent
import com.innovativeinside.pitchside.ui.components.TeamCrest
import com.innovativeinside.pitchside.ui.components.TextAction
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

private enum class MatchTab(val label: String) { Overview("Overview"), Commentary("Commentary"), Timeline("Timeline"), Stats("Stats"), Lineups("Lineups"), Table("Table") }

/** Tabs backed by data for this match; Overview always exists. */
private fun tabsFor(d: MatchDetail): List<MatchTab> = buildList {
    add(MatchTab.Overview)
    if (d.commentary.isNotEmpty()) add(MatchTab.Commentary)
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
        val collapsed by remember { derivedStateOf { scroll.value > with(density) { 300.dp.toPx() } } }
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
    val glowLine = Pc.HeroOutline
    Column(Modifier.fillMaxWidth().background(Pc.Hero).statusBarsPadding().drawBehind { drawLine(glowLine, Offset(0f, size.height), Offset(size.width, size.height), 1.dp.toPx()) }) {
        Row(
            Modifier.fillMaxWidth().heightIn(min = 56.dp).padding(end = PcSpace.md).semantics(mergeDescendants = true) { contentDescription = matchDescription(m, hidden) },
            verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(PcSpace.xs),
        ) {
            IconAction(Icons.AutoMirrored.Outlined.ArrowBack, "Back", nav.back, tint = Pc.OnHero)
            TeamCrest(m.home, CrestSize.S.dp, onHero = true)
            Text(m.home.short.uppercase(), style = PcType.TeamNameSmall, color = Pc.OnHero)
            Text(if (m.hasScore && !hidden) "${m.homeScore} – ${m.awayScore}" else "v", style = PcType.ScoreRow.copy(fontSize = 22.sp), color = Pc.OnHero)
            Text(m.away.short.uppercase(), style = PcType.TeamNameSmall, color = Pc.OnHero)
            TeamCrest(m.away, CrestSize.S.dp, onHero = true)
            Spacer(Modifier.weight(1f))
            if (m.status == Status.Live) LiveBadge(m.label, onHero = true, compact = true) else Text(if (m.status == Status.Finished) "FT" else m.label, style = PcType.Tag, color = Pc.OnHero2)
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
                Row(
                    Modifier.pcClickable(PcShape.Chip, "Open ${match.leagueName}") { nav.openLeague(match.leagueId) }.padding(horizontal = PcSpace.xs, vertical = 2.dp),
                    verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    if (match.leagueLogo != null) LeagueBadge(match.leagueName.take(2).uppercase(), 22, match.leagueLogo)
                    Text(match.leagueName.uppercase(), style = PcType.Tag.copy(fontSize = 17.sp), maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
                Text(listOf(match.country, match.round).filter { it.isNotBlank() }.joinToString(" · ").uppercase(), style = PcType.Kicker.copy(fontSize = 11.sp), maxLines = 1, overflow = TextOverflow.Ellipsis)
            } else Text("MATCH", style = PcType.Tag.copy(fontSize = 17.sp))
        }
        if (match != null && match.status != Status.Finished) {
            val on = match.id in prefs.matchAlerts
            IconAction(if (on) Icons.Filled.Notifications else Icons.Outlined.NotificationsNone, if (on) "Turn off match alerts" else "Turn on match alerts", { app.toggleMatchAlert(match.id) }, tint = if (on) Pc.Accent else Pc.Text2, filled = true)
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
    Spacer(Modifier.height(PcSpace.sm))
    PillTabs(tabs.map { it.label }, tabs.indexOf(tab), { tabName = tabs[it].name }, Modifier.testTag("match_tabs"))
    Column(Modifier.padding(PcSpace.screen), verticalArrangement = Arrangement.spacedBy(PcSpace.gutter)) {
        when (tab) {
            MatchTab.Overview -> OverviewTab(d, hidden, onStats = { tabName = MatchTab.Stats.name }) { app.reveal(match.id) }
            MatchTab.Commentary -> CommentaryTab(d, hidden) { app.reveal(match.id) }
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

/**
 * The score hero on a [GlowCard]: both clubs (tap to open) with large crests and uppercase names, the animated
 * score with the LIVE minute / FT / kickoff day under it, the goalscorers, a divider and the possession / shots
 * strip when the source has stats, then venue and kickoff.
 */
@Composable
private fun MatchHeader(d: MatchDetail, hidden: Boolean, nav: MatchNav) {
    val app = LocalApp.current
    val match = d.match
    GlowCard(Modifier.padding(horizontal = PcSpace.screen).fillMaxWidth().testTag("match_header")) {
        Column(Modifier.padding(PcSpace.md), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(PcSpace.sm)) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.Top) {
                HeaderTeam(match.home) { nav.openTeam(match.home.id) }
                Column(Modifier.widthIn(min = 112.dp).padding(top = PcSpace.sm), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(PcSpace.xs)) {
                    if (hidden) {
                        Text("– –", style = PcType.ScoreDisplay, color = Pc.OnHero2)
                        TextAction("Reveal score", { app.reveal(match.id) }, color = Pc.HeroAccent)
                    } else {
                        if (match.hasScore) ScoreHero(
                            match.homeScore, match.awayScore,
                            modifier = Modifier.semantics { contentDescription = "Score ${match.homeScore} to ${match.awayScore}" },
                        ) else Text(match.label, style = PcType.ScoreCompact.copy(fontSize = 40.sp, lineHeight = 42.sp), color = Pc.OnHero)
                        when (match.status) {
                            Status.Live -> LiveBadge(match.label, onHero = true)
                            Status.Finished -> Text("FULL TIME", style = PcType.Tag, color = Pc.OnHero2)
                            Status.Postponed -> Text("POSTPONED", style = PcType.Tag, color = Pc.Warning)
                            Status.Upcoming -> Text(formatDay(match.kickoffMs).uppercase(), style = PcType.Tag, color = Pc.OnHero2)
                        }
                    }
                }
                HeaderTeam(match.away) { nav.openTeam(match.away.id) }
            }
            if (!hidden && match.hasScore) {
                val goals = d.events.filter { it.kind == EventKind.Goal }.sortedBy { it.minute }
                if (goals.isNotEmpty()) Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(PcSpace.md)) {
                    Text(goals.filter { it.isHome }.joinToString("\n") { "${it.player.substringAfterLast(' ')} ${it.minuteLabel}" }, style = PcType.Caption, color = Pc.OnHero2, modifier = Modifier.weight(1f), textAlign = TextAlign.Center)
                    Icon(Icons.Outlined.SportsSoccer, null, tint = Pc.HeroAccent, modifier = Modifier.size(14.dp))
                    Text(goals.filter { !it.isHome }.joinToString("\n") { "${it.player.substringAfterLast(' ')} ${it.minuteLabel}" }, style = PcType.Caption, color = Pc.OnHero2, modifier = Modifier.weight(1f), textAlign = TextAlign.Center)
                }
            }
            val strip = if (hidden) emptyList() else d.stats.filter { it.label == "Possession" || it.label == "Shots" }
            Hairline(color = Pc.OnHero.copy(alpha = 0.10f))
            if (strip.isNotEmpty()) Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(PcSpace.lg, Alignment.CenterHorizontally)) {
                strip.forEach { st ->
                    val t = androidx.compose.ui.text.buildAnnotatedString {
                        withStyle(androidx.compose.ui.text.SpanStyle(color = Pc.OnHero2, fontWeight = FontWeight.Medium)) { append(st.label.uppercase() + " ") }
                        withStyle(androidx.compose.ui.text.SpanStyle(color = Pc.OnHero, fontWeight = FontWeight.Bold)) { append("${st.homeText} - ${st.awayText}") }
                    }
                    Text(t, style = PcType.Tag.copy(fontSize = 16.sp), maxLines = 1)
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
        Text(team.name.uppercase(), style = PcType.TeamName, color = Pc.OnHero, maxLines = 2, overflow = TextOverflow.Ellipsis, textAlign = TextAlign.Center)
    }
}

@Composable
private fun SpoilerGate(what: String, onReveal: () -> Unit) =
    EmptyState("$what hidden by Spoiler Shield", "Reveal the score to see it. Your other shielded matches stay hidden.", actionLabel = "Reveal score", onAction = onReveal)

/** A titled card: the consistent building block of the Overview (title over a surface card). */
@Composable
private fun TitledCard(title: String, action: String? = null, onAction: () -> Unit = {}, content: @Composable () -> Unit) {
    SectionHeader(title, action, onAction)
    PcCard(Modifier.fillMaxWidth()) { content() }
}

/**
 * Overview as titled cards in reading order: what happened (key events), highlights, key numbers, win probability,
 * form, head to head, recent results, table snapshot, match info, and odds last. A card only appears when the source
 * has data for it, and the match info card only lists the facts that exist, so there are no empty boxes or dashes.
 */
@Composable
private fun OverviewTab(d: MatchDetail, hidden: Boolean, onStats: () -> Unit, onReveal: () -> Unit) {
    val m = d.match
    if (hidden) { SpoilerGate("Match overview", onReveal); return }
    val key = d.events.filter { it.kind == EventKind.Goal || it.kind == EventKind.RedCard }.sortedWith(compareBy({ it.minute }, { it.extra ?: 0 }))
    if (key.isNotEmpty()) TitledCard("Key events") {
        Column(Modifier.padding(vertical = PcSpace.xxs)) {
            key.forEachIndexed { i, e ->
                if (i > 0) Hairline(Modifier.padding(horizontal = PcSpace.md))
                KeyEventLine(e)
            }
        }
    }
    if (d.highlights.isNotEmpty()) HighlightsCard(d.highlights)
    if (d.stats.isNotEmpty()) TitledCard("Key numbers", "All stats", onStats) {
        Column(Modifier.padding(PcSpace.md), verticalArrangement = Arrangement.spacedBy(PcSpace.md)) { d.stats.take(4).forEach { StatBar(it) } }
    }
    d.prediction?.let { ProbabilityCard(m, it) }
    val homeForm = d.table.firstOrNull { it.team.id == m.home.id }?.form.orEmpty().ifBlank { FormText.formFrom(m.home.id, d.homeRecent) }
    val awayForm = d.table.firstOrNull { it.team.id == m.away.id }?.form.orEmpty().ifBlank { FormText.formFrom(m.away.id, d.awayRecent) }
    val forms = listOf(m.home to homeForm, m.away to awayForm).filter { it.second.isNotBlank() }
    if (forms.isNotEmpty()) TitledCard("Form · last 5") {
        Column(Modifier.padding(horizontal = PcSpace.md, vertical = PcSpace.xxs)) {
            forms.forEachIndexed { i, (t, f) ->
                if (i > 0) Hairline()
                Row(Modifier.fillMaxWidth().heightIn(min = 52.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(PcSpace.sm)) {
                    TeamCrest(t, CrestSize.S.dp)
                    Text(t.name, style = PcType.BodyStrong, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f))
                    FormDots(f.takeLast(5))
                }
            }
        }
    }
    if (d.h2h.isNotEmpty()) HeadToHeadCard(m, d.h2h)
    if (d.homeRecent.isNotEmpty() || d.awayRecent.isNotEmpty()) RecentResultsCard(m, d.homeRecent, d.awayRecent)
    if (d.table.isNotEmpty()) MiniTableCard(d)
    val info = buildList {
        add("Competition" to listOf(m.leagueName, m.country).filter { it.isNotBlank() }.joinToString(" · "))
        if (m.round.isNotBlank()) add("Round" to m.round)
        add("Kickoff" to formatKickoff(m.kickoffMs))
        if (m.venue.isNotBlank()) add("Venue" to m.venue)
        m.referee?.takeIf { it.isNotBlank() }?.let { add("Referee" to it) }
    }.filter { it.second.isNotBlank() }
    TitledCard("Match info") {
        Column(Modifier.padding(horizontal = PcSpace.md, vertical = PcSpace.xxs)) {
            info.forEachIndexed { i, (k, v) -> InfoLine(k, v, last = i == info.lastIndex) }
        }
    }
    d.odds?.let { OddsCard(it) }
}

/** One goal or red card: home events read from the left, away from the right, the minute in the middle. */
@Composable
private fun KeyEventLine(e: MatchEvent) {
    val what = if (e.kind == EventKind.Goal) "Goal" else "Red card"
    Row(
        Modifier.fillMaxWidth().heightIn(min = 44.dp).padding(horizontal = PcSpace.md).semantics(mergeDescendants = true) { contentDescription = "${e.minuteLabel}, $what, ${e.player}" },
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Row(Modifier.weight(1f), horizontalArrangement = Arrangement.spacedBy(6.dp, Alignment.End), verticalAlignment = Alignment.CenterVertically) {
            if (e.isHome) {
                Text(e.player, style = PcType.Label, maxLines = 1, overflow = TextOverflow.Ellipsis, textAlign = TextAlign.End, modifier = Modifier.weight(1f, fill = false))
                EventIcon(e.kind, 16)
            }
        }
        Text(e.minuteLabel, style = PcType.Numeric.copy(fontWeight = FontWeight.Bold), textAlign = TextAlign.Center, modifier = Modifier.width(56.dp))
        Row(Modifier.weight(1f), horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
            if (!e.isHome) {
                EventIcon(e.kind, 16)
                Text(e.player, style = PcType.Label, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f, fill = false))
            }
        }
    }
}

/** Both clubs' table positions side by side (Overview, when there are no key events yet). */
@Composable
private fun TableSpotTile(d: MatchDetail, modifier: Modifier) {
    val m = d.match
    BentoTile("League position", modifier) {
        listOf(m.home, m.away).forEach { t ->
            val r = d.table.firstOrNull { it.team.id == t.id }
            Row(verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(r?.let { FormText.ordinal(it.rank) } ?: "–", style = PcType.ScoreRow)
                Text(r?.let { "${it.points} pts" } ?: t.short, style = PcType.Caption, modifier = Modifier.padding(bottom = 2.dp))
            }
            Text(t.name, style = PcType.Caption, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
    }
}

/** The table around both clubs: each side with its neighbours above and below, so the stakes are visible. */
@Composable
private fun MiniTableCard(d: MatchDetail) {
    val m = d.match
    val idx = listOf(m.home.id, m.away.id).mapNotNull { id -> d.table.indexOfFirst { it.team.id == id }.takeIf { it >= 0 } }
    if (idx.isEmpty()) return
    val keep = idx.flatMap { (it - 1)..(it + 1) }.filter { it in d.table.indices }.toSortedSet().toList()
    SectionHeader("Table snapshot")
    PcCard(Modifier.fillMaxWidth()) {
        Column {
            StandingsHeader(showForm = true)
            keep.forEachIndexed { i, k ->
                if (i > 0 && k != keep[i - 1] + 1) Box(Modifier.fillMaxWidth().height(14.dp), contentAlignment = Alignment.Center) { Text("···", style = PcType.Caption) }
                else if (i > 0) Hairline()
                val r = d.table[k]
                StandingsRow(r, {}, highlight = r.team.id == m.home.id || r.team.id == m.away.id, showForm = true)
            }
        }
    }
}

/** Each side's latest results (from the head-to-head feed), newest first, with W/D/L chips. */
@Composable
private fun RecentResultsCard(m: Match, home: List<Match>, away: List<Match>) {
    SectionHeader("Recent results")
    Row(Modifier.fillMaxWidth().height(IntrinsicSize.Min), horizontalArrangement = Arrangement.spacedBy(PcSpace.gutter)) {
        listOf(m.home to home, m.away to away).filter { it.second.isNotEmpty() }.forEach { (team, games) ->
            PcCard(Modifier.weight(1f).fillMaxHeight()) {
                Column(Modifier.padding(PcSpace.sm), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        TeamCrest(team, 20.dp)
                        Text(team.short, style = PcType.LabelStrong, maxLines = 1)
                    }
                    games.filter { it.hasScore }.take(5).forEach { g ->
                        val home = g.home.id == team.id
                        val opp = if (home) g.away else g.home
                        val mine = if (home) g.homeScore else g.awayScore
                        val theirs = if (home) g.awayScore else g.homeScore
                        val res = if (mine == null || theirs == null) '-' else if (mine > theirs) 'W' else if (mine < theirs) 'L' else 'D'
                        Row(
                            Modifier.fillMaxWidth().semantics(mergeDescendants = true) { contentDescription = "${if (home) "versus" else "at"} ${opp.name}, $mine to $theirs" },
                            verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp),
                        ) {
                            if (res != '-') FormChip(res, 16.dp)
                            Text("${if (home) "v" else "@"} ${opp.short}", style = PcType.Label, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f))
                            Text("$mine–$theirs", style = PcType.Numeric.copy(fontWeight = FontWeight.Bold))
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ProbabilityCard(m: Match, p: WinProbability) {
    SectionHeader(if (m.status == Status.Upcoming) "Win probability" else "Pre-match win probability")
    PcCard(Modifier.fillMaxWidth()) {
        Column(
            Modifier.padding(PcSpace.md).semantics(mergeDescendants = true) {
                contentDescription = "Win probability: ${m.home.name} ${p.home}%, draw ${p.draw}%, ${m.away.name} ${p.away}%"
            },
            verticalArrangement = Arrangement.spacedBy(PcSpace.sm),
        ) {
            Row(Modifier.fillMaxWidth()) {
                ProbCell("${p.home}%", m.home.short, Modifier.weight(1f), TextAlign.Start)
                ProbCell("${p.draw}%", "Draw", Modifier.weight(1f), TextAlign.Center)
                ProbCell("${p.away}%", m.away.short, Modifier.weight(1f), TextAlign.End)
            }
            ProbabilityBar(p, Modifier.fillMaxWidth())
            val extra = listOfNotNull(p.over25?.let { "Over 2.5 goals $it%" }, p.btts?.let { "Both teams score $it%" })
            if (extra.isNotEmpty()) Text(extra.joinToString(" · "), style = PcType.Caption)
        }
    }
}

@Composable
private fun ProbCell(value: String, label: String, modifier: Modifier, align: TextAlign) {
    Column(modifier) {
        Text(value, style = PcType.Numeric.copy(fontWeight = FontWeight.Bold), textAlign = align, modifier = Modifier.fillMaxWidth())
        Text(label, style = PcType.Caption, textAlign = align, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.fillMaxWidth())
    }
}

private fun price(v: Double) = String.format(java.util.Locale.US, "%.2f", v)

@Composable
private fun OddsCard(o: MatchOdds) {
    SectionHeader(if (o.live) "Live odds · 1X2" else "Odds · 1X2")
    PcCard(Modifier.fillMaxWidth()) {
        Column(
            Modifier.padding(PcSpace.md).semantics(mergeDescendants = true) {
                contentDescription = "${if (o.live) "Live odds" else "Odds"}: home ${price(o.home)}, draw ${price(o.draw)}, away ${price(o.away)}"
            },
            verticalArrangement = Arrangement.spacedBy(PcSpace.sm),
        ) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(PcSpace.xs)) {
                listOf("1" to o.home, "X" to o.draw, "2" to o.away).forEach { (k, v) ->
                    Row(
                        Modifier.weight(1f).clip(PcShape.Control).background(Pc.SurfaceVariant).padding(horizontal = PcSpace.sm, vertical = PcSpace.xs),
                        horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(k, style = PcType.Caption)
                        Text(price(v), style = PcType.Numeric.copy(fontWeight = FontWeight.SemiBold))
                    }
                }
            }
            Text(if (o.live) "In-play decimal prices" else "Decimal prices · ${o.bookmaker}", style = PcType.Caption)
        }
    }
}

@Composable
private fun HighlightsCard(clips: List<Highlight>) {
    val uri = LocalUriHandler.current
    SectionHeader("Highlights")
    PcCard(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(horizontal = PcSpace.md, vertical = PcSpace.xxs)) {
            clips.forEachIndexed { i, c ->
                Row(
                    Modifier.fillMaxWidth().heightIn(min = 48.dp).pcClickable(PcShape.Control, "Watch ${c.title}") { runCatching { uri.openUri(c.url) } },
                    verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(PcSpace.sm),
                ) {
                    Icon(Icons.Outlined.PlayCircle, null, tint = Pc.Accent, modifier = Modifier.size(22.dp))
                    Text(c.title, style = PcType.BodyStrong, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f))
                    Text("Highlights", style = PcType.Caption)
                }
                if (i < clips.lastIndex) Hairline()
            }
        }
    }
}

private fun h2hDate() = java.time.format.DateTimeFormatter.ofPattern("d MMM yyyy", java.util.Locale.getDefault())

@Composable
private fun HeadToHeadCard(m: Match, games: List<Match>) {
    val homeWins = games.count { g -> g.hasScore && ((g.home.id == m.home.id && g.homeScore!! > g.awayScore!!) || (g.away.id == m.home.id && g.awayScore!! > g.homeScore!!)) }
    val awayWins = games.count { g -> g.hasScore && ((g.home.id == m.away.id && g.homeScore!! > g.awayScore!!) || (g.away.id == m.away.id && g.awayScore!! > g.homeScore!!)) }
    val draws = games.count { it.hasScore && it.homeScore == it.awayScore }
    SectionHeader("Head to head")
    PcCard(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(horizontal = PcSpace.md, vertical = PcSpace.xs)) {
            Text("${m.home.short} $homeWins · Draws $draws · ${m.away.short} $awayWins", style = PcType.LabelStrong, modifier = Modifier.padding(vertical = PcSpace.xs))
            games.filter { it.hasScore }.forEach { g ->
                Hairline()
                Row(
                    Modifier.fillMaxWidth().heightIn(min = 44.dp).semantics(mergeDescendants = true) {},
                    verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(PcSpace.sm),
                ) {
                    Text(java.time.Instant.ofEpochMilli(g.kickoffMs).atZone(java.time.ZoneId.systemDefault()).format(h2hDate()), style = PcType.Caption, modifier = Modifier.width(84.dp))
                    Text(g.home.name, style = PcType.Label, maxLines = 1, overflow = TextOverflow.Ellipsis, textAlign = TextAlign.End, modifier = Modifier.weight(1f))
                    Text("${g.homeScore}–${g.awayScore}", style = PcType.Numeric.copy(fontWeight = FontWeight.Bold))
                    Text(g.away.name, style = PcType.Label, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f))
                }
            }
        }
    }
}

@Composable
private fun CommentaryTab(d: MatchDetail, hidden: Boolean, onReveal: () -> Unit) {
    if (hidden) { SpoilerGate("Commentary", onReveal); return }
    d.odds?.takeIf { it.live }?.let { OddsCard(it) }
    PcCard(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(horizontal = PcSpace.md, vertical = PcSpace.xs)) {
            Text("Live commentary · updates every 30 s", style = PcType.Caption, modifier = Modifier.padding(vertical = PcSpace.xs))
            d.commentary.forEach { c ->
                Hairline()
                Row(Modifier.fillMaxWidth().heightIn(min = 44.dp).padding(vertical = PcSpace.xs).semantics(mergeDescendants = true) {}, horizontalArrangement = Arrangement.spacedBy(PcSpace.sm)) {
                    Text(c.time, style = PcType.Numeric.copy(fontWeight = FontWeight.Bold), modifier = Modifier.width(52.dp))
                    Text(c.text, style = PcType.Body, modifier = Modifier.weight(1f))
                }
            }
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
            val goal = e.kind == EventKind.Goal
            Column(
                Modifier.clip(PcShape.Chip).background(if (goal) Pc.AccentContainer else Pc.SurfaceVariant).border(1.dp, if (goal) Pc.Accent.copy(alpha = 0.6f) else Pc.Outline, PcShape.Chip).padding(horizontal = 7.dp, vertical = 4.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(e.minuteLabel, style = PcType.Tag.copy(fontSize = 14.sp), color = if (goal) Pc.Accent else Pc.Text)
                EventIcon(e.kind, 14)
            }
        }
        Column(Modifier.weight(1f).padding(horizontal = PcSpace.sm)) { if (!e.isHome) EventText(title, e.player, e.detail, false) }
    }
}

@Composable
private fun EventText(title: String, player: String, sub: String, end: Boolean) {
    Column(horizontalAlignment = if (end) Alignment.End else Alignment.Start) {
        Text(player.uppercase(), style = PcType.TeamNameSmall, textAlign = if (end) TextAlign.End else TextAlign.Start, maxLines = 2, overflow = TextOverflow.Ellipsis)
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
    // One full-width card per team (stacked, not side by side) so player names are never cut short.
    Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(PcSpace.gutter)) {
        listOf(d.match.home to l.home, d.match.away to l.away).forEach { (team, lineup) ->
            PcCard(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(horizontal = PcSpace.md, vertical = PcSpace.sm), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Row(Modifier.fillMaxWidth().heightIn(min = 40.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(PcSpace.xs)) {
                        TeamCrest(team, CrestSize.S.dp)
                        Text(team.name, style = PcType.BodyStrong, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f))
                        if (lineup.formation.isNotBlank()) Text(lineup.formation, style = PcType.Tag, color = Pc.Accent)
                    }
                    if (lineup.coach.isNotBlank()) Text("Coach: ${lineup.coach}", style = PcType.Caption, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Hairline(Modifier.padding(vertical = PcSpace.xxs))
                    val order = mapOf("G" to 0, "D" to 1, "M" to 2, "F" to 3)
                    lineup.starters.sortedWith(compareBy({ order[it.position.take(1)] ?: 2 }, { it.number })).forEach { p ->
                        LineupLine(p, team.color) { nav.openPlayer(p.playerId) }
                    }
                    if (lineup.bench.isNotEmpty()) {
                        Spacer(Modifier.height(PcSpace.xs))
                        Overline("Bench")
                        lineup.bench.sortedBy { it.number }.forEach { p -> LineupLine(p, team.color, dim = true) { nav.openPlayer(p.playerId) } }
                    }
                }
            }
        }
    }
    when {
        l.home.hasPositions && l.away.hasPositions -> {
            if (l.home.shapeFromSquad || l.away.shapeFromSquad) Text("No formation was published: shapes are drawn from each starter's squad position.", style = PcType.Caption)
            PrimaryButton("Open pitch view", { nav.openLineup(d.match.id) }, Modifier.fillMaxWidth(), icon = Icons.Outlined.Groups)
        }
        else -> Text("Starting elevens as published. Pitch positions weren't released for this match, so they're listed by role where known.", style = PcType.Caption)
    }
}

@Composable
private fun LineupLine(p: com.innovativeinside.pitchside.data.model.LineupPlayer, color: Long, dim: Boolean = false, onClick: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().heightIn(min = 40.dp).pcClickable(PcShape.Small, "Open ${p.name}", onClick = onClick),
        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(PcSpace.xs),
    ) {
        PlayerAvatar(p.name, color, 26.dp, p.photo)
        Text(if (p.number > 0) p.number.toString() else "", style = PcType.Numeric, color = Pc.Text2, modifier = Modifier.width(20.dp), textAlign = TextAlign.End)
        Text(p.name, style = PcType.Label, color = if (dim) Pc.Text2 else Pc.Text, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f))
        if (p.position.isNotBlank()) Text(p.position.take(1), style = PcType.Micro, color = Pc.Text3)
        if (p.scored) Icon(Icons.Outlined.SportsSoccer, "Scored", tint = Pc.Accent, modifier = Modifier.size(14.dp))
    }
}

// ---------------- Pitch view ----------------

@Composable
fun LineupRoute(back: () -> Unit, openPlayer: (String) -> Unit, vm: MatchDetailViewModel = viewModel(factory = MatchDetailViewModel.Factory)) {
    val state by vm.state.collectAsStateWithLifecycle()
    Screen(maxWidth = 640) {
        Row(Modifier.fillMaxWidth().padding(PcSpace.xs), verticalAlignment = Alignment.CenterVertically) {
            BackButton(back)
            Text("LINEUPS", style = PcType.ScreenTitle.copy(fontSize = 26.sp), modifier = Modifier.weight(1f).padding(start = PcSpace.xs).semantics { heading() })
        }
        StateContent(state, vm::retry, Modifier.padding(horizontal = PcSpace.screen), loading = { DetailSkeleton() }) { d ->
            val l = d.lineups
            if (l == null || !l.home.hasPositions || !l.away.hasPositions) EmptyState("No pitch positions", "Lineups for this match are listed on the match page.", icon = Icons.Outlined.Groups)
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
    val stripe = Color.White.copy(alpha = 0.035f)
    val glow = Pc.Glow
    Column(verticalArrangement = Arrangement.spacedBy(PcSpace.sm)) {
        SegmentedTabs(listOf("${d.match.home.short} · ${home.formation}", "${d.match.away.short} · ${away.formation}"), if (showHome) 0 else 1, { showHome = it == 0 })
        BoxWithConstraints(Modifier.fillMaxWidth().height(470.dp).clip(PcShape.Hero).background(Pc.Pitch).border(1.dp, Pc.HeroOutline, PcShape.Hero)) {
            val w = maxWidth
            val h = maxHeight
            Canvas(Modifier.fillMaxSize()) {
                // Mowing stripes, then a soft emerald glow from the centre circle.
                val bands = 8
                val bandH = size.height / bands
                for (b in 0 until bands) if (b % 2 == 0) drawRect(stripe, Offset(0f, b * bandH), Size(size.width, bandH))
                drawCircle(Brush.radialGradient(listOf(glow.copy(alpha = 0.18f), Color.Transparent), center = Offset(size.width / 2, size.height / 2), radius = size.width * 0.6f), radius = size.width * 0.6f, center = Offset(size.width / 2, size.height / 2))
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
                    if (p.photo != null) Box(contentAlignment = Alignment.BottomEnd) {
                        PlayerAvatar(p.name, if (gk) 0xFFE0A100 else team.color, 34.dp, p.photo)
                        if (p.number > 0) Box(Modifier.size(16.dp).clip(CircleShape).background(Pc.Hero), contentAlignment = Alignment.Center) { Text(p.number.toString(), style = PcType.Micro, color = Pc.OnHero) }
                    } else Box(
                        Modifier.size(34.dp).clip(CircleShape).background(if (gk) Pc.Warning else Pc.team(team.color)).border(2.dp, if (p.scored) Pc.AccentContainer else Pc.OnHero, CircleShape),
                        contentAlignment = Alignment.Center,
                    ) { Text(p.number.toString(), style = PcType.LabelStrong, color = Pc.OnTeam) }
                    Text(p.name.substringAfterLast(' '), style = PcType.Caption.copy(fontWeight = if (p.scored) FontWeight.Bold else FontWeight.Medium), color = Pc.OnHero, maxLines = 1)
                }
            }
        }
        Overline(if (l.coach.isNotBlank()) "Bench · coach ${l.coach}" else "Bench")
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

@Preview(name = "Match · dark", showBackground = true, widthDp = 380, heightDp = 1200, backgroundColor = 0xFF0E1110)
@Composable
private fun MatchDarkPreview() = PreviewHost(dark = true) { Column { MatchContent(UiState.Success(previewDetail), previewNav) {} } }

@Preview(name = "Match · loading", showBackground = true, widthDp = 360)
@Composable
private fun MatchLoadingPreview() = PreviewHost(dark = false) { Column { MatchContent(UiState.Loading, previewNav) {} } }
