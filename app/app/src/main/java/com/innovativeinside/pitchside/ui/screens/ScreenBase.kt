package com.innovativeinside.pitchside.ui.screens

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.provider.CalendarContract
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.ScrollState
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
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.innovativeinside.pitchside.R
import com.innovativeinside.pitchside.core.AppError
import com.innovativeinside.pitchside.data.model.Match
import com.innovativeinside.pitchside.data.model.NewsItem
import com.innovativeinside.pitchside.data.model.StandingRow
import com.innovativeinside.pitchside.ui.LocalApp
import com.innovativeinside.pitchside.ui.LocalPrefs
import com.innovativeinside.pitchside.ui.components.BannerKind
import com.innovativeinside.pitchside.ui.components.Chip
import com.innovativeinside.pitchside.ui.components.CrestSize
import com.innovativeinside.pitchside.ui.components.Hairline
import com.innovativeinside.pitchside.ui.components.InfoBanner
import com.innovativeinside.pitchside.ui.components.MatchRow
import com.innovativeinside.pitchside.ui.components.NewsCardCompact
import com.innovativeinside.pitchside.ui.components.PcCard
import com.innovativeinside.pitchside.ui.components.PcSheet
import com.innovativeinside.pitchside.ui.components.StandingsHeader
import com.innovativeinside.pitchside.ui.components.StandingsRow
import com.innovativeinside.pitchside.ui.components.TeamCrest
import com.innovativeinside.pitchside.ui.components.newsDate
import com.innovativeinside.pitchside.ui.components.pcClickable
import com.innovativeinside.pitchside.ui.theme.Pc
import com.innovativeinside.pitchside.ui.theme.PcShape
import com.innovativeinside.pitchside.ui.theme.PcSpace
import com.innovativeinside.pitchside.ui.theme.PcType
import java.time.Instant
import java.time.LocalDate
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
fun Screen(
    modifier: Modifier = Modifier,
    scroll: Boolean = true,
    maxWidth: Int = 720,
    scrollState: ScrollState = rememberScrollState(),
    content: @Composable ColumnScope.() -> Unit,
) {
    Box(modifier.fillMaxSize().background(Pc.Bg).statusBarsPadding(), contentAlignment = Alignment.TopCenter) {
        val col = Modifier.widthIn(max = maxWidth.dp).fillMaxSize()
        Column(if (scroll) col.verticalScroll(scrollState) else col) {
            content()
            Spacer(Modifier.height(PcSpace.lg))
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
    scrollState: ScrollState = rememberScrollState(),
    content: @Composable ColumnScope.() -> Unit,
) {
    PullToRefreshBox(isRefreshing = refreshing, onRefresh = onRefresh, modifier = Modifier.fillMaxSize().background(Pc.Bg)) {
        Screen(maxWidth = maxWidth, scrollState = scrollState) {
            AnimatedVisibility(refreshError != null) {
                InfoBanner(stringResource(R.string.refresh_failed), BannerKind.Warning, Modifier.padding(horizontal = PcSpace.screen, vertical = PcSpace.xs), action = "OK", onAction = onDismissError)
            }
            content()
        }
    }
}

fun greeting(): String {
    val h = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
    return when { h < 12 -> "Good morning"; h < 18 -> "Good afternoon"; else -> "Good evening" }
}

fun timeAgo(ms: Long, now: Long = System.currentTimeMillis()): String = newsDate(ms, now)

// Built per call so a locale change while the app runs is picked up.
private fun dayFmt() = DateTimeFormatter.ofPattern("EEE d MMM", Locale.getDefault())
private fun timeFmt() = DateTimeFormatter.ofPattern("HH:mm", Locale.getDefault())
fun formatDay(ms: Long): String = Instant.ofEpochMilli(ms).atZone(ZoneId.systemDefault()).format(dayFmt())
fun formatKickoff(ms: Long): String = Instant.ofEpochMilli(ms).atZone(ZoneId.systemDefault()).let { "${it.format(dayFmt())} · ${it.format(timeFmt())}" }
fun longDate(d: LocalDate): String = d.format(DateTimeFormatter.ofPattern("EEEE d MMMM", Locale.getDefault()))

@Composable
fun SectionTitle(text: String, modifier: Modifier = Modifier) =
    Text(text, style = PcType.Subtitle, modifier = modifier.semantics { heading() })

/** Compact news row (old name). */
@Composable
fun NewsRow(item: NewsItem, onClick: () -> Unit, modifier: Modifier = Modifier) = NewsCardCompact(item, onClick, modifier)

/** Opens a calendar "new event" screen prefilled with the match (no calendar permission needed). */
fun addMatchToCalendar(ctx: Context, m: Match) {
    val intent = Intent(Intent.ACTION_INSERT).setData(CalendarContract.Events.CONTENT_URI)
        .putExtra(CalendarContract.EXTRA_EVENT_BEGIN_TIME, m.kickoffMs)
        .putExtra(CalendarContract.EXTRA_EVENT_END_TIME, m.kickoffMs + 115 * 60_000L)
        .putExtra(CalendarContract.Events.TITLE, "${m.home.name} vs ${m.away.name}")
        .putExtra(CalendarContract.Events.EVENT_LOCATION, m.venue)
        .putExtra(CalendarContract.Events.DESCRIPTION, "${m.leagueName} · ${m.round}")
    try { ctx.startActivity(intent) } catch (_: ActivityNotFoundException) { }
}

/**
 * Matches in one tile, separated by hairlines. Each row has the favourite star (opens a sheet to follow
 * either club) and, for upcoming games, the kickoff reminder bell (match alerts).
 */
@Composable
fun MatchList(matches: List<Match>, openMatch: (String) -> Unit, modifier: Modifier = Modifier) {
    val app = LocalApp.current
    val prefs = LocalPrefs.current
    var followFor by remember { mutableStateOf<Match?>(null) }
    PcCard(modifier.fillMaxWidth()) {
        Column {
            matches.forEachIndexed { i, m ->
                MatchRow(
                    m, app.isHidden(prefs, m), { openMatch(m.id) }, { app.reveal(m.id) },
                    favorite = m.involves(prefs.teams), onFavorite = { followFor = m },
                    reminderOn = m.id in prefs.matchAlerts, onReminder = { app.toggleMatchAlert(m.id) },
                )
                if (i < matches.lastIndex) Hairline(Modifier.padding(horizontal = PcSpace.sm))
            }
        }
    }
    followFor?.let { m -> FollowTeamsSheet(m) { followFor = null } }
}

/** Bottom sheet listing both clubs of a match with follow toggles. */
@Composable
fun FollowTeamsSheet(m: Match, onDismiss: () -> Unit) {
    val app = LocalApp.current
    val prefs = LocalPrefs.current
    PcSheet(onDismiss) {
        Text("Follow a team", style = PcType.Subtitle)
        Text("Followed teams' matches come first on Home and in alerts.", style = PcType.BodyMuted)
        listOf(m.home, m.away).forEach { t ->
            val on = t.id in prefs.teams
            Row(Modifier.fillMaxWidth().heightIn(min = 56.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(PcSpace.sm)) {
                TeamCrest(t, CrestSize.M.dp)
                Text(t.name, style = PcType.BodyStrong, modifier = Modifier.weight(1f), maxLines = 1, overflow = TextOverflow.Ellipsis)
                Chip(if (on) stringResource(R.string.action_following) else stringResource(R.string.action_follow), on, { app.toggleTeam(t.id, t.name) })
            }
        }
    }
}

/** Matches of one competition under a collapsible header (badge, name, count). */
@Composable
fun LeagueGroup(name: String, matches: List<Match>, openMatch: (String) -> Unit, modifier: Modifier = Modifier, openLeague: (() -> Unit)? = null, short: String? = null) {
    var open by rememberSaveable(name) { mutableStateOf(true) }
    Column(modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(PcSpace.xs)) {
        Row(
            Modifier.fillMaxWidth().heightIn(min = PcSpace.touch).pcClickable(PcShape.Control, if (open) "Collapse $name" else "Expand $name") { open = !open }.padding(horizontal = PcSpace.xxs),
            verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(PcSpace.xs),
        ) {
            LeagueBadge(short ?: name.split(" ").filter { it.isNotBlank() }.take(2).joinToString("") { it.first().uppercase() }, 28)
            Text(name, style = PcType.BodyStrong, modifier = Modifier.weight(1f).semantics { heading() }, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text("${matches.size}", style = PcType.Numeric, color = Pc.Text2)
            if (openLeague != null) Text("Table", style = PcType.LabelStrong, color = Pc.Accent, modifier = Modifier.pcClickable(PcShape.Chip, "Open $name", onClick = openLeague).padding(horizontal = PcSpace.xs, vertical = 14.dp))
            Icon(if (open) Icons.Filled.ExpandLess else Icons.Filled.ExpandMore, null, tint = Pc.Text2)
        }
        AnimatedVisibility(open) { MatchList(matches, openMatch) }
    }
}

/** League badge: the competition short code on a neutral rounded square. */
@Composable
fun LeagueBadge(short: String, size: Int = 40) {
    Box(Modifier.size(size.dp).clip(PcShape.Small).background(Pc.SurfaceVariant), contentAlignment = Alignment.Center) {
        Text(short.take(3), style = if (size >= 48) PcType.BodyStrong else PcType.Overline.copy(fontWeight = androidx.compose.ui.text.font.FontWeight.Bold), color = Pc.Accent, maxLines = 1)
    }
}

/** Full league table in one tile with a header row. [highlight] marks the clubs of the current match. */
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
            StandingsHeader(showForm = showForm, compact = compact)
            rows.forEachIndexed { i, r ->
                StandingsRow(r, { openTeam(r.team.id) }, highlight = r.team.id in highlight, followed = r.team.id in followed, showForm = showForm, compact = compact)
                if (i < rows.lastIndex) Hairline()
            }
        }
    }
}
