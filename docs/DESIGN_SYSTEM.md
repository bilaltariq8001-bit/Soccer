# Pitchside design system

Original to Pitchside: deep navy surfaces with one lime accent and a cyan "live" colour. Nothing is taken from the reference app's green/red branding. Tokens live in `ui/theme/Theme.kt`; components in `ui/components/`. Screens never pick their own colours, radii or fonts.

## Colour (dark is the default; light tuned for 4.5:1 text contrast)

| Token | Dark | Light | Use |
|---|---|---|---|
| `Pc.Bg` | #07111F | #F6F7F9 | Page background |
| `Pc.Surface` | #0D1B2E | #FFFFFF | Cards, fields, nav |
| `Pc.Raised` | #12263D | #EDF0F5 | Selected tab, chips, skeletons |
| `Pc.Border` / `BorderStrong` | #1B3150 / #24405F | #DCE2EB / #C3CDDA | 1dp outlines, dividers |
| `Pc.Text` / `Text2` / `Text3` | #F5F1E8 / #A9B7CC / #8496AF | #0E1420 / #4A5668 / #5F6B7D | Primary, secondary, meta |
| `Pc.Lime` (+ `OnLime`, tints) | #B7F34A | #4B7A00 | The one accent: primary buttons, selection, your club |
| `Pc.Live` | #4DD8FF | #00779A | Live badge, live count |
| `Pc.Amber` | #FFC857 | #8A5A00 | Postponed, yellow cards, warnings |
| `Pc.Red` | #FF6B6B | #C73A2D | Errors, red cards, destructive actions |

Material 3 `colorScheme` is derived from the same tokens (primary = lime, error = red), so Material widgets (DatePicker, Switch, PullToRefresh) match.

## Type
Manrope (variable) for text, Barlow Condensed for scores. Both SIL OFL, bundled in `res/font`.

| Style | Size / line | Use |
|---|---|---|
| Title | 26/32 ExtraBold | Screen titles |
| Headline | 20/26 ExtraBold | Section headers, top bars |
| Subhead | 17/22 Bold | Card titles |
| Body | 16/24 Medium | Paragraphs |
| BodySmall | 14/20 Medium | Secondary text |
| Label | 14/18 Bold | Rows, chips, buttons |
| Meta | 12/16 Medium | Timestamps, captions (minimum size) |
| Overline | 12/16 ExtraBold, +0.8 tracking | Section kickers |
| ScoreXL/L/M/S | 72/44/28/22 Barlow Bold | Scores, big numbers |

## Spacing, shape, elevation
- 4dp grid; screen gutters 16dp (24dp on onboarding/forms); card padding 12–16dp; list gaps 8dp, section gaps 16dp.
- Radii (`PcShape`): Badge 8, Chip 12, Control 14, Card 16, Hero 20; sheets 20 top corners.
- Flat by design: depth comes from surface steps (Bg → Surface → Raised) and 1dp borders, not shadows.
- Touch targets ≥ 48dp everywhere (buttons 52dp, icon actions 48dp, list rows ≥ 48dp).

## Components
| Component | File | Notes |
|---|---|---|
| `PrimaryButton` | Components.kt | Lime fill, 52dp; `loading` shows a spinner and blocks taps; disabled state |
| `SecondaryButton` | Components.kt | Outlined; `danger` variant in red |
| `TextAction`, `IconAction`, `BackButton` | Components.kt | 48dp, focus ring, click label for TalkBack |
| `PcTextField` | Fields.kt | Label above, inline error announced to TalkBack, password visibility toggle |
| `PcTopBar` | Fields.kt | Back + title/subtitle + actions |
| `PcCard` | Components.kt | Surface + 1dp border, optional click |
| `Chip`, `ChipRow`, `FlowChips`, `SegmentedTabs`, `UnderlineTabs` | Components.kt | Selected state exposed to accessibility |
| `MatchCard`, `HeroMatch`, `LeagueGroup` | Components.kt / screens | Merged content description incl. score or "hidden by Spoiler Shield" |
| `StandingsTable` | ScreenBase.kt | Short/Full/Form, your club tinted, match clubs highlighted |
| `BottomNav` / `SideRail` | Components.kt | Bottom bar on phones, rail ≥600dp, live-count badge |
| `PcDialog`, `PcSheet` | Overlays.kt | Confirm dialogs (danger variant) and bottom sheets |
| `InfoBanner` | Components.kt | Toast/snackbar (success), offline (warning), errors |
| `StateContent` | States.kt | One switch for Loading/Empty/Error/Success |
| `ListSkeleton`, `DetailSkeleton`, `MatchCardSkeleton` | States.kt / Components.kt | Pulsing skeletons, never a bare spinner on content |
| `EmptyState`, `ErrorState`, `StateCard` | States.kt | Icon, title, plain explanation, optional action; Retry only when retryable |

## Layout
- Phones: single column, max width 720dp. Tablets (≥600dp): side rail; Home splits into two columns at ≥600dp.
- Pull-to-refresh on every data tab and detail page; a failed refresh keeps data and shows a warning banner.
