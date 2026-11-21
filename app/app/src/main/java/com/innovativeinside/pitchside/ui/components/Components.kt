package com.innovativeinside.pitchside.ui.components

import android.provider.Settings
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.ErrorOutline
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.WifiOff
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.disabled
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.innovativeinside.pitchside.R
import com.innovativeinside.pitchside.data.model.StatRow
import com.innovativeinside.pitchside.ui.preview.PreviewHost
import com.innovativeinside.pitchside.ui.theme.Pc
import com.innovativeinside.pitchside.ui.theme.PcShape
import com.innovativeinside.pitchside.ui.theme.PcSpace
import com.innovativeinside.pitchside.ui.theme.PcType

/*
 * Pitchside 0.6 design system, part 1: motion, interaction, surfaces, buttons, chips and tabs, headers,
 * stats and loading. Dark charcoal surfaces with 1dp white-alpha borders, emerald accent, condensed
 * uppercase headings. Match, news, navigation and state components live in the sibling files.
 */

// ---------------- Motion ----------------

/** Tests and previews can force reduced motion; null means "read the system setting". */
val LocalReducedMotion = staticCompositionLocalOf<Boolean?> { null }

/**
 * True when the user turned animations off (Settings > Accessibility > Remove animations, which sets the
 * animator duration scale to 0). Pulses, shimmer, press scale and score animations are skipped then.
 */
@Composable
fun rememberReducedMotion(): Boolean {
    LocalReducedMotion.current?.let { return it }
    if (LocalInspectionMode.current) return true
    val ctx = LocalContext.current
    return remember(ctx) { Settings.Global.getFloat(ctx.contentResolver, Settings.Global.ANIMATOR_DURATION_SCALE, 1f) == 0f }
}

// ---------------- Interaction ----------------

/** Visible keyboard focus ring. Place before clickable. */
fun Modifier.focusRing(shape: Shape = PcShape.Control): Modifier = composed2 {
    var focused by remember { mutableStateOf(false) }
    this.onFocusChanged { focused = it.isFocused }
        .then(if (focused) Modifier.border(2.dp, Pc.Accent, shape) else Modifier)
}

@Suppress("UnnecessaryComposedModifier")
private fun Modifier.composed2(factory: @Composable Modifier.() -> Modifier): Modifier = this.composed { factory() }

/**
 * The app's click modifier: focus ring, ripple clipped to [shape], and (when [pressScale]) a subtle 0.97
 * press scale for cards. The scale is skipped when motion is reduced.
 */
fun Modifier.pcClickable(shape: Shape = PcShape.Control, label: String? = null, role: Role = Role.Button, pressScale: Boolean = false, onClick: () -> Unit): Modifier = composed2 {
    val source = remember { MutableInteractionSource() }
    val reduced = rememberReducedMotion()
    val pressed by source.collectIsPressedAsState()
    val scale by animateFloatAsState(if (pressScale && pressed && !reduced) 0.97f else 1f, tween(120), label = "press")
    this.graphicsLayer { scaleX = scale; scaleY = scale }
        .focusRing(shape).clip(shape)
        .clickable(interactionSource = source, indication = LocalIndication.current, role = role, onClickLabel = label, onClick = onClick)
}

// ---------------- Surfaces ----------------

/**
 * PremiumCard: the one neutral surface. Charcoal fill, 1dp white-alpha border, 20dp radius, no shadow.
 * Clickable cards get the press scale. Hero areas use [GlowCard] instead.
 */
@Composable
fun PcCard(
    modifier: Modifier = Modifier,
    shape: Shape = PcShape.Tile,
    container: Color = Pc.Surface,
    border: Color = Pc.Outline,
    onClick: (() -> Unit)? = null,
    content: @Composable () -> Unit,
) {
    val base = if (onClick != null) modifier.pcClickable(shape, pressScale = true, onClick = onClick) else modifier.clip(shape)
    Box(base.background(container, shape).border(1.dp, border, shape)) { content() }
}

/** The design-system name for [PcCard]. */
@Composable
fun PremiumCard(modifier: Modifier = Modifier, onClick: (() -> Unit)? = null, content: @Composable () -> Unit) =
    PcCard(modifier, onClick = onClick, content = content)

/** Hairline divider (white ~9% on dark). */
@Composable
fun Hairline(modifier: Modifier = Modifier, color: Color = Pc.Outline) = Box(modifier.fillMaxWidth().height(1.dp).background(color))

// ---------------- Buttons ----------------

/** Solid emerald pill, 52dp tall. */
@Composable
fun PrimaryButton(text: String, onClick: () -> Unit, modifier: Modifier = Modifier, enabled: Boolean = true, loading: Boolean = false, icon: ImageVector? = null) {
    val on = enabled || loading
    val bg = if (on) Pc.Accent else Pc.SurfaceVariant
    val fg = if (on) Pc.OnAccent else Pc.Text2
    Row(
        modifier.heightIn(min = 52.dp)
            .let { if (enabled && !loading) it.pcClickable(PcShape.Chip, pressScale = true, onClick = onClick) else it.clip(PcShape.Chip).semantics { disabled() } }
            .background(bg).padding(horizontal = PcSpace.lg),
        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.Center,
    ) {
        if (loading) CircularProgressIndicator(color = fg, strokeWidth = 2.dp, modifier = Modifier.size(20.dp).semantics { contentDescription = "Working" })
        else {
            if (icon != null) { Icon(icon, null, tint = fg, modifier = Modifier.size(20.dp)); Spacer(Modifier.width(PcSpace.xs)) }
            Text(text, style = PcType.BodyStrong.copy(fontWeight = FontWeight.ExtraBold), color = fg, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
    }
}

/** Outlined charcoal pill; [danger] turns it red. */
@Composable
fun SecondaryButton(text: String, onClick: () -> Unit, modifier: Modifier = Modifier, danger: Boolean = false, icon: ImageVector? = null) {
    val fg = if (danger) Pc.Error else Pc.Text
    Row(
        modifier.heightIn(min = 52.dp).pcClickable(PcShape.Chip, pressScale = true, onClick = onClick).background(Pc.Surface)
            .border(1.dp, if (danger) Pc.Error.copy(alpha = 0.6f) else Pc.OutlineStrong, PcShape.Chip).padding(horizontal = PcSpace.lg),
        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.Center,
    ) {
        if (icon != null) { Icon(icon, null, tint = if (danger) Pc.Error else Pc.Accent, modifier = Modifier.size(20.dp)); Spacer(Modifier.width(PcSpace.xs)) }
        Text(text, style = PcType.BodyStrong, color = fg, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}

@Composable
fun TextAction(text: String, onClick: () -> Unit, modifier: Modifier = Modifier, color: Color = Pc.Accent) {
    Box(modifier.heightIn(min = PcSpace.touch).pcClickable(PcShape.Control, onClick = onClick).padding(horizontal = PcSpace.xs), contentAlignment = Alignment.Center) {
        Text(text, style = PcType.LabelStrong, color = color, maxLines = 1)
    }
}

/** 48dp icon button. [filled] gives it a charcoal disc with a border; [dot] adds the emerald unread dot. */
@Composable
fun IconAction(icon: ImageVector, description: String, onClick: () -> Unit, tint: Color = Pc.Text, modifier: Modifier = Modifier, filled: Boolean = false, dot: Boolean = false) {
    Box(
        modifier.size(PcSpace.touch).pcClickable(CircleShape, description, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        if (filled) Box(Modifier.size(40.dp).clip(CircleShape).background(Pc.Surface).border(1.dp, Pc.Outline, CircleShape))
        Icon(icon, description, tint = tint, modifier = Modifier.size(24.dp))
        if (dot) Box(Modifier.align(Alignment.TopEnd).padding(top = 10.dp, end = 10.dp).size(9.dp).clip(CircleShape).background(Pc.Accent).border(1.5.dp, Pc.Bg, CircleShape))
    }
}

@Composable
fun BackButton(onBack: () -> Unit) = IconAction(Icons.AutoMirrored.Outlined.ArrowBack, stringResource(R.string.action_back), onBack, filled = true)

// ---------------- Chips, pills and tabs ----------------

/**
 * Filter chip / pill: 40dp visual pill inside a 48dp touch target. Selected = emerald tint, emerald border and
 * emerald label; otherwise charcoal with a hairline border.
 */
@Composable
fun Chip(label: String, selected: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier, leading: ImageVector? = null, count: Int? = null) {
    val fg = if (selected) Pc.Accent else Pc.Text
    Box(
        modifier.heightIn(min = PcSpace.touch).pcClickable(PcShape.Chip, role = Role.Tab, onClick = onClick).semantics { this.selected = selected },
        contentAlignment = Alignment.Center,
    ) {
        Row(
            Modifier.heightIn(min = 38.dp).clip(PcShape.Chip)
                .background(if (selected) Pc.AccentContainer else Pc.Surface)
                .border(1.dp, if (selected) Pc.Accent.copy(alpha = 0.7f) else Pc.Outline, PcShape.Chip)
                .padding(horizontal = 14.dp),
            verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            if (leading != null) Icon(leading, null, tint = fg, modifier = Modifier.size(16.dp))
            Text(label, style = if (selected) PcType.LabelStrong else PcType.Label, color = fg, maxLines = 1)
            if (count != null) Text(count.toString(), style = PcType.Numeric, color = if (selected) Pc.Accent else Pc.Text2)
        }
    }
}

/** Horizontally scrolling chips. Keeps the brief's name (FilterChipRow) and the old one (ChipRow). */
@Composable
fun FilterChipRow(options: List<String>, selected: String, onSelect: (String) -> Unit, modifier: Modifier = Modifier, contentPadding: Dp = PcSpace.screen) {
    Row(modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).padding(horizontal = contentPadding), horizontalArrangement = Arrangement.spacedBy(PcSpace.xs)) {
        options.forEach { Chip(it, it == selected, { onSelect(it) }) }
    }
}

@Composable
fun ChipRow(options: List<String>, selected: String, onSelect: (String) -> Unit, modifier: Modifier = Modifier, contentPadding: Dp = PcSpace.screen) =
    FilterChipRow(options, selected, onSelect, modifier, contentPadding)

/** Chips that wrap onto extra lines instead of scrolling, for use inside cards. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun FlowChips(options: List<String>, selected: String, onSelect: (String) -> Unit, modifier: Modifier = Modifier) {
    FlowRow(modifier, horizontalArrangement = Arrangement.spacedBy(PcSpace.xs)) {
        options.forEach { label -> Chip(label, label == selected, { onSelect(label) }) }
    }
}

/**
 * PillTabs: dark pill tabs for detail pages (match, team, league, player). Selected = solid emerald pill with
 * dark label; others charcoal with a hairline border. Labels are uppercase condensed. Scrolls when crowded.
 */
@Composable
fun PillTabs(options: List<String>, selected: Int, onSelect: (Int) -> Unit, modifier: Modifier = Modifier, contentPadding: Dp = PcSpace.screen) {
    Row(
        modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).padding(horizontal = contentPadding),
        horizontalArrangement = Arrangement.spacedBy(PcSpace.xs),
    ) {
        options.forEachIndexed { i, label ->
            val on = i == selected
            Box(
                Modifier.heightIn(min = PcSpace.touch).pcClickable(PcShape.Chip, role = Role.Tab) { onSelect(i) }.semantics { this.selected = on },
                contentAlignment = Alignment.Center,
            ) {
                Box(
                    Modifier.heightIn(min = 38.dp).clip(PcShape.Chip).background(if (on) Pc.Accent else Pc.Surface)
                        .border(1.dp, if (on) Pc.Accent else Pc.Outline, PcShape.Chip).padding(horizontal = 16.dp),
                    contentAlignment = Alignment.Center,
                ) { Text(label.uppercase(), style = PcType.Tag, color = if (on) Pc.OnAccent else Pc.Text2, maxLines = 1) }
            }
        }
    }
}

data class Segment(val label: String, val count: Int? = null, val live: Boolean = false)

/**
 * Segmented control: equal-width segments on a charcoal track; the selected one is an emerald-tinted thumb.
 * Counts sit next to labels in tabular figures; the Live count shows the pulsing-green dot when non-zero.
 */
@Composable
fun SegmentedControl(segments: List<Segment>, selected: Int, onSelect: (Int) -> Unit, modifier: Modifier = Modifier) {
    Row(
        modifier.fillMaxWidth().clip(PcShape.Chip).background(Pc.Surface).border(1.dp, Pc.Outline, PcShape.Chip).padding(3.dp),
        horizontalArrangement = Arrangement.spacedBy(3.dp),
    ) {
        segments.forEachIndexed { i, seg ->
            val on = i == selected
            Row(
                Modifier.weight(1f).heightIn(min = 42.dp).pcClickable(PcShape.Chip, role = Role.Tab) { onSelect(i) }
                    .background(if (on) Pc.AccentContainer else Color.Transparent, PcShape.Chip)
                    .let { if (on) it.border(1.dp, Pc.Accent.copy(alpha = 0.6f), PcShape.Chip) else it }
                    .semantics { this.selected = on }.padding(horizontal = 6.dp),
                verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.Center,
            ) {
                if (seg.live && (seg.count ?: 0) > 0) { Box(Modifier.size(6.dp).clip(CircleShape).background(Pc.Live)); Spacer(Modifier.width(5.dp)) }
                Text(seg.label, style = if (on) PcType.LabelStrong else PcType.Label, color = if (on) Pc.Accent else Pc.Text2, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f, fill = false))
                if (seg.count != null) {
                    Spacer(Modifier.width(5.dp))
                    Text(seg.count.toString(), style = PcType.Numeric, color = if (on) Pc.Accent else Pc.Text3, maxLines = 1)
                }
            }
        }
    }
}

/** Plain-label segmented control (kept for existing call sites). */
@Composable
fun SegmentedTabs(options: List<String>, selected: Int, onSelect: (Int) -> Unit, modifier: Modifier = Modifier) =
    SegmentedControl(options.map { Segment(it) }, selected, onSelect, modifier)

/** Detail-page tabs. In 0.6 these are [PillTabs]; the name stays for existing call sites. */
@Composable
fun UnderlineTabs(options: List<String>, selected: Int, onSelect: (Int) -> Unit, modifier: Modifier = Modifier) =
    PillTabs(options, selected, onSelect, modifier)

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

// ---------------- Headers ----------------

/**
 * Section header: condensed UPPERCASE title (a heading for TalkBack), an optional grey [count] suffix
 * ("3 matches"), and an optional emerald "SEE ALL" action.
 */
@Suppress("ModifierParameter")
@Composable
fun SectionHeader(title: String, action: String? = null, onAction: () -> Unit = {}, modifier: Modifier = Modifier, trailing: (@Composable () -> Unit)? = null, count: String? = null, maxLines: Int = 2, minHeight: Dp = PcSpace.touch) {
    Row(modifier.fillMaxWidth().heightIn(min = minHeight), verticalAlignment = Alignment.CenterVertically) {
        val text = buildAnnotatedString {
            append(title.uppercase())
            if (count != null) withStyle(SpanStyle(color = Pc.Text2, fontFamily = PcType.Body.fontFamily, fontWeight = FontWeight.Normal, fontSize = 15.sp, letterSpacing = 0.sp)) { append("  $count") }
        }
        Text(text, style = PcType.SectionTitle, modifier = Modifier.weight(1f).semantics { heading() }, maxLines = maxLines, overflow = TextOverflow.Ellipsis)
        trailing?.invoke()
        if (action != null) Box(
            Modifier.heightIn(min = PcSpace.touch).pcClickable(PcShape.Control, action, onClick = onAction).padding(start = PcSpace.xs, end = 2.dp),
            contentAlignment = Alignment.CenterEnd,
        ) { Text(action.uppercase(), style = PcType.Tag, color = Pc.Accent, maxLines = 1) }
    }
}

/** Uppercase kicker (condensed, tracked, grey). */
@Composable
fun Overline(text: String, modifier: Modifier = Modifier, color: Color = Pc.Text2) =
    Text(text.uppercase(), style = PcType.Overline, color = color, modifier = modifier, maxLines = 1, overflow = TextOverflow.Ellipsis)

// ---------------- Stats ----------------

/**
 * StatBar: values left and right in condensed bold, the label centred in uppercase grey, and paired bars
 * underneath: home emerald growing from the centre leftwards, away grey rightwards. The leader is bright.
 */
@Composable
fun StatBar(stat: StatRow, modifier: Modifier = Modifier) {
    val total = (stat.home + stat.away).coerceAtLeast(1)
    val hw = stat.home.toFloat() / total
    val homeLeads = stat.home > stat.away
    val awayLeads = stat.away > stat.home
    Column(modifier.fillMaxWidth().semantics(mergeDescendants = true) { contentDescription = "${stat.label}: home ${stat.homeText}, away ${stat.awayText}" }, verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(stat.homeText, style = PcType.ScoreRow.copy(fontSize = 18.sp), color = if (homeLeads) Pc.Accent else Pc.Text, modifier = Modifier.weight(1f))
            Text(stat.label.uppercase(), style = PcType.Overline, textAlign = TextAlign.Center)
            Text(stat.awayText, style = PcType.ScoreRow.copy(fontSize = 18.sp), color = Pc.Text, textAlign = TextAlign.End, modifier = Modifier.weight(1f))
        }
        Row(Modifier.fillMaxWidth().height(6.dp), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            Box(Modifier.weight(1f).fillMaxHeight().clip(CircleShape).background(Pc.SurfaceVariant)) {
                Box(Modifier.fillMaxWidth(hw.coerceIn(0.02f, 1f)).fillMaxHeight().align(Alignment.CenterEnd).clip(CircleShape).background(if (homeLeads || !awayLeads) Pc.Accent else Pc.Accent.copy(alpha = 0.55f)))
            }
            Box(Modifier.weight(1f).fillMaxHeight().clip(CircleShape).background(Pc.SurfaceVariant)) {
                Box(Modifier.fillMaxWidth((1 - hw).coerceIn(0.02f, 1f)).fillMaxHeight().clip(CircleShape).background(if (awayLeads) Pc.Text2 else Pc.Neutral))
            }
        }
    }
}

/** One form result as a rounded square with its letter, so colour is never the only signal. */
@Composable
fun FormChip(letter: Char, size: Dp = 18.dp) {
    val (bg, fg) = formColors(letter)
    Box(Modifier.size(size).clip(PcShape.Small).background(bg), contentAlignment = Alignment.Center) {
        Text(letter.toString(), style = PcType.Micro, color = fg)
    }
}

@Composable
private fun formColors(letter: Char): Pair<Color, Color> = when (letter) {
    'W' -> Pc.Success to Pc.OnAccent
    'L' -> Pc.Error to Pc.OnAccent
    else -> Pc.Neutral to Pc.Text
}

/**
 * FormPills: the last five results. [compact] draws slim pills (the mini standings card); otherwise lettered
 * squares. Always carries a spoken summary ("Form: won, won, drawn").
 */
@Composable
fun FormPills(form: String, modifier: Modifier = Modifier, compact: Boolean = false, size: Dp = 18.dp) {
    if (form.isBlank()) return
    val spoken = form.map { when (it) { 'W' -> "won"; 'L' -> "lost"; else -> "drawn" } }.joinToString(", ")
    Row(modifier.semantics(mergeDescendants = true) { contentDescription = "Form: $spoken" }, horizontalArrangement = Arrangement.spacedBy(if (compact) 3.dp else 3.dp), verticalAlignment = Alignment.CenterVertically) {
        form.takeLast(5).forEach { c ->
            if (compact) Box(Modifier.size(width = 6.dp, height = 14.dp).clip(CircleShape).background(formColors(c).first))
            else FormChip(c, size)
        }
    }
}

/** Older name for lettered form pills. */
@Composable
fun FormDots(form: String, modifier: Modifier = Modifier, size: Dp = 18.dp) = FormPills(form, modifier, compact = false, size = size)

// ---------------- Banners and loading ----------------

enum class BannerKind { Info, Success, Warning, Error }

@Composable
fun InfoBanner(text: String, kind: BannerKind, modifier: Modifier = Modifier, action: String? = null, onAction: () -> Unit = {}) {
    val (fg, icon) = when (kind) {
        BannerKind.Success -> Pc.Accent to Icons.Outlined.Check
        BannerKind.Warning -> Pc.Warning to Icons.Outlined.WifiOff
        BannerKind.Error -> Pc.Error to Icons.Outlined.ErrorOutline
        BannerKind.Info -> Pc.Text2 to Icons.Outlined.Info
    }
    Row(
        modifier.fillMaxWidth().clip(PcShape.Control).background(Pc.SurfaceVariant).border(1.dp, fg.copy(alpha = 0.35f), PcShape.Control).padding(horizontal = PcSpace.md, vertical = 10.dp)
            .semantics(mergeDescendants = true) { liveRegion = LiveRegionMode.Polite },
        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Icon(icon, null, tint = fg, modifier = Modifier.size(18.dp))
        Text(text, style = PcType.Label, color = Pc.Text, modifier = Modifier.weight(1f))
        if (action != null) Text(action, style = PcType.LabelStrong, color = Pc.Accent, modifier = Modifier.pcClickable(PcShape.Control, onClick = onAction).padding(horizontal = PcSpace.xs, vertical = 14.dp))
    }
}

/** Shown under the top of every tab while the device has no connection (driven by ConnectivityManager). */
@Composable
fun OfflineBanner(modifier: Modifier = Modifier) =
    InfoBanner(stringResource(R.string.offline_banner), BannerKind.Warning, modifier)

/** Skeleton block with a soft dark shimmer sweeping left to right (static when motion is reduced). */
fun Modifier.skeleton(shape: Shape = PcShape.Small): Modifier = composed2 {
    val reduced = rememberReducedMotion()
    val base = Pc.SurfaceVariant
    val glint = if (Pc.IsDark) Color.White.copy(alpha = 0.07f) else Color.White.copy(alpha = 0.7f)
    if (reduced) {
        this.clip(shape).background(base)
    } else {
        val t = rememberInfiniteTransition(label = "shimmer")
        val x by t.animateFloat(-1f, 2f, infiniteRepeatable(tween(1400, easing = LinearEasing), RepeatMode.Restart), label = "x")
        this.clip(shape).background(base).drawWithContent {
            drawContent()
            val w = size.width
            drawRect(Brush.linearGradient(listOf(Color.Transparent, glint, Color.Transparent), start = Offset(w * x - w * 0.5f, 0f), end = Offset(w * x, size.height)))
        }
    }
}

/** Row-shaped skeleton matching [MatchRow]. */
@Composable
fun MatchCardSkeleton(modifier: Modifier = Modifier) {
    Row(modifier.fillMaxWidth().heightIn(min = 72.dp).padding(horizontal = PcSpace.md, vertical = PcSpace.sm), horizontalArrangement = Arrangement.spacedBy(PcSpace.sm), verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.size(width = 40.dp, height = 16.dp).skeleton())
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(horizontalArrangement = Arrangement.spacedBy(PcSpace.xs), verticalAlignment = Alignment.CenterVertically) { Box(Modifier.size(22.dp).skeleton(CircleShape)); Box(Modifier.fillMaxWidth(0.6f).height(14.dp).skeleton()) }
            Row(horizontalArrangement = Arrangement.spacedBy(PcSpace.xs), verticalAlignment = Alignment.CenterVertically) { Box(Modifier.size(22.dp).skeleton(CircleShape)); Box(Modifier.fillMaxWidth(0.45f).height(14.dp).skeleton()) }
        }
        Box(Modifier.size(width = 16.dp, height = 36.dp).skeleton())
    }
}

/** Content padding for lists that sit above the bottom bar. */
val ListPadding = PaddingValues(start = PcSpace.screen, end = PcSpace.screen, top = PcSpace.xs, bottom = PcSpace.lg)

// ---------------- Previews ----------------

@Preview(name = "Controls · dark", showBackground = true, backgroundColor = 0xFF0E1110)
@Composable
private fun ControlsDarkPreview() = PreviewHost(dark = true) {
    Column(Modifier.padding(PcSpace.md), verticalArrangement = Arrangement.spacedBy(PcSpace.sm)) {
        SectionHeader("Top matches", "See all", count = "3 matches")
        PillTabs(listOf("Overview", "Stats", "Lineups"), 1, {}, contentPadding = 0.dp)
        FilterChipRow(listOf("All", "Premier Division", "Continental Cup"), "All", {}, contentPadding = 0.dp)
        SegmentedControl(listOf(Segment("Live", 3, live = true), Segment("Upcoming", 5), Segment("Finished", 2)), 0, {})
        StatBar(StatRow("Possession", 58, 42, "58%", "42%"))
        FormPills("WWDLW"); FormPills("WWDLW", compact = true)
        InfoBanner("Offline. Showing saved scores.", BannerKind.Warning)
        PrimaryButton("Follow", {})
        MatchCardSkeleton()
    }
}

@Preview(name = "Controls · light", showBackground = true)
@Composable
private fun ControlsLightPreview() = PreviewHost(dark = false) {
    Column(Modifier.padding(PcSpace.md), verticalArrangement = Arrangement.spacedBy(PcSpace.sm)) {
        SectionHeader("Matches", "See all")
        SegmentedControl(listOf(Segment("Live", 1, live = true), Segment("Upcoming", 5), Segment("Finished", 0)), 1, {})
    }
}
