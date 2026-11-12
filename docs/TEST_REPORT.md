# Test report

Run on 2026-11-12 in a Linux aarch64 sandbox (JDK 17.0.21, Android SDK 35, Gradle 8.10.2, AGP 8.7.3). No emulator or device was available.

## Results
| Command | Result |
|---|---|
| `./gradlew :app:assembleDebug` | ✅ BUILD SUCCESSFUL, `app-debug.apk` (~19 MB) |
| `./gradlew :app:testDebugUnitTest` | ✅ 57 tests, 0 failures, 0 skipped |
| `./gradlew :app:lintDebug` | ✅ 0 errors, 2 warnings (see below) |
| `./gradlew :app:connectedDebugAndroidTest` | ⏳ not run: no device in the sandbox. 1 instrumented test written |
| ktlint / Detekt / Spotless | Not configured in the project |

## Tests written
| Suite | Tests | Covers |
|---|---|---|
| `MappersTest` | 8 | Team colours/monograms, status mapping, match labels (live minute, HT, FT, postponed, kickoff time), upcoming never shows a score, events (unknown kinds dropped, newest first, stoppage time), lineup grid → pitch, form sanitising, news mojibake fix, positions |
| `AppErrorAndValidatorsTest` | 6 | Exception and HTTP status → typed error, retryability, Result → UiState, email/password/name/confirm rules |
| `PrefsReducerTest` | 6 | Follow/unfollow, 5-team free limit vs Pro, Spoiler Shield hide/reveal/caught up, persona rules, onboarding defaults, daily Ask quota reset |
| `HttpFootballApiTest` (MockWebServer) | 11 | Auth header present/absent, Accept and client headers, query and path encoding, 401 (session callback)/403/404/422/500 mapping, malformed JSON, timeout, network drop, GET retry on 503, no retry on 404, POST body and no retry, pagination cursor |
| `RepositoryTest` | 9 | Today's mix of statuses and ordering, empty far dates, TTL cache + forced refresh + expiry, offline/503/not-found as typed failures, match detail composition (events, stats, lineups, table; pre-kickoff and knockout cases), search min length and cross-type results, news paging, Ask validation, team and player pages |
| `ViewModelTest` (coroutines-test + Turbine) | 11 | Home loading → success and background polling that never replaces data with an error; error → retry; refresh failure keeps data with a banner; Matches date change and empty day; filter/group helpers; not-found not retryable; blank id; Search debounce, stale cancel, empty, error, retry, idle; News pagination and category; Ask quota and retry; Auth validation, sign up, wrong password, sign in, reset |
| `StateContentTest` (Robolectric) | 1 | Loading, empty, error with retry click, non-retryable error, success |
| `AuthScreenTest` (Robolectric) | 2 | Sign-up field errors then success; forgot-password flow |
| `AppNavigationTest` (Robolectric, full MainActivity) | 2 | Home → Matches (Live) → Leagues → League → Top scorers → back → News → Favorites; exit confirmation |
| `OnboardingFlowTest` (Robolectric, full MainActivity) | 1 | New user through every onboarding step to Home |
| `StateContentInstrumentedTest` (androidTest) | 1 | Error + retry on a real device (not run here) |

## Fixes found by tests
- Loading state was set inside the launched coroutine, so for one frame after a tap the old data stayed on screen. Now set synchronously in `LoadViewModel.load` and `NewsViewModel`.
- "Load more" flag was set late for the same reason.
- Polling job is now cancelled on every manual load, so a refresh can't race a background poll.

## Lint warnings left (all low risk)
- `UnusedResources` (none blocking) and `ObsoleteSdkInt` on `mipmap-anydpi-v26` (kept: the sandbox's aapt2 needs the qualifier for adaptive icons).
- A few cosmetic Compose lint hints. Full list: `app/app/build/reports/lint-results-debug.html` after running lint.

## Not covered by automated tests
- Real backend responses (no gateway deployed), push notifications, Play Billing, deep links on device, notification permission dialog, screenshot/visual regression.

## Commands
```bash
cd app
./gradlew :app:assembleDebug
./gradlew :app:testDebugUnitTest
./gradlew :app:lintDebug
./gradlew :app:connectedDebugAndroidTest   # with an emulator or device attached
```
On an ARM Linux host only: put `android.aapt2FromMavenOverride=<path to an aarch64 aapt2>` in `~/.gradle/gradle.properties`. Not needed on Windows, macOS or x86 Linux.
