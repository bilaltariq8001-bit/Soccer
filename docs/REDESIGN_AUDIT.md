# Pitchside redesign, Phase 0 audit (2026-11-13)

## Stack
Kotlin, Jetpack Compose, Material 3, single-activity, Navigation Compose. No XML views except splash/launcher resources.

## Two diverged code lines (decision needed)
| | `main` (Muhammad, c75226c, 2026-11-13) | `hark/rebuild-app` (PR #1) |
|---|---|---|
| Data | `FakeRepository` object, no loading/error | `FootballApi` (Http + Mock) -> `DefaultFootballRepository` -> 16 ViewModels with `UiState` |
| Theme | New palette (#F4F3EC bg, pitch green, coral live), Manrope + Barlow Condensed fonts bundled, `PcType` roles, animated splash, `DESIGN.md` | Older navy/lime `PcColors`, system fonts |
| Screens | Home, Matches, Match, Content, User, Onboarding (6 files) | 9 screen files incl. League, Team, Article, Auth, Account, Search |
| Tests | none | 57 passing |

Proposal: do the redesign on top of `hark/rebuild-app`, porting the fonts, splash and token work from `main` into it, then merge to `main` once approved.

## Screen / route map (rebuild branch, `ui/AppRoot.kt`)
- Onboarding: Welcome, Language, Teams, Watch, Alerts, Persona, Building (`ui/screens/Onboarding.kt`)
- Tabs: Home (`HomeScreen.kt`), Matches (`MatchesScreen.kt`), Leagues (`BrowseScreens.kt`), News (`NewsScreens.kt`), Favorites (`FavoritesSearch.kt`)
- Detail: Match + Lineup (`MatchDetailScreen.kt`, deep link `pitchside://match/{id}`), League (deep link), Team (deep link), Player (`BrowseScreens.kt`), Article (`NewsScreens.kt`)
- Other: Search (`FavoritesSearch.kt`), Ask, Paywall, Settings, Auth (sign in / create / reset), Account (`AccountScreens.kt`)
- Overlays: exit dialog, `PcDialog`, `PcSheet` (`ui/components/Overlays.kt`); date picker in Matches
- Shell: `BottomNav` (5 tabs, live badge) on compact, `SideRail` on medium/expanded

## Shared components (`ui/components/`)
PcCard, Crest, StatusBadge, ScoreText, MatchCard, TeamScoreRow, Primary/SecondaryButton, TextAction, IconAction, BackButton, Chip, ChipRow, FlowChips, SegmentedTabs, UnderlineTabs, PcSwitch, SectionHeader, Overline, PhotoPlaceholder, BriefCard, StatBar, FormChip, InfoBanner, MatchCardSkeleton, BottomNav, SideRail, LiveCountBadge, PcTextField, PcTopBar, PcDialog, PcSheet, SearchField, ErrorState, StateCard, EmptyState, ListSkeleton, DetailSkeleton.

## Hard-coded styles
- Raw `Color(0x..)` outside theme: MatchDetailScreen (2), Overlays (1).
- Raw `.sp` sizes outside theme: ~70 across screens (Browse 17, Account 13, Onboarding 13, MatchDetail 9, Home 7, others).
- Ad-hoc radii in several cards; no single shape scale on the branch (main has one).
- No reduced-motion handling; no offline/connectivity detection (InfoBanner exists but is not driven by network state).

## Data available vs. Image B / brief
| Element | Status | Source |
|---|---|---|
| Live hero (teams, score, minute, venue, competition) | Yes | `Match` |
| Stat strip (possession, shots) | Yes | `stats(fixtureId)` StatRow list |
| AI insight line | Yes (text only) | `Match.brief` |
| Win probability %, confidence ring | **No** | omit, needs backend |
| Standings top 3, played, GD, points, form | Yes | `standings(leagueId)` |
| Standings movement arrows | **No** (no previous rank) | needs backend |
| Zone markers | Partial (derive from rank + league rules, hard-coded) | needs backend for real rules |
| Next kickoff + countdown | Yes (derived) | fixtures + favorites + `kickoffMs` |
| Top scorer name + goals | Yes | `scorers(leagueId)` |
| Player photo | **No** (initials fallback only) | needs image URLs |
| Team crests | **No images** (initials + `Team.color`) | needs logo URLs |
| Team colours | Yes | `Team.color` |
| News lead/compact with image | Text yes, **image no** | `NewsItem` has no image URL |
| News filters | Reports, Transfers, Analysis | `NewsCategory` |
| News by language (Italian / French / Spanish) | **No source in the repo** | see open question |
| Notifications centre (Today / Earlier) | **No data or screen** | needs backend (FCM not connected) |
| Recent searches | Not stored | can add to DataStore (small, local) |
| Trending searches | **No** | needs backend |
| Player follow | Partial (teams + leagues followed; players not) | prefs change |
| Theme System/Light/Dark | Yes | `UserPrefs` |
| Quiet hours | **No** | prefs addition (local, small) |

## Open questions for Muhammad
1. Base branch: redesign on `hark/rebuild-app` and bring over your fonts/splash/tokens from `main`? (recommended)
2. The Italian / French / Spanish news API: it is not in this repo or its history. Which API or app is it (name, link, or the file)?
