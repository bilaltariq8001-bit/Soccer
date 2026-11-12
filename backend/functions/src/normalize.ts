import { createHash } from "node:crypto";
import { dateKey } from "./config";
import type {
  EventDoc, EventKind, FixtureDoc, RawEvent, RawFixture, RawLineup, RawStandingRow, RawStatsTeam, StandingRow, StatLine, Status,
} from "./types";

const LIVE = new Set(["1H", "2H", "HT", "ET", "BT", "P", "LIVE", "INT", "SUSP"]);
const FINISHED = new Set(["FT", "AET", "PEN", "AWD", "WO"]);
const POSTPONED = new Set(["PST", "CANC", "ABD"]);

export function statusOf(short: string): Status {
  if (LIVE.has(short)) return "live";
  if (FINISHED.has(short)) return "finished";
  if (POSTPONED.has(short)) return "postponed";
  return "upcoming";
}

export function labelOf(status: Status, short: string, elapsed: number | null, extra: number | null): string {
  switch (status) {
    case "live":
      if (short === "HT") return "HT";
      return elapsed == null ? "LIVE" : `${elapsed}${extra ? `+${extra}` : ""}'`;
    case "finished":
      return "FT";
    case "postponed":
      return "Postponed";
    default:
      return "";
  }
}

function slug(s: string): string {
  return s.toLowerCase().replace(/[^a-z0-9]+/g, "-").replace(/^-|-$/g, "");
}

/** Returns null for events we do not store (VAR, missed penalties). */
export function normalizeEvent(e: RawEvent): EventDoc | null {
  let kind: EventKind;
  if (e.type === "Goal") {
    if (e.detail === "Missed Penalty") return null;
    kind = "goal";
  } else if (e.type === "Card") {
    const d = e.detail.toLowerCase();
    if (d.includes("red") || d.includes("second yellow")) kind = "redCard";
    else kind = "yellowCard";
  } else if (e.type === "subst") {
    kind = "sub";
  } else {
    return null;
  }
  const minute = e.time.elapsed ?? 0;
  const extra = e.time.extra ?? null;
  const key = `${minute}${extra ? `p${extra}` : ""}-${kind}-${e.team.id}-${e.player.id ?? 0}-${slug(e.detail)}`;
  return {
    key, kind, minute, extra, teamId: e.team.id,
    playerName: e.player.name ?? "", assistName: e.assist?.name ?? null, detail: e.detail,
  };
}

export function normalizeEvents(raw: RawEvent[] | undefined): EventDoc[] {
  const out = new Map<string, EventDoc>();
  for (const e of raw ?? []) {
    const doc = normalizeEvent(e);
    if (doc) out.set(doc.key, doc);
  }
  return [...out.values()];
}

export function normalizeFixture(raw: RawFixture, events: EventDoc[] = normalizeEvents(raw.events)): FixtureDoc {
  const short = raw.fixture.status.short;
  const status = statusOf(short);
  const elapsed = raw.fixture.status.elapsed ?? null;
  const extra = raw.fixture.status.extra ?? null;
  const kickoffMs = raw.fixture.timestamp * 1000;
  const eventKeys = events.map((e) => e.key);
  const venue = [raw.fixture.venue.name, raw.fixture.venue.city].filter(Boolean).join(", ");
  const base = {
    id: String(raw.fixture.id),
    leagueId: raw.league.id, leagueName: raw.league.name, leagueLogo: raw.league.logo,
    season: raw.league.season, round: raw.league.round,
    dateKey: dateKey(kickoffMs), kickoffMs, status, statusShort: short,
    minute: status === "live" ? elapsed : null,
    label: labelOf(status, short, elapsed, extra),
    venue,
    home: { id: raw.teams.home.id, name: raw.teams.home.name, logo: raw.teams.home.logo },
    away: { id: raw.teams.away.id, name: raw.teams.away.name, logo: raw.teams.away.logo },
    homeScore: raw.goals.home, awayScore: raw.goals.away,
    eventKeys,
  };
  const hash = createHash("md5")
    .update(`${status}|${short}|${base.minute}|${base.homeScore}|${base.awayScore}|${eventKeys.join(",")}`)
    .digest("hex")
    .slice(0, 16);
  return { ...base, hash };
}

export function normalizeStandings(rows: RawStandingRow[]): StandingRow[] {
  return rows.map((r) => ({
    rank: r.rank, teamId: r.team.id, teamName: r.team.name, teamLogo: r.team.logo,
    played: r.all.played, won: r.all.win, drawn: r.all.draw, lost: r.all.lose,
    goalsFor: r.all.goals.for, goalsAgainst: r.all.goals.against, goalDiff: r.goalsDiff,
    points: r.points, form: r.form ?? "",
  }));
}

const STAT_LABELS: Array<[string, string]> = [
  ["Ball Possession", "Possession"], ["Total Shots", "Shots"], ["Shots on Goal", "On target"],
  ["Total passes", "Passes"], ["Fouls", "Fouls"], ["Corner Kicks", "Corners"],
];

function toNumber(v: number | string | null | undefined): number {
  if (v == null) return 0;
  if (typeof v === "number") return v;
  const n = parseFloat(v.replace("%", ""));
  return Number.isFinite(n) ? n : 0;
}

export function normalizeStats(raw: RawStatsTeam[], homeId: number): StatLine[] {
  const home = raw.find((t) => t.team.id === homeId);
  const away = raw.find((t) => t.team.id !== homeId);
  if (!home || !away) return [];
  const find = (t: RawStatsTeam, type: string) => t.statistics.find((s) => s.type === type)?.value;
  return STAT_LABELS.map(([type, label]) => {
    const h = find(home, type);
    const a = find(away, type);
    const pct = type === "Ball Possession";
    return {
      label, home: toNumber(h), away: toNumber(a),
      homeText: pct ? `${toNumber(h)}%` : String(toNumber(h)), awayText: pct ? `${toNumber(a)}%` : String(toNumber(a)),
    };
  });
}

export function normalizeLineup(raw: RawLineup) {
  const pick = (p: { player: { id: number; name: string; number: number | null; pos: string | null; grid: string | null } }) => ({
    id: p.player.id, name: p.player.name, number: p.player.number ?? 0, pos: p.player.pos ?? "", grid: p.player.grid ?? "",
  });
  return {
    teamId: raw.team.id, formation: raw.formation ?? "", coach: raw.coach.name ?? "",
    starters: raw.startXI.map(pick), bench: raw.substitutes.map(pick),
  };
}
