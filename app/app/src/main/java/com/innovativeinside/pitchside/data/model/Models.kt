package com.innovativeinside.pitchside.data.model

/** Domain models. The UI only ever sees these; DTOs from the network are mapped into them. */

enum class Status { Live, Upcoming, Finished, Postponed }

data class Team(
    val id: String,
    val name: String,
    val short: String,
    /** ARGB club colour used for the monogram crest. */
    val color: Long,
    val leagueId: String = "",
)

data class League(
    val id: String,
    val name: String,
    val short: String,
    val country: String,
    val teamCount: Int,
    val season: String,
    val currentRound: String,
)

data class Match(
    val id: String,
    val leagueId: String,
    val leagueName: String,
    val round: String,
    val venue: String,
    val referee: String?,
    val kickoffMs: Long,
    val home: Team,
    val away: Team,
    val homeScore: Int?,
    val awayScore: Int?,
    val status: Status,
    /** What the badge shows: "74'", "HT", "20:45", "FT" or "Postponed". */
    val label: String,
    /** Short Match Brief text (computed from match data on the backend). */
    val brief: String,
) {
    val hasScore: Boolean get() = homeScore != null && awayScore != null
    fun involves(teamIds: Collection<String>) = home.id in teamIds || away.id in teamIds
}

enum class EventKind { Goal, YellowCard, RedCard, Sub }

data class MatchEvent(
    val minute: Int,
    val extra: Int?,
    val isHome: Boolean,
    val kind: EventKind,
    val player: String,
    val detail: String,
) {
    val minuteLabel: String get() = if (extra != null && extra > 0) "$minute+$extra'" else "$minute'"
}

data class StatRow(val label: String, val home: Int, val away: Int, val homeText: String, val awayText: String)

/** Pitch position as a fraction of the pitch, y = 0 is the opponent goal. */
data class LineupPlayer(val playerId: String, val number: Int, val name: String, val position: String, val x: Float, val y: Float, val scored: Boolean = false)

data class Lineup(val teamId: String, val formation: String, val coach: String, val starters: List<LineupPlayer>, val bench: List<LineupPlayer>)

data class MatchLineups(val home: Lineup, val away: Lineup)

data class StandingRow(
    val rank: Int, val team: Team, val played: Int, val won: Int, val drawn: Int, val lost: Int,
    val goalsFor: Int, val goalsAgainst: Int, val goalDiff: Int, val points: Int, val form: String,
)

data class Scorer(val rank: Int, val playerId: String, val name: String, val team: Team, val goals: Int, val assists: Int)

data class NewsItem(
    val id: String,
    val category: NewsCategory,
    val title: String,
    val dek: String,
    val body: String,
    val source: String,
    val publishedMs: Long,
    val readMinutes: Int,
    val url: String?,
)

enum class NewsCategory(val label: String) { Reports("Match reports"), Transfers("Transfers"), Analysis("Analysis") }

data class PlayerSummary(
    val id: String,
    val name: String,
    val team: Team,
    val position: String,
    val number: Int,
    val nationality: String,
    val rating: Double,
) {
    val initials: String get() = name.split(" ").filter { it.isNotBlank() }.take(2).joinToString("") { it.first().uppercase() }
}

data class PlayerMatch(val matchId: String, val opponent: Team, val home: Boolean, val score: String, val minutes: Int, val rating: Double, val dateMs: Long)

data class CareerStop(val club: String, val years: String, val apps: Int, val goals: Int, val current: Boolean = false)

data class PlayerProfile(
    val summary: PlayerSummary,
    val age: Int,
    val heightCm: Int,
    val foot: String,
    val marketValue: String,
    val goals: Int,
    val assists: Int,
    val matches: Int,
    val minutes: Int,
    val shotsPer90: Double,
    val keyPassesPer90: Double,
    val xg: Double,
    val scoutingNote: String,
    val recentMatches: List<PlayerMatch>,
    val career: List<CareerStop>,
)

data class SquadPlayer(val playerId: String, val name: String, val number: Int, val position: Position, val rating: Double?)

enum class Position(val label: String) { Goalkeeper("Goalkeepers"), Defender("Defenders"), Midfielder("Midfielders"), Forward("Forwards") }

data class TeamDetail(
    val team: Team,
    val leagueName: String,
    val coach: String,
    val stadium: String,
    val city: String,
    val capacity: Int,
    val founded: Int,
    val squad: List<SquadPlayer>,
)

data class SearchResults(val teams: List<Team>, val leagues: List<League>, val players: List<PlayerSummary>) {
    val isEmpty: Boolean get() = teams.isEmpty() && leagues.isEmpty() && players.isEmpty()
}

data class Language(val code: String, val native: String, val english: String)
