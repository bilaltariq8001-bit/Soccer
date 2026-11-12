export type AlertKind = "goal" | "redCard" | "yellowCard" | "kickoff" | "fullTime";
export type Level = "all" | "key" | "final";

/** Which alert levels receive which kind of event. Mirrors the in-app alert choices. */
export function levelsFor(kind: AlertKind): Level[] {
  switch (kind) {
    case "goal":
    case "redCard":
      return ["all", "key"];
    case "fullTime":
      return ["all", "key", "final"];
    default:
      return ["all"];
  }
}

export function topicsFor(teamId: number, kind: AlertKind): string[] {
  return levelsFor(kind).map((level) => `team_${teamId}_${level}`);
}

/** Goals, red cards and full time wake the phone. Everything else is normal priority. */
export function isHighPriority(kind: AlertKind): boolean {
  return kind === "goal" || kind === "redCard" || kind === "fullTime";
}

export interface AlertInput {
  kind: AlertKind;
  fixtureId: string;
  eventKey: string;
  home: string;
  away: string;
  homeScore: number | null;
  awayScore: number | null;
  minute: number | null;
  scorer: string;
  homeTeamId: number;
  awayTeamId: number;
  tsMs: number;
}

/** Data-only payload: facts, never display text. The app writes the sentence in the user's language. All values are strings. */
export function buildPayload(input: AlertInput, teamId: number): Record<string, string> {
  return {
    v: "1",
    type: input.kind,
    matchId: input.fixtureId,
    teamId: String(teamId),
    home: input.home,
    away: input.away,
    score: `${input.homeScore ?? 0}-${input.awayScore ?? 0}`,
    minute: input.minute == null ? "" : String(input.minute),
    scorer: input.scorer,
    ts: String(Math.floor(input.tsMs / 1000)),
  };
}

export interface AlertMessage {
  topic: string;
  data: Record<string, string>;
  android: { priority: "high" | "normal" };
}

export interface AlertDeps {
  /** Returns true if this alert id was not sent before (and records it), false if it is a duplicate. */
  claim(id: string): Promise<boolean>;
  send(message: AlertMessage): Promise<void>;
}

/** Sends one alert to the topics of both clubs, once per (event, topic). Returns the number of messages sent. */
export async function sendAlert(deps: AlertDeps, input: AlertInput): Promise<number> {
  let sent = 0;
  for (const teamId of [input.homeTeamId, input.awayTeamId]) {
    for (const topic of topicsFor(teamId, input.kind)) {
      const id = `${input.fixtureId}_${input.eventKey}_${topic}`;
      if (!(await deps.claim(id))) continue;
      await deps.send({
        topic,
        data: buildPayload(input, teamId),
        android: { priority: isHighPriority(input.kind) ? "high" : "normal" },
      });
      sent++;
    }
  }
  return sent;
}
