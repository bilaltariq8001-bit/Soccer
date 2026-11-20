package com.innovativeinside.pitchside.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.fillMaxSize
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
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.border
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
    /** Fill the given height (single-viewport screens): content gets the full box, error and empty cards are centred. */
    fill: Boolean = false,
    content: @Composable (T) -> Unit,
) {
    // A Column, not a Box: screen bodies emit several siblings (header, tabs, sections) and a Box
    // would draw them on top of each other.
    // Crossfade between loading / empty / error / content (keyed on the kind of state, not the data, so a
    // refresh that keeps showing content never flickers). Instant when motion is reduced.
    val reduced = rememberReducedMotion()
    androidx.compose.animation.AnimatedContent(
        targetState = state,
        contentKey = { it::class },
        transitionSpec = {
            if (reduced) androidx.compose.animation.ContentTransform(androidx.compose.animation.EnterTransition.None, androidx.compose.animation.ExitTransition.None)
            else androidx.compose.animation.ContentTransform(androidx.compose.animation.fadeIn(androidx.compose.animation.core.tween(220)), androidx.compose.animation.fadeOut(androidx.compose.animation.core.tween(120)))
        },
        label = "state",
        modifier = modifier,
    ) { s ->
        if (fill) Box(Modifier.fillMaxSize()) {
            when (s) {
                UiState.Loading -> Box(Modifier.fillMaxSize().testTag("state_loading")) { loading() }
                UiState.Empty -> Box(Modifier.fillMaxSize().testTag("state_empty"), contentAlignment = Alignment.Center) { empty() }
                is UiState.Error -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { ErrorState(s.error, onRetry, Modifier.testTag("state_error")) }
                is UiState.Success -> content(s.data)
            }
        } else Column {
            when (s) {
                UiState.Loading -> Box(Modifier.testTag("state_loading")) { loading() }
                UiState.Empty -> Box(Modifier.testTag("state_empty")) { empty() }
                is UiState.Error -> ErrorState(s.error, onRetry, Modifier.testTag("state_error"))
                is UiState.Success -> content(s.data)
            }
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

/**
 * The shared state card (EmptyState and ErrorState): charcoal card, an icon in a glowing emerald disc, a condensed
 * title, a muted explanation and an optional pill action. Crossfades in with its parent state.
 */
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
            Box(
                Modifier.size(64.dp).background(androidx.compose.ui.graphics.Brush.radialGradient(listOf(tint.copy(alpha = 0.22f), androidx.compose.ui.graphics.Color.Transparent)), CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                Box(Modifier.size(48.dp).clip(CircleShape).background(Pc.SurfaceVariant).border(1.dp, tint.copy(alpha = 0.4f), CircleShape), contentAlignment = Alignment.Center) { Icon(icon, null, tint = tint, modifier = Modifier.size(24.dp)) }
            }
            Spacer(Modifier.height(PcSpace.xxs))
            Text(title, style = PcType.TeamName.copy(fontSize = 22.sp, lineHeight = 26.sp), textAlign = TextAlign.Center)
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

/**
 * Skeleton that mirrors the single-viewport Home: live hero, the top-matches rail and the today list, which takes
 * whatever height is left. Never taller than the box it is given.
 */
@Composable
fun HomeSkeleton(modifier: Modifier = Modifier) {
    androidx.compose.foundation.layout.BoxWithConstraints(modifier.fillMaxSize().semantics { contentDescription = "Loading" }) {
        val hero = (maxHeight * 0.36f).coerceIn(120.dp, 230.dp)
        val showRail = maxHeight >= 460.dp
        Column(Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(PcSpace.sm)) {
            Box(Modifier.fillMaxWidth(0.45f).height(22.dp).skeleton())
            Box(Modifier.fillMaxWidth().height(hero).skeleton(PcShape.Hero))
            if (showRail) {
                Box(Modifier.fillMaxWidth(0.4f).height(22.dp).skeleton())
                Row(horizontalArrangement = Arrangement.spacedBy(PcSpace.sm)) { repeat(2) { Box(Modifier.weight(1f).height(96.dp).skeleton(PcShape.Tile)) } }
            }
            Box(Modifier.fillMaxWidth().height(44.dp).skeleton(PcShape.Chip))
            Box(Modifier.fillMaxWidth().weight(1f).skeleton(PcShape.Tile))
        }
    }
}

/** Skeleton that mirrors the News layout: lead card then compact rows. */
@Composable
fun NewsSkeleton(modifier: Modifier = Modifier) {
    Column(modifier.fillMaxWidth().semantics { contentDescription = "Loading" }, verticalArrangement = Arrangement.spacedBy(PcSpace.sm)) {
        Box(Modifier.fillMaxWidth().aspectRatio(4f / 3f).skeleton(PcShape.Hero))
        repeat(3) {
            Row(horizontalArrangement = Arrangement.spacedBy(PcSpace.sm)) {
                Box(Modifier.size(width = 96.dp, height = 72.dp).skeleton(PcShape.Control))
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(PcSpace.xs)) { Box(Modifier.fillMaxWidth(0.3f).height(10.dp).skeleton()); Box(Modifier.fillMaxWidth().height(16.dp).skeleton()); Box(Modifier.fillMaxWidth(0.7f).height(16.dp).skeleton()) }
            }
        }
    }
}

@Composable
fun DetailSkeleton(modifier: Modifier = Modifier) {
    Column(modifier.fillMaxWidth().padding(PcSpace.md).semantics { contentDescription = "Loading" }, verticalArrangement = Arrangement.spacedBy(PcSpace.md)) {
        Box(Modifier.fillMaxWidth().height(220.dp).skeleton(PcShape.Hero))
        Row(horizontalArrangement = Arrangement.spacedBy(PcSpace.xs)) { repeat(4) { Box(Modifier.weight(1f).height(38.dp).skeleton(PcShape.Chip)) } }
        repeat(4) { Box(Modifier.fillMaxWidth().height(56.dp).skeleton(PcShape.Control)) }
    }
}
