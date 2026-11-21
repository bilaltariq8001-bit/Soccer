package com.innovativeinside.pitchside.ui.screens

import androidx.compose.ui.unit.sp
import androidx.compose.foundation.background
import androidx.compose.ui.draw.clip
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.EventBusy
import androidx.compose.material.icons.outlined.Star
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDefaults
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Text
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.innovativeinside.pitchside.R
import com.innovativeinside.pitchside.core.UiState
import com.innovativeinside.pitchside.data.model.Match
import com.innovativeinside.pitchside.ui.LocalApp
import com.innovativeinside.pitchside.ui.LocalPrefs
import com.innovativeinside.pitchside.ui.components.AppTopBar
import com.innovativeinside.pitchside.ui.components.Chip
import com.innovativeinside.pitchside.ui.components.EmptyState
import com.innovativeinside.pitchside.ui.components.IconAction
import com.innovativeinside.pitchside.ui.components.StateContent
import com.innovativeinside.pitchside.ui.components.TextAction
import com.innovativeinside.pitchside.ui.components.pcClickable
import com.innovativeinside.pitchside.ui.preview.PreviewData
import com.innovativeinside.pitchside.ui.preview.PreviewHost
import com.innovativeinside.pitchside.ui.theme.Pc
import com.innovativeinside.pitchside.ui.theme.PcShape
import com.innovativeinside.pitchside.ui.theme.PcSpace
import com.innovativeinside.pitchside.ui.theme.PcType
import com.innovativeinside.pitchside.ui.vm.MatchFilter
import com.innovativeinside.pitchside.ui.vm.MatchesViewModel
import com.innovativeinside.pitchside.ui.vm.competitionsOf
import com.innovativeinside.pitchside.ui.vm.filterByCompetition
import com.innovativeinside.pitchside.ui.vm.filterMatches
import com.innovativeinside.pitchside.ui.vm.groupByLeague
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.util.Locale

@Composable
fun MatchesRoute(openMatch: (String) -> Unit, openLeague: (String) -> Unit, openSearch: () -> Unit, initialFilter: MatchFilter? = null, vm: MatchesViewModel = viewModel(factory = MatchesViewModel.Factory)) {
    val state by vm.state.collectAsStateWithLifecycle()
    val date by vm.date.collectAsStateWithLifecycle()
    val filter by vm.filter.collectAsStateWithLifecycle()
    val myTeams by vm.myTeamsOnly.collectAsStateWithLifecycle()
    val refreshing by vm.refreshing.collectAsStateWithLifecycle()
    val refreshError by vm.refreshError.collectAsStateWithLifecycle()
    val nextMatchDay by vm.nextMatchDay.collectAsStateWithLifecycle()
    var applied by rememberSaveable { mutableStateOf(false) }
    if (!applied && initialFilter != null) { vm.setFilter(initialFilter); applied = true }
    RefreshScreen(refreshing, vm::refresh, refreshError, vm::dismissRefreshError) {
        MatchesContent(state, date, vm.today, filter, vm::selectDate, vm::setFilter, vm::retry, openMatch, openLeague, openSearch, myTeams, vm::setMyTeamsOnly, nextMatchDay)
    }
}

private val segmentFilters = listOf(MatchFilter.All, MatchFilter.Live, MatchFilter.Upcoming, MatchFilter.Finished)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MatchesContent(
    state: UiState<List<Match>>,
    date: LocalDate,
    today: LocalDate,
    filter: MatchFilter,
    onDate: (LocalDate) -> Unit,
    onFilter: (MatchFilter) -> Unit,
    onRetry: () -> Unit,
    openMatch: (String) -> Unit,
    openLeague: (String) -> Unit,
    openSearch: () -> Unit,
    myTeams: Boolean = false,
    onMyTeams: (Boolean) -> Unit = {},
    nextMatchDay: LocalDate? = null,
) {
    val app = LocalApp.current
    val prefs = LocalPrefs.current
    val competition by app.competition.collectAsStateWithLifecycle()
    var calendar by rememberSaveable { mutableStateOf(false) }
    AppTopBar("Matches", subtitle = dayLabel(date, today), onSearch = openSearch, actions = {
        if (date != today) TextAction("Today", { onDate(today) })
        IconAction(Icons.Outlined.CalendarMonth, "Pick a date", { calendar = true })
    })
    DateStrip(date, today, onDate)
    Column(Modifier.padding(horizontal = PcSpace.screen, vertical = PcSpace.sm), verticalArrangement = Arrangement.spacedBy(PcSpace.sm)) {
        val all = (state as? UiState.Success)?.data.orEmpty()
        val comps = competitionsOf(all, prefs.leagues)
        val selectedComp = competition?.takeIf { id -> comps.any { it.first == id } }
        val byComp = filterByCompetition(all, selectedComp).let { if (myTeams) filterMatches(it, MatchFilter.MyTeams, prefs.teams, prefs.leagues) else it }
        val segIndex = segmentFilters.indexOf(filter).coerceAtLeast(0)
        val liveCount = filterMatches(byComp, MatchFilter.Live, prefs.teams, prefs.leagues).size
        // One quiet filter row that is always there, so nothing jumps while a day loads. Leagues are already grouped below,
        // so the old per-competition chip row is gone; a competition picked elsewhere shows as one clearable chip.
        Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(PcSpace.xs), verticalAlignment = Alignment.CenterVertically) {
            Chip("All", segIndex == 0, { onFilter(MatchFilter.All) })
            Chip("Live", segIndex == 1, { onFilter(MatchFilter.Live) }, count = liveCount.takeIf { it > 0 })
            Chip("Upcoming", segIndex == 2, { onFilter(MatchFilter.Upcoming) })
            Chip("Finished", segIndex == 3, { onFilter(MatchFilter.Finished) })
            Chip("My teams", myTeams, { onMyTeams(!myTeams) }, leading = Icons.Outlined.Star)
            if (selectedComp != null) Chip("${comps.first { it.first == selectedComp }.second}  ✕", true, { app.setCompetition(null) })
        }
        StateContent(
            state, onRetry,
            empty = { NoMatchesState(date, today, nextMatchDay, onDate) },
        ) { _ ->
            val shown = filterMatches(byComp, segmentFilters[segIndex], prefs.teams, prefs.leagues)
            Column(verticalArrangement = Arrangement.spacedBy(PcSpace.gutter)) {
                if (shown.isEmpty()) {
                    EmptyState(
                        when { myTeams -> "None of your teams or leagues play"; filter == MatchFilter.Live -> "Nothing live right now"; else -> "No matches here" },
                        when { myTeams -> "Follow more teams from Search, or show every match."; filter == MatchFilter.Live -> "Live games appear here at kickoff. Alerts tell you when your teams start."; else -> "Try another filter or day." },
                        actionLabel = "Show all matches", onAction = { onFilter(MatchFilter.All); onMyTeams(false); app.setCompetition(null) }, icon = Icons.Outlined.EventBusy,
                    )
                } else groupByLeague(shown, prefs.leagues).forEach { (name, ms) ->
                    val first = ms.first()
                    androidx.compose.runtime.key(first.leagueId + name) {
                        LeagueGroup(
                            name, ms, openMatch, openLeague = { openLeague(first.leagueId) }, logo = ms.firstNotNullOfOrNull { it.leagueLogo },
                            country = first.country, stateKey = "lg-" + first.leagueId + name,
                        )
                    }
                }
            }
        }
    }
    if (calendar) {
        val pickerState = rememberDatePickerState(initialSelectedDateMillis = date.atStartOfDay().toInstant(ZoneOffset.UTC).toEpochMilli())
        DatePickerDialog(
            onDismissRequest = { calendar = false },
            confirmButton = {
                TextAction("Show matches", {
                    pickerState.selectedDateMillis?.let { onDate(Instant.ofEpochMilli(it).atZone(ZoneOffset.UTC).toLocalDate()) }
                    calendar = false
                })
            },
            dismissButton = { TextAction(stringResource(R.string.action_cancel), { calendar = false }, color = Pc.Text2) },
            colors = DatePickerDefaults.colors(containerColor = Pc.Surface),
        ) {
            DatePicker(
                pickerState,
                colors = DatePickerDefaults.colors(
                    containerColor = Pc.Surface, selectedDayContainerColor = Pc.Accent, selectedDayContentColor = Pc.OnAccent,
                    todayDateBorderColor = Pc.Accent, todayContentColor = Pc.Accent, dayContentColor = Pc.Text, weekdayContentColor = Pc.Text2,
                    titleContentColor = Pc.Text2, headlineContentColor = Pc.Text, navigationContentColor = Pc.Text, yearContentColor = Pc.Text,
                    selectedYearContainerColor = Pc.Accent, selectedYearContentColor = Pc.OnAccent,
                ),
            )
        }
    }
}

/** Designed empty day: names the day, and offers the next day that has matches (or today) as one tap. */
@Composable
private fun NoMatchesState(date: LocalDate, today: LocalDate, next: LocalDate?, onDate: (LocalDate) -> Unit) {
    val (label, target) = when {
        next != null -> "Jump to ${shortDay(next)}" to next
        date != today -> "Go to today" to today
        else -> null to null
    }
    EmptyState(
        noMatchesTitle(date),
        if (next != null) "The next matches in your competitions are on ${shortDay(next)}." else "Try another day, or browse a league's fixtures.",
        actionLabel = label, onAction = { target?.let(onDate) }, icon = Icons.Outlined.EventBusy,
    )
}

/** "Sun 15 Nov": the day as people say it. */
fun shortDay(d: LocalDate): String = d.format(DateTimeFormatter.ofPattern("EEE d MMM", Locale.getDefault()))
fun noMatchesTitle(d: LocalDate): String = "No matches on ${shortDay(d)}"

/**
 * A calm week bar: seven equal cells that always fit the screen (no sideways scrolling, nothing to lose track of),
 * centred on the picked day. Each cell is the short weekday over the date number; the picked day is a solid green
 * pill and today carries a small dot. Picking a far date from the calendar simply re-centres the week on it.
 */
@Composable
private fun DateStrip(date: LocalDate, today: LocalDate, onDate: (LocalDate) -> Unit) {
    val days = (-3L..3L).map { date.plusDays(it) }
    val weekday = DateTimeFormatter.ofPattern("EEE", Locale.getDefault())
    val full = DateTimeFormatter.ofPattern("EEEE d MMMM", Locale.getDefault())
    Row(
        Modifier.fillMaxWidth().padding(horizontal = PcSpace.screen).clip(PcShape.Tile).background(Pc.Surface).border(1.dp, Pc.Outline, PcShape.Tile).padding(4.dp),
        horizontalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        days.forEach { d ->
            val on = d == date
            val isToday = d == today
            Column(
                Modifier.weight(1f).heightIn(min = 58.dp).clip(PcShape.Control)
                    .background(if (on) Pc.Accent else androidx.compose.ui.graphics.Color.Transparent)
                    .pcClickable(PcShape.Control, role = Role.Tab) { onDate(d) }
                    .semantics(mergeDescendants = true) { selected = on; contentDescription = (if (isToday) "Today, " else "") + d.format(full) }
                    .padding(vertical = 6.dp),
                horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center,
            ) {
                Text(
                    (if (isToday) "Today" else d.format(weekday)).uppercase(Locale.getDefault()),
                    style = PcType.Overline.copy(fontSize = 11.sp), color = if (on) Pc.OnAccent.copy(alpha = 0.9f) else Pc.Text2, maxLines = 1,
                )
                Text(d.dayOfMonth.toString(), style = PcType.ScoreRow, color = if (on) Pc.OnAccent else Pc.Text, maxLines = 1)
                Box(Modifier.padding(top = 2.dp).size(4.dp).clip(androidx.compose.foundation.shape.CircleShape).background(if (isToday && !on) Pc.Accent else androidx.compose.ui.graphics.Color.Transparent))
            }
        }
    }
}

fun dayLabel(d: LocalDate, today: LocalDate): String = when (d) {
    today -> "Today"
    today.minusDays(1) -> "Yesterday"
    today.plusDays(1) -> "Tomorrow"
    else -> longDate(d)
}

@Preview(name = "Matches · success", showBackground = true, widthDp = 380, backgroundColor = 0xFF0E1110)
@Composable
private fun MatchesPreview() = PreviewHost(dark = true) {
    Column { MatchesContent(UiState.Success(PreviewData.matches), LocalDate.of(2026, 11, 12), LocalDate.of(2026, 11, 12), MatchFilter.All, {}, {}, {}, {}, {}, {}) }
}

@Preview(name = "Matches · empty · dark", showBackground = true, widthDp = 380, backgroundColor = 0xFF0E1110)
@Composable
private fun MatchesEmptyPreview() = PreviewHost(dark = true) {
    Column { MatchesContent(UiState.Empty, LocalDate.of(2026, 11, 15), LocalDate.of(2026, 11, 15), MatchFilter.All, {}, {}, {}, {}, {}, {}, nextMatchDay = LocalDate.of(2026, 11, 21)) }
}
