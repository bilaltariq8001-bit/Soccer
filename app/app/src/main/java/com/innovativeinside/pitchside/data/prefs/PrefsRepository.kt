package com.innovativeinside.pitchside.data.prefs

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.stringPreferencesKey
import java.io.IOException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.serialization.json.Json

interface PrefsRepository {
    val prefs: Flow<UserPrefs>
    /** Atomic read-modify-write. */
    suspend fun update(transform: (UserPrefs) -> UserPrefs)
    suspend fun reset()
}

/**
 * DataStore-backed preferences. DataStore does its IO off the main thread and serializes writes, so
 * concurrent [update] calls cannot lose data (the old SharedPreferences snapshot loop could).
 */
class DataStorePrefsRepository(private val store: DataStore<Preferences>) : PrefsRepository {
    private val key = stringPreferencesKey("user_prefs_v1")
    private val json = Json { ignoreUnknownKeys = true; encodeDefaults = true }

    private fun decode(s: String?): UserPrefs = s?.let { runCatching { json.decodeFromString(UserPrefs.serializer(), it) }.getOrNull() } ?: UserPrefs()

    override val prefs: Flow<UserPrefs> = store.data
        .catch { e -> if (e is IOException) emit(emptyPreferences()) else throw e }
        .map { decode(it[key]) }
        .distinctUntilChanged()

    override suspend fun update(transform: (UserPrefs) -> UserPrefs) {
        store.edit { p -> p[key] = json.encodeToString(UserPrefs.serializer(), transform(decode(p[key]))) }
    }

    override suspend fun reset() { store.edit { it.clear() } }
}

/** In-memory preferences for tests and previews. */
class InMemoryPrefsRepository(initial: UserPrefs = UserPrefs()) : PrefsRepository {
    private val state = MutableStateFlow(initial)
    val current: StateFlow<UserPrefs> = state.asStateFlow()
    override val prefs: Flow<UserPrefs> = state
    override suspend fun update(transform: (UserPrefs) -> UserPrefs) = state.update(transform)
    override suspend fun reset() { state.value = UserPrefs() }
}
