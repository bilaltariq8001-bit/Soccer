# Final report: Pitchside Android rebuild

Branch `hark/rebuild-app`, opened as a draft pull request against `main`. Nothing on `main` was changed.

## Summary
The uploaded app looked finished but was a static mock: every screen read a synchronous in-memory object, most matches showed the same events and stats, every player link opened the same player, there were no error states, several buttons did nothing, and "auth" was a toast. The rebuild keeps the original Pitchside brand and design tokens and replaces everything underneath with a real architecture: typed API → repository → per-screen ViewModels → Compose screens with loading, empty, error and retry everywhere. The app builds, 57 automated tests pass (including full onboarding and tab navigation on Robolectric), and lint has 0 errors.

## Screens (all on ViewModels, all with loading / empty / error / retry, all with previews or UI tests)
Welcome, Language, Follow teams, Watch style, Alerts, Persona, Building your home (real work), Home, Matches (date strip, calendar, Live/My teams), Match (Brief, Timeline, Stats, Lineups, Table), Pitch view, Leagues (with top players), League (Table Short/Full/Form, Matches, Top scorers, Teams), Team (Matches, Overview, Squad, Table), Player (Profile, Matches, Stats Season/Per 90, Career), News (paginated), Article (share, source), Favorites (teams with alert level + Spoiler Shield, leagues, players), Search, Ask, Paywall, Settings, Sign in / Create account / Reset password, Account (protected).

## Components
`StateContent`, `ErrorState`, `EmptyState`, `StateCard`, `ListSkeleton`, `DetailSkeleton`, `PcTextField`, `PcTopBar`, `LiveCountBadge`, loading `PrimaryButton`, plus the existing cards, chips, tabs, dialogs and sheets (now accessibility-labelled). See DESIGN_SYSTEM.md.

## ViewModels
`AppViewModel` (prefs, session, follow/shield/settings intents, live count), `LoadViewModel` base, `HomeViewModel`, `MatchesViewModel`, `MatchDetailViewModel`, `LeaguesViewModel`, `LeagueDetailViewModel`, `TeamViewModel`, `PlayerViewModel`, `ArticleViewModel`, `FavoritesViewModel`, `OnboardingTeamsViewModel`, `BuildHomeViewModel`, `NewsViewModel`, `SearchViewModel`, `AskViewModel`, `AuthViewModel`.

## Data
- `FootballApi` with two implementations: `HttpFootballApi` (OkHttp + kotlinx.serialization; bearer token, timeouts, GET retry with backoff, typed errors, 401 ends the session) and `MockFootballApi` (same DTOs, fictional clubs, dated relative to today, offline/503 switches).
- `DefaultFootballRepository`: Result-returning, in-memory TTL cache (20 s live / 60 s other), parallel loads for detail pages.
- Contracts in API_CONTRACTS.md, mirroring the Cloud Functions' Firestore documents.
- Switch to the real gateway: build with `-PpitchsideApiBaseUrl=https://your-gateway/`.

## Auth status
Working locally: sign up, sign in, reset password, sign out, session expiry, protected Account route. Passwords stored as salted SHA-256 (local only), session token encrypted with an Android Keystore AES-GCM key, backups excluded. The app stays guest-first, as the plan intended. **Not yet Firebase Auth**: that needs your `google-services.json`. Swap `LocalAuthRepository` for a Firebase implementation of the same `AuthRepository` interface.

## Firebase / Room / DataStore
- DataStore: in use for all preferences (one serialized document, atomic updates).
- Firebase: backend code exists in `backend/`; the Android side is not wired because there is no Firebase project config in the repo. Blocked on `google-services.json`.
- Room: not added. The in-memory cache covers tab switches; Room offline cache is the next data task.

## Tests
57 unit, API, repository, ViewModel and Robolectric Compose UI tests: 57 passed, 0 failed. 1 instrumented test written, not run (no device). Details in TEST_REPORT.md.

## Remaining risks
1. Backend gateway isn't deployed; the HTTP client is tested against MockWebServer, not the real service.
2. Firebase (Auth, FCM alerts, App Check, AI Logic) and Play Billing are not connected: they need your keys and Play Console products. Paywall says so honestly; debug builds have a "Pro preview" switch.
3. UI text is English only; the language setting is stored and meant for AI output.
4. Ask answers are canned on mock data until Firebase AI Logic is connected.
5. Highlights, TV schedule and referees from the reference app are intentionally not built (no licensed source).
6. Verified on Robolectric, not on a physical phone. Do one manual pass on a device before release.

## Build / run / test
```bash
cd app
./gradlew :app:assembleDebug                 # APK at app/app/build/outputs/apk/debug/
./gradlew :app:installDebug                  # with a device attached
./gradlew :app:testDebugUnitTest
./gradlew :app:lintDebug
./gradlew :app:connectedDebugAndroidTest
./gradlew :app:assembleDebug -PpitchsideApiBaseUrl=https://your-gateway/   # real API
```

## What a human should review next
1. Run the app on a phone; try Settings › Developer › Simulate offline and check every tab recovers with Try again.
2. Decide: Firebase Auth vs custom auth endpoints; Pro prices; free follow limit (currently 5).
3. Add `google-services.json`, then wire Firebase Auth, FCM and App Check.
4. Deploy the gateway (or a Firestore-backed `FootballApi`) and point `pitchsideApiBaseUrl` at it.
