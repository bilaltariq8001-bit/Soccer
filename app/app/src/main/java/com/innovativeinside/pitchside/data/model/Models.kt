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
    /** Crest image URL from the data source; the monogram is the fallback. */
    val logo: String? = null,
)

data class League(
    val id: String,
    val name: String,
    val short: String,
    val country: String,
    val teamCount: Int,
    val season: String,
    val currentRound: String,
    /** Competition logo and country flag URLs, when the source has them. */
    val logo: String? = null,
    val countryLogo: String? = null,
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
    /** Preview text from the data source. Not shown in the UI. */
    val brief: String,
    /** Pre-match win probability when the data source has one. */
    val probs: WinProbability? = null,
    /** Competition logo URL, for league headers in match lists. */
    val leagueLogo: String? = null,
    val country: String = "",
) {
    val hasScore: Boolean get() = homeScore != null && awayScore != null
    fun involves(teamIds: Collection<String>) = home.id in teamIds || away.id in teamIds
}

/** Percentages (0-100) for home win, draw and away win; goal markets when the source publishes them. */
data class WinProbability(val home: Int, val draw: Int, val away: Int, val over25: Int? = null, val btts: Int? = null)

/** Decimal 1X2 odds from one bookmaker, or the in-play market when [live]. */
data class MatchOdds(val bookmaker: String, val home: Double, val draw: Double, val away: Double, val live: Boolean = false)

/** One line of live commentary ("44:58", "Kunshan free kick"). */
data class Commentary(val time: String, val text: String)

/** A highlight clip opened in the browser or video app. */
data class Highlight(val title: String, val url: String)

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
data class LineupPlayer(val playerId: String, val number: Int, val name: String, val position: String, val x: Float, val y: Float, val scored: Boolean = false, val photo: String? = null)

/**
 * [hasPositions] is false when the source gave no formation and no usable positions, so the pitch view would only be a
 * guess. [shapeFromSquad] marks a formation worked out from each starter's squad position rather than published.
 */
data class Lineup(
    val teamId: String, val formation: String, val coach: String, val starters: List<LineupPlayer>, val bench: List<LineupPlayer>,
    val hasPositions: Boolean = true, val shapeFromSquad: Boolean = false,
)

data class MatchLineups(val home: Lineup, val away: Lineup)

data class StandingRow(
    val rank: Int, val team: Team, val played: Int, val won: Int, val drawn: Int, val lost: Int,
    val goalsFor: Int, val goalsAgainst: Int, val goalDiff: Int, val points: Int, val form: String,
)

data class Scorer(val rank: Int, val playerId: String, val name: String, val team: Team, val goals: Int, val assists: Int, val photo: String? = null)

data class NewsItem(
    val id: String,
    val category: NewsCategory,
    val title: String,
    val dek: String,
    val body: String,
    val source: String,
    val publishedMs: Long,
    val readMinutes: Int,
    /** Link to the full story on the publisher's site (RSS items always have one). */
    val url: String?,
    /** Lead image (RSS media:thumbnail). Null for stories without one. */
    val imageUrl: String? = null,
    /** Credit line shipped with the image ("Caption | Photographer/Agency"). */
    val imageCaption: String? = null,
    /** Which 90min edition the story comes from; null for stories from the Pitchside API. */
    val edition: NewsEdition? = null,
    val author: String? = null,
    /** BCP-47 language of the story ("fr", "it", "es", "pt-BR"). */
    val language: String? = null,
) {
    /** The kicker shown above the headline: the edition for feed stories, otherwise the category. */
    val kicker: String get() = edition?.label ?: category.label
}

enum class NewsCategory(val label: String) { Reports("Match reports"), Transfers("Transfers"), Analysis("Analysis") }

/** The 90min RSS editions the News tab offers. [path] is relative to https://www.90min.com/. */
enum class NewsEdition(val code: String, val label: String, val language: String, val path: String) {
    France("fr", "France", "fr", "fr/feed"),
    Italy("it", "Italy", "it", "it/feed"),
    Spain("es", "Spain", "es", "es/feed"),
    Brazil("br", "Brazil", "pt-BR", "pt-BR/feed"),
    ;

    companion object {
        fun fromCode(code: String): NewsEdition? = entries.firstOrNull { it.code == code }
    }
}

data class PlayerSummary(
    val id: String,
    val name: String,
    val team: Team,
    val position: String,
    val number: Int,
    val nationality: String,
    val rating: Double,
    /** Player photo URL, when the source has one. */
    val photo: String? = null,
) {
    val initials: String get() = name.split(" ").filter { it.isNotBlank() }.take(2).joinToString("") { it.first().uppercase() }
}

data class PlayerMatch(
    val matchId: String, val opponent: Team, val home: Boolean, val score: String, val minutes: Int, val rating: Double, val dateMs: Long,
    val goals: Int = 0, val started: Boolean = true,
    /** "W", "D" or "L" from the player's team's point of view; blank when unknown. */
    val result: String = "",
)

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
    val yellowCards: Int = 0,
    val redCards: Int = 0,
    val starts: Int = 0,
    /** Where the season numbers come from ("" = the source's own player stats). */
    val statsNote: String = "",
)

data class SquadPlayer(val playerId: String, val name: String, val number: Int, val position: Position, val rating: Double?, val photo: String? = null, val age: Int = 0)

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
