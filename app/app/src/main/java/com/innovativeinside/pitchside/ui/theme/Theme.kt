/* Hallmark · checked: 2026-11-12 · genre: modern-minimal (inferred) · shape: app screen · redesign: full
 * pre-emit critique: P4 H4 E3 S5 R4 V4 · weakest: E, small screen details; raised shared text sizes and badge height
 * floor: Compose app · theme: existing Pitchside brand · scope: app · built: the project · v2.0.0 */
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
    val pitch: Color, val onPitch: Color, val pitchLine: Color,
)

val DarkPc = PcColors(
    bg = Color(0xFF101A16), surface = Color(0xFF18251F), raised = Color(0xFF22332A), nav = Color(0xFF131F19),
    border = Color(0xFF2A3D32), borderStrong = Color(0xFF3A5143),
    text = Color(0xFFF4F1E7), text2 = Color(0xFFB7C2B6), text3 = Color(0xFF96A498),
    lime = Color(0xFFC8F06A), onLime = Color(0xFF172414), limeTint = Color(0x28C8F06A), limeTintSoft = Color(0x18C8F06A),
    live = Color(0xFFFF806B), liveTint = Color(0x28FF806B), amber = Color(0xFFFFCC74), amberTint = Color(0x24FFCC74),
    red = Color(0xFFFF746D), redTint = Color(0x2EFF746D), neutral = Color(0xFF718477),
    pitch = Color(0xFF193B2D), onPitch = Color(0xFFF5F4E9), pitchLine = Color(0xFF496C56),
)

/** Accent and status colours are darkened so text and icons keep 4.5:1 contrast on white surfaces. */
val LightPc = PcColors(
    bg = Color(0xFFF4F3EC), surface = Color(0xFFFFFEF8), raised = Color(0xFFE9EBDD), nav = Color(0xFFFFFEF8),
    border = Color(0xFFD9DED1), borderStrong = Color(0xFFBBC6B6),
    text = Color(0xFF17211B), text2 = Color(0xFF4D5B50), text3 = Color(0xFF5A6A5D),
    lime = Color(0xFF416D27), onLime = Color(0xFFFFFFFF), limeTint = Color(0x1F416D27), limeTintSoft = Color(0x12416D27),
    live = Color(0xFFC74735), liveTint = Color(0x1FC74735), amber = Color(0xFF875B00), amberTint = Color(0x1F875B00),
    red = Color(0xFFB63E32), redTint = Color(0x1FB63E32), neutral = Color(0xFF809083),
    pitch = Color(0xFF214731), onPitch = Color(0xFFFFFFFF), pitchLine = Color(0xFF719176),
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
    val Pitch: Color @Composable @ReadOnlyComposable get() = LocalPc.current.pitch
    val OnPitch: Color @Composable @ReadOnlyComposable get() = LocalPc.current.onPitch
    val PitchLine: Color @Composable @ReadOnlyComposable get() = LocalPc.current.pitchLine
}

object PcShape {
    val Chip = RoundedCornerShape(10.dp)
    val Control = RoundedCornerShape(14.dp)
    val Card = RoundedCornerShape(18.dp)
    val Hero = RoundedCornerShape(24.dp)
    val Badge = RoundedCornerShape(8.dp)
}

object PcSpace {
    val xxs = 4.dp
    val xs = 8.dp
    val sm = 12.dp
    val md = 16.dp
    val lg = 24.dp
    val xl = 32.dp
    val section = 28.dp
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
    val BodySmall: TextStyle @Composable @ReadOnlyComposable get() = TextStyle(fontFamily = PcFont.Body, fontWeight = FontWeight.Medium, fontSize = 15.sp, lineHeight = 22.sp, color = Pc.Text2)
    val Label: TextStyle @Composable @ReadOnlyComposable get() = TextStyle(fontFamily = PcFont.Body, fontWeight = FontWeight.Bold, fontSize = 14.sp, lineHeight = 20.sp, color = Pc.Text)
    val Meta: TextStyle @Composable @ReadOnlyComposable get() = TextStyle(fontFamily = PcFont.Body, fontWeight = FontWeight.Medium, fontSize = 14.sp, lineHeight = 20.sp, color = Pc.Text3)
    val Overline: TextStyle @Composable @ReadOnlyComposable get() = TextStyle(fontFamily = PcFont.Body, fontWeight = FontWeight.SemiBold, fontSize = 14.sp, lineHeight = 20.sp, color = Pc.Text2)
    val ScoreXL: TextStyle @Composable @ReadOnlyComposable get() = TextStyle(fontFamily = PcFont.Score, fontWeight = FontWeight.Bold, fontSize = 72.sp, lineHeight = 72.sp, color = Pc.Text)
    val ScoreL: TextStyle @Composable @ReadOnlyComposable get() = TextStyle(fontFamily = PcFont.Score, fontWeight = FontWeight.Bold, fontSize = 44.sp, lineHeight = 44.sp, color = Pc.Text)
    val ScoreM: TextStyle @Composable @ReadOnlyComposable get() = TextStyle(fontFamily = PcFont.Score, fontWeight = FontWeight.Bold, fontSize = 28.sp, lineHeight = 28.sp, color = Pc.Text)
    val ScoreS: TextStyle @Composable @ReadOnlyComposable get() = TextStyle(fontFamily = PcFont.Score, fontWeight = FontWeight.Bold, fontSize = 22.sp, lineHeight = 22.sp, color = Pc.Text)
}

@Composable
fun PitchsideTheme(dark: Boolean = true, content: @Composable () -> Unit) {
    val c = if (dark) DarkPc else LightPc
    val scheme = if (dark) {
        darkColorScheme(primary = c.lime, onPrimary = c.onLime, primaryContainer = c.limeTint, onPrimaryContainer = c.text, secondary = c.live, onSecondary = c.bg, secondaryContainer = c.liveTint, onSecondaryContainer = c.text, tertiary = c.amber, onTertiary = c.bg, tertiaryContainer = c.amberTint, onTertiaryContainer = c.text, background = c.bg, onBackground = c.text, surface = c.surface, onSurface = c.text, surfaceVariant = c.raised, onSurfaceVariant = c.text2, outline = c.borderStrong, outlineVariant = c.border, error = c.red)
    } else {
        lightColorScheme(primary = c.lime, onPrimary = c.onLime, primaryContainer = c.limeTint, onPrimaryContainer = c.text, secondary = c.live, onSecondary = Color.White, secondaryContainer = c.liveTint, onSecondaryContainer = c.text, tertiary = c.amber, onTertiary = Color.White, tertiaryContainer = c.amberTint, onTertiaryContainer = c.text, background = c.bg, onBackground = c.text, surface = c.surface, onSurface = c.text, surfaceVariant = c.raised, onSurfaceVariant = c.text2, outline = c.borderStrong, outlineVariant = c.border, error = c.red)
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
