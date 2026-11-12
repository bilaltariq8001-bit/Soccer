export type Status = "upcoming" | "live" | "finished" | "postponed";

export interface TeamRef {
  id: number;
  name: string;
  logo: string;
}

/** Normalized match summary stored at fixtures/{id}. */
export interface FixtureDoc {
  id: string;
  leagueId: number;
  leagueName: string;
  leagueLogo: string;
  season: number;
  round: string;
  dateKey: string;
  kickoffMs: number;
  status: Status;
  statusShort: string;
  minute: number | null;
  label: string;
  venue: string;
  home: TeamRef;
  away: TeamRef;
  homeScore: number | null;
  awayScore: number | null;
  eventKeys: string[];
  hash: string;
}

export type EventKind = "goal" | "redCard" | "yellowCard" | "sub";

export interface EventDoc {
  key: string;
  kind: EventKind;
  minute: number;
  extra: number | null;
  teamId: number;
  playerName: string;
  assistName: string | null;
  detail: string;
}

export interface StandingRow {
  rank: number;
  teamId: number;
  teamName: string;
  teamLogo: string;
  played: number;
  won: number;
  drawn: number;
  lost: number;
  goalsFor: number;
  goalsAgainst: number;
  goalDiff: number;
  points: number;
  form: string;
}

export interface StatLine {
  label: string;
  home: number;
  away: number;
  homeText: string;
  awayText: string;
}

// ---- Raw API-Football shapes (only the fields we read) ----

export interface RawTeam {
  id: number;
  name: string;
  logo: string;
}

export interface RawEvent {
  time: { elapsed: number | null; extra: number | null };
  team: RawTeam;
  player: { id: number | null; name: string | null };
  assist: { id: number | null; name: string | null };
  type: string;
  detail: string;
}

export interface RawFixture {
  fixture: {
    id: number;
    timestamp: number;
    venue: { name: string | null; city: string | null };
    status: { short: string; elapsed: number | null; extra?: number | null };
  };
  league: { id: number; name: string; logo: string; season: number; round: string };
  teams: { home: RawTeam; away: RawTeam };
  goals: { home: number | null; away: number | null };
  events?: RawEvent[];
}

export interface RawStandingRow {
  rank: number;
  team: RawTeam;
  points: number;
  goalsDiff: number;
  form: string | null;
  all: { played: number; win: number; draw: number; lose: number; goals: { for: number; against: number } };
}

export interface RawStandings {
  league: { id: number; season: number; standings: RawStandingRow[][] };
}

export interface RawTopScorer {
  player: { id: number; name: string; photo: string };
  statistics: Array<{ team: RawTeam; goals: { total: number | null } }>;
}

export interface RawStatsTeam {
  team: RawTeam;
  statistics: Array<{ type: string; value: number | string | null }>;
}

export interface RawLineupPlayer {
  player: { id: number; name: string; number: number | null; pos: string | null; grid: string | null };
}

export interface RawLineup {
  team: RawTeam;
  coach: { name: string | null };
  formation: string | null;
  startXI: RawLineupPlayer[];
  substitutes: RawLineupPlayer[];
}
