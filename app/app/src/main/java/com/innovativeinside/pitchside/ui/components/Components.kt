package com.innovativeinside.pitchside.ui.components

import android.provider.Settings
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.KeyboardArrowRight
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.innovativeinside.pitchside.R
import com.innovativeinside.pitchside.data.model.StatRow
import com.innovativeinside.pitchside.ui.preview.PreviewHost
import com.innovativeinside.pitchside.ui.theme.Pc
import com.innovativeinside.pitchside.ui.theme.PcShape
import com.innovativeinside.pitchside.ui.theme.PcSpace
import com.innovativeinside.pitchside.ui.theme.PcType

// ---------------- Motion ----------------

/** Tests and previews can force reduced motion; null means "read the system setting". */
val LocalReducedMotion = staticCompositionLocalOf<Boolean?> { null }

/**
 * True when the user turned animations off (Settings > Accessibility > Remove animations, which sets the
 * animator duration scale to 0). Pulses, shimmer and the hero swap are skipped then.
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

fun Modifier.pcClickable(shape: Shape = PcShape.Control, label: String? = null, role: Role = Role.Button, onClick: () -> Unit): Modifier =
    this.focusRing(shape).clip(shape).clickable(role = role, onClickLabel = label, onClick = onClick)

// ---------------- Surfaces ----------------

/**
 * The one neutral tile: surface colour, 1dp outline, 16dp radius, no shadow. Every card and bento tile
 * uses it; only the live hero uses the deep-green stage instead.
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
    val base = modifier.clip(shape).background(container, shape).border(1.dp, border, shape)
    Box(if (onClick != null) base.pcClickable(shape, onClick = onClick) else base) { content() }
}

/** Hairline divider. */
@Composable
fun Hairline(modifier: Modifier = Modifier, color: Color = Pc.Outline) = Box(modifier.fillMaxWidth().height(1.dp).background(color))

// ---------------- Buttons ----------------

@Composable
fun PrimaryButton(text: String, onClick: () -> Unit, modifier: Modifier = Modifier, enabled: Boolean = true, loading: Boolean = false, icon: ImageVector? = null) {
    val on = enabled || loading
    val bg = if (on) Pc.Accent else Pc.SurfaceVariant
    val fg = if (on) Pc.OnAccent else Pc.Text2
    Row(
        modifier.heightIn(min = PcSpace.touch).clip(PcShape.Control).background(bg)
            .let { if (enabled && !loading) it.pcClickable(PcShape.Control, onClick = onClick) else it.semantics { disabled() } }
            .padding(horizontal = PcSpace.lg),
        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.Center,
    ) {
        if (loading) CircularProgressIndicator(color = fg, strokeWidth = 2.dp, modifier = Modifier.size(20.dp).semantics { contentDescription = "Working" })
        else {
            if (icon != null) { Icon(icon, null, tint = fg, modifier = Modifier.size(20.dp)); Spacer(Modifier.width(PcSpace.xs)) }
            Text(text, style = PcType.LabelStrong, color = fg, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
    }
}

@Composable
fun SecondaryButton(text: String, onClick: () -> Unit, modifier: Modifier = Modifier, danger: Boolean = false, icon: ImageVector? = null) {
    val fg = if (danger) Pc.Error else Pc.Text
    Row(
        modifier.heightIn(min = PcSpace.touch).pcClickable(PcShape.Control, onClick = onClick).background(Pc.Surface)
            .border(1.dp, if (danger) Pc.Error else Pc.OutlineStrong, PcShape.Control).padding(horizontal = PcSpace.lg),
        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.Center,
    ) {
        if (icon != null) { Icon(icon, null, tint = fg, modifier = Modifier.size(20.dp)); Spacer(Modifier.width(PcSpace.xs)) }
        Text(text, style = PcType.LabelStrong, color = fg, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}

@Composable
fun TextAction(text: String, onClick: () -> Unit, modifier: Modifier = Modifier, color: Color = Pc.Accent) {
    Box(modifier.heightIn(min = PcSpace.touch).pcClickable(PcShape.Control, onClick = onClick).padding(horizontal = PcSpace.xs), contentAlignment = Alignment.Center) {
        Text(text, style = PcType.LabelStrong, color = color, maxLines = 1)
    }
}

/** 48dp icon button. [filled] gives it the neutral tile look (used in top bars). */
@Composable
fun IconAction(icon: ImageVector, description: String, onClick: () -> Unit, tint: Color = Pc.Text, modifier: Modifier = Modifier, filled: Boolean = false, dot: Boolean = false) {
    Box(
        modifier.size(PcSpace.touch).pcClickable(PcShape.Control, description, onClick = onClick)
            .background(if (filled) Pc.Surface else Color.Transparent)
            .let { if (filled) it.border(1.dp, Pc.Outline, PcShape.Control) else it },
        contentAlignment = Alignment.Center,
    ) {
        Icon(icon, description, tint = tint, modifier = Modifier.size(22.dp))
        if (dot) Box(Modifier.align(Alignment.TopEnd).padding(top = 11.dp, end = 11.dp).size(8.dp).clip(CircleShape).background(Pc.Live).border(1.5.dp, Pc.Surface, CircleShape))
    }
}

@Composable
fun BackButton(onBack: () -> Unit) = IconAction(Icons.AutoMirrored.Outlined.ArrowBack, stringResource(R.string.action_back), onBack)

// ---------------- Chips and segmented controls ----------------

/** Filter chip: pill, selected = accentContainer + accent text + accent outline. */
@Composable
fun Chip(label: String, selected: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier, leading: ImageVector? = null, count: Int? = null) {
    val fg = if (selected) Pc.Accent else Pc.Text
    Row(
        modifier.heightIn(min = 40.dp).pcClickable(PcShape.Chip, role = Role.Tab, onClick = onClick)
            .background(if (selected) Pc.AccentContainer else Pc.Surface)
            .border(1.dp, if (selected) Pc.Accent else Pc.Outline, PcShape.Chip)
            .semantics { this.selected = selected }.padding(horizontal = 14.dp),
        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        if (selected && leading == null) Icon(Icons.Outlined.Check, null, tint = fg, modifier = Modifier.size(16.dp))
        if (leading != null) Icon(leading, null, tint = fg, modifier = Modifier.size(16.dp))
        Text(label, style = if (selected) PcType.LabelStrong else PcType.Label, color = fg, maxLines = 1)
        if (count != null) Text(count.toString(), style = PcType.Numeric, color = if (selected) Pc.Accent else Pc.Text2)
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

/** Chips that wrap onto extra lines instead of scrolling, for use inside tiles. */
@OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class)
@Composable
fun FlowChips(options: List<String>, selected: String, onSelect: (String) -> Unit, modifier: Modifier = Modifier) {
    androidx.compose.foundation.layout.FlowRow(modifier, horizontalArrangement = Arrangement.spacedBy(PcSpace.xs), verticalArrangement = Arrangement.spacedBy(PcSpace.xs)) {
        options.forEach { label -> Chip(label, label == selected, { onSelect(label) }) }
    }
}

data class Segment(val label: String, val count: Int? = null, val live: Boolean = false)

/**
 * Segmented control: equal-width segments on a surfaceVariant track; the selected one is a white
 * (surface) thumb. Counts sit next to labels in tabular figures; the Live count is red when non-zero.
 */
@Composable
fun SegmentedControl(segments: List<Segment>, selected: Int, onSelect: (Int) -> Unit, modifier: Modifier = Modifier) {
    Row(modifier.fillMaxWidth().clip(PcShape.Control).background(Pc.SurfaceVariant).padding(3.dp), horizontalArrangement = Arrangement.spacedBy(3.dp)) {
        segments.forEachIndexed { i, seg ->
            val on = i == selected
            Row(
                Modifier.weight(1f).heightIn(min = 42.dp).pcClickable(PcShape.Control, role = Role.Tab) { onSelect(i) }
                    .background(if (on) Pc.Surface else Color.Transparent, PcShape.Control)
                    .let { if (on) it.border(1.dp, Pc.Outline, PcShape.Control) else it }
                    .semantics { this.selected = on }.padding(horizontal = 6.dp),
                verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.Center,
            ) {
                if (seg.live && (seg.count ?: 0) > 0) { Box(Modifier.size(6.dp).clip(CircleShape).background(Pc.Live)); Spacer(Modifier.width(6.dp)) }
                Text(seg.label, style = if (on) PcType.LabelStrong else PcType.Label, color = if (on) Pc.Text else Pc.Text2, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f, fill = false))
                if (seg.count != null) {
                    Spacer(Modifier.width(5.dp))
                    Text(seg.count.toString(), style = PcType.Numeric, color = if (seg.live && seg.count > 0) Pc.LiveText else Pc.Text2, maxLines = 1)
                }
            }
        }
    }
}

/** Plain-label segmented control (kept for existing call sites). */
@Composable
fun SegmentedTabs(options: List<String>, selected: Int, onSelect: (Int) -> Unit, modifier: Modifier = Modifier) =
    SegmentedControl(options.map { Segment(it) }, selected, onSelect, modifier)

/** Underline tabs for detail pages: accent label and 3dp accent indicator on the selected tab. */
@Composable
fun UnderlineTabs(options: List<String>, selected: Int, onSelect: (Int) -> Unit, modifier: Modifier = Modifier) {
    Column(modifier.fillMaxWidth()) {
        Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).padding(horizontal = PcSpace.xs)) {
            options.forEachIndexed { i, label ->
                val on = i == selected
                Column(
                    Modifier.heightIn(min = PcSpace.touch).pcClickable(PcShape.Small, role = Role.Tab) { onSelect(i) }.padding(horizontal = PcSpace.sm).semantics { this.selected = on },
                    horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Bottom,
                ) {
                    Spacer(Modifier.height(14.dp))
                    Text(label, style = if (on) PcType.LabelStrong else PcType.Label, color = if (on) Pc.Accent else Pc.Text2, maxLines = 1)
                    Spacer(Modifier.height(11.dp))
                    Box(Modifier.height(3.dp).widthIn(min = 24.dp).fillMaxWidth().clip(PcShape.Small).background(if (on) Pc.Accent else Color.Transparent))
                }
            }
        }
        Hairline()
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

// ---------------- Headers ----------------

/** Section header: Subtitle title (a heading for TalkBack), optional count, optional "See all". */
@Suppress("ModifierParameter")
@Composable
fun SectionHeader(title: String, action: String? = null, onAction: () -> Unit = {}, modifier: Modifier = Modifier, trailing: (@Composable () -> Unit)? = null) {
    Row(modifier.fillMaxWidth().heightIn(min = PcSpace.touch), verticalAlignment = Alignment.CenterVertically) {
        Text(title, style = PcType.Subtitle, modifier = Modifier.weight(1f).semantics { heading() }, maxLines = 1, overflow = TextOverflow.Ellipsis)
        trailing?.invoke()
        if (action != null) Row(
            Modifier.heightIn(min = PcSpace.touch).pcClickable(PcShape.Control, action, onClick = onAction).padding(start = PcSpace.xs),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(action, style = PcType.LabelStrong, color = Pc.Accent)
            Icon(Icons.AutoMirrored.Outlined.KeyboardArrowRight, null, tint = Pc.Accent, modifier = Modifier.size(18.dp))
        }
    }
}

/** Uppercase kicker. */
@Composable
fun Overline(text: String, modifier: Modifier = Modifier, color: Color = Pc.Text2) =
    Text(text.uppercase(), style = PcType.Overline, color = color, modifier = modifier, maxLines = 1, overflow = TextOverflow.Ellipsis)

// ---------------- Stats ----------------

/** Paired horizontal bars: home (accent) grows from the centre leftwards, away (neutral) rightwards. */
@Composable
fun StatBar(stat: StatRow, modifier: Modifier = Modifier) {
    val total = (stat.home + stat.away).coerceAtLeast(1)
    val hw = stat.home.toFloat() / total
    val homeLeads = stat.home > stat.away
    val awayLeads = stat.away > stat.home
    Column(modifier.fillMaxWidth().semantics(mergeDescendants = true) { contentDescription = "${stat.label}: home ${stat.homeText}, away ${stat.awayText}" }, verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(stat.homeText, style = PcType.Numeric.copy(fontWeight = if (homeLeads) PcType.LabelStrong.fontWeight else PcType.Label.fontWeight), modifier = Modifier.weight(1f))
            Text(stat.label, style = PcType.Caption, textAlign = TextAlign.Center)
            Text(stat.awayText, style = PcType.Numeric.copy(fontWeight = if (awayLeads) PcType.LabelStrong.fontWeight else PcType.Label.fontWeight), textAlign = TextAlign.End, modifier = Modifier.weight(1f))
        }
        Row(Modifier.fillMaxWidth().height(6.dp), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            Box(Modifier.weight(1f).fillMaxHeight().clip(CircleShape).background(Pc.SurfaceVariant)) {
                Box(Modifier.fillMaxWidth(hw.coerceIn(0.02f, 1f)).fillMaxHeight().align(Alignment.CenterEnd).clip(CircleShape).background(if (homeLeads || !awayLeads) Pc.Accent else Pc.Accent.copy(alpha = 0.55f)))
            }
            Box(Modifier.weight(1f).fillMaxHeight().clip(CircleShape).background(Pc.SurfaceVariant)) {
                Box(Modifier.fillMaxWidth((1 - hw).coerceIn(0.02f, 1f)).fillMaxHeight().clip(CircleShape).background(Pc.Neutral))
            }
        }
    }
}

/** Form dot: coloured W/D/L with the letter inside, so colour is never the only signal. */
@Composable
fun FormChip(letter: Char, size: Dp = 18.dp) {
    val (bg, fg) = when (letter) {
        'W' -> Pc.Success to Pc.OnAccent
        'L' -> Pc.Error to Pc.OnAccent
        else -> Pc.Neutral to Pc.Text
    }
    Box(Modifier.size(size).clip(CircleShape).background(bg), contentAlignment = Alignment.Center) {
        Text(letter.toString(), style = PcType.Micro, color = if (letter == 'D') Pc.Text else Pc.Surface)
    }
}

/** A row of form dots with a spoken summary ("Form: won, won, drawn"). */
@Composable
fun FormDots(form: String, modifier: Modifier = Modifier, size: Dp = 18.dp) {
    if (form.isBlank()) return
    val spoken = form.map { when (it) { 'W' -> "won"; 'L' -> "lost"; else -> "drawn" } }.joinToString(", ")
    Row(modifier.semantics(mergeDescendants = true) { contentDescription = "Form: $spoken" }, horizontalArrangement = Arrangement.spacedBy(3.dp)) {
        form.takeLast(5).forEach { FormChip(it, size) }
    }
}

// ---------------- Banners and loading ----------------

enum class BannerKind { Info, Success, Warning, Error }

@Composable
fun InfoBanner(text: String, kind: BannerKind, modifier: Modifier = Modifier, action: String? = null, onAction: () -> Unit = {}) {
    val (bg, fg, icon) = when (kind) {
        BannerKind.Success -> Triple(Pc.AccentContainer, Pc.Accent, Icons.Outlined.Check)
        BannerKind.Warning -> Triple(Pc.WarningContainer, Pc.Text, Icons.Outlined.WifiOff)
        BannerKind.Error -> Triple(Pc.ErrorContainer, Pc.Error, Icons.Outlined.ErrorOutline)
        BannerKind.Info -> Triple(Pc.SurfaceVariant, Pc.Text, Icons.Outlined.Info)
    }
    Row(
        modifier.fillMaxWidth().clip(PcShape.Control).background(bg).border(1.dp, Pc.Outline, PcShape.Control).padding(horizontal = PcSpace.md, vertical = 10.dp)
            .semantics(mergeDescendants = true) { liveRegion = LiveRegionMode.Polite },
        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Icon(icon, null, tint = if (kind == BannerKind.Warning) Pc.Warning else fg, modifier = Modifier.size(18.dp))
        Text(text, style = PcType.Label, color = fg, modifier = Modifier.weight(1f))
        if (action != null) Text(action, style = PcType.LabelStrong, color = fg, modifier = Modifier.pcClickable(PcShape.Control, onClick = onAction).padding(horizontal = PcSpace.xs, vertical = 14.dp))
    }
}

/** Shown under the top of every tab while the device has no connection (driven by ConnectivityManager). */
@Composable
fun OfflineBanner(modifier: Modifier = Modifier) =
    InfoBanner(stringResource(R.string.offline_banner), BannerKind.Warning, modifier)

/** Skeleton block with a soft left-to-right shimmer (static when motion is reduced). */
fun Modifier.skeleton(shape: Shape = PcShape.Small): Modifier = composed2 {
    val reduced = rememberReducedMotion()
    val base = Pc.SurfaceVariant
    val glint = Pc.Surface
    if (reduced) {
        this.clip(shape).background(base)
    } else {
        val t = rememberInfiniteTransition(label = "shimmer")
        val x by t.animateFloat(-1f, 2f, infiniteRepeatable(tween(1300, easing = LinearEasing), RepeatMode.Restart), label = "x")
        this.clip(shape).background(base).drawWithContent {
            drawContent()
            val w = size.width
            drawRect(Brush.linearGradient(listOf(base.copy(alpha = 0f), glint.copy(alpha = 0.55f), base.copy(alpha = 0f)), start = Offset(w * x - w * 0.4f, 0f), end = Offset(w * x, size.height)))
        }
    }
}

/** Row-shaped skeleton matching [MatchRow]. */
@Composable
fun MatchCardSkeleton(modifier: Modifier = Modifier) {
    Row(modifier.fillMaxWidth().heightIn(min = 72.dp).padding(horizontal = PcSpace.md, vertical = PcSpace.sm), horizontalArrangement = Arrangement.spacedBy(PcSpace.sm), verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.size(width = 40.dp, height = 16.dp).skeleton())
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(horizontalArrangement = Arrangement.spacedBy(PcSpace.xs), verticalAlignment = Alignment.CenterVertically) { Box(Modifier.size(20.dp).skeleton(CircleShape)); Box(Modifier.fillMaxWidth(0.6f).height(14.dp).skeleton()) }
            Row(horizontalArrangement = Arrangement.spacedBy(PcSpace.xs), verticalAlignment = Alignment.CenterVertically) { Box(Modifier.size(20.dp).skeleton(CircleShape)); Box(Modifier.fillMaxWidth(0.45f).height(14.dp).skeleton()) }
        }
        Box(Modifier.size(width = 14.dp, height = 36.dp).skeleton())
    }
}

/** Content padding for lists that sit above the bottom bar. */
val ListPadding = PaddingValues(start = PcSpace.screen, end = PcSpace.screen, top = PcSpace.xs, bottom = PcSpace.lg)

// ---------------- Previews ----------------

@Preview(name = "Chips + segmented", showBackground = true)
@Composable
private fun ControlsPreview() = PreviewHost(dark = false) {
    Column(Modifier.padding(PcSpace.md), verticalArrangement = Arrangement.spacedBy(PcSpace.sm)) {
        FilterChipRow(listOf("All", "Premier Division", "Continental Cup"), "All", {}, contentPadding = 0.dp)
        SegmentedControl(listOf(Segment("Live", 3, live = true), Segment("Upcoming", 5), Segment("Finished", 2)), 0, {})
        StatBar(StatRow("Possession", 58, 42, "58%", "42%"))
        FormDots("WWDLW")
        InfoBanner("Offline. Showing saved scores.", BannerKind.Warning)
        PrimaryButton("Follow", {})
        SectionHeader("Matches", "See all")
    }
}

@Preview(name = "Chips + segmented · dark", showBackground = true, backgroundColor = 0xFF0C0F0D)
@Composable
private fun ControlsDarkPreview() = PreviewHost(dark = true) {
    Column(Modifier.padding(PcSpace.md), verticalArrangement = Arrangement.spacedBy(PcSpace.sm)) {
        FilterChipRow(listOf("All", "Premier Division"), "Premier Division", {}, contentPadding = 0.dp)
        SegmentedControl(listOf(Segment("Live", 1, live = true), Segment("Upcoming", 5), Segment("Finished", 0)), 1, {})
        MatchCardSkeleton()
    }
}
