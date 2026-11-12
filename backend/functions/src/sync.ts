import type { Firestore } from "firebase-admin/firestore";
import type { ApiClient } from "./apiFootball";
import { DETAIL_REFRESH_TTL_MS, TRACKED_IDS, TRACKED_LEAGUES, currentSeason, dateKey } from "./config";
import { normalizeEvents, normalizeFixture, normalizeLineup, normalizeStandings, normalizeStats } from "./normalize";
import type { FixtureDoc, RawFixture, RawLineup, RawStandings, RawStatsTeam, RawTopScorer } from "./types";

export interface SyncContext {
  db: Firestore;
  api: ApiClient;
  now: Date;
}

async function commitChunks(db: Firestore, writes: Array<{ path: string; data: Record<string, unknown>; merge: boolean }>) {
  for (let i = 0; i < writes.length; i += 400) {
    const batch = db.batch();
    for (const w of writes.slice(i, i + 400)) batch.set(db.doc(w.path), w.data, { merge: w.merge });
    await batch.commit();
  }
}

/** Daily: fixtures for yesterday (late corrections), today and the next two days. 1 API call per date. */
export async function syncFixtures(ctx: SyncContext, daysBack = 1, daysAhead = 2): Promise<number> {
  let written = 0;
  for (let offset = -daysBack; offset <= daysAhead; offset++) {
    const date = dateKey(ctx.now.getTime() + offset * 86_400_000);
    const raw = (await ctx.api.get<RawFixture>("/fixtures", { date })).filter((r) => TRACKED_IDS.has(r.league.id));
    const writes = raw.map((r) => {
      // Date lists carry no events, so keep eventKeys and hash out of this write: the live poll owns them.
      const { eventKeys: _keys, hash: _hash, ...rest } = normalizeFixture(r, []);
      return { path: `fixtures/${rest.id}`, data: rest as Record<string, unknown>, merge: true };
    });
    await commitChunks(ctx.db, writes);
    written += writes.length;
  }
  return written;
}

/** Standings for the given leagues, 1 API call each. */
export async function refreshStandings(ctx: SyncContext, leagueIds: number[]): Promise<number> {
  const season = currentSeason(ctx.now);
  let done = 0;
  for (const leagueId of leagueIds) {
    const res = await ctx.api.get<RawStandings>("/standings", { league: leagueId, season });
    const table = res[0]?.league.standings[0];
    if (!table) continue;
    await ctx.db.doc(`standings/${leagueId}_${season}`).set({ rows: normalizeStandings(table), updatedAtMs: ctx.now.getTime() });
    done++;
  }
  return done;
}

/** Scheduled every 10 minutes: refreshes only the leagues whose matches just finished. */
export async function refreshDirtyStandings(ctx: SyncContext): Promise<number> {
  const ref = ctx.db.doc("ops/standingsDirty");
  const snap = await ref.get();
  const dirty = (snap.data()?.leagues as Record<string, number> | undefined) ?? {};
  const ids = Object.keys(dirty).map(Number).filter((id) => TRACKED_IDS.has(id));
  if (ids.length === 0) return 0;
  const done = await refreshStandings(ctx, ids);
  await ref.set({ leagues: {} });
  return done;
}

/** Daily: top scorers, 1 API call per league. */
export async function syncScorers(ctx: SyncContext): Promise<number> {
  const season = currentSeason(ctx.now);
  let done = 0;
  for (const l of TRACKED_LEAGUES) {
    const res = await ctx.api.get<RawTopScorer>("/players/topscorers", { league: l.id, season });
    const rows = res.slice(0, 20).map((s, i) => ({
      rank: i + 1, playerId: s.player.id, name: s.player.name, photo: s.player.photo,
      teamId: s.statistics[0]?.team.id ?? 0, teamName: s.statistics[0]?.team.name ?? "", goals: s.statistics[0]?.goals.total ?? 0,
    }));
    await ctx.db.doc(`scorers/${l.id}_${season}`).set({ rows, updatedAtMs: ctx.now.getTime() });
    done++;
  }
  return done;
}

export interface RefreshResult {
  refreshed: boolean;
  reason?: string;
}

/**
 * On-demand detail for one match. A 45 s lock means any number of viewers cost a single refresh.
 * Fetches events and statistics (2 calls), plus lineups when the match is near or past kickoff (1 call).
 */
export async function refreshMatchDetail(ctx: SyncContext, fixtureId: string): Promise<RefreshResult> {
  const fixRef = ctx.db.doc(`fixtures/${fixtureId}`);
  const fix = (await fixRef.get()).data() as FixtureDoc | undefined;
  if (!fix) return { refreshed: false, reason: "unknown-fixture" };

  const lockRef = fixRef.collection("meta").doc("lock");
  const nowMs = ctx.now.getTime();
  const acquired = await ctx.db.runTransaction(async (tx) => {
    const last = ((await tx.get(lockRef)).data()?.refreshedAtMs as number | undefined) ?? 0;
    if (nowMs - last < DETAIL_REFRESH_TTL_MS) return false;
    tx.set(lockRef, { refreshedAtMs: nowMs });
    return true;
  });
  if (!acquired) return { refreshed: false, reason: "recently-refreshed" };

  const events = normalizeEvents((await ctx.api.get<RawFixture>("/fixtures", { id: fixtureId }))[0]?.events);
  const batch = ctx.db.batch();
  for (const e of events) batch.set(fixRef.collection("events").doc(e.key), e);

  const stats = await ctx.api.get<RawStatsTeam>("/fixtures/statistics", { fixture: fixtureId });
  batch.set(fixRef.collection("meta").doc("stats"), { rows: normalizeStats(stats, fix.home.id), updatedAtMs: nowMs });

  const nearKickoff = fix.kickoffMs - nowMs < 90 * 60_000;
  if (nearKickoff || fix.status !== "upcoming") {
    const lineups = await ctx.api.get<RawLineup>("/fixtures/lineups", { fixture: fixtureId });
    if (lineups.length > 0) {
      batch.set(fixRef.collection("meta").doc("lineups"), { teams: lineups.map(normalizeLineup), updatedAtMs: nowMs });
    }
  }
  await batch.commit();
  return { refreshed: true };
}
