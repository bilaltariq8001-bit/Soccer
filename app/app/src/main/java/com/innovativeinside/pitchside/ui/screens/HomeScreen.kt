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
import androidx.compose.material.icons.outlined.QuestionAnswer
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
import com.innovativeinside.pitchside.ui.vm.CompetitionCount
import com.innovativeinside.pitchside.ui.vm.countsByCompetition
import com.innovativeinside.pitchside.ui.vm.inForm
import com.innovativeinside.pitchside.ui.vm.pickSpotlight
import com.innovativeinside.pitchside.data.model.FormText
import androidx.compose.foundation.layout.width
import androidx.compose.ui.text.style.TextAlign
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
        actions = { IconAction(Icons.Outlined.QuestionAnswer, stringResource(R.string.action_ask), actions.openAsk, tint = Pc.Accent) },
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
        LiveSection(live, actions, heroStats, loadStats, d)

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
private fun LiveSection(live: List<Match>, actions: HomeActions, heroStats: Map<String, List<StatRow>>, loadStats: (String) -> Unit, d: HomeData) {
    val app = LocalApp.current
    val prefs = LocalPrefs.current
    val reduced = rememberReducedMotion()
    var promoted by rememberSaveable { mutableStateOf<String?>(null) }
    val ranked = rankLiveMatches(live, prefs.teams)
    val hero = pickHero(live, prefs.teams, promoted)

    if (hero == null) { SpotlightSection(d, actions); return }

    SectionHeader("Live now", trailing = {
        if (live.isNotEmpty()) Row(
            Modifier.clip(PcShape.Chip).background(Pc.LiveContainer).padding(horizontal = 10.dp, vertical = 4.dp).testTag("live_count"),
            verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Box(Modifier.size(6.dp).clip(CircleShape).background(Pc.Live))
            Text("${live.size} live", style = PcType.Numeric.copy(fontWeight = FontWeight.Bold), color = Pc.LiveText)
        }
    })


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

/**
 * Nothing live: a compact "nothing live" line, then the spotlight match (a followed team's next game, else the biggest
 * game coming up by table position) on the same hero stage, with its win probability and a kickoff countdown.
 */
@Composable
private fun SpotlightSection(d: HomeData, actions: HomeActions) {
    val app = LocalApp.current
    val prefs = LocalPrefs.current
    var now by remember { androidx.compose.runtime.mutableLongStateOf(System.currentTimeMillis()) }
    LaunchedEffect(Unit) { while (true) { kotlinx.coroutines.delay(30_000); now = System.currentTimeMillis() } }
    val spot = pickSpotlight(d.upcoming, d.matches, prefs.teams, d.tables, now)
    SectionHeader(if (spot?.status == Status.Finished) "Result of the day" else "Match spotlight", trailing = {
        Row(
            Modifier.clip(PcShape.Chip).background(Pc.SurfaceVariant).padding(horizontal = 10.dp, vertical = 4.dp).testTag("no_live"),
            verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Box(Modifier.size(6.dp).clip(CircleShape).background(Pc.Text3))
            Text("Nothing live right now", style = PcType.Caption)
        }
    })
    if (spot == null) {
        PcCard(Modifier.fillMaxWidth()) {
            Row(Modifier.padding(PcSpace.md), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(PcSpace.sm)) {
                Box(Modifier.size(40.dp).clip(CircleShape).background(Pc.SurfaceVariant), contentAlignment = Alignment.Center) { Icon(Icons.Outlined.SportsSoccer, null, tint = Pc.Text2) }
                Column(Modifier.weight(1f)) {
                    Text("No fixtures in the next few days", style = PcType.BodyStrong)
                    Text("Live matches take over this spot at kickoff.", style = PcType.Caption)
                }
            }
        }
        return
    }
    val table = d.tables[spot.leagueId].orEmpty()
    val hr = table.firstOrNull { it.team.id == spot.home.id }
    val ar = table.firstOrNull { it.team.id == spot.away.id }
    val upcoming = spot.status == Status.Upcoming
    LiveMatchTile(
        spot, app.isHidden(prefs, spot), { actions.openMatch(spot.id) }, { app.reveal(spot.id) },
        modifier = Modifier.testTag("spotlight"),
        topRight = if (upcoming) formatDay(spot.kickoffMs) else null,
    ) {
        if (hr != null || ar != null) Row(Modifier.fillMaxWidth()) {
            Text(hr?.let { "${FormText.ordinal(it.rank)} · ${it.points} pts" }.orEmpty(), style = PcType.Caption, color = Pc.OnHero2, modifier = Modifier.weight(1f), textAlign = TextAlign.Center)
            Spacer(Modifier.width(112.dp))
            Text(ar?.let { "${FormText.ordinal(it.rank)} · ${it.points} pts" }.orEmpty(), style = PcType.Caption, color = Pc.OnHero2, modifier = Modifier.weight(1f), textAlign = TextAlign.Center)
        }
        val p = spot.probs
        if (upcoming && p != null) {
            Column(Modifier.fillMaxWidth().clip(PcShape.Control).background(Pc.HeroRaised).padding(horizontal = PcSpace.sm, vertical = PcSpace.xs), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Row(Modifier.fillMaxWidth()) {
                    Text("${p.home}%", style = PcType.Numeric.copy(fontWeight = FontWeight.Bold), color = Pc.OnHero, modifier = Modifier.weight(1f))
                    Text("DRAW ${p.draw}%", style = PcType.Overline, color = Pc.OnHero2, modifier = Modifier.weight(1f), textAlign = TextAlign.Center)
                    Text("${p.away}%", style = PcType.Numeric.copy(fontWeight = FontWeight.Bold), color = Pc.OnHero, modifier = Modifier.weight(1f), textAlign = TextAlign.End)
                }
                com.innovativeinside.pitchside.ui.components.ProbabilityBar(p, Modifier.fillMaxWidth())
            }
        }
        if (upcoming) Text("Kicks off in ${com.innovativeinside.pitchside.ui.components.countdown(now, spot.kickoffMs)}", style = PcType.LabelStrong, color = Pc.OnHero2)
    }
}

/** Two-column bento. Tiles without data are left out; an odd tile spans the full row, so there are no holes. */
@Composable
private fun BentoGrid(d: HomeData, live: List<Match>, actions: HomeActions) {
    val app = LocalApp.current
    val prefs = LocalPrefs.current
    val ctx = LocalContext.current
    val next = nextKickoff(d.upcoming, prefs.teams, System.currentTimeMillis())
    val tiles = buildList<@Composable (Modifier) -> Unit> {
        if (d.table.isNotEmpty()) add { m -> StandingsTile(d.tableLeague?.name ?: "Table", d.table, prefs.teams.toSet(), { actions.openLeague(d.tableLeague?.id ?: d.table.first().team.leagueId) }, m) }
        if (next != null) add { m -> NextKickoffTile(next, next.id in prefs.matchAlerts, { app.toggleMatchAlert(next.id) }, { addMatchToCalendar(ctx, next) }, { actions.openMatch(next.id) }, m) }
        d.scorers.firstOrNull()?.let { s -> add { m -> PlayerStatTile(s, { actions.openPlayer(s.playerId) }, m) } }
        val perComp = countsByCompetition(d.matches)
        if (perComp.isNotEmpty()) add { m -> TodayByCompetitionTile(perComp, actions.openLeague, actions.openMatches, m) }
        val hot = inForm(d.tables)
        if (hot.isNotEmpty()) add { m -> FormGuideTile(hot, actions.openTeam, m) }
        if (d.matches.isNotEmpty()) add { m -> TodayNumbersTile(d.matches, actions.openMatches, m) }
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

/** How many games each competition has today, with its logo; busiest first. */
@Composable
private fun TodayByCompetitionTile(rows: List<CompetitionCount>, openLeague: (String) -> Unit, openMatches: () -> Unit, modifier: Modifier) {
    com.innovativeinside.pitchside.ui.components.BentoTile(
        "Today by competition", modifier, openMatches,
        description = "Today by competition: " + rows.joinToString("; ") { "${it.name}, ${it.count} matches" },
    ) {
        rows.take(4).forEach { c ->
            Row(Modifier.fillMaxWidth().heightIn(min = 28.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                LeagueBadge(c.name.split(" ").filter { it.isNotBlank() }.take(2).joinToString("") { it.first().uppercase() }, 22, c.logo)
                Text(c.name, style = PcType.Label, maxLines = 1, overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis, modifier = Modifier.weight(1f))
                if (c.live > 0) Box(Modifier.size(6.dp).clip(CircleShape).background(Pc.Live))
                Text(c.count.toString(), style = PcType.Numeric.copy(fontWeight = FontWeight.Bold))
            }
        }
        if (rows.size > 4) Text("+${rows.size - 4} more", style = PcType.Caption)
    }
}

/** Sides with the best last-five record across the covered tables. */
@Composable
private fun FormGuideTile(rows: List<com.innovativeinside.pitchside.data.model.StandingRow>, openTeam: (String) -> Unit, modifier: Modifier) {
    com.innovativeinside.pitchside.ui.components.BentoTile(
        "In form", modifier, { openTeam(rows.first().team.id) },
        description = "Teams in form: " + rows.joinToString("; ") { "${it.team.name}, ${it.form.toList().joinToString(" ")}" },
    ) {
        rows.forEach { r ->
            Row(Modifier.fillMaxWidth().heightIn(min = 30.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                com.innovativeinside.pitchside.ui.components.TeamCrest(r.team, 20.dp)
                Text(r.team.short, style = PcType.LabelStrong, maxLines = 1, modifier = Modifier.weight(1f))
                com.innovativeinside.pitchside.ui.components.FormDots(r.form.takeLast(5), size = 14.dp)
            }
        }
        Text("Last five league games", style = PcType.Caption)
    }
}

/** Today's matchday in numbers: games, goals so far, finished and still to come. */
@Composable
private fun TodayNumbersTile(matches: List<Match>, openMatches: () -> Unit, modifier: Modifier) {
    val goals = matches.filter { it.status != Status.Upcoming }.sumOf { (it.homeScore ?: 0) + (it.awayScore ?: 0) }
    val done = matches.count { it.status == Status.Finished }
    val toCome = matches.count { it.status == Status.Upcoming }
    com.innovativeinside.pitchside.ui.components.BentoTile(
        "Matchday", modifier, openMatches,
        description = "Today: ${matches.size} matches, $goals goals, $done finished, $toCome to come",
    ) {
        Row(verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(goals.toString(), style = PcType.ScoreCompact)
            Text("GOALS", style = PcType.Overline, modifier = Modifier.padding(bottom = 5.dp))
        }
        Text("${matches.size} matches today", style = PcType.LabelStrong)
        Text(listOfNotNull("$done finished".takeIf { done > 0 }, "$toCome to come".takeIf { toCome > 0 }).joinToString(" · ").ifBlank { "All under way" }, style = PcType.Caption)
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
