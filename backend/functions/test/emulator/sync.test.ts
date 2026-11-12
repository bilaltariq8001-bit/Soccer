import { beforeEach, describe, expect, it } from "vitest";
import { runCleanup } from "../../src/cleanup";
import { refreshDirtyStandings, refreshMatchDetail, syncFixtures } from "../../src/sync";
import type { RawStandings, RawStatsTeam } from "../../src/types";
import { clearAll, db, fakeApi, goalEvent, rawFixture } from "./helpers";

const NOW = new Date("2026-10-29T18:00:00Z");
const nowSec = Math.floor(NOW.getTime() / 1000);

describe("daily sync", () => {
  beforeEach(clearAll);

  it("writes fixtures but never overwrites eventKeys or hash owned by the live poll", async () => {
    await db().doc("fixtures/1001").set({ id: "1001", status: "live", eventKeys: ["k1"], hash: "keep-me", homeScore: 0, awayScore: 0 });
    const api = fakeApi(() => [
      rawFixture({ ts: nowSec, short: "2H", elapsed: 60, goals: [1, 0] }),
      rawFixture({ id: 9999, league: 999, ts: nowSec }), // untracked league, must be ignored
    ]);
    const written = await syncFixtures({ db: db(), api, now: NOW });
    expect(written).toBe(4); // 4 dates, 1 tracked fixture each
    expect(api.calls).toHaveLength(4);
    const doc = (await db().doc("fixtures/1001").get()).data();
    expect(doc?.homeScore).toBe(1);
    expect(doc?.eventKeys).toEqual(["k1"]);
    expect(doc?.hash).toBe("keep-me");
    expect((await db().doc("fixtures/9999").get()).exists).toBe(false);
  });
});

describe("standings", () => {
  beforeEach(clearAll);

  it("refreshes only the dirty leagues and clears the flag", async () => {
    await db().doc("ops/standingsDirty").set({ leagues: { "39": NOW.getTime() } });
    const standings: RawStandings = {
      league: {
        id: 39, season: 2026,
        standings: [[{ rank: 1, team: { id: 10, name: "Northgate", logo: "" }, points: 22, goalsDiff: 14, form: "WWDWL", all: { played: 9, win: 7, draw: 1, lose: 1, goals: { for: 25, against: 11 } } }]],
      },
    };
    const api = fakeApi(() => [standings]);
    const done = await refreshDirtyStandings({ db: db(), api, now: NOW });
    expect(done).toBe(1);
    expect(api.calls[0]).toEqual({ path: "/standings", params: { league: 39, season: 2026 } });
    const rows = (await db().doc("standings/39_2026").get()).data()?.rows;
    expect(rows[0]).toMatchObject({ teamId: 10, points: 22, goalDiff: 14, played: 9 });
    expect((await db().doc("ops/standingsDirty").get()).data()?.leagues).toEqual({});
    // Nothing dirty now: no further call.
    expect(await refreshDirtyStandings({ db: db(), api, now: NOW })).toBe(0);
    expect(api.calls).toHaveLength(1);
  });
});

describe("on-demand match detail", () => {
  beforeEach(clearAll);

  it("refreshes once, then refuses repeats inside the 45 second lock", async () => {
    await db().doc("fixtures/1001").set({ id: "1001", status: "live", kickoffMs: NOW.getTime() - 1_800_000, home: { id: 10 }, away: { id: 20 } });
    const stats: RawStatsTeam[] = [
      { team: { id: 10, name: "N", logo: "" }, statistics: [{ type: "Ball Possession", value: "54%" }, { type: "Total Shots", value: 11 }] },
      { team: { id: 20, name: "R", logo: "" }, statistics: [{ type: "Ball Possession", value: "46%" }, { type: "Total Shots", value: 14 }] },
    ];
    const api = fakeApi((path) => {
      if (path === "/fixtures") return [rawFixture({ ts: nowSec, short: "2H", elapsed: 61, goals: [1, 0], events: [goalEvent(23, 10, 7)] })];
      if (path === "/fixtures/statistics") return stats;
      return [];
    });
    const first = await refreshMatchDetail({ db: db(), api, now: NOW }, "1001");
    expect(first.refreshed).toBe(true);
    const rows = (await db().doc("fixtures/1001/meta/stats").get()).data()?.rows;
    expect(rows.find((r: { label: string }) => r.label === "Possession")).toMatchObject({ home: 54, away: 46 });
    expect((await db().collection("fixtures/1001/events").get()).size).toBe(1);

    const callsAfterFirst = api.calls.length;
    const second = await refreshMatchDetail({ db: db(), api, now: new Date(NOW.getTime() + 10_000) }, "1001");
    expect(second).toEqual({ refreshed: false, reason: "recently-refreshed" });
    expect(api.calls).toHaveLength(callsAfterFirst);

    const later = await refreshMatchDetail({ db: db(), api, now: new Date(NOW.getTime() + 60_000) }, "1001");
    expect(later.refreshed).toBe(true);
  });

  it("reports an unknown fixture without calling the API", async () => {
    const api = fakeApi(() => []);
    expect(await refreshMatchDetail({ db: db(), api, now: NOW }, "424242")).toEqual({ refreshed: false, reason: "unknown-fixture" });
    expect(api.calls).toHaveLength(0);
  });
});

describe("cleanup", () => {
  beforeEach(clearAll);

  it("removes old fixtures with their sub-collections, expired alerts and old usage counters, and keeps the rest", async () => {
    const day = 86_400_000;
    await db().doc("fixtures/old").set({ id: "old", kickoffMs: NOW.getTime() - 20 * day });
    await db().doc("fixtures/old/events/e1").set({ k: 1 });
    await db().doc("fixtures/old/meta/stats").set({ k: 1 });
    await db().doc("fixtures/fresh").set({ id: "fresh", kickoffMs: NOW.getTime() - 1 * day });
    await db().doc("sentAlerts/a").set({ expiresAt: NOW.getTime() - 1 });
    await db().doc("sentAlerts/b").set({ expiresAt: NOW.getTime() + day });
    await db().doc("apiUsage/20250101").set({ used: 5 });
    await db().doc("apiUsage/20261029").set({ used: 5 });

    const result = await runCleanup(db(), NOW);
    expect(result).toEqual({ fixtures: 1, alerts: 1, usage: 1 });
    expect((await db().doc("fixtures/old").get()).exists).toBe(false);
    expect((await db().collection("fixtures/old/events").get()).size).toBe(0);
    expect((await db().doc("fixtures/fresh").get()).exists).toBe(true);
    expect((await db().doc("sentAlerts/b").get()).exists).toBe(true);
    expect((await db().doc("apiUsage/20261029").get()).exists).toBe(true);
  });
});
