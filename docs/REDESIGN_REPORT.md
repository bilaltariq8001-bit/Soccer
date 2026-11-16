# Pitchside redesign report (v0.3.0, "Matchday Hub")

Branch `hark/rebuild-app`, local commits only (not pushed). Brief: `docs/REDESIGN_BRIEF.md`. Audit: `docs/REDESIGN_AUDIT.md`.
Package root `app/app/src/main/java/com/innovativeinside/pitchside/` is shortened to `…/`.

## 1. What changed, by screen

| Screen | File | What it is now |
|---|---|---|
| Home | `…/ui/screens/HomeScreen.kt` | "Football today" top bar (date, Ask, search, notifications). **Live now** header with a red "3 live" counter, the deep-green `LiveMatchTile` hero (competition · venue, LIVE 74', crests, 56sp score, possession/shots strip when stats exist, minute progress, AI insight line from `Match.brief`). Other live games as `MiniLiveTile`s underneath; tapping one promotes it (250ms fade + slide, none with reduced motion); swiping the hero steps through live games. **Today** fixture rail (upcoming first). **Bento** 2-column grid: Standings (top 3 of a followed league with a table, else Premier Division), Next kickoff (followed team, live countdown, reminder bell, add to calendar), Top scorer, AI insight. Tiles without data are dropped and an odd tile spans the row, so there are no holes. **Competitions** chips (All + competitions playing today) filter Home and the Matches tab (shared state in `AppViewModel.competition`). **Matches** with Live / Upcoming / Finished counts and `MatchRow`s. **Latest**: 1 lead + 2 compact 90min stories. Spoiler Shield banner kept. Loading = layout-shaped skeleton. |
| Match detail | `…/ui/screens/MatchDetailScreen.kt` | Deep-green score stage (XL crests that open the club, flashing score on change, LIVE badge, scorers, venue · kickoff). A compact score bar appears at the top once the stage scrolls away (collapsing header). Tabs only for data that exists: Overview, Timeline, Stats, Lineups, Table. Overview is bento: AI insight, Key events, Form (from the table's form string), Key numbers, match facts. Timeline is a centre rail with minute markers and goal/card/sub icons. Stats are paired bars. Lineups list + pitch view (pitch colours now tokens). Alerts bell and Spoiler Shield kept. |
| Matches tab | `…/ui/screens/MatchesScreen.kt` | Top bar with calendar picker; 7-day date strip (Yesterday / Today / Tomorrow named); chips row: "My teams" toggle + competition chips; segmented All / Live / Upcoming / Finished with counts; matches grouped by league with collapsible headers (badge, name, count, "Table" link); rows with live progress bar, favourite star (sheet to follow either club), kickoff reminder bell. Pull to refresh. |
| Leagues tab | `…/ui/screens/BrowseScreens.kt` (`LeaguesContent`) | Competition chips; header with follow star and link to the full page; Table / Fixtures / Top scorers toggle; table in a lazy list with a **sticky header**, form dots with letters, tabular numbers, row tap opens the team; then All competitions (follow toggles) and Top-rated players. |
| League / Team / Player pages | `…/ui/screens/BrowseScreens.kt` | Shared header (badge/crest/avatar, name, facts, Follow / Following button). Tabs only for data that exists. Team overview is bento (league position + form, next match) plus stat tiles and facts. |
| News tab | `…/ui/screens/NewsScreens.kt` | 90min headlines. Chips All / France / Italy / Spain / Brazil. Lead card (16:9 image, kicker, headline, dek, "90min · author · date") then compact rows with thumbnails, lazy list that loads the next page near the end. If the newest story is older than a week, an info line says so (the feeds are stale; dates are never faked). Skeleton, empty, error + retry. |
| Article | `…/ui/screens/NewsScreens.kt` (`ArticleRoute`) | Image with photo credit, kicker (edition), headline, author · 90min · date, the feed description, **Read on 90min** button (opens the browser) and a share action. No article scraping. |
| Favorites | `…/ui/screens/FavoritesSearch.kt` | "My matches today" (followed teams' matches as `MatchRow`s) at the top, segmented Teams / Leagues / Players with counts, follow toggles, alert level chips and Spoiler Shield per team, free-follow meter, empty state with suggested (popular) teams. |
| Search | `…/ui/screens/FavoritesSearch.kt` | Pill search field with Cancel; **recent searches** (DataStore, max 8, remove one or clear all); results grouped Teams / Competitions / Players and **Matches today** (filtered locally from today's fixtures). |
| Notifications | `…/ui/screens/NotificationsScreen.kt` (new route `notifications`) | Reachable from the Home bell. Today / Earlier structure with a designed empty state and a link to alert settings (no alert history exists yet). |
| Settings, Auth, Account, Ask, Paywall | `…/ui/screens/AccountScreens.kt` | Restyled through the shared components; theme System / Light / Dark kept; hard-coded sizes replaced by type roles. |
| Onboarding (7 steps) | `…/ui/screens/Onboarding.kt` | Restyled through tokens and components; flow unchanged. |
| Splash | `…/ui/screens/AnimatedSplashScreen.kt`, `res/drawable/ic_pitchside_splash_mark.xml`, `res/values*/colors.xml`, `res/values/themes.xml`, `…/MainActivity.kt` | Ported from `main` (Muhammad's intro) and recoloured to the new stage green; plays once per cold start over the already-composed app; skipped for deep links; honours "remove animations". |
| Shell | `…/ui/AppRoot.kt` | `BottomNavBar` (5 tabs, accent pill, live count on Matches), `SideRail` on wide screens, `OfflineBanner` driven by `ConnectivityManager`, new Notifications route. Every route, ViewModel and deep link (`pitchside://match|league|team/{id}`) is unchanged. |

## 2. Tokens and theme

- File: `…/ui/theme/Theme.kt` (single source of truth). Reference: `DESIGN.md` (rewritten for v3).
- Colours: `Pc.<Role>`, e.g. `Pc.Bg`, `Pc.Surface`, `Pc.SurfaceVariant`, `Pc.Outline`, `Pc.Text/Text2/Text3`, `Pc.Accent/AccentContainer/OnAccent`, `Pc.Live/LiveText/LiveContainer/LiveFill/OnLive`, `Pc.Success/Warning/Error`, `Pc.Hero/OnHero/OnHero2/HeroRaised/HeroOutline`. Light and dark values are exactly the brief's; extra roles (liveText, hero*, containers) are derived for contrast. Team colours go through `Pc.team(argb)` and are only used as rings/edges.
- Type: `PcType.ScoreDisplay (56) / ScoreCompact (28) / ScoreRow (20)` in Barlow Condensed with `tnum`; `Title 24`, `Headline 20` (lead news), `Subtitle 18`, `Body / BodyStrong / BodyMuted 15`, `Reading 16/26`, `Label / LabelStrong 13`, `Caption 12`, `Overline 11 (0.8 tracking, uppercase)`, `Numeric` (Manrope with tabular figures). Fonts bundled in `res/font`.
- Spacing: `PcSpace.xxs 4 / xs 8 / sm 12 / md 16 / lg 24 / xl 32`, `screen 16`, `gutter 12`, `touch 48`.
- Shapes: `PcShape.Tile 16`, `Control 12`, `Chip` (pill), `Crest` (circle), `Small 6` for inner marks.
- Window/splash colours that exist before Compose: `res/values/colors.xml`, `res/values-night/colors.xml`.
- Usage: `Text(x, style = PcType.BodyStrong)`, `Modifier.padding(PcSpace.md)`, `PcCard(...)` for every tile; never `Color(0x…)` or `.sp` in screens.

## 3. Components (`…/ui/components/`)

New: `Navigation.kt` (AppTopBar, BottomNavBar, SideRail, LiveCountBadge), `MatchComponents.kt` (TeamCrest S/M/L/XL, LiveBadge with pulse, StatusBadge, ScoreText with score flash, LiveMatchTile, MiniLiveTile, MatchRow, FixtureRailItem, spoken match descriptions), `BentoTiles.kt` (BentoTile, StandingsTile, StandingsRow, StandingsHeader, NextKickoffTile + countdown, PlayerAvatar, PlayerStatTile, AiInsightTile, BriefCard), `NewsComponents.kt` (NewsImage via Coil, NewsCardLead, NewsCardCompact, honest `newsDate`).
Updated: `Components.kt` (PcCard, Hairline, buttons, IconAction with unread dot, Chip, FilterChipRow, FlowChips, SegmentedControl with counts, UnderlineTabs, PcSwitch, SectionHeader with "See all", Overline, StatBar paired bars, FormChip/FormDots with letters, InfoBanner, OfflineBanner, shimmer `skeleton()`, `rememberReducedMotion()`), `States.kt` (StateContent, ErrorState, EmptyState, ListSkeleton, HomeSkeleton, NewsSkeleton, DetailSkeleton), `Overlays.kt` (PcDialog, PcSheet, pill SearchField with IME search), `Fields.kt`.
Screen-level helpers in `…/ui/screens/ScreenBase.kt`: MatchList (rows + follow sheet), FollowTeamsSheet, LeagueGroup, LeagueBadge, StandingsTable, add-to-calendar intent. Previews: every component file and every screen file has light/dark `@Preview`s.

## 4. News source (90min RSS)

- `…/data/news/NinetyMinNews.kt`: `NewsFeedSource` (interface), `NinetyMinNewsSource` (OkHttp, `https://www.90min.com/{fr|it|es|pt-BR}/feed`), `NinetyMinRssParser` (Android `XmlPullParser`, no new XML library), `NewsText` (entity decoding incl. double-encoded, tag stripping, clean ellipsis for truncated descriptions, RFC 822 dates, utm removal), `NewsImages` (asks the Minute Media CDN for 360/720/1080/1440px instead of 2880px).
- `NewsItem` gained `imageUrl`, `imageCaption`, `edition`, `author`, `language` (defaults keep the old API contract and mock working); `NewsEdition` enum (France, Italy, Spain, Brazil).
- `DefaultFootballRepository.headlines(edition, cursor, refresh)`: per-edition fetch cached 10 min in the existing TTL cache, "All" merges editions newest first and survives one edition failing, client-side pages of 20, `article(id)` served from fetched stories (ids are `<edition>-<guid>`, so after process death only that edition is reloaded). `news(category, cursor)` and `/v1/news` are unchanged.
- Wiring: `AppContainer` always uses the real 90min source (also on mock football data). Previews/tests use `…/data/mock/MockNewsFeedSource.kt`.
- Feed freshness on 2026-11-13: newest items FR 13 Jul 2026, IT 24 May 2026, pt-BR 11 May 2026, ES 11 Sep 2025. The app shows these dates as they are and says when a feed is older than a week.
- **Coil** (`io.coil-kt:coil-compose:2.7.0`, the one new dependency): async loading with memory and disk cache, downsampling, crossfade and error/placeholder slots for news photos. Compose has no built-in network image loader, and hand-rolling one on OkHttp would duplicate caching and bitmap sizing. 2.7.0 is the latest 2.x and matches Kotlin 2.0 / Compose BOM 2024.12.

## 5. Needs backend support (omitted or stubbed)

| Element | Why it is not shown | What would enable it |
|---|---|---|
| Win probability %, confidence ring on AI insight | No probability in any payload; the brief forbids inventing it | A model output field on fixtures |
| Standings movement arrows | No previous rank | `previousRank` on standings rows |
| Qualification / relegation zone colours | No competition rules in data (hard-coding would be guessing) | Zone ranges per league |
| Team crests, player photos | No image URLs | `logoUrl` / `photoUrl` (TeamCrest and PlayerAvatar already fall back to initials) |
| "Last event" on mini live tiles | Fixture list carries no latest event | `lastEvent` on live fixtures |
| Notification centre items | Alerts are not stored or delivered to the app (FCM not connected) | Alert history endpoint + push |
| Trending searches | No data | Trending endpoint |
| Search: match results beyond today | `/v1/search` has no matches | Matches in the search payload (today's are filtered locally) |
| Player follow suggestions, "suggested teams" by taste | Only "popular teams" exists | Recommendations endpoint |
| Full article text | Feeds only carry a description; scraping is out of scope | Licensed content API |
| Current 90min stories | 90min editions stopped updating after moving to Sports Illustrated | Another feed or API (the source is pluggable) |

## 6. Deviations from the brief

- **Base and fonts**: built on `hark/rebuild-app` (real data layer, tests) with Muhammad's fonts, splash and token ideas ported from `main`; `main` is untouched.
- **Light `text3` (#8A9099)** is 2.9:1 on the background, below AA, so it is used only for decorative marks (chevrons, placeholders); all information text uses `text2` or darker. Red LIVE *text* uses `liveText #B83A33` (5.7:1) because `#D94841` is 4.2:1 on white; `#D94841` stays for the dot, bars and pulse.
- **Matches tab segments** are All / Live / Upcoming / Finished (the old All / Live / My teams chips are kept: All and Live as segments, My teams as a chip), so no filter was lost.
- **Favourite star on rows** opens a sheet to follow either club, because favourites are teams, not matches; the bell keeps the existing per-match alerts.
- **News chips** are editions (the requested 90min France/Italy/Spain/Brazil) instead of "For you / Transfers / Match reports"; the old categories remain in the API contract.
- **Zone markers and movement arrows** omitted (section 5) rather than faked.
- **Lazy lists**: News and Leagues are `LazyColumn`s; Home, Matches and detail pages keep the existing scroll `Column`s (bounded content) with lazy horizontal rails.
- **Commits**: phase 1a was built on its own. The following five commits were split from one working tree that was built, tested and linted at the tip; the intermediate commits were not built individually.

## 7. Verification, known issues, next steps

- Build, tests and lint: see `docs/TEST_REPORT.md` (assembleDebug OK, 87 unit/Robolectric tests with 0 failures and 5 opt-in screenshot tests skipped, lint 0 errors).
- Screenshots: `ScreenshotCapture` renders Home (light/dark), News and Match detail with Robolectric native graphics when `PITCHSIDE_SHOTS=<dir>` is set. Robolectric's native runtime has no Linux aarch64 build, so in this sandbox it ran the test JVM as x86_64 under qemu-user; see TEST_REPORT for what was produced.
- Not verified here: a real device or emulator (none available), TalkBack walk-through, 200% font scale and RTL on device (layouts use ellipsis, `weight`, `heightIn` and start/end padding, but this was not seen on screen), live Coil image loading from the CDN on device.
- Known issues: the Favorites team list only resolves followed teams that are in the popular-teams payload (pre-existing); Home's "Today" rail and competition chips only cover today; the countdown ticks every 30 s; NextKickoff looks 3 days ahead.
- Next: wire alert history + push to fill Notifications; add crest/photo URLs; consider a fresher news source behind `NewsFeedSource`; run the instrumented test and an accessibility scan on a device.
