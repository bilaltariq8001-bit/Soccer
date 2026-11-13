package com.innovativeinside.pitchside.ui.screens

import android.animation.ValueAnimator
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.innovativeinside.pitchside.R
import com.innovativeinside.pitchside.ui.theme.PcType
import kotlinx.coroutines.delay

private const val INTRO_MS = 2_040L
private const val EXIT_MS = 200L

/** Short brand intro over the already-composed app, so animation never delays app initialization. */
@Composable
fun AnimatedSplashScreen(onFinished: () -> Unit) {
    val motionEnabled = remember { ValueAnimator.areAnimatorsEnabled() }
    val stadiumBase = colorResource(R.color.pitchside_window_background)
    val stadiumLight = colorResource(R.color.pitchside_splash_background_glow)
    val stadiumInk = colorResource(R.color.pitchside_splash_ink)
    val liveGreen = colorResource(R.color.pitchside_splash_primary)
    val liveCyan = colorResource(R.color.pitchside_splash_live)
    var entered by remember { mutableStateOf(false) }
    var exiting by remember { mutableStateOf(false) }

    val logoAlpha by animateFloatAsState(
        targetValue = if (entered) 1f else 0f,
        animationSpec = if (motionEnabled) tween(540, easing = FastOutSlowInEasing) else snap(),
        label = "pitchside-logo-alpha",
    )
    val logoReveal by animateFloatAsState(
        targetValue = if (entered) 1f else 0f,
        animationSpec = if (motionEnabled) tween(960, delayMillis = 60, easing = FastOutSlowInEasing) else snap(),
        label = "pitchside-logo-reveal",
    )
    val logoScale by animateFloatAsState(
        targetValue = if (entered) 1f else 0.9f,
        animationSpec = if (motionEnabled) spring(dampingRatio = 1f, stiffness = 320f) else snap(),
        label = "pitchside-logo-scale",
    )
    val logoRotation by animateFloatAsState(
        targetValue = if (entered) 0f else -7f,
        animationSpec = if (motionEnabled) tween(760, easing = FastOutSlowInEasing) else snap(),
        label = "pitchside-logo-rotation",
    )
    val brandAlpha by animateFloatAsState(
        targetValue = if (entered) 1f else 0f,
        animationSpec = if (motionEnabled) tween(480, delayMillis = 620, easing = FastOutSlowInEasing) else snap(),
        label = "pitchside-brand-alpha",
    )
    val taglineAlpha by animateFloatAsState(
        targetValue = if (entered) 1f else 0f,
        animationSpec = if (motionEnabled) tween(420, delayMillis = 940, easing = FastOutSlowInEasing) else snap(),
        label = "pitchside-tagline-alpha",
    )
    val atmosphere by animateFloatAsState(
        targetValue = if (entered) 0.34f else 0f,
        animationSpec = if (motionEnabled) tween(950, easing = FastOutSlowInEasing) else snap(),
        label = "stadium-atmosphere",
    )
    val signalProgress by animateFloatAsState(
        targetValue = if (entered) 1f else 0f,
        animationSpec = if (motionEnabled) tween(1_500, delayMillis = 180, easing = FastOutSlowInEasing) else snap(),
        label = "live-signal-rings",
    )
    val screenAlpha by animateFloatAsState(
        targetValue = if (exiting) 0f else 1f,
        animationSpec = if (motionEnabled) tween(EXIT_MS.toInt(), easing = FastOutSlowInEasing) else snap(),
        label = "pitchside-splash-exit",
    )

    LaunchedEffect(motionEnabled) {
        entered = true
        delay(if (motionEnabled) INTRO_MS else 140L)
        exiting = true
    }
    LaunchedEffect(exiting) {
        if (exiting) {
            delay(if (motionEnabled) EXIT_MS else 60L)
            onFinished()
        }
    }

    BoxWithConstraints(
        Modifier.fillMaxSize().background(stadiumBase).graphicsLayer { alpha = screenAlpha },
    ) {
        val compact = maxHeight < 440.dp
        val logoSize = minOf(maxWidth * 0.82f, maxHeight * 0.46f, 260.dp)
        val verticalOffset = if (maxHeight < 300.dp) 0.dp else minOf(logoSize * 0.18f, 46.dp)
        val brandSize = if (compact) 28.sp else 34.sp
        val taglineSize = if (compact) 13.sp else 15.sp
        val stackGap = if (compact) 12.dp else 18.dp

        Canvas(Modifier.fillMaxSize()) {
            // One restrained floodlight wash grows in after the native system splash.
            drawRect(
                brush = Brush.radialGradient(
                    colors = listOf(stadiumLight.copy(alpha = atmosphere), Color.Transparent),
                    center = androidx.compose.ui.geometry.Offset(size.width * 0.52f, size.height * 0.32f),
                    radius = size.height * 0.84f,
                ),
            )
            // A single low-contrast stadium roof line gives the dark field a sense of place.
            drawArc(
                color = stadiumInk.copy(alpha = 0.045f),
                startAngle = 202f,
                sweepAngle = 136f,
                useCenter = false,
                topLeft = androidx.compose.ui.geometry.Offset(-size.width * 0.30f, size.height * 0.48f),
                size = androidx.compose.ui.geometry.Size(size.width * 1.60f, size.width * 1.60f),
                style = Stroke(width = 1.dp.toPx()),
            )

            val center = Offset(size.width / 2f, size.height / 2f + verticalOffset.toPx())
            val logoRadius = logoSize.toPx() * 0.275f
            fun signalRing(progress: Float, tint: Color) {
                if (progress > 0f && progress < 1f) {
                    drawCircle(
                        color = tint.copy(alpha = 0.32f * (1f - progress)),
                        radius = logoRadius + logoSize.toPx() * 0.25f * progress,
                        center = center,
                        style = Stroke(width = 1.6.dp.toPx()),
                    )
                }
            }
            signalRing(signalProgress, liveGreen)
            signalRing(((signalProgress * 1.2f) - 0.35f).coerceIn(0f, 1f), liveCyan)
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .padding(horizontal = 24.dp)
                .graphicsLayer { translationY = verticalOffset.toPx() },
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Image(
                painter = painterResource(R.drawable.ic_pitchside_splash_mark),
                contentDescription = null,
                modifier = Modifier
                    .size(logoSize)
                    .drawWithContent {
                        if (logoReveal >= 0.999f) {
                            drawContent()
                        } else {
                            val center = Offset(size.width / 2f, size.height / 2f)
                            val radius = kotlin.math.hypot(size.width.toDouble(), size.height.toDouble()).toFloat()
                            val sweep = 360f * logoReveal
                            val revealPath = Path().apply {
                                moveTo(center.x, center.y)
                                arcTo(
                                    rect = Rect(center.x - radius, center.y - radius, center.x + radius, center.y + radius),
                                    startAngleDegrees = -90f,
                                    sweepAngleDegrees = sweep,
                                    forceMoveTo = false,
                                )
                                close()
                            }
                            clipPath(revealPath) { this@drawWithContent.drawContent() }
                        }
                    }
                    .graphicsLayer {
                        alpha = logoAlpha
                        scaleX = logoScale
                        scaleY = logoScale
                        rotationZ = logoRotation
                    },
            )
            Spacer(Modifier.height(stackGap))
            Text(
                text = stringResource(R.string.app_name),
                style = PcType.Title.copy(
                    color = stadiumInk,
                    fontSize = brandSize,
                    lineHeight = brandSize * 1.15f,
                    letterSpacing = 1.1.sp,
                    fontWeight = FontWeight.ExtraBold,
                ),
                textAlign = TextAlign.Center,
                modifier = Modifier.graphicsLayer { alpha = brandAlpha },
                maxLines = 1,
            )
            Spacer(Modifier.height(6.dp))
            Text(
                text = stringResource(R.string.splash_tagline),
                style = PcType.BodySmall.copy(color = stadiumInk.copy(alpha = 0.72f), fontSize = taglineSize),
                textAlign = TextAlign.Center,
                modifier = Modifier.graphicsLayer { alpha = taglineAlpha },
                maxLines = 1,
            )
        }
    }
}
