package com.innovativeinside.pitchside.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.ui.unit.sp
import androidx.compose.ui.graphics.Color
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Notifications
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
import androidx.compose.ui.layout.ContentScale
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
            Text(title.uppercase(), style = PcType.Tag.copy(fontSize = 16.sp), color = Pc.Text2, maxLines = 1, overflow = TextOverflow.Ellipsis)
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

/** Qualification / relegation colour for a table row's zone label from the data source; null when there is none. */
@Composable
fun zoneColor(zone: String): Color? = when {
    zone.isBlank() -> null
    zone.contains("relegation", ignoreCase = true) -> Pc.Error
    zone.contains("play", ignoreCase = true) -> Pc.Warning
    else -> Pc.Accent
}

/**
 * One table row. A 3dp marker on the left in the zone colour (promotion / qualification emerald, play-offs amber,
 * relegation red) when the source labels the place. Columns: position, crest + name, form pills, P, GD, PTS.
 */
@Composable
fun StandingsRow(r: StandingRow, onClick: () -> Unit, modifier: Modifier = Modifier, highlight: Boolean = false, followed: Boolean = false, showForm: Boolean = false, compact: Boolean = true) {
    val desc = "${r.rank}, ${r.team.name}, ${r.points} points, played ${r.played}, goal difference ${r.goalDiff}" +
        (if (r.zone.isNotBlank()) ", ${r.zone}" else "") +
        if (showForm && r.form.isNotBlank()) ", form ${r.form.toList().joinToString(" ")}" else ""
    val marker = zoneColor(r.zone)
    Row(
        modifier.fillMaxWidth().height(IntrinsicSize.Min).heightIn(min = 52.dp)
            .background(if (followed) Pc.AccentContainer else if (highlight) Pc.SurfaceVariant else Color.Transparent)
            .pcClickable(androidx.compose.ui.graphics.RectangleShape, "Open ${r.team.name}", onClick = onClick)
            .semantics(mergeDescendants = true) { contentDescription = desc }.padding(end = PcSpace.sm),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(Modifier.width(3.dp).fillMaxHeight().padding(vertical = 10.dp).clip(PcShape.Chip).background(marker ?: Color.Transparent))
        Text(r.rank.toString(), style = PcType.Tag.copy(fontSize = 16.sp), color = if (marker != null) Pc.Text else Pc.Text2, modifier = Modifier.width(30.dp), textAlign = TextAlign.Center)
        Row(Modifier.weight(1f), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(PcSpace.xs)) {
            TeamCrest(r.team, 24.dp)
            Text(r.team.name, style = if (followed || highlight) PcType.BodyStrong else PcType.Body, color = if (followed) Pc.Accent else Pc.Text, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
        if (showForm) FormPills(r.form, Modifier.padding(start = PcSpace.xxs, end = PcSpace.xs), size = 15.dp)
        Text(r.played.toString(), style = PcType.Numeric, color = Pc.Text2, modifier = Modifier.width(26.dp), textAlign = TextAlign.Center)
        if (!compact) Text("${r.won}-${r.drawn}-${r.lost}", style = PcType.Numeric, color = Pc.Text2, modifier = Modifier.width(58.dp), textAlign = TextAlign.Center)
        Text(if (r.goalDiff > 0) "+${r.goalDiff}" else r.goalDiff.toString(), style = PcType.Numeric, color = Pc.Text2, modifier = Modifier.width(34.dp), textAlign = TextAlign.Center)
        Text(r.points.toString(), style = PcType.ScoreRow, modifier = Modifier.width(30.dp), textAlign = TextAlign.End)
    }
}

/** Header row matching [StandingsRow]; sticky in lazy tables. */
@Composable
fun StandingsHeader(modifier: Modifier = Modifier, showForm: Boolean = false, compact: Boolean = true) {
    val h = PcType.Overline.copy(fontSize = 12.sp)
    Row(modifier.fillMaxWidth().height(38.dp).background(Pc.SurfaceVariant).padding(start = 3.dp, end = PcSpace.sm), verticalAlignment = Alignment.CenterVertically) {
        Text("#", style = h, modifier = Modifier.width(30.dp), textAlign = TextAlign.Center)
        Text("CLUB", style = h, modifier = Modifier.weight(1f))
        if (showForm) Text("FORM", style = h, modifier = Modifier.padding(end = PcSpace.xs))
        Text("P", style = h, modifier = Modifier.width(26.dp), textAlign = TextAlign.Center)
        if (!compact) Text("W-D-L", style = h, modifier = Modifier.width(58.dp), textAlign = TextAlign.Center)
        Text("GD", style = h, modifier = Modifier.width(34.dp), textAlign = TextAlign.Center)
        Text("PTS", style = h, color = Pc.Text, modifier = Modifier.width(30.dp), textAlign = TextAlign.End)
    }
}

/** Legend for the zone markers actually present in [rows] (labels as the source publishes them). */
@OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class)
@Composable
fun ZoneLegend(rows: List<StandingRow>, modifier: Modifier = Modifier) {
    val zones = rows.map { it.zone }.filter { it.isNotBlank() }.distinct()
    if (zones.isEmpty()) return
    androidx.compose.foundation.layout.FlowRow(modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(PcSpace.md), verticalArrangement = Arrangement.spacedBy(PcSpace.xxs)) {
        zones.forEach { z ->
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Box(Modifier.size(width = 3.dp, height = 14.dp).clip(PcShape.Chip).background(zoneColor(z) ?: Pc.Text3))
                Text(z, style = PcType.Caption)
            }
        }
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
            Column(Modifier.weight(1f).padding(PcSpace.md), verticalArrangement = Arrangement.spacedBy(PcSpace.xs)) {
                Text("NEXT KICKOFF", style = PcType.Tag.copy(fontSize = 16.sp), color = Pc.Text2)
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    TeamCrest(match.home, 28.dp)
                    Text("v", style = PcType.Caption)
                    TeamCrest(match.away, 28.dp)
                }
                Text("${match.home.name} vs ${match.away.name}".uppercase(), style = PcType.TeamNameSmall, maxLines = 2, overflow = TextOverflow.Ellipsis)
                Text(match.leagueName, style = PcType.Caption, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Spacer(Modifier.weight(1f, fill = false))
                Text(left, style = PcType.ScoreCompact, color = Pc.Accent, maxLines = 1)
                Row(horizontalArrangement = Arrangement.spacedBy(PcSpace.xxs)) {
                    IconAction(if (reminderOn) Icons.Filled.Notifications else Icons.Outlined.NotificationsNone, if (reminderOn) "Turn off kickoff reminder" else "Remind me at kickoff", onReminder, tint = if (reminderOn) Pc.Accent else Pc.Text2, modifier = Modifier.size(40.dp))
                    IconAction(Icons.Outlined.CalendarMonth, "Add to calendar", onCalendar, tint = Pc.Text2, modifier = Modifier.size(40.dp))
                }
            }
        }
    }
}

// ---------------- Player stat ----------------

/** Player photo in a team-colour ring; initials (or a silhouette when there is no name) while it loads or when there is none. */
@Composable
fun PlayerAvatar(name: String, color: Long, size: Dp = 40.dp, photo: String? = null) {
    val initials = name.split(" ").filter { it.isNotBlank() }.take(2).joinToString("") { it.first().uppercase() }
    val fallback: @Composable () -> Unit = {
        Box(Modifier.fillMaxSize().background(Pc.SurfaceVariant), contentAlignment = Alignment.Center) {
            if (initials.isBlank()) Icon(Icons.Outlined.Person, null, tint = Pc.Text2, modifier = Modifier.size(size * 0.6f))
            else Text(initials, style = if (size >= 56.dp) PcType.Subtitle else PcType.LabelStrong, color = Pc.Text)
        }
    }
    Box(Modifier.size(size).clip(CircleShape).border(1.5.dp, Pc.team(color).copy(alpha = 0.9f), CircleShape).padding(2.5.dp).clip(CircleShape), contentAlignment = Alignment.Center) {
        if (photo == null) fallback() else RemoteImage(photo, fallback, Modifier.fillMaxSize(), ContentScale.Crop)
    }
}

@Composable
fun PlayerAvatar(name: String, color: Long, size: Int) = PlayerAvatar(name, color, size.dp)

/** Top scorer tile: avatar, name, club and the goal count. */
@Composable
fun PlayerStatTile(scorer: Scorer, onClick: () -> Unit, modifier: Modifier = Modifier) {
    BentoTile("Top scorer", modifier, onClick, description = "Top scorer: ${scorer.name}, ${scorer.team.name}, ${scorer.goals} goals") {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(PcSpace.xs)) {
            PlayerAvatar(scorer.name, scorer.team.color, 44.dp, scorer.photo)
            Column(Modifier.weight(1f)) {
                Text(scorer.name, style = PcType.LabelStrong, maxLines = 2, overflow = TextOverflow.Ellipsis)
                Text(scorer.team.name, style = PcType.Caption, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
        }
        Spacer(Modifier.weight(1f, fill = false))
        Row(verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(scorer.goals.toString(), style = PcType.ScoreCompact, color = Pc.Accent)
            Text("GOALS", style = PcType.Overline, modifier = Modifier.padding(bottom = 5.dp))
            Spacer(Modifier.weight(1f))
            Icon(Icons.Outlined.SportsSoccer, null, tint = Pc.Text2, modifier = Modifier.size(20.dp))
        }
    }
}

/** Full-width titled note (player season snapshot and profile). */
@Composable
fun NoteCard(text: String, modifier: Modifier = Modifier, title: String) {
    PcCard(modifier.fillMaxWidth()) {
        Column(Modifier.fillMaxWidth().padding(PcSpace.md), verticalArrangement = Arrangement.spacedBy(PcSpace.xs)) {
            Text(title.uppercase(), style = PcType.Tag.copy(fontSize = 16.sp), color = Pc.Text2)
            Text(text, style = PcType.Body)
        }
    }
}

// ---------------- Previews ----------------

@Preview(name = "Bento tiles", showBackground = true, widthDp = 380)
@Composable
private fun BentoPreview() = PreviewHost(dark = true) {
    Column(Modifier.padding(PcSpace.md), verticalArrangement = Arrangement.spacedBy(PcSpace.gutter)) {
        Row(horizontalArrangement = Arrangement.spacedBy(PcSpace.gutter)) {
            StandingsTile("Premier Division", PreviewData.table, setOf("ng"), {}, Modifier.weight(1f))
            NextKickoffTile(PreviewData.upcoming, false, {}, {}, {}, Modifier.weight(1f)) { PreviewData.upcoming.kickoffMs - 90_000_000 }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(PcSpace.gutter)) {
            PlayerStatTile(Scorer(1, "p1", "Leo Marsh", PreviewData.live.home, 18, 4), {}, Modifier.weight(1f))
        }
        PcCard { Column { StandingsHeader(showForm = true); PreviewData.table.forEach { StandingsRow(it, {}, showForm = true, followed = it.team.id == "ng") } } }
    }
}
