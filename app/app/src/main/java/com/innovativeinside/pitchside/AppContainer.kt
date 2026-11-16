package com.innovativeinside.pitchside

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.preferencesDataStore
import com.innovativeinside.pitchside.core.AppClock
import com.innovativeinside.pitchside.core.DefaultDispatchers
import com.innovativeinside.pitchside.core.DispatcherProvider
import com.innovativeinside.pitchside.core.SystemClock
import com.innovativeinside.pitchside.data.auth.AuthRepository
import com.innovativeinside.pitchside.data.auth.KeystoreCipher
import com.innovativeinside.pitchside.data.auth.LocalAuthRepository
import com.innovativeinside.pitchside.data.mock.MockBehavior
import com.innovativeinside.pitchside.data.mock.MockFootballApi
import com.innovativeinside.pitchside.data.news.NinetyMinNewsSource
import com.innovativeinside.pitchside.data.prefs.DataStorePrefsRepository
import com.innovativeinside.pitchside.data.prefs.PrefsRepository
import com.innovativeinside.pitchside.data.remote.ApiFootballComApi
import com.innovativeinside.pitchside.data.remote.FootballApi
import com.innovativeinside.pitchside.data.remote.LiveScoreSocket
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import com.innovativeinside.pitchside.data.remote.HttpFootballApi
import com.innovativeinside.pitchside.data.repository.DefaultFootballRepository
import com.innovativeinside.pitchside.data.repository.FootballRepository

private val Context.prefsStore: DataStore<Preferences> by preferencesDataStore(name = "pitchside_prefs")
private val Context.authStore: DataStore<Preferences> by preferencesDataStore(name = "pitchside_auth")

/** Manual dependency container (no DI framework needed at this size). One instance per process. */
interface AppContainer {
    val clock: AppClock
    val dispatchers: DispatcherProvider
    val prefs: PrefsRepository
    val auth: AuthRepository
    val football: FootballRepository
    /** Non-null when the app runs on mock data; lets Settings > Developer simulate failures. */
    val mockBehavior: MockBehavior?
    val usesMockData: Boolean get() = mockBehavior != null
    /** Ids of matches that just changed, pushed by the livescore socket (empty flow when there is none). */
    val liveUpdates: Flow<Set<String>> get() = emptyFlow()
    /** Called from the activity's onStart / onStop so the live socket only runs in the foreground. */
    fun onForeground() {}
    fun onBackground() {}
}

class DefaultAppContainer(context: Context) : AppContainer {
    private val app = context.applicationContext
    override val clock: AppClock = SystemClock
    override val dispatchers: DispatcherProvider = DefaultDispatchers
    override val prefs: PrefsRepository = DataStorePrefsRepository(app.prefsStore)
    override val auth: AuthRepository = LocalAuthRepository(app.authStore, KeystoreCipher(), clock)

    private val apifootballKey = BuildConfig.APIFOOTBALL_KEY.trim()

    /** Real data comes from apifootball.com when a key is built in, else the Pitchside gateway; mock only without either. */
    override val mockBehavior: MockBehavior? =
        if (apifootballKey.isBlank() && BuildConfig.API_BASE_URL.isBlank()) MockBehavior() else null

    private val apiFootball: ApiFootballComApi? = apifootballKey.takeIf { it.isNotBlank() }?.let { ApiFootballComApi(it, zone = { clock.zone() }) }
    private val socket: LiveScoreSocket? = apiFootball?.let { LiveScoreSocket(apifootballKey, it) }

    private val api: FootballApi = apiFootball ?: mockBehavior?.let { MockFootballApi(clock, it) } ?: HttpFootballApi(
        baseUrl = BuildConfig.API_BASE_URL,
        tokenProvider = { auth.accessToken() },
        onUnauthorized = { auth.onUnauthorized() },
        clientVersion = "android/${BuildConfig.VERSION_NAME}",
    )

    override val liveUpdates: Flow<Set<String>> = socket?.updates ?: emptyFlow()
    override fun onForeground() { socket?.start() }
    override fun onBackground() { socket?.stop() }

    // News always comes from the public 90min RSS feeds, also when the rest of the app runs on mock data.
    override val football: FootballRepository = DefaultFootballRepository(api, dispatchers, clock, newsSource = NinetyMinNewsSource())
}
