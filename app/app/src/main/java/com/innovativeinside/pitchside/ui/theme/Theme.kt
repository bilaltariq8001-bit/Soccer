package com.innovativeinside.pitchside.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontVariation
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.innovativeinside.pitchside.R

/** Colour tokens, mirrored from the "Design system v2" board. One set per theme. */
@Immutable
class PcColors(
    val bg: Color, val surface: Color, val raised: Color, val nav: Color, val border: Color, val borderStrong: Color,
    val text: Color, val text2: Color, val text3: Color,
    val lime: Color, val onLime: Color, val limeTint: Color, val limeTintSoft: Color,
    val live: Color, val liveTint: Color, val amber: Color, val amberTint: Color, val red: Color, val redTint: Color, val neutral: Color,
)

val DarkPc = PcColors(
    bg = Color(0xFF07111F), surface = Color(0xFF0D1B2E), raised = Color(0xFF12263D), nav = Color(0xFF0A1626),
    border = Color(0xFF1B3150), borderStrong = Color(0xFF24405F),
    text = Color(0xFFF5F1E8), text2 = Color(0xFFA9B7CC), text3 = Color(0xFF8496AF),
    lime = Color(0xFFB7F34A), onLime = Color(0xFF07111F), limeTint = Color(0x1FB7F34A), limeTintSoft = Color(0x14B7F34A),
    live = Color(0xFF4DD8FF), liveTint = Color(0x244DD8FF), amber = Color(0xFFFFC857), amberTint = Color(0x24FFC857),
    red = Color(0xFFFF6B6B), redTint = Color(0x2EFF6B6B), neutral = Color(0xFF5B6E8C),
)

/** Accent and status colours are darkened so text and icons keep 4.5:1 contrast on white surfaces. */
val LightPc = PcColors(
    bg = Color(0xFFF6F7F9), surface = Color(0xFFFFFFFF), raised = Color(0xFFEDF0F5), nav = Color(0xFFFFFFFF),
    border = Color(0xFFDCE2EB), borderStrong = Color(0xFFC3CDDA),
    text = Color(0xFF0E1420), text2 = Color(0xFF4A5668), text3 = Color(0xFF5F6B7D),
    lime = Color(0xFF4B7A00), onLime = Color(0xFFFFFFFF), limeTint = Color(0x1F4B7A00), limeTintSoft = Color(0x144B7A00),
    live = Color(0xFF00779A), liveTint = Color(0x1F00779A), amber = Color(0xFF8A5A00), amberTint = Color(0x1F8A5A00),
    red = Color(0xFFC73A2D), redTint = Color(0x1FC73A2D), neutral = Color(0xFF8A97AB),
)

val LocalPc = staticCompositionLocalOf { DarkPc }

/** Read tokens as Pc.Text, Pc.Lime and so on. They follow the active theme. */
object Pc {
    val Bg: Color @Composable @ReadOnlyComposable get() = LocalPc.current.bg
    val Surface: Color @Composable @ReadOnlyComposable get() = LocalPc.current.surface
    val Raised: Color @Composable @ReadOnlyComposable get() = LocalPc.current.raised
    val Nav: Color @Composable @ReadOnlyComposable get() = LocalPc.current.nav
    val Border: Color @Composable @ReadOnlyComposable get() = LocalPc.current.border
    val BorderStrong: Color @Composable @ReadOnlyComposable get() = LocalPc.current.borderStrong
    val Text: Color @Composable @ReadOnlyComposable get() = LocalPc.current.text
    val Text2: Color @Composable @ReadOnlyComposable get() = LocalPc.current.text2
    val Text3: Color @Composable @ReadOnlyComposable get() = LocalPc.current.text3
    val Lime: Color @Composable @ReadOnlyComposable get() = LocalPc.current.lime
    val OnLime: Color @Composable @ReadOnlyComposable get() = LocalPc.current.onLime
    val LimeTint: Color @Composable @ReadOnlyComposable get() = LocalPc.current.limeTint
    val LimeTintSoft: Color @Composable @ReadOnlyComposable get() = LocalPc.current.limeTintSoft
    val Live: Color @Composable @ReadOnlyComposable get() = LocalPc.current.live
    val LiveTint: Color @Composable @ReadOnlyComposable get() = LocalPc.current.liveTint
    val Amber: Color @Composable @ReadOnlyComposable get() = LocalPc.current.amber
    val AmberTint: Color @Composable @ReadOnlyComposable get() = LocalPc.current.amberTint
    val Red: Color @Composable @ReadOnlyComposable get() = LocalPc.current.red
    val RedTint: Color @Composable @ReadOnlyComposable get() = LocalPc.current.redTint
    val Neutral: Color @Composable @ReadOnlyComposable get() = LocalPc.current.neutral
}

object PcShape {
    val Chip = RoundedCornerShape(12.dp)
    val Control = RoundedCornerShape(14.dp)
    val Card = RoundedCornerShape(16.dp)
    val Hero = RoundedCornerShape(20.dp)
    val Badge = RoundedCornerShape(8.dp)
}

/** Bundled fonts: Manrope (variable weight) for text, Barlow Condensed for scores. Both are SIL OFL licensed. */
@OptIn(androidx.compose.ui.text.ExperimentalTextApi::class)
object PcFont {
    val Body: FontFamily = FontFamily(
        Font(R.font.manrope, FontWeight.Normal, variationSettings = FontVariation.Settings(FontVariation.weight(400))),
        Font(R.font.manrope, FontWeight.Medium, variationSettings = FontVariation.Settings(FontVariation.weight(500))),
        Font(R.font.manrope, FontWeight.SemiBold, variationSettings = FontVariation.Settings(FontVariation.weight(600))),
        Font(R.font.manrope, FontWeight.Bold, variationSettings = FontVariation.Settings(FontVariation.weight(700))),
        Font(R.font.manrope, FontWeight.ExtraBold, variationSettings = FontVariation.Settings(FontVariation.weight(800))),
    )
    val Score: FontFamily = FontFamily(
        Font(R.font.barlow_condensed_semibold, FontWeight.SemiBold),
        Font(R.font.barlow_condensed_bold, FontWeight.Bold),
    )
}

object PcType {
    val Title: TextStyle @Composable @ReadOnlyComposable get() = TextStyle(fontFamily = PcFont.Body, fontWeight = FontWeight.ExtraBold, fontSize = 26.sp, lineHeight = 32.sp, color = Pc.Text)
    val Headline: TextStyle @Composable @ReadOnlyComposable get() = TextStyle(fontFamily = PcFont.Body, fontWeight = FontWeight.ExtraBold, fontSize = 20.sp, lineHeight = 26.sp, color = Pc.Text)
    val Subhead: TextStyle @Composable @ReadOnlyComposable get() = TextStyle(fontFamily = PcFont.Body, fontWeight = FontWeight.Bold, fontSize = 17.sp, lineHeight = 22.sp, color = Pc.Text)
    val Body: TextStyle @Composable @ReadOnlyComposable get() = TextStyle(fontFamily = PcFont.Body, fontWeight = FontWeight.Medium, fontSize = 16.sp, lineHeight = 24.sp, color = Pc.Text)
    val BodySmall: TextStyle @Composable @ReadOnlyComposable get() = TextStyle(fontFamily = PcFont.Body, fontWeight = FontWeight.Medium, fontSize = 14.sp, lineHeight = 20.sp, color = Pc.Text2)
    val Label: TextStyle @Composable @ReadOnlyComposable get() = TextStyle(fontFamily = PcFont.Body, fontWeight = FontWeight.Bold, fontSize = 14.sp, lineHeight = 18.sp, color = Pc.Text)
    val Meta: TextStyle @Composable @ReadOnlyComposable get() = TextStyle(fontFamily = PcFont.Body, fontWeight = FontWeight.Medium, fontSize = 12.sp, lineHeight = 16.sp, color = Pc.Text3)
    val Overline: TextStyle @Composable @ReadOnlyComposable get() = TextStyle(fontFamily = PcFont.Body, fontWeight = FontWeight.ExtraBold, fontSize = 12.sp, lineHeight = 16.sp, letterSpacing = 0.8.sp, color = Pc.Lime)
    val ScoreXL: TextStyle @Composable @ReadOnlyComposable get() = TextStyle(fontFamily = PcFont.Score, fontWeight = FontWeight.Bold, fontSize = 72.sp, lineHeight = 72.sp, color = Pc.Text)
    val ScoreL: TextStyle @Composable @ReadOnlyComposable get() = TextStyle(fontFamily = PcFont.Score, fontWeight = FontWeight.Bold, fontSize = 44.sp, lineHeight = 44.sp, color = Pc.Text)
    val ScoreM: TextStyle @Composable @ReadOnlyComposable get() = TextStyle(fontFamily = PcFont.Score, fontWeight = FontWeight.Bold, fontSize = 28.sp, lineHeight = 28.sp, color = Pc.Text)
    val ScoreS: TextStyle @Composable @ReadOnlyComposable get() = TextStyle(fontFamily = PcFont.Score, fontWeight = FontWeight.Bold, fontSize = 22.sp, lineHeight = 22.sp, color = Pc.Text)
}

@Composable
fun PitchsideTheme(dark: Boolean = true, content: @Composable () -> Unit) {
    val c = if (dark) DarkPc else LightPc
    val scheme = if (dark) {
        darkColorScheme(primary = c.lime, onPrimary = c.onLime, background = c.bg, onBackground = c.text, surface = c.surface, onSurface = c.text, surfaceVariant = c.raised, onSurfaceVariant = c.text2, outline = c.border, error = c.red)
    } else {
        lightColorScheme(primary = c.lime, onPrimary = c.onLime, background = c.bg, onBackground = c.text, surface = c.surface, onSurface = c.text, surfaceVariant = c.raised, onSurfaceVariant = c.text2, outline = c.border, error = c.red)
    }
    CompositionLocalProvider(LocalPc provides c) {
        MaterialTheme(
            colorScheme = scheme,
            typography = Typography(
                bodyLarge = PcType.Body, bodyMedium = PcType.BodySmall, titleLarge = PcType.Title,
                labelLarge = PcType.Label, labelSmall = PcType.Meta,
            ),
            content = content,
        )
    }
}
