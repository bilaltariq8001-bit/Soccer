package com.innovativeinside.pitchside

import android.app.Application
import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch
import com.innovativeinside.pitchside.data.FakeRepository
import com.innovativeinside.pitchside.data.Match
import com.innovativeinside.pitchside.data.Repository

enum class AlertLevel(val label: String, val desc: String) {
    Every("Every goal", "Goals, cards, kickoff, full time"),
    Key("Key moments", "Goals, red cards, full time"),
    Final("Final scores only", "One alert when the match ends"),
    Off("Not now", "No alerts. You can turn them on later."),
}

enum class WatchStyle(val title: String, val desc: String) {
    Live("Live, on TV or stream", "Fast scores and every goal alert."),
    Later("Highlights later", "Spoiler Shield on. Scores stay hidden until you tap reveal."),
    Check("I just check scores", "Big matches and final results, nothing noisy."),
}

enum class FollowKind(val label: String) { Clubs("Clubs"), National("National teams"), Players("Players"), BigOnly("Just big matches") }

enum class Persona(val title: String, val tagline: String, val bullets: List<String>) {
    Fanatic("Matchday Fanatic", "Never miss a goal.", listOf(
        "Live matches and your teams are pinned to the top.",
        "An alert for every goal, card and kickoff.",
        "Match Briefs update live during games.")),
    CatchUp("Catch-up Fan", "Watch first, spoil never.", listOf(
        "Scores for your teams stay hidden until you tap reveal.",
        "One final-score alert, held until you say you are caught up.",
        "Match Briefs open on the recap.")),
    Casual("Casual Follower", "Only what matters.", listOf(
        "Home shows the top leagues and today's big games.",
        "Alerts for key moments only.",
        "Short, simple Match Briefs.")),
    Nerd("Stats Nerd", "All the numbers.", listOf(
        "Match pages open on Stats, with xG and per-90 numbers up front.",
        "Players you follow are tracked closely.",
        "Deeper Match Briefs.")),
    Explorer("League Explorer", "Your leagues, one place.", listOf(
        "Home is grouped by league.",
        "Standings are one tap away.",
        "Alerts for key moments only.")),
}

class AppViewModel(app: Application) : AndroidViewModel(app) {
    private val prefs = app.getSharedPreferences("pitchside", Context.MODE_PRIVATE)

    /** Swap for the Firebase-backed implementation when the backend exists. */
    val repo: Repository = FakeRepository

    var onboarded by mutableStateOf(prefs.getBoolean("onboarded", false)); private set
    var language by mutableStateOf(prefs.getString("language", "English") ?: "English"); private set

    private fun loadList(key: String, default: List<String>): List<String> =
        prefs.getString(key, null)?.split(",")?.filter { it.isNotBlank() } ?: default

    private fun today(): Long = System.currentTimeMillis() / 86_400_000L

    // Onboarding answers
    var followKind by mutableStateOf(FollowKind.Clubs)
    var watchStyle by mutableStateOf(WatchStyle.Live)
    var alertLevel by mutableStateOf(AlertLevel.entries.firstOrNull { it.name == prefs.getString("alert", null) } ?: AlertLevel.Key)
    var quietHours by mutableStateOf(prefs.getBoolean("quiet", true))
    var detailed by mutableStateOf(prefs.getBoolean("detailed", true))

    // Follows and Spoiler Shield. All of these are saved on the device, see the save loop below.
    val followedTeams = mutableStateListOf<String>().also { it.addAll(loadList("teams", listOf("ng", "ru"))) }
    val teamAlert = mutableStateMapOf<String, AlertLevel>().also { map ->
        val saved = prefs.getString("alerts", null)
        if (saved == null) { map["ng"] = AlertLevel.Key; map["ru"] = AlertLevel.Key }
        else saved.split(",").filter { ":" in it }.forEach { pair ->
            val (id, name) = pair.split(":")
            AlertLevel.entries.firstOrNull { it.name == name }?.let { map[id] = it }
        }
    }
    val shielded = mutableStateListOf<String>().also { it.addAll(loadList("shielded", emptyList())) }
    val revealed = mutableStateListOf<String>()
    val followedLeagues = mutableStateListOf<String>().also { it.addAll(loadList("leagues", listOf("pd", "cc"))) }
    val followedPlayers = mutableStateListOf<String>().also { it.addAll(loadList("players", listOf("p9"))) }

    // Assistant quota (resets each day) and plan
    var askUsed by mutableIntStateOf(if (prefs.getLong("askDay", 0L) == today()) prefs.getInt("askUsed", 1) else 0); private set
    val askLimit = 5
    var isPro by mutableStateOf(false)

    /** "System", "Light" or "Dark". */
    var themeChoice by mutableStateOf(prefs.getString("theme", "System") ?: "System")

    init {
        // Save whenever any of the above changes, so follows, shields and settings survive a restart.
        viewModelScope.launch {
            snapshotFlow {
                listOf(
                    followedTeams.toList(), teamAlert.entries.map { "${it.key}:${it.value.name}" }, shielded.toList(),
                    followedLeagues.toList(), followedPlayers.toList(), listOf(alertLevel.name, quietHours, detailed, themeChoice, askUsed),
                )
            }.collect { s ->
                @Suppress("UNCHECKED_CAST")
                prefs.edit()
                    .putString("teams", (s[0] as List<String>).joinToString(","))
                    .putString("alerts", (s[1] as List<String>).joinToString(","))
                    .putString("shielded", (s[2] as List<String>).joinToString(","))
                    .putString("leagues", (s[3] as List<String>).joinToString(","))
                    .putString("players", (s[4] as List<String>).joinToString(","))
                    .putString("alert", alertLevel.name).putBoolean("quiet", quietHours).putBoolean("detailed", detailed)
                    .putString("theme", themeChoice).putInt("askUsed", askUsed).putLong("askDay", today())
                    .apply()
            }
        }
    }

    /** One-line confirmation shown at the bottom of the screen, cleared by the UI after a short delay. */
    var toastMessage by mutableStateOf<String?>(null)

    fun persona(): Persona = when {
        followKind == FollowKind.BigOnly -> Persona.Casual
        watchStyle == WatchStyle.Later -> Persona.CatchUp
        followKind == FollowKind.Players && detailed -> Persona.Nerd
        followedTeams.isEmpty() -> Persona.Explorer
        else -> Persona.Fanatic
    }

    fun applyPersonaDefaults() {
        val p = persona()
        shielded.clear()
        revealed.clear()
        if (p == Persona.CatchUp) shielded.addAll(followedTeams)
        if (p == Persona.CatchUp && alertLevel == AlertLevel.Key) alertLevel = AlertLevel.Final
        followedTeams.forEach { teamAlert[it] = alertLevel }
    }

    fun completeOnboarding() {
        applyPersonaDefaults()
        onboarded = true
        prefs.edit().putBoolean("onboarded", true).apply()
    }

    fun resetAll() {
        onboarded = false
        prefs.edit().clear().apply()
        followedTeams.clear(); followedTeams.addAll(listOf("ng", "ru"))
        shielded.clear(); revealed.clear()
        teamAlert.clear(); teamAlert["ng"] = AlertLevel.Key; teamAlert["ru"] = AlertLevel.Key
        followedLeagues.clear(); followedLeagues.addAll(listOf("pd", "cc"))
        followedPlayers.clear(); followedPlayers.add("p9")
        alertLevel = AlertLevel.Key; quietHours = true; detailed = true; themeChoice = "System"
        askUsed = 1; isPro = false
    }

    fun chooseLanguage(l: String) { language = l; prefs.edit().putString("language", l).apply() }

    fun toggleFollow(teamId: String) {
        val name = repo.allTeams().firstOrNull { it.id == teamId }?.name ?: "team"
        if (teamId in followedTeams) { followedTeams.remove(teamId); teamAlert.remove(teamId); shielded.remove(teamId); toastMessage = "Unfollowed $name" }
        else { followedTeams.add(teamId); teamAlert[teamId] = alertLevel; toastMessage = "Following $name" }
    }

    fun toggleShield(teamId: String) {
        if (teamId in shielded) shielded.remove(teamId) else shielded.add(teamId)
    }

    /** A score is hidden when either team is shielded, the match has a score and the user has not revealed it. */
    fun isHidden(m: Match): Boolean =
        m.homeScore != null && m.id !in revealed && (m.home.id in shielded || m.away.id in shielded)

    fun reveal(matchId: String) { if (matchId !in revealed) revealed.add(matchId) }
    fun caughtUp() { revealed.addAll(repo.matches().map { it.id }.filter { it !in revealed }) }
    fun shieldAgain() { revealed.clear() }

    val askLeft: Int get() = if (isPro) 99 else (askLimit - askUsed).coerceAtLeast(0)
    fun useAsk(): Boolean { if (!isPro && askUsed >= askLimit) return false; askUsed++; return true }
}

val LocalApp = staticCompositionLocalOf<AppViewModel> { error("AppViewModel not provided") }
