# Changed files

All paths under `app/app/src/` unless noted. Package root `com/innovativeinside/pitchside/` is shortened to `…/`.

## Build
| File | Why |
|---|---|
| `app/gradle/libs.versions.toml`, `app/build.gradle.kts`, `app/app/build.gradle.kts` | kotlinx.serialization, OkHttp, DataStore, SplashScreen; test stack (coroutines-test, Turbine, MockWebServer, Robolectric, Compose UI test); BuildConfig `API_BASE_URL`; lint config; Robolectric conscrypt switch |
| `app/app/proguard-rules.pro` | Keep rules for serialization and OkHttp in release |
| `main/AndroidManifest.xml` | `PitchsideApp`, INTERNET, splash theme, `pitchside://` deep links, data-extraction rules |
| `main/res/values/strings.xml`, `themes.xml`, `res/xml/data_extraction_rules.xml`, `mipmap-anydpi` | Shared strings, launch theme, backup rules, lint fix |

## Core and data (new)
| File | Why |
|---|---|
| `…/core/UiState.kt`, `AppError.kt`, `DispatcherProvider.kt`, `Clock.kt`, `Validators.kt` | Typed UI state, typed errors with HTTP/exception mapping, injectable dispatchers and clock, form rules |
| `…/data/model/Models.kt`, `Languages.kt` | Domain models (replaces old `data/Models.kt`) |
| `…/data/remote/dto/Dtos.kt` | Wire DTOs mirroring the backend documents |
| `…/data/remote/FootballApi.kt`, `HttpFootballApi.kt`, `Mappers.kt` | API interface, OkHttp client (auth header, timeouts, retry, error mapping), DTO→domain mappers |
| `…/data/mock/MockFootballApi.kt` | Mock gateway in DTO form, dated relative to today, failure switches (replaces `FakeRepository`) |
| `…/data/repository/FootballRepository.kt` | Single repository: Result-returning, TTL cache, parallel detail loads |
| `…/data/prefs/UserPrefs.kt`, `PrefsRepository.kt` | Preferences model, pure reducer, DataStore and in-memory stores |
| `…/data/auth/AuthRepository.kt`, `KeystoreCipher.kt` | Local + in-memory auth, validation, salted hashes, Keystore AES-GCM session token |
| `…/AppContainer.kt`, `PitchsideApp.kt` | Manual DI container; swappable in tests |
| Deleted: `AppViewModel.kt` (root), `data/FakeRepository.kt`, `data/Repository.kt`, `data/Models.kt` | Replaced by the above |

## UI
| File | Why |
|---|---|
| `…/MainActivity.kt` | Splash until prefs load; theme from prefs; locals |
| `…/ui/AppRoot.kt` | Full nav graph (19 routes, typed args, deep links), tab back stacks, toasts from events, offline banner, exit dialog, live badge |
| `…/ui/Locals.kt` | `LocalApp`, `LocalPrefs` |
| `…/ui/vm/*` | `AppViewModel` (session intents), `LoadViewModel` base, per-screen ViewModels, News/Search/Ask/Auth ViewModels, factory |
| `…/ui/components/Components.kt` | Live badge, loading button, string resources, accessibility fixes |
| `…/ui/components/States.kt`, `Fields.kt` | `StateContent`, `ErrorState`, `EmptyState`, skeletons, `PcTextField`, `PcTopBar` |
| `…/ui/screens/HomeScreen.kt`, `MatchesScreen.kt`, `MatchDetailScreen.kt`, `BrowseScreens.kt`, `NewsScreens.kt`, `FavoritesSearch.kt`, `AccountScreens.kt`, `ScreenBase.kt`, `Onboarding.kt` | Every screen rebuilt on ViewModels with loading/empty/error/retry and previews (replaces `MatchScreens.kt`, `ContentScreens.kt`, `UserScreens.kt`) |
| `…/ui/preview/PreviewSupport.kt` | Preview host, in-memory container, preview-only sample data |

## Tests (new)
`test/…/data/*Test.kt`, `test/…/ui/ViewModelTest.kt`, `test/…/uitest/ComposeUiTest.kt`, `test/…/testutil/TestUtil.kt`, `test/resources/robolectric.properties`, `androidTest/…/StateContentInstrumentedTest.kt`.

## Docs (new, `docs/`)
TASK_LIST, COMPETITOR_FEATURE_MAP, FEATURE_GAP_ANALYSIS, FIX_PLAN, DESIGN_SYSTEM, API_CONTRACTS, PROGRESS_LOG, CHECKPOINTS, QA_CHECKLIST, TEST_REPORT, CHANGED_FILES, FINAL_REPORT; root `README.md` rewritten.

## Redesign v0.3.0 (2026-11-13)
| File | Why |
|---|---|
| `app/gradle/libs.versions.toml`, `app/app/build.gradle.kts` | Coil 2.7.0 (`coil-compose`), version 0.3.0 (code 3) |
| `DESIGN.md` | Rewritten for the v3 token system |
| `main/res/font/*` | Manrope + Barlow Condensed (already on the branch, same files as `main`) |
| `main/res/values/colors.xml`, `values-night/colors.xml`, `values/themes.xml`, `drawable/ic_pitchside_splash_mark.xml`, `drawable/ic_launcher_foreground.xml`, `values/ic_launcher_background.xml`, `values/strings.xml` | Splash ported from `main` and recoloured; launch theme; launcher colours; splash tagline |
| `…/MainActivity.kt`, `…/ui/screens/AnimatedSplashScreen.kt` | Animated intro over the composed app (from `main`), kept prefs-gated system splash |
| `…/ui/theme/Theme.kt` | New token layer: colours (light/dark), type roles with tabular numerals, spacing, shapes |
| `…/data/model/Models.kt` | `NewsItem` image/caption/edition/author/language, `NewsEdition` |
| `…/data/news/NinetyMinNews.kt` (new) | `NewsFeedSource`, 90min RSS source + parser, text and image helpers |
| `…/data/mock/MockNewsFeedSource.kt` (new) | Offline news for previews and tests |
| `…/data/repository/FootballRepository.kt`, `…/AppContainer.kt` | `headlines()` with cache/merge/paging, feed-backed `article()`, wiring |
| `…/data/prefs/UserPrefs.kt` | Recent searches + reducer functions |
| `…/ui/vm/*` | Hero ranking/selection, next kickoff, filters and counts, competition state, hero stats, News by edition, Leagues selected competition, Search matches, Favorites today |
| `…/ui/components/Components.kt`, `States.kt`, `Overlays.kt`, `Fields.kt` | Restyled foundation components, segmented control with counts, shimmer, offline banner, reduced motion |
| `…/ui/components/Navigation.kt`, `MatchComponents.kt`, `BentoTiles.kt`, `NewsComponents.kt` (new) | Top bar, bottom nav, crest, live badge, live/mini tiles, match row, rail item, bento tiles, news cards |
| `…/ui/screens/*` | Every screen redesigned (see `docs/REDESIGN_REPORT.md`); new `NotificationsScreen.kt` |
| `…/ui/AppRoot.kt` | Notifications route, new bottom bar, offline banner |
| `…/ui/preview/PreviewSupport.kt` | 90min-style preview stories, scorers, upcoming |
| `test/…/data/NinetyMinNewsTest.kt`, `NewsRepositoryTest.kt`, `test/…/ui/HomeLogicTest.kt`, `test/…/uitest/RedesignUiTest.kt`, `ScreenshotCapture.kt`, `test/resources/fixtures/90min_fr_sample.xml` (new) | Parser, source, repository, hero logic, Compose tests, opt-in screenshots, real feed fixture |
| `test/…/ui/ViewModelTest.kt`, `test/…/uitest/ComposeUiTest.kt` | Updated for the new UI (nav test tags, section names, Leagues inline table) |
| `docs/REDESIGN_REPORT.md` (new), `docs/REDESIGN_AUDIT.md`, `docs/TEST_REPORT.md`, `README.md` | Reports |
