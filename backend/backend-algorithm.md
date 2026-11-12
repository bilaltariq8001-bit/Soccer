# Pitchside backend: algorithm and design

Status: design plus first implementation in `functions/`. Not deployed. API details marked **verify** came from search results, not the official docs, and must be checked in the API-Football dashboard before launch.

## 1. Goals and constraints

- Serve live scores, fixtures, standings, lineups, stats and alerts to the Android app with no key in the app.
- Cost must not grow with users. One shared poll feeds everyone.
- Stay inside the API-Football daily request cap (Free 100/day for dev, Pro 7,500/day at launch).
- Alerts must work without a background service on the phone: FCM data messages plus local rules.
- AI features use **Firebase AI Logic** (client SDK, App Check protected). The backend only prepares facts for them.
- Cloud Functions that call an external API and run on a schedule need the Firebase **Blaze** (pay as you go) plan. Dev can use the emulators on Spark.

## 2. Architecture

```
API-Football ──► Cloud Functions (scheduled sync, on-demand refresh) ──► Firestore (normalized, public read)
                         │                                                    │
                         ├──► FCM topics (team_{id}_all | key | final)        ▼
                         │                                              Android app (listeners + Room cache)
                         └──► ops docs (api usage, dirty flags)                │
                                                                               ▼
                                                              Firebase AI Logic (Gemini) with App Check
```

- The app never talks to API-Football. It reads Firestore and subscribes to FCM topics.
- Only Cloud Functions (Admin SDK) write football data. Security rules block all client writes to it.

## 3. Identity

- Firebase Auth **anonymous** sign-in on first launch. The uid owns `users/{uid}`.
- Later: link Google or email to the same uid, so follows survive a reinstall.
- Pro entitlement is a **custom claim** `pro: true` set by a server function after Play purchase verification (not in the user document, so users cannot edit it).

## 4. Firestore data model

| Path | Written by | Purpose |
|---|---|---|
| `leagues/{id}` | sync | Tracked competitions (name, logo, country) |
| `teams/{id}` | sync | Team name, logo, venue |
| `fixtures/{fixtureId}` | live poll, daily sync | One match summary: teams, score, status, minute, kickoff, `dateKey`, `eventKeys`, `hash` |
| `fixtures/{id}/events/{eventKey}` | live poll | Goals, cards, subs |
| `fixtures/{id}/meta/stats` | on-demand refresh | Possession, shots, passes, fouls, corners |
| `fixtures/{id}/meta/lineups` | on-demand refresh | Formation, starters, bench, coach |
| `fixtures/{id}/meta/briefFacts` | live poll | Deterministic facts for the Match Brief (see section 8) |
| `liveIndex/current` | live poll | One document listing all live matches, so the Live tab needs one listener |
| `standings/{leagueId}_{season}` | standings sync | Table rows with form |
| `scorers/{leagueId}_{season}` | daily sync | Top scorers |
| `users/{uid}` | the user | Preferences and follows (validated by rules) |
| `users/{uid}/aiUsage/{yyyymmdd}` | the user | UX counter for the Ask limit |
| `sentAlerts/{fixtureId_eventKey_topic}` | alerts | Dedupe so an alert is never sent twice |
| `apiUsage/{yyyymmdd}` | quota | API-Football calls used today |
| `ops/standingsDirty` | live poll | Leagues whose table needs a refresh |

Indexes: `fixtures` by `dateKey` + `kickoffMs`, by `dateKey` + `leagueId` + `kickoffMs`, and by `status` + `kickoffMs` (see `firestore.indexes.json`).

## 5. Sync algorithm

### 5.1 Live poll (every minute)

```
tick:
  mode = budgetMode()                       # normal < 80% of cap, degraded < 95%, stop otherwise
  if mode == stop: return
  if mode == degraded and minute % 3 != 0: return
  if not inLiveWindow(now): return           # nothing live and nothing kicking off within 10 minutes
  reserve 1 call                             # GET /fixtures?live=all  (events are included per live fixture)
  live = response filtered to tracked leagues
  for each fixture in live:
      doc = normalize(fixture)
      prev = Firestore fixtures/{id}
      if prev exists and prev.hash == doc.hash: continue          # nothing changed, no write
      write fixture doc, new events, briefFacts
      newEvents = events whose key is not in prev.eventKeys
      transitions = kickoff (upcoming -> live), full time (live -> finished)
      for each newEvent and transition: sendAlert (section 6)
      if transition is full time: mark league dirty for standings
  missing = ids in liveIndex not in this response    # finished or dropped
  refetch up to 5 of them by id (1 call each) so they reach FT
  write liveIndex/current
```

- `inLiveWindow` is true when a fixture has `status in [upcoming, live]` and `kickoffMs` within the last 3 hours or the next 10 minutes. This keeps quiet hours at zero calls.
- One call per tick regardless of user count. At 60 s this is at most about 1,440 calls on a day with matches around the clock, normally 600-1,200 (matches only run part of the day).
- Faster polling (30 s) is possible later with Cloud Tasks, at double the cost. Start at 60 s.

### 5.2 Daily fixtures sync (02:00 UTC)

`GET /fixtures?date=YYYY-MM-DD` for today and the next 2 days (3 calls). Filter to tracked leagues, write with `merge`. Past 2 days are re-synced once to catch late score corrections.

### 5.3 Standings and scorers

- Standings: scheduled every 10 minutes, processes `ops/standingsDirty` (leagues whose matches just finished), 1 call per league, then clears the flag. Also a daily full refresh.
- Scorers: daily, 1 call per league (`/players/topscorers`).

### 5.4 On-demand match detail

Callable `refreshMatch(fixtureId)`: requires sign-in and App Check. Rejects if `fixtures/{id}/meta/lock.refreshedAt` is under 45 s old (so many viewers cost one refresh). Fetches events and statistics (2 calls), and lineups when kickoff is within 90 minutes or the match has started (lineups appear 20-60 minutes before kickoff, **verify**).

### 5.5 Request budget

- `apiUsage/{day}.used` is incremented in a transaction before each call (`reserve`). If it would pass the cap, the call is refused.
- Alert at 80% (log + error report). Degrade at 80%, stop at 95% (keeps a reserve for final scores).
- Retries only for 429 and 5xx, exponential backoff, at most 3 attempts. The API returns HTTP 200 with an `errors` object on some failures, so the client checks it.
- Plans: dev on Free (100/day) with the emulator and a recorded-response mock; beta on Pro; scale to Ultra when usage nears 70% of the Pro cap.

## 6. Alerts

Topics (the app subscribes and unsubscribes when follows or levels change):

| Topic | Receives |
|---|---|
| `team_{id}_all` | goals, cards, kickoff, full time |
| `team_{id}_key` | goals, red cards, full time |
| `team_{id}_final` | full time |

Event to topics:

| Event | Topics |
|---|---|
| goal, red card | `_all`, `_key` |
| yellow card, kickoff | `_all` |
| full time | `_all`, `_key`, `_final` |

For every event, both teams' topics are used, because followers of either side want to know.

```
sendAlert(fixture, event, kind):
    for team in [home, away]:
        for level in levelsFor(kind):
            topic = team_{team.id}_{level}
            ref = sentAlerts/{fixtureId}_{eventKey}_{topic}
            if ref.create(...) fails with ALREADY_EXISTS: continue      # dedupe
            FCM.send({ topic, data: payload, android.priority: high for goal/red/full time else normal })
```

Payload is **data-only** and carries facts, never display text (see the Android-Channels board): `v, type, matchId, teamId, home, away, score, minute, scorer, ts`. All values are strings.

On the phone (not here): quiet hours, Spoiler Shield, per-team mute and the user's language are applied when the message arrives. Held alerts are released later by WorkManager. The backend never knows who is shielded, which keeps it simple and private.

## 7. Offline and caching

- Android keeps a Room cache; every screen renders cached data first, then updates (stale-while-revalidate).
- Firestore's own offline persistence can serve as the cache for listeners.
- Every list shows "Updated N min ago" from `updatedAt`.

## 8. Match Brief

Backend job (deterministic, no AI): for each changed live fixture, write `fixtures/{id}/meta/briefFacts` with score, minute, last 3 events, current table positions and points for both clubs, and the table **if the current result holds** (`standingsIfResultHolds`, see `functions/src/brief.ts`). This is where numbers come from, so the AI never invents them.

Client job: build a prompt from `briefFacts`, call Gemini through **Firebase AI Logic** with the system rule "use only these facts", show the text, cache it on the device keyed by `facts.hash + language`. If AI fails or the quota is used, show a template string built from the same facts.

Honest limits of this design:

- Firebase AI Logic is a client SDK, so there is **no shared server cache**: each device generates its own brief (cost grows with users). Mitigations: App Check, the per-user rate limit in Firebase AI Logic settings, device caching, short prompts, the cheapest Flash-class model read from Remote Config.
- The Ask limit counter in `users/{uid}/aiUsage` is a UX counter. A modified client could skip it, so the real hard limit is the AI Logic per-user rate limit plus App Check.
- If cost becomes a problem, add a server-side generator (Genkit/Vertex AI) that stores text in Firestore for everyone. That would not be Firebase AI Logic, so it is your call.

## 9. Security rules (summary, see `firestore.rules`)

- Default deny.
- Football data: read for any signed-in user (anonymous counts), no client writes.
- `users/{uid}`: read and write only by the owner, only known fields, follow list capped (5 free, 200 Pro by custom claim).
- `users/{uid}/aiUsage/{day}`: owner only, count capped at 5 (free) or 50 (Pro) and may only rise by 1.
- Turn on **App Check (Play Integrity)** enforcement for Firestore, Functions and Firebase AI Logic before launch.

## 10. Pro entitlement (next step)

Play Billing purchase token goes to a callable `verifyPurchase`, which checks it with the Google Play Developer API and sets the custom claim `pro: true`. Real-time developer notifications renew or revoke it. Not built yet.

## 11. Retention and cleanup (daily)

- Delete fixtures older than 14 days (with their sub-collections), `sentAlerts` past `expiresAt`, and `apiUsage` older than 90 days.

## 12. Observability

Structured logs per tick (calls used, fixtures changed, alerts sent), error reporting on failures, an alert when daily usage passes 80%.

## 13. Environments and secrets

- `dev` and `prod` Firebase projects. The API key lives in **Secret Manager** as `API_FOOTBALL_KEY` and is read only by functions.
- Region: set `REGION` in `functions/src/config.ts` to match your Firestore location (default `europe-west1`).

## 14. Legal

API-Football returns logo and photo URLs. Check its terms before hotlinking in a published app, and whether images must be served from your own CDN. League and team names here are real data, so the app's fictional names go away once the backend is connected.

## 15. Build order

1. Rules, indexes, config (done).
2. API client, quota, normalizers, alert logic, brief facts (done, unit-tested logic).
3. Scheduled functions and callable refresh (done, not yet run).
4. Install Node, build, run unit tests, run on the emulator with recorded responses.
5. Create the Firebase project, set the secret, deploy to dev.
6. Android: `FirestoreRepository` behind the existing `Repository` interface, FCM topic subscription, notification service, App Check, Firebase AI Logic.
