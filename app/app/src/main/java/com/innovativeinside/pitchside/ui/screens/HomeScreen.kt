package com.innovativeinside.pitchside.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.EventBusy
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.VisibilityOff
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.innovativeinside.pitchside.R
import com.innovativeinside.pitchside.core.AppError
import com.innovativeinside.pitchside.core.UiState
import com.innovativeinside.pitchside.data.model.League
import com.innovativeinside.pitchside.data.model.Match
import com.innovativeinside.pitchside.data.model.Status
import com.innovativeinside.pitchside.ui.LocalApp
import com.innovativeinside.pitchside.ui.LocalPrefs
import com.innovativeinside.pitchside.ui.components.Crest
import com.innovativeinside.pitchside.ui.components.EmptyState
import com.innovativeinside.pitchside.ui.components.IconAction
import com.innovativeinside.pitchside.ui.components.PcCard
import com.innovativeinside.pitchside.ui.components.PrimaryButton
import com.innovativeinside.pitchside.ui.components.SectionHeader
import com.innovativeinside.pitchside.ui.components.StateContent
import com.innovativeinside.pitchside.ui.components.StatusBadge
import com.innovativeinside.pitchside.ui.components.pcClickable
import com.innovativeinside.pitchside.ui.preview.PreviewData
import com.innovativeinside.pitchside.ui.preview.PreviewHost
import com.innovativeinside.pitchside.ui.theme.Pc
import com.innovativeinside.pitchside.ui.theme.PcShape
import com.innovativeinside.pitchside.ui.theme.PcType
import com.innovativeinside.pitchside.ui.vm.HomeData
import com.innovativeinside.pitchside.ui.vm.HomeViewModel
import com.innovativeinside.pitchside.ui.vm.groupByLeague

class HomeActions(
    val openMatch: (String) -> Unit,
    val openSearch: () -> Unit,
    val openAsk: () -> Unit,
    val openNews: () -> Unit,
    val openArticle: (String) -> Unit,
    val openLeague: (String) -> Unit,
    val openTeam: (String) -> Unit,
    val openMatches: () -> Unit,
)

@Composable
fun HomeRoute(actions: HomeActions, vm: HomeViewModel = viewModel(factory = HomeViewModel.Factory)) {
    val state by vm.state.collectAsStateWithLifecycle()
    val refreshing by vm.refreshing.collectAsStateWithLifecycle()
    val refreshError by vm.refreshError.collectAsStateWithLifecycle()
    RefreshScreen(refreshing, vm::refresh, refreshError, vm::dismissRefreshError, maxWidth = if (widthClass() != WidthClass.Compact) 1100 else 720) {
        HomeContent(state, actions, vm::retry)
    }
}

@Composable
fun HomeContent(state: UiState<HomeData>, actions: HomeActions, onRetry: () -> Unit) {
    val data = (state as? UiState.Success)?.data
    Row(Modifier.fillMaxWidth().padding(start = 16.dp, end = 8.dp, top = 16.dp), verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            Text(greeting(), style = PcType.Title.copy(fontSize = 28.sp, lineHeight = 34.sp))
            Text(
                if (data != null) "${data.live.size} live now · ${data.matches.size} matches today" else "Today's football",
                style = PcType.BodySmall,
            )
        }
        IconAction(Icons.Outlined.AutoAwesome, stringResource(R.string.action_ask), actions.openAsk, tint = Pc.Lime, filled = true)
        Spacer(Modifier.width(4.dp))
        IconAction(Icons.Outlined.Search, stringResource(R.string.action_search), actions.openSearch, filled = true)
    }
    Spacer(Modifier.height(16.dp))
    StateContent(state, onRetry, Modifier.padding(horizontal = 16.dp)) { d ->
        if (widthClass() != WidthClass.Compact) {
            Row(horizontalArrangement = Arrangement.spacedBy(24.dp)) {
                Column(Modifier.weight(1.6f), verticalArrangement = Arrangement.spacedBy(16.dp)) { MainColumn(d, actions) }
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(12.dp)) { SideColumn(d, actions) }
            }
        } else {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) { MainColumn(d, actions); SideColumn(d, actions) }
        }
    }
}

@Composable
private fun MainColumn(d: HomeData, actions: HomeActions) {
    val app = LocalApp.current
    val prefs = LocalPrefs.current
    if (d.leagues.isNotEmpty()) TopLeaguesRow(d.leagues, prefs.leagues, actions.openLeague)
    val hiddenNow = d.matches.filter { app.isHidden(prefs, it) }
    if (hiddenNow.isNotEmpty()) ShieldBanner(hiddenNow.size) { app.caughtUp(hiddenNow.map { it.id }) }

    val featured = d.live.firstOrNull { it.involves(prefs.teams) } ?: d.live.firstOrNull()
        ?: d.matches.firstOrNull { it.status == Status.Upcoming && it.involves(prefs.teams) }
    if (featured != null) HeroMatch(featured, app.isHidden(prefs, featured), { actions.openMatch(featured.id) }, { app.reveal(featured.id) })

    val mine = d.matches.filter { it.involves(prefs.teams) && it.id != featured?.id }
    if (mine.isNotEmpty()) {
        SectionHeader("Your teams")
        LeagueGroup("Following", mine, actions.openMatch)
    }

    SectionHeader("Today", "All matches", actions.openMatches)
    val rest = d.matches.filter { it.id != featured?.id && it !in mine }
    if (d.matches.isEmpty()) {
        EmptyState("No matches today", "Check tomorrow's fixtures or browse a league.", actionLabel = "See fixtures", onAction = actions.openMatches, icon = Icons.Outlined.EventBusy)
    } else if (rest.isEmpty()) {
        Text("That's every match today.", style = PcType.BodySmall)
    } else {
        groupByLeague(rest, prefs.leagues).forEach { (name, ms) ->
            LeagueGroup(name, ms, actions.openMatch, openLeague = { actions.openLeague(ms.first().leagueId) })
        }
    }
}

@Composable
private fun SideColumn(d: HomeData, actions: HomeActions) {
    if (d.news.isNotEmpty()) {
        SectionHeader("Latest news", "All news", actions.openNews)
        d.news.forEach { NewsRow(it, { actions.openArticle(it.id) }) }
    }
    if (d.table.isNotEmpty()) {
        SectionHeader("Premier Division", "Full table", { actions.openLeague("pd") })
        StandingsTable(d.table.take(5), actions.openTeam, compact = true)
    }
}

@Composable
private fun TopLeaguesRow(leagues: List<League>, followed: List<String>, open: (String) -> Unit) {
    Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        leagues.sortedBy { if (it.id in followed) 0 else 1 }.forEach { l ->
            Row(
                Modifier.heightIn(min = 44.dp).clip(PcShape.Control).background(Pc.Surface).border(1.dp, if (l.id in followed) Pc.Lime.copy(alpha = 0.6f) else Pc.Border, PcShape.Control)
                    .pcClickable(PcShape.Control, "Open ${l.name}") { open(l.id) }.padding(horizontal = 12.dp),
                verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Box(Modifier.size(26.dp).clip(PcShape.Badge).background(Pc.Raised), contentAlignment = Alignment.Center) { Text(l.short, color = Pc.Lime, fontWeight = FontWeight.ExtraBold, fontSize = 11.sp) }
                Text(l.name, style = PcType.Label, maxLines = 1)
            }
        }
    }
}

@Composable
fun ShieldBanner(count: Int, onCaughtUp: () -> Unit) {
    PcCard(Modifier.fillMaxWidth(), container = Pc.LimeTintSoft, border = Pc.Lime.copy(alpha = 0.45f)) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.size(40.dp).clip(PcShape.Control).background(Pc.Raised), contentAlignment = Alignment.Center) { Icon(Icons.Outlined.VisibilityOff, null, tint = Pc.Lime) }
                Column {
                    Text("Spoiler Shield is on", style = PcType.Subhead)
                    Text("$count ${if (count == 1) "score is" else "scores are"} hidden. Alerts are held until you're caught up.", style = PcType.BodySmall.copy(fontSize = 13.sp, lineHeight = 18.sp))
                }
            }
            PrimaryButton("I'm caught up", onCaughtUp, Modifier.fillMaxWidth())
        }
    }
}

@Composable
fun HeroMatch(match: Match, hidden: Boolean, onClick: () -> Unit, onReveal: () -> Unit, modifier: Modifier = Modifier) {
    val desc = "Featured match: ${match.home.name} versus ${match.away.name}, ${match.label}" +
        if (hidden) ", score hidden" else if (match.hasScore) ", ${match.homeScore} to ${match.awayScore}" else ""
    PcCard(modifier.fillMaxWidth().semantics(mergeDescendants = true) { contentDescription = desc }, shape = PcShape.Hero, onClick = onClick) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text("${match.leagueName} · ${match.venue}", style = PcType.BodySmall.copy(fontSize = 13.sp), modifier = Modifier.weight(1f), maxLines = 1, overflow = TextOverflow.Ellipsis)
                StatusBadge(match.status, match.label)
            }
            Row(Modifier.fillMaxWidth().padding(vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Crest(match.home, 56.dp); Text(match.home.name, style = PcType.Label.copy(fontSize = 15.sp, fontWeight = FontWeight.ExtraBold), maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
                if (hidden) Box(Modifier.size(width = 104.dp, height = 56.dp).clip(PcShape.Control).background(Pc.Raised).pcClickable(PcShape.Control, "Reveal score", onClick = onReveal), contentAlignment = Alignment.Center) {
                    Text("Reveal", style = PcType.Label.copy(color = Pc.Lime))
                } else Text(if (match.hasScore) "${match.homeScore} – ${match.awayScore}" else match.label, style = PcType.ScoreXL.copy(fontSize = 56.sp, lineHeight = 56.sp))
                Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Crest(match.away, 56.dp); Text(match.away.name, style = PcType.Label.copy(fontSize = 15.sp, color = Pc.Text2), maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
            }
            if (match.brief.isNotBlank()) {
                Box(Modifier.fillMaxWidth().height(1.dp).background(Pc.Border))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.Top) {
                    Icon(Icons.Outlined.AutoAwesome, null, tint = Pc.Lime, modifier = Modifier.size(18.dp).padding(top = 2.dp))
                    Text(if (hidden) "Brief hidden to avoid spoilers." else match.brief, style = PcType.BodySmall.copy(color = Pc.Text), modifier = Modifier.weight(1f), maxLines = 3, overflow = TextOverflow.Ellipsis)
                }
            }
        }
    }
}

// ---------------- Previews ----------------

private val previewActions = HomeActions({}, {}, {}, {}, {}, {}, {}, {})

@Preview(name = "Home · success", showBackground = true, backgroundColor = 0xFF07111F)
@Composable
private fun HomeSuccessPreview() = PreviewHost { Column { HomeContent(UiState.Success(PreviewData.home), previewActions) {} } }

@Preview(name = "Home · loading", showBackground = true, backgroundColor = 0xFF07111F)
@Composable
private fun HomeLoadingPreview() = PreviewHost { Column { HomeContent(UiState.Loading, previewActions) {} } }

@Preview(name = "Home · error", showBackground = true, backgroundColor = 0xFF07111F)
@Composable
private fun HomeErrorPreview() = PreviewHost { Column { HomeContent(UiState.Error(AppError.Network()), previewActions) {} } }

@Preview(name = "Home · light", showBackground = true)
@Composable
private fun HomeLightPreview() = PreviewHost(dark = false) { Column { HomeContent(UiState.Success(PreviewData.home.copy(matches = emptyList())), previewActions) {} } }
