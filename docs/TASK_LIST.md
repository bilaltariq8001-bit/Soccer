# Task list

Created at the start of the rebuild (2026-11-12) and ticked as work finished.

## P0: app-breaking
- [x] No async data layer: UI called a synchronous `FakeRepository` object directly. Replaced with API → repository → ViewModel.
- [x] No error states anywhere; a failed load had nowhere to go. Added `UiState` + `StateContent`/`ErrorState` with retry.
- [x] Preferences saved by a `snapshotFlow` loop into SharedPreferences (racy, lossy). Moved to DataStore + pure reducer.
- [x] Every match showed the same hardcoded events, stats and table. Detail now loads per match id.
- [x] Every player link opened "p9". Player pages load by id.
- [x] Sandbox build toolchain (JDK 17 + SDK 35 on aarch64) set up so the app is actually compiled and tested.

## P1: missing or broken features
- [x] Matches by date (yesterday / today / tomorrow / calendar), Live and My teams filters.
- [x] Live count badge and live polling.
- [x] Leagues list, League page (table, matches, scorers, teams).
- [x] Team page (matches, overview, squad, table).
- [x] Search across teams, leagues, players (debounced, cancellable).
- [x] News with pagination and an Article screen.
- [x] Auth: sign in, create account, reset password, sign out, protected Account route, encrypted session.
- [x] Real onboarding "building your home" step.
- [x] Typed HTTP client with auth header, timeouts, retry, error mapping (ready for the backend gateway).

## P2: UX polish
- [x] Pull-to-refresh with "couldn't refresh" banner that keeps old data.
- [x] Skeleton loaders on first load, never a blank page.
- [x] Explained empty states (pre-kickoff stats, lineups, knockout tables, far-off dates).
- [x] Exit confirmation, splash until prefs load, deep links.
- [x] Accessibility pass (merged row descriptions, live regions, decorative crests hidden, 48dp targets).
- [x] Developer switches to simulate offline / 503 on mock data.

## P3: optional / later
- [ ] Firebase Auth, Firestore sync, FCM alerts, App Check (needs `google-services.json`).
- [ ] Play Billing for Pro (needs Play Console products).
- [ ] Firebase AI Logic for Match Brief and Ask (needs Firebase project).
- [ ] Translated UI strings (only English ships; language choice drives AI output).
- [ ] Room offline cache (in-memory TTL cache today).
- [ ] Highlights, TV schedule, referees (need a licensed data source).
- [ ] Remove the duplicate `screenshots.zip` (waiting for your OK).
