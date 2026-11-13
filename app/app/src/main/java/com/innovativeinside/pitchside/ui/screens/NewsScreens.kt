package com.innovativeinside.pitchside.ui.screens

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Newspaper
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.innovativeinside.pitchside.R
import com.innovativeinside.pitchside.data.model.NewsCategory
import com.innovativeinside.pitchside.data.model.NewsItem
import com.innovativeinside.pitchside.ui.components.BackButton
import com.innovativeinside.pitchside.ui.components.ChipRow
import com.innovativeinside.pitchside.ui.components.DetailSkeleton
import com.innovativeinside.pitchside.ui.components.EmptyState
import com.innovativeinside.pitchside.ui.components.ErrorState
import com.innovativeinside.pitchside.ui.components.IconAction
import com.innovativeinside.pitchside.ui.components.ListSkeleton
import com.innovativeinside.pitchside.ui.components.Overline
import com.innovativeinside.pitchside.ui.components.PcCard
import com.innovativeinside.pitchside.ui.components.SecondaryButton
import com.innovativeinside.pitchside.ui.components.StateContent
import com.innovativeinside.pitchside.ui.components.TextAction
import com.innovativeinside.pitchside.ui.preview.PreviewData
import com.innovativeinside.pitchside.ui.preview.PreviewHost
import com.innovativeinside.pitchside.ui.theme.Pc
import com.innovativeinside.pitchside.ui.theme.PcShape
import com.innovativeinside.pitchside.ui.theme.PcType
import com.innovativeinside.pitchside.ui.vm.ArticleViewModel
import com.innovativeinside.pitchside.ui.vm.NewsState
import com.innovativeinside.pitchside.ui.vm.NewsViewModel

private val categories = listOf<NewsCategory?>(null) + NewsCategory.entries

@Composable
fun NewsRoute(openArticle: (String) -> Unit, vm: NewsViewModel = viewModel(factory = NewsViewModel.Factory)) {
    val s by vm.state.collectAsStateWithLifecycle()
    RefreshScreen(s.refreshing, vm::refresh, null, {}) {
        NewsContent(s, vm::setCategory, vm::retry, vm::loadMore, openArticle)
    }
}

@Composable
fun NewsContent(s: NewsState, onCategory: (NewsCategory?) -> Unit, onRetry: () -> Unit, onMore: () -> Unit, openArticle: (String) -> Unit) {
    Column(Modifier.padding(start = 16.dp, end = 16.dp, top = 16.dp)) {
        Text("News", style = PcType.Title.copy(fontSize = 30.sp, lineHeight = 36.sp))
        Text("Reports, transfers and analysis", style = PcType.BodyMuted)
    }
    Spacer(Modifier.height(16.dp))
    ChipRow(categories.map { it?.label ?: "All" }, s.category?.label ?: "All", { label -> onCategory(categories.firstOrNull { (it?.label ?: "All") == label }) })
    Spacer(Modifier.height(16.dp))
    Column(Modifier.padding(horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        when {
            s.loading -> ListSkeleton()
            s.error != null -> ErrorState(s.error, onRetry)
            s.items.isEmpty() -> EmptyState("No stories yet", "New stories in this section appear as soon as they're published.", actionLabel = if (s.category != null) "Show all news" else null, onAction = { onCategory(null) }, icon = Icons.Outlined.Newspaper)
            else -> {
                val f = s.items.first()
                PcCard(Modifier.fillMaxWidth(), shape = PcShape.Hero, onClick = { openArticle(f.id) }) {
                    Column {
                        NewsThumb(f.category, Modifier.fillMaxWidth().height(140.dp))
                        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Overline(f.category.label.uppercase())
                            Text(f.title, style = PcType.Subtitle.copy(fontSize = 22.sp, lineHeight = 28.sp))
                            Text(f.dek, style = PcType.BodyMuted)
                            Text("${timeAgo(f.publishedMs)} · ${f.readMinutes} min read", style = PcType.Caption)
                        }
                    }
                }
                s.items.drop(1).forEach { NewsRow(it, { openArticle(it.id) }) }
                when {
                    s.loadingMore -> Box(Modifier.fillMaxWidth().padding(16.dp), contentAlignment = Alignment.Center) { CircularProgressIndicator(color = Pc.Accent) }
                    s.loadMoreError != null -> ErrorState(s.loadMoreError, onMore)
                    s.nextCursor != null -> SecondaryButton(stringResource(R.string.action_load_more), onMore, Modifier.fillMaxWidth().testTag("load_more"))
                    else -> Text("You're all caught up.", style = PcType.Caption, modifier = Modifier.padding(8.dp))
                }
            }
        }
    }
}

@Composable
fun ArticleRoute(back: () -> Unit, vm: ArticleViewModel = viewModel(factory = ArticleViewModel.Factory)) {
    val state by vm.state.collectAsStateWithLifecycle()
    val ctx = LocalContext.current
    Screen(maxWidth = 680) {
        Row(Modifier.fillMaxWidth().padding(8.dp), verticalAlignment = Alignment.CenterVertically) {
            BackButton(back)
            Spacer(Modifier.weight(1f))
            val item = (state as? com.innovativeinside.pitchside.core.UiState.Success)?.data
            if (item != null) IconAction(Icons.Outlined.Share, "Share story", {
                val send = Intent(Intent.ACTION_SEND).setType("text/plain").putExtra(Intent.EXTRA_TEXT, listOfNotNull(item.title, item.url).joinToString("\n"))
                ctx.startActivity(Intent.createChooser(send, "Share story"))
            })
        }
        StateContent(state, vm::retry, Modifier.padding(horizontal = 20.dp), loading = { DetailSkeleton() }) { a -> ArticleBody(a) }
    }
}

@Composable
fun ArticleBody(a: NewsItem) {
    val ctx = LocalContext.current
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Overline(a.category.label.uppercase())
        Text(a.title, style = PcType.Title)
        Text(a.dek, style = PcType.Subtitle.copy(color = Pc.Text2))
        Text("${a.source} · ${timeAgo(a.publishedMs)} · ${a.readMinutes} min read", style = PcType.Caption)
        a.body.split("\n\n").forEach { Text(it, style = PcType.Body.copy(lineHeight = 26.sp)) }
        if (a.url != null) TextAction("Read at source", { ctx.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(a.url))) })
    }
}

@Preview(name = "News · success", showBackground = true, backgroundColor = 0xFF07111F)
@Composable
private fun NewsPreview() = PreviewHost { Column { NewsContent(NewsState(items = PreviewData.news, loading = false), {}, {}, {}, {}) } }

@Preview(name = "News · error", showBackground = true, backgroundColor = 0xFF07111F)
@Composable
private fun NewsErrorPreview() = PreviewHost { Column { NewsContent(NewsState(loading = false, error = com.innovativeinside.pitchside.core.AppError.Server(503)), {}, {}, {}, {}) } }
