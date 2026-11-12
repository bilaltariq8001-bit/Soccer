package com.innovativeinside.pitchside.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.outlined.Analytics
import androidx.compose.material.icons.outlined.Article
import androidx.compose.material.icons.outlined.SwapHoriz
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.innovativeinside.pitchside.R
import com.innovativeinside.pitchside.core.AppError
import com.innovativeinside.pitchside.data.model.Match
import com.innovativeinside.pitchside.data.model.NewsCategory
import com.innovativeinside.pitchside.data.model.NewsItem
import com.innovativeinside.pitchside.data.model.StandingRow
import com.innovativeinside.pitchside.ui.LocalApp
import com.innovativeinside.pitchside.ui.LocalPrefs
import com.innovativeinside.pitchside.ui.components.BannerKind
import com.innovativeinside.pitchside.ui.components.Crest
import com.innovativeinside.pitchside.ui.components.FormChip
import com.innovativeinside.pitchside.ui.components.InfoBanner
import com.innovativeinside.pitchside.ui.components.MatchCard
import com.innovativeinside.pitchside.ui.components.Overline
import com.innovativeinside.pitchside.ui.components.PcCard
import com.innovativeinside.pitchside.ui.components.pcClickable
import com.innovativeinside.pitchside.ui.theme.Pc
import com.innovativeinside.pitchside.ui.theme.PcShape
import com.innovativeinside.pitchside.ui.theme.PcType
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Calendar
import java.util.Locale

enum class WidthClass { Compact, Medium, Expanded }

@Composable
fun widthClass(): WidthClass {
    val w = LocalConfiguration.current.screenWidthDp
    return when {
        w >= 840 -> WidthClass.Expanded
        w >= 600 -> WidthClass.Medium
        else -> WidthClass.Compact
    }
}

/** Page container: status bar inset, background, optional scroll, readable max width on big screens. */
@Composable
fun Screen(modifier: Modifier = Modifier, scroll: Boolean = true, maxWidth: Int = 720, content: @Composable ColumnScope.() -> Unit) {
    Box(modifier.fillMaxSize().background(Pc.Bg).statusBarsPadding(), contentAlignment = Alignment.TopCenter) {
        val col = Modifier.widthIn(max = maxWidth.dp).fillMaxSize()
        Column(if (scroll) col.verticalScroll(rememberScrollState()) else col) {
            content()
            Spacer(Modifier.height(24.dp))
        }
    }
}

/** [Screen] with pull-to-refresh and the "couldn't refresh" banner. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RefreshScreen(
    refreshing: Boolean,
    onRefresh: () -> Unit,
    refreshError: AppError?,
    onDismissError: () -> Unit,
    maxWidth: Int = 720,
    content: @Composable ColumnScope.() -> Unit,
) {
    PullToRefreshBox(isRefreshing = refreshing, onRefresh = onRefresh, modifier = Modifier.fillMaxSize().background(Pc.Bg)) {
        Screen(maxWidth = maxWidth) {
            AnimatedVisibility(refreshError != null) {
                InfoBanner(stringResource(R.string.refresh_failed), BannerKind.Warning, Modifier.padding(horizontal = 16.dp, vertical = 8.dp), action = "OK", onAction = onDismissError)
            }
            content()
        }
    }
}

fun greeting(): String {
    val h = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
    return when { h < 12 -> "Good morning"; h < 18 -> "Good afternoon"; else -> "Good evening" }
}

fun timeAgo(ms: Long, now: Long = System.currentTimeMillis()): String {
    val m = ((now - ms) / 60_000).coerceAtLeast(0)
    return when {
        m < 1 -> "Just now"
        m < 60 -> "$m min ago"
        m < 24 * 60 -> "${m / 60} h ago"
        m < 48 * 60 -> "Yesterday"
        else -> "${m / (24 * 60)} days ago"
    }
}

private val dayFmt = DateTimeFormatter.ofPattern("EEE d MMM", Locale.getDefault())
private val timeFmt = DateTimeFormatter.ofPattern("HH:mm", Locale.getDefault())
fun formatDay(ms: Long): String = Instant.ofEpochMilli(ms).atZone(ZoneId.systemDefault()).format(dayFmt)
fun formatKickoff(ms: Long): String = Instant.ofEpochMilli(ms).atZone(ZoneId.systemDefault()).let { "${it.format(dayFmt)} · ${it.format(timeFmt)}" }

@Composable
fun SectionTitle(text: String, modifier: Modifier = Modifier) =
    Text(text, style = PcType.Headline, modifier = modifier.semantics { heading() })

@Composable
fun NewsThumb(category: NewsCategory, modifier: Modifier = Modifier) {
    val (icon, tint) = when (category) {
        NewsCategory.Reports -> Icons.Outlined.Article to Pc.Lime
        NewsCategory.Transfers -> Icons.Outlined.SwapHoriz to Pc.Amber
        NewsCategory.Analysis -> Icons.Outlined.Analytics to Pc.Live
    }
    Box(modifier.clip(PcShape.Control).background(Pc.Raised), contentAlignment = Alignment.Center) { Icon(icon, null, tint = tint, modifier = Modifier.size(28.dp)) }
}

@Composable
fun NewsRow(item: NewsItem, onClick: () -> Unit, modifier: Modifier = Modifier) {
    PcCard(modifier.fillMaxWidth(), onClick = onClick) {
        Row(Modifier.padding(12.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            NewsThumb(item.category, Modifier.size(width = 72.dp, height = 72.dp))
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Overline(item.category.label.uppercase())
                Text(item.title, style = PcType.Subhead.copy(fontSize = 15.sp, lineHeight = 20.sp), maxLines = 2, overflow = TextOverflow.Ellipsis)
                Text("${timeAgo(item.publishedMs)} · ${item.readMinutes} min read", style = PcType.Meta)
            }
        }
    }
}

/** Matches of one competition in a collapsible card. Followed teams' rows are marked by the card. */
@Composable
fun LeagueGroup(name: String, matches: List<Match>, openMatch: (String) -> Unit, modifier: Modifier = Modifier, openLeague: (() -> Unit)? = null) {
    val app = LocalApp.current
    val prefs = LocalPrefs.current
    var open by rememberSaveable(name) { mutableStateOf(true) }
    Column(modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(
            Modifier.fillMaxWidth().heightIn(min = 48.dp).pcClickable(PcShape.Control, if (open) "Collapse $name" else "Expand $name") { open = !open }.padding(horizontal = 4.dp),
            verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(name, style = PcType.Subhead, modifier = Modifier.weight(1f).semantics { heading() })
            Text("${matches.size}", style = PcType.Meta)
            if (openLeague != null) Text("Table", style = PcType.Label.copy(color = Pc.Lime), modifier = Modifier.pcClickable(PcShape.Chip, "Open $name", onClick = openLeague).padding(horizontal = 8.dp, vertical = 12.dp))
            Icon(if (open) Icons.Filled.ExpandLess else Icons.Filled.ExpandMore, null, tint = Pc.Text2)
        }
        AnimatedVisibility(open) {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                matches.forEach { m -> MatchCard(m, app.isHidden(prefs, m), { openMatch(m.id) }, { app.reveal(m.id) }) }
            }
        }
    }
}

/** Full league table. [highlight] marks the clubs of the current match. */
@Composable
fun StandingsTable(
    rows: List<StandingRow>,
    openTeam: (String) -> Unit,
    modifier: Modifier = Modifier,
    highlight: Set<String> = emptySet(),
    compact: Boolean = false,
    showForm: Boolean = false,
) {
    val followed = LocalPrefs.current.teams.toSet()
    PcCard(modifier.fillMaxWidth()) {
        Column {
            Row(Modifier.fillMaxWidth().height(40.dp).background(Pc.Raised).padding(horizontal = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                Text("#", style = PcType.Meta, modifier = Modifier.width(24.dp), textAlign = TextAlign.Center)
                Text("Club", style = PcType.Meta, modifier = Modifier.weight(1f).padding(start = 8.dp))
                Text("P", style = PcType.Meta, modifier = Modifier.width(28.dp), textAlign = TextAlign.Center)
                if (!compact) Text("W-D-L", style = PcType.Meta, modifier = Modifier.width(60.dp), textAlign = TextAlign.Center)
                Text("GD", style = PcType.Meta, modifier = Modifier.width(36.dp), textAlign = TextAlign.Center)
                Text("Pts", style = PcType.Meta.copy(color = Pc.Text), modifier = Modifier.width(36.dp), textAlign = TextAlign.End)
            }
            rows.forEachIndexed { i, r ->
                val fav = r.team.id in followed
                val mark = r.team.id in highlight
                val desc = "${r.rank}, ${r.team.name}, ${r.points} points, played ${r.played}, goal difference ${r.goalDiff}"
                Column(
                    Modifier.fillMaxWidth().background(if (fav) Pc.LimeTintSoft else if (mark) Pc.Raised else Color.Transparent)
                        .pcClickable(androidx.compose.ui.graphics.RectangleShape, "Open ${r.team.name}") { openTeam(r.team.id) }
                        .semantics(mergeDescendants = true) { contentDescription = desc },
                ) {
                    Row(Modifier.fillMaxWidth().heightIn(min = 52.dp).padding(horizontal = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                        Text(r.rank.toString(), style = PcType.Label, modifier = Modifier.width(24.dp), textAlign = TextAlign.Center)
                        Row(Modifier.weight(1f).padding(start = 8.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Crest(r.team, 24.dp)
                            Text(r.team.name, style = PcType.Body.copy(fontSize = 15.sp, fontWeight = if (fav || mark) FontWeight.ExtraBold else FontWeight.SemiBold), maxLines = 1, overflow = TextOverflow.Ellipsis)
                        }
                        Text(r.played.toString(), style = PcType.BodySmall, modifier = Modifier.width(28.dp), textAlign = TextAlign.Center)
                        if (!compact) Text("${r.won}-${r.drawn}-${r.lost}", style = PcType.BodySmall.copy(fontSize = 13.sp), modifier = Modifier.width(60.dp), textAlign = TextAlign.Center)
                        Text(if (r.goalDiff > 0) "+${r.goalDiff}" else r.goalDiff.toString(), style = PcType.BodySmall.copy(fontSize = 13.sp, color = if (r.goalDiff < 0) Pc.Red else Pc.Text2), modifier = Modifier.width(36.dp), textAlign = TextAlign.Center)
                        Text(r.points.toString(), style = PcType.ScoreS, modifier = Modifier.width(36.dp), textAlign = TextAlign.End)
                    }
                    if (showForm && r.form.isNotEmpty()) Row(Modifier.padding(start = 44.dp, bottom = 10.dp), horizontalArrangement = Arrangement.spacedBy(4.dp)) { r.form.forEach { FormChip(it) } }
                }
                if (i < rows.lastIndex) Box(Modifier.fillMaxWidth().height(1.dp).background(Pc.Border))
            }
        }
    }
}
