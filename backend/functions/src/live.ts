import type { Firestore } from "firebase-admin/firestore";
import { buildFacts } from "./brief";
import type { AlertDeps, AlertInput } from "./alerts";
import { sendAlert } from "./alerts";
import type { ApiClient } from "./apiFootball";
import { LIVE_WINDOW_AFTER_MS, LIVE_WINDOW_BEFORE_MS, MAX_MISSING_REFETCH, TRACKED_IDS } from "./config";
import { normalizeEvents, normalizeFixture } from "./normalize";
import type { Budget } from "./quota";
import type { EventDoc, FixtureDoc, RawFixture, StandingRow } from "./types";

export interface PollContext {
  db: Firestore;
  api: ApiClient;
  budget: Budget;
  alerts: AlertDeps;
  now: Date;
}

export interface PollResult {
  skipped?: string;
  changed: number;
  alertsSent: number;
  live: number;
}

/** True when something is live or about to kick off, so quiet hours cost zero API calls. */
export async function inLiveWindow(db: Firestore, now: Date): Promise<boolean> {
  const t = now.getTime();
  const q = await db.collection("fixtures")
    .where("status", "in", ["upcoming", "live"])
    .where("kickoffMs", ">=", t - LIVE_WINDOW_BEFORE_MS)
    .where("kickoffMs", "<=", t + LIVE_WINDOW_AFTER_MS)
    .limit(1)
    .get();
  return !q.empty;
}

export function summary(f: FixtureDoc) {
  return {
    id: f.id, leagueId: f.leagueId, leagueName: f.leagueName,
    home: { id: f.home.id, name: f.home.name, logo: f.home.logo },
    away: { id: f.away.id, name: f.away.name, logo: f.away.logo },
    homeScore: f.homeScore, awayScore: f.awayScore, status: f.status, label: f.label, minute: f.minute,
  };
}

async function loadTable(db: Firestore, cache: Map<string, StandingRow[] | null>, f: FixtureDoc): Promise<StandingRow[] | null> {
  const id = `${f.leagueId}_${f.season}`;
  if (cache.has(id)) return cache.get(id) ?? null;
  const snap = await db.doc(`standings/${id}`).get();
  const rows = (snap.data()?.rows as StandingRow[] | undefined) ?? null;
  cache.set(id, rows);
  return rows;
}

/** Writes one fixture if it changed and sends alerts for anything new. Returns [changed, alertsSent, doc]. */
async function processFixture(
  ctx: PollContext, raw: RawFixture, tables: Map<string, StandingRow[] | null>,
): Promise<{ changed: boolean; alertsSent: number; doc: FixtureDoc }> {
  const { db, alerts, now } = ctx;
  const events = normalizeEvents(raw.events);
  const doc = normalizeFixture(raw, events);
  const ref = db.doc(`fixtures/${doc.id}`);
  const prev = (await ref.get()).data() as FixtureDoc | undefined;
  if (prev && prev.hash === doc.hash) return { changed: false, alertsSent: 0, doc };

  const known = new Set(prev?.eventKeys ?? []);
  const fresh: EventDoc[] = events.filter((e) => !known.has(e.key));

  const batch = db.batch();
  batch.set(ref, doc);
  for (const e of fresh) batch.set(ref.collection("events").doc(e.key), e);
  const table = await loadTable(db, tables, doc);
  batch.set(ref.collection("meta").doc("briefFacts"), buildFacts(doc, events, table));
  await batch.commit();

  const base = (kind: AlertInput["kind"], eventKey: string, scorer: string, minute: number | null): AlertInput => ({
    kind, fixtureId: doc.id, eventKey, home: doc.home.name, away: doc.away.name,
    homeScore: doc.homeScore, awayScore: doc.awayScore, minute, scorer,
    homeTeamId: doc.home.id, awayTeamId: doc.away.id, tsMs: now.getTime(),
  });

  let sent = 0;
  for (const e of fresh) {
    if (e.kind === "goal") sent += await sendAlert(alerts, base("goal", e.key, e.playerName, e.minute));
    else if (e.kind === "redCard") sent += await sendAlert(alerts, base("redCard", e.key, e.playerName, e.minute));
    else if (e.kind === "yellowCard") sent += await sendAlert(alerts, base("yellowCard", e.key, e.playerName, e.minute));
  }
  // Kickoff only counts when we see the match in its first minutes, so a late first sighting does not alert.
  if (doc.status === "live" && prev?.status !== "live" && (doc.minute ?? 99) <= 2) {
    sent += await sendAlert(alerts, base("kickoff", "kickoff", "", doc.minute));
  }
  if (doc.status === "finished" && prev?.status === "live") {
    sent += await sendAlert(alerts, base("fullTime", "fulltime", "", null));
    await db.doc("ops/standingsDirty").set({ leagues: { [String(doc.leagueId)]: now.getTime() } }, { merge: true });
  }
  return { changed: true, alertsSent: sent, doc };
}

/** One tick of the live poll. See "5.1 Live poll" in backend-algorithm.md. */
export async function runLivePoll(ctx: PollContext): Promise<PollResult> {
  const { db, api, budget, now } = ctx;
  const mode = await budget.mode();
  if (mode === "stop") return { skipped: "budget-stop", changed: 0, alertsSent: 0, live: 0 };
  if (mode === "degraded" && now.getUTCMinutes() % 3 !== 0) return { skipped: "budget-degraded", changed: 0, alertsSent: 0, live: 0 };
  if (!(await inLiveWindow(db, now))) return { skipped: "no-live-window", changed: 0, alertsSent: 0, live: 0 };

  const tables = new Map<string, StandingRow[] | null>();
  const raw = (await api.get<RawFixture>("/fixtures", { live: "all" })).filter((r) => TRACKED_IDS.has(r.league.id));
  const liveDocs = new Map<string, FixtureDoc>();
  let changed = 0;
  let alertsSent = 0;

  for (const r of raw) {
    const res = await processFixture(ctx, r, tables);
    if (res.changed) changed++;
    alertsSent += res.alertsSent;
    liveDocs.set(res.doc.id, res.doc);
  }

  // Matches that were live last tick but vanished from the live list have probably finished. Fetch them directly.
  const idxRef = db.doc("liveIndex/current");
  const prevIds = ((await idxRef.get()).data()?.ids as string[] | undefined) ?? [];
  const missing = prevIds.filter((id) => !liveDocs.has(id)).slice(0, MAX_MISSING_REFETCH);
  for (const id of missing) {
    const found = await api.get<RawFixture>("/fixtures", { id });
    for (const r of found) {
      const res = await processFixture(ctx, r, tables);
      if (res.changed) changed++;
      alertsSent += res.alertsSent;
      if (res.doc.status === "live") liveDocs.set(res.doc.id, res.doc);
    }
  }

  const docs = [...liveDocs.values()].filter((d) => d.status === "live");
  await idxRef.set({ ids: docs.map((d) => d.id), items: docs.map(summary), updatedAtMs: now.getTime() });
  return { changed, alertsSent, live: docs.length };
}
