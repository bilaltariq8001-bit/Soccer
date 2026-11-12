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
import com.innovativeinside.pitchside.data.prefs.DataStorePrefsRepository
import com.innovativeinside.pitchside.data.prefs.PrefsRepository
import com.innovativeinside.pitchside.data.remote.FootballApi
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
}

class DefaultAppContainer(context: Context) : AppContainer {
    private val app = context.applicationContext
    override val clock: AppClock = SystemClock
    override val dispatchers: DispatcherProvider = DefaultDispatchers
    override val prefs: PrefsRepository = DataStorePrefsRepository(app.prefsStore)
    override val auth: AuthRepository = LocalAuthRepository(app.authStore, KeystoreCipher(), clock)

    override val mockBehavior: MockBehavior? = if (BuildConfig.API_BASE_URL.isBlank()) MockBehavior() else null

    private val api: FootballApi = mockBehavior?.let { MockFootballApi(clock, it) } ?: HttpFootballApi(
        baseUrl = BuildConfig.API_BASE_URL,
        tokenProvider = { auth.accessToken() },
        onUnauthorized = { auth.onUnauthorized() },
        clientVersion = "android/${BuildConfig.VERSION_NAME}",
    )

    override val football: FootballRepository = DefaultFootballRepository(api, dispatchers, clock)
}
