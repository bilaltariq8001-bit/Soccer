package com.innovativeinside.pitchside.ui.theme

import androidx.compose.foundation.shape.CircleShape
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

/*
 * Pitchside design tokens, the single source of truth for colour, type, spacing and shape.
 * Screens and components read them as Pc.<Color>, PcType.<Role>, PcSpace.<step> and PcShape.<shape>;
 * nothing outside this file declares a raw colour, font size or font family. See DESIGN.md.
 */

/** Colour tokens. One instance per theme; the names describe a role, never a hue. */
@Immutable
class PcColors(
    val bg: Color,
    val surface: Color,
    val surfaceVariant: Color,
    val outline: Color,
    val outlineStrong: Color,
    val text: Color,
    val text2: Color,
    /** Decorative only (dividers, placeholders, disabled). Below 4.5:1, never for information. */
    val text3: Color,
    val accent: Color,
    val accentContainer: Color,
    val onAccent: Color,
    /** LIVE state only: the pulse dot, live progress bars, live counters. */
    val live: Color,
    /** LIVE text on surfaces, darkened (light) so it keeps 4.5:1. */
    val liveText: Color,
    val liveContainer: Color,
    /** Solid LIVE pill fill and the text on it. */
    val liveFill: Color,
    val onLive: Color,
    val success: Color,
    val successContainer: Color,
    val warning: Color,
    val warningContainer: Color,
    val error: Color,
    val errorContainer: Color,
    /** The deep-green stage of the live hero tile and the match header, with its text colours. */
    val hero: Color,
    val heroRaised: Color,
    val onHero: Color,
    val onHero2: Color,
    val heroOutline: Color,
    /** Away side of paired stat bars. */
    val neutral: Color,
    val nav: Color,
    val scrim: Color,
    /** Fixed white for text on team-colour crests (team colours are always dark enough). */
    val onTeam: Color,
    val pitch: Color,
    val pitchLine: Color,
    val isDark: Boolean,
)

val LightPc = PcColors(
    bg = Color(0xFFF4F3EC), surface = Color(0xFFFFFFFF), surfaceVariant = Color(0xFFECEBE2),
    outline = Color(0xFFE0DED3), outlineStrong = Color(0xFFC9C6B8),
    text = Color(0xFF14181F), text2 = Color(0xFF5F6670), text3 = Color(0xFF8A9099),
    accent = Color(0xFF1F4D35), accentContainer = Color(0xFFE3EBDD), onAccent = Color(0xFFFFFFFF),
    live = Color(0xFFD94841), liveText = Color(0xFFB83A33), liveContainer = Color(0xFFFBE9E7), liveFill = Color(0xFFB83A33), onLive = Color(0xFFFFFFFF),
    success = Color(0xFF2E7D4F), successContainer = Color(0xFFE1F0E6),
    warning = Color(0xFFC98A1B), warningContainer = Color(0xFFF8EDD7),
    error = Color(0xFFC0392B), errorContainer = Color(0xFFF9E3E0),
    hero = Color(0xFF1F4D35), heroRaised = Color(0xFF2A5B41), onHero = Color(0xFFFFFFFF), onHero2 = Color(0xFFB9CDBF), heroOutline = Color(0xFF3A6A50),
    neutral = Color(0xFFB9BCB2), nav = Color(0xFFFFFFFF), scrim = Color(0x99000000), onTeam = Color(0xFFFFFFFF),
    pitch = Color(0xFF1F4D35), pitchLine = Color(0x47FFFFFF),
    isDark = false,
)

val DarkPc = PcColors(
    bg = Color(0xFF0C0F0D), surface = Color(0xFF151915), surfaceVariant = Color(0xFF1D221D),
    outline = Color(0xFF2A302A), outlineStrong = Color(0xFF3A423A),
    text = Color(0xFFF2F4F1), text2 = Color(0xFFA4ABA3), text3 = Color(0xFF7E857D),
    accent = Color(0xFF4FBF86), accentContainer = Color(0xFF1B3427), onAccent = Color(0xFF0C0F0D),
    live = Color(0xFFFF5A52), liveText = Color(0xFFFF6B63), liveContainer = Color(0xFF3A1A18), liveFill = Color(0xFFFF5A52), onLive = Color(0xFF0C0F0D),
    success = Color(0xFF5CC48A), successContainer = Color(0xFF173226),
    warning = Color(0xFFE0A93A), warningContainer = Color(0xFF33290F),
    error = Color(0xFFFF6B5E), errorContainer = Color(0xFF3A1A17),
    hero = Color(0xFF17432F), heroRaised = Color(0xFF1F503A), onHero = Color(0xFFF2F4F1), onHero2 = Color(0xFFB5C9BC), heroOutline = Color(0xFF2C5C44),
    neutral = Color(0xFF4A524A), nav = Color(0xFF111411), scrim = Color(0xB3000000), onTeam = Color(0xFFFFFFFF),
    pitch = Color(0xFF17432F), pitchLine = Color(0x47FFFFFF),
    isDark = true,
)

val LocalPc = staticCompositionLocalOf { LightPc }

/** Read colour tokens as Pc.Accent, Pc.Text2 and so on. They follow the active theme. */
object Pc {
    val Bg: Color @Composable @ReadOnlyComposable get() = LocalPc.current.bg
    val Surface: Color @Composable @ReadOnlyComposable get() = LocalPc.current.surface
    val SurfaceVariant: Color @Composable @ReadOnlyComposable get() = LocalPc.current.surfaceVariant
    val Outline: Color @Composable @ReadOnlyComposable get() = LocalPc.current.outline
    val OutlineStrong: Color @Composable @ReadOnlyComposable get() = LocalPc.current.outlineStrong
    val Text: Color @Composable @ReadOnlyComposable get() = LocalPc.current.text
    val Text2: Color @Composable @ReadOnlyComposable get() = LocalPc.current.text2
    val Text3: Color @Composable @ReadOnlyComposable get() = LocalPc.current.text3
    val Accent: Color @Composable @ReadOnlyComposable get() = LocalPc.current.accent
    val AccentContainer: Color @Composable @ReadOnlyComposable get() = LocalPc.current.accentContainer
    val OnAccent: Color @Composable @ReadOnlyComposable get() = LocalPc.current.onAccent
    val Live: Color @Composable @ReadOnlyComposable get() = LocalPc.current.live
    val LiveText: Color @Composable @ReadOnlyComposable get() = LocalPc.current.liveText
    val LiveContainer: Color @Composable @ReadOnlyComposable get() = LocalPc.current.liveContainer
    val LiveFill: Color @Composable @ReadOnlyComposable get() = LocalPc.current.liveFill
    val OnLive: Color @Composable @ReadOnlyComposable get() = LocalPc.current.onLive
    val Success: Color @Composable @ReadOnlyComposable get() = LocalPc.current.success
    val SuccessContainer: Color @Composable @ReadOnlyComposable get() = LocalPc.current.successContainer
    val Warning: Color @Composable @ReadOnlyComposable get() = LocalPc.current.warning
    val WarningContainer: Color @Composable @ReadOnlyComposable get() = LocalPc.current.warningContainer
    val Error: Color @Composable @ReadOnlyComposable get() = LocalPc.current.error
    val ErrorContainer: Color @Composable @ReadOnlyComposable get() = LocalPc.current.errorContainer
    val Hero: Color @Composable @ReadOnlyComposable get() = LocalPc.current.hero
    val HeroRaised: Color @Composable @ReadOnlyComposable get() = LocalPc.current.heroRaised
    val OnHero: Color @Composable @ReadOnlyComposable get() = LocalPc.current.onHero
    val OnHero2: Color @Composable @ReadOnlyComposable get() = LocalPc.current.onHero2
    val HeroOutline: Color @Composable @ReadOnlyComposable get() = LocalPc.current.heroOutline
    val Neutral: Color @Composable @ReadOnlyComposable get() = LocalPc.current.neutral
    val Nav: Color @Composable @ReadOnlyComposable get() = LocalPc.current.nav
    val Scrim: Color @Composable @ReadOnlyComposable get() = LocalPc.current.scrim
    val OnTeam: Color @Composable @ReadOnlyComposable get() = LocalPc.current.onTeam
    val Pitch: Color @Composable @ReadOnlyComposable get() = LocalPc.current.pitch
    val PitchLine: Color @Composable @ReadOnlyComposable get() = LocalPc.current.pitchLine
    val IsDark: Boolean @Composable @ReadOnlyComposable get() = LocalPc.current.isDark

    /** A club colour as a token-layer Color (team colours come from data as ARGB longs). */
    fun team(argb: Long): Color = Color(argb)
}

/** Spacing scale: 4, 8, 12, 16, 24, 32. Screen padding 16, bento gutter 12. */
object PcSpace {
    val xxs = 4.dp
    val xs = 8.dp
    val sm = 12.dp
    val md = 16.dp
    val lg = 24.dp
    val xl = 32.dp
    val screen = 16.dp
    val gutter = 12.dp
    /** Minimum touch target. */
    val touch = 48.dp
}

/** Shapes: tiles 16, buttons and controls 12, chips fully rounded, crests circular. */
object PcShape {
    val Tile = RoundedCornerShape(16.dp)
    val Control = RoundedCornerShape(12.dp)
    val Chip = RoundedCornerShape(percent = 50)
    val Crest = CircleShape
    /** Small inner marks (badges, bars) that sit inside a tile. */
    val Small = RoundedCornerShape(6.dp)
    val SheetTop = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp)

    // Compatibility aliases for older call sites.
    val Card = Tile
    val Hero = Tile
    val Badge = Small
}

/** Bundled fonts: Manrope (variable weight) for UI, Barlow Condensed for scores. Both SIL OFL. */
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

private const val TNUM = "tnum"

/**
 * Type roles from the brief. Every score, minute, time and table number uses a style with tabular
 * numerals (the Score* roles and [Numeric]), so digits line up and do not jiggle during live updates.
 */
object PcType {
    /** 56sp condensed bold: the hero score. */
    val ScoreDisplay: TextStyle @Composable @ReadOnlyComposable get() = TextStyle(fontFamily = PcFont.Score, fontWeight = FontWeight.Bold, fontSize = 56.sp, lineHeight = 56.sp, fontFeatureSettings = TNUM, color = Pc.Text)
    /** 28sp condensed bold: mini tiles, rail items, big numbers in tiles. */
    val ScoreCompact: TextStyle @Composable @ReadOnlyComposable get() = TextStyle(fontFamily = PcFont.Score, fontWeight = FontWeight.Bold, fontSize = 28.sp, lineHeight = 30.sp, fontFeatureSettings = TNUM, color = Pc.Text)
    /** 20sp condensed: scores inside rows and table points. */
    val ScoreRow: TextStyle @Composable @ReadOnlyComposable get() = TextStyle(fontFamily = PcFont.Score, fontWeight = FontWeight.Bold, fontSize = 20.sp, lineHeight = 22.sp, fontFeatureSettings = TNUM, color = Pc.Text)
    val Title: TextStyle @Composable @ReadOnlyComposable get() = TextStyle(fontFamily = PcFont.Body, fontWeight = FontWeight.SemiBold, fontSize = 24.sp, lineHeight = 30.sp, color = Pc.Text)
    /** 20sp: lead news headlines (between title and subtitle). */
    val Headline: TextStyle @Composable @ReadOnlyComposable get() = TextStyle(fontFamily = PcFont.Body, fontWeight = FontWeight.Bold, fontSize = 20.sp, lineHeight = 26.sp, color = Pc.Text)
    val Subtitle: TextStyle @Composable @ReadOnlyComposable get() = TextStyle(fontFamily = PcFont.Body, fontWeight = FontWeight.SemiBold, fontSize = 18.sp, lineHeight = 24.sp, color = Pc.Text)
    val Body: TextStyle @Composable @ReadOnlyComposable get() = TextStyle(fontFamily = PcFont.Body, fontWeight = FontWeight.Normal, fontSize = 15.sp, lineHeight = 22.sp, color = Pc.Text)
    val BodyStrong: TextStyle @Composable @ReadOnlyComposable get() = TextStyle(fontFamily = PcFont.Body, fontWeight = FontWeight.SemiBold, fontSize = 15.sp, lineHeight = 22.sp, color = Pc.Text)
    val BodyMuted: TextStyle @Composable @ReadOnlyComposable get() = TextStyle(fontFamily = PcFont.Body, fontWeight = FontWeight.Normal, fontSize = 15.sp, lineHeight = 22.sp, color = Pc.Text2)
    /** Long-form reading (article dek and body): body size with a looser line height. */
    val Reading: TextStyle @Composable @ReadOnlyComposable get() = TextStyle(fontFamily = PcFont.Body, fontWeight = FontWeight.Normal, fontSize = 16.sp, lineHeight = 26.sp, color = Pc.Text)
    val Label: TextStyle @Composable @ReadOnlyComposable get() = TextStyle(fontFamily = PcFont.Body, fontWeight = FontWeight.Medium, fontSize = 13.sp, lineHeight = 18.sp, color = Pc.Text)
    val LabelStrong: TextStyle @Composable @ReadOnlyComposable get() = TextStyle(fontFamily = PcFont.Body, fontWeight = FontWeight.Bold, fontSize = 13.sp, lineHeight = 18.sp, color = Pc.Text)
    val Caption: TextStyle @Composable @ReadOnlyComposable get() = TextStyle(fontFamily = PcFont.Body, fontWeight = FontWeight.Normal, fontSize = 12.sp, lineHeight = 16.sp, color = Pc.Text2)
    /** Uppercase kicker: competition labels, tile titles, section kickers. Callers pass uppercase text. */
    val Overline: TextStyle @Composable @ReadOnlyComposable get() = TextStyle(fontFamily = PcFont.Body, fontWeight = FontWeight.Medium, fontSize = 11.sp, lineHeight = 14.sp, letterSpacing = 0.8.sp, color = Pc.Text2)
    /** UI-font numbers (times, table columns, counts) with tabular figures. */
    val Numeric: TextStyle @Composable @ReadOnlyComposable get() = TextStyle(fontFamily = PcFont.Body, fontWeight = FontWeight.Medium, fontSize = 13.sp, lineHeight = 18.sp, fontFeatureSettings = TNUM, color = Pc.Text)
    /** Tiny count bubbles (nav badge). */
    val Micro: TextStyle @Composable @ReadOnlyComposable get() = TextStyle(fontFamily = PcFont.Body, fontWeight = FontWeight.ExtraBold, fontSize = 10.sp, lineHeight = 12.sp, fontFeatureSettings = TNUM, color = Pc.Text)
    /** Brand wordmark (onboarding welcome, splash). */
    val Display: TextStyle @Composable @ReadOnlyComposable get() = TextStyle(fontFamily = PcFont.Body, fontWeight = FontWeight.ExtraBold, fontSize = 32.sp, lineHeight = 38.sp, color = Pc.Text)
}

@Composable
fun PitchsideTheme(dark: Boolean = false, content: @Composable () -> Unit) {
    val c = if (dark) DarkPc else LightPc
    val scheme = if (dark) {
        darkColorScheme(
            primary = c.accent, onPrimary = c.onAccent, primaryContainer = c.accentContainer, onPrimaryContainer = c.text,
            secondary = c.live, onSecondary = c.onLive, secondaryContainer = c.liveContainer, onSecondaryContainer = c.text,
            tertiary = c.warning, onTertiary = c.bg, tertiaryContainer = c.warningContainer, onTertiaryContainer = c.text,
            background = c.bg, onBackground = c.text, surface = c.surface, onSurface = c.text, surfaceVariant = c.surfaceVariant,
            onSurfaceVariant = c.text2, surfaceContainerHigh = c.surface, surfaceContainerHighest = c.surfaceVariant,
            outline = c.outlineStrong, outlineVariant = c.outline, error = c.error, onError = c.bg, scrim = c.scrim,
        )
    } else {
        lightColorScheme(
            primary = c.accent, onPrimary = c.onAccent, primaryContainer = c.accentContainer, onPrimaryContainer = c.text,
            secondary = c.live, onSecondary = c.onLive, secondaryContainer = c.liveContainer, onSecondaryContainer = c.text,
            tertiary = c.warning, onTertiary = c.surface, tertiaryContainer = c.warningContainer, onTertiaryContainer = c.text,
            background = c.bg, onBackground = c.text, surface = c.surface, onSurface = c.text, surfaceVariant = c.surfaceVariant,
            onSurfaceVariant = c.text2, surfaceContainerHigh = c.surface, surfaceContainerHighest = c.surfaceVariant,
            outline = c.outlineStrong, outlineVariant = c.outline, error = c.error, onError = c.surface, scrim = c.scrim,
        )
    }
    CompositionLocalProvider(LocalPc provides c) {
        MaterialTheme(
            colorScheme = scheme,
            typography = Typography(
                bodyLarge = PcType.Body, bodyMedium = PcType.BodyMuted, bodySmall = PcType.Caption,
                titleLarge = PcType.Title, titleMedium = PcType.Subtitle, titleSmall = PcType.BodyStrong,
                labelLarge = PcType.LabelStrong, labelMedium = PcType.Label, labelSmall = PcType.Caption,
                headlineSmall = PcType.Title,
            ),
            content = content,
        )
    }
}
