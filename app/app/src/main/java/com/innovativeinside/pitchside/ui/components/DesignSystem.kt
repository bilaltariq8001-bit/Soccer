package com.innovativeinside.pitchside.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.NotificationsNone
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.innovativeinside.pitchside.R
import com.innovativeinside.pitchside.data.model.StandingRow
import com.innovativeinside.pitchside.ui.preview.PreviewData
import com.innovativeinside.pitchside.ui.preview.PreviewHost
import com.innovativeinside.pitchside.ui.theme.Pc
import com.innovativeinside.pitchside.ui.theme.PcShape
import com.innovativeinside.pitchside.ui.theme.PcSpace
import com.innovativeinside.pitchside.ui.theme.PcType

/*
 * Pitchside 0.6 design system, part 2: the "Football Today" signature pieces: emerald glows, the glow hero card,
 * the screen header (title + date, search, bell, avatar) and the mini standings card.
 */

// ---------------- Glows ----------------

/** Screen background with the emerald glow; use as the root background of every screen. */
@Composable
fun Modifier.glowBackground(): Modifier {
    val bg = Pc.Bg
    val glow = Pc.Glow.copy(alpha = if (Pc.IsDark) 0.13f else 0.07f)
    return this.background(bg).drawBehind {
        val r = size.width * 0.95f
        drawCircle(Brush.radialGradient(listOf(glow, Color.Transparent), center = Offset(size.width * 0.5f, size.width * 0.32f), radius = r), radius = r, center = Offset(size.width * 0.5f, size.width * 0.32f))
    }
}

/**
 * GlowCard: the hero surface. A raised charcoal-green vertical gradient, an emerald radial glow from the top, a
 * 1dp border that is emerald at the top and fades to the hairline, 24dp radius, and a faint emerald halo around
 * the card. Content on it uses the OnHero colours (it stays dark in the light theme too).
 */
@Composable
fun GlowCard(modifier: Modifier = Modifier, onClick: (() -> Unit)? = null, shape: Shape = PcShape.Hero, content: @Composable () -> Unit) {
    val glow = Pc.Glow
    val hero = Pc.Hero
    val raised = Pc.HeroRaised
    val outline = Pc.Outline
    val halo = glow.copy(alpha = if (Pc.IsDark) 0.10f else 0.14f)
    val base = modifier.drawBehind {
        val r = size.width * 0.62f
        drawCircle(Brush.radialGradient(listOf(halo, Color.Transparent), center = Offset(size.width / 2, size.height * 0.45f), radius = r), radius = r, center = Offset(size.width / 2, size.height * 0.45f))
    }
    val clicked = if (onClick != null) base.pcClickable(shape, pressScale = true, onClick = onClick) else base.clip(shape)
    Box(
        clicked.background(Brush.verticalGradient(listOf(raised, hero)), shape)
            .drawBehind {
                val r = size.width * 0.7f
                drawCircle(Brush.radialGradient(listOf(glow.copy(alpha = 0.22f), Color.Transparent), center = Offset(size.width / 2, 0f), radius = r), radius = r, center = Offset(size.width / 2, 0f))
            }
            .border(1.dp, Brush.verticalGradient(listOf(glow.copy(alpha = 0.45f), outline)), shape),
    ) { content() }
}

// ---------------- Screen header ----------------

/**
 * ScreenHeader: big condensed UPPERCASE title with a grey uppercase subtitle (the date) on the left; search, the
 * notification bell (emerald dot when [unread]) and the avatar on the right. [onBack] adds a back button for
 * pushed screens.
 */
@Composable
fun ScreenHeader(
    title: String,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    onBack: (() -> Unit)? = null,
    onSearch: (() -> Unit)? = null,
    onNotifications: (() -> Unit)? = null,
    unread: Boolean = false,
    onAvatar: (() -> Unit)? = null,
    avatarInitial: String? = null,
    actions: @Composable () -> Unit = {},
) {
    Row(
        modifier.fillMaxWidth().padding(start = if (onBack != null) PcSpace.xs else PcSpace.screen + 2.dp, end = PcSpace.xs, top = PcSpace.md, bottom = PcSpace.xs),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (onBack != null) { BackButton(onBack); Spacer(Modifier.width(PcSpace.xs)) }
        Column(Modifier.weight(1f)) {
            Text(title.uppercase(), style = if (onBack != null) PcType.ScreenTitle.copy(fontSize = 26.sp, lineHeight = 28.sp) else PcType.ScreenTitle, maxLines = 2, overflow = TextOverflow.Ellipsis, modifier = Modifier.semantics { heading() })
            if (subtitle != null) Text(subtitle.uppercase(), style = PcType.Kicker, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
        actions()
        if (onSearch != null) IconAction(Icons.Outlined.Search, stringResource(R.string.action_search), onSearch, tint = Pc.Text2)
        if (onNotifications != null) IconAction(Icons.Outlined.NotificationsNone, if (unread) "Notifications, unread" else "Notifications", onNotifications, tint = Pc.Text2, dot = unread)
        if (onAvatar != null) Avatar(avatarInitial, onAvatar)
    }
}

/** Profile avatar: the account initial on an emerald-tinted disc with a ring, or a person glyph for guests. */
@Composable
fun Avatar(initial: String?, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Box(modifier.size(PcSpace.touch).pcClickable(CircleShape, "Account and settings", onClick = onClick).semantics { contentDescription = "Account and settings" }, contentAlignment = Alignment.Center) {
        Box(
            Modifier.size(42.dp).clip(CircleShape).background(Brush.linearGradient(listOf(Pc.AccentContainer, Pc.SurfaceVariant))).border(1.5.dp, Pc.Accent.copy(alpha = 0.55f), CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            if (initial.isNullOrBlank()) Icon(Icons.Outlined.Person, null, tint = Pc.Text, modifier = Modifier.size(22.dp))
            else Text(initial.take(1).uppercase(), style = PcType.TeamName, color = Pc.Text)
        }
    }
}

// ---------------- League badge ----------------

/** LeagueBadge: the competition logo drawn on the surface, or its short code on a charcoal square while loading / when there is none. */
@Composable
fun LeagueLogo(short: String, size: Int = 40, logo: String? = null) {
    val code: @Composable () -> Unit = {
        Box(Modifier.fillMaxSize().clip(PcShape.Small).background(Pc.SurfaceVariant).border(1.dp, Pc.Outline, PcShape.Small), contentAlignment = Alignment.Center) {
            Text(short.take(3).uppercase(), style = if (size >= 48) PcType.TeamName else PcType.Overline.copy(fontSize = 11.sp), color = Pc.Accent, maxLines = 1)
        }
    }
    Box(Modifier.size(size.dp).clearAndSetSemantics { }, contentAlignment = Alignment.Center) {
        if (logo == null) code() else RemoteImage(logo, code, Modifier.fillMaxSize(), ContentScale.Fit)
    }
}

// ---------------- Previews ----------------

@Preview(name = "Header + glow card · dark", showBackground = true, widthDp = 380, backgroundColor = 0xFF0E1110)
@Composable
private fun DesignSystemPreview() = PreviewHost(dark = true) {
    Column(Modifier.glowBackground().padding(bottom = PcSpace.md), verticalArrangement = Arrangement.spacedBy(PcSpace.sm)) {
        ScreenHeader("Football today", subtitle = "Sunday, Nov 15", onSearch = {}, onNotifications = {}, unread = true, onAvatar = {}, avatarInitial = "M")
        GlowCard(Modifier.padding(horizontal = PcSpace.md).fillMaxWidth().height(120.dp)) {}
    }
}
