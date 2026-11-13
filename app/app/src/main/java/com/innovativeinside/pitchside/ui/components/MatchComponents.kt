package com.innovativeinside.pitchside.ui.components

import androidx.compose.animation.Animatable
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.KeyboardArrowRight
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.NotificationsNone
import androidx.compose.material.icons.outlined.StarBorder
import androidx.compose.material.icons.outlined.Visibility
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.innovativeinside.pitchside.data.model.Match
import com.innovativeinside.pitchside.data.model.StatRow
import com.innovativeinside.pitchside.data.model.Status
import com.innovativeinside.pitchside.data.model.Team
import com.innovativeinside.pitchside.ui.preview.PreviewData
import com.innovativeinside.pitchside.ui.preview.PreviewHost
import com.innovativeinside.pitchside.ui.theme.Pc
import com.innovativeinside.pitchside.ui.theme.PcShape
import com.innovativeinside.pitchside.ui.theme.PcSpace
import com.innovativeinside.pitchside.ui.theme.PcType
import com.innovativeinside.pitchside.ui.vm.liveProgress

// ---------------- Accessibility text ----------------

private fun ordinal(n: Int): String = n.toString() + when {
    n % 100 in 11..13 -> "th"
    n % 10 == 1 -> "st"
    n % 10 == 2 -> "nd"
    n % 10 == 3 -> "rd"
    else -> "th"
}

/** Spoken status: "74th minute, live", "half time, live", "full time", "kicks off at 20:45", "postponed". */
fun spokenStatus(m: Match): String = when (m.status) {
    Status.Live -> {
        val minute = Regex("^(\\d+)").find(m.label)?.groupValues?.get(1)?.toIntOrNull()
        when {
            m.label.equals("HT", true) -> "half time, live"
            minute != null -> "${ordinal(minute)} minute, live"
            else -> "live"
        }
    }
    Status.Finished -> "full time"
    Status.Upcoming -> "kicks off at ${m.label}"
    Status.Postponed -> "postponed"
}

/** "Northgate 2, Riverside 1, 74th minute, live" (scores left out when hidden or not started). */
fun matchDescription(m: Match, hidden: Boolean = false): String = buildString {
    val showScore = m.hasScore && !hidden
    append(m.home.name); if (showScore) append(" ${m.homeScore}")
    append(", "); append(m.away.name); if (showScore) append(" ${m.awayScore}")
    append(", "); append(spokenStatus(m))
    if (hidden) append(", score hidden by Spoiler Shield")
}

// ---------------- Crest ----------------

enum class CrestSize(val dp: Dp) { S(24.dp), M(32.dp), L(48.dp), XL(64.dp) }

/**
 * Circular team badge. No crest images exist yet, so it shows the club initials on a neutral disc with a
 * team-colour ring (the only place a club colour appears at this size). Decorative for TalkBack: the
 * team name is always next to it.
 */
@Composable
fun TeamCrest(team: Team, size: Dp = CrestSize.M.dp, modifier: Modifier = Modifier, onHero: Boolean = false, ring: Boolean = true) {
    val ringWidth = if (size >= 48.dp) 3.dp else 2.dp
    val style = when {
        size >= 64.dp -> PcType.Subtitle
        size >= 48.dp -> PcType.BodyStrong
        size >= 32.dp -> PcType.LabelStrong
        else -> PcType.Micro
    }
    Box(
        modifier.size(size).clip(CircleShape).background(if (onHero) Pc.HeroRaised else Pc.SurfaceVariant)
            .let { if (ring) it.border(ringWidth, Pc.team(team.color), CircleShape) else it }
            .clearAndSetSemantics {},
        contentAlignment = Alignment.Center,
    ) {
        Text(team.short.take(3), style = style, color = if (onHero) Pc.OnHero else Pc.Text, maxLines = 1)
    }
}

/** Older call sites. */
@Composable
fun Crest(team: Team, size: Dp = 24.dp) = TeamCrest(team, size)

// ---------------- Live badge and status ----------------

/**
 * Red LIVE badge: a slowly pulsing dot, "LIVE" and the minute ("LIVE 74'"). The text always says LIVE,
 * so colour is never the only signal. The pulse stops when the user has reduced motion.
 */
@Composable
fun LiveBadge(minute: String?, modifier: Modifier = Modifier, onHero: Boolean = false) {
    val reduced = rememberReducedMotion()
    val bg = if (onHero) Pc.LiveFill else Pc.LiveContainer
    val fg = if (onHero) Pc.OnLive else Pc.LiveText
    val dot = if (onHero) Pc.OnLive else Pc.Live
    val pulse = if (reduced) 1f else {
        val t = rememberInfiniteTransition(label = "live")
        val a by t.animateFloat(1f, 0.35f, infiniteRepeatable(tween(1100), RepeatMode.Reverse), label = "pulse")
        a
    }
    Row(
        modifier.heightIn(min = 24.dp).clip(PcShape.Chip).background(bg).padding(horizontal = 9.dp, vertical = 3.dp)
            .semantics(mergeDescendants = true) { contentDescription = if (minute.isNullOrBlank()) "Live" else "Live, $minute" },
        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Box(Modifier.size(7.dp).scale(if (reduced) 1f else 0.85f + 0.15f * pulse).clip(CircleShape).background(dot.copy(alpha = pulse)))
        Text("LIVE", style = PcType.Overline.copy(fontWeight = FontWeight.Bold), color = fg)
        if (!minute.isNullOrBlank()) Text(minute, style = PcType.Numeric.copy(fontWeight = FontWeight.Bold), color = fg)
    }
}

/** Compact status for any match: LIVE badge, kickoff time, FT or Postponed. */
@Composable
fun StatusBadge(status: Status, label: String, modifier: Modifier = Modifier) {
    when (status) {
        Status.Live -> LiveBadge(label, modifier)
        Status.Upcoming -> Text(label, style = PcType.Numeric.copy(fontWeight = FontWeight.SemiBold), color = Pc.Text, modifier = modifier)
        Status.Finished -> Text("FT", style = PcType.LabelStrong, color = Pc.Text2, modifier = modifier)
        Status.Postponed -> Box(modifier.clip(PcShape.Chip).background(Pc.WarningContainer).padding(horizontal = 8.dp, vertical = 2.dp)) {
            Text("Postponed", style = PcType.Caption, color = Pc.Text)
        }
    }
}

// ---------------- Score ----------------

/**
 * Shared score typography ("2 – 1"): condensed bold, tabular figures. When [flashKey] changes after the
 * first composition, the score briefly lights up (600ms) so a new goal is noticed; skipped on reduced motion.
 */
@Composable
fun ScoreText(home: Int?, away: Int?, modifier: Modifier = Modifier, style: TextStyle = PcType.ScoreDisplay, color: Color = Pc.Text, flashKey: Any? = null, flashColor: Color = Pc.Live) {
    val reduced = rememberReducedMotion()
    val flash = remember { Animatable(Color.Transparent) }
    val first = remember { booleanArrayOf(true) }
    LaunchedEffect(flashKey) {
        if (first[0]) { first[0] = false; return@LaunchedEffect }
        if (reduced || flashKey == null) return@LaunchedEffect
        flash.snapTo(flashColor.copy(alpha = 0.35f))
        flash.animateTo(Color.Transparent, tween(600))
    }
    val text = if (home != null && away != null) "$home – $away" else "–"
    Text(text, style = style, color = color, maxLines = 1, modifier = modifier.clip(PcShape.Small).background(flash.value).padding(horizontal = 4.dp))
}

/** Single number score for rows (keeps the old signature). */
@Composable
fun ScoreText(value: String, modifier: Modifier = Modifier, bold: Boolean = true) {
    Text(value, style = PcType.ScoreRow, color = if (bold) Pc.Text else Pc.Text2, modifier = modifier.widthIn(min = 18.dp), textAlign = TextAlign.End, maxLines = 1)
}

// ---------------- Live hero tile ----------------

/**
 * The dominant 2x2 live tile: deep-green stage, competition and venue overline, LIVE badge, both crests
 * and names, the 56sp score, an optional possession/shots strip, and the AI insight line from Match.brief.
 */
@Composable
fun LiveMatchTile(
    match: Match,
    hidden: Boolean,
    onClick: () -> Unit,
    onReveal: () -> Unit,
    modifier: Modifier = Modifier,
    stats: List<StatRow> = emptyList(),
) {
    PcCard(
        modifier.fillMaxWidth().semantics(mergeDescendants = true) { contentDescription = "Featured match: " + matchDescription(match, hidden) },
        container = Pc.Hero, border = Pc.HeroOutline, onClick = onClick,
    ) {
        Column(Modifier.padding(PcSpace.md), verticalArrangement = Arrangement.spacedBy(PcSpace.sm)) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text(
                    listOf(match.leagueName, match.venue).filter { it.isNotBlank() }.joinToString(" · ").uppercase(),
                    style = PcType.Overline, color = Pc.OnHero2, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f),
                )
                Spacer(Modifier.width(PcSpace.xs))
                when (match.status) {
                    Status.Live -> LiveBadge(match.label, onHero = true)
                    else -> Text(if (match.status == Status.Finished) "FT" else match.label, style = PcType.LabelStrong, color = Pc.OnHero)
                }
            }
            Row(Modifier.fillMaxWidth().padding(vertical = PcSpace.xs), verticalAlignment = Alignment.CenterVertically) {
                HeroTeam(match.home)
                Box(Modifier.widthIn(min = 112.dp), contentAlignment = Alignment.Center) {
                    if (hidden) {
                        Box(
                            Modifier.heightIn(min = PcSpace.touch).clip(PcShape.Control).background(Pc.HeroRaised).pcClickable(PcShape.Control, "Reveal score", onClick = onReveal).padding(horizontal = PcSpace.md),
                            contentAlignment = Alignment.Center,
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                Icon(Icons.Outlined.Visibility, null, tint = Pc.OnHero, modifier = Modifier.size(18.dp))
                                Text("Reveal", style = PcType.LabelStrong, color = Pc.OnHero)
                            }
                        }
                    } else if (match.hasScore) {
                        ScoreText(match.homeScore, match.awayScore, color = Pc.OnHero, flashKey = "${match.homeScore}-${match.awayScore}", flashColor = Pc.OnHero)
                    } else {
                        Text(match.label, style = PcType.ScoreCompact, color = Pc.OnHero)
                    }
                }
                HeroTeam(match.away)
            }
            val strip = if (hidden) emptyList() else stats.filter { it.label == "Possession" || it.label == "Shots" }
            if (strip.isNotEmpty()) {
                Row(Modifier.fillMaxWidth().clip(PcShape.Control).background(Pc.HeroRaised).padding(horizontal = PcSpace.sm, vertical = PcSpace.xs), horizontalArrangement = Arrangement.SpaceEvenly) {
                    strip.forEach { s ->
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text(s.homeText, style = PcType.Numeric.copy(fontWeight = FontWeight.Bold), color = Pc.OnHero)
                            Text(s.label.uppercase(), style = PcType.Overline, color = Pc.OnHero2)
                            Text(s.awayText, style = PcType.Numeric.copy(fontWeight = FontWeight.Bold), color = Pc.OnHero)
                        }
                    }
                }
            }
            if (match.status == Status.Live) liveProgress(match.label)?.let { p ->
                Box(Modifier.fillMaxWidth().height(3.dp).clip(CircleShape).background(Pc.HeroRaised)) {
                    Box(Modifier.fillMaxWidth(p).fillMaxHeight().clip(CircleShape).background(Pc.Live))
                }
            }
            if (match.brief.isNotBlank()) {
                Row(horizontalArrangement = Arrangement.spacedBy(PcSpace.xs), verticalAlignment = Alignment.Top) {
                    Icon(Icons.Outlined.AutoAwesome, null, tint = Pc.OnHero2, modifier = Modifier.padding(top = 2.dp).size(16.dp))
                    Text(
                        if (hidden) "Brief hidden to avoid spoilers." else match.brief,
                        style = PcType.Label, color = Pc.OnHero, maxLines = 3, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f),
                    )
                }
            }
        }
    }
}

@Composable
private fun RowScope.HeroTeam(team: Team) {
    Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(PcSpace.xs)) {
        TeamCrest(team, CrestSize.L.dp, onHero = true)
        Text(team.name, style = PcType.BodyStrong, color = Pc.OnHero, maxLines = 2, overflow = TextOverflow.Ellipsis, textAlign = TextAlign.Center)
    }
}

// ---------------- Mini live tile ----------------

/** Compact tile for the other live matches; tapping promotes it into the hero. */
@Composable
fun MiniLiveTile(match: Match, hidden: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier) {
    PcCard(
        modifier.width(168.dp).semantics(mergeDescendants = true) { contentDescription = matchDescription(match, hidden) + ". Show as featured match" },
        onClick = onClick,
    ) {
        Column(Modifier.padding(PcSpace.sm), verticalArrangement = Arrangement.spacedBy(PcSpace.xs)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.size(6.dp).clip(CircleShape).background(Pc.Live))
                Spacer(Modifier.width(6.dp))
                Text(match.label, style = PcType.Numeric.copy(fontWeight = FontWeight.Bold), color = Pc.LiveText)
                Spacer(Modifier.width(6.dp))
                Text(match.leagueName.uppercase(), style = PcType.Overline, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f))
            }
            MiniTeamLine(match.home, if (hidden) null else match.homeScore, lead = (match.homeScore ?: 0) > (match.awayScore ?: 0))
            MiniTeamLine(match.away, if (hidden) null else match.awayScore, lead = (match.awayScore ?: 0) > (match.homeScore ?: 0))
        }
    }
}

@Composable
private fun MiniTeamLine(team: Team, score: Int?, lead: Boolean) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(PcSpace.xs)) {
        TeamCrest(team, CrestSize.S.dp)
        Text(team.short, style = PcType.LabelStrong, color = if (lead) Pc.Text else Pc.Text2, modifier = Modifier.weight(1f), maxLines = 1)
        Text(score?.toString() ?: "–", style = PcType.ScoreCompact, color = if (lead) Pc.Text else Pc.Text2)
    }
}

// ---------------- Match row ----------------

/**
 * One match in a list. Left: live minute (red, with a thin progress bar under the row), kickoff time,
 * FT or Postponed. Middle: both teams on two lines (winner emphasised once finished). Right: scores,
 * then a favourite star (follow a team in this match), a reminder bell for upcoming games, and a chevron.
 */
@Composable
fun MatchRow(
    match: Match,
    hidden: Boolean,
    onClick: () -> Unit,
    onReveal: () -> Unit,
    modifier: Modifier = Modifier,
    favorite: Boolean = false,
    onFavorite: (() -> Unit)? = null,
    reminderOn: Boolean = false,
    onReminder: (() -> Unit)? = null,
) {
    val hs = match.homeScore
    val aw = match.awayScore
    val finished = match.status == Status.Finished
    val homeWin = finished && hs != null && aw != null && hs > aw
    val awayWin = finished && hs != null && aw != null && aw > hs
    Column(
        modifier.fillMaxWidth().heightIn(min = 72.dp).pcClickable(PcShape.Control, "Open match", onClick = onClick)
            .semantics(mergeDescendants = true) { contentDescription = matchDescription(match, hidden) },
    ) {
        Row(Modifier.fillMaxWidth().heightIn(min = 70.dp).padding(start = PcSpace.sm, end = PcSpace.xxs), verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.width(52.dp), contentAlignment = Alignment.CenterStart) {
                when (match.status) {
                    Status.Live -> Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(Modifier.size(6.dp).clip(CircleShape).background(Pc.Live)); Spacer(Modifier.width(4.dp))
                            Text(match.label, style = PcType.Numeric.copy(fontWeight = FontWeight.Bold), color = Pc.LiveText, maxLines = 1)
                        }
                        Text("LIVE", style = PcType.Overline, color = Pc.LiveText)
                    }
                    Status.Upcoming -> Text(match.label, style = PcType.Numeric.copy(fontWeight = FontWeight.SemiBold), maxLines = 1)
                    Status.Finished -> Text("FT", style = PcType.LabelStrong, color = Pc.Text2)
                    Status.Postponed -> Text("PPD", style = PcType.LabelStrong, color = Pc.Warning)
                }
            }
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                RowTeam(match.home, hs, hidden, emphasise = homeWin || (!finished), dim = awayWin)
                RowTeam(match.away, aw, hidden, emphasise = awayWin || (!finished), dim = homeWin)
            }
            if (hidden) {
                Box(Modifier.size(PcSpace.touch).pcClickable(PcShape.Control, "Reveal score", onClick = onReveal), contentAlignment = Alignment.Center) {
                    Icon(Icons.Outlined.Visibility, "Reveal score", tint = Pc.Text2, modifier = Modifier.size(20.dp))
                }
            }
            if (onFavorite != null) Box(Modifier.size(PcSpace.touch).pcClickable(PcShape.Control, if (favorite) "Following a team in this match" else "Follow a team in this match", onClick = onFavorite), contentAlignment = Alignment.Center) {
                Icon(if (favorite) Icons.Filled.Star else Icons.Outlined.StarBorder, if (favorite) "Following" else "Follow", tint = if (favorite) Pc.Accent else Pc.Text2, modifier = Modifier.size(20.dp))
            }
            if (onReminder != null && match.status == Status.Upcoming) Box(Modifier.size(PcSpace.touch).pcClickable(PcShape.Control, if (reminderOn) "Turn off kickoff reminder" else "Remind me at kickoff", onClick = onReminder), contentAlignment = Alignment.Center) {
                Icon(if (reminderOn) Icons.Filled.Notifications else Icons.Outlined.NotificationsNone, if (reminderOn) "Reminder on" else "Reminder off", tint = if (reminderOn) Pc.Accent else Pc.Text2, modifier = Modifier.size(20.dp))
            }
            Icon(Icons.AutoMirrored.Outlined.KeyboardArrowRight, null, tint = Pc.Text3, modifier = Modifier.size(18.dp))
        }
        if (match.status == Status.Live) liveProgress(match.label)?.let { p ->
            Box(Modifier.fillMaxWidth().padding(horizontal = PcSpace.sm).height(2.dp).clip(CircleShape).background(Pc.LiveContainer)) {
                Box(Modifier.fillMaxWidth(p).fillMaxHeight().clip(CircleShape).background(Pc.Live))
            }
        }
    }
}

@Composable
private fun RowTeam(team: Team, score: Int?, hidden: Boolean, emphasise: Boolean, dim: Boolean) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(PcSpace.xs)) {
        TeamCrest(team, CrestSize.S.dp)
        Text(
            team.name, style = if (emphasise && !dim) PcType.BodyStrong else PcType.Body, color = if (dim) Pc.Text2 else Pc.Text,
            maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f),
        )
        when {
            hidden -> Box(Modifier.size(width = 16.dp, height = 18.dp).clip(PcShape.Small).background(Pc.SurfaceVariant))
            score != null -> ScoreText(score.toString(), bold = !dim)
            else -> Spacer(Modifier.width(16.dp))
        }
    }
}

/** Older call sites. */
@Composable
fun MatchCard(match: Match, hidden: Boolean, onClick: () -> Unit, onReveal: () -> Unit, modifier: Modifier = Modifier) =
    MatchRow(match, hidden, onClick, onReveal, modifier)

// ---------------- Fixture rail ----------------

/** Compact horizontal fixture: competition overline, time / LIVE / FT, two crests with abbreviations. */
@Composable
fun FixtureRailItem(match: Match, hidden: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier) {
    PcCard(modifier.width(132.dp).semantics(mergeDescendants = true) { contentDescription = match.leagueName + ", " + matchDescription(match, hidden) }, onClick = onClick) {
        Column(Modifier.padding(PcSpace.sm), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(match.leagueName.uppercase(), style = PcType.Overline, maxLines = 1, overflow = TextOverflow.Ellipsis)
            when (match.status) {
                Status.Live -> Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(Modifier.size(6.dp).clip(CircleShape).background(Pc.Live)); Spacer(Modifier.width(4.dp))
                    Text(match.label, style = PcType.Numeric.copy(fontWeight = FontWeight.Bold), color = Pc.LiveText)
                }
                Status.Finished -> Text("FT", style = PcType.LabelStrong, color = Pc.Text2)
                Status.Postponed -> Text("Postponed", style = PcType.LabelStrong, color = Pc.Warning, maxLines = 1)
                Status.Upcoming -> Text(match.label, style = PcType.Numeric.copy(fontWeight = FontWeight.Bold))
            }
            listOf(match.home to match.homeScore, match.away to match.awayScore).forEach { (t, s) ->
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    TeamCrest(t, CrestSize.S.dp)
                    Text(t.short, style = PcType.LabelStrong, modifier = Modifier.weight(1f), maxLines = 1)
                    if (s != null && !hidden) Text(s.toString(), style = PcType.ScoreRow)
                }
            }
        }
    }
}

// ---------------- Previews ----------------

@Preview(name = "Live tile · light", showBackground = true, widthDp = 380)
@Composable
private fun LiveTilePreview() = PreviewHost(dark = false) {
    Column(Modifier.padding(PcSpace.md), verticalArrangement = Arrangement.spacedBy(PcSpace.sm)) {
        LiveMatchTile(PreviewData.live, false, {}, {}, stats = listOf(StatRow("Possession", 58, 42, "58%", "42%"), StatRow("Shots", 8, 5, "8", "5")))
        MiniLiveTile(PreviewData.live, false, {})
    }
}

@Preview(name = "Live tile · dark", showBackground = true, widthDp = 380, backgroundColor = 0xFF0C0F0D)
@Composable
private fun LiveTileDarkPreview() = PreviewHost(dark = true) {
    Column(Modifier.padding(PcSpace.md)) { LiveMatchTile(PreviewData.live, false, {}, {}) }
}

@Preview(name = "Match rows", showBackground = true, widthDp = 360)
@Composable
private fun MatchRowsPreview() = PreviewHost(dark = false) {
    Column(Modifier.padding(PcSpace.md)) {
        PreviewData.matches.forEach { MatchRow(it, false, {}, {}, onFavorite = {}, onReminder = {}) }
        Row(horizontalArrangement = Arrangement.spacedBy(PcSpace.xs)) { PreviewData.matches.forEach { FixtureRailItem(it, false, {}) } }
        Row(horizontalArrangement = Arrangement.spacedBy(PcSpace.xs), verticalAlignment = Alignment.CenterVertically) {
            CrestSize.entries.forEach { TeamCrest(PreviewData.live.home, it.dp) }
            LiveBadge("74'")
        }
    }
}
