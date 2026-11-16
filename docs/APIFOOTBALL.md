# apifootball.com integration

Set `apifootballKey=<your key>` in your **user** Gradle properties (`~/.gradle/gradle.properties` or `$GRADLE_USER_HOME/gradle.properties`) or pass `-PapifootballKey=...`. Never commit the key. When it is set, `AppContainer` uses `ApiFootballComApi` and the app shows no mock data; news stays on the 90min RSS feeds.

Code: `data/remote/ApiFootballComApi.kt` (client, cache, divisions), `data/remote/ApiFootballParse.kt` (pure JSON to DTO parsing), `data/remote/LiveScoreSocket.kt` (livescore WebSocket). Tests: `ApiFootballTest` with trimmed real responses in `src/test/resources/afl/`.

## Endpoints used

| Doc section | Action | Used for |
|---|---|---|
| Countries | `get_countries` | Order and group competitions by country on the Leagues tab |
| Competitions | `get_leagues` | The competition list (coverage follows the plan automatically) |
| Teams | `get_teams&team_id=` | Team page: crest, venue, coach, founded, squad |
| Players | `get_players&player_id=` / `player_name=` | Player page; player search |
| Standings | `get_standings` | Tables (and splitting multi-division leagues) |
| Events | `get_events` (`from/to`, `league_id`, `team_id`, `match_id`, `match_live=1`) | Fixtures, results, live scores, goals, cards, subs, inline lineups and stats, form, scorer fallback |
| Lineups | `get_lineups` | Fallback when the event has no lineup yet |
| Statistics | `get_statistics` | Fallback when the event has no stats yet |
| Odds | `get_odds&match_id=` | Pre-match 1X2 on Match detail (bet365 preferred, else first bookmaker with all three prices) |
| Live odds and comments | `get_live_odds_commnets` (sic) | Commentary tab and live 1X2 on live matches |
| Head to head | `get_H2H` | "Head to head" section on Match detail |
| Predictions | `get_predictions` | Win probability on Match detail and the 1/X/2 bar on upcoming match rows |
| Top scorers | `get_topscorers` | Top scorers; when the plan lacks it, goals are counted from the season's `get_events` |
| Videos | `get_videos&match_id=` | "Highlights" rows on finished matches, only when clips exist |
| Livescore WebSockets | `wss://wss.apifootball.com/livescore?APIkey=…&timezone=+00:00` | Push updates to Home, Matches and Match detail while the app is in the foreground; 30 s polling stays as fallback; reconnect backoff 2 s to 60 s |
| Football News | `get_news` | **Not used** by decision: news stays on 90min RSS |
| Widgets / WordPress | – | Web embeds; not applicable to a native app |

## Behaviour
- Every request sends `timezone=UTC`; times are converted on device. Note: the API caches responses server side and that cache can ignore a changed `timezone` for the same URL, so always send the same value.
- Error objects (`{"error":404,"message":"No event found..."}`) on list endpoints mean "nothing" (empty list). `1005` (method not on plan) and a bad key become `AppError.Forbidden`; optional extras (odds, predictions, H2H, videos, commentary) then simply don't show. Gated and empty answers are cached too, so they cost one call per cache period.
- In-memory cache per URL: live 30 s, day fixtures 1 min (10 min for other days), league windows 5 min, standings 10 min, predictions/odds/scorers 30 min, teams/players/leagues 1 h, countries 1 day. Concurrent identical requests share one network call. Rate limit is 180 calls/hour per endpoint.
- Leagues that put several divisions under one id (England "Non League Premier") are split into one competition per division with ids `<leagueId>.<stageKey>`, matched to standings groups by team membership.
- Form (last five) is computed from finished results in the league window; Ask answers from live tables and scorers only.

## Rich UI from live data (0.5.0)
- **Images**: `team_badge` / `team_home_badge` / `team_away_badge` → `Team.logo`; `league_logo` / `country_logo` → `League.logo` / `countryLogo` and `Match.leagueLogo`; `player_image` → `PlayerSummary.photo`, `SquadPlayer.photo`, `Scorer.photo`, `LineupPlayer.photo`. Rendered with Coil (`RemoteImage`) in `TeamCrest`, `PlayerAvatar` and `LeagueBadge`; initials / short code are the loading and missing fallback.
- **Squads per league**: `get_teams&league_id=` (one call per league per hour) gives player positions, numbers, nationality and photos for top scorers, top players, lineups and player pages.
- **Lineups**: with no published formation (`match_*team_system` blank, `lineup_position` 0) starters are placed by squad position when the squad list knows at least 9 of 11 and there is one goalkeeper (`ApiFootballParse.squadShape`), marked `shapeFromSquad`; otherwise the lineup is listed by role and the pitch view is not offered.
- **Player season**: when `get_players` leaves season numbers blank (smaller plans), apps, starts, minutes, goals, assists, cards and the last six games are counted from the team's season `get_events` (`ApiFootballParse.playerSeason`).
- **Recent results**: `get_H2H` `firstTeam_lastResults` / `secondTeam_lastResults` feed the match page form and "Recent results".
- **Match insight**: `data/insight/MatchBrief` writes a 2–4 sentence brief from the table, form, published prediction, head-to-head and goal events; empty when there is too little data (no filler).
- **Stats**: 0–0 rows the feed doesn't track (throw-ins, free kicks...) are dropped; card rows are kept.
