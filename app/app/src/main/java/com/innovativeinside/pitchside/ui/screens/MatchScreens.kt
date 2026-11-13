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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material.icons.outlined.SportsSoccer
import androidx.compose.material.icons.outlined.SwapVert
import androidx.compose.material.icons.outlined.Style
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.innovativeinside.pitchside.LocalApp
import com.innovativeinside.pitchside.data.EventKind
import com.innovativeinside.pitchside.data.Match
import com.innovativeinside.pitchside.data.Status
import com.innovativeinside.pitchside.ui.components.BackButton
import com.innovativeinside.pitchside.ui.components.BriefCard
import com.innovativeinside.pitchside.ui.components.Chip
import com.innovativeinside.pitchside.ui.components.Crest
import com.innovativeinside.pitchside.ui.components.EmptyState
import com.innovativeinside.pitchside.ui.components.FormChip
import com.innovativeinside.pitchside.ui.components.IconAction
import com.innovativeinside.pitchside.ui.components.MatchCard
import com.innovativeinside.pitchside.ui.components.MatchCardSkeleton
import com.innovativeinside.pitchside.ui.components.PcCard
import com.innovativeinside.pitchside.ui.components.PrimaryButton
import com.innovativeinside.pitchside.ui.components.SectionHeader
import com.innovativeinside.pitchside.ui.components.SegmentedTabs
import com.innovativeinside.pitchside.ui.components.StatBar
import com.innovativeinside.pitchside.ui.components.StatusBadge
import com.innovativeinside.pitchside.ui.components.TextAction
import com.innovativeinside.pitchside.ui.components.UnderlineTabs
import com.innovativeinside.pitchside.ui.components.pcClickable
import com.innovativeinside.pitchside.ui.theme.Pc
import com.innovativeinside.pitchside.ui.theme.PcShape
import com.innovativeinside.pitchside.ui.theme.PcType
import kotlinx.coroutines.delay

// ---------- Matches tab ----------

@Composable
fun MatchesScreen(openMatch: (String) -> Unit) {
    val app = LocalApp.current
    var tab by rememberSaveable { mutableStateOf(0) }
    var mine by rememberSaveable { mutableStateOf(false) }
    var loading by remember { mutableStateOf(true) }
    LaunchedEffect(Unit) { delay(600); loading = false }
    val all = app.repo.matches().filter { !mine || it.isFollowed(app.followedTeams) }
    fun group(m: Match) = when (m.status) { Status.Live -> 0; Status.Finished -> 2; else -> 1 }
    val shown = all.filter { group(it) == tab }
    Screen(maxWidth = 720) {
        Column(Modifier.padding(horizontal = 16.dp, vertical = 16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Column {
                Text("Matches", style = PcType.Title.copy(fontSize = 30.sp, lineHeight = 36.sp))
                Text("Updated 12 seconds ago", style = PcType.BodySmall)
            }
            SegmentedTabs(listOf("Live ${all.count { group(it) == 0 }}", "Upcoming ${all.count { group(it) == 1 }}", "Finished ${all.count { group(it) == 2 }}"), tab, { tab = it })
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Chip("All matches", !mine, { mine = false })
                Chip("My teams", mine, { mine = true })
            }
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                when {
                    loading -> repeat(4) { MatchCardSkeleton() }
                    shown.isEmpty() -> EmptyState(
                        if (mine) "None of your teams are playing" else "No matches here yet",
                        "Pull back later or switch the tab. Alerts will tell you when your teams kick off.",
                        actionLabel = if (mine) "Show all matches" else null, onAction = { mine = false },
                    )
                    else -> shown.forEach { m -> MatchCard(m, app.isHidden(m), { openMatch(m.id) }, { app.reveal(m.id) }) }
                }
            }
        }
    }
}

// ---------- Match detail ----------

@Composable
fun MatchScreen(matchId: String, onBack: () -> Unit, openLineup: (String) -> Unit) {
    val app = LocalApp.current
    val match = app.repo.match(matchId)
    if (match == null) {
        Screen { Row(Modifier.padding(8.dp)) { BackButton(onBack) }; EmptyState("Match not found", "This match is no longer available.", Modifier.padding(16.dp)) }
        return
    }
    var tab by rememberSaveable { mutableStateOf(0) }
    var alertsOn by rememberSaveable { mutableStateOf(true) }
    val hidden = app.isHidden(match)
    val tabs = listOf("Overview", "Timeline", "Stats", "Lineups", "Table")
    val wide = widthClass() != WidthClass.Compact

    Screen(maxWidth = if (wide) 900 else 720) {
        Row(Modifier.fillMaxWidth().padding(start = 8.dp, end = 8.dp, top = 8.dp), verticalAlignment = Alignment.CenterVertically) {
            BackButton(onBack)
            Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                Text(match.league, style = PcType.Label.copy(fontSize = 15.sp, fontWeight = FontWeight.ExtraBold))
                Text("${match.round} / ${match.venue}", style = PcType.Meta.copy(color = Pc.Text2))
            }
            IconAction(if (alertsOn) Icons.Filled.Notifications else Icons.Outlined.Notifications, if (alertsOn) "Match alerts on" else "Match alerts off", { alertsOn = !alertsOn }, tint = if (alertsOn) Pc.Lime else Pc.Text2)
        }
        PcCard(Modifier.padding(horizontal = 16.dp, vertical = 8.dp).fillMaxWidth(), shape = PcShape.Hero, container = Pc.Pitch, border = Pc.PitchLine) {
            Column(Modifier.padding(20.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.Top) {
                    Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) { Crest(match.home, 60.dp); Text(match.home.name, style = PcType.Label.copy(fontSize = 15.sp, fontWeight = FontWeight.ExtraBold, color = Pc.OnPitch), maxLines = 1, overflow = TextOverflow.Ellipsis) }
                    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        if (hidden) {
                            Text("– –", style = PcType.ScoreXL.copy(fontSize = 56.sp, lineHeight = 56.sp, color = Pc.OnPitch.copy(alpha = 0.7f)))
                            TextAction("Reveal score", { app.reveal(match.id) })
                        } else {
                            Text(if (match.homeScore != null) "${match.homeScore} – ${match.awayScore}" else match.label, style = if (match.homeScore != null) PcType.ScoreXL.copy(fontSize = 60.sp, lineHeight = 62.sp, color = Pc.OnPitch) else PcType.ScoreL.copy(color = Pc.OnPitch))
                            Row(Modifier.clip(PcShape.Badge).background(if (match.status == Status.Live) Pc.LiveTint else Pc.OnPitch.copy(alpha = 0.12f)).padding(horizontal = 10.dp, vertical = 7.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                if (match.status == Status.Live) Box(Modifier.size(7.dp).clip(CircleShape).background(Pc.Live))
                                Text(if (match.status == Status.Live) "LIVE  ${match.label}" else match.label, style = PcType.Meta, color = if (match.status == Status.Live) Pc.Live else Pc.OnPitch, fontWeight = FontWeight.ExtraBold)
                            }
                        }
                    }
                    Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) { Crest(match.away, 60.dp); Text(match.away.name, style = PcType.Label.copy(fontSize = 15.sp, color = Pc.OnPitch.copy(alpha = 0.84f)), maxLines = 1, overflow = TextOverflow.Ellipsis) }
                }
                if (!hidden && match.homeScore != null) {
                    val events = app.repo.events(match.id).filter { it.kind == EventKind.Goal }.sortedBy { it.minute.filter { c -> c.isDigit() }.toIntOrNull() ?: 0 }
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text(events.filter { it.isHome }.joinToString(", ") { it.title.removePrefix("Goal · ") + " " + it.minute }, style = PcType.Meta.copy(color = Pc.OnPitch.copy(alpha = 0.76f)), modifier = Modifier.weight(1f))
                        Text(events.filter { !it.isHome }.joinToString(", ") { it.title.removePrefix("Goal · ") + " " + it.minute }, style = PcType.Meta.copy(color = Pc.OnPitch.copy(alpha = 0.76f)), textAlign = TextAlign.End, modifier = Modifier.weight(1f))
                    }
                }
            }
        }
        UnderlineTabs(tabs, tab, { tab = it })
        Box(Modifier.fillMaxWidth().height(1.dp).background(Pc.Border))
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            when (tab) {
                0 -> OverviewTab(match, hidden, onStats = { tab = 2 })
                1 -> TimelineTab(match, hidden)
                2 -> StatsTab(match, hidden)
                3 -> LineupsTab(match, openLineup)
                else -> TableTab(match)
            }
        }
    }
}

@Composable
private fun OverviewTab(match: Match, hidden: Boolean, onStats: () -> Unit) {
    val app = LocalApp.current
    if (hidden) {
        EmptyState("Brief hidden by Spoiler Shield", "Reveal the score to see the Match Brief, timeline and stats.", actionLabel = "Reveal score", onAction = { app.reveal(match.id) })
        return
    }
    BriefCard(match.brief, title = if (match.status == Status.Live) "Live match brief" else "Match brief")
    PcCard(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(horizontal = 16.dp, vertical = 4.dp)) {
            InfoLine("Kickoff", if (match.status == Status.Upcoming) match.label else "19:00, Thu 29 Oct")
            InfoLine("Venue", match.venue)
            Row(Modifier.fillMaxWidth().heightIn(min = 48.dp), verticalAlignment = Alignment.CenterVertically) {
                Text("Form (last 5)", style = PcType.BodySmall, modifier = Modifier.weight(1f))
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) { "WWDWL".forEach { FormChip(it) } }
            }
        }
    }
    if (match.status == Status.Live || match.status == Status.Finished) {
        SectionHeader("Key numbers", "All stats", onStats)
        app.repo.stats(match.id).take(3).forEach { StatBar(it) }
    }
}

@Composable
private fun InfoLine(label: String, value: String) {
    Row(Modifier.fillMaxWidth().heightIn(min = 48.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(label, style = PcType.BodySmall, modifier = Modifier.weight(1f))
        Text(value, style = PcType.Label.copy(fontSize = 14.sp))
    }
    Box(Modifier.fillMaxWidth().height(1.dp).background(Pc.Border))
}

@Composable
private fun TimelineTab(match: Match, hidden: Boolean) {
    val app = LocalApp.current
    if (hidden || match.status == Status.Upcoming || match.status == Status.Postponed) {
        EmptyState(if (hidden) "Timeline hidden" else "No events yet", if (hidden) "Reveal the score to see goals and cards." else "Events appear here as the match unfolds.")
        return
    }
    Column {
        app.repo.events(match.id).forEach { e ->
            Row(Modifier.fillMaxWidth().heightIn(min = 64.dp).padding(vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f).padding(end = 8.dp), horizontalAlignment = Alignment.End) { if (e.isHome) EventText(e.title, e.sub, Alignment.End) }
                Column(Modifier.width(56.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(e.minute, style = PcType.ScoreS)
                    val (icon, tint) = when (e.kind) { EventKind.Goal -> Icons.Outlined.SportsSoccer to Pc.Lime; EventKind.Card -> Icons.Outlined.Style to Pc.Amber; EventKind.Sub -> Icons.Outlined.SwapVert to Pc.Text2 }
                    Box(Modifier.size(24.dp).clip(CircleShape).background(Pc.Raised), contentAlignment = Alignment.Center) { Icon(icon, e.kind.name, tint = tint, modifier = Modifier.size(14.dp)) }
                }
                Column(Modifier.weight(1f).padding(start = 8.dp)) { if (!e.isHome) EventText(e.title, e.sub, Alignment.Start) }
            }
            Box(Modifier.fillMaxWidth().height(1.dp).background(Pc.Border))
        }
    }
}

@Composable
private fun EventText(title: String, sub: String, align: Alignment.Horizontal) {
    Column(horizontalAlignment = align) {
        Text(title, style = PcType.Label.copy(fontSize = 14.sp, fontWeight = FontWeight.ExtraBold), textAlign = if (align == Alignment.End) TextAlign.End else TextAlign.Start)
        Text(sub, style = PcType.Meta.copy(color = Pc.Text2), textAlign = if (align == Alignment.End) TextAlign.End else TextAlign.Start)
    }
}

@Composable
private fun StatsTab(match: Match, hidden: Boolean) {
    val app = LocalApp.current
    if (hidden || match.status == Status.Upcoming || match.status == Status.Postponed) {
        EmptyState(if (hidden) "Stats hidden" else "Stats start at kickoff", if (hidden) "Reveal the score to see the numbers." else "Possession, shots and more appear once the match begins.")
        return
    }
    Row(Modifier.fillMaxWidth()) {
        Row(Modifier.weight(1f), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) { Box(Modifier.size(10.dp).clip(androidx.compose.foundation.shape.RoundedCornerShape(3.dp)).background(Pc.Lime)); Text(match.home.name, style = PcType.Label) }
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) { Text(match.away.name, style = PcType.Label.copy(color = Pc.Text2)); Box(Modifier.size(10.dp).clip(androidx.compose.foundation.shape.RoundedCornerShape(3.dp)).background(Pc.Neutral)) }
    }
    app.repo.stats(match.id).forEach { StatBar(it) }
}

@Composable
private fun LineupsTab(match: Match, openLineup: (String) -> Unit) {
    val app = LocalApp.current
    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        listOf(true, false).forEach { home ->
            val l = app.repo.lineup(match.id, home)
            PcCard(Modifier.weight(1f)) {
                Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text("${if (home) match.home.name else match.away.name} / ${l.formation}", style = PcType.Label.copy(color = if (home) Pc.Lime else Pc.Text2))
                    l.starters.sortedBy { it.number }.forEach { p ->
                        Row(Modifier.heightIn(min = 32.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            Text(p.number.toString(), style = PcType.Label.copy(color = Pc.Text3), modifier = Modifier.width(22.dp)); Text(p.name, style = PcType.Label.copy(fontWeight = FontWeight.SemiBold))
                        }
                    }
                }
            }
        }
    }
    PrimaryButton("Open pitch view", { openLineup(match.id) }, Modifier.fillMaxWidth())
}

@Composable
private fun TableTab(match: Match) {
    val app = LocalApp.current
    PcCard(Modifier.fillMaxWidth()) {
        Column {
            app.repo.standings("pd").take(5).forEachIndexed { i, r ->
                val mine = r.team.id == match.home.id || r.team.id == match.away.id
                Row(Modifier.fillMaxWidth().height(52.dp).background(if (r.team.id in app.followedTeams) Pc.LimeTintSoft else if (mine) Pc.Raised else Color.Transparent).padding(horizontal = 16.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(r.rank.toString(), style = PcType.Label, modifier = Modifier.width(24.dp)); Crest(r.team, 24.dp)
                    Text(r.team.name, style = PcType.Body.copy(fontSize = 15.sp, fontWeight = if (mine) FontWeight.ExtraBold else FontWeight.SemiBold), modifier = Modifier.weight(1f))
                    Text(r.played.toString(), style = PcType.BodySmall, modifier = Modifier.width(28.dp), textAlign = TextAlign.Center)
                    Text(if (r.goalDiff > 0) "+${r.goalDiff}" else r.goalDiff.toString(), style = PcType.BodySmall, modifier = Modifier.width(36.dp), textAlign = TextAlign.Center)
                    Text(r.points.toString(), style = PcType.ScoreS, modifier = Modifier.width(32.dp), textAlign = TextAlign.End)
                }
                if (i < 4) Box(Modifier.fillMaxWidth().height(1.dp).background(Pc.Border))
            }
        }
    }
    Text("Both clubs in this match are highlighted. Your clubs show a lime tint.", style = PcType.Meta)
}

// ---------- Pitch view ----------

@Composable
fun LineupScreen(matchId: String, onBack: () -> Unit) {
    val app = LocalApp.current
    val match = app.repo.match(matchId) ?: return
    var home by rememberSaveable { mutableStateOf(true) }
    val l = app.repo.lineup(match.id, home)
    val team = if (home) match.home else match.away
    Screen(maxWidth = 640) {
        Row(Modifier.fillMaxWidth().padding(8.dp), verticalAlignment = Alignment.CenterVertically) {
            BackButton(onBack)
            Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                Text("${match.home.name} ${match.homeScore ?: "-"} – ${match.awayScore ?: "-"} ${match.away.name}", style = PcType.Label.copy(fontSize = 15.sp))
                Text(match.label, style = PcType.Meta.copy(color = if (match.status == Status.Live) Pc.Live else Pc.Text2))
            }
            Spacer(Modifier.width(48.dp))
        }
        Column(Modifier.padding(horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            SegmentedTabs(listOf("${match.home.name} (${app.repo.lineup(match.id, true).formation})", "${match.away.name} (${app.repo.lineup(match.id, false).formation})"), if (home) 0 else 1, { home = it == 0 })
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
                        Modifier.offset(x = w * p.x - 28.dp, y = h * p.y - 24.dp).width(56.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        Box(Modifier.size(34.dp).clip(CircleShape).background(if (p.number == 1) Pc.Amber else Color(team.color)).border(2.dp, if (p.scored) Pc.Lime else Color.White, CircleShape), contentAlignment = Alignment.Center) {
                            Text(p.number.toString(), color = if (p.number == 1) Pc.OnLime else Color.White, fontWeight = FontWeight.ExtraBold, fontSize = 13.sp)
                        }
                        Text(p.name, color = Color.White, fontSize = 14.sp, fontWeight = if (p.scored) FontWeight.Bold else FontWeight.Medium, maxLines = 1)
                    }
                }
            }
            Text("Bench / Coach ${l.coach}", style = PcType.Overline.copy(color = Pc.Text2))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) { l.bench.forEach { Box(Modifier.heightIn(min = 36.dp).clip(PcShape.Chip).background(Pc.Raised).padding(horizontal = 12.dp), contentAlignment = Alignment.Center) { Text(it, style = PcType.Label) } } }
        }
    }
}
