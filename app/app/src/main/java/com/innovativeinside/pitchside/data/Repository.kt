package com.innovativeinside.pitchside.data

/**
 * The single seam between the UI and data. The screens only talk to this interface, so the backend
 * (Firebase + API-Football proxy) replaces [FakeRepository] without touching any UI code.
 */
interface Repository {
    fun matches(): List<Match>
    fun match(id: String): Match?
    fun events(matchId: String): List<MatchEvent>
    fun stats(matchId: String): List<StatRow>
    fun lineup(matchId: String, home: Boolean): Lineup
    fun standings(leagueId: String): List<StandingRow>
    fun scorers(leagueId: String): List<Scorer>
    fun news(): List<NewsItem>
    fun player(id: String): PlayerProfile
    fun popularTeams(): List<Team>
    fun allTeams(): List<Team>
    fun leagues(): List<LeagueInfo>
    fun languages(): List<Language>
    fun answer(question: String): String
}
