package com.innovativeinside.pitchside.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.KeyboardArrowRight
import androidx.compose.material.icons.outlined.EventBusy
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
import androidx.compose.ui.layout.Placeable
import androidx.compose.ui.layout.SubcomposeLayout
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Dp
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
import com.innovativeinside.pitchside.ui.components.FilterChipRow
import com.innovativeinside.pitchside.ui.components.Hairline
import com.innovativeinside.pitchside.ui.components.HighlightRow
import com.innovativeinside.pitchside.ui.components.HomeSkeleton
import com.innovativeinside.pitchside.ui.components.LiveMatchTile
import com.innovativeinside.pitchside.ui.components.MatchCardCompact
import com.innovativeinside.pitchside.ui.components.PcCard
import com.innovativeinside.pitchside.ui.components.ProbabilityBar
import com.innovativeinside.pitchside.ui.components.ScreenHeader
import com.innovativeinside.pitchside.ui.components.SectionHeader
import com.innovativeinside.pitchside.ui.components.Segment
import com.innovativeinside.pitchside.ui.components.SegmentedControl
import com.innovativeinside.pitchside.ui.components.StateContent
import com.innovativeinside.pitchside.ui.components.countdown
import com.innovativeinside.pitchside.ui.components.matchDescription
import com.innovativeinside.pitchside.ui.components.pcClickable
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
    // Home is one fixed screen: no vertical scrolling, pull-to-refresh still works.
    RefreshScreen(refreshing, vm::refresh, refreshError, vm::dismissRefreshError, maxWidth = 760, fillViewport = true) {
        HomeContent(state, actions, vm::retry, stats, vm::loadStats, Modifier.weight(1f))
    }
}

/** "SUNDAY, NOV 15": the header date as the reference shows it. */
fun headerDate(d: LocalDate): String = d.format(DateTimeFormatter.ofPattern("EEEE, MMM d", Locale.getDefault()))

/**
 * Home, "Football Today", on exactly one screen (it never scrolls vertically): the header (title, date, search,
 * bell, avatar), the Spoiler Shield banner when it is hiding scores, the LIVE MATCH pager (or the match spotlight
 * when nothing is live), TOP MATCHES as a horizontal rail, and TODAY's matches with Live / Upcoming / Finished,
 * which takes the remaining height and shows as many rows as fit, ending with "See all".
 *
 * On short screens or with a large font the lower-priority pieces shrink or drop out (hero stats, the rail, the
 * competition chips) rather than clip; see [planHome]. Standings and news live on their own tabs.
 *
 * Give [modifier] a bounded height (weight or fillMaxSize).
 */
@Composable
fun HomeContent(
    state: UiState<HomeData>,
    actions: HomeActions,
    onRetry: () -> Unit,
    heroStats: Map<String, List<StatRow>> = emptyMap(),
    loadStats: (String) -> Unit = {},
    modifier: Modifier = Modifier,
) {
    val app = LocalApp.current
    val session by app.session.collectAsStateWithLifecycle()
    Column(modifier.fillMaxWidth()) {
        ScreenHeader(
            stringResource(R.string.home_title), subtitle = headerDate(LocalDate.now()),
            onSearch = actions.openSearch, onNotifications = actions.openNotifications,
            onAvatar = actions.openAccount, avatarInitial = session?.displayName,
        )
        StateContent(
            state, onRetry,
            Modifier.weight(1f).fillMaxWidth().padding(start = PcSpace.screen, end = PcSpace.screen, bottom = PcSpace.xs),
            loading = { HomeSkeleton(Modifier.padding(top = PcSpace.xxs)) },
            fill = true,
        ) { d ->
            HomeBody(d, actions, heroStats, loadStats)
        }
    }
}

/** How much of the hero card to show. */
internal enum class HeroSize { Compact, Standard, Full }

/** What fits on Home at the current size; null hero means it is left out (very short screens such as landscape). */
internal data class HomePlan(val hero: HeroSize?, val rail: Boolean, val chips: Boolean, val todayHeader: Boolean)

/**
 * Decides which pieces fit in [height]. [t] converts a text line height in sp to dp at the current font scale, so
 * the estimates grow with the font. Order of priority: today's list with one row, a compact hero, the rail, room for
 * a second row, a fuller hero, a third row, hero stats, competition chips. Today's list takes whatever is left and
 * measures its own rows, so an estimate that is a little off only changes how many rows show.
 */
internal fun planHome(
    height: Dp,
    t: (Float) -> Dp,
    shield: Boolean,
    spotlight: Boolean,
    pager: Boolean,
    heroHasExtras: Boolean,
    roomyFont: Boolean,
    hasRail: Boolean,
    hasChips: Boolean,
): HomePlan {
    val gap = 12.dp
    val hdr = maxOf(32.dp, t(26f)) + 4.dp
    val row = maxOf(60.dp, t(19f) * 2 + 16.dp)
    val todayCore = hdr + maxOf(48.dp, t(18f) + 14.dp) + 8.dp + row + 49.dp
    val shieldH = if (shield) maxOf(56.dp, t(16f) * 2 + 20.dp) + gap else 0.dp
    val dots = if (pager) 14.dp else 0.dp
    val compact = hdr + 16.dp + 48.dp + 4.dp + maxOf(44.dp + t(19f), t(46f)) + (if (spotlight) 4.dp + t(18f) else 0.dp) + dots
    val standard = hdr + 32.dp + 48.dp + 12.dp + 72.dp + t(21f) * 2 +
        (if (!roomyFont) 12.dp + 32.dp + t(13f) * 2 else 0.dp) +
        (if (spotlight) 24.dp + t(16f) + t(18f) else 0.dp) + dots
    val extras = if (heroHasExtras) (if (spotlight) 37.dp + t(18f) else 25.dp + t(18f)) else 0.dp
    val rail = hdr + 24.dp + maxOf(34.dp, t(26f) + t(16f) + 22.dp) + 8.dp + t(19f) + gap
    val chips = 56.dp

    var avail = height - shieldH - todayCore
    var hero: HeroSize? = null
    if (avail >= compact + gap) { hero = HeroSize.Compact; avail -= compact + gap }
    var showRail = false
    var showChips = false
    if (hero != null && hasRail && avail >= rail) { showRail = true; avail -= rail }
    if (avail >= row) avail -= row
    if (hero != null && avail >= standard - compact) { hero = HeroSize.Standard; avail -= standard - compact }
    if (avail >= row) avail -= row
    if (hero == HeroSize.Standard && heroHasExtras && avail >= extras) { hero = HeroSize.Full; avail -= extras }
    if (hasChips && avail >= chips) { showChips = true }
    return HomePlan(hero, showRail, showChips, todayHeader = hero != null || height >= todayCore + shieldH)
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
    val hiddenNow = d.matches.filter { app.isHidden(prefs, it) }
    var followFor by remember { mutableStateOf<Match?>(null) }
    var promoted by rememberSaveable { mutableStateOf<String?>(null) }
    var now by remember { mutableLongStateOf(System.currentTimeMillis()) }
    LaunchedEffect(Unit) { while (true) { kotlinx.coroutines.delay(30_000); now = System.currentTimeMillis() } }
    val hero = pickHero(live, prefs.teams, promoted)
    val spot = if (hero == null) pickSpotlight(d.upcoming, d.matches, prefs.teams, d.tables, now) else null
    val density = LocalDensity.current
    val t: (Float) -> Dp = { sp -> with(density) { sp.sp.toDp() } }

    BoxWithConstraints(Modifier.fillMaxSize()) {
        val twoPane = maxWidth >= 600.dp && maxHeight < 560.dp
        val heroExtras = if (hero != null) heroStats[hero.id].orEmpty().any { it.label == "Possession" || it.label == "Shots" }
        else spot != null && spot.status == Status.Upcoming && spot.probs != null
        val roomy = density.fontScale <= 1.15f
        val shield = hiddenNow.isNotEmpty()
        val heroSlot: @Composable (HeroSize) -> Unit = { size ->
            if (hero != null) LiveSection(live, hero, size, actions, heroStats, loadStats, { promoted = it }) { followFor = it }
            else SpotlightSection(spot, d, size, now, actions) { followFor = it }
        }
        val rail: @Composable () -> Unit = { TopMatchesRail(matches, actions) }
        val today: @Composable (Modifier, Boolean, Boolean) -> Unit = { m, header, chips ->
            TodaySection(d.matches, matches, comps, selectedComp, actions, m, header, chips) { followFor = it }
        }
        val shieldBanner: @Composable () -> Unit = { ShieldBanner(hiddenNow.size) { app.caughtUp(hiddenNow.map { it.id }) } }

        if (twoPane) {
            // Wide but short (tablet / foldable landscape): hero and rail on the left, today's list on the right.
            val left = planHome(maxHeight, t, shield, hero == null, live.size > 1, heroExtras, roomy, matches.isNotEmpty(), false)
            val right = planHome(maxHeight, t, false, false, false, false, roomy, false, comps.size > 1)
            Row(Modifier.fillMaxSize(), horizontalArrangement = Arrangement.spacedBy(PcSpace.md)) {
                Column(Modifier.weight(1f).fillMaxHeight(), verticalArrangement = Arrangement.spacedBy(PcSpace.sm)) {
                    if (shield) shieldBanner()
                    heroSlot(left.hero ?: HeroSize.Compact)
                    if (left.rail) rail()
                }
                today(Modifier.weight(1f).fillMaxHeight(), true, right.chips)
            }
        } else {
            val plan = planHome(maxHeight, t, shield, hero == null, live.size > 1, heroExtras, roomy, matches.isNotEmpty(), comps.size > 1)
            Column(Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(PcSpace.sm)) {
                if (shield) shieldBanner()
                plan.hero?.let { heroSlot(it) }
                if (plan.rail) rail()
                today(Modifier.weight(1f).fillMaxWidth(), plan.todayHeader, plan.chips)
            }
        }
    }
    followFor?.let { m -> FollowTeamsSheet(m) { followFor = null } }
}

/** Home section header: one line, no action (the today list ends in "See all"), compact height. */
@Composable
private fun HomeSectionHeader(title: String, modifier: Modifier = Modifier, count: String? = null, trailing: (@Composable () -> Unit)? = null) =
    SectionHeader(title, modifier = modifier, count = count, trailing = trailing, maxLines = 1, minHeight = 32.dp)

@Composable
private fun LiveSection(
    live: List<Match>,
    hero: Match,
    size: HeroSize,
    actions: HomeActions,
    heroStats: Map<String, List<StatRow>>,
    loadStats: (String) -> Unit,
    onPromote: (String) -> Unit,
    onMenu: (Match) -> Unit,
) {
    val app = LocalApp.current
    val prefs = LocalPrefs.current
    val reduced = rememberReducedMotion()
    val scope = rememberCoroutineScope()
    val ranked = rankLiveMatches(live, prefs.teams)
    val compact = size == HeroSize.Compact
    val stats: (Match) -> List<StatRow> = { m -> if (size == HeroSize.Full) heroStats[m.id].orEmpty() else emptyList() }

    Column(verticalArrangement = Arrangement.spacedBy(PcSpace.xxs)) {
        HomeSectionHeader("Live match", count = "${live.size} live", modifier = Modifier.testTag("live_count"))
        if (ranked.size <= 1) {
            LaunchedEffect(hero.id, hero.label) { loadStats(hero.id) }
            LiveMatchTile(
                hero, app.isHidden(prefs, hero), { actions.openMatch(hero.id) }, { app.reveal(hero.id) },
                modifier = Modifier.testTag("live_hero"), stats = stats(hero), onMenu = { onMenu(hero) }, compact = compact,
            )
            return@Column
        }
        // Several live: a pager with the next card peeking. The settled page is the featured match.
        val pager = rememberPagerState(initialPage = ranked.indexOfFirst { it.id == hero.id }.coerceAtLeast(0)) { ranked.size }
        LaunchedEffect(pager.settledPage) { ranked.getOrNull(pager.settledPage)?.let { onPromote(it.id) } }
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
                stats = stats(m),
                onMenu = { onMenu(m) },
                description = if (featured) null else matchDescription(m, app.isHidden(prefs, m)) + ". Show as featured match",
                compact = compact,
            )
        }
        Spacer(Modifier.height(4.dp))
        PagerDots(ranked.size, pager.currentPage)
    }
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
 * position) on the same glow card. Full adds the win probability; Standard keeps table positions and the
 * countdown; Compact keeps only the countdown.
 */
@Composable
private fun SpotlightSection(spot: Match?, d: HomeData, size: HeroSize, now: Long, actions: HomeActions, onMenu: (Match) -> Unit) {
    val app = LocalApp.current
    val prefs = LocalPrefs.current
    Column(verticalArrangement = Arrangement.spacedBy(PcSpace.xxs)) {
        HomeSectionHeader(if (spot?.status == Status.Finished) "Result of the day" else "Match spotlight", trailing = {
            Row(
                Modifier.clip(PcShape.Chip).background(Pc.Surface).padding(horizontal = 10.dp, vertical = 5.dp).testTag("no_live"),
                verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                Box(Modifier.size(6.dp).clip(CircleShape).background(Pc.Text3))
                Text("Nothing live right now", style = PcType.Caption, maxLines = 1)
            }
        })
        if (spot == null) {
            PcCard(Modifier.fillMaxWidth()) {
                Row(Modifier.padding(PcSpace.md), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(PcSpace.sm)) {
                    Box(Modifier.size(44.dp).clip(CircleShape).background(Pc.AccentContainer), contentAlignment = Alignment.Center) { Icon(Icons.Outlined.SportsSoccer, null, tint = Pc.Accent) }
                    Column(Modifier.weight(1f)) {
                        Text("No fixtures in the next few days", style = PcType.BodyStrong, maxLines = 2, overflow = TextOverflow.Ellipsis)
                        Text("Live matches take over this spot at kickoff.", style = PcType.Caption, maxLines = 2, overflow = TextOverflow.Ellipsis)
                    }
                }
            }
            return@Column
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
            compact = size == HeroSize.Compact,
        ) {
            if (size != HeroSize.Compact && (hr != null || ar != null)) Row(Modifier.fillMaxWidth()) {
                Text(hr?.let { "${FormText.ordinal(it.rank)} · ${it.points} pts" }.orEmpty(), style = PcType.Caption, color = Pc.OnHero2, modifier = Modifier.weight(1f), textAlign = TextAlign.Center, maxLines = 1)
                Spacer(Modifier.width(104.dp))
                Text(ar?.let { "${FormText.ordinal(it.rank)} · ${it.points} pts" }.orEmpty(), style = PcType.Caption, color = Pc.OnHero2, modifier = Modifier.weight(1f), textAlign = TextAlign.Center, maxLines = 1)
            }
            val p = spot.probs
            if (size == HeroSize.Full && upcoming && p != null) {
                Hairline(color = Pc.OnHero.copy(alpha = 0.10f))
                Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Row(Modifier.fillMaxWidth()) {
                        Text("${p.home}%", style = PcType.Tag.copy(fontSize = 16.sp), color = Pc.OnHero, modifier = Modifier.weight(1f), maxLines = 1)
                        Text("DRAW ${p.draw}%", style = PcType.Overline, color = Pc.OnHero2, modifier = Modifier.weight(1f), textAlign = TextAlign.Center, maxLines = 1)
                        Text("${p.away}%", style = PcType.Tag.copy(fontSize = 16.sp), color = Pc.OnHero, modifier = Modifier.weight(1f), textAlign = TextAlign.End, maxLines = 1)
                    }
                    ProbabilityBar(p, Modifier.fillMaxWidth(), onHero = true)
                }
            }
            if (upcoming) Text(
                "KICKS OFF IN ${countdown(now, spot.kickoffMs).uppercase()}", style = PcType.Tag, color = Pc.HeroAccent,
                textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth(), maxLines = 1, overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

/** TOP MATCHES: compact cards in a horizontal rail, upcoming first, then live, then finished. */
@Composable
private fun TopMatchesRail(matches: List<Match>, actions: HomeActions) {
    val app = LocalApp.current
    val prefs = LocalPrefs.current
    val order = mapOf(Status.Upcoming to 0, Status.Live to 1, Status.Postponed to 2, Status.Finished to 3)
    Column(verticalArrangement = Arrangement.spacedBy(PcSpace.xxs)) {
        HomeSectionHeader("Top matches")
        LazyRow(Modifier.fillMaxWidth().testTag("today_rail"), horizontalArrangement = Arrangement.spacedBy(PcSpace.sm)) {
            items(matches.sortedWith(compareBy<Match>({ order[it.status] }, { it.kickoffMs })), key = { it.id }) { m ->
                MatchCardCompact(m, app.isHidden(prefs, m), { actions.openMatch(m.id) }, dense = true)
            }
        }
    }
}

/**
 * TODAY: count suffix, optional competition chips, Live / Upcoming / Finished, then one card with as many rows as
 * fit the remaining height and a "See all" row that opens Matches.
 */
@Composable
private fun TodaySection(
    all: List<Match>,
    matches: List<Match>,
    comps: List<Pair<String, String>>,
    selectedComp: String?,
    actions: HomeActions,
    modifier: Modifier,
    header: Boolean,
    chips: Boolean,
    onFollow: (Match) -> Unit,
) {
    val app = LocalApp.current
    val prefs = LocalPrefs.current
    val counts = statusCounts(matches)
    val segs = listOf(MatchFilter.Live, MatchFilter.Upcoming, MatchFilter.Finished)
    var seg by rememberSaveable { mutableIntStateOf(if ((counts[MatchFilter.Live] ?: 0) > 0) 0 else 1) }
    val total = all.size
    Column(modifier, verticalArrangement = Arrangement.spacedBy(PcSpace.xs)) {
        if (header) HomeSectionHeader("Today", count = "$total ${if (total == 1) "match" else "matches"}", modifier = Modifier.padding(bottom = 0.dp))
        if (chips) {
            val labels = listOf("All") + comps.map { it.second }
            FilterChipRow(labels, comps.firstOrNull { it.first == selectedComp }?.second ?: "All", { label ->
                app.setCompetition(comps.firstOrNull { it.second == label }?.first)
            }, contentPadding = 0.dp)
        }
        if (all.isEmpty()) {
            NoMatchesCard(actions.openMatches)
            return@Column
        }
        SegmentedControl(
            listOf(Segment("Live", counts[MatchFilter.Live], live = true), Segment("Upcoming", counts[MatchFilter.Upcoming]), Segment("Finished", counts[MatchFilter.Finished])),
            seg, { seg = it },
        )
        val shown = filterMatches(matches, segs[seg], prefs.teams, prefs.leagues)
            .sortedWith(compareBy<Match>({ if (it.involves(prefs.teams)) 0 else 1 }, { it.kickoffMs }))
        PcCard(Modifier.fillMaxWidth().weight(1f, fill = false).testTag("highlights")) {
            if (shown.isEmpty()) {
                Column {
                    Text(
                        when (segs[seg]) { MatchFilter.Live -> "Nothing live right now."; MatchFilter.Upcoming -> "No more kickoffs today."; else -> "No results yet today." },
                        style = PcType.BodyMuted, maxLines = 2, overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.padding(horizontal = PcSpace.md, vertical = PcSpace.sm),
                    )
                    Hairline(Modifier.padding(horizontal = PcSpace.md))
                    SeeAllRow(total, 0, actions.openMatches)
                }
            } else {
                FitList(
                    shown,
                    // Same scoreboard row as the Matches tab, so a match reads the same everywhere.
                    row = { m ->
                        com.innovativeinside.pitchside.ui.components.MatchRow(
                            m, app.isHidden(prefs, m), { actions.openMatch(m.id) }, { app.reveal(m.id) },
                            favorite = m.involves(prefs.teams), onFavorite = { onFollow(m) },
                            reminderOn = m.id in prefs.matchAlerts, onReminder = { app.toggleMatchAlert(m.id) },
                        )
                    },
                    footer = { more -> SeeAllRow(total, more, actions.openMatches) },
                )
            }
        }
    }
}

/**
 * A column that shows only the rows that fit its maximum height (no scrolling), then [footer] with the number of
 * rows left out. Rows are separated by hairlines. With unbounded height every row shows.
 */
@Composable
private fun FitList(items: List<Match>, row: @Composable (Match) -> Unit, footer: @Composable (Int) -> Unit, modifier: Modifier = Modifier) {
    SubcomposeLayout(modifier.fillMaxWidth()) { c ->
        val loose = Constraints(minWidth = c.maxWidth, maxWidth = c.maxWidth, minHeight = 0, maxHeight = Constraints.Infinity)
        val foot: @Composable (Int) -> Unit = { more -> Column { Hairline(Modifier.padding(horizontal = PcSpace.md)); footer(more) } }
        val footerH = subcompose("probe") { foot(items.size) }.maxOfOrNull { it.measure(loose).height } ?: 0
        val placed = mutableListOf<Placeable>()
        var used = 0
        for ((i, m) in items.withIndex()) {
            val p = subcompose("row-${m.id}") {
                Column { if (i > 0) Hairline(Modifier.padding(horizontal = PcSpace.md)); row(m) }
            }.first().measure(loose)
            if (c.hasBoundedHeight && used + p.height + footerH > c.maxHeight) break
            placed += p
            used += p.height
        }
        val f = subcompose("footer") { if (placed.isEmpty()) footer(items.size) else foot(items.size - placed.size) }.first().measure(loose)
        val h = (used + f.height).coerceIn(c.minHeight, if (c.hasBoundedHeight) c.maxHeight else Int.MAX_VALUE)
        layout(c.maxWidth, h) {
            var y = 0
            placed.forEach { it.placeRelative(0, y); y += it.height }
            f.placeRelative(0, y)
        }
    }
}

/** "SEE ALL 12 MATCHES   +7 more ›": the last row of today's list, opens Matches. */
@Composable
private fun SeeAllRow(total: Int, more: Int, onClick: () -> Unit) {
    val label = "See all $total ${if (total == 1) "match" else "matches"}"
    Row(
        Modifier.fillMaxWidth().heightIn(min = PcSpace.touch).pcClickable(PcShape.Control, label, onClick = onClick)
            .semantics(mergeDescendants = true) { contentDescription = label + if (more > 0) ", $more more not shown" else "" }
            .padding(start = PcSpace.md, end = PcSpace.xs).testTag("see_all_today"),
        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(PcSpace.xs),
    ) {
        Text(label.uppercase(), style = PcType.Tag, color = Pc.Accent, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f))
        if (more > 0) Text("+$more more", style = PcType.Caption, maxLines = 1)
        Icon(Icons.AutoMirrored.Outlined.KeyboardArrowRight, null, tint = Pc.Accent, modifier = Modifier.size(20.dp))
    }
}

/** No matches at all today: a short card (fits small screens) with a link to the fixtures. */
@Composable
private fun NoMatchesCard(openMatches: () -> Unit) {
    PcCard(Modifier.fillMaxWidth().testTag("no_matches_today")) {
        Column {
            Row(Modifier.padding(PcSpace.md), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(PcSpace.sm)) {
                Box(Modifier.size(44.dp).clip(CircleShape).background(Pc.AccentContainer), contentAlignment = Alignment.Center) { Icon(Icons.Outlined.EventBusy, null, tint = Pc.Accent) }
                Column(Modifier.weight(1f)) {
                    Text("No matches today", style = PcType.BodyStrong, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Text("Check tomorrow's fixtures or browse a league.", style = PcType.Caption, maxLines = 2, overflow = TextOverflow.Ellipsis)
                }
            }
            Hairline(Modifier.padding(horizontal = PcSpace.md))
            Row(
                Modifier.fillMaxWidth().heightIn(min = PcSpace.touch).pcClickable(PcShape.Control, "See fixtures", onClick = openMatches).padding(horizontal = PcSpace.md),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text("SEE FIXTURES", style = PcType.Tag, color = Pc.Accent, modifier = Modifier.weight(1f), maxLines = 1)
                Icon(Icons.AutoMirrored.Outlined.KeyboardArrowRight, null, tint = Pc.Accent, modifier = Modifier.size(20.dp))
            }
        }
    }
}

/** Spoiler Shield banner, one compact row: icon, what is hidden, and "I'm caught up" (48dp target). */
@Composable
fun ShieldBanner(count: Int, onCaughtUp: () -> Unit) {
    PcCard(Modifier.fillMaxWidth(), container = Pc.AccentContainer, border = Pc.Accent.copy(alpha = 0.35f)) {
        Row(
            Modifier.fillMaxWidth().heightIn(min = 56.dp).padding(start = PcSpace.md, end = PcSpace.xxs, top = PcSpace.xxs, bottom = PcSpace.xxs),
            horizontalArrangement = Arrangement.spacedBy(PcSpace.sm), verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(Icons.Outlined.VisibilityOff, null, tint = Pc.Accent, modifier = Modifier.size(22.dp))
            Column(Modifier.weight(1f).semantics(mergeDescendants = true) { contentDescription = "Spoiler Shield is on. $count scores hidden." }) {
                Text("SPOILER SHIELD ON", style = PcType.Tag, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text("$count ${if (count == 1) "score" else "scores"} hidden", style = PcType.Caption, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
            Box(
                Modifier.heightIn(min = PcSpace.touch).pcClickable(PcShape.Chip, "I'm caught up", onClick = onCaughtUp).padding(horizontal = PcSpace.sm),
                contentAlignment = Alignment.Center,
            ) { Text("I'M CAUGHT UP", style = PcType.Tag, color = Pc.Accent, maxLines = 1) }
        }
    }
}

// ---------------- Previews ----------------

private val previewActions = HomeActions({}, {}, {}, {}, {}, {}, {}, {})

@Preview(name = "Home · dark", showBackground = true, widthDp = 412, heightDp = 780, backgroundColor = 0xFF0E1110)
@Composable
private fun HomeDarkPreview() = PreviewHost(dark = true) { HomeContent(UiState.Success(PreviewData.home), previewActions, {}, modifier = Modifier.fillMaxSize()) }

@Preview(name = "Home · light", showBackground = true, widthDp = 412, heightDp = 780)
@Composable
private fun HomeLightPreview() = PreviewHost(dark = false) { HomeContent(UiState.Success(PreviewData.home), previewActions, {}, modifier = Modifier.fillMaxSize()) }

@Preview(name = "Home · small phone", showBackground = true, widthDp = 360, heightDp = 500, backgroundColor = 0xFF0E1110)
@Composable
private fun HomeSmallPreview() = PreviewHost(dark = true) { HomeContent(UiState.Success(PreviewData.home), previewActions, {}, modifier = Modifier.fillMaxSize()) }

@Preview(name = "Home · large font", showBackground = true, widthDp = 360, heightDp = 500, fontScale = 1.6f, backgroundColor = 0xFF0E1110)
@Composable
private fun HomeLargeFontPreview() = PreviewHost(dark = true) { HomeContent(UiState.Success(PreviewData.home), previewActions, {}, modifier = Modifier.fillMaxSize()) }

@Preview(name = "Home · no live", showBackground = true, widthDp = 360, heightDp = 640, backgroundColor = 0xFF0E1110)
@Composable
private fun HomeNoLivePreview() = PreviewHost(dark = true) {
    HomeContent(UiState.Success(PreviewData.home.copy(matches = PreviewData.matches.filter { it.status != Status.Live })), previewActions, {}, modifier = Modifier.fillMaxSize())
}

@Preview(name = "Home · loading", showBackground = true, widthDp = 360, heightDp = 640, backgroundColor = 0xFF0E1110)
@Composable
private fun HomeLoadingPreview() = PreviewHost(dark = true) { HomeContent(UiState.Loading, previewActions, {}, modifier = Modifier.fillMaxSize()) }

@Preview(name = "Home · error", showBackground = true, widthDp = 360, heightDp = 640, backgroundColor = 0xFF0E1110)
@Composable
private fun HomeErrorPreview() = PreviewHost(dark = true) { HomeContent(UiState.Error(AppError.Network()), previewActions, {}, modifier = Modifier.fillMaxSize()) }
