package com.innovativeinside.pitchside.ui.vm

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.innovativeinside.pitchside.AppContainer
import com.innovativeinside.pitchside.data.auth.Session
import com.innovativeinside.pitchside.data.model.Match
import com.innovativeinside.pitchside.data.prefs.AlertLevel
import com.innovativeinside.pitchside.data.prefs.FollowKind
import com.innovativeinside.pitchside.data.prefs.PrefsReducer
import com.innovativeinside.pitchside.data.prefs.ThemeChoice
import com.innovativeinside.pitchside.data.prefs.UserPrefs
import com.innovativeinside.pitchside.data.prefs.WatchStyle
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/** One-off messages and navigation the UI handles once (snackbar, open paywall). */
sealed interface AppEvent {
    data class Message(val text: String) : AppEvent
    data object OpenPaywall : AppEvent
}

/**
 * Activity-scoped state shared by every screen: the user's preferences (follows, Spoiler Shield, theme,
 * alert levels) and the signed-in session. Screens read [prefs] and call the intent functions below;
 * all writes go through [PrefsReducer] and DataStore, so they are atomic and survive process death.
 */
class AppViewModel(private val c: AppContainer) : ViewModel() {

    /** Null until DataStore has been read once; the UI shows a splash until then (no flash of onboarding). */
    val prefs: StateFlow<UserPrefs?> = c.prefs.prefs.stateIn(viewModelScope, SharingStarted.Eagerly, null)
    val session: StateFlow<Session?> = c.auth.session.stateIn(viewModelScope, SharingStarted.Eagerly, null)

    /** Live match count for the nav badge. Polls only while the UI is collecting (stops in background). */
    val liveCount: StateFlow<Int> = flow {
        while (true) {
            c.football.liveMatches().onSuccess { emit(it.size) }
            delay(60_000)
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), 0)

    /** Competition chip selected on Home; the Matches tab shares it. Null is "All". */
    private val _competition = kotlinx.coroutines.flow.MutableStateFlow<String?>(null)
    val competition: StateFlow<String?> = _competition
    fun setCompetition(leagueId: String?) { _competition.value = leagueId }

    private val _events = Channel<AppEvent>(Channel.BUFFERED)
    val events = _events.receiveAsFlow()

    val usesMockData: Boolean get() = c.usesMockData
    val mockBehavior get() = c.mockBehavior

    private fun update(block: (UserPrefs) -> UserPrefs) = viewModelScope.launch { c.prefs.update(block) }
    private fun say(text: String) { _events.trySend(AppEvent.Message(text)) }

    fun toggleTeam(teamId: String, teamName: String) = viewModelScope.launch {
        val current = c.prefs.prefs.first()
        when (val r = PrefsReducer.toggleTeam(current, teamId)) {
            PrefsReducer.FollowResult.LimitReached -> {
                say("Free accounts follow up to ${UserPrefs.FREE_TEAM_LIMIT} teams.")
                _events.trySend(AppEvent.OpenPaywall)
            }
            is PrefsReducer.FollowResult.Ok -> {
                c.prefs.update { PrefsReducer.toggleTeam(it, teamId).let { res -> (res as? PrefsReducer.FollowResult.Ok)?.prefs ?: it } }
                say(if (r.following) "Following $teamName" else "Unfollowed $teamName")
            }
        }
    }

    fun toggleLeague(id: String, name: String) {
        val following = prefs.value?.leagues?.contains(id) != true
        update { PrefsReducer.toggleLeague(it, id) }
        say(if (following) "Following $name" else "Unfollowed $name")
    }

    fun togglePlayer(id: String, name: String) {
        val following = prefs.value?.players?.contains(id) != true
        update { PrefsReducer.togglePlayer(it, id) }
        say(if (following) "Following $name" else "Unfollowed $name")
    }

    fun toggleMatchAlert(id: String) {
        val on = prefs.value?.matchAlerts?.contains(id) != true
        update { PrefsReducer.toggleMatchAlert(it, id) }
        say(if (on) "Match alerts on" else "Match alerts off")
    }

    fun toggleShield(teamId: String) = update { PrefsReducer.toggleShield(it, teamId) }
    fun setShieldAll(on: Boolean) = update { PrefsReducer.shieldAll(it, on) }
    fun setTeamAlert(teamId: String, level: AlertLevel) = update { PrefsReducer.setTeamAlert(it, teamId, level) }
    fun reveal(matchId: String) = update { PrefsReducer.reveal(it, matchId) }
    fun caughtUp(matchIds: Collection<String>) { update { PrefsReducer.caughtUp(it, matchIds) }; say("Scores revealed. Held alerts released.") }

    fun isHidden(p: UserPrefs?, m: Match) = p != null && PrefsReducer.isHidden(p, m.id, m.home.id, m.away.id, m.hasScore)

    // Settings
    fun setTheme(t: ThemeChoice) = update { it.copy(theme = t) }
    fun setLanguage(code: String) = update { it.copy(language = code) }
    fun setAlertLevel(l: AlertLevel) = update { it.copy(alertLevel = l) }
    fun setQuietHours(on: Boolean) = update { it.copy(quietHours = on) }
    fun setDetailed(on: Boolean) = update { it.copy(detailed = on) }
    fun setPro(on: Boolean) { update { it.copy(isPro = on) }; say(if (on) "Pro preview on" else "Pro preview off") }

    // Onboarding
    fun setFollowKind(k: FollowKind) = update { it.copy(followKind = k) }
    fun setWatchStyle(w: WatchStyle) = update { it.copy(watchStyle = w) }
    fun setOnboardingTeams(ids: List<String>) = update { p -> p.copy(teams = ids.take(UserPrefs.FREE_TEAM_LIMIT)) }
    fun completeOnboarding() = update(PrefsReducer::completeOnboarding)

    fun resetAll() = viewModelScope.launch {
        c.prefs.reset()
        c.auth.signOut()
        say("App data reset")
    }

    fun signOut() = viewModelScope.launch { c.auth.signOut(); say("Signed out") }

    fun message(text: String) = say(text)

    // Search history (DataStore, local only)
    fun addRecentSearch(q: String) = update { PrefsReducer.addRecentSearch(it, q) }
    fun removeRecentSearch(q: String) = update { PrefsReducer.removeRecentSearch(it, q) }
    fun clearRecentSearches() = update(PrefsReducer::clearRecentSearches)

    companion object {
        val Factory = containerFactory { c, _ -> AppViewModel(c) }
    }
}
