package com.innovativeinside.pitchside.ui.screens

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.EventBusy
import androidx.compose.material.icons.outlined.SportsSoccer
import androidx.compose.material.icons.outlined.VisibilityOff
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.innovativeinside.pitchside.R
import com.innovativeinside.pitchside.core.AppError
import com.innovativeinside.pitchside.core.UiState
import com.innovativeinside.pitchside.data.model.Match
import com.innovativeinside.pitchside.data.model.StatRow
import com.innovativeinside.pitchside.data.model.Status
import com.innovativeinside.pitchside.ui.LocalApp
import com.innovativeinside.pitchside.ui.LocalPrefs
import com.innovativeinside.pitchside.ui.components.AiInsightTile
import com.innovativeinside.pitchside.ui.components.AppTopBar
import com.innovativeinside.pitchside.ui.components.EmptyState
import com.innovativeinside.pitchside.ui.components.FilterChipRow
import com.innovativeinside.pitchside.ui.components.FixtureRailItem
import com.innovativeinside.pitchside.ui.components.HomeSkeleton
import com.innovativeinside.pitchside.ui.components.IconAction
import com.innovativeinside.pitchside.ui.components.LiveMatchTile
import com.innovativeinside.pitchside.ui.components.MiniLiveTile
import com.innovativeinside.pitchside.ui.components.NewsCardCompact
import com.innovativeinside.pitchside.ui.components.NewsCardLead
import com.innovativeinside.pitchside.ui.components.NextKickoffTile
import com.innovativeinside.pitchside.ui.components.PcCard
import com.innovativeinside.pitchside.ui.components.PlayerStatTile
import com.innovativeinside.pitchside.ui.components.PrimaryButton
import com.innovativeinside.pitchside.ui.components.SectionHeader
import com.innovativeinside.pitchside.ui.components.Segment
import com.innovativeinside.pitchside.ui.components.SegmentedControl
import com.innovativeinside.pitchside.ui.components.StandingsTile
import com.innovativeinside.pitchside.ui.components.StateContent
import com.innovativeinside.pitchside.ui.components.rememberReducedMotion
import com.innovativeinside.pitchside.ui.preview.PreviewData
import com.innovativeinside.pitchside.ui.preview.PreviewHost
import com.innovativeinside.pitchside.ui.theme.Pc
import com.innovativeinside.pitchside.ui.theme.PcShape
import com.innovativeinside.pitchside.ui.theme.PcSpace
import com.innovativeinside.pitchside.ui.theme.PcType
import com.innovativeinside.pitchside.ui.vm.HomeData
import com.innovativeinside.pitchside.ui.vm.HomeViewModel
import com.innovativeinside.pitchside.ui.vm.MatchFilter
import com.innovativeinside.pitchside.ui.vm.competitionsOf
import com.innovativeinside.pitchside.ui.vm.filterByCompetition
import com.innovativeinside.pitchside.ui.vm.filterMatches
import com.innovativeinside.pitchside.ui.vm.nextKickoff
import com.innovativeinside.pitchside.ui.vm.pickHero
import com.innovativeinside.pitchside.ui.vm.rankLiveMatches
import com.innovativeinside.pitchside.ui.vm.statusCounts
import java.time.LocalDate

class HomeActions(
    val openMatch: (String) -> Unit,
    val openSearch: () -> Unit,
    val openAsk: () -> Unit,
    val openNews: () -> Unit,
    val openArticle: (String) -> Unit,
    val openLeague: (String) -> Unit,
    val openTeam: (String) -> Unit,
    val openMatches: () -> Unit,
    val openNotifications: () -> Unit = {},
    val openPlayer: (String) -> Unit = {},
)

@Composable
fun HomeRoute(actions: HomeActions, vm: HomeViewModel = viewModel(factory = HomeViewModel.Factory)) {
    val state by vm.state.collectAsStateWithLifecycle()
    val refreshing by vm.refreshing.collectAsStateWithLifecycle()
    val refreshError by vm.refreshError.collectAsStateWithLifecycle()
    val stats by vm.heroStats.collectAsStateWithLifecycle()
    RefreshScreen(refreshing, vm::refresh, refreshError, vm::dismissRefreshError, maxWidth = 760) {
        HomeContent(state, actions, vm::retry, stats, vm::loadStats)
    }
}

/**
 * Home, the "Matchday Hub": top bar, the live hero (with the other live games as mini tiles), today's
 * fixture rail, the bento grid, competition chips, the Live/Upcoming/Finished match list and the latest news.
 */
@Composable
fun HomeContent(
    state: UiState<HomeData>,
    actions: HomeActions,
    onRetry: () -> Unit,
    heroStats: Map<String, List<StatRow>> = emptyMap(),
    loadStats: (String) -> Unit = {},
) {
    AppTopBar(
        "Football today", subtitle = longDate(LocalDate.now()),
        onSearch = actions.openSearch, onNotifications = actions.openNotifications,
        actions = { IconAction(Icons.Outlined.AutoAwesome, stringResource(R.string.action_ask), actions.openAsk, tint = Pc.Accent) },
    )
    StateContent(state, onRetry, Modifier.padding(horizontal = PcSpace.screen), loading = { HomeSkeleton(Modifier.padding(top = PcSpace.xs)) }) { d ->
        HomeBody(d, actions, heroStats, loadStats)
    }
}

@Composable
private fun HomeBody(d: HomeData, actions: HomeActions, heroStats: Map<String, List<StatRow>>, loadStats: (String) -> Unit) {
    val app = LocalApp.current
    val prefs = LocalPrefs.current
    val competition by app.competition.collectAsStateWithLifecycle()
    val comps = competitionsOf(d.matches, prefs.leagues)
    val selectedComp = competition?.takeIf { id -> comps.any { it.first == id } }
    val matches = filterByCompetition(d.matches, selectedComp)
    val live = matches.filter { it.status == Status.Live }

    Column(verticalArrangement = Arrangement.spacedBy(PcSpace.gutter)) {
        val hiddenNow = d.matches.filter { app.isHidden(prefs, it) }
        if (hiddenNow.isNotEmpty()) ShieldBanner(hiddenNow.size) { app.caughtUp(hiddenNow.map { it.id }) }

        // 1. LIVE NOW
        LiveSection(live, actions, heroStats, loadStats)

        // 2. TODAY rail: upcoming first, then live, then finished.
        if (matches.isNotEmpty()) {
            SectionHeader("Today", "See all", actions.openMatches)
            val order = mapOf(Status.Upcoming to 0, Status.Live to 1, Status.Postponed to 2, Status.Finished to 3)
            LazyRow(
                Modifier.fillMaxWidth().padding(horizontal = 0.dp).testTag("today_rail"),
                horizontalArrangement = Arrangement.spacedBy(PcSpace.xs),
            ) {
                items(matches.sortedWith(compareBy<Match>({ order[it.status] }, { it.kickoffMs })), key = { it.id }) { m ->
                    FixtureRailItem(m, app.isHidden(prefs, m), { actions.openMatch(m.id) })
                }
            }
        }

        // 3. BENTO
        BentoGrid(d, live, actions)

        // 4. COMPETITIONS
        if (comps.size > 1) {
            SectionHeader("Competitions")
            val labels = listOf("All") + comps.map { it.second }
            FilterChipRow(labels, comps.firstOrNull { it.first == selectedComp }?.second ?: "All", { label ->
                app.setCompetition(comps.firstOrNull { it.second == label }?.first)
            }, contentPadding = 0.dp)
        }

        // 5. MATCHES
        MatchesSection(matches, actions)

        // 6. LATEST news (lighter than the live section: no stage, plain rows)
        if (d.news.isNotEmpty()) {
            SectionHeader("Latest", "All news", actions.openNews)
            NewsCardLead(d.news.first(), { actions.openArticle(d.news.first().id) })
            PcCard(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(horizontal = PcSpace.md, vertical = PcSpace.xxs)) {
                    d.news.drop(1).take(2).forEach { n -> NewsCardCompact(n, { actions.openArticle(n.id) }) }
                }
            }
        }
    }
}

@Composable
private fun LiveSection(live: List<Match>, actions: HomeActions, heroStats: Map<String, List<StatRow>>, loadStats: (String) -> Unit) {
    val app = LocalApp.current
    val prefs = LocalPrefs.current
    val reduced = rememberReducedMotion()
    var promoted by rememberSaveable { mutableStateOf<String?>(null) }
    val ranked = rankLiveMatches(live, prefs.teams)
    val hero = pickHero(live, prefs.teams, promoted)

    SectionHeader("Live now", trailing = {
        if (live.isNotEmpty()) Row(
            Modifier.clip(PcShape.Chip).background(Pc.LiveContainer).padding(horizontal = 10.dp, vertical = 4.dp).testTag("live_count"),
            verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Box(Modifier.size(6.dp).clip(CircleShape).background(Pc.Live))
            Text("${live.size} live", style = PcType.Numeric.copy(fontWeight = FontWeight.Bold), color = Pc.LiveText)
        }
    })

    if (hero == null) {
        PcCard(Modifier.fillMaxWidth().testTag("no_live")) {
            Row(Modifier.padding(PcSpace.md), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(PcSpace.sm)) {
                Box(Modifier.size(40.dp).clip(CircleShape).background(Pc.SurfaceVariant), contentAlignment = Alignment.Center) { Icon(Icons.Outlined.SportsSoccer, null, tint = Pc.Text2) }
                Column(Modifier.weight(1f)) {
                    Text("Nothing live right now", style = PcType.BodyStrong)
                    Text("Live matches appear here at kickoff.", style = PcType.Caption)
                }
            }
        }
        return
    }

    LaunchedEffect(hero.id, hero.label) { loadStats(hero.id) }
    val rtl = LocalLayoutDirection.current == LayoutDirection.Rtl
    val currentRanked by rememberUpdatedState(ranked)
    val currentHero by rememberUpdatedState(hero)
    var drag by remember { mutableFloatStateOf(0f) }
    // Swipe left/right on the hero to step through the live matches.
    val swipe = Modifier.pointerInput(Unit) {
        detectHorizontalDragGestures(
            onDragStart = { drag = 0f },
            onDragEnd = {
                val list = currentRanked
                if (list.size > 1 && kotlin.math.abs(drag) > 80.dp.toPx()) {
                    val i = list.indexOfFirst { it.id == currentHero.id }.coerceAtLeast(0)
                    val step = if ((drag < 0) != rtl) 1 else -1
                    promoted = list[(i + step + list.size) % list.size].id
                }
            },
        ) { _, amount -> drag += amount }
    }
    AnimatedContent(
        targetState = hero,
        contentKey = { it.id },
        transitionSpec = {
            if (reduced) EnterTransition.None togetherWith ExitTransition.None
            else (fadeIn(tween(250)) + slideInHorizontally(tween(250)) { it / 10 }) togetherWith fadeOut(tween(150))
        },
        label = "hero",
    ) { m ->
        LiveMatchTile(
            m, app.isHidden(prefs, m), { actions.openMatch(m.id) }, { app.reveal(m.id) },
            modifier = swipe.testTag("live_hero"), stats = heroStats[m.id].orEmpty(),
        )
    }
    val others = ranked.filter { it.id != hero.id }
    if (others.isNotEmpty()) {
        LazyRow(Modifier.fillMaxWidth().testTag("mini_live_row"), horizontalArrangement = Arrangement.spacedBy(PcSpace.xs)) {
            items(others, key = { it.id }) { m -> MiniLiveTile(m, app.isHidden(prefs, m), { promoted = m.id }) }
        }
    }
}

/** Two-column bento. Tiles without data are left out; an odd tile spans the full row, so there are no holes. */
@Composable
private fun BentoGrid(d: HomeData, live: List<Match>, actions: HomeActions) {
    val app = LocalApp.current
    val prefs = LocalPrefs.current
    val ctx = LocalContext.current
    val next = nextKickoff(d.upcoming, prefs.teams, System.currentTimeMillis())
    val insightMatch = pickHero(live, prefs.teams, null)?.takeIf { it.brief.isNotBlank() && !app.isHidden(prefs, it) }
        ?: d.matches.firstOrNull { it.brief.isNotBlank() && it.status != Status.Finished && !app.isHidden(prefs, it) }
    val tiles = buildList<@Composable (Modifier) -> Unit> {
        if (d.table.isNotEmpty()) add { m -> StandingsTile(d.tableLeague?.name ?: "Table", d.table, prefs.teams.toSet(), { actions.openLeague(d.tableLeague?.id ?: d.table.first().team.leagueId) }, m) }
        if (next != null) add { m -> NextKickoffTile(next, next.id in prefs.matchAlerts, { app.toggleMatchAlert(next.id) }, { addMatchToCalendar(ctx, next) }, { actions.openMatch(next.id) }, m) }
        d.scorers.firstOrNull()?.let { s -> add { m -> PlayerStatTile(s, { actions.openPlayer(s.playerId) }, m) } }
        insightMatch?.let { im -> add { m -> AiInsightTile(im.brief, "${im.home.short} v ${im.away.short} · ${im.leagueName}", { actions.openMatch(im.id) }, m) } }
    }
    if (tiles.isEmpty()) return
    Column(Modifier.testTag("bento"), verticalArrangement = Arrangement.spacedBy(PcSpace.gutter)) {
        tiles.chunked(2).forEach { row ->
            Row(Modifier.fillMaxWidth().height(IntrinsicSize.Min), horizontalArrangement = Arrangement.spacedBy(PcSpace.gutter)) {
                row.forEach { tile -> tile(Modifier.weight(1f).fillMaxHeight()) }
            }
        }
    }
}

@Composable
private fun MatchesSection(matches: List<Match>, actions: HomeActions) {
    val prefs = LocalPrefs.current
    val counts = statusCounts(matches)
    val segs = listOf(MatchFilter.Live, MatchFilter.Upcoming, MatchFilter.Finished)
    var seg by rememberSaveable { mutableIntStateOf(if ((counts[MatchFilter.Live] ?: 0) > 0) 0 else 1) }
    SectionHeader("Matches", "See all", actions.openMatches)
    SegmentedControl(
        listOf(Segment("Live", counts[MatchFilter.Live], live = true), Segment("Upcoming", counts[MatchFilter.Upcoming]), Segment("Finished", counts[MatchFilter.Finished])),
        seg, { seg = it },
    )
    val shown = filterMatches(matches, segs[seg], prefs.teams, prefs.leagues)
        .sortedWith(compareBy<Match>({ if (it.involves(prefs.teams)) 0 else 1 }, { it.kickoffMs }))
    if (matches.isEmpty()) {
        EmptyState("No matches today", "Check tomorrow's fixtures or browse a league.", actionLabel = "See fixtures", onAction = actions.openMatches, icon = Icons.Outlined.EventBusy)
    } else if (shown.isEmpty()) {
        Text(
            when (segs[seg]) { MatchFilter.Live -> "Nothing live right now."; MatchFilter.Upcoming -> "No more kickoffs today."; else -> "No results yet today." },
            style = PcType.BodyMuted, modifier = Modifier.padding(vertical = PcSpace.xs),
        )
    } else {
        MatchList(shown, actions.openMatch)
    }
}

@Composable
fun ShieldBanner(count: Int, onCaughtUp: () -> Unit) {
    PcCard(Modifier.fillMaxWidth(), container = Pc.AccentContainer) {
        Column(Modifier.padding(PcSpace.md), verticalArrangement = Arrangement.spacedBy(PcSpace.sm)) {
            Row(horizontalArrangement = Arrangement.spacedBy(PcSpace.sm), verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Outlined.VisibilityOff, null, tint = Pc.Accent)
                Column(Modifier.weight(1f).semantics(mergeDescendants = true) { contentDescription = "Spoiler Shield is on. $count scores hidden." }) {
                    Text("Spoiler Shield is on", style = PcType.BodyStrong)
                    Text("$count ${if (count == 1) "score is" else "scores are"} hidden. Alerts wait until you're caught up.", style = PcType.Caption)
                }
            }
            PrimaryButton("I'm caught up", onCaughtUp, Modifier.fillMaxWidth().heightIn(min = 48.dp))
        }
    }
}

// ---------------- Previews ----------------

private val previewActions = HomeActions({}, {}, {}, {}, {}, {}, {}, {})

@Preview(name = "Home · light", showBackground = true, widthDp = 380, heightDp = 1800)
@Composable
private fun HomeLightPreview() = PreviewHost(dark = false) { Column { HomeContent(UiState.Success(PreviewData.home), previewActions, {}) } }

@Preview(name = "Home · dark", showBackground = true, widthDp = 380, heightDp = 1800, backgroundColor = 0xFF0C0F0D)
@Composable
private fun HomeDarkPreview() = PreviewHost(dark = true) { Column { HomeContent(UiState.Success(PreviewData.home), previewActions, {}) } }

@Preview(name = "Home · no live", showBackground = true, widthDp = 360, heightDp = 1200)
@Composable
private fun HomeNoLivePreview() = PreviewHost(dark = false) {
    Column { HomeContent(UiState.Success(PreviewData.home.copy(matches = PreviewData.matches.filter { it.status != Status.Live })), previewActions, {}) }
}

@Preview(name = "Home · loading", showBackground = true, widthDp = 360)
@Composable
private fun HomeLoadingPreview() = PreviewHost(dark = false) { Column { HomeContent(UiState.Loading, previewActions, {}) } }

@Preview(name = "Home · error", showBackground = true, widthDp = 360)
@Composable
private fun HomeErrorPreview() = PreviewHost(dark = false) { Column { HomeContent(UiState.Error(AppError.Network()), previewActions, {}) } }
