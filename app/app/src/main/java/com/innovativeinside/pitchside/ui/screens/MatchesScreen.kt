package com.innovativeinside.pitchside.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
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
import com.innovativeinside.pitchside.ui.components.Segment
import com.innovativeinside.pitchside.ui.components.SegmentedControl
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
    var applied by rememberSaveable { mutableStateOf(false) }
    if (!applied && initialFilter != null) { vm.setFilter(initialFilter); applied = true }
    RefreshScreen(refreshing, vm::refresh, refreshError, vm::dismissRefreshError) {
        MatchesContent(state, date, vm.today, filter, vm::selectDate, vm::setFilter, vm::retry, openMatch, openLeague, openSearch, myTeams, vm::setMyTeamsOnly)
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
) {
    val app = LocalApp.current
    val prefs = LocalPrefs.current
    val competition by app.competition.collectAsStateWithLifecycle()
    var calendar by rememberSaveable { mutableStateOf(false) }
    AppTopBar("Matches", subtitle = dayLabel(date, today), onSearch = openSearch, actions = {
        IconAction(Icons.Outlined.CalendarMonth, "Pick a date", { calendar = true })
    })
    DateStrip(date, today, onDate)
    Column(Modifier.padding(horizontal = PcSpace.screen, vertical = PcSpace.sm), verticalArrangement = Arrangement.spacedBy(PcSpace.sm)) {
        if (date !in (-3L..3L).map { today.plusDays(it) }) TextAction("Back to today", { onDate(today) })
        val all = (state as? UiState.Success)?.data.orEmpty()
        val comps = competitionsOf(all, prefs.leagues)
        val selectedComp = competition?.takeIf { id -> comps.any { it.first == id } }
        Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(PcSpace.xs)) {
            Chip("My teams", myTeams, { onMyTeams(!myTeams) }, leading = Icons.Outlined.Star)
            Chip("All", selectedComp == null, { app.setCompetition(null) })
            comps.forEach { (id, name) -> Chip(name, selectedComp == id, { app.setCompetition(id) }) }
        }
        val byComp = filterByCompetition(all, selectedComp).let { if (myTeams) filterMatches(it, MatchFilter.MyTeams, prefs.teams, prefs.leagues) else it }
        val counts = segmentFilters.associateWith { f -> filterMatches(byComp, f, prefs.teams, prefs.leagues).size }
        val segIndex = segmentFilters.indexOf(filter).coerceAtLeast(0)
        SegmentedControl(
            listOf(Segment("All", counts[MatchFilter.All]), Segment("Live", counts[MatchFilter.Live], live = true), Segment("Upcoming", counts[MatchFilter.Upcoming]), Segment("Finished", counts[MatchFilter.Finished])),
            segIndex, { onFilter(segmentFilters[it]) },
        )
        StateContent(
            state, onRetry,
            empty = { EmptyState("No matches on ${dayLabel(date, today)}", "Try another day, or browse a league's fixtures.", actionLabel = if (date != today) "Go to today" else null, onAction = { onDate(today) }, icon = Icons.Outlined.EventBusy) },
        ) { _ ->
            val shown = filterMatches(byComp, segmentFilters[segIndex], prefs.teams, prefs.leagues)
            Column(verticalArrangement = Arrangement.spacedBy(PcSpace.sm)) {
                if (shown.isEmpty()) {
                    EmptyState(
                        when { myTeams -> "None of your teams or leagues play"; filter == MatchFilter.Live -> "Nothing live right now"; else -> "No matches here" },
                        when { myTeams -> "Follow more teams from Search, or show every match."; filter == MatchFilter.Live -> "Live games appear here at kickoff. Alerts tell you when your teams start."; else -> "Try another filter or day." },
                        actionLabel = "Show all matches", onAction = { onFilter(MatchFilter.All); onMyTeams(false); app.setCompetition(null) }, icon = Icons.Outlined.EventBusy,
                    )
                } else groupByLeague(shown, prefs.leagues).forEach { (name, ms) ->
                    LeagueGroup(name, ms, openMatch, openLeague = { openLeague(ms.first().leagueId) })
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

/** Seven days around today; yesterday, today and tomorrow are named. Scrolls to keep the selection visible. */
@Composable
private fun DateStrip(date: LocalDate, today: LocalDate, onDate: (LocalDate) -> Unit) {
    val days = (-3L..3L).map { today.plusDays(it) }
    val listState = rememberLazyListState(initialFirstVisibleItemIndex = (days.indexOf(date).takeIf { it >= 0 } ?: 3).minus(1).coerceAtLeast(0))
    val dayName = DateTimeFormatter.ofPattern("EEE", Locale.getDefault())
    val dayNum = DateTimeFormatter.ofPattern("d MMM", Locale.getDefault())
    LazyRow(state = listState, contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = PcSpace.screen), horizontalArrangement = Arrangement.spacedBy(PcSpace.xs)) {
        items(days, key = { it.toString() }) { d ->
            val on = d == date
            val name = when (d) { today -> "Today"; today.minusDays(1) -> "Yesterday"; today.plusDays(1) -> "Tomorrow"; else -> d.format(dayName) }
            Column(
                Modifier.widthIn(min = 72.dp).heightIn(min = 52.dp).pcClickable(PcShape.Control, role = Role.Tab) { onDate(d) }
                    .background(if (on) Pc.Accent else Pc.Surface, PcShape.Control).border(1.dp, if (on) Pc.Accent else Pc.Outline, PcShape.Control)
                    .semantics(mergeDescendants = true) { selected = on; contentDescription = "$name, ${d.format(dayNum)}" }
                    .padding(horizontal = PcSpace.sm, vertical = 6.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(name, style = PcType.LabelStrong, color = if (on) Pc.OnAccent else Pc.Text, maxLines = 1)
                Text(d.format(dayNum), style = PcType.Caption.copy(fontWeight = FontWeight.Medium), color = if (on) Pc.OnAccent else Pc.Text2, maxLines = 1)
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

@Preview(name = "Matches · success", showBackground = true, widthDp = 380)
@Composable
private fun MatchesPreview() = PreviewHost(dark = false) {
    Column { MatchesContent(UiState.Success(PreviewData.matches), LocalDate.of(2026, 11, 12), LocalDate.of(2026, 11, 12), MatchFilter.All, {}, {}, {}, {}, {}, {}) }
}

@Preview(name = "Matches · empty · dark", showBackground = true, widthDp = 380, backgroundColor = 0xFF0C0F0D)
@Composable
private fun MatchesEmptyPreview() = PreviewHost(dark = true) {
    Column { MatchesContent(UiState.Empty, LocalDate.of(2026, 11, 30), LocalDate.of(2026, 11, 12), MatchFilter.All, {}, {}, {}, {}, {}, {}) }
}
