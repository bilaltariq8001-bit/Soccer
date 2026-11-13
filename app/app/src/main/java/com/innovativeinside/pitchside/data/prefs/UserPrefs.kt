package com.innovativeinside.pitchside.data.prefs

import kotlinx.serialization.Serializable

enum class AlertLevel(val label: String, val desc: String) {
    Every("Every goal", "Goals, cards, kickoff, full time"),
    Key("Key moments", "Goals, red cards, full time"),
    Final("Final scores only", "One alert when the match ends"),
    Off("Muted", "No alerts for this team"),
}

enum class WatchStyle(val title: String, val desc: String) {
    Live("Live, on TV or stream", "Fast scores and every goal alert."),
    Later("Highlights later", "Spoiler Shield on. Scores stay hidden until you tap reveal."),
    Check("I just check scores", "Big matches and final results, nothing noisy."),
}

enum class FollowKind(val label: String) { Clubs("Clubs"), National("National teams"), Players("Players"), BigOnly("Just big matches") }

enum class ThemeChoice(val label: String) { System("System"), Light("Light"), Dark("Dark") }

enum class Persona(val title: String, val tagline: String, val bullets: List<String>) {
    Fanatic("Matchday Fanatic", "Never miss a goal.", listOf(
        "Live matches and your teams are pinned to the top.",
        "An alert for every goal, card and kickoff.",
        "Match Briefs update live during games.")),
    CatchUp("Catch-up Fan", "Watch first, spoil never.", listOf(
        "Scores for your teams stay hidden until you tap reveal.",
        "One final-score alert, held until you say you're caught up.",
        "Match Briefs open on the recap.")),
    Casual("Casual Follower", "Only what matters.", listOf(
        "Home shows the top leagues and today's big games.",
        "Alerts for key moments only.",
        "Short, simple Match Briefs.")),
    Nerd("Stats Nerd", "All the numbers.", listOf(
        "Match pages open on Stats, with xG up front.",
        "Players you follow are tracked closely.",
        "Deeper Match Briefs.")),
    Explorer("League Explorer", "Your leagues, one place.", listOf(
        "Home is grouped by league.",
        "Standings are one tap away.",
        "Alerts for key moments only.")),
}

/** Everything the user chooses, persisted on device as one JSON document in DataStore. */
@Serializable
data class UserPrefs(
    val onboarded: Boolean = false,
    val language: String = "en",
    val theme: ThemeChoice = ThemeChoice.System,
    val followKind: FollowKind = FollowKind.Clubs,
    val watchStyle: WatchStyle = WatchStyle.Live,
    val alertLevel: AlertLevel = AlertLevel.Key,
    val quietHours: Boolean = true,
    val detailed: Boolean = true,
    val teams: List<String> = emptyList(),
    val teamAlerts: Map<String, AlertLevel> = emptyMap(),
    val leagues: List<String> = listOf("pd"),
    val players: List<String> = emptyList(),
    val matchAlerts: List<String> = emptyList(),
    val shielded: List<String> = emptyList(),
    val revealed: List<String> = emptyList(),
    val askDay: Long = 0,
    val askUsed: Int = 0,
    val isPro: Boolean = false,
    val persona: Persona? = null,
    /** Last searches, newest first (local only). */
    val recentSearches: List<String> = emptyList(),
) {
    companion object {
        const val FREE_TEAM_LIMIT = 5
        const val FREE_ASK_LIMIT = 5
        const val PRO_ASK_LIMIT = 50
        const val RECENT_SEARCH_LIMIT = 8
    }
}

/** Pure state transitions on [UserPrefs]. Kept free of Android so they are unit tested directly. */
object PrefsReducer {
    sealed interface FollowResult {
        data class Ok(val prefs: UserPrefs, val following: Boolean) : FollowResult
        data object LimitReached : FollowResult
    }

    fun toggleTeam(p: UserPrefs, teamId: String): FollowResult = if (teamId in p.teams) {
        FollowResult.Ok(p.copy(teams = p.teams - teamId, teamAlerts = p.teamAlerts - teamId, shielded = p.shielded - teamId), following = false)
    } else if (!p.isPro && p.teams.size >= UserPrefs.FREE_TEAM_LIMIT) {
        FollowResult.LimitReached
    } else {
        FollowResult.Ok(p.copy(teams = p.teams + teamId, teamAlerts = p.teamAlerts + (teamId to p.alertLevel)), following = true)
    }

    fun toggleLeague(p: UserPrefs, id: String) = p.copy(leagues = if (id in p.leagues) p.leagues - id else p.leagues + id)
    fun togglePlayer(p: UserPrefs, id: String) = p.copy(players = if (id in p.players) p.players - id else p.players + id)
    fun toggleMatchAlert(p: UserPrefs, id: String) = p.copy(matchAlerts = if (id in p.matchAlerts) p.matchAlerts - id else p.matchAlerts + id)
    fun toggleShield(p: UserPrefs, teamId: String) = p.copy(shielded = if (teamId in p.shielded) p.shielded - teamId else p.shielded + teamId)
    fun setTeamAlert(p: UserPrefs, teamId: String, level: AlertLevel) = p.copy(teamAlerts = p.teamAlerts + (teamId to level))
    fun reveal(p: UserPrefs, matchId: String) = if (matchId in p.revealed) p else p.copy(revealed = p.revealed + matchId)
    fun caughtUp(p: UserPrefs, matchIds: Collection<String>) = p.copy(revealed = (p.revealed + matchIds).distinct())
    fun shieldAll(p: UserPrefs, on: Boolean) = p.copy(shielded = if (on) p.teams else emptyList(), revealed = emptyList())

    fun persona(p: UserPrefs): Persona = when {
        p.followKind == FollowKind.BigOnly -> Persona.Casual
        p.watchStyle == WatchStyle.Later -> Persona.CatchUp
        p.followKind == FollowKind.Players && p.detailed -> Persona.Nerd
        p.teams.isEmpty() -> Persona.Explorer
        else -> Persona.Fanatic
    }

    /** Applied when onboarding finishes: persona defaults for Spoiler Shield and alert levels. */
    fun completeOnboarding(p: UserPrefs): UserPrefs {
        val persona = persona(p)
        val level = if (persona == Persona.CatchUp && p.alertLevel == AlertLevel.Key) AlertLevel.Final else p.alertLevel
        return p.copy(
            onboarded = true,
            persona = persona,
            alertLevel = level,
            shielded = if (persona == Persona.CatchUp) p.teams else emptyList(),
            revealed = emptyList(),
            teamAlerts = p.teams.associateWith { level },
        )
    }

    /** Adds a search to the front of the recent list (case-insensitive de-dup, capped). */
    fun addRecentSearch(p: UserPrefs, query: String): UserPrefs {
        val q = query.trim()
        if (q.length < 2) return p
        val list = (listOf(q) + p.recentSearches.filterNot { it.equals(q, ignoreCase = true) }).take(UserPrefs.RECENT_SEARCH_LIMIT)
        return p.copy(recentSearches = list)
    }

    fun removeRecentSearch(p: UserPrefs, query: String) = p.copy(recentSearches = p.recentSearches.filterNot { it.equals(query, ignoreCase = true) })
    fun clearRecentSearches(p: UserPrefs) = p.copy(recentSearches = emptyList())

    fun askLimit(p: UserPrefs) = if (p.isPro) UserPrefs.PRO_ASK_LIMIT else UserPrefs.FREE_ASK_LIMIT

    /** Remaining questions today; the counter resets when the day changes. */
    fun askLeft(p: UserPrefs, today: Long): Int = (askLimit(p) - if (p.askDay == today) p.askUsed else 0).coerceAtLeast(0)

    fun useAsk(p: UserPrefs, today: Long): UserPrefs? {
        if (askLeft(p, today) <= 0) return null
        val used = if (p.askDay == today) p.askUsed else 0
        return p.copy(askDay = today, askUsed = used + 1)
    }

    /** A score is hidden when a team is shielded, the match has a score and the user has not revealed it. */
    fun isHidden(p: UserPrefs, matchId: String, homeId: String, awayId: String, hasScore: Boolean) =
        hasScore && matchId !in p.revealed && (homeId in p.shielded || awayId in p.shielded)
}
