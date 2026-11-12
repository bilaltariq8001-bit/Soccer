import { describe, expect, it } from "vitest";
import { sendAlert, topicsFor, buildPayload, isHighPriority } from "../src/alerts";
import type { AlertMessage } from "../src/alerts";
import { createApiClient, ApiError } from "../src/apiFootball";
import { buildFacts, standingsIfResultHolds } from "../src/brief";
import { labelOf, normalizeEvent, normalizeFixture, normalizeStats, statusOf } from "../src/normalize";
import { modeFor, usageKey } from "../src/quota";
import type { RawFixture, RawEvent } from "../src/types";

const rawFixture = (over: Partial<RawFixture> = {}): RawFixture => ({
  fixture: { id: 1001, timestamp: 1_793_273_000, venue: { name: "Northgate Park", city: "Northgate" }, status: { short: "2H", elapsed: 74, extra: null } },
  league: { id: 39, name: "Premier League", logo: "l.png", season: 2026, round: "Regular Season - 9" },
  teams: { home: { id: 10, name: "Northgate", logo: "n.png" }, away: { id: 20, name: "Riverside", logo: "r.png" } },
  goals: { home: 2, away: 1 },
  events: [],
  ...over,
});

const goal = (minute: number, teamId: number, playerId: number, detail = "Normal Goal"): RawEvent => ({
  time: { elapsed: minute, extra: null }, team: { id: teamId, name: "x", logo: "" },
  player: { id: playerId, name: "P" + playerId }, assist: { id: null, name: null }, type: "Goal", detail,
});

describe("status and labels", () => {
  it("maps provider codes", () => {
    expect(statusOf("1H")).toBe("live");
    expect(statusOf("HT")).toBe("live");
    expect(statusOf("FT")).toBe("finished");
    expect(statusOf("PEN")).toBe("finished");
    expect(statusOf("PST")).toBe("postponed");
    expect(statusOf("NS")).toBe("upcoming");
  });
  it("builds labels", () => {
    expect(labelOf("live", "2H", 74, null)).toBe("74'");
    expect(labelOf("live", "2H", 90, 3)).toBe("90+3'");
    expect(labelOf("live", "HT", 45, null)).toBe("HT");
    expect(labelOf("finished", "FT", 90, null)).toBe("FT");
    expect(labelOf("postponed", "PST", null, null)).toBe("Postponed");
  });
});

describe("events", () => {
  it("keeps goals, ignores missed penalties and VAR", () => {
    expect(normalizeEvent(goal(61, 10, 7))?.kind).toBe("goal");
    expect(normalizeEvent(goal(61, 10, 7, "Missed Penalty"))).toBeNull();
    expect(normalizeEvent({ ...goal(1, 10, 7), type: "Var", detail: "Goal cancelled" })).toBeNull();
  });
  it("tells red from yellow cards", () => {
    expect(normalizeEvent({ ...goal(55, 20, 3), type: "Card", detail: "Yellow Card" })?.kind).toBe("yellowCard");
    expect(normalizeEvent({ ...goal(55, 20, 3), type: "Card", detail: "Red Card" })?.kind).toBe("redCard");
    expect(normalizeEvent({ ...goal(55, 20, 3), type: "Card", detail: "Second Yellow card" })?.kind).toBe("redCard");
  });
  it("gives stable keys", () => {
    expect(normalizeEvent(goal(61, 10, 7))?.key).toBe(normalizeEvent(goal(61, 10, 7))?.key);
    expect(normalizeEvent(goal(61, 10, 7))?.key).not.toBe(normalizeEvent(goal(62, 10, 7))?.key);
  });
});

describe("fixture normalization", () => {
  it("changes the hash when the score changes and not otherwise", () => {
    const a = normalizeFixture(rawFixture());
    const b = normalizeFixture(rawFixture());
    const c = normalizeFixture(rawFixture({ goals: { home: 3, away: 1 } }));
    expect(a.hash).toBe(b.hash);
    expect(a.hash).not.toBe(c.hash);
    expect(a.status).toBe("live");
    expect(a.minute).toBe(74);
    expect(a.dateKey).toMatch(/^\d{4}-\d{2}-\d{2}$/);
  });
});

describe("stats", () => {
  it("maps provider names and percentages", () => {
    const rows = normalizeStats([
      { team: { id: 10, name: "N", logo: "" }, statistics: [{ type: "Ball Possession", value: "54%" }, { type: "Total Shots", value: 11 }] },
      { team: { id: 20, name: "R", logo: "" }, statistics: [{ type: "Ball Possession", value: "46%" }, { type: "Total Shots", value: 14 }] },
    ], 10);
    const poss = rows.find((r) => r.label === "Possession")!;
    expect(poss.home).toBe(54);
    expect(poss.homeText).toBe("54%");
    expect(rows.find((r) => r.label === "Shots")!.away).toBe(14);
  });
});

describe("alerts", () => {
  it("routes kinds to levels", () => {
    expect(topicsFor(10, "goal")).toEqual(["team_10_all", "team_10_key"]);
    expect(topicsFor(10, "yellowCard")).toEqual(["team_10_all"]);
    expect(topicsFor(10, "fullTime")).toEqual(["team_10_all", "team_10_key", "team_10_final"]);
    expect(isHighPriority("goal")).toBe(true);
    expect(isHighPriority("kickoff")).toBe(false);
  });
  it("sends once per topic and dedupes repeats", async () => {
    const claimed = new Set<string>();
    const sent: AlertMessage[] = [];
    const deps = {
      claim: async (id: string) => { if (claimed.has(id)) return false; claimed.add(id); return true; },
      send: async (m: AlertMessage) => { sent.push(m); },
    };
    const input = {
      kind: "goal" as const, fixtureId: "1001", eventKey: "61-goal-10-7", home: "Northgate", away: "Riverside",
      homeScore: 2, awayScore: 1, minute: 61, scorer: "Ellis", homeTeamId: 10, awayTeamId: 20, tsMs: 1_793_273_000_000,
    };
    expect(await sendAlert(deps, input)).toBe(4);
    expect(await sendAlert(deps, input)).toBe(0);
    expect(sent).toHaveLength(4);
    expect(sent[0].data).toMatchObject({ v: "1", type: "goal", matchId: "1001", score: "2-1", minute: "61", scorer: "Ellis" });
    expect(Object.values(buildPayload(input, 10)).every((v) => typeof v === "string")).toBe(true);
  });
});

describe("brief facts", () => {
  const table = [
    { teamId: 10, points: 19, goalDiff: 7, goalsFor: 20 },
    { teamId: 20, points: 20, goalDiff: 9, goalsFor: 22 },
    { teamId: 30, points: 22, goalDiff: 14, goalsFor: 25 },
  ];
  it("moves clubs up when the current result would hold", () => {
    const r = standingsIfResultHolds(table, 10, 20, 2, 1)!;
    expect(r.before).toEqual({ homeRank: 3, awayRank: 2 });
    expect(r.after.homePoints).toBe(22);
    expect(r.after.awayPoints).toBe(20);
    expect(r.after.homeRank).toBe(2); // 22 points, GD +8 vs 30 on 22 points, GD +14
    expect(r.after.awayRank).toBe(3);
  });
  it("returns null for clubs missing from the table", () => {
    expect(standingsIfResultHolds(table, 99, 20, 1, 0)).toBeNull();
  });
  it("builds a stable hash that changes with the score", () => {
    const f = normalizeFixture(rawFixture());
    const a = buildFacts(f, [], null);
    const b = buildFacts(f, [], null);
    const c = buildFacts({ ...f, homeScore: 3 }, [], null);
    expect(a.hash).toBe(b.hash);
    expect(a.hash).not.toBe(c.hash);
  });
});

describe("budget", () => {
  it("degrades at 80% and stops at 95%", () => {
    expect(modeFor(10, 100)).toBe("normal");
    expect(modeFor(80, 100)).toBe("degraded");
    expect(modeFor(95, 100)).toBe("stop");
  });
  it("uses a UTC day key", () => {
    expect(usageKey(new Date("2026-10-29T23:59:00Z"))).toBe("20261029");
  });
});

describe("api client", () => {
  const ok = (body: unknown) => ({ ok: true, status: 200, json: async () => body }) as unknown as Response;
  const status = (s: number) => ({ ok: false, status: s, json: async () => ({}) }) as unknown as Response;
  const budget = { reserve: async () => true };

  it("returns the response array and sends the key header", async () => {
    let header = "";
    const client = createApiClient("KEY", budget, (async (_u: string, init?: RequestInit) => {
      header = (init?.headers as Record<string, string>)["x-apisports-key"];
      return ok({ errors: [], response: [{ a: 1 }] });
    }) as typeof fetch, 1);
    expect(await client.get("/fixtures", { live: "all" })).toEqual([{ a: 1 }]);
    expect(header).toBe("KEY");
  });
  it("retries 429 then succeeds", async () => {
    let calls = 0;
    const client = createApiClient("K", budget, (async () => (++calls < 3 ? status(429) : ok({ response: [1] }))) as typeof fetch, 1);
    expect(await client.get("/x", {})).toEqual([1]);
    expect(calls).toBe(3);
  });
  it("throws on an errors object inside a 200", async () => {
    const client = createApiClient("K", budget, (async () => ok({ errors: { rateLimit: "Too many requests" }, response: [] })) as typeof fetch, 1);
    await expect(client.get("/x", {})).rejects.toBeInstanceOf(ApiError);
  });
  it("does not call the API when the budget is exhausted", async () => {
    let called = false;
    const client = createApiClient("K", { reserve: async () => false }, (async () => { called = true; return ok({}); }) as typeof fetch, 1);
    await expect(client.get("/x", {})).rejects.toBeInstanceOf(ApiError);
    expect(called).toBe(false);
  });
});
