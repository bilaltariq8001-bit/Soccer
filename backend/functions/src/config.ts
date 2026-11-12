/** Deployment region. Keep it close to your Firestore location. */
export const REGION = "europe-west1";

/** Direct API-Football base URL. Verify the auth header name in your dashboard (x-apisports-key vs x-rapidapi-key). */
export const API_BASE = "https://v3.football.api-sports.io";
export const API_KEY_HEADER = "x-apisports-key";

/** Competitions we serve. Ids are API-Football league ids. Verify each one in the dashboard before launch. */
export const TRACKED_LEAGUES: ReadonlyArray<{ id: number; name: string }> = [
  { id: 39, name: "Premier League" },
  { id: 140, name: "La Liga" },
  { id: 135, name: "Serie A" },
  { id: 78, name: "Bundesliga" },
  { id: 61, name: "Ligue 1" },
  { id: 2, name: "UEFA Champions League" },
  { id: 3, name: "UEFA Europa League" },
  { id: 253, name: "Major League Soccer" },
];

export const TRACKED_IDS: ReadonlySet<number> = new Set(TRACKED_LEAGUES.map((l) => l.id));

/** European-style season start: July onwards belongs to the season that started that year. */
export function currentSeason(now: Date = new Date()): number {
  return now.getUTCMonth() >= 6 ? now.getUTCFullYear() : now.getUTCFullYear() - 1;
}

/** yyyy-MM-dd in UTC. */
export function dateKey(ms: number): string {
  return new Date(ms).toISOString().slice(0, 10);
}

export const LIVE_WINDOW_BEFORE_MS = 3 * 60 * 60 * 1000;
export const LIVE_WINDOW_AFTER_MS = 10 * 60 * 1000;
export const DETAIL_REFRESH_TTL_MS = 45 * 1000;
export const FIXTURE_RETENTION_DAYS = 14;
export const MAX_MISSING_REFETCH = 5;
