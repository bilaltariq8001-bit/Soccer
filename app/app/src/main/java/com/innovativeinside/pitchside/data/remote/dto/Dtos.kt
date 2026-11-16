package com.innovativeinside.pitchside.data.remote.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/*
 * Wire shapes of the Pitchside REST gateway. They mirror the normalized documents the backend writes
 * (backend/functions/src/types.ts: FixtureDoc, EventDoc, StandingRow, StatLine), with ids as strings.
 * Unknown fields are ignored, and optional fields have defaults, so the app tolerates additive changes.
 */

@Serializable
data class ListEnvelope<T>(val items: List<T> = emptyList(), val nextCursor: String? = null, val updatedAtMs: Long? = null)

@Serializable
data class ErrorBody(val code: String? = null, val message: String? = null)

@Serializable
data class TeamRefDto(
    val id: String,
    val name: String,
    val short: String? = null,
    val color: String? = null,
    val logo: String? = null,
    val leagueId: String? = null,
)

@Serializable
data class FixtureDto(
    val id: String,
    val leagueId: String,
    val leagueName: String,
    val round: String = "",
    val kickoffMs: Long,
    /** "upcoming" | "live" | "finished" | "postponed" */
    val status: String,
    val statusShort: String = "",
    val minute: Int? = null,
    val label: String = "",
    val venue: String = "",
    val referee: String? = null,
    val home: TeamRefDto,
    val away: TeamRefDto,
    val homeScore: Int? = null,
    val awayScore: Int? = null,
    val brief: String? = null,
    /** Pre-match win probability, when the source publishes one (apifootball.com get_predictions). */
    val prediction: PredictionDto? = null,
    val leagueLogo: String? = null,
    val country: String = "",
)

/** Win / draw / win probabilities in percent, plus a couple of goal markets when known. */
@Serializable
data class PredictionDto(
    val home: Double,
    val draw: Double,
    val away: Double,
    val over25: Double? = null,
    val btts: Double? = null,
)

/** Decimal 1X2 prices from one bookmaker (or the live market when [live]). */
@Serializable
data class OddsDto(val bookmaker: String, val home: Double, val draw: Double, val away: Double, val updated: String? = null, val live: Boolean = false)

@Serializable
data class CommentDto(val time: String, val text: String, val state: String = "")

/** Live commentary and the live 1X2 market of one match. */
@Serializable
data class LiveDataDto(val comments: List<CommentDto> = emptyList(), val liveOdds: OddsDto? = null)

@Serializable
data class VideoDto(val title: String, val url: String)

@Serializable
data class EventDto(
    val key: String,
    /** "goal" | "yellowCard" | "redCard" | "sub" */
    val kind: String,
    val minute: Int,
    val extra: Int? = null,
    val teamId: String,
    val playerName: String,
    val assistName: String? = null,
    val detail: String = "",
)

@Serializable
data class StatLineDto(val label: String, val home: Int, val away: Int, val homeText: String? = null, val awayText: String? = null)

@Serializable
data class LineupPlayerDto(val id: String, val name: String, val number: Int? = null, val pos: String? = null, val grid: String? = null, val photo: String? = null)

@Serializable
data class LineupDto(
    val teamId: String,
    val coach: String? = null,
    val formation: String? = null,
    val startXI: List<LineupPlayerDto> = emptyList(),
    val substitutes: List<LineupPlayerDto> = emptyList(),
    /** True when [formation] and the grid were worked out from squad positions, not published. */
    val derived: Boolean = false,
)

@Serializable
data class LineupsDto(val teams: List<LineupDto> = emptyList())

@Serializable
data class StandingRowDto(
    val rank: Int,
    val team: TeamRefDto,
    val played: Int,
    val won: Int,
    val drawn: Int,
    val lost: Int,
    val goalsFor: Int = 0,
    val goalsAgainst: Int = 0,
    val goalDiff: Int,
    val points: Int,
    val form: String? = null,
)

@Serializable
data class LeagueDto(
    val id: String,
    val name: String,
    val short: String? = null,
    val country: String = "",
    val teamCount: Int = 0,
    val season: String = "",
    val currentRound: String = "",
    val logo: String? = null,
    val countryLogo: String? = null,
)

@Serializable
data class ScorerDto(val rank: Int, val playerId: String, val name: String, val team: TeamRefDto, val goals: Int, val assists: Int = 0, val photo: String? = null)

@Serializable
data class NewsDto(
    val id: String,
    /** "reports" | "transfers" | "analysis" */
    val category: String,
    val title: String,
    val dek: String = "",
    val body: String = "",
    val source: String = "",
    val publishedMs: Long,
    val readMinutes: Int = 1,
    val url: String? = null,
)

@Serializable
data class PlayerSummaryDto(
    val id: String,
    val name: String,
    val team: TeamRefDto,
    val position: String = "",
    val number: Int = 0,
    val nationality: String = "",
    val rating: Double = 0.0,
    val photo: String? = null,
)

@Serializable
data class PlayerMatchDto(
    val matchId: String, val opponent: TeamRefDto, val home: Boolean, val score: String, val minutes: Int, val rating: Double, val dateMs: Long,
    val goals: Int = 0, val started: Boolean = true, val result: String = "",
)

@Serializable
data class CareerStopDto(val club: String, val years: String, val apps: Int, val goals: Int, val current: Boolean = false)

@Serializable
data class PlayerDto(
    val summary: PlayerSummaryDto,
    val age: Int = 0,
    val heightCm: Int = 0,
    val foot: String = "",
    val marketValue: String = "",
    val goals: Int = 0,
    val assists: Int = 0,
    val matches: Int = 0,
    val minutes: Int = 0,
    val shotsPer90: Double = 0.0,
    val keyPassesPer90: Double = 0.0,
    val xg: Double = 0.0,
    val scoutingNote: String = "",
    val recentMatches: List<PlayerMatchDto> = emptyList(),
    val career: List<CareerStopDto> = emptyList(),
    val yellowCards: Int = 0,
    val redCards: Int = 0,
    val starts: Int = 0,
    val statsNote: String = "",
)

@Serializable
data class SquadPlayerDto(val playerId: String, val name: String, val number: Int = 0, val position: String, val rating: Double? = null, val photo: String? = null, val age: Int = 0)

@Serializable
data class TeamDto(
    val team: TeamRefDto,
    val leagueName: String = "",
    val coach: String = "",
    val stadium: String = "",
    val city: String = "",
    val capacity: Int = 0,
    val founded: Int = 0,
    val squad: List<SquadPlayerDto> = emptyList(),
)

@Serializable
data class SearchDto(
    val teams: List<TeamRefDto> = emptyList(),
    val leagues: List<LeagueDto> = emptyList(),
    val players: List<PlayerSummaryDto> = emptyList(),
)

@Serializable
data class AskRequest(val question: String, val language: String)

@Serializable
data class AskResponse(val answer: String, @SerialName("remainingToday") val remaining: Int? = null)

@Serializable
data class AuthRequest(val email: String, val password: String, val displayName: String? = null)

@Serializable
data class AuthResponse(val userId: String, val email: String, val displayName: String, val accessToken: String, val refreshToken: String? = null, val expiresAtMs: Long? = null)

@Serializable
data class ResetRequest(val email: String)
