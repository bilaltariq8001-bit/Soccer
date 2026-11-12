import { beforeEach, describe, expect, it } from "vitest";
import { runLivePoll } from "../../src/live";
import type { FixtureDoc } from "../../src/types";
import { budget, capturedAlerts, clearAll, db, fakeApi, goalEvent, rawFixture, redCardEvent } from "./helpers";

const NOW = new Date("2026-10-29T18:00:00Z");
const nowSec = Math.floor(NOW.getTime() / 1000);

/** A fixture that is about to kick off, so the live poll is allowed to run. */
async function seedUpcoming(id = "1001", kickoffMs = NOW.getTime() + 60_000) {
  await db().doc(`fixtures/${id}`).set({
    id, leagueId: 39, season: 2026, status: "upcoming", kickoffMs, eventKeys: [], hash: "seed",
    home: { id: 10, name: "Northgate", logo: "" }, away: { id: 20, name: "Riverside", logo: "" },
  });
}

describe("live poll against the Firestore emulator", () => {
  beforeEach(clearAll);

  it("does nothing and makes no API call when nothing is live or about to start", async () => {
    const api = fakeApi(() => []);
    const { deps } = capturedAlerts(db());
    const res = await runLivePoll({ db: db(), api, budget: budget(), alerts: deps, now: NOW });
    expect(res.skipped).toBe("no-live-window");
    expect(api.calls).toHaveLength(0);
  });

  it("respects the API budget modes", async () => {
    await seedUpcoming();
    const { deps } = capturedAlerts(db());
    const api = fakeApi(() => []);
    const stop = await runLivePoll({ db: db(), api, budget: budget("stop"), alerts: deps, now: NOW });
    expect(stop.skipped).toBe("budget-stop");
    // 18:00 is minute 0, which is a multiple of 3. Use 18:01 to land on a skipped tick.
    const degraded = await runLivePoll({ db: db(), api, budget: budget("degraded"), alerts: deps, now: new Date("2026-10-29T18:01:00Z") });
    expect(degraded.skipped).toBe("budget-degraded");
    expect(api.calls).toHaveLength(0);
  });

  it("walks a whole match: kickoff, no-change tick, goal, red card, full time", async () => {
    await seedUpcoming();
    const { deps, sent } = capturedAlerts(db());
    let liveResponse = [rawFixture({ ts: nowSec, short: "1H", elapsed: 1 })];
    let finalResponse = [rawFixture({ ts: nowSec, short: "FT", elapsed: 90, goals: [1, 0], events: [goalEvent(23, 10, 7), redCardEvent(30, 20, 3)] })];
    const api = fakeApi((_path, params) => (params.live ? liveResponse : finalResponse));
    const poll = (at = NOW) => runLivePoll({ db: db(), api, budget: budget(), alerts: deps, now: at });

    // 1. Kickoff: both clubs' "all" topics only.
    let res = await poll();
    expect(res.changed).toBe(1);
    expect(res.live).toBe(1);
    expect(sent.map((m) => m.topic).sort()).toEqual(["team_10_all", "team_20_all"]);
    expect(sent[0].data).toMatchObject({ v: "1", type: "kickoff", matchId: "1001", score: "0-0" });
    expect(sent[0].android.priority).toBe("normal");
    expect(((await db().doc("fixtures/1001").get()).data() as FixtureDoc).status).toBe("live");
    expect((await db().doc("liveIndex/current").get()).data()?.ids).toEqual(["1001"]);

    // 2. Same data again: nothing written, nothing sent.
    sent.length = 0;
    res = await poll();
    expect(res.changed).toBe(0);
    expect(sent).toHaveLength(0);

    // 3. A goal at 23': both clubs, both "all" and "key" topics, high priority, stored event and brief facts.
    liveResponse = [rawFixture({ ts: nowSec, short: "1H", elapsed: 23, goals: [1, 0], events: [goalEvent(23, 10, 7)] })];
    res = await poll();
    expect(sent).toHaveLength(4);
    expect(sent.map((m) => m.topic).sort()).toEqual(["team_10_all", "team_10_key", "team_20_all", "team_20_key"]);
    expect(sent.every((m) => m.android.priority === "high")).toBe(true);
    expect(sent[0].data).toMatchObject({ type: "goal", score: "1-0", minute: "23", scorer: "Ellis" });
    expect((await db().collection("fixtures/1001/events").get()).size).toBe(1);
    const facts = (await db().doc("fixtures/1001/meta/briefFacts").get()).data();
    expect(facts).toMatchObject({ homeScore: 1, awayScore: 0, status: "live" });

    // 4. A red card for the away side.
    sent.length = 0;
    liveResponse = [rawFixture({ ts: nowSec, short: "1H", elapsed: 30, goals: [1, 0], events: [goalEvent(23, 10, 7), redCardEvent(30, 20, 3)] })];
    await poll();
    expect(sent).toHaveLength(4);
    expect(sent[0].data.type).toBe("redCard");

    // 5. The match drops out of the live list. It is fetched by id and found finished: full time alert to all three levels.
    sent.length = 0;
    liveResponse = [];
    res = await poll();
    expect(api.calls.some((c) => c.params.id === "1001")).toBe(true);
    expect(sent).toHaveLength(6);
    expect(sent.map((m) => m.topic)).toContain("team_10_final");
    expect(sent[0].data.type).toBe("fullTime");
    expect(((await db().doc("fixtures/1001").get()).data() as FixtureDoc).status).toBe("finished");
    expect((await db().doc("liveIndex/current").get()).data()?.ids).toEqual([]);
    expect((await db().doc("ops/standingsDirty").get()).data()?.leagues).toHaveProperty("39");
  });

  it("never sends the same alert twice, even if the fixture write is replayed", async () => {
    await seedUpcoming();
    const { deps, sent } = capturedAlerts(db());
    const api = fakeApi(() => [rawFixture({ ts: nowSec, short: "1H", elapsed: 23, goals: [1, 0], events: [goalEvent(23, 10, 7)] })]);
    await runLivePoll({ db: db(), api, budget: budget(), alerts: deps, now: NOW });
    expect(sent).toHaveLength(4);
    // Simulate a crash after sending but before the fixture was saved: the same goal is processed again.
    await seedUpcoming();
    sent.length = 0;
    await runLivePoll({ db: db(), api, budget: budget(), alerts: deps, now: NOW });
    expect(sent).toHaveLength(0);
  });

  it("includes the table impact in the brief facts when standings exist", async () => {
    await seedUpcoming();
    await db().doc("standings/39_2026").set({
      rows: [
        { rank: 1, teamId: 30, teamName: "C", teamLogo: "", played: 9, won: 7, drawn: 1, lost: 1, goalsFor: 25, goalsAgainst: 11, goalDiff: 14, points: 22, form: "" },
        { rank: 2, teamId: 20, teamName: "Riverside", teamLogo: "", played: 9, won: 6, drawn: 2, lost: 1, goalsFor: 22, goalsAgainst: 13, goalDiff: 9, points: 20, form: "" },
        { rank: 3, teamId: 10, teamName: "Northgate", teamLogo: "", played: 9, won: 6, drawn: 1, lost: 2, goalsFor: 20, goalsAgainst: 13, goalDiff: 7, points: 19, form: "" },
      ],
    });
    const { deps } = capturedAlerts(db());
    const api = fakeApi(() => [rawFixture({ ts: nowSec, short: "2H", elapsed: 74, goals: [2, 1], events: [goalEvent(23, 10, 7)] })]);
    await runLivePoll({ db: db(), api, budget: budget(), alerts: deps, now: NOW });
    const facts = (await db().doc("fixtures/1001/meta/briefFacts").get()).data();
    expect(facts?.impact.before).toEqual({ homeRank: 3, awayRank: 2 });
    expect(facts?.impact.after.homePoints).toBe(22);
    expect(facts?.impact.after.homeRank).toBe(2);
  });
});
