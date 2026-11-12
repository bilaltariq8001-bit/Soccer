# Pitchside

Football live scores with a short Match Brief on every game, Spoiler Shield and smart alerts. Android (Kotlin, Jetpack Compose, Material 3) plus a Firebase backend.

| Folder | What |
|---|---|
| `app/` | Android app (Gradle project) |
| `backend/` | Firebase Cloud Functions (API-Football sync, live polling, alerts) |
| `docs/` | Rebuild reports: FINAL_REPORT, TEST_REPORT, API_CONTRACTS, DESIGN_SYSTEM, feature map and more |
| `screenshots/` | Reference app screenshots (product research only, not shipped) |

## Run the app
```bash
cd app
./gradlew :app:assembleDebug        # or open app/ in Android Studio and press Run
./gradlew :app:testDebugUnitTest    # 57 tests
./gradlew :app:lintDebug
```
By default the app runs on built-in mock data (fictional clubs, dated relative to today, so there are always live games). To use a real gateway: `./gradlew :app:assembleDebug -PpitchsideApiBaseUrl=https://your-gateway/` (contracts in `docs/API_CONTRACTS.md`).

## Architecture
```
Compose screen ── observes ──> ViewModel (StateFlow<UiState<T>>)
                                   │ calls
                                   ▼
                     FootballRepository (Result<T>, cache)
                                   │
                    FootballApi ── HttpFootballApi (OkHttp)  or  MockFootballApi
Preferences: DataStore (UserPrefs + PrefsReducer)    Auth: AuthRepository (Keystore-encrypted session)
```
Package `com.innovativeinside.pitchside`: `core/` (UiState, AppError, validators), `data/` (model, remote, mock, repository, prefs, auth), `ui/` (vm, screens, components, theme, preview).

See `docs/FINAL_REPORT.md` for what's done, what's blocked on keys, and what to review next.
