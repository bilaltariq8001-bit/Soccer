# API contracts (Pitchside REST gateway)

The app talks to one gateway; the API-Football key never ships in the app. Base URL comes from the Gradle property `pitchsideApiBaseUrl` (empty = built-in mock). Implementation: `data/remote/HttpFootballApi.kt`; shapes: `data/remote/dto/Dtos.kt` (they mirror the Firestore documents in `backend/functions/src/types.ts`, with ids as strings).

## Conventions
- JSON, UTF-8. Unknown fields are ignored by the app, so additive changes are safe.
- Lists are wrapped: `{"items":[...], "nextCursor": "4"|null, "updatedAtMs": 1794508200000}`.
- Auth: `Authorization: Bearer <token>` when signed in; all football reads also work as guest. Header `X-Client: android/<version>`.
- Errors: HTTP status + `{"code":"string","message":"human readable"}`. App mapping: 400/422 → Validation (message shown), 401 → Unauthorized (session cleared, sign-in offered), 403 → Forbidden, 404 → NotFound, 429 → RateLimited, 5xx → Server. Network/timeout/parse errors are typed too.
- Client policy: connect 10 s, read 15 s, call 30 s. GETs retry up to 2 times (backoff 400 ms × attempt) on network, timeout, 429, 5xx. POSTs never retry.

## Endpoints

| Method | Path | Auth | Query / path | Success body |
|---|---|---|---|---|
| GET | `/v1/fixtures` | optional | `date=YYYY-MM-DD` | `ListEnvelope<Fixture>` |
| GET | `/v1/fixtures/live` | optional | – | `ListEnvelope<Fixture>` |
| GET | `/v1/fixtures/{id}` | optional | id | `Fixture` |
| GET | `/v1/fixtures/{id}/events` | optional | id | `ListEnvelope<Event>` |
| GET | `/v1/fixtures/{id}/stats` | optional | id | `ListEnvelope<StatLine>` |
| GET | `/v1/fixtures/{id}/lineups` | optional | id | `{"teams":[Lineup]}` (empty until announced) |
| GET | `/v1/leagues` | optional | – | `ListEnvelope<League>` |
| GET | `/v1/leagues/{id}` | optional | id | `League` |
| GET | `/v1/leagues/{id}/fixtures` | optional | id | `ListEnvelope<Fixture>` |
| GET | `/v1/leagues/{id}/standings` | optional | id | `ListEnvelope<StandingRow>` (empty for knockouts) |
| GET | `/v1/leagues/{id}/scorers` | optional | id | `ListEnvelope<Scorer>` |
| GET | `/v1/teams` | optional | `popular=true` | `ListEnvelope<TeamRef>` |
| GET | `/v1/teams/{id}` | optional | id | `Team` |
| GET | `/v1/teams/{id}/fixtures` | optional | id | `ListEnvelope<Fixture>` |
| GET | `/v1/players/{id}` | optional | id | `Player` |
| GET | `/v1/players/top` | optional | – | `ListEnvelope<PlayerSummary>` |
| GET | `/v1/news` | optional | `category=reports|transfers|analysis`, `cursor` | `ListEnvelope<News>` (paged) |
| GET | `/v1/news/{id}` | optional | id | `News` |
| GET | `/v1/search` | optional | `q` (≥2 chars) | `{"teams":[],"leagues":[],"players":[]}` |
| POST | `/v1/ask` | optional (quota per user/device) | body `{"question":"…","language":"en"}` | `{"answer":"…","remainingToday":4}` |
| POST | `/v1/auth/signin` | – | body `{"email","password"}` | `{"userId","email","displayName","accessToken","refreshToken","expiresAtMs"}` |
| POST | `/v1/auth/signup` | – | body `{"email","password","displayName"}` | same as signin |
| POST | `/v1/auth/reset` | – | body `{"email"}` | `204`, always (no account probing) |

Auth endpoints are defined (DTOs `AuthRequest`, `AuthResponse`, `ResetRequest`) for a custom backend; the recommended path is Firebase Auth instead (see FINAL_REPORT.md).

## Shapes

```json
// Fixture
{"id":"m1","leagueId":"pd","leagueName":"Premier Division","round":"Round 9","kickoffMs":1794506400000,
 "status":"live","statusShort":"2H","minute":74,"label":"","venue":"Northgate Park","referee":"A. Calder",
 "home":{"id":"ng","name":"Northgate","short":"NG","color":"#1F6F6B","logo":null,"leagueId":"pd"},
 "away":{"id":"ru","name":"Riverside","short":"RU","color":"#7A3FBF"},
 "homeScore":2,"awayScore":1,"brief":"Northgate lead 2-1, but Riverside have had 7 shots since half time."}
// status: upcoming | live | finished | postponed (cancelled/suspended map to postponed)

// Event
{"key":"e1","kind":"goal","minute":23,"extra":null,"teamId":"ng","playerName":"Leo Marsh","assistName":"Theo Ellis","detail":""}
// kind: goal | yellowCard | redCard | sub (unknown kinds are skipped)

// StatLine
{"label":"Possession","home":54,"away":46,"homeText":"54%","awayText":"46%"}

// Lineup (grid is API-Football "row:col", row 1 = goalkeeper)
{"teamId":"ng","coach":"A. Lindqvist","formation":"4-3-3",
 "startXI":[{"id":"ng-1","name":"Hale","number":1,"pos":"G","grid":"1:1"}],"substitutes":[...]}

// StandingRow
{"rank":1,"team":{...TeamRef},"played":9,"won":7,"drawn":1,"lost":1,"goalsFor":21,"goalsAgainst":7,"goalDiff":14,"points":22,"form":"WWDWL"}

// League
{"id":"pd","name":"Premier Division","short":"PD","country":"Northland","teamCount":8,"season":"2026/27","currentRound":"Round 9"}

// Scorer
{"rank":1,"playerId":"ng-9","name":"Leo Marsh","team":{...},"goals":7,"assists":2}

// News
{"id":"n1","category":"reports","title":"…","dek":"…","body":"…","source":"Pitchside Desk","publishedMs":1794507480000,"readMinutes":4,"url":null}

// Team
{"team":{...},"leagueName":"Premier Division","coach":"…","stadium":"Northgate Park","city":"…","capacity":41000,"founded":1902,
 "squad":[{"playerId":"ng-9","name":"Leo Marsh","number":9,"position":"F","rating":7.6}]}

// Player
{"summary":{"id":"ng-9","name":"Leo Marsh","team":{...},"position":"Striker","number":9,"nationality":"Northland","rating":7.6},
 "age":27,"heightCm":183,"foot":"Right","marketValue":"€24.5m","goals":7,"assists":3,"matches":9,"minutes":760,
 "shotsPer90":3.1,"keyPassesPer90":1.2,"xg":6.2,"scoutingNote":"…",
 "recentMatches":[{"matchId":"…","opponent":{...},"home":true,"score":"2-1","minutes":90,"rating":8.1,"dateMs":…}],
 "career":[{"club":"Northgate","years":"2023 - now","apps":98,"goals":41,"current":true}]}
```

## Example
```
GET /v1/fixtures?date=2026-11-12
Accept: application/json
X-Client: android/0.2.0

200 {"items":[{ ...Fixture }],"updatedAtMs":1794508200000}
404 {"code":"not_found","message":"Fixture not found"}
```

## Backend mapping
The existing Cloud Functions already write `fixtures/{id}`, `fixtures/{id}/events`, `fixtures/{id}/meta/{stats,lineups}`, `standings/{leagueId}` and `scorers/{leagueId}`. A thin HTTPS function (or reading Firestore directly through a `FirestoreFootballApi` implementing the same interface) serves these contracts.
