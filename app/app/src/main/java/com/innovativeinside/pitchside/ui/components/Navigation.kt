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
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.EmojiEvents
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Newspaper
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material.icons.outlined.Search
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

/**
 * Tab-level top bar: title (or greeting) with a date/subtitle line, then search and notifications.
 * The notifications action shows a red dot when [unread] is true.
 */
@Composable
fun AppTopBar(
    title: String,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    onSearch: (() -> Unit)? = null,
    onNotifications: (() -> Unit)? = null,
    unread: Boolean = false,
    actions: @Composable () -> Unit = {},
) {
    Row(modifier.fillMaxWidth().padding(start = PcSpace.screen, end = PcSpace.xs, top = PcSpace.sm, bottom = PcSpace.xs), verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            if (subtitle != null) Text(subtitle.uppercase(), style = PcType.Overline, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(title, style = PcType.Title, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.semantics { heading() })
        }
        actions()
        if (onSearch != null) IconAction(Icons.Outlined.Search, stringResource(R.string.action_search), onSearch)
        if (onNotifications != null) IconAction(Icons.Outlined.Notifications, if (unread) "Notifications, unread" else "Notifications", onNotifications, dot = unread)
    }
}

// ---------------- Bottom navigation ----------------

data class NavItem(val route: String, @androidx.annotation.StringRes val label: Int, val icon: ImageVector, val selectedIcon: ImageVector)

val navItems = listOf(
    NavItem("home", R.string.nav_home, Icons.Outlined.Home, Icons.Filled.Home),
    NavItem("matches", R.string.nav_matches, Icons.Outlined.SportsSoccer, Icons.Filled.SportsSoccer),
    NavItem("leagues", R.string.nav_leagues, Icons.Outlined.EmojiEvents, Icons.Filled.EmojiEvents),
    NavItem("news", R.string.nav_news, Icons.Outlined.Newspaper, Icons.Filled.Newspaper),
    NavItem("following", R.string.nav_favorites, Icons.Outlined.FavoriteBorder, Icons.Filled.Favorite),
)

/**
 * Docked bottom bar with the five tabs. The selected tab gets an accent pill behind its icon and an
 * accent label. Content above it gets its own bottom padding (the bar is outside the scroll area).
 */
@Composable
fun BottomNavBar(current: String?, onNavigate: (String) -> Unit, badges: Map<String, Int> = emptyMap()) {
    Column(Modifier.background(Pc.Nav).navigationBarsPadding()) {
        Hairline()
        Row(Modifier.fillMaxWidth().padding(horizontal = PcSpace.xxs, vertical = 6.dp)) {
            navItems.forEach { item ->
                val on = item.route == current
                val label = stringResource(item.label)
                val n = badges[item.route] ?: 0
                Column(
                    Modifier.weight(1f).heightIn(min = 56.dp).testTag("nav_${item.route}").pcClickable(PcShape.Control, role = Role.Tab) { onNavigate(item.route) }
                        .semantics(mergeDescendants = true) { selected = on; contentDescription = if (n > 0) "$label, $n live" else label },
                    horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center,
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Box(Modifier.size(width = 56.dp, height = 30.dp).clip(PcShape.Chip).background(if (on) Pc.AccentContainer else Color.Transparent))
                        Icon(if (on) item.selectedIcon else item.icon, null, tint = if (on) Pc.Accent else Pc.Text2, modifier = Modifier.size(22.dp))
                        if (n > 0) LiveCountBadge(n, Modifier.align(Alignment.TopEnd).offset(x = (-6).dp, y = (-2).dp))
                    }
                    Spacer(Modifier.height(4.dp))
                    Text(label, style = if (on) PcType.LabelStrong else PcType.Label, color = if (on) Pc.Accent else Pc.Text2, maxLines = 1)
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
                Box(Modifier.size(36.dp).clip(PcShape.Control).background(Pc.Hero), contentAlignment = Alignment.Center) { Text("P", style = PcType.Subtitle, color = Pc.OnHero) }
                if (wide) Text("Pitchside", style = PcType.Subtitle)
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
                    if (wide) Text(stringResource(item.label), style = if (on) PcType.LabelStrong else PcType.Label, color = if (on) Pc.Accent else Pc.Text2)
                }
            }
            Spacer(Modifier.weight(1f))
            Row(
                Modifier.then(if (wide) Modifier.fillMaxWidth() else Modifier.size(PcSpace.touch)).heightIn(min = PcSpace.touch)
                    .pcClickable(PcShape.Control, onClick = onAsk).background(Pc.Surface).border(1.dp, Pc.Outline, PcShape.Control),
                verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.Center,
            ) {
                Icon(Icons.Outlined.AutoAwesome, stringResource(R.string.action_ask), tint = Pc.Accent, modifier = Modifier.size(20.dp))
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

@Preview(name = "Top bar + bottom nav", showBackground = true, widthDp = 380)
@Composable
private fun NavPreview() = PreviewHost(dark = false) {
    Column {
        AppTopBar("Football today", subtitle = "Friday 13 November", onSearch = {}, onNotifications = {}, unread = true)
        Spacer(Modifier.height(PcSpace.lg))
        BottomNavBar("home", {}, mapOf("matches" to 3))
    }
}
