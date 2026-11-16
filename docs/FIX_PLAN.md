# Fix plan

1. **Toolchain.** Install JDK 17, Android SDK 35 and an aarch64 `aapt2` in the sandbox so `assembleDebug`, unit tests and lint really run.
2. **Data layer.** DTOs mirroring `backend/functions/src/types.ts` → `FootballApi` interface → `HttpFootballApi` (OkHttp + kotlinx.serialization) and `MockFootballApi` (same DTOs, generated relative to today) → `Mappers` → `DefaultFootballRepository` returning `Result<T>` with `AppError`.
3. **State.** `UiState<T>` + `LoadViewModel` base (single in-flight load, pull-to-refresh, background polling for live data). One ViewModel per screen, created with a container factory, nav args via `SavedStateHandle`.
4. **Preferences.** `UserPrefs` serialized into DataStore; every mutation through `PrefsReducer` (pure, tested). Activity-scoped `AppViewModel` exposes prefs/session and intents.
5. **Auth.** `AuthRepository` with local implementation (salted hashes, Keystore AES-GCM token), `AuthViewModel` with field validation, Account as a protected route.
6. **Screens.** Rebuild Home, Matches, Match, Lineup, Leagues, League, Team, Player, News, Article, Favorites, Search, Ask, Paywall, Settings, Auth, Account on the existing design tokens; fix onboarding.
7. **Tests.** Mappers, errors, validators, reducer, HTTP client (MockWebServer), repository, ViewModels (coroutines-test + Turbine), Robolectric Compose UI and navigation flows.
8. **Docs + PR.** Push to `hark/rebuild-app`, open a draft PR; never touch `main` directly.
