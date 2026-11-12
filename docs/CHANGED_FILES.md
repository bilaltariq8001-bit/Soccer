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
