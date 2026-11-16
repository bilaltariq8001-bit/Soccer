package com.innovativeinside.pitchside.ui.screens

import android.animation.ValueAnimator
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
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
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.innovativeinside.pitchside.R
import com.innovativeinside.pitchside.ui.theme.PcType
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random

/**
 * Matchday intro, played over the already-composed app so it never delays loading.
 *
 * One linear clock (0..1) drives every beat; each element maps the clock to its own window and
 * reads it only in draw / graphicsLayer lambdas, so the whole intro runs without recomposition.
 *
 * Beats: floodlights flicker on -> pitch lines trace -> crest kicks in with a shockwave and sparks
 * -> letters drop in -> tagline + light sweep -> zoom-through exit. Tap anywhere to skip.
 */
private const val TOTAL_MS = 2_700
private const val REDUCED_MS = 450

@Composable
fun AnimatedSplashScreen(onFinished: () -> Unit) {
    val motionEnabled = remember { ValueAnimator.areAnimatorsEnabled() }
    val finish by rememberUpdatedState(onFinished)
    val clock = remember { Animatable(0f) }

    LaunchedEffect(Unit) {
        if (motionEnabled) {
            clock.animateTo(1f, tween(TOTAL_MS, easing = LinearEasing))
        } else {
            clock.snapTo(T.HOLD_END) // everything settled, no motion
            kotlinx.coroutines.delay(REDUCED_MS.toLong())
            clock.snapTo(1f)
        }
        finish()
    }

    val colors = SplashColors(
        stage = colorResource(R.color.pitchside_splash_stage),
        glow = colorResource(R.color.pitchside_splash_background_glow),
        ink = colorResource(R.color.pitchside_splash_ink),
        accent = colorResource(R.color.pitchside_splash_primary),
        live = colorResource(R.color.pitchside_splash_live),
    )
    val sparks = remember { buildSparks() }
    val appName = stringResource(R.string.app_name)

    BoxWithConstraints(
        Modifier
            .fillMaxSize()
            .background(colors.stage)
            .semantics { contentDescription = appName }
            .pointerInput(Unit) { detectTapGestures { finish() } }
            .graphicsLayer {
                val exit = ease(seg(clock.value, T.HOLD_END, 1f))
                alpha = 1f - exit
            },
    ) {
        val compact = maxHeight < 440.dp
        val logoSize = minOf(maxWidth * 0.62f, maxHeight * 0.36f, 200.dp)
        val lift = if (maxHeight < 300.dp) 0.dp else minOf(logoSize * 0.28f, 56.dp)

        StadiumBackdrop(clock, colors, sparks, logoSize, lift)

        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .padding(horizontal = 24.dp)
                .graphicsLayer {
                    val exit = ease(seg(clock.value, T.HOLD_END, 1f))
                    translationY = -lift.toPx()
                    scaleX = 1f + 0.18f * exit
                    scaleY = 1f + 0.18f * exit
                },
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Crest(clock, logoSize)
            Spacer(Modifier.height(if (compact) 14.dp else 22.dp))
            Wordmark(clock, appName.uppercase(), colors, if (compact) 28.sp else 36.sp)
            Spacer(Modifier.height(8.dp))
            Tagline(clock, colors, if (compact) 12.sp else 14.sp)
        }
    }
}

/* ---------- timeline ---------- */

private object T {
    const val LIGHTS_START = 0.00f
    const val LIGHTS_END = 0.24f
    const val LINES_START = 0.10f
    const val LINES_END = 0.46f
    const val CREST_START = 0.26f
    const val CREST_END = 0.50f
    const val IMPACT = 0.40f
    const val IMPACT_END = 0.78f
    const val LETTERS_START = 0.46f
    const val LETTER_STEP = 0.022f
    const val LETTER_LEN = 0.14f
    const val TAGLINE_START = 0.66f
    const val TAGLINE_END = 0.78f
    const val SWEEP_START = 0.70f
    const val SWEEP_END = 0.86f
    const val HOLD_END = 0.88f
}

private fun seg(t: Float, start: Float, end: Float): Float = ((t - start) / (end - start)).coerceIn(0f, 1f)

private fun ease(x: Float): Float = FastOutSlowInEasing.transform(x)

/** easeOutBack: a small, confident overshoot. */
private fun back(x: Float, s: Float = 1.70158f): Float {
    if (x <= 0f) return 0f
    if (x >= 1f) return 1f
    val p = x - 1f
    return 1f + (s + 1f) * p * p * p + s * p * p
}

/** Floodlight flicker: two quick dips before the lamp holds. */
private fun flicker(x: Float): Float = when {
    x <= 0f -> 0f
    x < 0.25f -> 0.9f
    x < 0.38f -> 0.25f
    x < 0.55f -> 1f
    x < 0.62f -> 0.55f
    else -> 1f
}

/* ---------- pieces ---------- */

@Stable
private class SplashColors(val stage: Color, val glow: Color, val ink: Color, val accent: Color, val live: Color)

private class Spark(val angle: Float, val distance: Float, val radius: Float, val delay: Float, val live: Boolean)

private fun buildSparks(): List<Spark> {
    val rnd = Random(7)
    return List(22) { i ->
        Spark(
            angle = (i / 22f) * 2f * PI.toFloat() + rnd.nextFloat() * 0.25f,
            distance = 0.75f + rnd.nextFloat() * 0.55f,
            radius = 1.6f + rnd.nextFloat() * 2.2f,
            delay = rnd.nextFloat() * 0.12f,
            live = i % 7 == 0,
        )
    }
}

@Composable
private fun StadiumBackdrop(clock: Animatable<Float, *>, c: SplashColors, sparks: List<Spark>, logoSize: Dp, lift: Dp) {
    Canvas(Modifier.fillMaxSize()) {
        val t = clock.value
        val center = Offset(size.width / 2f, size.height / 2f - lift.toPx())

        // Base atmosphere grows in under the lights.
        val atmo = ease(seg(t, T.LIGHTS_START, T.LINES_END))
        drawRect(
            Brush.radialGradient(
                listOf(c.glow.copy(alpha = 0.9f * atmo), Color.Transparent),
                center = center,
                radius = size.maxDimension * 0.75f,
            ),
        )

        // Four floodlight beams switch on one after another, each with a flicker.
        val corners = listOf(
            Offset(0f, 0f), Offset(size.width, 0f),
            Offset(0f, size.height * 0.08f), Offset(size.width, size.height * 0.08f),
        )
        corners.forEachIndexed { i, from ->
            val local = seg(t, T.LIGHTS_START + i * 0.045f, T.LIGHTS_END + i * 0.045f)
            val a = flicker(local) * (0.16f + 0.04f * (i % 2))
            if (a > 0f) floodBeam(from, center, a, c.ink)
            if (local > 0f) {
                drawCircle(c.ink.copy(alpha = flicker(local) * 0.9f), radius = 3.dp.toPx(), center = from + Offset(if (from.x == 0f) 14.dp.toPx() else -14.dp.toPx(), 18.dp.toPx() + from.y))
            }
        }

        // Pitch markings trace in: halfway line from the centre spot outward, then the centre circle.
        val lines = seg(t, T.LINES_START, T.LINES_END)
        val lineInk = c.accent.copy(alpha = 0.22f)
        val stroke = 1.5.dp.toPx()
        val half = ease(seg(lines, 0f, 0.55f)) * size.width / 2f
        if (half > 0f) {
            drawLine(lineInk, center - Offset(half, 0f), center + Offset(half, 0f), stroke, StrokeCap.Round)
        }
        val circleR = logoSize.toPx() * 0.78f
        val sweep = 360f * ease(seg(lines, 0.25f, 1f))
        if (sweep > 0f) {
            drawArc(
                lineInk, startAngle = -90f, sweepAngle = sweep, useCenter = false,
                topLeft = center - Offset(circleR, circleR), size = Size(circleR * 2, circleR * 2),
                style = Stroke(stroke, cap = StrokeCap.Round),
            )
        }
        // Outer pitch arc low on screen (perspective hint of the penalty box).
        val boxT = ease(seg(lines, 0.4f, 1f))
        if (boxT > 0f) {
            val w = size.width * 0.9f
            drawArc(
                c.accent.copy(alpha = 0.10f), startAngle = 200f, sweepAngle = 140f * boxT, useCenter = false,
                topLeft = Offset((size.width - w) / 2f, size.height * 0.70f), size = Size(w, w * 0.6f),
                style = Stroke(stroke),
            )
        }

        // Kick-off impact: a shockwave pair, sparks, and one red "live" pulse.
        val impact = seg(t, T.IMPACT, T.IMPACT_END)
        if (impact > 0f && impact < 1f) {
            val baseR = logoSize.toPx() * 0.42f
            shock(center, baseR, ease(seg(impact, 0f, 0.7f)), c.accent, 0.55f)
            shock(center, baseR, ease(seg(impact, 0.12f, 0.9f)), c.ink, 0.30f)
            sparks.forEach { s ->
                val p = ease(seg(impact, s.delay, 0.75f + s.delay))
                if (p > 0f && p < 1f) {
                    val d = baseR + logoSize.toPx() * 0.9f * s.distance * p
                    val pos = center + Offset(cos(s.angle) * d, sin(s.angle) * d)
                    val col = if (s.live) c.live else c.ink
                    drawCircle(col.copy(alpha = (1f - p) * 0.95f), radius = s.radius.dp.toPx() * (1f - 0.5f * p), center = pos)
                }
            }
        }
    }
}

private fun DrawScope.floodBeam(from: Offset, to: Offset, alpha: Float, ink: Color) {
    val dx = to.x - from.x
    val dy = to.y - from.y
    val len = kotlin.math.hypot(dx, dy)
    val nx = -dy / len
    val ny = dx / len
    val spread = size.minDimension * 0.42f
    val path = Path().apply {
        moveTo(from.x, from.y)
        lineTo(to.x + nx * spread, to.y + ny * spread)
        lineTo(to.x - nx * spread, to.y - ny * spread)
        close()
    }
    drawPath(
        path,
        Brush.linearGradient(listOf(ink.copy(alpha = alpha), Color.Transparent), start = from, end = to + Offset(dx * 0.2f, dy * 0.2f)),
    )
}

private fun DrawScope.shock(center: Offset, baseR: Float, p: Float, color: Color, maxAlpha: Float) {
    if (p <= 0f || p >= 1f) return
    drawCircle(
        color.copy(alpha = maxAlpha * (1f - p)),
        radius = baseR + size.minDimension * 0.42f * p,
        center = center,
        style = Stroke(width = (3.dp.toPx() * (1f - p)).coerceAtLeast(0.5f)),
    )
}

@Composable
private fun Crest(clock: Animatable<Float, *>, logoSize: Dp) {
    Image(
        painter = painterResource(R.drawable.ic_pitchside_splash_mark),
        contentDescription = null,
        modifier = Modifier
            .size(logoSize)
            .graphicsLayer {
                val t = clock.value
                val p = seg(t, T.CREST_START, T.CREST_END)
                val s = 0.35f + 0.65f * back(p, 2.2f)
                // Small "settle" bump right on impact.
                val bump = seg(t, T.IMPACT, T.IMPACT + 0.08f)
                val squash = if (bump in 0.001f..0.999f) sin(bump * PI.toFloat()) * 0.05f else 0f
                alpha = ease(seg(p, 0f, 0.4f))
                scaleX = s + squash
                scaleY = s - squash
                rotationZ = -32f * (1f - ease(p))
            },
    )
}

@Composable
private fun Wordmark(clock: Animatable<Float, *>, word: String, c: SplashColors, fontSize: TextUnit) {
    Box(
        Modifier
            .graphicsLayer { compositingStrategy = CompositingStrategy.Offscreen }
            .drawWithContent {
                drawContent()
                // Light sweep across the letters once they have landed.
                val p = ease(seg(clock.value, T.SWEEP_START, T.SWEEP_END))
                if (p > 0f && p < 1f) {
                    val band = size.width * 0.35f
                    val x = -band + (size.width + band * 2) * p
                    drawRect(
                        Brush.linearGradient(
                            listOf(Color.Transparent, c.accent.copy(alpha = 0.9f), Color.Transparent),
                            start = Offset(x - band, 0f),
                            end = Offset(x + band, size.height),
                        ),
                        blendMode = BlendMode.SrcAtop,
                    )
                }
            },
    ) {
        Row {
            word.forEachIndexed { i, ch ->
                Text(
                    text = ch.toString(),
                    style = PcType.Display.copy(
                        color = c.ink,
                        fontSize = fontSize,
                        lineHeight = fontSize * 1.15f,
                        letterSpacing = 2.sp,
                        fontWeight = FontWeight.ExtraBold,
                    ),
                    maxLines = 1,
                    modifier = Modifier.graphicsLayer {
                        val start = T.LETTERS_START + i * T.LETTER_STEP
                        val p = seg(clock.value, start, start + T.LETTER_LEN)
                        alpha = ease(seg(p, 0f, 0.5f))
                        translationY = -28.dp.toPx() * (1f - back(p))
                        rotationX = 70f * (1f - ease(p))
                    },
                )
            }
        }
    }
}

@Composable
private fun Tagline(clock: Animatable<Float, *>, c: SplashColors, fontSize: TextUnit) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        // Live dot: emerald, pulsing once the tagline lands.
        Canvas(Modifier.size(8.dp)) {
            val t = clock.value
            val inP = ease(seg(t, T.TAGLINE_START, T.TAGLINE_END))
            val pulse = seg(t, T.TAGLINE_END, T.HOLD_END)
            drawCircle(c.live.copy(alpha = inP), radius = size.minDimension / 2f * inP)
            if (pulse > 0f && pulse < 1f) {
                drawCircle(c.live.copy(alpha = 0.5f * (1f - pulse)), radius = size.minDimension / 2f * (1f + 1.4f * pulse))
            }
        }
        Spacer(Modifier.size(8.dp))
        Text(
            text = stringResource(R.string.splash_tagline).uppercase(),
            style = PcType.Kicker.copy(color = c.ink.copy(alpha = 0.78f), fontSize = fontSize, letterSpacing = 1.4.sp),
            textAlign = TextAlign.Center,
            maxLines = 1,
            modifier = Modifier.graphicsLayer {
                val p = ease(seg(clock.value, T.TAGLINE_START, T.TAGLINE_END))
                alpha = p
                translationY = 12.dp.toPx() * (1f - p)
            },
        )
    }
}
