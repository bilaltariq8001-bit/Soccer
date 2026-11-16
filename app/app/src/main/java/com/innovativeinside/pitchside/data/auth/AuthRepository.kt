package com.innovativeinside.pitchside.data.auth

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.stringPreferencesKey
import com.innovativeinside.pitchside.core.AppClock
import com.innovativeinside.pitchside.core.AppError
import com.innovativeinside.pitchside.core.Validators
import java.io.IOException
import java.security.MessageDigest
import java.util.UUID
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.serialization.Serializable
import kotlinx.serialization.builtins.MapSerializer
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.json.Json

/** A signed-in user. The access token is never shown in the UI or logged. */
@Serializable
data class Session(val userId: String, val email: String, val displayName: String, val expiresAtMs: Long)

/**
 * Account sign-in. The app works fully as a guest (anonymous-first, as planned); an account adds backup
 * of follows and settings. Swap [LocalAuthRepository] for a Firebase Auth implementation once
 * google-services.json exists (see FINAL_REPORT.md).
 */
interface AuthRepository {
    /** Current session, or null when signed out or expired. */
    val session: Flow<Session?>
    suspend fun signIn(email: String, password: String): Result<Session>
    suspend fun signUp(name: String, email: String, password: String): Result<Session>
    suspend fun sendPasswordReset(email: String): Result<Unit>
    suspend fun signOut()
    /** Bearer token for API calls, or null. Expired sessions return null and are cleared. */
    suspend fun accessToken(): String?
    /** Called by the network layer on HTTP 401. */
    suspend fun onUnauthorized()
}

/** Encrypts stored secrets. [KeystoreCipher] on device; [PlainCipher] in JVM tests. */
interface SecretCipher {
    fun encrypt(plain: String): String
    fun decrypt(stored: String): String?
}

object PlainCipher : SecretCipher {
    override fun encrypt(plain: String) = plain
    override fun decrypt(stored: String): String? = stored
}

/**
 * Device-local account store. Accounts are kept as salted SHA-256 hashes (never plain passwords), and the
 * session token is encrypted with an Android Keystore key before it touches disk.
 */
class LocalAuthRepository(
    private val store: DataStore<Preferences>,
    private val cipher: SecretCipher,
    private val clock: AppClock,
    private val latencyMs: Long = 600,
    private val sessionDays: Long = 30,
) : AuthRepository {

    private val sessionKey = stringPreferencesKey("session")
    private val tokenKey = stringPreferencesKey("token")
    private val accountsKey = stringPreferencesKey("accounts")
    private val json = Json { ignoreUnknownKeys = true }

    @Serializable
    private data class Account(val userId: String, val name: String, val salt: String, val hash: String)

    private val accountsSer = MapSerializer(String.serializer(), Account.serializer())

    private fun hash(salt: String, password: String): String =
        MessageDigest.getInstance("SHA-256").digest("$salt:$password".toByteArray()).joinToString("") { "%02x".format(it) }

    private fun decodeSession(s: String?): Session? = s?.let { runCatching { json.decodeFromString(Session.serializer(), it) }.getOrNull() }

    override val session: Flow<Session?> = store.data
        .catch { e -> if (e is IOException) emit(emptyPreferences()) else throw e }
        .map { p -> decodeSession(p[sessionKey])?.takeIf { it.expiresAtMs > clock.nowMs() } }
        .distinctUntilChanged()

    private suspend fun accounts(): Map<String, Account> =
        store.data.first()[accountsKey]?.let { runCatching { json.decodeFromString(accountsSer, it) }.getOrNull() } ?: emptyMap()

    private suspend fun startSession(email: String, acc: Account): Session {
        val s = Session(acc.userId, email, acc.name, clock.nowMs() + sessionDays * 86_400_000L)
        val token = UUID.randomUUID().toString()
        store.edit {
            it[sessionKey] = json.encodeToString(Session.serializer(), s)
            it[tokenKey] = cipher.encrypt(token)
        }
        return s
    }

    override suspend fun signIn(email: String, password: String): Result<Session> {
        Validators.email(email)?.let { return Result.failure(AppError.Validation(it)) }
        Validators.loginPassword(password)?.let { return Result.failure(AppError.Validation(it)) }
        delay(latencyMs)
        val key = email.trim().lowercase()
        val acc = accounts()[key] ?: return Result.failure(AppError.Validation("No account uses that email. Create one instead."))
        if (hash(acc.salt, password) != acc.hash) return Result.failure(AppError.Validation("That password isn't right. Try again or reset it."))
        return Result.success(startSession(key, acc))
    }

    override suspend fun signUp(name: String, email: String, password: String): Result<Session> {
        (Validators.displayName(name) ?: Validators.email(email) ?: Validators.password(password))?.let { return Result.failure(AppError.Validation(it)) }
        delay(latencyMs)
        val key = email.trim().lowercase()
        val existing = accounts()
        if (key in existing) return Result.failure(AppError.Validation("An account already uses that email. Sign in instead."))
        val salt = UUID.randomUUID().toString()
        val acc = Account(UUID.randomUUID().toString(), name.trim(), salt, hash(salt, password))
        store.edit { it[accountsKey] = json.encodeToString(accountsSer, existing + (key to acc)) }
        return Result.success(startSession(key, acc))
    }

    override suspend fun sendPasswordReset(email: String): Result<Unit> {
        Validators.email(email)?.let { return Result.failure(AppError.Validation(it)) }
        delay(latencyMs)
        // Always succeed, whether or not the account exists, so the form can't be used to probe emails.
        return Result.success(Unit)
    }

    override suspend fun signOut() { store.edit { it.remove(sessionKey); it.remove(tokenKey) } }

    override suspend fun accessToken(): String? {
        val p = store.data.first()
        val s = decodeSession(p[sessionKey]) ?: return null
        if (s.expiresAtMs <= clock.nowMs()) { signOut(); return null }
        return p[tokenKey]?.let(cipher::decrypt)
    }

    override suspend fun onUnauthorized() = signOut()
}

/** In-memory auth for previews and tests. Same validation rules as [LocalAuthRepository]. */
class InMemoryAuthRepository(private val clock: AppClock) : AuthRepository {
    private val state = kotlinx.coroutines.flow.MutableStateFlow<Session?>(null)
    private val accounts = mutableMapOf<String, Pair<String, String>>() // email -> (name, password)
    private var token: String? = null
    override val session: Flow<Session?> = state

    override suspend fun signIn(email: String, password: String): Result<Session> {
        Validators.email(email)?.let { return Result.failure(AppError.Validation(it)) }
        Validators.loginPassword(password)?.let { return Result.failure(AppError.Validation(it)) }
        val acc = accounts[email.trim().lowercase()] ?: return Result.failure(AppError.Validation("No account uses that email. Create one instead."))
        if (acc.second != password) return Result.failure(AppError.Validation("That password isn't right. Try again or reset it."))
        return Result.success(start(email, acc.first))
    }

    override suspend fun signUp(name: String, email: String, password: String): Result<Session> {
        (Validators.displayName(name) ?: Validators.email(email) ?: Validators.password(password))?.let { return Result.failure(AppError.Validation(it)) }
        val key = email.trim().lowercase()
        if (key in accounts) return Result.failure(AppError.Validation("An account already uses that email. Sign in instead."))
        accounts[key] = name.trim() to password
        return Result.success(start(key, name.trim()))
    }

    private fun start(email: String, name: String): Session {
        token = UUID.randomUUID().toString()
        return Session("u-${email.hashCode()}", email.trim().lowercase(), name, clock.nowMs() + 86_400_000L).also { state.value = it }
    }

    override suspend fun sendPasswordReset(email: String): Result<Unit> =
        Validators.email(email)?.let { Result.failure(AppError.Validation(it)) } ?: Result.success(Unit)

    override suspend fun signOut() { state.value = null; token = null }
    override suspend fun accessToken(): String? = token.takeIf { state.value != null }
    override suspend fun onUnauthorized() = signOut()
}
