package com.innovativeinside.pitchside.ui.components

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Newspaper
import androidx.compose.material.icons.filled.SportsSoccer
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.ErrorOutline
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Newspaper
import androidx.compose.material.icons.outlined.Pause
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material.icons.outlined.SportsSoccer
import androidx.compose.material.icons.outlined.Visibility
import androidx.compose.material.icons.outlined.WifiOff
import androidx.compose.material.icons.outlined.EmojiEvents
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material3.Icon
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.disabled
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.res.stringResource
import com.innovativeinside.pitchside.R
import com.innovativeinside.pitchside.data.model.Match
import com.innovativeinside.pitchside.data.model.StatRow
import com.innovativeinside.pitchside.data.model.Status
import com.innovativeinside.pitchside.data.model.Team
import com.innovativeinside.pitchside.ui.theme.Pc
import com.innovativeinside.pitchside.ui.theme.PcShape
import com.innovativeinside.pitchside.ui.theme.PcType

/** Visible keyboard focus ring, matching the canvas. Place before clickable. */
fun Modifier.focusRing(shape: Shape = PcShape.Control): Modifier = composed2 {
    var focused by remember { mutableStateOf(false) }
    this.onFocusChanged { focused = it.isFocused }
        .then(if (focused) Modifier.border(2.dp, Pc.Accent, shape) else Modifier)
}

@Suppress("UnnecessaryComposedModifier")
private fun Modifier.composed2(factory: @Composable Modifier.() -> Modifier): Modifier =
    this.composed { factory() }

fun Modifier.pcClickable(shape: Shape = PcShape.Control, label: String? = null, role: Role = Role.Button, onClick: () -> Unit): Modifier =
    this.focusRing(shape).clip(shape).clickable(role = role, onClickLabel = label, onClick = onClick)

// ---- Cards and surfaces ----

@Composable
fun PcCard(
    modifier: Modifier = Modifier,
    shape: Shape = PcShape.Card,
    container: Color = Pc.Surface,
    border: Color = Pc.Outline,
    onClick: (() -> Unit)? = null,
    content: @Composable () -> Unit,
) {
    val base = modifier.clip(shape).background(container, shape).border(1.dp, border, shape)
    Box(if (onClick != null) base.pcClickable(shape, onClick = onClick) else base) { content() }
}

@Composable
fun Crest(team: Team, size: Dp = 24.dp) {
    // Decorative: the team name is always next to it, so TalkBack skips the monogram.
    Box(Modifier.size(size).clip(CircleShape).background(Color(team.color)).clearAndSetSemantics {}, contentAlignment = Alignment.Center) {
        Text(team.short, color = Color.White, fontWeight = FontWeight.ExtraBold, fontSize = (size.value * 0.36f).sp, maxLines = 1)
    }
}

@Composable
fun StatusBadge(status: Status, label: String, modifier: Modifier = Modifier) {
    val (bg, fg) = when (status) {
        Status.Live -> Pc.LiveContainer to Pc.LiveText
        Status.Upcoming -> Pc.SurfaceVariant to Pc.Text
        Status.Finished -> Pc.SurfaceVariant to Pc.Text2
        Status.Postponed -> Pc.WarningContainer to Pc.Warning
    }
    val text = when (status) {
        Status.Live -> "LIVE $label"
        Status.Finished -> "Full time"
        else -> label
    }
    Row(
        modifier.height(24.dp).clip(PcShape.Badge).background(bg).padding(horizontal = 8.dp),
        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        when (status) {
            Status.Live -> Box(Modifier.size(6.dp).clip(CircleShape).background(fg))
            Status.Upcoming -> Icon(Icons.Outlined.Schedule, null, tint = fg, modifier = Modifier.size(14.dp))
            Status.Finished -> Icon(Icons.Outlined.Check, null, tint = fg, modifier = Modifier.size(14.dp))
            Status.Postponed -> Icon(Icons.Outlined.Pause, null, tint = fg, modifier = Modifier.size(14.dp))
        }
        Text(text, style = PcType.Caption, color = fg, fontWeight = FontWeight.ExtraBold, maxLines = 1)
    }
}

@Composable
fun ScoreText(value: String, modifier: Modifier = Modifier, bold: Boolean = true) {
    Text(value, style = PcType.ScoreCompact, color = if (bold) Pc.Text else Pc.Text2, modifier = modifier.widthIn(min = 20.dp), textAlign = TextAlign.End)
}

@Composable
fun MatchCard(match: Match, hidden: Boolean, onClick: () -> Unit, onReveal: () -> Unit, modifier: Modifier = Modifier) {
    val hs = match.homeScore
    val a = match.awayScore
    val desc = buildString {
        append(match.home.name); if (hs != null && !hidden) append(" $hs")
        append(" versus "); append(match.away.name); if (a != null && !hidden) append(" $a")
        append(", "); append(when (match.status) { Status.Live -> "live ${match.label}"; Status.Finished -> "full time"; Status.Upcoming -> "kicks off ${match.label}"; Status.Postponed -> "postponed" })
        if (hidden) append(", score hidden by Spoiler Shield")
    }
    PcCard(modifier.semantics(mergeDescendants = true) { contentDescription = desc }, onClick = onClick) {
        Row(Modifier.fillMaxWidth().heightIn(min = 80.dp).padding(horizontal = 16.dp, vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.width(96.dp), contentAlignment = Alignment.CenterStart) { StatusBadge(match.status, match.label) }
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                TeamScoreRow(match.home, hs, hidden, hs != null && a != null && hs > a)
                TeamScoreRow(match.away, a, hidden, hs != null && a != null && a > hs)
            }
            Spacer(Modifier.width(8.dp))
            if (hidden) {
                Box(
                    Modifier.size(48.dp).pcClickable(PcShape.Control, "Reveal score", onClick = onReveal).background(Pc.SurfaceVariant).border(1.dp, Pc.Outline, PcShape.Control),
                    contentAlignment = Alignment.Center,
                ) { Icon(Icons.Outlined.Visibility, "Reveal score", tint = Pc.Text) }
            } else {
                Icon(Icons.Filled.ChevronRight, null, tint = Pc.Text3, modifier = Modifier.size(20.dp))
            }
        }
    }
}

@Composable
private fun TeamScoreRow(team: Team, score: Int?, hidden: Boolean, leading: Boolean) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Crest(team, 24.dp)
        Text(
            team.name, style = PcType.Body.copy(fontSize = 15.sp), fontWeight = if (leading) FontWeight.ExtraBold else FontWeight.SemiBold,
            maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f),
        )
        when {
            hidden -> Box(Modifier.size(width = 28.dp, height = 20.dp).clip(RoundedCornerShape(6.dp)).background(Pc.Outline))
            score != null -> ScoreText(score.toString(), bold = leading)
            else -> Text("–", color = Pc.Text3, fontSize = 22.sp, modifier = Modifier.widthIn(min = 20.dp), textAlign = TextAlign.End)
        }
    }
}

// ---- Controls ----

@Composable
fun PrimaryButton(text: String, onClick: () -> Unit, modifier: Modifier = Modifier, enabled: Boolean = true, loading: Boolean = false) {
    val bg = if (enabled || loading) Pc.Accent else Pc.SurfaceVariant
    val fg = if (enabled || loading) Pc.OnAccent else Pc.Text3
    Box(
        modifier.heightIn(min = 52.dp).clip(PcShape.Control).background(bg).let { if (enabled && !loading) it.pcClickable(PcShape.Control, onClick = onClick) else it.semantics { disabled() } }
            .padding(horizontal = 24.dp),
        contentAlignment = Alignment.Center,
    ) {
        if (loading) androidx.compose.material3.CircularProgressIndicator(color = fg, strokeWidth = 2.5.dp, modifier = Modifier.size(22.dp).semantics { contentDescription = "Working" })
        else Text(text, color = fg, fontWeight = FontWeight.ExtraBold, fontSize = 16.sp)
    }
}

@Composable
fun SecondaryButton(text: String, onClick: () -> Unit, modifier: Modifier = Modifier, danger: Boolean = false) {
    Box(
        modifier.heightIn(min = 52.dp).pcClickable(PcShape.Control, onClick = onClick).background(Pc.Surface).border(1.dp, if (danger) Pc.Error else Pc.Outline, PcShape.Control).padding(horizontal = 24.dp),
        contentAlignment = Alignment.Center,
    ) { Text(text, color = if (danger) Pc.Error else Pc.Text, fontWeight = FontWeight.Bold, fontSize = 16.sp) }
}

@Composable
fun TextAction(text: String, onClick: () -> Unit, modifier: Modifier = Modifier, color: Color = Pc.Accent) {
    Box(modifier.heightIn(min = 48.dp).pcClickable(PcShape.Control, onClick = onClick).padding(horizontal = 8.dp), contentAlignment = Alignment.Center) {
        Text(text, color = color, fontWeight = FontWeight.Bold, fontSize = 14.sp)
    }
}

@Composable
fun IconAction(icon: ImageVector, description: String, onClick: () -> Unit, tint: Color = Pc.Text, modifier: Modifier = Modifier, filled: Boolean = false) {
    Box(
        modifier.size(48.dp).pcClickable(PcShape.Control, description, onClick = onClick).background(if (filled) Pc.Surface else Color.Transparent).let { if (filled) it.border(1.dp, Pc.Outline, PcShape.Control) else it },
        contentAlignment = Alignment.Center,
    ) { Icon(icon, description, tint = tint, modifier = Modifier.size(22.dp)) }
}

@Composable
fun BackButton(onBack: () -> Unit) = IconAction(Icons.AutoMirrored.Filled.ArrowBack, stringResource(R.string.action_back), onBack)

@Composable
fun Chip(label: String, selected: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Box(
        modifier.heightIn(min = 40.dp).pcClickable(PcShape.Control, role = Role.Button, onClick = onClick)
            .background(if (selected) Pc.Accent else Pc.Surface).border(1.dp, if (selected) Pc.Accent else Pc.Outline, PcShape.Control)
            .semantics { this.selected = selected }.padding(horizontal = 16.dp),
        contentAlignment = Alignment.Center,
    ) { Text(label, color = if (selected) Pc.OnAccent else Pc.Text, fontWeight = FontWeight.Bold, fontSize = 14.sp, maxLines = 1) }
}

@Composable
fun ChipRow(options: List<String>, selected: String, onSelect: (String) -> Unit, modifier: Modifier = Modifier, contentPadding: Dp = 16.dp) {
    Row(modifier.horizontalScroll(rememberScrollState()).padding(horizontal = contentPadding), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        options.forEach { Chip(it, it == selected, { onSelect(it) }) }
    }
}

/** Chips that wrap onto extra lines instead of scrolling, for use inside cards. */
@OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class)
@Composable
fun FlowChips(options: List<String>, selected: String, onSelect: (String) -> Unit, modifier: Modifier = Modifier) {
    androidx.compose.foundation.layout.FlowRow(modifier, horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        options.forEach { label -> Chip(label, label == selected, { onSelect(label) }) }
    }
}

@Composable
fun SegmentedTabs(options: List<String>, selected: Int, onSelect: (Int) -> Unit, modifier: Modifier = Modifier) {
    Row(modifier.fillMaxWidth().clip(PcShape.Control).background(Pc.Surface).border(1.dp, Pc.Outline, PcShape.Control).padding(4.dp), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
        options.forEachIndexed { i, label ->
            val on = i == selected
            Box(
                Modifier.weight(1f).heightIn(min = 40.dp).pcClickable(RoundedCornerShape(10.dp), role = Role.Tab) { onSelect(i) }
                    .background(if (on) Pc.SurfaceVariant else Color.Transparent).semantics { this.selected = on },
                contentAlignment = Alignment.Center,
            ) { Text(label, color = if (on) Pc.Text else Pc.Text2, fontWeight = FontWeight.Bold, fontSize = 14.sp, maxLines = 1) }
        }
    }
}

/** Underline tabs used on detail pages. */
@Composable
fun UnderlineTabs(options: List<String>, selected: Int, onSelect: (Int) -> Unit, modifier: Modifier = Modifier) {
    Row(modifier.horizontalScroll(rememberScrollState()).padding(horizontal = 8.dp)) {
        options.forEachIndexed { i, label ->
            val on = i == selected
            Column(
                Modifier.heightIn(min = 48.dp).pcClickable(RoundedCornerShape(8.dp), role = Role.Tab) { onSelect(i) }.padding(horizontal = 12.dp).semantics { this.selected = on },
                horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center,
            ) {
                Text(label, color = if (on) Pc.Accent else Pc.Text2, fontWeight = FontWeight.ExtraBold, fontSize = 14.sp)
                Spacer(Modifier.height(8.dp))
                Box(Modifier.height(3.dp).fillMaxWidth().background(if (on) Pc.Accent else Color.Transparent))
            }
        }
    }
}

@Composable
fun PcSwitch(checked: Boolean, onChange: (Boolean) -> Unit) {
    Switch(
        checked = checked, onCheckedChange = onChange,
        colors = SwitchDefaults.colors(
            checkedThumbColor = Pc.OnAccent, checkedTrackColor = Pc.Accent, checkedBorderColor = Pc.Accent,
            uncheckedThumbColor = Pc.Text2, uncheckedTrackColor = Pc.SurfaceVariant, uncheckedBorderColor = Pc.OutlineStrong,
        ),
    )
}

// ---- Layout pieces ----

@Suppress("ModifierParameter") // Positional (title, action, onAction) reads best at call sites.
@Composable
fun SectionHeader(title: String, action: String? = null, onAction: () -> Unit = {}, modifier: Modifier = Modifier) {
    Row(modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Text(title, style = PcType.Subtitle, modifier = Modifier.weight(1f))
        if (action != null) TextAction(action, onAction)
    }
}

@Composable
fun Overline(text: String, modifier: Modifier = Modifier, color: Color = Pc.Accent) = Text(text, style = PcType.Overline, color = color, modifier = modifier)

@Composable
fun PhotoPlaceholder(modifier: Modifier = Modifier, label: String = "Photo") {
    Box(modifier.clip(PcShape.Control).background(Pc.SurfaceVariant), contentAlignment = Alignment.BottomStart) {
        Text(label, style = PcType.Caption, modifier = Modifier.padding(8.dp))
    }
}

@Composable
fun BriefCard(text: String, modifier: Modifier = Modifier, title: String = "MATCH BRIEF · LIVE", updated: String = "2 min ago", compactMeta: Boolean = false) {
    PcCard(modifier, border = Pc.Accent.copy(alpha = 0.45f)) {
        Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Icon(Icons.Outlined.AutoAwesome, null, tint = Pc.Accent, modifier = Modifier.size(16.dp))
                Overline(title, Modifier.weight(1f))
                Text(updated, style = PcType.Caption)
            }
            Text(text, style = PcType.Body)
            if (!compactMeta) Text("AI-generated from match data", style = PcType.Caption)
        }
    }
}

@Composable
fun StatBar(stat: StatRow, modifier: Modifier = Modifier) {
    val total = (stat.home + stat.away).coerceAtLeast(1)
    val hw = stat.home.toFloat() / total
    Column(modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Row {
            Text(stat.homeText, style = PcType.Label.copy(fontSize = 15.sp), modifier = Modifier.weight(1f))
            Text(stat.label, style = PcType.BodyMuted)
            Text(stat.awayText, style = PcType.Label.copy(fontSize = 15.sp), textAlign = TextAlign.End, modifier = Modifier.weight(1f))
        }
        Row(Modifier.fillMaxWidth().height(8.dp), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            Box(Modifier.weight(hw.coerceIn(0.02f, 0.98f)).fillMaxHeight().clip(RoundedCornerShape(4.dp)).background(Pc.Accent))
            Box(Modifier.weight((1 - hw).coerceIn(0.02f, 0.98f)).fillMaxHeight().clip(RoundedCornerShape(4.dp)).background(Pc.Neutral))
        }
    }
}

@Composable
fun FormChip(letter: Char) {
    val (bg, fg) = when (letter) {
        'W' -> Pc.AccentContainer to Pc.Accent
        'L' -> Pc.ErrorContainer to Pc.Error
        else -> Pc.SurfaceVariant to Pc.Text2
    }
    Box(Modifier.size(26.dp).clip(RoundedCornerShape(8.dp)).background(bg), contentAlignment = Alignment.Center) {
        Text(letter.toString(), color = fg, fontWeight = FontWeight.ExtraBold, fontSize = 12.sp)
    }
}

// ---- States ----

enum class BannerKind { Info, Success, Warning, Error }

@Composable
fun InfoBanner(text: String, kind: BannerKind, modifier: Modifier = Modifier, action: String? = null, onAction: () -> Unit = {}) {
    val (bg, fg, icon) = when (kind) {
        BannerKind.Success -> Triple(Pc.AccentContainer, Pc.Accent, Icons.Outlined.Check)
        BannerKind.Warning -> Triple(Pc.WarningContainer, Pc.Warning, Icons.Outlined.WifiOff)
        BannerKind.Error -> Triple(Pc.ErrorContainer, Pc.Error, Icons.Outlined.ErrorOutline)
        BannerKind.Info -> Triple(Pc.LiveContainer, Pc.LiveText, Icons.Outlined.ErrorOutline)
    }
    Row(
        modifier.fillMaxWidth().clip(PcShape.Control).background(bg).border(1.dp, fg.copy(alpha = 0.4f), PcShape.Control).padding(horizontal = 16.dp, vertical = 12.dp).semantics(mergeDescendants = true) {},
        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Icon(icon, null, tint = fg, modifier = Modifier.size(18.dp))
        Text(text, color = fg, fontWeight = FontWeight.SemiBold, fontSize = 14.sp, modifier = Modifier.weight(1f))
        if (action != null) Text(action, color = fg, fontWeight = FontWeight.ExtraBold, fontSize = 14.sp, modifier = Modifier.pcClickable(PcShape.Chip, onClick = onAction).padding(horizontal = 8.dp, vertical = 12.dp))
    }
}

fun Modifier.skeleton(): Modifier = composed2 {
    val t = rememberInfiniteTransition(label = "skeleton")
    val a by t.animateFloat(0.45f, 1f, infiniteRepeatable(tween(900), RepeatMode.Reverse), label = "alpha")
    this.alpha(a).background(Pc.SurfaceVariant, RoundedCornerShape(8.dp))
}

@Composable
fun MatchCardSkeleton(modifier: Modifier = Modifier) {
    PcCard(modifier.fillMaxWidth()) {
        Row(Modifier.fillMaxWidth().padding(16.dp), horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(width = 64.dp, height = 24.dp).skeleton())
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Box(Modifier.fillMaxWidth(0.7f).height(18.dp).skeleton())
                Box(Modifier.fillMaxWidth(0.5f).height(18.dp).skeleton())
            }
        }
    }
}

// ---- Navigation ----

data class NavItem(val route: String, @androidx.annotation.StringRes val label: Int, val icon: ImageVector, val selectedIcon: ImageVector)

val navItems = listOf(
    NavItem("home", R.string.nav_home, Icons.Outlined.Home, Icons.Filled.Home),
    NavItem("matches", R.string.nav_matches, Icons.Outlined.SportsSoccer, Icons.Filled.SportsSoccer),
    NavItem("leagues", R.string.nav_leagues, Icons.Outlined.EmojiEvents, Icons.Filled.EmojiEvents),
    NavItem("news", R.string.nav_news, Icons.Outlined.Newspaper, Icons.Filled.Newspaper),
    NavItem("following", R.string.nav_favorites, Icons.Outlined.FavoriteBorder, Icons.Filled.Favorite),
)

@Composable
fun BottomNav(current: String?, onNavigate: (String) -> Unit, badges: Map<String, Int> = emptyMap()) {
    Column(Modifier.background(Pc.Nav).navigationBarsPadding()) {
        Box(Modifier.fillMaxWidth().height(1.dp).background(Pc.Outline))
        Row(Modifier.fillMaxWidth().background(Pc.Nav).padding(horizontal = 8.dp, vertical = 6.dp)) {
            navItems.forEach { item ->
                val on = item.route == current
                Column(
                    Modifier.weight(1f).heightIn(min = 56.dp).pcClickable(PcShape.Control, role = Role.Tab) { onNavigate(item.route) }.semantics { selected = on },
                    horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center,
                ) {
                    Box {
                        Icon(if (on) item.selectedIcon else item.icon, null, tint = if (on) Pc.Accent else Pc.Text2, modifier = Modifier.size(22.dp))
                        val n = badges[item.route] ?: 0
                        if (n > 0) LiveCountBadge(n, Modifier.align(Alignment.TopEnd).offset(x = 12.dp, y = (-6).dp))
                    }
                    Text(stringResource(item.label), color = if (on) Pc.Accent else Pc.Text2, fontSize = 12.sp, fontWeight = if (on) FontWeight.ExtraBold else FontWeight.SemiBold)
                }
            }
        }
    }
}

@Composable
fun SideRail(current: String?, onNavigate: (String) -> Unit, onAsk: () -> Unit, wide: Boolean, badges: Map<String, Int> = emptyMap()) {
    Row(Modifier.fillMaxHeight()) {
        Column(
            Modifier.fillMaxHeight().width(if (wide) 248.dp else 88.dp).background(Pc.Nav).padding(horizontal = if (wide) 16.dp else 8.dp, vertical = 24.dp),
            horizontalAlignment = if (wide) Alignment.Start else Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Row(Modifier.padding(bottom = 24.dp, start = if (wide) 8.dp else 0.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Box(Modifier.size(36.dp).clip(RoundedCornerShape(10.dp)).background(Pc.Accent), contentAlignment = Alignment.Center) { Text("P", color = Pc.OnAccent, fontWeight = FontWeight.ExtraBold, fontSize = 18.sp) }
                if (wide) Text("Pitchside", style = PcType.Subtitle)
            }
            navItems.forEach { item ->
                val on = item.route == current
                Row(
                    Modifier.then(if (wide) Modifier.fillMaxWidth() else Modifier.size(72.dp, 64.dp)).heightIn(min = 48.dp).pcClickable(PcShape.Control, role = Role.Tab) { onNavigate(item.route) }
                        .background(if (on) Pc.AccentContainer else Color.Transparent).padding(horizontal = 12.dp).semantics { selected = on },
                    verticalAlignment = Alignment.CenterVertically, horizontalArrangement = if (wide) Arrangement.spacedBy(12.dp) else Arrangement.Center,
                ) {
                    Box {
                        Icon(if (on) item.selectedIcon else item.icon, null, tint = if (on) Pc.Accent else Pc.Text2, modifier = Modifier.size(22.dp))
                        val n = badges[item.route] ?: 0
                        if (n > 0) LiveCountBadge(n, Modifier.align(Alignment.TopEnd).offset(x = 12.dp, y = (-6).dp))
                    }
                    if (!wide) Spacer(Modifier.width(0.dp))
                    if (wide) Text(stringResource(item.label), color = if (on) Pc.Accent else Pc.Text2, fontWeight = if (on) FontWeight.ExtraBold else FontWeight.SemiBold, fontSize = 15.sp)
                }
            }
            Spacer(Modifier.weight(1f))
            Row(
                Modifier.then(if (wide) Modifier.fillMaxWidth() else Modifier.size(48.dp)).heightIn(min = 48.dp).pcClickable(PcShape.Control, onClick = onAsk).background(Pc.Surface).border(1.dp, Pc.Outline, PcShape.Control),
                verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.Center,
            ) {
                Icon(Icons.Outlined.AutoAwesome, "Ask Pitchside", tint = Pc.Accent, modifier = Modifier.size(20.dp))
                if (wide) { Spacer(Modifier.width(8.dp)); Text("Ask Pitchside", color = Pc.Accent, fontWeight = FontWeight.ExtraBold, fontSize = 14.sp) }
            }
        }
        Box(Modifier.fillMaxHeight().width(1.dp).background(Pc.Outline))
    }
}


/** Small live-count bubble on the Matches tab, like a notification count. */
@Composable
fun LiveCountBadge(count: Int, modifier: Modifier = Modifier) {
    Box(
        modifier.heightIn(min = 16.dp).widthIn(min = 16.dp).clip(CircleShape).background(Pc.LiveText).padding(horizontal = 4.dp)
            .semantics { contentDescription = "$count live" },
        contentAlignment = Alignment.Center,
    ) { Text(if (count > 9) "9+" else count.toString(), color = Pc.Bg, fontSize = 10.sp, fontWeight = FontWeight.ExtraBold) }
}
