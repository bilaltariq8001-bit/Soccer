import { FieldPath } from "firebase-admin/firestore";
import type { Firestore } from "firebase-admin/firestore";
import { FIXTURE_RETENTION_DAYS } from "./config";
import { usageKey } from "./quota";

export interface CleanupResult {
  fixtures: number;
  alerts: number;
  usage: number;
}

/** Daily retention: old fixtures (with sub-collections), expired alert dedupe records, old API usage counters. */
export async function runCleanup(db: Firestore, now: Date): Promise<CleanupResult> {
  const nowMs = now.getTime();
  const result: CleanupResult = { fixtures: 0, alerts: 0, usage: 0 };

  const oldFixtures = await db.collection("fixtures").where("kickoffMs", "<", nowMs - FIXTURE_RETENTION_DAYS * 86_400_000).limit(200).get();
  for (const d of oldFixtures.docs) {
    await db.recursiveDelete(d.ref);
    result.fixtures++;
  }

  const expired = await db.collection("sentAlerts").where("expiresAt", "<", nowMs).limit(500).get();
  if (!expired.empty) {
    const batch = db.batch();
    expired.docs.forEach((d) => batch.delete(d.ref));
    await batch.commit();
    result.alerts = expired.size;
  }

  const cutoff = usageKey(new Date(nowMs - 90 * 86_400_000));
  const oldUsage = await db.collection("apiUsage").where(FieldPath.documentId(), "<", cutoff).limit(200).get();
  if (!oldUsage.empty) {
    const batch = db.batch();
    oldUsage.docs.forEach((d) => batch.delete(d.ref));
    await batch.commit();
    result.usage = oldUsage.size;
  }
  return result;
}
