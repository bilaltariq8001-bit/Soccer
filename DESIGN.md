# Pitchside visual system (v3, "Matchday Hub")

## Direction

Football intelligence + live broadcast + modern editorial. Calm, sharp, scannable. The live match is the
strongest element on any screen it appears on: it sits on a deep-green stage with light text while every
other tile shares one neutral surface. Reporting gets a quieter reading surface; tables and settings stay
low-noise. No pastel-per-tile, no gradients, no glass, no heavy shadows.

This version keeps Muhammad's v2 foundations (warm ivory canvas, pitch-green stage, Manrope + Barlow
Condensed, animated splash, 4dp rhythm) and replaces the v2 lime/coral accents with the redesign brief's
palette: one brand green and a red that means LIVE and nothing else.

## Where the tokens live

`app/app/src/main/java/com/innovativeinside/pitchside/ui/theme/Theme.kt` is the single source of truth.

| Kind | API | Example |
| --- | --- | --- |
| Colour | `Pc.<Role>` (follows light/dark) | `Pc.Surface`, `Pc.Accent`, `Pc.LiveText` |
| Type | `PcType.<Role>` | `PcType.ScoreDisplay`, `PcType.Overline` |
| Spacing | `PcSpace.<step>` | `PcSpace.md` (16), `PcSpace.gutter` (12) |
| Shape | `PcShape.<shape>` | `PcShape.Tile` (16), `PcShape.Control` (12), `PcShape.Chip` (pill) |

Window/splash colours that must exist before Compose runs are mirrored in `res/values/colors.xml` and
`res/values-night/colors.xml`.

## Colour roles

| Role | Light | Dark | Use |
| --- | --- | --- | --- |
| bg | `#F4F3EC` | `#0C0F0D` | Page canvas |
| surface | `#FFFFFF` | `#151915` | Tiles, rows, sheets |
| surfaceVariant | `#ECEBE2` | `#1D221D` | Secondary tiles, unselected chips, skeletons |
| outline | `#E0DED3` | `#2A302A` | 1dp borders (instead of shadows) |
| text / text2 | `#14181F` / `#5F6670` | `#F2F4F1` / `#A4ABA3` | Primary and secondary text |
| text3 | `#8A9099` | `#7E857D` | Decorative only (dividers, placeholders) |
| accent / accentContainer / onAccent | `#1F4D35` / `#E3EBDD` / `#FFFFFF` | `#4FBF86` / `#1B3427` / `#0C0F0D` | Brand, selection, primary buttons |
| live / liveContainer | `#D94841` / `#FBE9E7` | `#FF5A52` / `#3A1A18` | LIVE state only (dot, progress, counters) |
| liveText / liveFill | `#B83A33` | `#FF6B63` / `#FF5A52` | LIVE text and solid LIVE pill (AA contrast) |
| success / warning / error | `#2E7D4F` / `#C98A1B` / `#C0392B` | `#5CC48A` / `#E0A93A` / `#FF6B5E` | Form dots, zones, errors. Sparingly |
| hero / onHero / onHero2 | `#1F4D35` / `#FFFFFF` / `#B9CDBF` | `#17432F` / `#F2F4F1` / `#B5C9BC` | Live hero tile and match header stage |

Team colours (`Team.color`) are accents only: a 3dp crest ring, a 3dp edge on the next-kickoff tile, the
pitch-view shirts. Never large fills.

Contrast: every text/background pair used for information is at least 4.5:1 (checked: text2 on bg 5.2,
liveText on surface 5.7, onHero2 on hero 5.8, accent on accentContainer 7.9). `#D94841` on white is 4.2,
so red text uses `liveText`; `#D94841` itself is used for the dot, bars and pulse.

## Type

Manrope (bundled variable font) for UI, Barlow Condensed (bundled) for scores. All score roles and
`Numeric` set `fontFeatureSettings = "tnum"` so digits are tabular.

| Role | Size | Use |
| --- | --- | --- |
| ScoreDisplay | 56 condensed bold | Hero score, match header |
| ScoreCompact | 28 condensed bold | Mini live tiles, tile numbers, countdown |
| ScoreRow | 20 condensed bold | Row scores, table points |
| Title | 24 semibold | Screen titles, lead headlines |
| Subtitle | 18 semibold | Section headers, card titles |
| Body / BodyStrong / BodyMuted | 15 | Reading, team names, secondary copy |
| Reading | 16 / 26 line | Article dek and description |
| Label / LabelStrong | 13 medium / bold | Chips, tabs, buttons, row meta |
| Caption | 12 | Timestamps, sources |
| Overline | 11 medium, 0.8 tracking, uppercase | Competition labels, tile kickers |

## Spacing, shape, elevation

- Spacing 4, 8, 12, 16, 24, 32. Screen padding 16. Bento gutter 12.
- Tiles 16dp radius, buttons/controls 12dp, chips fully rounded, crests circular.
- No elevation. Surfaces separate by contrast and a 1dp outline. The bottom bar is docked, not floating.
- Touch targets at least 48dp. Icons: Material outlined set, 24dp (20dp inside dense rows).

## Components (ui/components)

AppTopBar, BottomNavBar (Home, Matches, Leagues, News, Favorites; accent pill on the selected tab),
FilterChipRow, SegmentedControl (with counts), TeamCrest (initials, team-colour ring, S/M/L/XL), LiveBadge
(pulsing dot, reduced-motion aware), ScoreText, LiveMatchTile (deep green hero), MiniLiveTile, MatchRow,
FixtureRailItem, StandingsTile/StandingsRow, NextKickoffTile (countdown), PlayerStatTile, AiInsightTile,
NewsCard (lead/compact, Coil image), SectionHeader, EmptyState, ErrorState, LoadingSkeleton (shimmer),
OfflineBanner, PcDialog, PcSheet. Previews live next to each component and in `ComponentPreviews.kt`.

## Motion

Purposeful only: tile press ripple, hero swap (250ms cross-fade + slide), score-change flash (600ms),
slow LIVE pulse, collapsing match header, skeleton shimmer. When the system animator duration scale is 0
("Remove animations"), pulses, shimmer and the hero swap are disabled (`rememberReducedMotion()`).

## Hierarchy rules

1. Live match state (hero tile) first; nothing else uses the hero stage on Home.
2. Today's fixtures, then glanceable bento tiles on one neutral surface; tiles without data are hidden.
3. Filters and the full match list, then the latest news, visually lighter than the live section.
