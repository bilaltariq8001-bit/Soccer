package com.innovativeinside.pitchside.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Newspaper
import androidx.compose.material.icons.filled.SportsSoccer
import androidx.compose.material.icons.outlined.QuestionAnswer
import androidx.compose.material.icons.outlined.EmojiEvents
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Newspaper
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.StarBorder
import androidx.compose.ui.unit.sp
import androidx.compose.material.icons.outlined.SportsSoccer
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.innovativeinside.pitchside.R
import com.innovativeinside.pitchside.ui.preview.PreviewHost
import com.innovativeinside.pitchside.ui.theme.Pc
import com.innovativeinside.pitchside.ui.theme.PcShape
import com.innovativeinside.pitchside.ui.theme.PcSpace
import com.innovativeinside.pitchside.ui.theme.PcType

// ---------------- Top bar ----------------

/** Tab-level top bar. In 0.6 this is the [ScreenHeader] (uppercase title, date, search, bell, avatar). */
@Composable
fun AppTopBar(
    title: String,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    onSearch: (() -> Unit)? = null,
    onNotifications: (() -> Unit)? = null,
    unread: Boolean = false,
    onAvatar: (() -> Unit)? = null,
    avatarInitial: String? = null,
    actions: @Composable () -> Unit = {},
) = ScreenHeader(title, modifier, subtitle, null, onSearch, onNotifications, unread, onAvatar, avatarInitial, actions)

// ---------------- Bottom navigation ----------------

data class NavItem(val route: String, @androidx.annotation.StringRes val label: Int, val icon: ImageVector, val selectedIcon: ImageVector)

val navItems = listOf(
    NavItem("home", R.string.nav_home, Icons.Outlined.Home, Icons.Filled.Home),
    NavItem("matches", R.string.nav_matches, Icons.Outlined.SportsSoccer, Icons.Filled.SportsSoccer),
    NavItem("leagues", R.string.nav_leagues, Icons.Outlined.EmojiEvents, Icons.Filled.EmojiEvents),
    NavItem("news", R.string.nav_news, Icons.Outlined.Newspaper, Icons.Filled.Newspaper),
    NavItem("following", R.string.nav_favorites, Icons.Outlined.StarBorder, Icons.Filled.Star),
)

/**
 * Floating bottom bar with the five tabs: a rounded charcoal bar with a 1dp border, inset from the screen edges.
 * The selected tab is an emerald icon and emerald uppercase label with an emerald underline; the others are grey.
 */
@Composable
fun BottomNavBar(current: String?, onNavigate: (String) -> Unit, badges: Map<String, Int> = emptyMap()) {
    Box(Modifier.fillMaxWidth().background(Pc.Bg).navigationBarsPadding().padding(start = PcSpace.sm, end = PcSpace.sm, top = PcSpace.xxs, bottom = PcSpace.xs)) {
        Row(
            Modifier.fillMaxWidth().clip(PcShape.Nav).background(Pc.Nav).border(1.dp, Pc.Outline, PcShape.Nav).padding(horizontal = PcSpace.xxs),
        ) {
            // Labels scale with the font setting up to 1.15x so five uppercase labels never clip at 360dp.
            val fs = androidx.compose.ui.platform.LocalDensity.current.fontScale
            val labelSize = (12f * minOf(fs, 1.15f) / fs).sp
            navItems.forEach { item ->
                val on = item.route == current
                val label = stringResource(item.label)
                val n = badges[item.route] ?: 0
                Column(
                    Modifier.weight(1f).heightIn(min = 64.dp).testTag("nav_${item.route}").pcClickable(PcShape.Control, role = Role.Tab) { onNavigate(item.route) }
                        .semantics(mergeDescendants = true) { selected = on; contentDescription = if (n > 0) "$label, $n live" else label },
                    horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Bottom,
                ) {
                    Spacer(Modifier.height(10.dp))
                    Box(contentAlignment = Alignment.Center) {
                        Icon(if (on) item.selectedIcon else item.icon, null, tint = if (on) Pc.Accent else Pc.Text2, modifier = Modifier.size(24.dp))
                        if (n > 0) LiveCountBadge(n, Modifier.align(Alignment.TopEnd).offset(x = 10.dp, y = (-6).dp))
                    }
                    Spacer(Modifier.height(3.dp))
                    Text(label.uppercase(), style = PcType.Tag.copy(fontSize = labelSize, lineHeight = labelSize * 1.2f, letterSpacing = 0.5.sp), color = if (on) Pc.Accent else Pc.Text2, maxLines = 1, overflow = TextOverflow.Clip)
                    Spacer(Modifier.height(6.dp))
                    Box(Modifier.size(width = 28.dp, height = 3.dp).clip(PcShape.Chip).background(if (on) Pc.Accent else Color.Transparent))
                }
            }
        }
    }
}

/** Old name. */
@Composable
fun BottomNav(current: String?, onNavigate: (String) -> Unit, badges: Map<String, Int> = emptyMap()) = BottomNavBar(current, onNavigate, badges)

/** Navigation rail for medium/expanded widths: the same five destinations plus Ask. */
@Composable
fun SideRail(current: String?, onNavigate: (String) -> Unit, onAsk: () -> Unit, wide: Boolean, badges: Map<String, Int> = emptyMap()) {
    Row(Modifier.fillMaxHeight()) {
        Column(
            Modifier.fillMaxHeight().width(if (wide) 240.dp else 88.dp).background(Pc.Nav).padding(horizontal = if (wide) PcSpace.md else PcSpace.xs, vertical = PcSpace.lg),
            horizontalAlignment = if (wide) Alignment.Start else Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(PcSpace.xxs),
        ) {
            Row(Modifier.padding(bottom = PcSpace.lg, start = if (wide) PcSpace.xs else 0.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Box(Modifier.size(36.dp).clip(PcShape.Control).background(Pc.Accent), contentAlignment = Alignment.Center) { Text("P", style = PcType.TeamName, color = Pc.OnAccent) }
                if (wide) Text("PITCHSIDE", style = PcType.SectionTitle)
            }
            navItems.forEach { item ->
                val on = item.route == current
                Row(
                    Modifier.then(if (wide) Modifier.fillMaxWidth() else Modifier.size(72.dp, 56.dp)).heightIn(min = PcSpace.touch)
                        .pcClickable(PcShape.Chip, role = Role.Tab) { onNavigate(item.route) }
                        .background(if (on) Pc.AccentContainer else Color.Transparent, PcShape.Chip).padding(horizontal = PcSpace.sm).semantics { selected = on },
                    verticalAlignment = Alignment.CenterVertically, horizontalArrangement = if (wide) Arrangement.spacedBy(PcSpace.sm) else Arrangement.Center,
                ) {
                    Box {
                        Icon(if (on) item.selectedIcon else item.icon, if (wide) null else stringResource(item.label), tint = if (on) Pc.Accent else Pc.Text2, modifier = Modifier.size(22.dp))
                        val n = badges[item.route] ?: 0
                        if (n > 0) LiveCountBadge(n, Modifier.align(Alignment.TopEnd).offset(x = 10.dp, y = (-6).dp))
                    }
                    if (wide) Text(stringResource(item.label).uppercase(), style = PcType.Tag, color = if (on) Pc.Accent else Pc.Text2)
                }
            }
            Spacer(Modifier.weight(1f))
            Row(
                Modifier.then(if (wide) Modifier.fillMaxWidth() else Modifier.size(PcSpace.touch)).heightIn(min = PcSpace.touch)
                    .pcClickable(PcShape.Control, onClick = onAsk).background(Pc.Surface).border(1.dp, Pc.Outline, PcShape.Control),
                verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.Center,
            ) {
                Icon(Icons.Outlined.QuestionAnswer, stringResource(R.string.action_ask), tint = Pc.Accent, modifier = Modifier.size(20.dp))
                if (wide) { Spacer(Modifier.width(PcSpace.xs)); Text(stringResource(R.string.action_ask), style = PcType.LabelStrong, color = Pc.Accent) }
            }
        }
        Box(Modifier.fillMaxHeight().width(1.dp).background(Pc.Outline))
    }
}

/** Small live-count bubble on the Matches tab. */
@Composable
fun LiveCountBadge(count: Int, modifier: Modifier = Modifier) {
    Box(
        modifier.heightIn(min = 16.dp).widthIn(min = 16.dp).clip(CircleShape).background(Pc.LiveFill).padding(horizontal = 4.dp)
            .semantics { contentDescription = "$count live" },
        contentAlignment = Alignment.Center,
    ) { Text(if (count > 9) "9+" else count.toString(), style = PcType.Micro, color = Pc.OnLive) }
}

@Preview(name = "Top bar + bottom nav", showBackground = true, widthDp = 380, backgroundColor = 0xFF0E1110)
@Composable
private fun NavPreview() = PreviewHost(dark = true) {
    Column {
        AppTopBar("Football today", subtitle = "Friday, Nov 13", onSearch = {}, onNotifications = {}, unread = true, onAvatar = {})
        Spacer(Modifier.height(PcSpace.lg))
        BottomNavBar("home", {}, mapOf("matches" to 3))
    }
}
