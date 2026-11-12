package com.innovativeinside.pitchside.ui.vm

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.innovativeinside.pitchside.core.AppError
import com.innovativeinside.pitchside.core.UiState
import com.innovativeinside.pitchside.core.toUiState
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

/**
 * Base for screens that load one thing: exposes [state] (Loading, Empty, Error, Success), [refreshing]
 * for pull-to-refresh, and [refreshError] when a refresh fails but older data is still on screen.
 * Only one load runs at a time; a new load cancels the previous one, so results can't arrive out of order.
 * Subclasses call [load] from their init block.
 */
abstract class LoadViewModel<T> : ViewModel() {
    private val _state = MutableStateFlow<UiState<T>>(UiState.Loading)
    val state: StateFlow<UiState<T>> = _state.asStateFlow()

    private val _refreshing = MutableStateFlow(false)
    val refreshing: StateFlow<Boolean> = _refreshing.asStateFlow()

    private val _refreshError = MutableStateFlow<AppError?>(null)
    val refreshError: StateFlow<AppError?> = _refreshError.asStateFlow()

    private var loadJob: Job? = null
    private var pollJob: Job? = null

    protected abstract suspend fun fetch(refresh: Boolean): Result<T>
    protected open fun isEmpty(data: T): Boolean = (data as? Collection<*>)?.isEmpty() == true
    /** Return a delay to keep refreshing in the background (for live data), or null to stop. */
    protected open fun pollIntervalMs(data: T): Long? = null

    fun load(refresh: Boolean = false) {
        loadJob?.cancel()
        pollJob?.cancel()
        // State changes synchronously, so the UI shows the spinner or skeleton in the same frame as the tap.
        val hadData = _state.value is UiState.Success
        if (refresh && hadData) _refreshing.value = true else _state.value = UiState.Loading
        _refreshError.value = null
        loadJob = viewModelScope.launch {
            val result = fetch(refresh)
            val next = result.toUiState(::isEmpty)
            if (next is UiState.Error && hadData && refresh) _refreshError.value = next.error else _state.value = next
            _refreshing.value = false
            schedulePoll()
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

    /** Background refresh: never shows a spinner and never replaces good data with an error. */
    private fun silentRefresh() {
        loadJob?.cancel()
        loadJob = viewModelScope.launch {
            fetch(true).onSuccess { _state.value = if (isEmpty(it)) UiState.Empty else UiState.Success(it) }
            schedulePoll()
        }
    }
}
