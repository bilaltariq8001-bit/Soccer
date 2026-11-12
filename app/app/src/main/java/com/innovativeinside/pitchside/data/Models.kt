package com.innovativeinside.pitchside.data

enum class Status { Live, Upcoming, Finished, Postponed }

data class Team(val id: String, val name: String, val short: String, val color: Long)

data class Match(
    val id: String,
    val league: String,
    val round: String,
    val venue: String,
    val home: Team,
    val away: Team,
    val homeScore: Int?,
    val awayScore: Int?,
    val status: Status,
    /** "74'", "HT", "20:45", "FT" or "Postponed". */
    val label: String,
    val brief: String,
)

data class StandingRow(
    val rank: Int, val team: Team, val played: Int, val won: Int, val drawn: Int, val lost: Int,
    val goalDiff: Int, val points: Int, val form: String,
)

data class Scorer(val rank: Int, val name: String, val team: Team, val goals: Int)

data class NewsItem(val id: String, val category: String, val title: String, val dek: String, val meta: String)

enum class EventKind { Goal, Card, Sub }

data class MatchEvent(val minute: String, val isHome: Boolean, val title: String, val sub: String, val kind: EventKind)

data class StatRow(val label: String, val home: Int, val away: Int, val homeText: String = home.toString(), val awayText: String = away.toString())

/** Pitch position as a fraction of the pitch, y = 0 is the opponent goal. */
data class LineupPlayer(val number: Int, val name: String, val x: Float, val y: Float, val scored: Boolean = false)

data class Lineup(val formation: String, val coach: String, val starters: List<LineupPlayer>, val bench: List<String>)

data class PlayerProfile(
    val id: String, val name: String, val initials: String, val team: Team, val position: String, val number: Int,
    val age: Int, val heightCm: Int, val foot: String,
    val goals: Int, val assists: Int, val rating: Double, val matches: Int,
    val lastRatings: List<Double>, val shotsPer90: Double, val keyPassesPer90: Double,
    val scoutingNote: String,
    val career: List<CareerStop>,
)

data class CareerStop(val club: String, val years: String, val apps: Int, val goals: Int, val current: Boolean = false)

data class Language(val native: String, val english: String)

data class LeagueInfo(val id: String, val name: String, val short: String, val teams: Int)
