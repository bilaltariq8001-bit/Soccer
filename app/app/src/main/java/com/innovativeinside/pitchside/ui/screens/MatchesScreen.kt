package com.innovativeinside.pitchside.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.EventBusy
import androidx.compose.material.icons.outlined.Search
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.innovativeinside.pitchside.R
import com.innovativeinside.pitchside.core.UiState
import com.innovativeinside.pitchside.data.model.Match
import com.innovativeinside.pitchside.ui.LocalPrefs
import com.innovativeinside.pitchside.ui.components.Chip
import com.innovativeinside.pitchside.ui.components.EmptyState
import com.innovativeinside.pitchside.ui.components.IconAction
import com.innovativeinside.pitchside.ui.components.SegmentedTabs
import com.innovativeinside.pitchside.ui.components.StateContent
import com.innovativeinside.pitchside.ui.components.TextAction
import com.innovativeinside.pitchside.ui.preview.PreviewData
import com.innovativeinside.pitchside.ui.preview.PreviewHost
import com.innovativeinside.pitchside.ui.theme.Pc
import com.innovativeinside.pitchside.ui.theme.PcType
import com.innovativeinside.pitchside.ui.vm.MatchFilter
import com.innovativeinside.pitchside.ui.vm.MatchesViewModel
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
    val refreshing by vm.refreshing.collectAsStateWithLifecycle()
    val refreshError by vm.refreshError.collectAsStateWithLifecycle()
    var applied by rememberSaveable { mutableStateOf(false) }
    if (!applied && initialFilter != null) { vm.setFilter(initialFilter); applied = true }
    RefreshScreen(refreshing, vm::refresh, refreshError, vm::dismissRefreshError) {
        MatchesContent(state, date, vm.today, filter, vm::selectDate, vm::setFilter, vm::retry, openMatch, openLeague, openSearch)
    }
}

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
) {
    val prefs = LocalPrefs.current
    var calendar by rememberSaveable { mutableStateOf(false) }
    Column(Modifier.padding(horizontal = 16.dp, vertical = 16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text("Matches", style = PcType.Title.copy(fontSize = 30.sp, lineHeight = 36.sp))
                Text(dayLabel(date, today), style = PcType.BodyMuted)
            }
            IconAction(Icons.Outlined.Search, stringResource(R.string.action_search), openSearch, filled = true)
            Spacer(Modifier.width(8.dp))
            IconAction(Icons.Outlined.CalendarMonth, "Pick a date", { calendar = true }, filled = true)
        }
        val days = listOf(today.minusDays(1), today, today.plusDays(1))
        val idx = days.indexOf(date)
        SegmentedTabs(
            listOf("Yesterday", "Today", "Tomorrow").let { if (idx == -1) it else it },
            if (idx == -1) -1 else idx,
            { onDate(days[it]) },
        )
        if (idx == -1) TextAction("Back to today", { onDate(today) })
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Chip("All", filter == MatchFilter.All, { onFilter(MatchFilter.All) })
            Chip("Live", filter == MatchFilter.Live, { onFilter(MatchFilter.Live) })
            Chip("My teams", filter == MatchFilter.MyTeams, { onFilter(MatchFilter.MyTeams) })
        }
        StateContent(
            state, onRetry,
            empty = { EmptyState("No matches on ${dayLabel(date, today)}", "Try another day, or browse a league's fixtures.", actionLabel = if (date != today) "Go to today" else null, onAction = { onDate(today) }, icon = Icons.Outlined.EventBusy) },
        ) { all ->
            val shown = filterMatches(all, filter, prefs.teams, prefs.leagues)
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                if (shown.isEmpty()) {
                    EmptyState(
                        when (filter) { MatchFilter.Live -> "Nothing live right now"; MatchFilter.MyTeams -> "None of your teams or leagues play"; else -> "No matches" },
                        when (filter) { MatchFilter.Live -> "Live games appear here at kickoff. Alerts tell you when your teams start."; else -> "Follow more teams from Search, or show every match." },
                        actionLabel = "Show all matches", onAction = { onFilter(MatchFilter.All) }, icon = Icons.Outlined.EventBusy,
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
                ),
            )
        }
    }
}

private fun longDay() = DateTimeFormatter.ofPattern("EEEE d MMMM", Locale.getDefault())
fun dayLabel(d: LocalDate, today: LocalDate): String = when (d) {
    today -> "Today"
    today.minusDays(1) -> "Yesterday"
    today.plusDays(1) -> "Tomorrow"
    else -> d.format(longDay())
}

@Preview(name = "Matches · success", showBackground = true, backgroundColor = 0xFF07111F)
@Composable
private fun MatchesPreview() = PreviewHost {
    Column { MatchesContent(UiState.Success(PreviewData.matches), LocalDate.of(2026, 11, 12), LocalDate.of(2026, 11, 12), MatchFilter.All, {}, {}, {}, {}, {}, {}) }
}

@Preview(name = "Matches · empty", showBackground = true, backgroundColor = 0xFF07111F)
@Composable
private fun MatchesEmptyPreview() = PreviewHost {
    Column { MatchesContent(UiState.Empty, LocalDate.of(2026, 11, 30), LocalDate.of(2026, 11, 12), MatchFilter.All, {}, {}, {}, {}, {}, {}) }
}
