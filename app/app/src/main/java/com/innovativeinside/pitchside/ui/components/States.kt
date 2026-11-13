package com.innovativeinside.pitchside.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CloudOff
import androidx.compose.material.icons.outlined.ErrorOutline
import androidx.compose.material.icons.outlined.Inbox
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.SearchOff
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.text.style.TextAlign
import com.innovativeinside.pitchside.ui.theme.PcSpace
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.innovativeinside.pitchside.R
import com.innovativeinside.pitchside.core.AppError
import com.innovativeinside.pitchside.core.UiState
import com.innovativeinside.pitchside.ui.theme.Pc
import com.innovativeinside.pitchside.ui.theme.PcShape
import com.innovativeinside.pitchside.ui.theme.PcType

/**
 * Renders a [UiState] with the app's standard loading, empty and error treatments. Every data screen
 * goes through this, so no screen can end up blank.
 */
@Composable
fun <T> StateContent(
    state: UiState<T>,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier,
    loading: @Composable () -> Unit = { ListSkeleton() },
    empty: @Composable () -> Unit = { EmptyState(stringResource(R.string.state_empty_title), stringResource(R.string.state_empty_body)) },
    content: @Composable (T) -> Unit,
) {
    Box(modifier) {
        when (state) {
            UiState.Loading -> Box(Modifier.testTag("state_loading")) { loading() }
            UiState.Empty -> Box(Modifier.testTag("state_empty")) { empty() }
            is UiState.Error -> ErrorState(state.error, onRetry, Modifier.testTag("state_error"))
            is UiState.Success -> content(state.data)
        }
    }
}

/** Error card with a plain explanation and, when the error is retryable, a Retry button. */
@Composable
fun ErrorState(error: AppError, onRetry: () -> Unit, modifier: Modifier = Modifier) {
    val (icon, title) = errorVisual(error)
    StateCard(
        icon = icon, title = title, body = error.message,
        actionLabel = if (error.retryable) stringResource(R.string.action_retry) else null, onAction = onRetry,
        modifier = modifier.semantics { liveRegion = LiveRegionMode.Polite },
        tint = Pc.Error,
    )
}

@Composable
private fun errorVisual(e: AppError): Pair<ImageVector, String> = when (e) {
    is AppError.Network -> Icons.Outlined.CloudOff to stringResource(R.string.error_offline_title)
    is AppError.Timeout -> Icons.Outlined.CloudOff to stringResource(R.string.error_timeout_title)
    is AppError.Unauthorized, is AppError.Forbidden -> Icons.Outlined.Lock to stringResource(R.string.error_auth_title)
    is AppError.NotFound -> Icons.Outlined.SearchOff to stringResource(R.string.error_notfound_title)
    else -> Icons.Outlined.ErrorOutline to stringResource(R.string.error_generic_title)
}

@Composable
fun StateCard(
    icon: ImageVector,
    title: String,
    body: String,
    modifier: Modifier = Modifier,
    actionLabel: String? = null,
    onAction: () -> Unit = {},
    tint: androidx.compose.ui.graphics.Color = Pc.Accent,
) {
    PcCard(modifier.fillMaxWidth()) {
        Column(Modifier.fillMaxWidth().padding(horizontal = PcSpace.lg, vertical = PcSpace.lg), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(PcSpace.xs)) {
            Box(Modifier.size(48.dp).clip(CircleShape).background(Pc.SurfaceVariant), contentAlignment = Alignment.Center) { Icon(icon, null, tint = tint, modifier = Modifier.size(24.dp)) }
            Spacer(Modifier.height(PcSpace.xxs))
            Text(title, style = PcType.Subtitle, textAlign = TextAlign.Center)
            Text(body, style = PcType.BodyMuted, textAlign = TextAlign.Center)
            if (actionLabel != null) { Spacer(Modifier.height(PcSpace.xs)); PrimaryButton(actionLabel, onAction, Modifier.testTag("retry_button")) }
        }
    }
}

/** Empty state: a load succeeded but there is nothing to show. */
@Composable
fun EmptyState(title: String, body: String, modifier: Modifier = Modifier, actionLabel: String? = null, onAction: () -> Unit = {}, icon: ImageVector = Icons.Outlined.Inbox) =
    StateCard(icon, title, body, modifier, actionLabel, onAction)

/** Skeleton list shown on first load (no spinners on content screens): rows inside one tile. */
@Suppress("ModifierParameter")
@Composable
fun ListSkeleton(rows: Int = 4, modifier: Modifier = Modifier) {
    PcCard(modifier.fillMaxWidth().semantics { liveRegion = LiveRegionMode.Polite; contentDescription = "Loading" }) {
        Column { repeat(rows) { i -> MatchCardSkeleton(); if (i < rows - 1) Hairline() } }
    }
}

/** Skeleton that mirrors the Home layout: hero, rail, two bento rows. */
@Composable
fun HomeSkeleton(modifier: Modifier = Modifier) {
    Column(modifier.fillMaxWidth().semantics { contentDescription = "Loading" }, verticalArrangement = Arrangement.spacedBy(PcSpace.gutter)) {
        Box(Modifier.fillMaxWidth().height(236.dp).skeleton(PcShape.Tile))
        Row(horizontalArrangement = Arrangement.spacedBy(PcSpace.xs)) { repeat(3) { Box(Modifier.weight(1f).height(112.dp).skeleton(PcShape.Tile)) } }
        repeat(2) { Row(horizontalArrangement = Arrangement.spacedBy(PcSpace.gutter)) { repeat(2) { Box(Modifier.weight(1f).height(156.dp).skeleton(PcShape.Tile)) } } }
    }
}

/** Skeleton that mirrors the News layout: lead card then compact rows. */
@Composable
fun NewsSkeleton(modifier: Modifier = Modifier) {
    Column(modifier.fillMaxWidth().semantics { contentDescription = "Loading" }, verticalArrangement = Arrangement.spacedBy(PcSpace.sm)) {
        Box(Modifier.fillMaxWidth().aspectRatio(16f / 9f).skeleton(PcShape.Tile))
        Box(Modifier.fillMaxWidth(0.8f).height(20.dp).skeleton())
        repeat(3) {
            Row(horizontalArrangement = Arrangement.spacedBy(PcSpace.sm)) {
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(PcSpace.xs)) { Box(Modifier.fillMaxWidth(0.3f).height(10.dp).skeleton()); Box(Modifier.fillMaxWidth().height(16.dp).skeleton()); Box(Modifier.fillMaxWidth(0.7f).height(16.dp).skeleton()) }
                Box(Modifier.size(width = 104.dp, height = 78.dp).skeleton(PcShape.Control))
            }
        }
    }
}

@Composable
fun DetailSkeleton(modifier: Modifier = Modifier) {
    Column(modifier.fillMaxWidth().padding(PcSpace.md).semantics { contentDescription = "Loading" }, verticalArrangement = Arrangement.spacedBy(PcSpace.md)) {
        Box(Modifier.fillMaxWidth().height(180.dp).skeleton(PcShape.Tile))
        Row(horizontalArrangement = Arrangement.spacedBy(PcSpace.xs)) { repeat(4) { Box(Modifier.weight(1f).height(32.dp).skeleton()) } }
        repeat(4) { Box(Modifier.fillMaxWidth().height(56.dp).skeleton(PcShape.Control)) }
    }
}
