in this app i have previous api in which i take news of Italian ,french and Spanish news you should use that app and just do in the same way as ui provide you n very first and also improve ui of the app
# TASK: Redesign the entire "Pitchside" football app UI, using the attached bento-grid design as the visual direction

You are a senior mobile engineer and product designer working inside an existing codebase. Your job is to redesign the UI of the whole app so it matches a new design direction, WITHOUT breaking existing functionality, data flow, or business logic.

## 0. ATTACHMENTS (read these first)
1. Image A: the CURRENT home screen ("Matchday"). It shows the existing information: a live hero card (Northgate 2–1 Riverside, LIVE 74', venue Northgate Park, AI insight line "Northgate lead 2–1, but Riverside have had 7 shots since half time"), competition filter chips (All, Premier Division, Continental Cup), a "Matches" section with a Live / Upcoming / Finished segmented control, and a bottom nav of Home, Matches, Leagues, News, Favorites.
2. Image B: the NEW DIRECTION, "Matchday Hub". A bento-grid home with a large live match tile, standings tile, next kickoff tile, top scorer tile, and AI insight tile.

Use Image B as the layout and component direction. Use Image A as the source of truth for features and data that must be kept.

## 1. PRODUCT CONTEXT
Pitchside is an Android football app. The home screen must answer immediately: "What is happening in football right now?" The app is a personal football command center: live scores, today's fixtures, standings, news, favorites, and AI insights, all fast to scan, one-handed, and consistent.

## 2. NON-NEGOTIABLE RULES
- Do NOT remove, rename, or break any existing feature, route, API call, model, state management, analytics event, or deep link. This is a UI redesign, not a feature rewrite.
- Do NOT invent features or data the app does not support. If a design element in Image B needs data that does not exist (for example win probability %, top scorer, form dots), render it ONLY if the data source already exists or can be derived from existing data. Otherwise leave it out, or make it a clearly isolated, easily removable component, and list it in the final report under "Needs backend support".
- Keep the original five-tab navigation: Home, Matches, Leagues, News, Favorites. Image B's nav (Home, Matches, Stats, Profile) is NOT the target. Do not drop News, Leagues or Favorites. Keep search and notifications reachable from Home.
- Keep all existing filters and controls: competition chips and the Live / Upcoming / Finished segmented control.
- Follow the project's existing architecture, language, framework, naming and lint rules. Do not migrate frameworks or add heavy dependencies. Prefer what is already in the project.
- Work in small, reviewable commits grouped by phase. Keep the app building and running at the end of every phase.

## 3. PHASE 0: AUDIT (do this before writing any UI code)
1. Detect the stack (e.g. Kotlin + Jetpack Compose, Flutter, React Native, or XML views) and state it. Follow its idioms for everything below (Compose: theme + composables; Flutter: ThemeData + widgets; RN: theme + StyleSheet/components).
2. Map the whole app: list every screen, route, dialog, bottom sheet, and reusable component, with file paths.
3. Identify the current theme/styling approach (colors, typography, spacing, shapes) and where hard-coded values live.
4. Identify the data models and state sources used by each screen (live matches, fixtures, standings, news, favorites, search, notifications, settings).
5. Produce a short written audit: screens found, shared components found, hard-coded styles found, data available vs. missing for each element in Image B. Only then proceed.

## 4. PHASE 1: DESIGN SYSTEM (tokens first, then components)
Create a single source of truth for design tokens and use it everywhere. Replace hard-coded colors, sizes and fonts with tokens. Support light and dark themes.

### 4.1 Design intent
Football intelligence + live sports broadcast + modern editorial product. Calm, sharp, scannable. The LIVE match must dominate. Avoid the generic AI-app look: no random pastel colors per tile, no heavy gradients, no glassmorphism, no heavy shadows, no oversized decorative icons, no huge empty areas.

### 4.2 Color tokens (light theme, adjust if the existing brand palette conflicts, but keep this logic)
- background: warm off-white #F4F3EC
- surface: #FFFFFF
- surfaceVariant: #ECEBE2 (secondary tiles, chips)
- outline: #E0DED3 (1dp borders instead of shadows)
- textPrimary: #14181F
- textSecondary: #5F6670
- textTertiary: #8A9099
- accent (brand green): #1F4D35, accentContainer: #E3EBDD, onAccent: #FFFFFF
- live: #D94841 (LIVE state ONLY), liveContainer: #FBE9E7
- success #2E7D4F, warning #C98A1B, error #C0392B (used sparingly: standings movement, form dots)
- Team colors: used ONLY as small accents (a 3–4dp edge, badge ring, score accent), never as large fills. Resolve team colors from data if available, else fall back to accent.

Dark theme: background #0C0F0D, surface #151915, surfaceVariant #1D221D, outline #2A302A, textPrimary #F2F4F1, textSecondary #A4ABA3, accent #4FBF86, live #FF5A52. Maintain WCAG AA contrast for all text and key icons.

### 4.3 Typography
Use the existing app font if any. Otherwise use a clean sans (Inter, or Roboto Flex on Android) for UI and a condensed bold face for scores (e.g. Barlow Condensed / Oswald), bundled locally. ALL scores, minutes, times and table numbers use tabular numerals.
- scoreDisplay 56sp bold condensed
- scoreCompact 28sp bold condensed
- title 24sp semibold
- subtitle 18sp semibold
- body 15sp regular
- label 13sp medium (chips, tabs)
- caption 12sp regular
- overline 11sp medium, uppercase, 0.8 letter spacing (competition labels, section kickers)

### 4.4 Spacing, shape, elevation
- Spacing scale: 4, 8, 12, 16, 24, 32. Screen horizontal padding 16. Bento gutter 12.
- Shape: tiles 16dp radius, buttons 12dp, chips fully rounded, crests circular. No more than two radius values on a screen apart from chips and crests.
- Elevation: none by default. Separation by surface contrast and 1dp outline. A single subtle shadow only for the floating nav if it floats.
- Minimum touch target 48dp. Icons: 24dp, 2dp outlined style, consistent set.

### 4.5 Core components (build once, reuse everywhere, each with previews/stories where the framework supports it)
1. AppTopBar: title or greeting, date, search action, notifications action (with unread dot). Collapses on scroll where natural.
2. BottomNavBar: 5 tabs (Home, Matches, Leagues, News, Favorites), outlined icon + label, selected state with an accent indicator pill. Never overlaps scrolling content; content has bottom inset padding.
3. FilterChipRow: horizontally scrolling chips, selected = accentContainer + accent text + accent outline.
4. SegmentedControl: Live (count) / Upcoming (count) / Finished (count).
5. TeamCrest: circular badge with fallback initials, sizes S/M/L/XL, optional team-color ring.
6. LiveBadge: pulsing red dot + "LIVE" + minute (e.g. LIVE 74'). Pulse is subtle and respects reduced-motion.
7. ScoreText: the shared score typography component.
8. LiveMatchTile (large, 2x2 hero): overline (competition, venue), LiveBadge, two crests with names, scoreDisplay, a compact stat strip (possession and shots if data exists), an AI insight line with sparkle icon, tap opens Match Detail. Background is deep accent green (dark surface) with light text so it clearly dominates the screen.
9. MiniLiveTile (for the other live matches): crests, scoreCompact, minute, last event if available.
10. MatchRow: time or live minute | crest + name (two lines) | score | favorite star | chevron. States: live (red minute, thin progress bar), upcoming (time + reminder bell), finished (FT, winner emphasized).
11. FixtureRailItem: compact horizontal item with time, two crests, abbreviations, competition overline.
12. StandingsTile and StandingsRow: position, crest, name, played, GD, points, form dots, movement arrows, zone marker.
13. NextKickoffTile: fixture, countdown, add-reminder/calendar action.
14. PlayerStatTile (top scorer): player photo (fallback silhouette), name, stat.
15. AiInsightTile: sparkle icon, short insight text, optional small confidence indicator only if real data exists.
16. NewsCard (editorial): variants: lead (large image, kicker, big headline, source + time) and compact (small image, kicker, headline, meta). Strong headline hierarchy.
17. SectionHeader: title + optional "See all".
18. EmptyState, ErrorState (retry), LoadingSkeleton (shimmer matching the final layout), OfflineBanner.
19. BottomSheet and Dialog styles consistent with tokens.

## 5. PHASE 2: SCREENS (redesign every screen in the app)
For each screen: keep existing data and behavior, apply the new components and tokens, implement loading / empty / error / offline states, and ensure it works in light and dark themes and at large font scales.

### 5.1 Home (bento "Matchday Hub", the primary screen)
Top to bottom:
1. AppTopBar: "Football today" with date, search, notifications.
2. LIVE NOW: the most important live match as a LiveMatchTile spanning full width. Priority order for choosing the hero: a followed team's live match, then the closest/most active live match, then the earliest live match. If several matches are live, show the other live matches as a horizontally scrollable row of MiniLiveTiles directly beneath the hero; tapping one promotes it into the hero (animated, ~250ms). Show the live count (e.g. "3 live").
3. TODAY: horizontal FixtureRail of today's matches (time, crests, abbreviations, competition), showing upcoming first.
4. BENTO GRID (2 columns, 12dp gutters): StandingsTile (top 3, from the followed or main competition), NextKickoffTile (next followed fixture with countdown), PlayerStatTile (top scorer, only if data exists), AiInsightTile (derived from existing insight data). Tiles use ONE neutral surface style, not different pastel colors per tile. Hide any tile whose data is unavailable; the grid must reflow gracefully with no empty holes.
5. COMPETITIONS: FilterChipRow (All, Premier Division, Continental Cup, ...). Selecting a chip filters the matches shown on Home and the Matches tab, using the existing filter logic.
6. MATCHES: SectionHeader "Matches" + "See all", SegmentedControl (Live / Upcoming / Finished) and MatchRows. This preserves everything in the old home screen.
7. NEWS: SectionHeader "Latest" with 1 lead NewsCard + 2 compact NewsCards, visually lighter than the live section.
The bottom nav must never cover content.

### 5.2 Match Detail
Collapsing header (crests, ScoreText, minute/LiveBadge, venue, competition) that condenses on scroll. Tabs: Overview, Timeline, Stats, Lineups, Table (show only tabs backed by existing data). Overview uses bento tiles (key events, stats bars, form, AI insight). Timeline is a vertical event list with minute markers and icons for goals, cards and subs. Stats uses paired horizontal bars (home vs away). Live data updates in place without layout jumps.

### 5.3 Matches tab
Date strip (previous / today / next days), competition FilterChipRow, SegmentedControl, matches grouped by league with collapsible headers (crest + name + count). Live rows show a thin progress bar. Favorite star toggles. Pull-to-refresh.

### 5.4 Leagues tab
Competition picker (chips), sub-toggle Table / Fixtures / Top scorers. Standings table with sticky header, form dots, movement indicators, zone color markers for qualification/relegation, tabular numerals. Row tap opens the team page.

### 5.5 News tab
Filter chips (e.g. For you, Transfers, Match reports), lead story + compact list, consistent kicker/headline/meta hierarchy, pagination or infinite scroll as exists today. Article screen restyled with the same typography, readable line length, and a share action.

### 5.6 Favorites tab
Sections for Teams, Players, Competitions with follow toggles, a "My matches today" block at the top reusing MatchRow, and a designed empty state with suggested teams if the app supports it.

### 5.7 Team / Player / Competition pages
Header with crest or photo, name, key facts, follow button. Tabs only for data that exists (Overview, Fixtures, Squad, Stats, Table). Reuse bento tiles and rows.

### 5.8 Search
Search field with cancel, recent searches, trending items if supported, instant results grouped by Teams, Players, Competitions, Matches using the standard row components.

### 5.9 Notifications and Settings
Notification center grouped by Today / Earlier with icons per type. Settings screen restyled: toggles, notification levels, quiet hours, theme selection (System / Light / Dark), and any other existing settings, all using shared list-item components.

### 5.10 Onboarding / auth / any other screens
Find and restyle every remaining screen, dialog and bottom sheet discovered in the audit so nothing looks like the old design.

## 6. MOTION AND INTERACTION
- Subtle, purposeful motion only: tile press states, hero swap transition (~250ms), score-change flash (brief highlight on the changed digit, ~600ms), LIVE dot pulse (slow), collapsing headers, skeleton shimmer.
- Respect the system "reduce motion" setting by disabling pulses and non-essential animation.
- Predictable gestures: swipe between hero live matches, pull to refresh, back navigation preserved, scroll position and selected filters preserved across tab switches.

## 7. ACCESSIBILITY AND QUALITY
- WCAG AA contrast in both themes. Never rely on color alone: LIVE also has text, standings movement also has arrows, form dots also have letters for screen readers.
- Content descriptions / semantics for every icon button, crest, tile and row (e.g. "Northgate 2, Riverside 1, 74th minute, live").
- Support font scaling up to 200% without clipping; support small (360dp) and large screens, landscape without crashes, and RTL layouts.
- One-handed usability: primary actions reachable in the lower half where practical, 48dp targets.
- Performance: use lazy lists, avoid unnecessary recomposition/rebuilds, image caching with correct sizing, no layout jank during live updates, skeletons instead of spinners.
- Handle long team names (ellipsize at 1 to 2 lines), missing crests, missing images, null stats, and 0-0 / extra-time / penalties / postponed / abandoned statuses without breaking layout.

## 8. WHAT TO AVOID
- Different pastel colors for each bento tile; large gradients; glassmorphism; heavy shadows; giant rounded rectangles for every section; oversized decorative icons; lime-green LIVE badges (LIVE is red); multiple accent colors; huge empty spaces; dashboard clutter.
- Copying any existing football app's layout or branding.
- Hard-coded colors, sizes, strings or fonts outside the token layer.
- Adding new third-party dependencies unless clearly necessary (explain if you do).

## 9. EXECUTION PLAN AND COMMITS
1. Audit report (Phase 0).
2. Tokens + theme (light/dark) + typography + shapes (Phase 1a).
3. Core components with previews (Phase 1b).
4. Home screen (Phase 2.1).
5. Match Detail, Matches tab.
6. Leagues, News, Favorites.
7. Team/Player/Competition, Search, Notifications, Settings, remaining screens.
8. Polish: states (loading/empty/error/offline), motion, accessibility, dark mode pass, cleanup of dead styles.
After each step, build the app, run existing tests and linters, and fix regressions before moving on.

## 10. TESTING AND VERIFICATION
- Run the existing test suite and add UI tests/previews for new components where the project already has that practice.
- Verify with sample/mocked data for: 0 live matches, 1 live, 3+ live, long names, missing data, offline, error, empty favorites.
- Check light/dark, font scale 100% and 200%, 360dp and larger widths.
- Compare the final Home screen against Image A. If it looks like "the same app with new colors," revisit the composition and hierarchy.

## 11. FINAL DELIVERABLE (report back with)
1. Summary of what changed, by screen, with file paths.
2. The token/theme file locations and how to use them.
3. List of new/updated components.
4. "Needs backend support": design elements that were omitted or stubbed because data does not exist.
5. Any deviations from this brief and why.
6. Known issues and recommended next steps.

Definition of done: every screen uses the new design system, the five-tab navigation is intact, all previous functionality still works, the build and tests pass, light and dark themes both look correct, and the LIVE match is clearly the strongest element on the Home screen.

Begin with Phase 0 (audit). Do not write UI code until you have presented the audit summary.
[file: Design_mobile_football_home_screen_2a132223.webp]