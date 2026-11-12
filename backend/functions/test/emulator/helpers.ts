import { getApps, initializeApp } from "firebase-admin/app";
import { getFirestore } from "firebase-admin/firestore";
import type { Firestore } from "firebase-admin/firestore";
import type { AlertMessage } from "../../src/alerts";
import type { ApiClient } from "../../src/apiFootball";
import { firestoreAlertDeps } from "../../src/firestoreAlerts";
import type { Budget, BudgetMode } from "../../src/quota";
import type { RawEvent, RawFixture } from "../../src/types";

export const PROJECT = "demo-pitchside";

/** Firestore Admin client bound to the emulator (FIRESTORE_EMULATOR_HOST is set by `firebase emulators:exec`). */
export function db(): Firestore {
  if (getApps().length === 0) initializeApp({ projectId: PROJECT });
  return getFirestore();
}

export async function clearAll(): Promise<void> {
  const d = db();
  for (const c of await d.listCollections()) await d.recursiveDelete(c);
}

export function rawFixture(o: {
  id?: number; short?: string; elapsed?: number | null; ts: number; league?: number;
  home?: number; away?: number; goals?: [number | null, number | null]; events?: RawEvent[];
}): RawFixture {
  return {
    fixture: {
      id: o.id ?? 1001, timestamp: o.ts, venue: { name: "Northgate Park", city: "Northgate" },
      status: { short: o.short ?? "1H", elapsed: o.elapsed ?? 1, extra: null },
    },
    league: { id: o.league ?? 39, name: "Premier League", logo: "l.png", season: 2026, round: "Regular Season - 9" },
    teams: {
      home: { id: o.home ?? 10, name: "Northgate", logo: "n.png" },
      away: { id: o.away ?? 20, name: "Riverside", logo: "r.png" },
    },
    goals: { home: o.goals?.[0] ?? 0, away: o.goals?.[1] ?? 0 },
    events: o.events ?? [],
  };
}

export function goalEvent(minute: number, teamId: number, playerId: number, name = "Ellis"): RawEvent {
  return {
    time: { elapsed: minute, extra: null }, team: { id: teamId, name: "T", logo: "" },
    player: { id: playerId, name }, assist: { id: null, name: null }, type: "Goal", detail: "Normal Goal",
  };
}

export function redCardEvent(minute: number, teamId: number, playerId: number): RawEvent {
  return {
    time: { elapsed: minute, extra: null }, team: { id: teamId, name: "T", logo: "" },
    player: { id: playerId, name: "Okoro" }, assist: { id: null, name: null }, type: "Card", detail: "Red Card",
  };
}

/** Fake API: the handler decides what each endpoint returns, and every call is recorded. */
export function fakeApi(handler: (path: string, params: Record<string, string | number>) => unknown[]): ApiClient & { calls: Array<{ path: string; params: Record<string, string | number> }> } {
  const calls: Array<{ path: string; params: Record<string, string | number> }> = [];
  return {
    calls,
    async get<T>(path: string, params: Record<string, string | number>): Promise<T[]> {
      calls.push({ path, params });
      return handler(path, params) as T[];
    },
  };
}

export function budget(mode: BudgetMode = "normal"): Budget {
  return { mode: async () => mode, reserve: async () => true };
}

/** Real Firestore dedupe, with FCM replaced by a list so tests can inspect what would have been sent. */
export function capturedAlerts(database: Firestore, now: () => number = Date.now) {
  const sent: AlertMessage[] = [];
  const deps = firestoreAlertDeps(database, async (m) => { sent.push(m); }, now);
  return { deps, sent };
}
