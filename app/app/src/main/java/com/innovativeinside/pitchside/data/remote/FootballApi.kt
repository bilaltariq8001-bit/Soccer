package com.innovativeinside.pitchside.data.remote

import com.innovativeinside.pitchside.data.remote.dto.AskRequest
import com.innovativeinside.pitchside.data.remote.dto.AskResponse
import com.innovativeinside.pitchside.data.remote.dto.EventDto
import com.innovativeinside.pitchside.data.remote.dto.FixtureDto
import com.innovativeinside.pitchside.data.remote.dto.LeagueDto
import com.innovativeinside.pitchside.data.remote.dto.LineupsDto
import com.innovativeinside.pitchside.data.remote.dto.NewsDto
import com.innovativeinside.pitchside.data.remote.dto.PlayerDto
import com.innovativeinside.pitchside.data.remote.dto.PlayerSummaryDto
import com.innovativeinside.pitchside.data.remote.dto.PredictionDto
import com.innovativeinside.pitchside.data.remote.dto.OddsDto
import com.innovativeinside.pitchside.data.remote.dto.LiveDataDto
import com.innovativeinside.pitchside.data.remote.dto.VideoDto
import com.innovativeinside.pitchside.data.remote.dto.ScorerDto
import com.innovativeinside.pitchside.data.remote.dto.SearchDto
import com.innovativeinside.pitchside.data.remote.dto.StandingRowDto
import com.innovativeinside.pitchside.data.remote.dto.StatLineDto
import com.innovativeinside.pitchside.data.remote.dto.TeamDto
import com.innovativeinside.pitchside.data.remote.dto.TeamRefDto

/**
 * The football data API, one function per endpoint in API_CONTRACTS.md. Implementations throw
 * [com.innovativeinside.pitchside.core.AppError] on failure. Two implementations exist:
 * [HttpFootballApi] (real gateway over OkHttp) and the mock in data/mock.
 */
interface FootballApi {
    /** GET /v1/fixtures?date=YYYY-MM-DD */
    suspend fun fixtures(date: String): List<FixtureDto>
    /** GET /v1/fixtures/live */
    suspend fun liveFixtures(): List<FixtureDto>
    /** GET /v1/fixtures/{id} */
    suspend fun fixture(id: String): FixtureDto
    /** GET /v1/fixtures/{id}/events */
    suspend fun events(fixtureId: String): List<EventDto>
    /** GET /v1/fixtures/{id}/stats */
    suspend fun stats(fixtureId: String): List<StatLineDto>
    /** GET /v1/fixtures/{id}/lineups */
    suspend fun lineups(fixtureId: String): LineupsDto
    /** GET /v1/leagues */
    suspend fun leagues(): List<LeagueDto>
    /** GET /v1/leagues/{id} */
    suspend fun league(id: String): LeagueDto
    /** GET /v1/leagues/{id}/fixtures */
    suspend fun leagueFixtures(leagueId: String): List<FixtureDto>
    /** GET /v1/leagues/{id}/standings */
    suspend fun standings(leagueId: String): List<StandingRowDto>
    /** GET /v1/leagues/{id}/scorers */
    suspend fun scorers(leagueId: String): List<ScorerDto>
    /** GET /v1/teams?popular=true */
    suspend fun popularTeams(): List<TeamRefDto>
    /** GET /v1/teams/{id} */
    suspend fun team(id: String): TeamDto
    /** GET /v1/teams/{id}/fixtures */
    suspend fun teamFixtures(teamId: String): List<FixtureDto>
    /** GET /v1/players/{id} */
    suspend fun player(id: String): PlayerDto
    /** GET /v1/players/top */
    suspend fun topPlayers(): List<PlayerSummaryDto>
    /** GET /v1/news?category=&cursor= */
    suspend fun news(category: String?, cursor: String?): Page<NewsDto>
    /** GET /v1/news/{id} */
    suspend fun article(id: String): NewsDto
    /** GET /v1/search?q= */
    suspend fun search(query: String): SearchDto
    /** POST /v1/ask (auth optional, quota enforced server side) */
    suspend fun ask(request: AskRequest): AskResponse
    /** Pre-match probabilities for one fixture; null when the source has none (default: unsupported). */
    suspend fun prediction(fixtureId: String): PredictionDto? = null
    /** Previous meetings of the two teams in this fixture, newest first (default: unsupported). */
    suspend fun headToHead(fixtureId: String): List<FixtureDto> = emptyList()
    /** Each team's latest finished results before this fixture, newest first: (home side, away side). */
    suspend fun recentResults(fixtureId: String): Pair<List<FixtureDto>, List<FixtureDto>> = emptyList<FixtureDto>() to emptyList()
    /** Pre-match 1X2 odds for one fixture (default: unsupported). */
    suspend fun odds(fixtureId: String): OddsDto? = null
    /** Live commentary and live odds while a fixture is in play (default: unsupported). */
    suspend fun liveCommentary(fixtureId: String): LiveDataDto? = null
    /** Highlight videos for one fixture (default: unsupported). */
    suspend fun videos(fixtureId: String): List<VideoDto> = emptyList()
}

data class Page<T>(val items: List<T>, val nextCursor: String?)
