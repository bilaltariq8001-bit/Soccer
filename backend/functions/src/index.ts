import { initializeApp } from "firebase-admin/app";
import { getFirestore } from "firebase-admin/firestore";
import type { Firestore } from "firebase-admin/firestore";
import { getMessaging } from "firebase-admin/messaging";
import { setGlobalOptions } from "firebase-functions/v2";
import { HttpsError, onCall } from "firebase-functions/v2/https";
import { onSchedule } from "firebase-functions/v2/scheduler";
import { defineInt, defineSecret } from "firebase-functions/params";
import * as logger from "firebase-functions/logger";
import type { AlertDeps } from "./alerts";
import { createApiClient } from "./apiFootball";
import { REGION, TRACKED_LEAGUES } from "./config";
import { runCleanup } from "./cleanup";
import { firestoreAlertDeps } from "./firestoreAlerts";
import { runLivePoll } from "./live";
import { createBudget } from "./quota";
import { refreshDirtyStandings, refreshMatchDetail, refreshStandings, syncFixtures, syncScorers } from "./sync";

initializeApp();
setGlobalOptions({ region: REGION, maxInstances: 5 });

/** Set with: firebase functions:secrets:set API_FOOTBALL_KEY */
const apiKey = defineSecret("API_FOOTBALL_KEY");
/** Daily request cap of your API-Football plan (Free 100, Pro 7500). Set per environment. */
const dailyCap = defineInt("API_DAILY_CAP", { default: 100 });

function context() {
  const db = getFirestore();
  const budget = createBudget(db, dailyCap.value());
  const api = createApiClient(apiKey.value(), budget);
  return { db, budget, api, now: new Date() };
}

function alertDeps(db: Firestore): AlertDeps {
  const messaging = getMessaging();
  return firestoreAlertDeps(db, (message) => messaging.send(message));
}

/** Every minute. Skips itself outside match windows and when the API budget is low. */
export const pollLive = onSchedule({ schedule: "every 1 minutes", secrets: [apiKey], timeoutSeconds: 55, memory: "256MiB" }, async () => {
  const c = context();
  const result = await runLivePoll({ ...c, alerts: alertDeps(c.db) });
  logger.info("pollLive", result);
});

/** 02:00 UTC daily: fixtures for yesterday, today and the next two days. */
export const syncFixturesDaily = onSchedule({ schedule: "0 2 * * *", timeZone: "UTC", secrets: [apiKey], timeoutSeconds: 300 }, async () => {
  const c = context();
  const written = await syncFixtures(c);
  const standings = await refreshStandings(c, TRACKED_LEAGUES.map((l) => l.id));
  logger.info("syncFixturesDaily", { written, standings });
});

/** Every 10 minutes: refresh tables of leagues whose matches just finished. */
export const refreshStandingsDirty = onSchedule({ schedule: "every 10 minutes", secrets: [apiKey], timeoutSeconds: 120 }, async () => {
  const done = await refreshDirtyStandings(context());
  if (done > 0) logger.info("refreshStandingsDirty", { done });
});

/** 03:00 UTC daily: top scorers. */
export const syncScorersDaily = onSchedule({ schedule: "0 3 * * *", timeZone: "UTC", secrets: [apiKey], timeoutSeconds: 300 }, async () => {
  logger.info("syncScorersDaily", { done: await syncScorers(context()) });
});

/** 04:00 UTC daily: retention. */
export const cleanupDaily = onSchedule({ schedule: "0 4 * * *", timeZone: "UTC", timeoutSeconds: 300 }, async () => {
  logger.info("cleanupDaily", await runCleanup(getFirestore(), new Date()));
});

/** Called by the app when a match screen opens. Signed-in users with a valid App Check token only. */
export const refreshMatch = onCall({ secrets: [apiKey], enforceAppCheck: true }, async (request) => {
  if (!request.auth) throw new HttpsError("unauthenticated", "Sign in first.");
  const fixtureId = String(request.data?.fixtureId ?? "");
  if (!/^\d+$/.test(fixtureId)) throw new HttpsError("invalid-argument", "fixtureId must be a number.");
  return refreshMatchDetail(context(), fixtureId);
});
