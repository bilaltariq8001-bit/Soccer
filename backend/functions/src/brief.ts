import { createHash } from "node:crypto";
import type { EventDoc, FixtureDoc, StandingRow } from "./types";

export interface RankInput {
  teamId: number;
  points: number;
  goalDiff: number;
  goalsFor: number;
}

function rankOf(rows: RankInput[], teamId: number): number {
  const sorted = [...rows].sort((a, b) => b.points - a.points || b.goalDiff - a.goalDiff || b.goalsFor - a.goalsFor || a.teamId - b.teamId);
  return sorted.findIndex((r) => r.teamId === teamId) + 1;
}

export interface ImpactResult {
  before: { homeRank: number; awayRank: number };
  after: { homeRank: number; awayRank: number; homePoints: number; awayPoints: number };
}

/**
 * Where both clubs would sit if the current score were the final result.
 * The table is assumed not to include this match yet (true while it is being played).
 */
export function standingsIfResultHolds(rows: RankInput[], homeId: number, awayId: number, homeScore: number, awayScore: number): ImpactResult | null {
  if (!rows.some((r) => r.teamId === homeId) || !rows.some((r) => r.teamId === awayId)) return null;
  const before = { homeRank: rankOf(rows, homeId), awayRank: rankOf(rows, awayId) };
  const after = rows.map((r) => {
    if (r.teamId === homeId) {
      const pts = homeScore > awayScore ? 3 : homeScore === awayScore ? 1 : 0;
      return { ...r, points: r.points + pts, goalDiff: r.goalDiff + homeScore - awayScore, goalsFor: r.goalsFor + homeScore };
    }
    if (r.teamId === awayId) {
      const pts = awayScore > homeScore ? 3 : homeScore === awayScore ? 1 : 0;
      return { ...r, points: r.points + pts, goalDiff: r.goalDiff + awayScore - homeScore, goalsFor: r.goalsFor + awayScore };
    }
    return r;
  });
  const h = after.find((r) => r.teamId === homeId)!;
  const a = after.find((r) => r.teamId === awayId)!;
  return { before, after: { homeRank: rankOf(after, homeId), awayRank: rankOf(after, awayId), homePoints: h.points, awayPoints: a.points } };
}

export interface BriefFacts {
  fixtureId: string;
  status: string;
  minute: number | null;
  homeName: string;
  awayName: string;
  homeScore: number | null;
  awayScore: number | null;
  recent: Array<{ minute: number; kind: string; player: string; teamId: number }>;
  impact: ImpactResult | null;
  hash: string;
}

/** Deterministic facts for the Match Brief. The client phrases them with Firebase AI Logic and must not add numbers. */
export function buildFacts(fixture: FixtureDoc, events: EventDoc[], table: StandingRow[] | null): BriefFacts {
  const recent = [...events]
    .filter((e) => e.kind === "goal" || e.kind === "redCard")
    .sort((a, b) => b.minute - a.minute)
    .slice(0, 3)
    .map((e) => ({ minute: e.minute, kind: e.kind, player: e.playerName, teamId: e.teamId }));
  const hasScore = fixture.homeScore != null && fixture.awayScore != null;
  const impact = table && hasScore && fixture.status === "live"
    ? standingsIfResultHolds(
      table.map((r) => ({ teamId: r.teamId, points: r.points, goalDiff: r.goalDiff, goalsFor: r.goalsFor })),
      fixture.home.id, fixture.away.id, fixture.homeScore!, fixture.awayScore!,
    )
    : null;
  const body = {
    fixtureId: fixture.id, status: fixture.status, minute: fixture.minute,
    homeName: fixture.home.name, awayName: fixture.away.name,
    homeScore: fixture.homeScore, awayScore: fixture.awayScore, recent, impact,
  };
  const hash = createHash("md5").update(JSON.stringify(body)).digest("hex").slice(0, 16);
  return { ...body, hash };
}
