import type { Firestore } from "firebase-admin/firestore";

export type BudgetMode = "normal" | "degraded" | "stop";

/** Below 80% of the daily cap everything runs, up to 95% we poll less, above that only a reserve is kept. */
export function modeFor(used: number, cap: number): BudgetMode {
  const ratio = used / cap;
  if (ratio >= 0.95) return "stop";
  if (ratio >= 0.8) return "degraded";
  return "normal";
}

/** UTC day, matching the provider's daily reset (verify). */
export function usageKey(now: Date): string {
  return now.toISOString().slice(0, 10).replace(/-/g, "");
}

export interface Budget {
  mode(): Promise<BudgetMode>;
  reserve(n?: number): Promise<boolean>;
}

export function createBudget(db: Firestore, cap: number, now: () => Date = () => new Date()): Budget {
  const ref = () => db.doc(`apiUsage/${usageKey(now())}`);
  return {
    async mode() {
      const snap = await ref().get();
      return modeFor((snap.data()?.used as number | undefined) ?? 0, cap);
    },
    async reserve(n = 1) {
      return db.runTransaction(async (tx) => {
        const snap = await tx.get(ref());
        const used = (snap.data()?.used as number | undefined) ?? 0;
        if (used + n > cap) return false;
        tx.set(ref(), { used: used + n, cap }, { merge: true });
        return true;
      });
    },
  };
}
