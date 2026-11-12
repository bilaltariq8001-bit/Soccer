# Feature gap analysis

"Before" is the code as uploaded on 2026-11-12 (commit ace9581). "After" is branch `hark/rebuild-app`.

| Expected feature | Before | Gap | Fix | Priority | Status |
|---|---|---|---|---|---|
| Data architecture | One global `AppViewModel` + synchronous `FakeRepository` object called straight from composables | No async, no errors, no loading, untestable | `FootballApi` (HTTP + mock) → `DefaultFootballRepository` (Result, cache, dispatcher) → per-screen ViewModels with `UiState` | P0 | Done |
| Loading/empty/error/retry | Fake `delay(500)` skeleton on 2 screens; no error states | Screens could never show an error or retry | `StateContent` + `ErrorState` + `LoadViewModel` (refresh keeps data, banner on failure) | P0 | Done |
| Persistence | SharedPreferences written from a `snapshotFlow` loop | Unsafe concurrent writes, string-joined lists | DataStore with one serialized `UserPrefs` document + pure `PrefsReducer` | P0 | Done |
| Fixtures by date | Today only | No yesterday/tomorrow/calendar | Matches: Yesterday/Today/Tomorrow + date picker, Live/My teams filters, league grouping | P1 | Done |
| Live list + count | None | No live badge | Live filter, live-count badge on Matches tab, 30 s polling while live | P1 | Done |
| League page | One hardcoded "Premier Division" screen as a tab | No league list, no per-league page | Leagues tab (list + top players) and League page (Table Short/Full/Form, Matches, Top scorers, Teams) | P1 | Done |
| Team page | Missing | Couldn't open a club | Team page: Matches, Overview, Squad by position, Table; follow + Spoiler Shield | P1 | Done |
| Player page | Always player "p9" | Every scorer opened the same player | Player by id: Profile, Matches, Stats (Season/Per 90), Career | P1 | Done |
| Match detail | Hardcoded events/stats for every match, table always "pd" | Wrong data on most matches | Detail combines fixture, events, stats, lineups, table; knockout + pre-kickoff states explained | P1 | Done |
| Search | Filters in-memory; players only "p9" | Not real search | Debounced repository search, cancel stale queries, tabs, per-tab empty | P1 | Done |
| News | Static list, cards did nothing | Dead buttons | Paginated news, category chips, Article screen with share + source link | P1 | Done |
| Auth | "Account sign-in arrives with the backend" toast | No auth at all | Sign in / Create account / Reset password, validation, Keystore-encrypted session, protected Account route, sign out | P1 | Done (local auth; Firebase pending keys) |
| Onboarding build step | Fake timed loader | Lied about work | Real steps: save prefs, prefetch fixtures/leagues; offline steps marked skipped | P1 | Done |
| Settings rows | Privacy/Terms/Restore did nothing | Dead buttons | Privacy dialog, Share, Rate, Version, Developer failure simulation, reset | P2 | Done |
| Exit confirmation | None | Accidental exit | Back on Home asks first | P2 | Done |
| Deep links | None | Alerts can't open a match | `pitchside://match/{id}`, `league/{id}`, `team/{id}` | P2 | Done |
| Splash | Onboarding could flash before prefs load | Visual glitch | SplashScreen held until DataStore read | P2 | Done |
| Accessibility | Partial | Crest monograms read aloud; errors not announced | Decorative crests hidden, merged descriptions on match rows/table rows, live regions on errors, 48dp targets | P2 | Done |
| Highlights / Shorts / TV / Referees | - | No licensed data source | Not built (documented) | P3 | Out of scope |
| Push alerts (FCM), Play Billing, Firebase AI | - | Need google-services.json, Play Console | Interfaces ready; documented | P3 | Blocked on secrets |
