package com.innovativeinside.pitchside.ui.components

import androidx.compose.animation.Animatable
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.ContentTransform
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.KeyboardArrowRight
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.MoreVert
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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
    val p = m.probs
    if (p != null && m.status == Status.Upcoming) append(", win chance ${m.home.name} ${p.home}%, draw ${p.draw}%, ${m.away.name} ${p.away}%")
}

/**
 * Home win / draw / away win as one thin three-part bar (home emerald, draw grey, away muted). Decorative:
 * callers put the numbers in text or the content description.
 */
@Composable
fun ProbabilityBar(p: com.innovativeinside.pitchside.data.model.WinProbability, modifier: Modifier = Modifier, height: Dp = 6.dp, onHero: Boolean = false) {
    Row(modifier.height(height).clip(CircleShape).clearAndSetSemantics {}, horizontalArrangement = Arrangement.spacedBy(2.dp)) {
        Box(Modifier.weight(p.home.coerceAtLeast(1).toFloat()).fillMaxHeight().background(if (onHero) Pc.HeroAccent else Pc.Accent))
        Box(Modifier.weight(p.draw.coerceAtLeast(1).toFloat()).fillMaxHeight().background(if (onHero) Pc.OnHero2 else Pc.Text3))
        Box(Modifier.weight(p.away.coerceAtLeast(1).toFloat()).fillMaxHeight().background(if (onHero) Pc.HeroRaised else Pc.Neutral))
    }
}

// ---------------- Crest ----------------

enum class CrestSize(val dp: Dp) { S(24.dp), M(32.dp), L(48.dp), XL(64.dp) }

/**
 * CrestImage: the club crest from the data source through Coil (cached), drawn straight on the dark surface like
 * the reference. While it loads, or when there is none, the club initials on a charcoal disc with a team-colour
 * ring. Decorative for TalkBack: the team name is always next to it.
 */
@Composable
fun TeamCrest(team: Team, size: Dp = CrestSize.M.dp, modifier: Modifier = Modifier, onHero: Boolean = false, ring: Boolean = true) {
    val ringWidth = if (size >= 48.dp) 2.5.dp else 1.5.dp
    val style = when {
        size >= 64.dp -> PcType.TeamName.copy(fontSize = 22.sp)
        size >= 48.dp -> PcType.TeamName
        size >= 32.dp -> PcType.LabelStrong
        else -> PcType.Micro
    }
    val initials: @Composable () -> Unit = {
        Box(
            Modifier.fillMaxSize().clip(CircleShape).background(if (onHero) Pc.HeroRaised else Pc.SurfaceVariant)
                .let { if (ring) it.border(ringWidth, Pc.team(team.color), CircleShape) else it },
            contentAlignment = Alignment.Center,
        ) { Text(team.short.take(3).uppercase(), style = style, color = if (onHero) Pc.OnHero else Pc.Text, maxLines = 1) }
    }
    Box(modifier.size(size).clearAndSetSemantics {}, contentAlignment = Alignment.Center) {
        val logo = team.logo
        if (logo == null) initials()
        else RemoteImage(logo, initials, Modifier.fillMaxSize(), ContentScale.Fit)
    }
}

/** The design-system name for [TeamCrest]. */
@Composable
fun CrestImage(team: Team, size: Dp = CrestSize.M.dp, modifier: Modifier = Modifier, onHero: Boolean = false) = TeamCrest(team, size, modifier, onHero)

/**
 * A remote image (crest, logo, player photo) through Coil with [fallback] shown while loading and on error, so a slow or
 * missing image never leaves a hole. Decorative: callers give the name in text next to it.
 */
@Composable
fun RemoteImage(url: String, fallback: @Composable () -> Unit, modifier: Modifier = Modifier, scale: ContentScale = ContentScale.Crop) {
    val ctx = androidx.compose.ui.platform.LocalContext.current
    coil.compose.SubcomposeAsyncImage(
        model = remember(url) { coil.request.ImageRequest.Builder(ctx).data(url).crossfade(true).build() },
        contentDescription = null,
        contentScale = scale,
        loading = { fallback() },
        error = { fallback() },
        modifier = modifier,
    )
}

/** Older call sites. */
@Composable
fun Crest(team: Team, size: Dp = 24.dp) = TeamCrest(team, size)

// ---------------- Live badge and status ----------------

/**
 * LiveBadge: "● LIVE 73'" with a softly pulsing emerald dot, emerald LIVE and the minute in white. The text
 * always says LIVE, so colour is never the only signal. The pulse stops when the user has reduced motion.
 */
@Composable
fun LiveBadge(minute: String?, modifier: Modifier = Modifier, onHero: Boolean = false, compact: Boolean = false) {
    val reduced = rememberReducedMotion()
    val green = if (onHero) Pc.HeroAccent else Pc.LiveText
    val minuteColor = if (onHero) Pc.OnHero else Pc.Text
    val pulse = if (reduced) 1f else {
        val t = rememberInfiniteTransition(label = "live")
        val a by t.animateFloat(1f, 0.35f, infiniteRepeatable(tween(1000), RepeatMode.Reverse), label = "pulse")
        a
    }
    val style = if (compact) PcType.Tag.copy(fontSize = 13.sp) else PcType.Tag.copy(fontSize = 17.sp)
    Row(
        modifier.heightIn(min = 22.dp).semantics(mergeDescendants = true) { contentDescription = if (minute.isNullOrBlank()) "Live" else "Live, $minute" },
        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(if (compact) 5.dp else 7.dp),
    ) {
        Box(contentAlignment = Alignment.Center) {
            if (!reduced) Box(Modifier.size(if (compact) 11.dp else 14.dp).scale(1.6f - 0.6f * pulse).clip(CircleShape).background(green.copy(alpha = 0.25f * pulse)))
            Box(Modifier.size(if (compact) 7.dp else 10.dp).clip(CircleShape).background(green))
        }
        Text("LIVE", style = style, color = green)
        if (!minute.isNullOrBlank()) Text(minute, style = style.copy(fontWeight = FontWeight.SemiBold), color = minuteColor)
    }
}

/** Compact status for any match: LIVE badge, kickoff time, FT or Postponed. */
@Composable
fun StatusBadge(status: Status, label: String, modifier: Modifier = Modifier) {
    when (status) {
        Status.Live -> LiveBadge(label, modifier, compact = true)
        Status.Upcoming -> Text(label, style = PcType.Tag, color = Pc.Text, modifier = modifier)
        Status.Finished -> Text("FT", style = PcType.Tag, color = Pc.Text2, modifier = modifier)
        Status.Postponed -> Box(modifier.clip(PcShape.Chip).background(Pc.WarningContainer).padding(horizontal = 8.dp, vertical = 2.dp)) {
            Text("Postponed", style = PcType.Caption, color = Pc.Text)
        }
    }
}

// ---------------- Score ----------------

/**
 * ScoreHero / shared score typography ("2 – 1"): condensed bold, tabular figures. A changed score slides in
 * from below (old one slides up and out) and briefly lights up, so a new goal is noticed. Both are skipped
 * on reduced motion.
 */
@Composable
fun ScoreText(home: Int?, away: Int?, modifier: Modifier = Modifier, style: TextStyle = PcType.ScoreDisplay, color: Color = Pc.Text, flashKey: Any? = null, flashColor: Color = Pc.Accent) {
    val reduced = rememberReducedMotion()
    val flash = remember { Animatable(Color.Transparent) }
    val first = remember { booleanArrayOf(true) }
    LaunchedEffect(flashKey) {
        if (first[0]) { first[0] = false; return@LaunchedEffect }
        if (reduced || flashKey == null) return@LaunchedEffect
        flash.snapTo(flashColor.copy(alpha = 0.30f))
        flash.animateTo(Color.Transparent, tween(700))
    }
    val text = if (home != null && away != null) "$home – $away" else "–"
    AnimatedContent(
        targetState = text,
        transitionSpec = {
            if (reduced) ContentTransform(EnterTransition.None, ExitTransition.None)
            else (slideInVertically(tween(280)) { it / 2 } + fadeIn(tween(280))) togetherWith (slideOutVertically(tween(200)) { -it / 2 } + fadeOut(tween(200)))
        },
        label = "score",
        modifier = modifier.clip(PcShape.Small).background(flash.value).padding(horizontal = 6.dp),
    ) { t -> Text(t, style = style, color = color, maxLines = 1) }
}

/** The design-system name for the big animated score. */
@Composable
fun ScoreHero(home: Int?, away: Int?, modifier: Modifier = Modifier, color: Color = Pc.OnHero, style: androidx.compose.ui.text.TextStyle = PcType.ScoreDisplay) =
    ScoreText(home, away, modifier, style, color, flashKey = "$home-$away", flashColor = Pc.HeroAccent)

/** Single number score for rows (keeps the old signature). */
@Composable
fun ScoreText(value: String, modifier: Modifier = Modifier, bold: Boolean = true) {
    Text(value, style = PcType.ScoreRow, color = if (bold) Pc.Text else Pc.Text2, modifier = modifier.widthIn(min = 18.dp), textAlign = TextAlign.End, maxLines = 1)
}

// ---------------- Live hero card ----------------

/**
 * The live hero (reference "LIVE MATCH" card) on a [GlowCard]: "● LIVE 73'" top left, competition logo centred on
 * top, overflow menu top right, large crests, uppercase team names, the huge score, a divider and a stats strip
 * (POSSESSION 52% - 48%   SHOTS 9 - 7) when the data source has stats for the match.
 */
@Composable
fun LiveMatchTile(
    match: Match,
    hidden: Boolean,
    onClick: () -> Unit,
    onReveal: () -> Unit,
    modifier: Modifier = Modifier,
    stats: List<StatRow> = emptyList(),
    /** Replaces the status text at the top left for non-live matches ("Tomorrow", "Sat 16 Nov"). */
    topRight: String? = null,
    onMenu: (() -> Unit)? = null,
    /** Spoken summary; defaults to "Featured match: <match>". */
    description: String? = null,
    /**
     * Compact layout for short screens (Home fits one viewport): smaller crests and score, one-line names, the
     * competition as a one-line label in the top row and no stats strip.
     */
    compact: Boolean = false,
    footer: @Composable ColumnScope.() -> Unit = {},
) {
    val spoken = description ?: ("Featured match: " + matchDescription(match, hidden))
    GlowCard(modifier.fillMaxWidth().semantics(mergeDescendants = true) { contentDescription = spoken }, onClick = onClick) {
        Column(
            Modifier.padding(horizontal = PcSpace.md, vertical = if (compact) PcSpace.xs else PcSpace.md),
            verticalArrangement = Arrangement.spacedBy(if (compact) PcSpace.xxs else PcSpace.sm),
        ) {
            // Status left, competition centred, menu right. Equal side weights keep the mark centred; with a large
            // font the mark moves to its own line so nothing overlaps.
            val roomy = !compact && androidx.compose.ui.platform.LocalDensity.current.fontScale <= 1.15f
            Row(Modifier.fillMaxWidth().heightIn(min = 40.dp), verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.weight(1f), contentAlignment = Alignment.CenterStart) {
                    when (match.status) {
                        Status.Live -> LiveBadge(match.label, onHero = true)
                        Status.Finished -> Text(topRight ?: "FT", style = PcType.Tag.copy(fontSize = 17.sp), color = Pc.OnHero2)
                        else -> Text((topRight ?: match.label).uppercase(), style = PcType.Tag.copy(fontSize = 15.sp), color = Pc.OnHero2, maxLines = 2)
                    }
                }
                if (roomy) CompetitionMark(match, Modifier.widthIn(max = 116.dp))
                else if (compact) Text(
                    match.leagueName.uppercase(), style = PcType.Overline.copy(fontSize = 11.sp, lineHeight = 13.sp), color = Pc.OnHero2,
                    maxLines = 1, overflow = TextOverflow.Ellipsis, textAlign = TextAlign.Center,
                    modifier = Modifier.widthIn(max = 140.dp).clearAndSetSemantics {},
                )
                Box(Modifier.weight(1f), contentAlignment = Alignment.CenterEnd) {
                    if (onMenu != null) Box(
                        Modifier.size(PcSpace.touch).pcClickable(PcShape.Control, "More options for this match", onClick = onMenu),
                        contentAlignment = Alignment.Center,
                    ) {
                        Box(Modifier.size(32.dp).clip(PcShape.Small).background(Pc.HeroRaised), contentAlignment = Alignment.Center) {
                            Icon(Icons.Outlined.MoreVert, "More options", tint = Pc.OnHero2, modifier = Modifier.size(20.dp))
                        }
                    }
                }
            }
            if (!roomy && !compact) CompetitionMark(match, Modifier.fillMaxWidth())
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                HeroTeam(match.home, compact)
                Box(Modifier.widthIn(min = if (compact) 96.dp else 104.dp), contentAlignment = Alignment.Center) {
                    if (hidden) {
                        Box(
                            Modifier.heightIn(min = PcSpace.touch).pcClickable(PcShape.Chip, "Reveal score", onClick = onReveal).background(Pc.HeroRaised).padding(horizontal = PcSpace.md),
                            contentAlignment = Alignment.Center,
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                Icon(Icons.Outlined.Visibility, null, tint = Pc.OnHero, modifier = Modifier.size(18.dp))
                                Text("Reveal", style = PcType.LabelStrong, color = Pc.OnHero)
                            }
                        }
                    } else if (match.hasScore) {
                        ScoreHero(match.homeScore, match.awayScore, style = if (compact) PcType.ScoreDisplay.copy(fontSize = 44.sp, lineHeight = 46.sp) else PcType.ScoreDisplay)
                    } else {
                        Text(match.label, style = PcType.ScoreCompact.copy(fontSize = if (compact) 32.sp else 40.sp, lineHeight = if (compact) 34.sp else 42.sp), color = Pc.OnHero, maxLines = 1)
                    }
                }
                HeroTeam(match.away, compact)
            }
            val strip = if (hidden || compact) emptyList() else stats.filter { it.label == "Possession" || it.label == "Shots" }
            if (strip.isNotEmpty()) {
                Hairline(color = Pc.OnHero.copy(alpha = 0.10f))
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(PcSpace.lg, Alignment.CenterHorizontally), verticalAlignment = Alignment.CenterVertically) {
                    strip.forEach { s ->
                        val t = buildAnnotatedString {
                            withStyle(SpanStyle(color = Pc.OnHero2, fontWeight = FontWeight.Medium)) { append(s.label.uppercase() + " ") }
                            withStyle(SpanStyle(color = Pc.OnHero, fontWeight = FontWeight.Bold)) { append("${s.homeText} - ${s.awayText}") }
                        }
                        Text(t, style = PcType.Tag.copy(fontSize = 16.sp), maxLines = 1)
                    }
                }
            }
            footer()
        }
    }
}

/** Competition logo (or monogram) with the name in small uppercase under it, centred on top of hero cards. */
@Composable
private fun CompetitionMark(match: Match, modifier: Modifier = Modifier) {
    Column(modifier.clearAndSetSemantics {}, horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(2.dp)) {
        val logo = match.leagueLogo
        if (logo != null) RemoteImage(logo, { Box(Modifier.fillMaxSize()) }, Modifier.size(30.dp), ContentScale.Fit)
        Text(match.leagueName.uppercase(), style = PcType.Overline.copy(fontSize = 11.sp, lineHeight = 13.sp), color = Pc.OnHero, maxLines = 2, overflow = TextOverflow.Ellipsis, textAlign = TextAlign.Center)
    }
}

@Composable
private fun RowScope.HeroTeam(team: Team, compact: Boolean = false) {
    Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(if (compact) PcSpace.xxs else PcSpace.xs)) {
        TeamCrest(team, if (compact) 40.dp else CrestSize.XL.dp, onHero = true)
        Text(
            team.name.uppercase(), style = if (compact) PcType.TeamNameSmall else PcType.TeamName, color = Pc.OnHero,
            maxLines = if (compact) 1 else 2, overflow = TextOverflow.Ellipsis, textAlign = TextAlign.Center,
        )
    }
}

// ---------------- Mini live tile ----------------

/** Compact card for another live match; tapping promotes it into the hero. */
@Composable
fun MiniLiveTile(match: Match, hidden: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier) {
    PcCard(
        modifier.width(176.dp).semantics(mergeDescendants = true) { contentDescription = matchDescription(match, hidden) + ". Show as featured match" },
        onClick = onClick,
    ) {
        Column(Modifier.padding(PcSpace.sm), verticalArrangement = Arrangement.spacedBy(PcSpace.xs)) {
            LiveBadge(match.label, compact = true)
            MiniTeamLine(match.home, if (hidden) null else match.homeScore, lead = (match.homeScore ?: 0) > (match.awayScore ?: 0))
            MiniTeamLine(match.away, if (hidden) null else match.awayScore, lead = (match.awayScore ?: 0) > (match.homeScore ?: 0))
        }
    }
}

@Composable
private fun MiniTeamLine(team: Team, score: Int?, lead: Boolean) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(PcSpace.xs)) {
        TeamCrest(team, CrestSize.S.dp)
        Text(team.short.uppercase(), style = PcType.TeamNameSmall, color = if (lead) Pc.Text else Pc.Text2, modifier = Modifier.weight(1f), maxLines = 1)
        Text(score?.toString() ?: "–", style = PcType.ScoreCompact, color = if (lead) Pc.Text else Pc.Text2)
    }
}

// ---------------- Match row ----------------

/** Short status shown under the score in a match row: "74'", "HT", "FT", "AET", "Pens 4-3", "Postp.". */
fun rowStatus(m: Match): String = when (m.status) {
    Status.Live -> m.label.ifBlank { "LIVE" }
    Status.Finished -> m.label.ifBlank { "FT" }
    Status.Postponed -> when {
        m.label.startsWith("Canc", true) -> "Canc."
        m.label.startsWith("Postp", true) || m.label.isBlank() -> "Postp."
        else -> m.label.take(6)
    }
    Status.Upcoming -> ""
}

/** Fixed row height so lists never jump while scores and statuses change. */
val MatchRowHeight = 68.dp

/**
 * One match in a list, laid out symmetrically like a scoreboard: home name (right-aligned) and crest | the score, or
 * the kickoff time for games not started, with the status under it (live minute in the live colour, FT, HT, Pens,
 * Postponed) | crest and away name (left-aligned). Once finished the winner is bold and the loser dimmed. One trailing
 * action keeps every row the same width: the kickoff reminder bell for upcoming games, otherwise the follow star.
 * The row height is fixed, so nothing moves when a score or status changes.
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
    val decided = match.status == Status.Finished && hs != null && aw != null && !hidden
    val homeWin = decided && hs!! > aw!!
    val awayWin = decided && aw!! > hs!!
    Row(
        modifier.fillMaxWidth().height(MatchRowHeight).pcClickable(PcShape.Control, "Open match", onClick = onClick)
            .semantics(mergeDescendants = true) { contentDescription = matchDescription(match, hidden) }
            .padding(start = PcSpace.sm),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        // Home: name right-aligned against its crest.
        Row(Modifier.weight(1f), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(PcSpace.xs, Alignment.End)) {
            RowTeamName(match.home.name, bold = homeWin, dim = awayWin, align = TextAlign.End, modifier = Modifier.weight(1f, fill = false))
            TeamCrest(match.home, CrestSize.S.dp)
        }
        RowCentre(match, hidden, onReveal, Modifier.width(78.dp))
        // Away: crest then name, left-aligned.
        Row(Modifier.weight(1f), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(PcSpace.xs)) {
            TeamCrest(match.away, CrestSize.S.dp)
            RowTeamName(match.away.name, bold = awayWin, dim = homeWin, align = TextAlign.Start, modifier = Modifier.weight(1f, fill = false))
        }
        Box(Modifier.width(PcSpace.touch).fillMaxHeight(), contentAlignment = Alignment.Center) {
            when {
                onReminder != null && match.status == Status.Upcoming -> Box(
                    Modifier.size(PcSpace.touch).pcClickable(CircleShape, if (reminderOn) "Turn off kickoff reminder" else "Remind me at kickoff", onClick = onReminder),
                    contentAlignment = Alignment.Center,
                ) { Icon(if (reminderOn) Icons.Filled.Notifications else Icons.Outlined.NotificationsNone, null, tint = if (reminderOn) Pc.Accent else Pc.Text3, modifier = Modifier.size(20.dp)) }
                onFavorite != null -> Box(
                    Modifier.size(PcSpace.touch).pcClickable(CircleShape, if (favorite) "Following a team in this match" else "Follow a team in this match", onClick = onFavorite),
                    contentAlignment = Alignment.Center,
                ) { Icon(if (favorite) Icons.Filled.Star else Icons.Outlined.StarBorder, null, tint = if (favorite) Pc.Accent else Pc.Text3, modifier = Modifier.size(20.dp)) }
            }
        }
    }
}

@Composable
private fun RowTeamName(name: String, bold: Boolean, dim: Boolean, align: TextAlign, modifier: Modifier = Modifier) {
    Text(
        name,
        style = PcType.Label.copy(fontSize = 14.sp, lineHeight = 17.sp, fontWeight = if (bold) FontWeight.Bold else FontWeight.Medium),
        color = if (dim) Pc.Text2 else Pc.Text,
        maxLines = 2, overflow = TextOverflow.Ellipsis, textAlign = align, modifier = modifier,
    )
}

/** Score (or kickoff time) over the status line. Both lines always take their space so rows line up. */
@Composable
private fun RowCentre(match: Match, hidden: Boolean, onReveal: () -> Unit, modifier: Modifier) {
    val status = rowStatus(match)
    val statusColor = when (match.status) {
        Status.Live -> Pc.LiveText
        Status.Postponed -> Pc.Warning
        else -> Pc.Text2
    }
    Column(modifier, horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
        Box(Modifier.height(24.dp), contentAlignment = Alignment.Center) {
            when {
                match.status == Status.Upcoming -> Text(match.label, style = PcType.Tag.copy(fontSize = 16.sp), color = Pc.Text, maxLines = 1)
                hidden && match.hasScore -> Box(
                    Modifier.size(width = 56.dp, height = 24.dp).pcClickable(PcShape.Chip, "Reveal score", onClick = onReveal).background(Pc.SurfaceVariant),
                    contentAlignment = Alignment.Center,
                ) { Icon(Icons.Outlined.Visibility, null, tint = Pc.Text2, modifier = Modifier.size(16.dp)) }
                match.hasScore -> Text(
                    "${match.homeScore} - ${match.awayScore}", style = PcType.ScoreRow,
                    color = if (match.status == Status.Live) Pc.LiveText else Pc.Text, maxLines = 1,
                )
                else -> Text("vs", style = PcType.Label, color = Pc.Text3, maxLines = 1)
            }
        }
        Box(Modifier.height(16.dp), contentAlignment = Alignment.Center) {
            if (status.isNotEmpty()) Text(
                status, style = PcType.Caption.copy(fontWeight = if (match.status == Status.Live) FontWeight.Bold else FontWeight.Medium),
                color = statusColor, maxLines = 1, overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

/** Older call sites. */
@Composable
fun MatchCard(match: Match, hidden: Boolean, onClick: () -> Unit, onReveal: () -> Unit, modifier: Modifier = Modifier) =
    MatchRow(match, hidden, onClick, onReveal, modifier)

// ---------------- Highlight row ----------------

/**
 * HighlightRow (reference "TODAY'S HIGHLIGHTS"): "crest MAN UNITED vs COPENHAGEN • 21:00" / "• FT 2 - 1" / "• LIVE 67'"
 * on one line that wraps to two when names are long, the follow star on the right and, for upcoming games, the
 * kickoff reminder bell. Live games get the thin emerald minute bar.
 */
@Composable
fun HighlightRow(
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
    val name = PcType.TeamNameSmall.copy(fontSize = 17.sp)
    val grey = Pc.Text2
    val green = Pc.LiveText
    val text = buildAnnotatedString {
        append(match.home.name.uppercase())
        withStyle(SpanStyle(color = grey, fontWeight = FontWeight.Medium)) { append(" vs ") }
        append(match.away.name.uppercase())
        withStyle(SpanStyle(color = grey, fontWeight = FontWeight.Medium)) {
            append(" • ")
            when (match.status) {
                Status.Upcoming -> append(match.label)
                Status.Finished -> append(if (match.hasScore && !hidden) "FT ${match.homeScore} - ${match.awayScore}" else "FT")
                Status.Postponed -> append("Postponed")
                Status.Live -> Unit
            }
        }
        if (match.status == Status.Live) withStyle(SpanStyle(color = green, fontWeight = FontWeight.Bold)) {
            append("LIVE ${match.label}")
            if (match.hasScore && !hidden) append("  ${match.homeScore} - ${match.awayScore}")
        }
    }
    Column(
        modifier.fillMaxWidth().heightIn(min = 60.dp).pcClickable(PcShape.Control, "Open match", onClick = onClick)
            .semantics(mergeDescendants = true) { contentDescription = matchDescription(match, hidden) },
    ) {
        Row(Modifier.fillMaxWidth().heightIn(min = 58.dp).padding(start = PcSpace.md, end = PcSpace.xxs), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(PcSpace.sm)) {
            TeamCrest(match.home, 30.dp)
            Text(text, style = name, modifier = Modifier.weight(1f).padding(vertical = PcSpace.xs), maxLines = 2, overflow = TextOverflow.Ellipsis)
            if (hidden && match.hasScore) Box(Modifier.size(PcSpace.touch).pcClickable(CircleShape, "Reveal score", onClick = onReveal), contentAlignment = Alignment.Center) {
                Icon(Icons.Outlined.Visibility, "Reveal score", tint = Pc.Text2, modifier = Modifier.size(20.dp))
            }
            if (onReminder != null && match.status == Status.Upcoming) Box(Modifier.size(PcSpace.touch).pcClickable(CircleShape, if (reminderOn) "Turn off kickoff reminder" else "Remind me at kickoff", onClick = onReminder), contentAlignment = Alignment.Center) {
                Icon(if (reminderOn) Icons.Filled.Notifications else Icons.Outlined.NotificationsNone, if (reminderOn) "Reminder on" else "Reminder off", tint = if (reminderOn) Pc.Accent else Pc.Text3, modifier = Modifier.size(20.dp))
            }
            if (onFavorite != null) Box(Modifier.size(PcSpace.touch).pcClickable(CircleShape, if (favorite) "Following a team in this match" else "Follow a team in this match", onClick = onFavorite), contentAlignment = Alignment.Center) {
                Icon(Icons.Filled.Star, if (favorite) "Following" else "Follow", tint = if (favorite) Pc.Accent else Pc.Text3, modifier = Modifier.size(24.dp))
            }
        }
        if (match.status == Status.Live) liveProgress(match.label)?.let { p ->
            Box(Modifier.fillMaxWidth().padding(horizontal = PcSpace.md).height(2.dp).clip(CircleShape).background(Pc.LiveContainer)) {
                Box(Modifier.fillMaxWidth(p).fillMaxHeight().clip(CircleShape).background(Pc.Live))
            }
        }
    }
}

// ---------------- Compact match card ----------------

/**
 * MatchCardCompact (reference "TOP MATCHES"): crest, competition logo over the big kickoff time (or score / LIVE),
 * crest; uppercase names underneath.
 */
@Composable
fun MatchCardCompact(match: Match, hidden: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier, dense: Boolean = false) {
    // [dense]: the Home rail on a single-viewport screen; smaller crests and score, same reading order.
    val crest = if (dense) 34.dp else 44.dp
    val score = if (dense) PcType.ScoreCompact.copy(fontSize = 24.sp, lineHeight = 26.sp) else PcType.ScoreCompact
    PcCard(modifier.width(if (dense) 208.dp else 232.dp).semantics(mergeDescendants = true) { contentDescription = match.leagueName + ", " + matchDescription(match, hidden) }, onClick = onClick) {
        Column(
            Modifier.padding(horizontal = PcSpace.sm, vertical = if (dense) PcSpace.sm else PcSpace.md),
            verticalArrangement = Arrangement.spacedBy(if (dense) PcSpace.xs else PcSpace.sm),
        ) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.weight(1f), contentAlignment = Alignment.Center) { TeamCrest(match.home, crest) }
                Column(Modifier.widthIn(min = 76.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    val logo = match.leagueLogo
                    if (logo != null) RemoteImage(logo, { Box(Modifier.fillMaxSize()) }, Modifier.size(if (dense) 18.dp else 22.dp), ContentScale.Fit)
                    else Text(match.leagueName.uppercase(), style = PcType.Overline.copy(fontSize = 10.sp), maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.widthIn(max = 76.dp))
                    when (match.status) {
                        Status.Upcoming -> Text(match.label, style = score, maxLines = 1)
                        Status.Live -> {
                            if (match.hasScore && !hidden) Text("${match.homeScore} – ${match.awayScore}", style = score, maxLines = 1)
                            LiveBadge(match.label, compact = true)
                        }
                        Status.Finished -> {
                            Text(if (match.hasScore && !hidden) "${match.homeScore} – ${match.awayScore}" else "FT", style = score, maxLines = 1)
                            if (match.hasScore && !hidden && !dense) Text("FT", style = PcType.Overline)
                        }
                        Status.Postponed -> Text("PPD", style = score, color = Pc.Warning, maxLines = 1)
                    }
                }
                Box(Modifier.weight(1f), contentAlignment = Alignment.Center) { TeamCrest(match.away, crest) }
            }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(PcSpace.xs)) {
                Text(match.home.name.uppercase(), style = PcType.TeamNameSmall, maxLines = 1, overflow = TextOverflow.Ellipsis, textAlign = TextAlign.Center, modifier = Modifier.weight(1f))
                Text(match.away.name.uppercase(), style = PcType.TeamNameSmall, maxLines = 1, overflow = TextOverflow.Ellipsis, textAlign = TextAlign.Center, modifier = Modifier.weight(1f))
            }
        }
    }
}

/** Older name for [MatchCardCompact]. */
@Composable
fun FixtureRailItem(match: Match, hidden: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier) = MatchCardCompact(match, hidden, onClick, modifier)

// ---------------- Previews ----------------

@Preview(name = "Live hero · dark", showBackground = true, widthDp = 380, backgroundColor = 0xFF0E1110)
@Composable
private fun LiveTileDarkPreview() = PreviewHost(dark = true) {
    Column(Modifier.padding(PcSpace.md), verticalArrangement = Arrangement.spacedBy(PcSpace.sm)) {
        LiveMatchTile(PreviewData.live, false, {}, {}, stats = listOf(StatRow("Possession", 52, 48, "52%", "48%"), StatRow("Shots", 9, 7, "9", "7")), onMenu = {})
        Row(horizontalArrangement = Arrangement.spacedBy(PcSpace.xs)) { PreviewData.matches.take(2).forEach { MatchCardCompact(it, false, {}) } }
    }
}

@Preview(name = "Live hero · light", showBackground = true, widthDp = 380)
@Composable
private fun LiveTileLightPreview() = PreviewHost(dark = false) {
    Column(Modifier.padding(PcSpace.md)) { LiveMatchTile(PreviewData.live, false, {}, {}) }
}

@Preview(name = "Match rows", showBackground = true, widthDp = 360, backgroundColor = 0xFF0E1110)
@Composable
private fun MatchRowsPreview() = PreviewHost(dark = true) {
    Column(Modifier.padding(PcSpace.md)) {
        PreviewData.matches.forEach { MatchRow(it, false, {}, {}, onFavorite = {}, onReminder = {}) }
        PreviewData.matches.forEach { HighlightRow(it, false, {}, {}, onFavorite = {}) }
        Row(horizontalArrangement = Arrangement.spacedBy(PcSpace.xs), verticalAlignment = Alignment.CenterVertically) {
            CrestSize.entries.forEach { TeamCrest(PreviewData.live.home, it.dp) }
            LiveBadge("74'")
        }
    }
}
