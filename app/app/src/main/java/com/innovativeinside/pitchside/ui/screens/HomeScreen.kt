package com.innovativeinside.pitchside.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.VisibilityOff
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.innovativeinside.pitchside.LocalApp
import com.innovativeinside.pitchside.data.Match
import com.innovativeinside.pitchside.data.Status
import com.innovativeinside.pitchside.ui.components.BannerKind
import com.innovativeinside.pitchside.ui.components.ChipRow
import com.innovativeinside.pitchside.ui.components.Crest
import com.innovativeinside.pitchside.ui.components.EmptyState
import com.innovativeinside.pitchside.ui.components.IconAction
import com.innovativeinside.pitchside.ui.components.InfoBanner
import com.innovativeinside.pitchside.ui.components.MatchCard
import com.innovativeinside.pitchside.ui.components.MatchCardSkeleton
import com.innovativeinside.pitchside.ui.components.PcCard
import com.innovativeinside.pitchside.ui.components.PrimaryButton
import com.innovativeinside.pitchside.ui.components.SectionHeader
import com.innovativeinside.pitchside.ui.components.SegmentedTabs
import com.innovativeinside.pitchside.ui.components.pcClickable
import com.innovativeinside.pitchside.ui.theme.Pc
import com.innovativeinside.pitchside.ui.theme.PcShape
import com.innovativeinside.pitchside.ui.theme.PcType
import kotlinx.coroutines.delay

class HomeActions(
    val openMatch: (String) -> Unit,
    val openSearch: () -> Unit,
    val openAsk: () -> Unit,
    val openNews: () -> Unit,
    val openLeagues: () -> Unit,
    val openMatches: () -> Unit,
)

@Composable
fun HomeScreen(actions: HomeActions) {
    val app = LocalApp.current
    var tab by rememberSaveable { mutableStateOf(0) }
    var league by rememberSaveable { mutableStateOf("All") }
    var loading by remember { mutableStateOf(true) }
    LaunchedEffect(Unit) { delay(500); loading = false }

    val all = app.repo.matches()
    val leagues = listOf("All") + app.repo.leagues().map { it.name }
    val inLeague = all.filter { league == "All" || it.league == league }
    fun group(m: Match) = when (m.status) { Status.Live -> 0; Status.Finished -> 2; else -> 1 }
    val counts = (0..2).map { g -> inLeague.count { group(it) == g } }
    val shown = inLeague.filter { group(it) == tab }
    val featured = all.firstOrNull { it.status == Status.Live && it.isFollowed(app.followedTeams) } ?: all.firstOrNull { it.status == Status.Live }
    val wide = widthClass() == WidthClass.Expanded

    Screen(maxWidth = if (wide) 1100 else 720) {
        Row(Modifier.fillMaxWidth().padding(start = 20.dp, end = 12.dp, top = 18.dp, bottom = 4.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text("PITCHSIDE  /  MATCHDAY", style = PcType.Overline.copy(color = Pc.Lime))
                Text("Matchday", style = PcType.Title.copy(fontSize = 32.sp, lineHeight = 38.sp))
                Text("${all.count { it.status == Status.Live }} live · ${all.size} matches today", style = PcType.BodySmall)
            }
            IconAction(Icons.Outlined.AutoAwesome, "Ask Pitchside", actions.openAsk, tint = Pc.Lime)
            IconAction(Icons.Outlined.Search, "Search", actions.openSearch, filled = false)
        }
        Spacer(Modifier.height(16.dp))
        ChipRow(leagues, league, { league = it })
        Spacer(Modifier.height(16.dp))

        if (app.shielded.isNotEmpty() && app.revealed.size < all.size && all.any { app.isHidden(it) }) {
            ShieldBanner(
                modifier = Modifier.padding(horizontal = 16.dp),
                onCaughtUp = app::caughtUp,
            )
            Spacer(Modifier.height(16.dp))
        }

        if (wide) {
            Row(Modifier.padding(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(24.dp)) {
                Column(Modifier.weight(1.6f), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    if (featured != null) HeroMatch(featured, app.isHidden(featured), { actions.openMatch(featured.id) }, { app.reveal(featured.id) })
                    MatchesSection(tab, { tab = it }, counts, shown, loading, league, { league = "All" }, actions)
                }
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(12.dp)) { SidePanels(actions) }
            }
        } else {
            Column(Modifier.padding(horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                if (featured != null) HeroMatch(featured, app.isHidden(featured), { actions.openMatch(featured.id) }, { app.reveal(featured.id) })
                MatchesSection(tab, { tab = it }, counts, shown, loading, league, { league = "All" }, actions)
                SidePanels(actions)
            }
        }
    }
}

@Composable
private fun SidePanels(actions: HomeActions) {
    val app = LocalApp.current
    SectionHeader("Latest news", "All news", actions.openNews)
    app.repo.news().take(2).forEach { NewsRow(it, actions.openNews) }
    Spacer(Modifier.height(4.dp))
    SectionHeader("Premier Division", "Full table", actions.openLeagues)
    StandingsPreview(app.repo.standings("pd"), app.followedTeams.toSet(), actions.openLeagues)
}

@Composable
private fun MatchesSection(
    tab: Int, onTab: (Int) -> Unit, counts: List<Int>, shown: List<Match>, loading: Boolean, league: String, onResetLeague: () -> Unit, actions: HomeActions,
) {
    val app = LocalApp.current
    SectionHeader("Matches", "See all", actions.openMatches)
    SegmentedTabs(listOf("Live ${counts[0]}", "Upcoming ${counts[1]}", "Finished ${counts[2]}"), tab, onTab)
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        when {
            loading -> repeat(3) { MatchCardSkeleton() }
            shown.isEmpty() -> EmptyState(
                title = when (tab) { 0 -> "No live matches in $league"; 1 -> "Nothing scheduled in $league"; else -> "No finished matches in $league" },
                body = when (tab) { 0 -> "Next kickoff 20:45, Harbor City v Redmont."; 1 -> "New fixtures appear as soon as they are announced."; else -> "Results show here once a match ends." },
                actionLabel = if (league != "All") "Show all competitions" else null, onAction = onResetLeague,
            )
            else -> shown.forEach { m -> MatchCard(m, app.isHidden(m), { actions.openMatch(m.id) }, { app.reveal(m.id) }) }
        }
    }
}

@Composable
fun ShieldBanner(modifier: Modifier = Modifier, onCaughtUp: () -> Unit) {
    PcCard(modifier.fillMaxWidth(), container = Pc.LimeTintSoft, border = Pc.Lime.copy(alpha = 0.45f)) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.size(40.dp).clip(PcShape.Control).background(Pc.Raised), contentAlignment = Alignment.Center) { Icon(Icons.Outlined.VisibilityOff, null, tint = Pc.Lime) }
                Column {
                    Text("Spoiler Shield is on", style = PcType.Subhead)
                    Text("Scores for your shielded teams are hidden. Alerts are held.", style = PcType.BodySmall)
                }
            }
            PrimaryButton("I'm caught up", onCaughtUp, Modifier.fillMaxWidth())
        }
    }
}

@Composable
fun HeroMatch(match: Match, hidden: Boolean, onClick: () -> Unit, onReveal: () -> Unit, modifier: Modifier = Modifier) {
    val desc = "Featured match: ${match.home.name} versus ${match.away.name}, ${match.label}" +
        if (hidden) ", score hidden" else ", ${match.homeScore} to ${match.awayScore}"
    val pitchLine = Pc.PitchLine.copy(alpha = 0.48f)
    PcCard(modifier.fillMaxWidth().semantics(mergeDescendants = true) { contentDescription = desc }, shape = PcShape.Hero, container = Pc.Pitch, border = Pc.PitchLine, onClick = onClick) {
        Box(Modifier.fillMaxWidth().drawBehind {
            val stroke = 1.dp.toPx()
            val inset = 14.dp.toPx()
            drawRoundRect(pitchLine, topLeft = Offset(inset, inset), size = Size(size.width - inset * 2, size.height - inset * 2), cornerRadius = CornerRadius(18.dp.toPx()), style = Stroke(stroke))
        }) {
        Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(match.league, style = PcType.Label.copy(color = Pc.OnPitch, fontWeight = FontWeight.Bold), maxLines = 1)
                    Text(match.venue, style = PcType.Meta.copy(color = Pc.OnPitch.copy(alpha = 0.76f)), maxLines = 1)
                }
                Row(Modifier.clip(PcShape.Badge).background(if (match.status == Status.Live) Pc.LiveTint else Pc.OnPitch.copy(alpha = 0.12f)).padding(horizontal = 10.dp, vertical = 7.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    if (match.status == Status.Live) Box(Modifier.size(7.dp).clip(CircleShape).background(Pc.Live))
                    Text(if (match.status == Status.Live) "LIVE  ${match.label}" else match.label, color = if (match.status == Status.Live) Pc.Live else Pc.OnPitch, style = PcType.Meta, fontWeight = FontWeight.ExtraBold, maxLines = 1)
                }
            }
            Row(Modifier.fillMaxWidth().padding(vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Crest(match.home, 52.dp); Text(match.home.name, style = PcType.Label.copy(fontSize = 15.sp, fontWeight = FontWeight.ExtraBold, color = Pc.OnPitch), maxLines = 1, overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis)
                }
                if (hidden) Box(Modifier.size(width = 104.dp, height = 56.dp).clip(PcShape.Control).background(Pc.Raised).pcClickable(PcShape.Control, "Reveal score", onClick = onReveal), contentAlignment = Alignment.Center) {
                    Text("Reveal", style = PcType.Label.copy(color = Pc.Lime))
                } else Text(if (match.homeScore != null) "${match.homeScore} – ${match.awayScore}" else match.label, style = PcType.ScoreXL.copy(fontSize = 58.sp, lineHeight = 58.sp, color = Pc.OnPitch))
                Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Crest(match.away, 52.dp); Text(match.away.name, style = PcType.Label.copy(fontSize = 15.sp, fontWeight = FontWeight.ExtraBold, color = Pc.OnPitch.copy(alpha = 0.82f)), maxLines = 1, overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis)
                }
            }
            Box(Modifier.fillMaxWidth().height(1.dp).background(Pc.PitchLine))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.Top) {
                Icon(Icons.Outlined.AutoAwesome, null, tint = Pc.Lime, modifier = Modifier.size(18.dp).padding(top = 2.dp))
                Text(if (hidden) "Brief hidden to avoid spoilers." else match.brief.substringBefore(". ").let { if (it.endsWith(".")) it else "$it." }, style = PcType.BodySmall.copy(color = Pc.OnPitch), modifier = Modifier.weight(1f))
            }
        }
        }
    }
}
