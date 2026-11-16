package com.innovativeinside.pitchside.ui.screens

import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.innovativeinside.pitchside.ui.components.NewsHeroCard
import com.innovativeinside.pitchside.ui.components.NewsRow
import com.innovativeinside.pitchside.ui.components.SectionHeader
import com.innovativeinside.pitchside.ui.components.glowBackground
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.OpenInNew
import androidx.compose.material.icons.outlined.Newspaper
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.innovativeinside.pitchside.core.UiState
import com.innovativeinside.pitchside.data.model.NewsEdition
import com.innovativeinside.pitchside.data.model.NewsItem
import com.innovativeinside.pitchside.ui.components.AppTopBar
import com.innovativeinside.pitchside.ui.components.BackButton
import com.innovativeinside.pitchside.ui.components.DetailSkeleton
import com.innovativeinside.pitchside.ui.components.EmptyState
import com.innovativeinside.pitchside.ui.components.ErrorState
import com.innovativeinside.pitchside.ui.components.FilterChipRow
import com.innovativeinside.pitchside.ui.components.Hairline
import com.innovativeinside.pitchside.ui.components.IconAction
import com.innovativeinside.pitchside.ui.components.NewsImage
import com.innovativeinside.pitchside.ui.components.NewsSkeleton
import com.innovativeinside.pitchside.ui.components.Overline
import com.innovativeinside.pitchside.ui.components.PrimaryButton
import com.innovativeinside.pitchside.ui.components.StateContent
import com.innovativeinside.pitchside.ui.components.newsDate
import com.innovativeinside.pitchside.ui.preview.PreviewData
import com.innovativeinside.pitchside.ui.preview.PreviewHost
import com.innovativeinside.pitchside.ui.theme.Pc
import com.innovativeinside.pitchside.ui.theme.PcShape
import com.innovativeinside.pitchside.ui.theme.PcSpace
import com.innovativeinside.pitchside.ui.theme.PcType
import com.innovativeinside.pitchside.ui.vm.ArticleViewModel
import com.innovativeinside.pitchside.ui.vm.NewsState
import com.innovativeinside.pitchside.ui.vm.NewsViewModel


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NewsRoute(openArticle: (String) -> Unit, vm: NewsViewModel = viewModel(factory = NewsViewModel.Factory)) {
    val s by vm.state.collectAsStateWithLifecycle()
    PullToRefreshBox(isRefreshing = s.refreshing, onRefresh = vm::refresh, modifier = Modifier.fillMaxSize().background(Pc.Bg)) {
        NewsContent(s, vm::setEdition, vm::retry, vm::loadMore, openArticle)
    }
}

/**
 * News tab: 90min headlines with edition chips (All, France, Italy, Spain, Brazil), a lead story and a
 * compact list that loads the next page as you near the end. Dates are the feed's real publish dates.
 */
@Composable
fun NewsContent(s: NewsState, onEdition: (NewsEdition?) -> Unit, onRetry: () -> Unit, onMore: () -> Unit, openArticle: (String) -> Unit) {
    val list = rememberLazyListState()
    val nearEnd by remember { derivedStateOf { list.layoutInfo.visibleItemsInfo.lastOrNull()?.index?.let { it >= list.layoutInfo.totalItemsCount - 3 } == true } }
    LaunchedEffect(nearEnd, s.items.size) { if (nearEnd && s.nextCursor != null && !s.loadingMore && s.loadMoreError == null) onMore() }
    val labels = listOf("All") + NewsEdition.entries.map { it.label }
    Box(Modifier.fillMaxSize().glowBackground().statusBarsPadding(), contentAlignment = Alignment.TopCenter) {
        LazyColumn(Modifier.widthIn(max = 720.dp).fillMaxSize().testTag("news_list"), state = list, contentPadding = PaddingValues(bottom = PcSpace.lg)) {
            item(key = "top") { AppTopBar("News", subtitle = "From 90min · France, Italy, Spain, Brazil") }
            item(key = "chips") {
                FilterChipRow(labels, s.edition?.label ?: "All", { label -> onEdition(NewsEdition.entries.firstOrNull { it.label == label }) }, Modifier.padding(vertical = PcSpace.xs))
            }
            when {
                s.loading -> item(key = "loading") { NewsSkeleton(Modifier.padding(PcSpace.screen)) }
                s.error != null -> item(key = "error") { ErrorState(s.error, onRetry, Modifier.padding(PcSpace.screen).testTag("state_error")) }
                s.items.isEmpty() -> item(key = "empty") {
                    EmptyState(
                        "No stories yet", "New stories from this edition appear as soon as 90min publishes them.",
                        Modifier.padding(PcSpace.screen), actionLabel = if (s.edition != null) "Show all editions" else null, onAction = { onEdition(null) }, icon = Icons.Outlined.Newspaper,
                    )
                }
                else -> {
                    val lead = s.items.first()
                    item(key = "lead-${lead.id}") { NewsHeroCard(lead, { openArticle(lead.id) }, Modifier.padding(horizontal = PcSpace.screen, vertical = PcSpace.xs), aspect = 4f / 3.2f, headlineLines = 4) }
                    if (s.items.size > 1) item(key = "latest-header") { SectionHeader("Latest stories", modifier = Modifier.padding(horizontal = PcSpace.screen).padding(top = PcSpace.xs)) }
                    itemsIndexed(s.items.drop(1), key = { _, n -> n.id }) { i, n ->
                        Column(Modifier.padding(horizontal = PcSpace.screen)) {
                            if (i > 0) Hairline()
                            NewsRow(n, { openArticle(n.id) })
                        }
                    }
                    item(key = "footer") {
                        Box(Modifier.fillMaxWidth().padding(PcSpace.md), contentAlignment = Alignment.Center) {
                            when {
                                s.loadingMore -> CircularProgressIndicator(color = Pc.Accent, strokeWidth = 2.dp, modifier = Modifier.testTag("load_more"))
                                s.loadMoreError != null -> ErrorState(s.loadMoreError, onMore)
                                s.nextCursor != null -> Spacer(Modifier.height(PcSpace.xl))
                                else -> Text("You're all caught up.", style = PcType.Caption)
                            }
                        }
                    }
                }
            }
        }
    }
}

// ---------------- Article ----------------

fun shareStory(ctx: Context, item: NewsItem) {
    val send = Intent(Intent.ACTION_SEND).setType("text/plain").putExtra(Intent.EXTRA_SUBJECT, item.title)
        .putExtra(Intent.EXTRA_TEXT, listOfNotNull(item.title, item.url).joinToString("\n"))
    ctx.startActivity(Intent.createChooser(send, "Share story"))
}

fun openInBrowser(ctx: Context, url: String) {
    try { ctx.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)).addCategory(Intent.CATEGORY_BROWSABLE)) } catch (_: ActivityNotFoundException) { }
}

@Composable
fun ArticleRoute(back: () -> Unit, vm: ArticleViewModel = viewModel(factory = ArticleViewModel.Factory)) {
    val state by vm.state.collectAsStateWithLifecycle()
    val ctx = LocalContext.current
    val item = (state as? UiState.Success)?.data
    Box(Modifier.fillMaxSize().glowBackground()) {
        Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()), horizontalAlignment = Alignment.CenterHorizontally) {
            Column(Modifier.widthIn(max = 680.dp).fillMaxWidth()) {
                if (item?.imageUrl == null) Spacer(Modifier.statusBarsPadding().height(64.dp))
                StateContent(state, vm::retry, loading = { DetailSkeleton(Modifier.statusBarsPadding().padding(top = 56.dp)) }) { a -> ArticleBody(a) }
                Spacer(Modifier.height(PcSpace.lg))
            }
        }
        // Floating back / share over the image header.
        Row(Modifier.fillMaxWidth().statusBarsPadding().padding(horizontal = PcSpace.xs, vertical = PcSpace.xxs), verticalAlignment = Alignment.CenterVertically) {
            BackButton(back)
            Spacer(Modifier.weight(1f))
            if (item != null) IconAction(Icons.Outlined.Share, "Share story", { shareStory(ctx, item) }, filled = true)
        }
    }
}

/**
 * The story as the feed publishes it: a big edge-to-edge image header with a dark gradient and the kicker and
 * headline over it, then author and date, the description, and the way out to the full article. The full article
 * lives on 90min, one tap away; it is not scraped into the app.
 */
@Composable
fun ArticleBody(a: NewsItem) {
    val ctx = LocalContext.current
    Column(verticalArrangement = Arrangement.spacedBy(PcSpace.sm)) {
        if (a.imageUrl != null) {
            Box(Modifier.fillMaxWidth().aspectRatio(4f / 3.4f)) {
                NewsImage(a.imageUrl, Modifier.fillMaxSize(), shape = androidx.compose.ui.graphics.RectangleShape, description = a.imageCaption)
                Box(Modifier.fillMaxSize().background(Brush.verticalGradient(0f to Color.Black.copy(alpha = 0.45f), 0.3f to Color.Transparent, 0.55f to Color.Transparent, 1f to Pc.Bg)))
                Column(Modifier.align(Alignment.BottomStart).padding(horizontal = PcSpace.lg, vertical = PcSpace.xs), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(a.kicker.uppercase(), style = PcType.Tag, color = Pc.Accent)
                    Text(a.title, style = PcType.Title.copy(lineHeight = 32.sp), modifier = Modifier.semantics { heading() })
                }
            }
            a.imageCaption?.let { Text(it, style = PcType.Caption, maxLines = 2, modifier = Modifier.padding(horizontal = PcSpace.lg)) }
        }
        Column(Modifier.padding(horizontal = PcSpace.lg), verticalArrangement = Arrangement.spacedBy(PcSpace.sm)) {
            if (a.imageUrl == null) {
                Text(a.kicker.uppercase(), style = PcType.Tag, color = Pc.Accent)
                Text(a.title, style = PcType.Title.copy(lineHeight = 32.sp), modifier = Modifier.semantics { heading() })
            }
            Text(listOfNotNull(a.author?.takeIf { it.isNotBlank() }, a.source.ifBlank { null }, newsDate(a.publishedMs).ifBlank { null }).joinToString(" · ").uppercase(), style = PcType.Kicker.copy(fontSize = 12.sp))
            Hairline()
            if (a.dek.isNotBlank()) Text(a.dek, style = PcType.Reading.copy(fontWeight = FontWeight.SemiBold))
            a.body.split("\n\n").filter { it.isNotBlank() }.forEach { Text(it, style = PcType.Reading, color = Pc.Text) }
            if (a.url != null) {
                Spacer(Modifier.height(PcSpace.xs))
                PrimaryButton(if (a.edition != null) "Read on 90min" else "Read at source", { openInBrowser(ctx, a.url) }, Modifier.fillMaxWidth().testTag("read_source"), icon = Icons.AutoMirrored.Outlined.OpenInNew)
                if (a.edition != null) Text("The full story opens on 90min.com in your browser.", style = PcType.Caption)
            }
        }
    }
}

@Preview(name = "News · success", showBackground = true, widthDp = 380, heightDp = 900)
@Composable
private fun NewsPreview() = PreviewHost(dark = true) { NewsContent(NewsState(items = PreviewData.news, loading = false), {}, {}, {}, {}) }

@Preview(name = "News · error · dark", showBackground = true, widthDp = 380, backgroundColor = 0xFF0E1110)
@Composable
private fun NewsErrorPreview() = PreviewHost(dark = true) { NewsContent(NewsState(loading = false, error = com.innovativeinside.pitchside.core.AppError.Network()), {}, {}, {}, {}) }

@Preview(name = "Article", showBackground = true, widthDp = 380, heightDp = 900)
@Composable
private fun ArticlePreview() = PreviewHost(dark = false) { Column { ArticleBody(PreviewData.news.first()) } }
