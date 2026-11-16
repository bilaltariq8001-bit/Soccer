package com.innovativeinside.pitchside.ui.vm

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.innovativeinside.pitchside.core.AppError
import com.innovativeinside.pitchside.core.UiState
import com.innovativeinside.pitchside.core.toUiState
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull

/** Upper bound for one data-layer call (see DefaultFootballRepository): past it the call fails with a Timeout. */
const val LOAD_TIMEOUT_MS = com.innovativeinside.pitchside.data.repository.CALL_TIMEOUT_MS

/**
 * Runs [fetch] and turns every failure (unexpected exception, optional [timeoutMs] limit) into a [Result.failure] of
 * [AppError]. Cancellation still propagates, so a newer load can replace this one.
 *
 * The time limit for network work lives in the data layer, on the IO dispatcher; [timeoutMs] here is for callers whose
 * clock is known to be safe for timers (a main-thread timer is skipped ahead by Robolectric's paused looper).
 */
suspend fun <T> boundedFetch(timeoutMs: Long? = null, fetch: suspend () -> Result<T>): Result<T> = try {
    if (timeoutMs == null) fetch() else withTimeoutOrNull(timeoutMs) { fetch() } ?: Result.failure(AppError.Timeout())
} catch (c: CancellationException) {
    throw c
} catch (t: Throwable) {
    Result.failure(AppError.from(t))
}

/**
 * Base for screens that load one thing: exposes [state] (Loading, Empty, Error, Success), [refreshing]
 * for pull-to-refresh, and [refreshError] when a refresh fails but older data is still on screen.
 *
 * Guarantees:
 * - The pull-to-refresh indicator ([refreshing]) is only ever on while a user refresh of data already on screen is
 *   running. It is turned off in a finally block, so it ends on success, error, empty, timeout and cancellation.
 * - The first load (and any load with nothing on screen) shows the skeleton ([UiState.Loading]), never the indicator.
 * - Every network call under [fetch] is time-limited in the data layer, so a load always finishes; exceptions become
 *   an error state instead of a stuck coroutine.
 * - Only the newest load may publish. Background refreshes (polling, the live socket) never cancel a user load and
 *   never publish over a newer one (e.g. after the user picked another date).
 */
abstract class LoadViewModel<T> : ViewModel() {
    private val _state = MutableStateFlow<UiState<T>>(UiState.Loading)
    val state: StateFlow<UiState<T>> = _state.asStateFlow()

    private val _refreshing = MutableStateFlow(false)
    val refreshing: StateFlow<Boolean> = _refreshing.asStateFlow()

    private val _refreshError = MutableStateFlow<AppError?>(null)
    val refreshError: StateFlow<AppError?> = _refreshError.asStateFlow()

    private var loadJob: Job? = null
    private var silentJob: Job? = null
    private var pollJob: Job? = null
    /** Bumped by every user-visible load; a result is published only if its generation is still current. */
    private var generation = 0L

    protected abstract suspend fun fetch(refresh: Boolean): Result<T>
    protected open fun isEmpty(data: T): Boolean = (data as? Collection<*>)?.isEmpty() == true
    /** Return a delay to keep refreshing in the background (for live data), or null to stop. */
    protected open fun pollIntervalMs(data: T): Long? = null
    /**
     * Optional extra limit for a whole load, measured on the main dispatcher. Null by default: every network call is
     * already bounded in the data layer, and a main-thread timer is unreliable under Robolectric.
     */
    protected open val loadTimeoutMs: Long? = null
    /** Called after every published result (success, empty or error) of a user-visible load. */
    protected open fun onLoaded(state: UiState<T>) {}

    fun load(refresh: Boolean = false) {
        loadJob?.cancel()
        silentJob?.cancel()
        pollJob?.cancel()
        val token = ++generation
        // State changes synchronously, so the UI shows the indicator or skeleton in the same frame as the tap.
        val hadData = _state.value is UiState.Success
        val pull = refresh && hadData
        _refreshing.value = pull
        if (!pull) _state.value = UiState.Loading
        _refreshError.value = null
        loadJob = viewModelScope.launch {
            try {
                val next = boundedFetch(loadTimeoutMs) { fetch(refresh) }.toUiState(::isEmpty)
                if (token != generation) return@launch
                if (next is UiState.Error && pull) _refreshError.value = next.error else _state.value = next
                onLoaded(_state.value)
                schedulePoll()
            } finally {
                // Runs on success, error, timeout and cancellation. A newer load owns the flag once it has started.
                if (token == generation) _refreshing.value = false
            }
        }
    }

    fun refresh() = load(refresh = true)
    fun retry() = load(refresh = true)
    fun dismissRefreshError() { _refreshError.value = null }

    private fun schedulePoll() {
        pollJob?.cancel()
        val data = (_state.value as? UiState.Success)?.data ?: return
        val interval = pollIntervalMs(data) ?: return
        pollJob = viewModelScope.launch {
            delay(interval)
            if (isActive) silentRefresh()
        }
    }

    /**
     * Collects a push stream (the livescore socket) and refreshes silently when [shouldReload] says the change matters
     * to what is on screen. Bursts are coalesced: at most one reload every [minGapMs].
     */
    protected fun reloadOn(updates: kotlinx.coroutines.flow.Flow<Set<String>>, minGapMs: Long = 5_000, shouldReload: (T, Set<String>) -> Boolean) {
        viewModelScope.launch {
            var last = 0L
            updates.collect { ids ->
                val data = (_state.value as? UiState.Success)?.data ?: return@collect
                val now = System.currentTimeMillis()
                if (now - last >= minGapMs && shouldReload(data, ids)) { last = now; silentRefresh() }
            }
        }
    }

    /**
     * Background refresh: never shows an indicator, never replaces good data with an error, never cancels a load the
     * user started (it is skipped while one runs) and never publishes after a newer load began.
     */
    private fun silentRefresh() {
        if (loadJob?.isActive == true || silentJob?.isActive == true) return
        val token = generation
        silentJob = viewModelScope.launch {
            boundedFetch(loadTimeoutMs) { fetch(true) }.onSuccess {
                if (token == generation) _state.value = if (isEmpty(it)) UiState.Empty else UiState.Success(it)
            }
            if (token == generation) schedulePoll()
        }
    }
}
