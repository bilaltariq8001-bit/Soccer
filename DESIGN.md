# Pitchside visual system (v4, "Football Today", 0.6.0)

## Direction

Premium dark sports app. Charcoal canvas, charcoal cards with 1dp white-alpha borders and large radii, one
emerald accent, and a soft emerald glow behind hero areas. Condensed UPPERCASE sports headings (Barlow
Condensed), Manrope for body, tabular numbers for every score and stat. Dark is the primary theme; the light
variant keeps the same structure (and the hero cards stay dark in both).

The reference is the "Football Today" home mock: big screen title with the date, a glowing LIVE MATCH hero
card, TOP MATCHES cards, a TODAY'S HIGHLIGHTS list, MINI STANDINGS & NEWS, and a floating bottom bar.

## Where the tokens live

`app/app/src/main/java/com/innovativeinside/pitchside/ui/theme/Theme.kt` is the single source of truth.

| Kind | API | Example |
| --- | --- | --- |
| Colour | `Pc.<Role>` (follows light/dark) | `Pc.Surface`, `Pc.Accent`, `Pc.Glow`, `Pc.HeroAccent` |
| Type | `PcType.<Role>` | `PcType.ScreenTitle`, `PcType.SectionTitle`, `PcType.TeamName`, `PcType.ScoreDisplay`, `PcType.Tag` |
| Spacing | `PcSpace.<step>` | `PcSpace.md` (16), `PcSpace.section` (28), `PcSpace.touch` (48) |
| Shape | `PcShape.<shape>` | `Tile` (20), `Hero` (24), `Control` (14), `Chip` (pill), `Nav` (24) |

Window/splash colours that must exist before Compose runs are mirrored in `res/values/colors.xml` and
`res/values-night/colors.xml` (charcoal stage, emerald glow and accent).

## Colour roles (dark / light)

| Role | Dark | Light | Use |
| --- | --- | --- | --- |
| bg | `#0E1110` | `#F2F4F2` | Canvas (with the emerald radial glow at the top) |
| surface / surfaceVariant | `#1A1F1C` / `#232A26` | `#FFFFFF` / `#E8ECE9` | Cards, rows / table headers, skeletons, tracks |
| outline / outlineStrong | white 9% / 17% | ink 10% / 20% | 1dp borders and dividers (no shadows) |
| text / text2 / text3 | `#F3F5F4` / `#9AA39E` / `#6E7772` | `#0E1110` / `#5A635E` / `#8A928D` | Primary, secondary (dates, labels), decorative |
| accent / accentContainer / onAccent | `#2EE06E` / `#143323` / `#06140B` | `#0F7D3B` / `#DDF5E5` / `#FFFFFF` | SEE ALL, active nav, selection, primary buttons |
| live / liveText | `#2EE06E` | `#16A34A` / `#0F7D3B` | LIVE dot and label (LIVE is green, as in the reference) |
| hero / heroRaised / heroOutline | `#18201C` / `#222C27` / emerald 20% | same (always dark) | GlowCard gradient and border |
| glow / heroAccent | `#22C55E` / `#2EE06E` | same | Radial glows (low alpha) / accent on hero cards |
| success / warning / error | `#22C55E` / `#F5B83D` / `#FF6467` | `#16A34A` / `#B7791F` / `#D03A3A` | Form W / play-off zone, yellow cards / L, relegation, red cards |

## Type

| Role | Face | Use |
| --- | --- | --- |
| ScreenTitle 30sp | Barlow Condensed Bold, uppercase | "FOOTBALL TODAY", "MATCHES", "NEWS" |
| Kicker 13sp | Manrope SemiBold, tracked, grey, uppercase | Date under the title ("SUNDAY, NOV 15") |
| SectionTitle 22sp | Barlow Condensed Bold, uppercase | "LIVE MATCH", "TOP MATCHES", with a grey Manrope count suffix |
| TeamName 18sp / TeamNameSmall 16sp | Barlow Condensed | Team names in hero cards and rows (uppercase) |
| ScoreDisplay 64sp / ScoreCompact 28sp / ScoreRow 20sp | Barlow Condensed Bold, tabular | Scores, big numbers |
| Tag 15sp | Barlow Condensed Bold, tracked | SEE ALL, nav labels, pill tabs, LIVE, minutes |
| Body / Caption / Label | Manrope | Reading text, meta, controls |

## Components (`ui/components/`)

ScreenHeader, AppTopBar (alias), Avatar, SectionHeader (count + SEE ALL), PremiumCard / PcCard, GlowCard,
glowBackground, LiveBadge, ScoreHero / ScoreText (animated), LiveMatchTile (live hero), MatchCardCompact,
HighlightRow, MatchRow, MiniLiveTile, CrestImage / TeamCrest (Coil + initials fallback), LeagueLogo /
LeagueBadge, FormPills (compact pills and lettered squares), StandingsRow / StandingsHeader / StandingsTable with
zone markers and ZoneLegend, MiniStandingsCard, MatchedHeightPair, StatBar, ProbabilityBar, NewsHeroCard,
NewsRow, NewsImage, PillTabs, SegmentedControl, Chip / ChipRow / FilterChipRow / FlowChips, PrimaryButton,
SecondaryButton, TextAction, IconAction, BackButton, InfoBanner / OfflineBanner, EmptyState, ErrorState,
StateCard, StateContent (crossfades), skeleton shimmer + Home/News/Detail/List skeletons, BottomNavBar (floating),
SideRail, PcDialog, PcSheet, SearchField, PcTextField, BentoTile, NextKickoffTile, PlayerAvatar, PlayerStatTile.

## Motion

Pulsing live dot, score slide + flash on change, 0.97 press scale on cards and primary buttons, crossfade between
loading / empty / error / content, dark shimmer skeletons, pager for several live matches. All of it is skipped
when the system "Remove animations" setting is on (`rememberReducedMotion`).

## Accessibility

Touch targets 48dp (pills draw 38-40dp inside a 48dp target). Every match, card and row has a spoken summary;
colour is never the only signal (form pills carry letters or a spoken summary, LIVE always says LIVE, table zones
are named in the legend and in the row description). Bottom-nav labels cap their font scaling at 1.15x; the mini
standings / news pair stacks on narrow screens or large fonts; the hero's competition mark moves to its own line
with large fonts.
