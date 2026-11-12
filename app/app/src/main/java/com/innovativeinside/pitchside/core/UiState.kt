package com.innovativeinside.pitchside.core

/**
 * The one state type every data-driven screen renders. Loading, Empty and Error are first-class, so a
 * screen can never show a blank page: it always has something to say and, on error, a way to retry.
 */
sealed interface UiState<out T> {
    data object Loading : UiState<Nothing>
    data object Empty : UiState<Nothing>
    data class Success<T>(val data: T) : UiState<T>
    data class Error(val error: AppError) : UiState<Nothing>
}

val <T> UiState<T>.dataOrNull: T? get() = (this as? UiState.Success)?.data

/** Maps a repository result into a UI state, treating an "empty" value as [UiState.Empty]. */
fun <T> Result<T>.toUiState(isEmpty: (T) -> Boolean = { (it as? Collection<*>)?.isEmpty() == true }): UiState<T> =
    fold(
        onSuccess = { if (isEmpty(it)) UiState.Empty else UiState.Success(it) },
        onFailure = { UiState.Error(AppError.from(it)) },
    )
