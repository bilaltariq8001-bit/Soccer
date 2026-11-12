# Pitchside backend

Firebase backend for the Pitchside app: Firestore, Cloud Functions (TypeScript), FCM.
Design and algorithms: [backend-algorithm.md](backend-algorithm.md).

## What is here

| Path | Purpose |
|---|---|
| `firestore.rules`, `firestore.indexes.json` | Security rules and indexes |
| `functions/src/live.ts` | Live poll (every minute), change detection, alerts, liveIndex |
| `functions/src/sync.ts` | Daily fixtures, standings, scorers, on-demand match detail |
| `functions/src/alerts.ts` | Alert routing to topics, payload, dedupe |
| `functions/src/brief.ts` | Deterministic Match Brief facts and "if the result holds" table |
| `functions/src/apiFootball.ts`, `quota.ts` | API client with retry, daily request budget |
| `functions/test/logic.test.ts` | Unit tests for the pure logic |

## Status

Compiles cleanly. 18 unit tests plus 23 emulator tests pass (Node 26 LTS, firebase-tools 15). Not deployed.

- `npm test`: fast unit tests (pure logic).
- `npm run test:emulated`: starts the Firestore emulator and runs the live poll, daily sync, standings, on-demand detail, cleanup and security rules tests against it. The API and FCM are faked, so no key, project or billing is needed.

**Not tested against the real thing:** API-Football's actual response shapes (field names come from its public docs and search results, so check one real response), real FCM delivery, App Check, and Play Billing.

## Setup (needs your Google account, so it cannot be done for you)

1. Install Node.js 20 or 22 LTS and the Firebase CLI: `npm install -g firebase-tools`.
2. Create a Firebase project in the console. Upgrade it to the **Blaze** plan (needed for scheduled functions that call an outside API; the free quota covers development).
3. In the console: enable **Firestore** (choose a location, then set `REGION` in `functions/src/config.ts` to match), **Authentication > Anonymous**, **Cloud Messaging**, and **App Check** with Play Integrity.
4. `firebase login`, then put your project id in `.firebaserc`.
5. Get an API-Football key. Set it as a secret: `firebase functions:secrets:set API_FOOTBALL_KEY`. Confirm the header name in `functions/src/config.ts`.
6. Set `API_DAILY_CAP` to your plan's daily request limit (100 on Free, 7500 on Pro) in `functions/.env.<project>` or when prompted.
7. Check each id in `TRACKED_LEAGUES` in the API-Football dashboard.

## Run

```bash
cd functions
npm install
npm test            # unit tests
npm run build       # TypeScript
npm run serve       # emulators (Functions, Firestore, Auth)
firebase deploy --only firestore:rules,firestore:indexes,functions
```

## Next

Android side: `FirestoreRepository` behind the existing `Repository` interface, FCM topic subscription and a messaging service that applies quiet hours and Spoiler Shield, App Check, and Firebase AI Logic for Match Brief and Ask.
