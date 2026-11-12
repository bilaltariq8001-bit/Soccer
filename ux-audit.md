# Phase 0 + 1: Intake and UX audit of the reference app (LiveFootball)

Reference: installed app `com.football.livescores.liveupdates` v1.0.51, walked on a real TECNO BG6 (720x1612). Screens are numbered as in `screenshots/`. We design an **original** UI; we do not copy its name, logo, colours or artwork.

## Phase 0 - Intake (assumptions, change any)

| Item | Assumed |
|---|---|
| App | Football live scores, fixtures, stats, alerts and AI insights (name TBD) |
| Reference | Competitor app on the device (own app: no) |
| Platform | Android (Compose, Material 3) |
| Backend | Firebase + thin proxy to API-Football; AI via Firebase AI Logic |
| Identity | Anonymous Firebase Auth first, optional sign-in later |
| Not allowed in v1 | Interstitials on navigation/back; embedded YouTube dependency; scraped Shorts; fake testimonials; dark-pattern paywalls |
| Audience | Global casual-to-hardcore football fans, multi-language (Urdu/Arabic/Hindi/Spanish/English first) |
| Style | Original premium look (not the competitor's green/red) |

## 1. Screen inventory and current flow

Launch -> Select Language (01) -> Your Teams (03) -> Your Leagues (03/04) -> Your Players (04) -> Get Started -> notification permission over a loading home (05) -> **Home/Fixtures** (06, 07) with 5 tabs:

- Fixtures (06-07), Highlights > Highlights / General / Transfers (08-10), **Live** list (49) > Watch > Pro paywall (50) > YouTube player (51), Shorts (18), Favorites > Leagues / Teams / Players (19).
- Header: hamburger -> **More** page (20-21, 45-48): Pro banner, TV Schedule (42), Players (43), Language (48), Notifications (44), Theme (45-47), Rate, Feedback, Share, Privacy, Terms, Version.
- Drill-downs: Match (11-16: Preview, Table, Summary, Commentary, Lineup, Stats), League (22-26), Team (27-30), Player (31-36), Search (38-40), Calendar sheet (41), Exit dialog (37), Interstitial ads (17 and others not saved).

## 2. Scores (1-10)

| Area | Score | Evidence |
|---|---|---|
| First impression & onboarding | 4 | Three forced list steps with native ads inside the lists; no value shown before asking for choices; cold notification prompt on a loading screen (05) |
| Information architecture | 6 | Clear 5 tabs, but Highlights mixes video + news + transfers; Players list buried in More; Shorts off-topic; "Live" is a separate screen instead of a filter on Fixtures |
| Navigation | 5 | Hamburger opens a full page, not a drawer; back from match returned to the Live list; deep stacks with no breadcrumbs; interstitials on tab change and back |
| Visual hierarchy | 6 | Cards and bold winners read well; heavy flat green header on every screen; top ad strip pushes real content down; small grey text on grey (FT badge, club names) |
| Consistency | 6 | League, team, player and match headers each use a different pattern; tab bars scroll and clip ("Refe..."); mixed illustration styles |
| Feedback & states | 5 | Generic "Loading... may take few seconds"; skeleton only on player page; "No data available" + Retry for Commentary/Stats with no explanation |
| Permissions & trust | 4 | Notification prompt with no rationale; ad creatives that open the Play Store; Pro banner jumps straight to a Google Play sheet; two different Pro flows |
| Accessibility | 5 | Visual review only (not tested with TalkBack): low contrast secondary text, tiny crest icons, 11-12sp labels in tables |
| Error / empty / offline | 5 | Good illustrated empty states (TV, search); offline mode **not tested**; no inline retry guidance |
| Retention & value | 6 | Follow + per-item bell, rich player pages, dark theme. Weak: no widgets seen, no personalised home, no insight layer, ad fatigue |
| **Overall** | **5.2** | Strong content depth, weak experience around it |

## 3. Issues and fixes

| # | Screen | Problem | Severity | Fix |
|---|---|---|---|---|
| 1 | All | Full-screen video/playable ads on most taps; some open Play Store; close buttons delayed | Critical | No interstitials on navigation or back; max 1 per ~10 min at natural breaks; banner/native only |
| 2 | Onboarding 01-05 | 3 forced list steps, ads inside lists, no value first | High | 3-step quiz with a "popular near you" preselect, show a live preview of the user's home as it builds; allow skip everywhere |
| 3 | 05 | Notification permission asked cold over a spinner | High | Explain-first sheet ("Goal alerts for your teams"), ask after first follow; denied-state fallback to settings |
| 4 | 49 | Live list took many seconds; spinner only | High | Cached live scores, skeleton cards, last-updated stamp |
| 5 | 50/51 | Watch depends on YouTube embed that showed "Sign in to confirm you're not a bot" | High | No embed in v1; link out to official highlights |
| 6 | 14/16 | "No data available" for Commentary/Stats with no reason | High | Explain ("Stats start at kickoff"), show what is available, AI-written preview/recap in its place |
| 7 | 06 | Home puts ads and rails above scores; "Following" is mid-page | Medium | Followed teams' matches first; live pinned; Top Leagues as a compact chip row |
| 8 | 20 | Pro entry opens Google Play sheet directly; second paywall elsewhere | Medium | One paywall screen, one price grid, trial, restore |
| 9 | 18 | Shorts shows American football from Pinterest | Medium | Cut from v1 (park to v2 with licensed source) |
| 10 | 22-30 | Tab bars clip; tables have tiny text | Medium | Segmented scrollable tabs with edge fade; 14sp min table text; sticky header |
| 11 | 24/25 | Placeholder avatars for most players | Low | Monogram avatars in team colours |
| 12 | 20 | Hamburger leads to full page | Low | Replace with a profile/settings tab or a top-bar avatar sheet |
| 13 | 08-10 | Highlights tab mixes three content types | Low | Split: Highlights/News as sub-tabs in a Discover tab (v2) |
| 14 | All | Not verified: reviews/complaints on Play Store | - | Add a review pass (web) before Phase 2 sign-off |

## 4. Keep (works well)

- 5-tab bottom nav with prominent Live button and badge count.
- Collapsible league cards, Date/Round toggle, per-item bell toggles.
- Rich team/player data (xG, per-90, career timeline, market value).
- Bottom-sheet calendar and theme picker; illustrated empty states; skeleton loader idea (player page); exit confirmation.
- Dark theme as a real near-black UI.

## 5. Missing screens, dialogs and states (to design)

- Splash, welcome, personalised onboarding + "building your home" screen, persona result.
- Permission rationale and denied flows (notifications; Android 13+ POST_NOTIFICATIONS).
- Match: timeline with events, lineup **pitch view**, H2H, form strip, shot map, AI recap and preview cards.
- AI assistant chat, quota-reached sheet, AI disclaimer and report.
- Notification centre and match-alert settings, widgets (live score, next match).
- Paywall (monthly / yearly / lifetime), restore, success.
- States: offline banner, stale-data stamp, error with retry, empty favourites, no live matches, first-run loading.
- Dialogs: confirm unfollow, destructive reset, rate-us prompt, language change, exit.

## 6. What "premium and better" means for our design (input to Phase 2/4)

1. **Calm, modern, content-first:** neutral surfaces, one confident accent, club colours used sparingly; no permanent heavy header.
2. **Scores are the hero:** large tabular numerals, live minute pulse, goal flash animation, haptics on goals.
3. **Personal home:** My Teams first, live pinned, "Today for you" AI digest.
4. **Insight layer (AI):** recap, preview, "why it matters", chat, translations (Firebase AI Logic).
5. **Fast and honest:** cached data, skeletons, last-updated stamps, no fake spinners.
6. **Respectful monetisation:** light ads, one paywall, no tricks.
7. **Accessible and global:** RTL, large text, contrast 4.5:1, 14sp min, localized numerals.

## 7. Next

Phase 2 (unique value under constraints) and Phase 3 (personalised onboarding) follow after your go-ahead. Pending: a Play Store review pass on the competitor (issue 14) and confirmation of the assumptions in Phase 0.
