# Plan: Our football live-scores app (working name: TBD)

Based on `competitor-analysis.md` (LiveFootball teardown, 51 screenshots). All AI features use **Firebase AI Logic**.

## 1. Goal and positioning

Match the competitor's content depth (league / team / player pages, rich match tabs) while beating it on the things users will notice first:

| Competitor weakness | Our answer |
|---|---|
| Full-screen ad on almost every tap | Banner + native ads only; at most 1 interstitial per ~10 min at natural breaks; none on navigation or back |
| Slow or empty screens (Live, Stats, Commentary) | Cached data, skeleton loaders, graceful "not available" states |
| Watch depends on a YouTube embed that breaks | No embedded-video dependency in v1; link out to official highlights |
| Plain data, no insight | **AI layer**: recaps, previews, chat assistant, translation |
| Single costly lifetime Pro price | Free tier + monthly / yearly Pro + optional lifetime |

## 2. Scope

**MVP (v1)**
- Fixtures home (Yesterday / Today / Tomorrow, calendar sheet, followed leagues pinned, Top Leagues row)
- Live tab with live count badge
- Match detail: Preview, Table, Summary, Commentary, Lineup, Stats
- League page: Matches, Standings, Teams, Top Scorers
- Team page: Matches, Overview, Squad, Standings
- Player page: Profile, Matches, Stats, Career
- Favorites (leagues / teams / players), search, per-match and per-team alerts
- **Spoiler Shield:** hide scores/results for chosen teams or leagues until revealed; alerts held until "I'm caught up" (client logic, no AI)
- **Smart Alerts:** per-team alert level (every goal / key moments / final only), quiet hours, via FCM topics (see `ux-phase2-3.md`)
- **Match Brief (hero AI feature):** before / live / after explanation card on every match, facts computed by the backend and phrased by Firebase AI Logic
- Settings: language, theme (system/light/dark), notifications, privacy, rate, share
- Onboarding (language, optional follow picks, notification prompt after value is shown)
- AI features (section 5), ads, Pro subscription

**Later (v2):** News and transfers feed, highlights, TV schedule, widgets, Wear OS, referees tab, shorts (only if there is a licensed content source).

## 3. Tech stack (recommended defaults)

- **Android native:** Kotlin, Jetpack Compose, Material 3, Navigation Compose, Hilt, Coroutines/Flow, Room (offline cache), Coil, WorkManager.
- **Firebase:** Auth (anonymous first, link account later), Firestore (user prefs, AI quotas), Cloud Messaging, Remote Config, Analytics, Crashlytics, App Check (Play Integrity), **Firebase AI Logic**.
- **Backend (thin):** Cloud Functions or Cloud Run that proxies the football data API so the API key never ships in the app, caches responses in Firestore/Redis, and fans out live events to FCM topics.
- **Billing:** Google Play Billing (subscriptions).
- **Ads:** AdMob (+ mediation later). Compose-friendly native ad templates.

Package layout: `core` (network, db, design system), `feature-fixtures`, `feature-match`, `feature-league`, `feature-team`, `feature-player`, `feature-favorites`, `feature-search`, `feature-ai`, `feature-settings`, `billing`, `ads`.

## 4. Data layer

- **Provider (decided): API-Football** (api-sports.io, v3, base URL `https://v3.football.api-sports.io`). It covers what the competitor shows: fixtures and live scores, events, lineups, team and player statistics, standings, top scorers, players, coaches, venues, transfers, injuries, predictions and odds. Auth is an API key header; confirm the exact header name (`x-apisports-key` vs the RapidAPI `x-rapidapi-key`) in the dashboard.
- **Plans (verify on the live pricing page):** Free 100 requests/day; Pro 7,500/day; Ultra 75,000/day; Mega 150,000/day; higher tiers up to 1.5M/day. Third-party sources disagree on prices (vendor guide says about $19 / $29 / $39 per month), so confirm before budgeting. Use Free for development and Pro or Ultra for launch.
- **Request budget is the main design constraint.** The key must live only on our backend, and the app never calls API-Football directly. One backend poll of `fixtures?live=all` serves every user, so cost does not grow with users.
  - Live scores: poll `fixtures?live=all` every 30-60 s only while matches are live (a typical day is roughly 1,500-3,000 calls). Faster polling (the vendor's older docs mention about 15 s) costs far more and gains little for a scores app.
  - Per-match detail (events, lineups, statistics): fetch only for matches users are actually viewing or that are in followed leagues, cache for 30-60 s, and share across users.
  - Static or slow data (leagues, teams, squads, standings, top scorers, players, venues): fetch once and refresh on a schedule (standings after each match, squads daily, profiles weekly) into Firestore.
  - Lineups appear roughly 20-60 minutes before kickoff; poll only in that window.
- **Plan tiers by phase:** dev on Free with a recorded-response mock; beta on Pro; scale to Ultra when live polling plus detail fetches approach the daily cap. Add a request counter and alert at 80% of the daily limit.
- **Licensing:** API-Football supplies logo and photo URLs, but confirm their terms allow display in a published app and whether images must be cached on our CDN or can be hotlinked. Check this before launch.
- Alternatives kept in reserve if cost or coverage becomes a problem: Sportmonks, football-data.org (limited free tier). The normalization layer below keeps the provider swappable.
- **Polling vs push:** backend polls the provider on a schedule (faster during live windows), writes normalized docs to Firestore, app listens to a small "live scores" document set; FCM topics (`match_{id}`, `team_{id}`, `league_{id}`) carry goal, red card, kickoff and full-time alerts.
- **Offline:** Room cache with stale-while-revalidate; every screen renders cached data first.
- **Normalization:** one internal model (Match, Team, League, Player, Event, Stat) so the provider can be swapped.

## 5. AI features with Firebase AI Logic

Use the Firebase AI Logic Android SDK (`com.google.firebase:firebase-ai`) calling Gemini, with no model API key in the app. Use the Gemini Developer API backend to start (free tier, easy), and keep Vertex AI as an option for scale. Do not hard-code model names: read them from **Remote Config** so models can be changed without an app release, and confirm the current recommended Gemini Flash model at build time.

| Feature | How it works | Where |
|---|---|---|
| **AI match recap** | Send structured events + score + key stats as JSON, get a 3-4 sentence recap in the user's language. Cache result per match in Firestore so one call serves everyone. | Match > Summary |
| **AI match preview** | Form, H2H, standings and injuries in, a short preview and "what to watch" out. Replaces the competitor's raw win-probability card with an explanation. | Match > Preview |
| **Ask Football (chat assistant)** | Chat UI using **function calling**: model calls our tools (`getFixtures`, `getStandings`, `getPlayerStats`, `getTeamForm`) that hit our backend, so answers use real data, not guesses. | New tab / FAB |
| **News and commentary translation / summary** | Translate and condense articles and live commentary into the app language. | News, Commentary |
| **Personalized digest** | Daily "your teams" summary written from followed teams, shown as a card and optional notification text. | Home, Notifications |
| **Smart follow suggestions** | Suggest teams/leagues from a few picks during onboarding. | Onboarding |
| **Voice Q&A (stretch)** | Gemini Live API for spoken questions about a match. | Later |

Guardrails and operations:
- **App Check (Play Integrity) enforced** for Firebase AI Logic so only our real app can call the models.
- **Per-user rate limit** in the Firebase AI Logic settings plus our own daily quota in Firestore: Free = a few AI actions/day, Pro = higher; optional "watch a rewarded ad for more".
- **System instructions** that restrict the assistant to football, forbid inventing scores or stats, and require tool results for any numbers.
- **Cost control:** use the cheapest Flash-class model, cache recaps/previews per match, cap input size, stream responses for perceived speed.
- **Fallback:** if AI fails or is over quota, show the plain data screen (AI is always an enhancement, never blocking).
- **Safety:** use safety settings, label AI text as "AI-generated", and keep a user report button.
- Optional later: hybrid on-device inference for short summaries to cut cost.

## 6. Monetization

- Free: banner on list screens, native ad between league cards (max 1 per screen), rewarded video for extra AI quota or to unlock Pro features for a day. No interstitials on tab or back navigation.
- Pro: ad-free, higher AI quota, unlimited follows, extra widgets. Monthly, yearly (default highlighted) and one-time lifetime.
- Paywall: one well-designed screen (benefits, trial, price, restore) reachable from More, a Pro crown, and AI-quota-hit sheet. Avoid dark patterns.
- Follow limit for free users is optional; start generous, tune with Remote Config.

## 7. Design direction

- Reuse the patterns that worked: 5-tab bottom nav with a prominent Live button, collapsible league cards, bottom-sheet pickers, per-item bell toggles, skeleton loaders, illustrated empty states, exit confirmation.
- Our own brand, color and icon (not the competitor's green/red clone). Material 3 with light, dark and dynamic color.
- Accessibility: large text, contrast, TalkBack labels for scores.

## 8. Phases

1. **Foundation (wk 1-2):** project setup, design system, Firebase project, App Check, backend proxy + provider integration, Room cache.
2. **Core screens (wk 3-6):** Fixtures, Live, Match detail, League, Team, Player, Search, Favorites.
3. **Alerts + settings (wk 7):** FCM topics, notification settings, language, theme, onboarding.
4. **AI (wk 8-9):** Firebase AI Logic integration, recap, preview, chat with function calling, quotas, caching.
5. **Monetization (wk 10):** AdMob, Play Billing, paywall, Remote Config tuning.
6. **Hardening (wk 11-12):** performance, offline, crash fixes, Play Store listing, closed beta.

(Timeline assumes a small team; adjust once team size is known.)

## 9. Risks

- Data licensing and API-Football request caps (live polling and per-match detail are the main cost drivers); mitigated by backend caching, shared polling and request alerts.
- Single-provider dependency: keep the internal normalized model so a swap is possible.
- Crest and player photo rights.
- AI hallucination of football facts, mitigated by tool calling and system instructions.
- AI cost spikes, mitigated by caching, quotas, App Check.
- Notification fatigue; give granular controls from day one.
- Pakistan pricing: set regional Pro prices (competitor lifetime is Rs 8,300).

## 10. Verification

- Unit tests for mappers, quota logic and AI prompt builders (with a fake AI client).
- Compose UI tests for key flows (follow a team, open match, hit AI quota).
- Staging Firebase project; test live data with a recorded provider feed.
- Manual pass against the 51 reference screenshots for feature parity.
- Check ads never fire on navigation, and AI is blocked without a valid App Check token.
- Play pre-launch report and a closed beta before release.

## 11. Open decisions

1. ~~Football data provider~~ **Decided: API-Football.** Still open: exact plan and monthly budget (confirm current prices).
2. Platform: Android native (assumed) vs also iOS later (Kotlin Multiplatform option).
3. App name, brand, and target regions/languages for launch.
4. Free-tier limits (follows, AI actions per day) and Pro prices.
5. Whether v1 includes News/Highlights or waits for v2.
