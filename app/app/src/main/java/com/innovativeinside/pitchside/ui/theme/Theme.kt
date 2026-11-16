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
 * Pitchside design tokens (0.6 "Football Today" language), the single source of truth for colour, type,
 * spacing and shape. Dark charcoal is the primary theme; the light variant keeps the same structure.
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
    /** Emerald used only for soft radial glows behind hero areas (drawn at low alpha). */
    val glow: Color,
    /** Accent on the always-dark hero stage (the LIVE dot and label inside hero cards). */
    val heroAccent: Color,
    val isDark: Boolean,
)

// Light variant: same structure as dark (charcoal hero stage, emerald accent), on a soft off-white page.
val LightPc = PcColors(
    bg = Color(0xFFF2F4F2), surface = Color(0xFFFFFFFF), surfaceVariant = Color(0xFFE8ECE9),
    outline = Color(0x1A0E1110), outlineStrong = Color(0x330E1110),
    text = Color(0xFF0E1110), text2 = Color(0xFF5A635E), text3 = Color(0xFF8A928D),
    accent = Color(0xFF0F7D3B), accentContainer = Color(0xFFDDF5E5), onAccent = Color(0xFFFFFFFF),
    live = Color(0xFF16A34A), liveText = Color(0xFF0F7D3B), liveContainer = Color(0xFFDDF5E5), liveFill = Color(0xFF0F7D3B), onLive = Color(0xFFFFFFFF),
    success = Color(0xFF16A34A), successContainer = Color(0xFFDDF5E5),
    warning = Color(0xFFB7791F), warningContainer = Color(0xFFFBF0DA),
    error = Color(0xFFD03A3A), errorContainer = Color(0xFFFBE4E4),
    hero = Color(0xFF151B18), heroRaised = Color(0xFF212925), onHero = Color(0xFFF3F5F4), onHero2 = Color(0xFFA3ACA7), heroOutline = Color(0x332EE06E),
    neutral = Color(0xFFC3C9C5), nav = Color(0xFFFFFFFF), scrim = Color(0x99000000), onTeam = Color(0xFFFFFFFF),
    pitch = Color(0xFF0F3A22), pitchLine = Color(0x47FFFFFF),
    glow = Color(0xFF22C55E), heroAccent = Color(0xFF2EE06E),
    isDark = false,
)

// Dark (default): premium charcoal, 1dp white-alpha borders, emerald accent and a soft emerald glow.
val DarkPc = PcColors(
    bg = Color(0xFF0E1110), surface = Color(0xFF1A1F1C), surfaceVariant = Color(0xFF232A26),
    outline = Color(0x17FFFFFF), outlineStrong = Color(0x2BFFFFFF),
    text = Color(0xFFF3F5F4), text2 = Color(0xFF9AA39E), text3 = Color(0xFF6E7772),
    accent = Color(0xFF2EE06E), accentContainer = Color(0xFF143323), onAccent = Color(0xFF06140B),
    live = Color(0xFF2EE06E), liveText = Color(0xFF2EE06E), liveContainer = Color(0xFF143323), liveFill = Color(0xFF2EE06E), onLive = Color(0xFF06140B),
    success = Color(0xFF22C55E), successContainer = Color(0xFF143323),
    warning = Color(0xFFF5B83D), warningContainer = Color(0xFF33290F),
    error = Color(0xFFFF6467), errorContainer = Color(0xFF3A1A1B),
    hero = Color(0xFF18201C), heroRaised = Color(0xFF222C27), onHero = Color(0xFFF3F5F4), onHero2 = Color(0xFF9AA39E), heroOutline = Color(0x332EE06E),
    neutral = Color(0xFF3B443F), nav = Color(0xFF151A17), scrim = Color(0xB3000000), onTeam = Color(0xFFFFFFFF),
    pitch = Color(0xFF0F3A22), pitchLine = Color(0x40FFFFFF),
    glow = Color(0xFF22C55E), heroAccent = Color(0xFF2EE06E),
    isDark = true,
)

val LocalPc = staticCompositionLocalOf { DarkPc }

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
    val Glow: Color @Composable @ReadOnlyComposable get() = LocalPc.current.glow
    val HeroAccent: Color @Composable @ReadOnlyComposable get() = LocalPc.current.heroAccent
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
    /** Space between home sections. */
    val section = 28.dp
}

/** Shapes: cards 20, hero cards 24, controls 14, chips and pills fully rounded, crests circular. */
object PcShape {
    val Tile = RoundedCornerShape(20.dp)
    val Hero = RoundedCornerShape(24.dp)
    val Control = RoundedCornerShape(14.dp)
    val Chip = RoundedCornerShape(percent = 50)
    val Crest = CircleShape
    /** Small inner marks (badges, bars, form pills) that sit inside a card. */
    val Small = RoundedCornerShape(8.dp)
    val SheetTop = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
    /** The floating bottom bar. */
    val Nav = RoundedCornerShape(24.dp)

    // Compatibility aliases for older call sites.
    val Card = Tile
    val Badge = Small
}

/** Bundled fonts: Manrope (variable weight) for body, Barlow Condensed for display, headings and scores. Both SIL OFL. */
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
    /** Condensed sports display face for screen titles, section headers and team names. */
    val Display: FontFamily = Score
}

private const val TNUM = "tnum"

/**
 * Type roles from the brief. Every score, minute, time and table number uses a style with tabular
 * numerals (the Score* roles and [Numeric]), so digits line up and do not jiggle during live updates.
 */
object PcType {
    /** 64sp condensed bold: the hero score ("2 - 1"). */
    val ScoreDisplay: TextStyle @Composable @ReadOnlyComposable get() = TextStyle(fontFamily = PcFont.Score, fontWeight = FontWeight.Bold, fontSize = 64.sp, lineHeight = 64.sp, fontFeatureSettings = TNUM, color = Pc.Text)
    /** 30sp condensed bold UPPERCASE screen title ("FOOTBALL TODAY"). Callers pass uppercase text. */
    val ScreenTitle: TextStyle @Composable @ReadOnlyComposable get() = TextStyle(fontFamily = PcFont.Display, fontWeight = FontWeight.Bold, fontSize = 30.sp, lineHeight = 32.sp, letterSpacing = 0.4.sp, color = Pc.Text)
    /** 22sp condensed bold UPPERCASE section header ("LIVE MATCH", "TOP MATCHES"). */
    val SectionTitle: TextStyle @Composable @ReadOnlyComposable get() = TextStyle(fontFamily = PcFont.Display, fontWeight = FontWeight.Bold, fontSize = 22.sp, lineHeight = 26.sp, letterSpacing = 0.3.sp, color = Pc.Text)
    /** 18sp condensed bold: team names in hero cards and rows (uppercase), card titles. */
    val TeamName: TextStyle @Composable @ReadOnlyComposable get() = TextStyle(fontFamily = PcFont.Display, fontWeight = FontWeight.Bold, fontSize = 18.sp, lineHeight = 21.sp, letterSpacing = 0.3.sp, color = Pc.Text)
    /** 16sp condensed semibold: team names in dense rows and compact cards. */
    val TeamNameSmall: TextStyle @Composable @ReadOnlyComposable get() = TextStyle(fontFamily = PcFont.Display, fontWeight = FontWeight.SemiBold, fontSize = 16.sp, lineHeight = 19.sp, letterSpacing = 0.3.sp, color = Pc.Text)
    /** 13sp grey uppercase kicker under screen titles ("SUNDAY, NOV 15"). */
    val Kicker: TextStyle @Composable @ReadOnlyComposable get() = TextStyle(fontFamily = PcFont.Body, fontWeight = FontWeight.SemiBold, fontSize = 13.sp, lineHeight = 18.sp, letterSpacing = 1.sp, color = Pc.Text2)
    /** 14sp condensed bold uppercase label with tracking: nav labels, SEE ALL, pill tabs, LIVE. */
    val Tag: TextStyle @Composable @ReadOnlyComposable get() = TextStyle(fontFamily = PcFont.Display, fontWeight = FontWeight.Bold, fontSize = 15.sp, lineHeight = 18.sp, letterSpacing = 0.6.sp, fontFeatureSettings = TNUM, color = Pc.Text)
    /** 28sp condensed bold: mini tiles, rail items, big numbers in tiles. */
    val ScoreCompact: TextStyle @Composable @ReadOnlyComposable get() = TextStyle(fontFamily = PcFont.Score, fontWeight = FontWeight.Bold, fontSize = 28.sp, lineHeight = 30.sp, fontFeatureSettings = TNUM, color = Pc.Text)
    /** 20sp condensed: scores inside rows and table points. */
    val ScoreRow: TextStyle @Composable @ReadOnlyComposable get() = TextStyle(fontFamily = PcFont.Score, fontWeight = FontWeight.Bold, fontSize = 20.sp, lineHeight = 22.sp, fontFeatureSettings = TNUM, color = Pc.Text)
    /** 28sp condensed bold: page titles on detail screens (team, league, player, auth). */
    val Title: TextStyle @Composable @ReadOnlyComposable get() = TextStyle(fontFamily = PcFont.Display, fontWeight = FontWeight.Bold, fontSize = 28.sp, lineHeight = 32.sp, letterSpacing = 0.3.sp, color = Pc.Text)
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
    val Overline: TextStyle @Composable @ReadOnlyComposable get() = TextStyle(fontFamily = PcFont.Display, fontWeight = FontWeight.Bold, fontSize = 13.sp, lineHeight = 16.sp, letterSpacing = 0.8.sp, color = Pc.Text2)
    /** UI-font numbers (times, table columns, counts) with tabular figures. */
    val Numeric: TextStyle @Composable @ReadOnlyComposable get() = TextStyle(fontFamily = PcFont.Body, fontWeight = FontWeight.Medium, fontSize = 13.sp, lineHeight = 18.sp, fontFeatureSettings = TNUM, color = Pc.Text)
    /** Tiny count bubbles (nav badge). */
    val Micro: TextStyle @Composable @ReadOnlyComposable get() = TextStyle(fontFamily = PcFont.Body, fontWeight = FontWeight.ExtraBold, fontSize = 10.sp, lineHeight = 12.sp, fontFeatureSettings = TNUM, color = Pc.Text)
    /** Brand wordmark and big statements (onboarding welcome, paywall). */
    val Display: TextStyle @Composable @ReadOnlyComposable get() = TextStyle(fontFamily = PcFont.Display, fontWeight = FontWeight.Bold, fontSize = 40.sp, lineHeight = 42.sp, letterSpacing = 0.3.sp, color = Pc.Text)
}

@Composable
fun PitchsideTheme(dark: Boolean = true, content: @Composable () -> Unit) {
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
