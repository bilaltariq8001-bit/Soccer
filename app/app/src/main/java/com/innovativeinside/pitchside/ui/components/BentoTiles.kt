package com.innovativeinside.pitchside.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.NotificationsNone
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.SportsSoccer
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.innovativeinside.pitchside.data.model.Match
import com.innovativeinside.pitchside.data.model.Scorer
import com.innovativeinside.pitchside.data.model.StandingRow
import com.innovativeinside.pitchside.ui.preview.PreviewData
import com.innovativeinside.pitchside.ui.preview.PreviewHost
import com.innovativeinside.pitchside.ui.theme.Pc
import com.innovativeinside.pitchside.ui.theme.PcShape
import com.innovativeinside.pitchside.ui.theme.PcSpace
import com.innovativeinside.pitchside.ui.theme.PcType
import kotlinx.coroutines.delay

/** Shared bento tile: neutral surface, overline title, fixed minimum height so a 2-column row lines up. */
@Composable
fun BentoTile(title: String, modifier: Modifier = Modifier, onClick: (() -> Unit)? = null, description: String? = null, content: @Composable ColumnScope.() -> Unit) {
    PcCard(modifier.let { if (description != null) it.semantics(mergeDescendants = true) { contentDescription = description } else it }, onClick = onClick) {
        Column(Modifier.fillMaxWidth().heightIn(min = 156.dp).padding(PcSpace.md), verticalArrangement = Arrangement.spacedBy(PcSpace.xs)) {
            Overline(title)
            content()
        }
    }
}

// ---------------- Standings ----------------

/** Top of a table: position, crest, name and points. Movement arrows need backend data and are omitted. */
@Composable
fun StandingsTile(leagueName: String, rows: List<StandingRow>, followed: Set<String>, onClick: () -> Unit, modifier: Modifier = Modifier) {
    BentoTile("Standings", modifier, onClick, description = "$leagueName table: " + rows.take(3).joinToString("; ") { "${it.rank}, ${it.team.name}, ${it.points} points" }) {
        Text(leagueName, style = PcType.Caption, maxLines = 1, overflow = TextOverflow.Ellipsis)
        Spacer(Modifier.height(2.dp))
        rows.take(3).forEach { r ->
            val mine = r.team.id in followed
            Row(Modifier.fillMaxWidth().heightIn(min = 28.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(r.rank.toString(), style = PcType.Numeric.copy(fontWeight = FontWeight.Bold), modifier = Modifier.width(14.dp))
                TeamCrest(r.team, 20.dp)
                Text(r.team.name, style = if (mine) PcType.LabelStrong else PcType.Label, color = if (mine) Pc.Accent else Pc.Text, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f))
                Text(r.points.toString(), style = PcType.Numeric.copy(fontWeight = FontWeight.Bold))
            }
        }
    }
}

/** One table row. Columns: position, crest + name, P, GD, Pts, optional form dots. Tabular numerals. */
@Composable
fun StandingsRow(r: StandingRow, onClick: () -> Unit, modifier: Modifier = Modifier, highlight: Boolean = false, followed: Boolean = false, showForm: Boolean = false, compact: Boolean = true) {
    val desc = "${r.rank}, ${r.team.name}, ${r.points} points, played ${r.played}, goal difference ${r.goalDiff}" +
        if (showForm && r.form.isNotBlank()) ", form ${r.form.toList().joinToString(" ")}" else ""
    Row(
        modifier.fillMaxWidth().heightIn(min = PcSpace.touch).background(if (followed) Pc.AccentContainer else if (highlight) Pc.SurfaceVariant else Pc.Surface)
            .pcClickable(androidx.compose.ui.graphics.RectangleShape, "Open ${r.team.name}", onClick = onClick)
            .semantics(mergeDescendants = true) { contentDescription = desc }.padding(horizontal = PcSpace.sm),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(r.rank.toString(), style = PcType.Numeric.copy(fontWeight = FontWeight.Bold), modifier = Modifier.width(24.dp), textAlign = TextAlign.Center)
        Row(Modifier.weight(1f).padding(start = PcSpace.xs), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(PcSpace.xs)) {
            TeamCrest(r.team, CrestSize.S.dp)
            Text(r.team.name, style = if (followed || highlight) PcType.BodyStrong else PcType.Body, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
        if (showForm) FormDots(r.form, Modifier.padding(end = PcSpace.xs), size = 16.dp)
        Text(r.played.toString(), style = PcType.Numeric, color = Pc.Text2, modifier = Modifier.width(28.dp), textAlign = TextAlign.Center)
        if (!compact) Text("${r.won}-${r.drawn}-${r.lost}", style = PcType.Numeric, color = Pc.Text2, modifier = Modifier.width(60.dp), textAlign = TextAlign.Center)
        Text(if (r.goalDiff > 0) "+${r.goalDiff}" else r.goalDiff.toString(), style = PcType.Numeric, color = Pc.Text2, modifier = Modifier.width(36.dp), textAlign = TextAlign.Center)
        Text(r.points.toString(), style = PcType.ScoreRow, modifier = Modifier.width(32.dp), textAlign = TextAlign.End)
    }
}

/** Header row matching [StandingsRow]; sticky in lazy tables. */
@Composable
fun StandingsHeader(modifier: Modifier = Modifier, showForm: Boolean = false, compact: Boolean = true) {
    Row(modifier.fillMaxWidth().height(36.dp).background(Pc.SurfaceVariant).padding(horizontal = PcSpace.sm), verticalAlignment = Alignment.CenterVertically) {
        Text("#", style = PcType.Overline, modifier = Modifier.width(24.dp), textAlign = TextAlign.Center)
        Text("CLUB", style = PcType.Overline, modifier = Modifier.weight(1f).padding(start = PcSpace.xs))
        if (showForm) Text("FORM", style = PcType.Overline, modifier = Modifier.padding(end = PcSpace.xs))
        Text("P", style = PcType.Overline, modifier = Modifier.width(28.dp), textAlign = TextAlign.Center)
        if (!compact) Text("W-D-L", style = PcType.Overline, modifier = Modifier.width(60.dp), textAlign = TextAlign.Center)
        Text("GD", style = PcType.Overline, modifier = Modifier.width(36.dp), textAlign = TextAlign.Center)
        Text("PTS", style = PcType.Overline, color = Pc.Text, modifier = Modifier.width(32.dp), textAlign = TextAlign.End)
    }
}

// ---------------- Next kickoff ----------------

/** "1d 04h 12m" until kickoff; "Starting now" once it has passed. */
fun countdown(fromMs: Long, toMs: Long): String {
    val m = ((toMs - fromMs) / 60_000).coerceAtLeast(0)
    if (m == 0L) return "Starting now"
    val d = m / (24 * 60)
    val h = (m / 60) % 24
    val min = m % 60
    return when {
        d > 0 -> "${d}d %02dh %02dm".format(h, min)
        h > 0 -> "${h}h %02dm".format(min)
        else -> "${min}m"
    }
}

/**
 * Next followed fixture with a live countdown (ticks every 30s), a reminder toggle (match alerts) and an
 * add-to-calendar action. A 3dp edge in the home side's colour is the only team colour on the tile.
 */
@Composable
fun NextKickoffTile(
    match: Match,
    reminderOn: Boolean,
    onReminder: () -> Unit,
    onCalendar: () -> Unit,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    nowMs: () -> Long = System::currentTimeMillis,
) {
    var now by remember { mutableLongStateOf(nowMs()) }
    LaunchedEffect(match.id) { while (true) { now = nowMs(); delay(30_000) } }
    val left = countdown(now, match.kickoffMs)
    PcCard(modifier.semantics(mergeDescendants = true) { contentDescription = "Next kickoff: ${match.home.name} versus ${match.away.name}, in $left" }, onClick = onClick) {
        Row(Modifier.fillMaxWidth().heightIn(min = 156.dp)) {
            Box(Modifier.width(3.dp).fillMaxHeight().heightIn(min = 156.dp).background(Pc.team(match.home.color)))
            Column(Modifier.weight(1f).padding(PcSpace.md), verticalArrangement = Arrangement.spacedBy(PcSpace.xs)) {
                Overline("Next kickoff")
                Text("${match.home.name} vs ${match.away.name}", style = PcType.BodyStrong, maxLines = 2, overflow = TextOverflow.Ellipsis)
                Text(match.leagueName, style = PcType.Caption, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Spacer(Modifier.weight(1f, fill = false))
                Text(left, style = PcType.ScoreCompact, maxLines = 1)
                Row(horizontalArrangement = Arrangement.spacedBy(PcSpace.xxs)) {
                    IconAction(if (reminderOn) Icons.Filled.Notifications else Icons.Outlined.NotificationsNone, if (reminderOn) "Turn off kickoff reminder" else "Remind me at kickoff", onReminder, tint = if (reminderOn) Pc.Accent else Pc.Text2, modifier = Modifier.size(40.dp))
                    IconAction(Icons.Outlined.CalendarMonth, "Add to calendar", onCalendar, tint = Pc.Text2, modifier = Modifier.size(40.dp))
                }
            }
        }
    }
}

// ---------------- Player stat ----------------

/** Initials avatar in a team-colour ring; a silhouette when there is no name. Player photos need backend URLs. */
@Composable
fun PlayerAvatar(name: String, color: Long, size: Dp = 40.dp) {
    val initials = name.split(" ").filter { it.isNotBlank() }.take(2).joinToString("") { it.first().uppercase() }
    Box(Modifier.size(size).clip(CircleShape).background(Pc.SurfaceVariant).border(2.dp, Pc.team(color), CircleShape), contentAlignment = Alignment.Center) {
        if (initials.isBlank()) Icon(Icons.Outlined.Person, null, tint = Pc.Text2, modifier = Modifier.size(size * 0.6f))
        else Text(initials, style = if (size >= 56.dp) PcType.Subtitle else PcType.LabelStrong, color = Pc.Text)
    }
}

@Composable
fun PlayerAvatar(name: String, color: Long, size: Int) = PlayerAvatar(name, color, size.dp)

/** Top scorer tile: avatar, name, club and the goal count. */
@Composable
fun PlayerStatTile(scorer: Scorer, onClick: () -> Unit, modifier: Modifier = Modifier) {
    BentoTile("Top scorer", modifier, onClick, description = "Top scorer: ${scorer.name}, ${scorer.team.name}, ${scorer.goals} goals") {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(PcSpace.xs)) {
            PlayerAvatar(scorer.name, scorer.team.color, 40.dp)
            Column(Modifier.weight(1f)) {
                Text(scorer.name, style = PcType.LabelStrong, maxLines = 2, overflow = TextOverflow.Ellipsis)
                Text(scorer.team.name, style = PcType.Caption, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
        }
        Spacer(Modifier.weight(1f, fill = false))
        Row(verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(scorer.goals.toString(), style = PcType.ScoreCompact)
            Text("GOALS", style = PcType.Overline, modifier = Modifier.padding(bottom = 5.dp))
            Spacer(Modifier.weight(1f))
            Icon(Icons.Outlined.SportsSoccer, null, tint = Pc.Text2, modifier = Modifier.size(20.dp))
        }
    }
}

// ---------------- AI insight ----------------

/** Insight text from Match.brief. No probability or confidence ring: no such data exists. */
@Composable
fun AiInsightTile(text: String, context: String, onClick: () -> Unit, modifier: Modifier = Modifier) {
    BentoTile("AI insight", modifier, onClick, description = "AI insight about $context: $text") {
        Box(Modifier.size(28.dp).clip(CircleShape).background(Pc.AccentContainer), contentAlignment = Alignment.Center) {
            Icon(Icons.Outlined.AutoAwesome, null, tint = Pc.Accent, modifier = Modifier.size(16.dp))
        }
        Text(text, style = PcType.Label, maxLines = 4, overflow = TextOverflow.Ellipsis)
        Text(context, style = PcType.Caption, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}

/** Full-width brief (match detail, player scouting note). */
@Composable
fun BriefCard(text: String, modifier: Modifier = Modifier, title: String = "MATCH BRIEF", updated: String = "", compactMeta: Boolean = false) {
    PcCard(modifier.fillMaxWidth()) {
        Column(Modifier.fillMaxWidth().padding(PcSpace.md), verticalArrangement = Arrangement.spacedBy(PcSpace.xs)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(PcSpace.xs)) {
                Box(Modifier.size(24.dp).clip(CircleShape).background(Pc.AccentContainer), contentAlignment = Alignment.Center) {
                    Icon(Icons.Outlined.AutoAwesome, null, tint = Pc.Accent, modifier = Modifier.size(14.dp))
                }
                Overline(title, Modifier.weight(1f))
                if (updated.isNotBlank()) Text(updated, style = PcType.Caption)
            }
            Text(text, style = PcType.Body)
            if (!compactMeta) Text("AI-generated from match data", style = PcType.Caption)
        }
    }
}

// ---------------- Previews ----------------

@Preview(name = "Bento tiles", showBackground = true, widthDp = 380)
@Composable
private fun BentoPreview() = PreviewHost(dark = false) {
    Column(Modifier.padding(PcSpace.md), verticalArrangement = Arrangement.spacedBy(PcSpace.gutter)) {
        Row(horizontalArrangement = Arrangement.spacedBy(PcSpace.gutter)) {
            StandingsTile("Premier Division", PreviewData.table, setOf("ng"), {}, Modifier.weight(1f))
            NextKickoffTile(PreviewData.upcoming, false, {}, {}, {}, Modifier.weight(1f)) { PreviewData.upcoming.kickoffMs - 90_000_000 }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(PcSpace.gutter)) {
            PlayerStatTile(Scorer(1, "p1", "Leo Marsh", PreviewData.live.home, 18, 4), {}, Modifier.weight(1f))
            AiInsightTile(PreviewData.live.brief, "Northgate v Riverside", {}, Modifier.weight(1f))
        }
        PcCard { Column { StandingsHeader(showForm = true); PreviewData.table.forEach { StandingsRow(it, {}, showForm = true, followed = it.team.id == "ng") } } }
    }
}
