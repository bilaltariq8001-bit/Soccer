package com.innovativeinside.pitchside.data.remote

import com.innovativeinside.pitchside.data.model.CareerStop
import com.innovativeinside.pitchside.data.model.EventKind
import com.innovativeinside.pitchside.data.model.League
import com.innovativeinside.pitchside.data.model.Lineup
import com.innovativeinside.pitchside.data.model.LineupPlayer
import com.innovativeinside.pitchside.data.model.Match
import com.innovativeinside.pitchside.data.model.MatchEvent
import com.innovativeinside.pitchside.data.model.MatchLineups
import com.innovativeinside.pitchside.data.model.NewsCategory
import com.innovativeinside.pitchside.data.model.NewsItem
import com.innovativeinside.pitchside.data.model.PlayerMatch
import com.innovativeinside.pitchside.data.model.PlayerProfile
import com.innovativeinside.pitchside.data.model.PlayerSummary
import com.innovativeinside.pitchside.data.model.Position
import com.innovativeinside.pitchside.data.model.Scorer
import com.innovativeinside.pitchside.data.model.SearchResults
import com.innovativeinside.pitchside.data.model.SquadPlayer
import com.innovativeinside.pitchside.data.model.StandingRow
import com.innovativeinside.pitchside.data.model.StatRow
import com.innovativeinside.pitchside.data.model.Status
import com.innovativeinside.pitchside.data.model.Team
import com.innovativeinside.pitchside.data.model.TeamDetail
import com.innovativeinside.pitchside.data.remote.dto.CareerStopDto
import com.innovativeinside.pitchside.data.remote.dto.EventDto
import com.innovativeinside.pitchside.data.remote.dto.FixtureDto
import com.innovativeinside.pitchside.data.remote.dto.LeagueDto
import com.innovativeinside.pitchside.data.remote.dto.LineupDto
import com.innovativeinside.pitchside.data.remote.dto.LineupsDto
import com.innovativeinside.pitchside.data.remote.dto.NewsDto
import com.innovativeinside.pitchside.data.remote.dto.PlayerDto
import com.innovativeinside.pitchside.data.remote.dto.PlayerMatchDto
import com.innovativeinside.pitchside.data.remote.dto.PlayerSummaryDto
import com.innovativeinside.pitchside.data.remote.dto.ScorerDto
import com.innovativeinside.pitchside.data.remote.dto.SearchDto
import com.innovativeinside.pitchside.data.remote.dto.SquadPlayerDto
import com.innovativeinside.pitchside.data.remote.dto.StandingRowDto
import com.innovativeinside.pitchside.data.remote.dto.StatLineDto
import com.innovativeinside.pitchside.data.remote.dto.TeamDto
import com.innovativeinside.pitchside.data.remote.dto.TeamRefDto

/** DTO to domain mappers. Pure functions, unit tested in MappersTest. */
object Mappers {
    private const val FALLBACK_COLOR = 0xFF34495EL

    fun team(d: TeamRefDto): Team = Team(
        id = d.id,
        name = d.name,
        short = d.short?.takeIf { it.isNotBlank() } ?: monogram(d.name),
        color = parseColor(d.color),
        leagueId = d.leagueId.orEmpty(),
    )

    fun monogram(name: String): String {
        val words = name.split(' ', '-').filter { it.isNotBlank() && it.first().isLetter() }
        return when {
            words.size >= 2 -> "${words[0].first()}${words[1].first()}".uppercase()
            words.size == 1 -> words[0].take(2).uppercase()
            else -> "?"
        }
    }

    /** "#RRGGBB" or "#AARRGGBB" to ARGB. Anything else falls back to a neutral slate. */
    fun parseColor(hex: String?): Long {
        val h = hex?.removePrefix("#") ?: return FALLBACK_COLOR
        return when (h.length) {
            6 -> h.toLongOrNull(16)?.let { 0xFF000000L or it } ?: FALLBACK_COLOR
            8 -> h.toLongOrNull(16) ?: FALLBACK_COLOR
            else -> FALLBACK_COLOR
        }
    }

    fun status(s: String): Status = when (s.lowercase()) {
        "live" -> Status.Live
        "finished" -> Status.Finished
        "postponed", "cancelled", "canceled", "suspended" -> Status.Postponed
        else -> Status.Upcoming
    }

    fun match(d: FixtureDto, formatTime: (Long) -> String): Match {
        val st = status(d.status)
        val label = when {
            d.label.isNotBlank() -> d.label
            st == Status.Live && d.statusShort == "HT" -> "HT"
            st == Status.Live && d.minute != null -> "${d.minute}'"
            st == Status.Finished -> "FT"
            st == Status.Postponed -> "Postponed"
            else -> formatTime(d.kickoffMs)
        }
        return Match(
            id = d.id, leagueId = d.leagueId, leagueName = d.leagueName, round = d.round, venue = d.venue,
            referee = d.referee?.takeIf { it.isNotBlank() }, kickoffMs = d.kickoffMs,
            home = team(d.home), away = team(d.away),
            homeScore = if (st == Status.Upcoming) null else d.homeScore,
            awayScore = if (st == Status.Upcoming) null else d.awayScore,
            status = st, label = label, brief = d.brief.orEmpty(),
        )
    }

    fun eventKind(s: String): EventKind? = when (s) {
        "goal" -> EventKind.Goal
        "yellowCard" -> EventKind.YellowCard
        "redCard" -> EventKind.RedCard
        "sub" -> EventKind.Sub
        else -> null
    }

    /** Unknown event kinds are dropped rather than crashing the timeline. Newest first. */
    fun events(list: List<EventDto>, homeTeamId: String): List<MatchEvent> = list.mapNotNull { e ->
        val kind = eventKind(e.kind) ?: return@mapNotNull null
        MatchEvent(
            minute = e.minute, extra = e.extra, isHome = e.teamId == homeTeamId, kind = kind, player = e.playerName,
            detail = e.assistName?.let { "Assist $it" } ?: e.detail,
        )
    }.sortedWith(compareByDescending<MatchEvent> { it.minute }.thenByDescending { it.extra ?: 0 })

    fun stat(d: StatLineDto) = StatRow(d.label, d.home, d.away, d.homeText ?: d.home.toString(), d.awayText ?: d.away.toString())

    /** API-Football grid "row:col" (row 1 = goalkeeper) to pitch fractions; falls back to a spread by index. */
    fun lineup(d: LineupDto, scorers: Set<String> = emptySet()): Lineup {
        val starters = d.startXI
        val rows = starters.mapNotNull { it.grid?.split(':')?.firstOrNull()?.toIntOrNull() }
        val maxRow = (rows.maxOrNull() ?: 4).coerceAtLeast(2)
        val players = starters.mapIndexed { i, p ->
            val grid = p.grid?.split(':')?.mapNotNull { it.toIntOrNull() }
            val (x, y) = if (grid != null && grid.size == 2) {
                val row = grid[0]; val col = grid[1]
                val inRow = starters.count { it.grid?.substringBefore(':')?.toIntOrNull() == row }.coerceAtLeast(1)
                val xf = col.toFloat() / (inRow + 1)
                val yf = 0.9f - (row - 1).toFloat() / (maxRow - 1).toFloat() * 0.72f
                xf to yf
            } else {
                ((i % 4) + 1) / 5f to (0.85f - (i / 4) * 0.3f)
            }
            LineupPlayer(p.id, p.number ?: 0, p.name, p.pos.orEmpty(), x, y, p.name in scorers)
        }
        val bench = d.substitutes.map { LineupPlayer(it.id, it.number ?: 0, it.name, it.pos.orEmpty(), 0f, 0f) }
        return Lineup(d.teamId, d.formation ?: "-", d.coach ?: "Not announced", players, bench)
    }

    fun lineups(d: LineupsDto, homeId: String, awayId: String, scorers: Set<String> = emptySet()): MatchLineups? {
        val h = d.teams.firstOrNull { it.teamId == homeId } ?: return null
        val a = d.teams.firstOrNull { it.teamId == awayId } ?: return null
        if (h.startXI.isEmpty() && a.startXI.isEmpty()) return null
        return MatchLineups(lineup(h, scorers), lineup(a, scorers))
    }

    fun standing(d: StandingRowDto) = StandingRow(
        d.rank, team(d.team), d.played, d.won, d.drawn, d.lost, d.goalsFor, d.goalsAgainst, d.goalDiff, d.points,
        d.form.orEmpty().filter { it == 'W' || it == 'D' || it == 'L' }.takeLast(5),
    )

    fun league(d: LeagueDto) = League(d.id, d.name, d.short ?: monogram(d.name), d.country, d.teamCount, d.season, d.currentRound)

    fun scorer(d: ScorerDto) = Scorer(d.rank, d.playerId, d.name, team(d.team), d.goals, d.assists)

    fun newsCategory(s: String): NewsCategory = when (s.lowercase()) {
        "transfers" -> NewsCategory.Transfers
        "analysis" -> NewsCategory.Analysis
        else -> NewsCategory.Reports
    }

    fun news(d: NewsDto) = NewsItem(d.id, newsCategory(d.category), cleanText(d.title), cleanText(d.dek), cleanText(d.body), d.source, d.publishedMs, d.readMinutes.coerceAtLeast(1), d.url)

    /** Fixes the mojibake seen in the competitor's feed ("men,Äôs") when a UTF-8 apostrophe was read as Mac Roman. */
    fun cleanText(s: String): String = s.replace(",Äô", "’").replace(",Äú", "“").replace(",Äù", "”").replace(",Äî", "—").trim()

    fun playerSummary(d: PlayerSummaryDto) = PlayerSummary(d.id, d.name, team(d.team), d.position, d.number, d.nationality, d.rating)

    private fun playerMatch(d: PlayerMatchDto) = PlayerMatch(d.matchId, team(d.opponent), d.home, d.score, d.minutes, d.rating, d.dateMs)
    private fun career(d: CareerStopDto) = CareerStop(d.club, d.years, d.apps, d.goals, d.current)

    fun player(d: PlayerDto) = PlayerProfile(
        summary = playerSummary(d.summary), age = d.age, heightCm = d.heightCm, foot = d.foot, marketValue = d.marketValue,
        goals = d.goals, assists = d.assists, matches = d.matches, minutes = d.minutes,
        shotsPer90 = d.shotsPer90, keyPassesPer90 = d.keyPassesPer90, xg = d.xg, scoutingNote = d.scoutingNote,
        recentMatches = d.recentMatches.map(::playerMatch), career = d.career.map(::career),
    )

    fun position(s: String): Position = when (s.uppercase().take(1)) {
        "G" -> Position.Goalkeeper
        "D" -> Position.Defender
        "M" -> Position.Midfielder
        else -> Position.Forward
    }

    private fun squad(d: SquadPlayerDto) = SquadPlayer(d.playerId, d.name, d.number, position(d.position), d.rating)

    fun team(d: TeamDto) = TeamDetail(team(d.team), d.leagueName, d.coach, d.stadium, d.city, d.capacity, d.founded, d.squad.map(::squad))

    fun search(d: SearchDto) = SearchResults(d.teams.map(::team), d.leagues.map(::league), d.players.map(::playerSummary))
}
