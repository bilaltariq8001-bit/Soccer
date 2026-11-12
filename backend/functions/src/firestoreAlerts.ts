import type { Firestore } from "firebase-admin/firestore";
import type { AlertDeps, AlertMessage } from "./alerts";

/** Alert dedupe backed by Firestore: `create` fails with ALREADY_EXISTS when the alert was already recorded. */
export function firestoreAlertDeps(db: Firestore, send: (message: AlertMessage) => Promise<unknown>, now: () => number = Date.now): AlertDeps {
  return {
    async claim(id) {
      try {
        await db.doc(`sentAlerts/${id}`).create({ createdAtMs: now(), expiresAt: now() + 3 * 86_400_000 });
        return true;
      } catch (e) {
        const err = e as { code?: number | string; message?: string };
        if (err.code === 6 || err.code === "already-exists" || /already exists/i.test(err.message ?? "")) return false;
        throw e;
      }
    },
    async send(message) {
      await send(message);
    },
  };
}
