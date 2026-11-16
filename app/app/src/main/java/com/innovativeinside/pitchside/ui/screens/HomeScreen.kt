package com.innovativeinside.pitchside.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.EventBusy
import androidx.compose.material.icons.outlined.QuestionAnswer
import androidx.compose.material.icons.outlined.SportsSoccer
import androidx.compose.material.icons.outlined.VisibilityOff
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.innovativeinside.pitchside.R
import com.innovativeinside.pitchside.core.AppError
import com.innovativeinside.pitchside.core.UiState
import com.innovativeinside.pitchside.data.model.FormText
import com.innovativeinside.pitchside.data.model.Match
import com.innovativeinside.pitchside.data.model.StatRow
import com.innovativeinside.pitchside.data.model.Status
import com.innovativeinside.pitchside.ui.LocalApp
import com.innovativeinside.pitchside.ui.LocalPrefs
import com.innovativeinside.pitchside.ui.components.BentoTile
import com.innovativeinside.pitchside.ui.components.EmptyState
import com.innovativeinside.pitchside.ui.components.FilterChipRow
import com.innovativeinside.pitchside.ui.components.FormPills
import com.innovativeinside.pitchside.ui.components.Hairline
import com.innovativeinside.pitchside.ui.components.HighlightRow
import com.innovativeinside.pitchside.ui.components.HomeSkeleton
import com.innovativeinside.pitchside.ui.components.LiveMatchTile
import com.innovativeinside.pitchside.ui.components.MatchCardCompact
import com.innovativeinside.pitchside.ui.components.MiniStandingsCard
import com.innovativeinside.pitchside.ui.components.MatchedHeightPair
import com.innovativeinside.pitchside.ui.components.NewsHeroCard
import com.innovativeinside.pitchside.ui.components.NewsRow
import com.innovativeinside.pitchside.ui.components.NextKickoffTile
import com.innovativeinside.pitchside.ui.components.PcCard
import com.innovativeinside.pitchside.ui.components.PlayerStatTile
import com.innovativeinside.pitchside.ui.components.PrimaryButton
import com.innovativeinside.pitchside.ui.components.ProbabilityBar
import com.innovativeinside.pitchside.ui.components.ScreenHeader
import com.innovativeinside.pitchside.ui.components.SectionHeader
import com.innovativeinside.pitchside.ui.components.Segment
import com.innovativeinside.pitchside.ui.components.SegmentedControl
import com.innovativeinside.pitchside.ui.components.StateContent
import com.innovativeinside.pitchside.ui.components.TeamCrest
import com.innovativeinside.pitchside.ui.components.countdown
import com.innovativeinside.pitchside.ui.components.matchDescription
import com.innovativeinside.pitchside.ui.components.rememberReducedMotion
import com.innovativeinside.pitchside.ui.preview.PreviewData
import com.innovativeinside.pitchside.ui.preview.PreviewHost
import com.innovativeinside.pitchside.ui.theme.Pc
import com.innovativeinside.pitchside.ui.theme.PcShape
import com.innovativeinside.pitchside.ui.theme.PcSpace
import com.innovativeinside.pitchside.ui.theme.PcType
import com.innovativeinside.pitchside.ui.vm.CompetitionCount
import com.innovativeinside.pitchside.ui.vm.HomeData
import com.innovativeinside.pitchside.ui.vm.HomeViewModel
import com.innovativeinside.pitchside.ui.vm.MatchFilter
import com.innovativeinside.pitchside.ui.vm.competitionsOf
import com.innovativeinside.pitchside.ui.vm.countsByCompetition
import com.innovativeinside.pitchside.ui.vm.filterByCompetition
import com.innovativeinside.pitchside.ui.vm.filterMatches
import com.innovativeinside.pitchside.ui.vm.inForm
import com.innovativeinside.pitchside.ui.vm.nextKickoff
import com.innovativeinside.pitchside.ui.vm.pickHero
import com.innovativeinside.pitchside.ui.vm.pickSpotlight
import com.innovativeinside.pitchside.ui.vm.rankLiveMatches
import com.innovativeinside.pitchside.ui.vm.statusCounts
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

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
    /** The avatar in the header: account and settings. */
    val openAccount: () -> Unit = {},
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

/** "SUNDAY, NOV 15": the header date as the reference shows it. */
fun headerDate(d: LocalDate): String = d.format(DateTimeFormatter.ofPattern("EEEE, MMM d", Locale.getDefault()))

/**
 * Home, "Football Today": the header (title, date, search, bell, avatar), the LIVE MATCH pager (or the match
 * spotlight when nothing is live), TOP MATCHES, TODAY'S HIGHLIGHTS with competition and status filters, MINI
 * STANDINGS & NEWS, the matchday cards and more news.
 */
@Composable
fun HomeContent(
    state: UiState<HomeData>,
    actions: HomeActions,
    onRetry: () -> Unit,
    heroStats: Map<String, List<StatRow>> = emptyMap(),
    loadStats: (String) -> Unit = {},
) {
    val app = LocalApp.current
    val session by app.session.collectAsStateWithLifecycle()
    ScreenHeader(
        stringResource(R.string.home_title), subtitle = headerDate(LocalDate.now()),
        onSearch = actions.openSearch, onNotifications = actions.openNotifications,
        onAvatar = actions.openAccount, avatarInitial = session?.displayName,
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
    var followFor by remember { mutableStateOf<Match?>(null) }

    Column(verticalArrangement = Arrangement.spacedBy(PcSpace.sm)) {
        val hiddenNow = d.matches.filter { app.isHidden(prefs, it) }
        if (hiddenNow.isNotEmpty()) ShieldBanner(hiddenNow.size) { app.caughtUp(hiddenNow.map { it.id }) }

        // 1. LIVE MATCH (pager) or the spotlight when nothing is live.
        LiveSection(live, actions, heroStats, loadStats, d) { followFor = it }

        // 2. TOP MATCHES: horizontal compact cards, upcoming first, then live, then finished.
        if (matches.isNotEmpty()) {
            Spacer(Modifier.height(PcSpace.xs))
            SectionHeader("Top matches", "See all", actions.openMatches)
            val order = mapOf(Status.Upcoming to 0, Status.Live to 1, Status.Postponed to 2, Status.Finished to 3)
            LazyRow(Modifier.fillMaxWidth().testTag("today_rail"), horizontalArrangement = Arrangement.spacedBy(PcSpace.sm)) {
                items(matches.sortedWith(compareBy<Match>({ order[it.status] }, { it.kickoffMs })), key = { it.id }) { m ->
                    MatchCardCompact(m, app.isHidden(prefs, m), { actions.openMatch(m.id) })
                }
            }
        }

        // 3. TODAY'S HIGHLIGHTS: competition chips, Live / Upcoming / Finished, then one bordered list.
        Spacer(Modifier.height(PcSpace.xs))
        HighlightsSection(d.matches, matches, comps, selectedComp, actions) { followFor = it }

        // 4. MINI STANDINGS & NEWS
        MiniStandingsAndNews(d, actions)

        // 5. MATCHDAY cards
        BentoGrid(d, actions)

        // 6. MORE NEWS
        val more = d.news.drop(1).take(3)
        if (more.isNotEmpty()) {
            Spacer(Modifier.height(PcSpace.xs))
            SectionHeader("More news", "See all", actions.openNews)
            PcCard(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(horizontal = PcSpace.md)) {
                    more.forEachIndexed { i, n -> if (i > 0) Hairline(); NewsRow(n, { actions.openArticle(n.id) }) }
                }
            }
        }
    }
    followFor?.let { m -> FollowTeamsSheet(m) { followFor = null } }
}

@Composable
private fun LiveSection(live: List<Match>, actions: HomeActions, heroStats: Map<String, List<StatRow>>, loadStats: (String) -> Unit, d: HomeData, onMenu: (Match) -> Unit) {
    val app = LocalApp.current
    val prefs = LocalPrefs.current
    val reduced = rememberReducedMotion()
    val scope = rememberCoroutineScope()
    var promoted by rememberSaveable { mutableStateOf<String?>(null) }
    val ranked = rankLiveMatches(live, prefs.teams)
    val hero = pickHero(live, prefs.teams, promoted)

    if (hero == null) { SpotlightSection(d, actions, onMenu); return }

    SectionHeader("Live match", count = "${live.size} live", modifier = Modifier.testTag("live_count"))

    if (ranked.size == 1) {
        LaunchedEffect(hero.id, hero.label) { loadStats(hero.id) }
        LiveMatchTile(
            hero, app.isHidden(prefs, hero), { actions.openMatch(hero.id) }, { app.reveal(hero.id) },
            modifier = Modifier.testTag("live_hero"), stats = heroStats[hero.id].orEmpty(), onMenu = { onMenu(hero) },
        )
        return
    }

    // Several live: a pager with the next card peeking. The settled page is the featured match.
    val pager = rememberPagerState(initialPage = ranked.indexOfFirst { it.id == hero.id }.coerceAtLeast(0)) { ranked.size }
    LaunchedEffect(pager.settledPage) { ranked.getOrNull(pager.settledPage)?.let { promoted = it.id } }
    // Keep the featured match in view when the live list reorders.
    LaunchedEffect(hero.id, ranked.map { it.id }) {
        val i = ranked.indexOfFirst { it.id == hero.id }
        if (i >= 0 && i != pager.currentPage) pager.scrollToPage(i)
    }
    val current = ranked.getOrElse(pager.currentPage) { hero }
    LaunchedEffect(current.id, current.label) { loadStats(current.id) }
    HorizontalPager(
        pager,
        modifier = Modifier.fillMaxWidth().testTag("live_pager"),
        contentPadding = PaddingValues(end = 36.dp),
        pageSpacing = PcSpace.sm,
        beyondViewportPageCount = ranked.size,
        key = { ranked[it].id },
        verticalAlignment = Alignment.Top,
    ) { page ->
        val m = ranked[page]
        val featured = page == pager.currentPage
        LiveMatchTile(
            m, app.isHidden(prefs, m),
            onClick = {
                if (featured) actions.openMatch(m.id)
                else scope.launch { if (reduced) pager.scrollToPage(page) else pager.animateScrollToPage(page) }
            },
            onReveal = { app.reveal(m.id) },
            modifier = if (featured) Modifier.testTag("live_hero") else Modifier,
            stats = heroStats[m.id].orEmpty(),
            onMenu = { onMenu(m) },
            description = if (featured) null else matchDescription(m, app.isHidden(prefs, m)) + ". Show as featured match",
        )
    }
    PagerDots(ranked.size, pager.currentPage)
}

/** Small page indicator under the live pager; the current page is a wide emerald pill. Decorative. */
@Composable
private fun PagerDots(count: Int, current: Int) {
    Row(Modifier.fillMaxWidth().clearAndSetSemantics {}, horizontalArrangement = Arrangement.spacedBy(5.dp, Alignment.CenterHorizontally)) {
        repeat(count) { i ->
            Box(Modifier.size(width = if (i == current) 18.dp else 6.dp, height = 6.dp).clip(CircleShape).background(if (i == current) Pc.Accent else Pc.OutlineStrong))
        }
    }
}

/**
 * Nothing live: the spotlight match (a followed team's next game, else the biggest game coming up by table
 * position) on the same glow card, with table positions, win probability and a kickoff countdown.
 */
@Composable
private fun SpotlightSection(d: HomeData, actions: HomeActions, onMenu: (Match) -> Unit) {
    val app = LocalApp.current
    val prefs = LocalPrefs.current
    var now by remember { mutableLongStateOf(System.currentTimeMillis()) }
    LaunchedEffect(Unit) { while (true) { kotlinx.coroutines.delay(30_000); now = System.currentTimeMillis() } }
    val spot = pickSpotlight(d.upcoming, d.matches, prefs.teams, d.tables, now)
    SectionHeader(if (spot?.status == Status.Finished) "Result of the day" else "Match spotlight", trailing = {
        Row(
            Modifier.clip(PcShape.Chip).background(Pc.Surface).padding(horizontal = 10.dp, vertical = 5.dp).testTag("no_live"),
            verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Box(Modifier.size(6.dp).clip(CircleShape).background(Pc.Text3))
            Text("Nothing live right now", style = PcType.Caption)
        }
    })
    if (spot == null) {
        PcCard(Modifier.fillMaxWidth()) {
            Row(Modifier.padding(PcSpace.md), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(PcSpace.sm)) {
                Box(Modifier.size(44.dp).clip(CircleShape).background(Pc.AccentContainer), contentAlignment = Alignment.Center) { Icon(Icons.Outlined.SportsSoccer, null, tint = Pc.Accent) }
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
        onMenu = { onMenu(spot) },
    ) {
        if (hr != null || ar != null) Row(Modifier.fillMaxWidth()) {
            Text(hr?.let { "${FormText.ordinal(it.rank)} · ${it.points} pts" }.orEmpty(), style = PcType.Caption, color = Pc.OnHero2, modifier = Modifier.weight(1f), textAlign = TextAlign.Center)
            Spacer(Modifier.width(104.dp))
            Text(ar?.let { "${FormText.ordinal(it.rank)} · ${it.points} pts" }.orEmpty(), style = PcType.Caption, color = Pc.OnHero2, modifier = Modifier.weight(1f), textAlign = TextAlign.Center)
        }
        val p = spot.probs
        if (upcoming && p != null) {
            Hairline(color = Pc.OnHero.copy(alpha = 0.10f))
            Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Row(Modifier.fillMaxWidth()) {
                    Text("${p.home}%", style = PcType.Tag.copy(fontSize = 16.sp), color = Pc.OnHero, modifier = Modifier.weight(1f))
                    Text("DRAW ${p.draw}%", style = PcType.Overline, color = Pc.OnHero2, modifier = Modifier.weight(1f), textAlign = TextAlign.Center)
                    Text("${p.away}%", style = PcType.Tag.copy(fontSize = 16.sp), color = Pc.OnHero, modifier = Modifier.weight(1f), textAlign = TextAlign.End)
                }
                ProbabilityBar(p, Modifier.fillMaxWidth(), onHero = true)
            }
        }
        if (upcoming) Text("KICKS OFF IN ${countdown(now, spot.kickoffMs).uppercase()}", style = PcType.Tag, color = Pc.HeroAccent, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
    }
}

/** TODAY'S HIGHLIGHTS: count suffix, competition chips, Live / Upcoming / Finished, and the bordered list. */
@Composable
private fun HighlightsSection(all: List<Match>, matches: List<Match>, comps: List<Pair<String, String>>, selectedComp: String?, actions: HomeActions, onFollow: (Match) -> Unit) {
    val app = LocalApp.current
    val prefs = LocalPrefs.current
    val counts = statusCounts(matches)
    val segs = listOf(MatchFilter.Live, MatchFilter.Upcoming, MatchFilter.Finished)
    var seg by rememberSaveable { mutableIntStateOf(if ((counts[MatchFilter.Live] ?: 0) > 0) 0 else 1) }
    SectionHeader("Today's highlights", "See all", actions.openMatches, count = "${all.size} ${if (all.size == 1) "match" else "matches"}")
    if (comps.size > 1) {
        val labels = listOf("All") + comps.map { it.second }
        FilterChipRow(labels, comps.firstOrNull { it.first == selectedComp }?.second ?: "All", { label ->
            app.setCompetition(comps.firstOrNull { it.second == label }?.first)
        }, contentPadding = 0.dp)
    }
    SegmentedControl(
        listOf(Segment("Live", counts[MatchFilter.Live], live = true), Segment("Upcoming", counts[MatchFilter.Upcoming]), Segment("Finished", counts[MatchFilter.Finished])),
        seg, { seg = it },
    )
    val shown = filterMatches(matches, segs[seg], prefs.teams, prefs.leagues)
        .sortedWith(compareBy<Match>({ if (it.involves(prefs.teams)) 0 else 1 }, { it.kickoffMs }))
    if (matches.isEmpty()) {
        EmptyState("No matches today", "Check tomorrow's fixtures or browse a league.", actionLabel = "See fixtures", onAction = actions.openMatches, icon = Icons.Outlined.EventBusy)
    } else if (shown.isEmpty()) {
        PcCard(Modifier.fillMaxWidth()) {
            Text(
                when (segs[seg]) { MatchFilter.Live -> "Nothing live right now."; MatchFilter.Upcoming -> "No more kickoffs today."; else -> "No results yet today." },
                style = PcType.BodyMuted, modifier = Modifier.padding(PcSpace.md),
            )
        }
    } else {
        PcCard(Modifier.fillMaxWidth().testTag("highlights")) {
            Column {
                shown.forEachIndexed { i, m ->
                    if (i > 0) Hairline(Modifier.padding(horizontal = PcSpace.md))
                    HighlightRow(
                        m, app.isHidden(prefs, m), { actions.openMatch(m.id) }, { app.reveal(m.id) },
                        favorite = m.involves(prefs.teams), onFavorite = { onFollow(m) },
                        reminderOn = m.id in prefs.matchAlerts, onReminder = { app.toggleMatchAlert(m.id) },
                    )
                }
            }
        }
    }
}

/**
 * MINI STANDINGS & NEWS: the covered table's top four beside the latest story. Side by side when there is room;
 * stacked on narrow screens or with a large font so nothing clips.
 */
@Composable
private fun MiniStandingsAndNews(d: HomeData, actions: HomeActions) {
    val prefs = LocalPrefs.current
    val lead = d.news.firstOrNull()
    val hasTable = d.table.isNotEmpty()
    if (!hasTable && lead == null) return
    Spacer(Modifier.height(PcSpace.xs))
    SectionHeader(
        when { hasTable && lead != null -> "Mini standings & news"; hasTable -> "Mini standings"; else -> "Latest news" },
        modifier = Modifier.testTag("mini_standings_news"),
    )
    val standings: @Composable (Modifier) -> Unit = { m ->
        MiniStandingsCard(
            d.tableLeague?.name ?: "Table", d.tableLeague?.logo, d.table, prefs.teams.toSet(),
            { actions.openLeague(d.tableLeague?.id ?: d.table.first().team.leagueId) }, m,
        )
    }
    val news: @Composable (Modifier, Float?) -> Unit = { m, aspect -> lead?.let { NewsHeroCard(it, { actions.openArticle(it.id) }, m, label = "Latest news", aspect = aspect) } }
    BoxWithConstraints(Modifier.fillMaxWidth()) {
        val fontScale = LocalDensity.current.fontScale
        // Side by side needs ~150dp of row content per card; on 360dp phones (or with a large font) stack them.
        val sideBySide = hasTable && lead != null && maxWidth >= 352.dp && fontScale <= 1.15f
        if (sideBySide) {
            MatchedHeightPair(Modifier.fillMaxWidth(), first = { standings(Modifier.fillMaxWidth()) }, second = { news(Modifier.fillMaxSize(), null) })
        } else {
            Column(verticalArrangement = Arrangement.spacedBy(PcSpace.sm)) {
                if (hasTable) standings(Modifier.fillMaxWidth())
                news(Modifier.fillMaxWidth(), 16f / 10f)
            }
        }
    }
}

/** MATCHDAY: two-column cards. Cards without data are left out; an odd card spans the full row, so there are no holes. */
@Composable
private fun BentoGrid(d: HomeData, actions: HomeActions) {
    val app = LocalApp.current
    val prefs = LocalPrefs.current
    val ctx = LocalContext.current
    val next = nextKickoff(d.upcoming, prefs.teams, System.currentTimeMillis())
    val tiles = buildList<@Composable (Modifier) -> Unit> {
        if (next != null) add { m -> NextKickoffTile(next, next.id in prefs.matchAlerts, { app.toggleMatchAlert(next.id) }, { addMatchToCalendar(ctx, next) }, { actions.openMatch(next.id) }, m) }
        d.scorers.firstOrNull()?.let { s -> add { m -> PlayerStatTile(s, { actions.openPlayer(s.playerId) }, m) } }
        val perComp = countsByCompetition(d.matches)
        if (perComp.isNotEmpty()) add { m -> TodayByCompetitionTile(perComp, actions.openMatches, m) }
        val hot = inForm(d.tables)
        if (hot.isNotEmpty()) add { m -> FormGuideTile(hot, actions.openTeam, m) }
        if (d.matches.isNotEmpty()) add { m -> TodayNumbersTile(d.matches, actions.openMatches, m) }
        add { m -> AskTile(actions.openAsk, m) }
    }
    if (tiles.isEmpty()) return
    Spacer(Modifier.height(PcSpace.xs))
    SectionHeader("Matchday")
    Column(Modifier.testTag("bento"), verticalArrangement = Arrangement.spacedBy(PcSpace.sm)) {
        tiles.chunked(2).forEach { row ->
            Row(Modifier.fillMaxWidth().height(IntrinsicSize.Min), horizontalArrangement = Arrangement.spacedBy(PcSpace.sm)) {
                row.forEach { tile -> tile(Modifier.weight(1f).fillMaxHeight()) }
            }
        }
    }
}

/** How many games each competition has today, with its logo; busiest first. */
@Composable
private fun TodayByCompetitionTile(rows: List<CompetitionCount>, openMatches: () -> Unit, modifier: Modifier) {
    BentoTile(
        "By competition", modifier, openMatches,
        description = "Today by competition: " + rows.joinToString("; ") { "${it.name}, ${it.count} matches" },
    ) {
        rows.take(4).forEach { c ->
            Row(Modifier.fillMaxWidth().heightIn(min = 28.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                LeagueBadge(c.name.split(" ").filter { it.isNotBlank() }.take(2).joinToString("") { it.first().uppercase() }, 22, c.logo)
                Text(c.name, style = PcType.Label, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f))
                if (c.live > 0) Box(Modifier.size(6.dp).clip(CircleShape).background(Pc.Live))
                Text(c.count.toString(), style = PcType.Tag)
            }
        }
        if (rows.size > 4) Text("+${rows.size - 4} more", style = PcType.Caption)
    }
}

/** Sides with the best last-five record across the covered tables. */
@Composable
private fun FormGuideTile(rows: List<com.innovativeinside.pitchside.data.model.StandingRow>, openTeam: (String) -> Unit, modifier: Modifier) {
    BentoTile(
        "In form", modifier, { openTeam(rows.first().team.id) },
        description = "Teams in form: " + rows.joinToString("; ") { "${it.team.name}, ${it.form.toList().joinToString(" ")}" },
    ) {
        rows.forEach { r ->
            Row(Modifier.fillMaxWidth().heightIn(min = 30.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                TeamCrest(r.team, 20.dp)
                Text(r.team.short.uppercase(), style = PcType.TeamNameSmall, maxLines = 1, modifier = Modifier.weight(1f))
                FormPills(r.form.takeLast(5), compact = true)
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
    BentoTile(
        "In numbers", modifier, openMatches,
        description = "Today: ${matches.size} matches, $goals goals, $done finished, $toCome to come",
    ) {
        Row(verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(goals.toString(), style = PcType.ScoreCompact, color = Pc.Accent)
            Text("GOALS", style = PcType.Overline, modifier = Modifier.padding(bottom = 5.dp))
        }
        Text("${matches.size} matches today", style = PcType.LabelStrong)
        Text(listOfNotNull("$done finished".takeIf { done > 0 }, "$toCome to come".takeIf { toCome > 0 }).joinToString(" · ").ifBlank { "All under way" }, style = PcType.Caption)
    }
}

/** Ask (questions answered from match data): kept on Home as a card so the header stays title, search, bell, avatar. */
@Composable
private fun AskTile(openAsk: () -> Unit, modifier: Modifier) {
    val label = stringResource(R.string.action_ask)
    BentoTile(label, modifier, openAsk, description = "$label: questions about any match, team or table, answered from match data") {
        Box(Modifier.size(40.dp).clip(CircleShape).background(Pc.AccentContainer), contentAlignment = Alignment.Center) {
            Icon(Icons.Outlined.QuestionAnswer, null, tint = Pc.Accent, modifier = Modifier.size(22.dp))
        }
        Text("Any match, team or table", style = PcType.BodyStrong)
        Text("Answers from match data", style = PcType.Caption)
    }
}

@Composable
fun ShieldBanner(count: Int, onCaughtUp: () -> Unit) {
    PcCard(Modifier.fillMaxWidth(), container = Pc.AccentContainer, border = Pc.Accent.copy(alpha = 0.35f)) {
        Column(Modifier.padding(PcSpace.md), verticalArrangement = Arrangement.spacedBy(PcSpace.sm)) {
            Row(horizontalArrangement = Arrangement.spacedBy(PcSpace.sm), verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Outlined.VisibilityOff, null, tint = Pc.Accent)
                Column(Modifier.weight(1f).semantics(mergeDescendants = true) { contentDescription = "Spoiler Shield is on. $count scores hidden." }) {
                    Text("SPOILER SHIELD IS ON", style = PcType.Tag)
                    Text("$count ${if (count == 1) "score is" else "scores are"} hidden. Alerts wait until you're caught up.", style = PcType.Caption)
                }
            }
            PrimaryButton("I'm caught up", onCaughtUp, Modifier.fillMaxWidth())
        }
    }
}

// ---------------- Previews ----------------

private val previewActions = HomeActions({}, {}, {}, {}, {}, {}, {}, {})

@Preview(name = "Home · dark", showBackground = true, widthDp = 380, heightDp = 2000, backgroundColor = 0xFF0E1110)
@Composable
private fun HomeDarkPreview() = PreviewHost(dark = true) { Column { HomeContent(UiState.Success(PreviewData.home), previewActions, {}) } }

@Preview(name = "Home · light", showBackground = true, widthDp = 380, heightDp = 2000)
@Composable
private fun HomeLightPreview() = PreviewHost(dark = false) { Column { HomeContent(UiState.Success(PreviewData.home), previewActions, {}) } }

@Preview(name = "Home · no live", showBackground = true, widthDp = 360, heightDp = 1400, backgroundColor = 0xFF0E1110)
@Composable
private fun HomeNoLivePreview() = PreviewHost(dark = true) {
    Column { HomeContent(UiState.Success(PreviewData.home.copy(matches = PreviewData.matches.filter { it.status != Status.Live })), previewActions, {}) }
}

@Preview(name = "Home · loading", showBackground = true, widthDp = 360, backgroundColor = 0xFF0E1110)
@Composable
private fun HomeLoadingPreview() = PreviewHost(dark = true) { Column { HomeContent(UiState.Loading, previewActions, {}) } }

@Preview(name = "Home · error", showBackground = true, widthDp = 360, backgroundColor = 0xFF0E1110)
@Composable
private fun HomeErrorPreview() = PreviewHost(dark = true) { Column { HomeContent(UiState.Error(AppError.Network()), previewActions, {}) } }
