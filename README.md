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
./gradlew :app:testDebugUnitTest    # 87 tests (+5 opt-in screenshot renders)
./gradlew :app:lintDebug
```
By default the app runs on built-in mock football data (fictional clubs, dated relative to today, so there are always live games). News is always real (see below). To use a real gateway: `./gradlew :app:assembleDebug -PpitchsideApiBaseUrl=https://your-gateway/` (contracts in `docs/API_CONTRACTS.md`).

## News source (90min RSS)
The News tab and Home's "Latest" read the public 90min RSS feeds, one per edition:

| Edition | Feed |
|---|---|
| France | https://www.90min.com/fr/feed |
| Italy | https://www.90min.com/it/feed |
| Spain | https://www.90min.com/es/feed |
| Brazil | https://www.90min.com/pt-BR/feed |

`data/news/NinetyMinNews.kt` fetches them with OkHttp, parses with Android's `XmlPullParser` (headline, description, author, date, image + credit, link) and caches each edition for 10 minutes. "All" merges the editions newest first. The article screen shows what the feed publishes and opens the full story on 90min ("Read on 90min"); article bodies are not scraped. Images load with Coil, sized for the screen.

Note: as of November 2026 these editions are no longer updated (newest stories: FR 13 Jul 2026, IT 24 May 2026, BR 11 May 2026, ES 11 Sep 2025). The app shows the real dates and says when a feed is older than a week. The source is an interface (`NewsFeedSource`), so a fresher feed can be plugged in without touching the UI.

## Design
UI follows the "Matchday Hub" redesign: tokens in `ui/theme/Theme.kt`, rules in `DESIGN.md`, report in `docs/REDESIGN_REPORT.md`.

## Architecture
```
Compose screen ── observes ──> ViewModel (StateFlow<UiState<T>>)
                                   │ calls
                                   ▼
                     FootballRepository (Result<T>, cache)
                                   │
                    FootballApi ── HttpFootballApi (OkHttp)  or  MockFootballApi
                    NewsFeedSource ── NinetyMinNewsSource (90min RSS)  or  MockNewsFeedSource
Preferences: DataStore (UserPrefs + PrefsReducer)    Auth: AuthRepository (Keystore-encrypted session)
```
Package `com.innovativeinside.pitchside`: `core/` (UiState, AppError, validators), `data/` (model, remote, news, mock, repository, prefs, auth), `ui/` (vm, screens, components, theme, preview).

See `docs/FINAL_REPORT.md` for what's done, what's blocked on keys, and what to review next.
