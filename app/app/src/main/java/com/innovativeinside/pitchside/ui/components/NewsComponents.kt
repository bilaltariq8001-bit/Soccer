package com.innovativeinside.pitchside.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Newspaper
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import coil.compose.SubcomposeAsyncImage
import coil.request.ImageRequest
import com.innovativeinside.pitchside.data.model.NewsItem
import com.innovativeinside.pitchside.data.news.NewsImages
import com.innovativeinside.pitchside.ui.preview.PreviewData
import com.innovativeinside.pitchside.ui.preview.PreviewHost
import com.innovativeinside.pitchside.ui.theme.Pc
import com.innovativeinside.pitchside.ui.theme.PcShape
import com.innovativeinside.pitchside.ui.theme.PcSpace
import com.innovativeinside.pitchside.ui.theme.PcType
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

/**
 * Honest story date: "Just now", "3 h ago", "Yesterday", "4 days ago" within a week, otherwise the date
 * itself ("13 Jul", or "11 Sep 2025" in another year). The 90min feeds can be months old; never fake recency.
 */
fun newsDate(ms: Long, now: Long = System.currentTimeMillis(), zone: ZoneId = ZoneId.systemDefault()): String {
    if (ms <= 0) return ""
    val m = ((now - ms) / 60_000).coerceAtLeast(0)
    return when {
        m < 1 -> "Just now"
        m < 60 -> "$m min ago"
        m < 24 * 60 -> "${m / 60} h ago"
        m < 48 * 60 -> "Yesterday"
        m < 7 * 24 * 60 -> "${m / (24 * 60)} days ago"
        else -> {
            val d = Instant.ofEpochMilli(ms).atZone(zone)
            val sameYear = d.year == Instant.ofEpochMilli(now).atZone(zone).year
            d.format(DateTimeFormatter.ofPattern(if (sameYear) "d MMM" else "d MMM yyyy", Locale.getDefault()))
        }
    }
}

/** "90min · Ilies Peeters · 13 Jul" */
fun newsMeta(item: NewsItem): String = listOfNotNull(item.source.ifBlank { null }, item.author?.takeIf { it.isNotBlank() && it != item.source }, newsDate(item.publishedMs).ifBlank { null }).joinToString(" · ")

/**
 * News photo through Coil (memory + disk cache). Requests the CDN size that matches the drawn width and
 * falls back to a neutral placeholder with a newspaper icon while loading or when there is no image.
 */
@Composable
fun NewsImage(url: String?, modifier: Modifier = Modifier, shape: Shape = PcShape.Control, description: String? = null) {
    BoxWithConstraints(modifier.clip(shape).background(Pc.SurfaceVariant), contentAlignment = Alignment.Center) {
        val px = with(LocalDensity.current) { maxWidth.toPx().toInt() }
        val placeholder = @Composable {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { Icon(Icons.Outlined.Newspaper, null, tint = Pc.Text3, modifier = Modifier.size(28.dp)) }
        }
        val sized = NewsImages.sized(url, px)
        if (sized == null) placeholder()
        else SubcomposeAsyncImage(
            model = ImageRequest.Builder(LocalContext.current).data(sized).crossfade(true).build(),
            contentDescription = description,
            contentScale = ContentScale.Crop,
            loading = { placeholder() },
            error = { placeholder() },
            modifier = Modifier.fillMaxSize(),
        )
    }
}

/**
 * NewsHeroCard (reference "LATEST NEWS"): full-bleed photo, dark bottom gradient, an optional uppercase label on
 * top, the headline in bold white and the honest publish time ("3 h ago"). Text sits on the gradient, so it reads in
 * both themes.
 */
@Composable
fun NewsHeroCard(item: NewsItem, onClick: () -> Unit, modifier: Modifier = Modifier, label: String? = null, aspect: Float? = 4f / 3f, headlineLines: Int = 3) {
    val scrim = Pc.Scrim
    Box(
        modifier.fillMaxWidth().let { if (aspect != null) it.aspectRatio(aspect) else it.heightIn(min = 190.dp) }.semantics(mergeDescendants = true) { contentDescription = "${item.kicker}. ${item.title}. ${newsMeta(item)}" }
            .pcClickable(PcShape.Tile, pressScale = true, onClick = onClick).border(1.dp, Pc.Outline, PcShape.Tile),
    ) {
        NewsImage(item.imageUrl, Modifier.fillMaxSize(), shape = PcShape.Tile)
        Box(Modifier.fillMaxSize().background(Brush.verticalGradient(0f to Color.Transparent, 0.35f to scrim.copy(alpha = 0.15f), 1f to Color.Black.copy(alpha = 0.88f))))
        if (label != null) Text(label.uppercase(), style = PcType.Tag.copy(fontSize = 17.sp), color = Color.White, modifier = Modifier.align(Alignment.TopStart).padding(PcSpace.md))
        Column(Modifier.align(Alignment.BottomStart).padding(PcSpace.md), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            if (label == null) Text(item.kicker.uppercase(), style = PcType.Overline, color = Pc.HeroAccent, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(item.title, style = PcType.Headline.copy(fontWeight = FontWeight.ExtraBold, lineHeight = 24.sp), color = Color.White, maxLines = headlineLines, overflow = TextOverflow.Ellipsis)
            Text(newsMeta(item), style = PcType.Caption, color = Color.White.copy(alpha = 0.75f), maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
    }
}

/** Older name for the lead story. */
@Composable
fun NewsCardLead(item: NewsItem, onClick: () -> Unit, modifier: Modifier = Modifier) = NewsHeroCard(item, onClick, modifier)

/** NewsRow: rounded thumbnail on the left, emerald kicker, bold headline (3 lines) and the meta line. */
@Composable
fun NewsRow(item: NewsItem, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Row(
        modifier.fillMaxWidth().heightIn(min = 92.dp).pcClickable(PcShape.Control, onClick = onClick).padding(vertical = PcSpace.sm)
            .semantics(mergeDescendants = true) { contentDescription = "${item.kicker}. ${item.title}. ${newsMeta(item)}" },
        horizontalArrangement = Arrangement.spacedBy(PcSpace.sm),
    ) {
        NewsImage(item.imageUrl, Modifier.size(width = 96.dp, height = 72.dp))
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
            Overline(item.kicker, color = Pc.Accent)
            Text(item.title, style = PcType.BodyStrong.copy(fontWeight = FontWeight.Bold), maxLines = 3, overflow = TextOverflow.Ellipsis)
            Text(newsMeta(item), style = PcType.Caption, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
    }
}

/** Older name for [NewsRow]. */
@Composable
fun NewsCardCompact(item: NewsItem, onClick: () -> Unit, modifier: Modifier = Modifier) = NewsRow(item, onClick, modifier)

@Preview(name = "News cards", showBackground = true, widthDp = 380, backgroundColor = 0xFF0E1110)
@Composable
private fun NewsCardsPreview() = PreviewHost(dark = true) {
    Column(Modifier.padding(PcSpace.md), verticalArrangement = Arrangement.spacedBy(PcSpace.sm)) {
        NewsHeroCard(PreviewData.news.first(), {})
        PreviewData.news.drop(1).forEach { NewsRow(it, {}) }
    }
}
