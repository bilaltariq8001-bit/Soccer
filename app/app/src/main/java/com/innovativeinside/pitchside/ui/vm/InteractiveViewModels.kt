package com.innovativeinside.pitchside.ui.vm

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.innovativeinside.pitchside.AppContainer
import com.innovativeinside.pitchside.core.AppError
import com.innovativeinside.pitchside.core.Validators
import com.innovativeinside.pitchside.data.auth.AuthRepository
import com.innovativeinside.pitchside.data.model.NewsCategory
import com.innovativeinside.pitchside.data.model.NewsItem
import com.innovativeinside.pitchside.data.model.SearchResults
import com.innovativeinside.pitchside.data.prefs.PrefsReducer
import com.innovativeinside.pitchside.data.prefs.PrefsRepository
import com.innovativeinside.pitchside.data.repository.FootballRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

// ---------------- News (paginated) ----------------

data class NewsState(
    val category: NewsCategory? = null,
    val items: List<NewsItem> = emptyList(),
    val loading: Boolean = true,
    val loadingMore: Boolean = false,
    val refreshing: Boolean = false,
    val error: AppError? = null,
    val loadMoreError: AppError? = null,
    val nextCursor: String? = null,
) {
    val endReached: Boolean get() = !loading && nextCursor == null
}

class NewsViewModel(private val repo: FootballRepository) : ViewModel() {
    private val _state = MutableStateFlow(NewsState())
    val state: StateFlow<NewsState> = _state.asStateFlow()
    private var job: Job? = null

    init { reload() }

    fun setCategory(c: NewsCategory?) { if (c != _state.value.category) { _state.value = NewsState(category = c); reload() } }

    fun refresh() = reload(pull = true)
    fun retry() = reload()

    private fun reload(pull: Boolean = false) {
        job?.cancel()
        _state.update { it.copy(loading = !pull || it.items.isEmpty(), refreshing = pull, error = null, loadMoreError = null) }
        job = viewModelScope.launch {
            repo.news(_state.value.category, null)
                .onSuccess { page -> _state.update { it.copy(items = page.items, nextCursor = page.nextCursor, loading = false, refreshing = false) } }
                .onFailure { e -> _state.update { it.copy(loading = false, refreshing = false, error = AppError.from(e)) } }
        }
    }

    fun loadMore() {
        val s = _state.value
        if (s.loading || s.loadingMore || s.nextCursor == null) return
        _state.update { it.copy(loadingMore = true, loadMoreError = null) }
        job = viewModelScope.launch {
            repo.news(s.category, s.nextCursor)
                .onSuccess { page -> _state.update { it.copy(items = (it.items + page.items).distinctBy { n -> n.id }, nextCursor = page.nextCursor, loadingMore = false) } }
                .onFailure { e -> _state.update { it.copy(loadingMore = false, loadMoreError = AppError.from(e)) } }
        }
    }

    companion object { val Factory = containerFactory { c, _ -> NewsViewModel(c.football) } }
}

// ---------------- Search ----------------

sealed interface SearchState {
    data object Idle : SearchState
    data object Loading : SearchState
    data class Empty(val query: String) : SearchState
    data class Results(val query: String, val results: SearchResults) : SearchState
    data class Error(val query: String, val error: AppError) : SearchState
}

enum class SearchTab { All, Teams, Leagues, Players }

/** Live search: debounced, latest query wins (older requests are cancelled by flatMapLatest). */
@OptIn(FlowPreview::class, ExperimentalCoroutinesApi::class)
class SearchViewModel(private val repo: FootballRepository, debounceMs: Long = 300) : ViewModel() {
    private val _query = MutableStateFlow("")
    val query: StateFlow<String> = _query.asStateFlow()
    private val _tab = MutableStateFlow(SearchTab.All)
    val tab: StateFlow<SearchTab> = _tab.asStateFlow()
    private val retryTick = MutableStateFlow(0)

    val state: StateFlow<SearchState> = _query
        .map { it.trim() }
        .debounce(debounceMs)
        .distinctUntilChanged()
        .let { q -> kotlinx.coroutines.flow.combine(q, retryTick) { a, _ -> a } }
        .flatMapLatest { q ->
            if (q.length < 2) flow { emit(SearchState.Idle) }
            else flow {
                val r = repo.search(q)
                emit(r.fold({ if (it.isEmpty) SearchState.Empty(q) else SearchState.Results(q, it) }, { SearchState.Error(q, AppError.from(it)) }))
            }.onStart { emit(SearchState.Loading) }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), SearchState.Idle)

    fun onQuery(q: String) { _query.value = q.take(60) }
    fun setTab(t: SearchTab) { _tab.value = t }
    fun retry() { retryTick.update { it + 1 } }

    companion object { val Factory = containerFactory { c, _ -> SearchViewModel(c.football) } }
}

// ---------------- Ask ----------------

data class ChatMessage(val fromUser: Boolean, val text: String, val failed: Boolean = false)

data class AskState(
    val messages: List<ChatMessage> = emptyList(),
    val input: String = "",
    val sending: Boolean = false,
    val left: Int = 0,
    val limit: Int = 0,
    val isPro: Boolean = false,
    val quotaHit: Boolean = false,
    val inputError: String? = null,
)

class AskViewModel(
    private val repo: FootballRepository,
    private val prefs: PrefsRepository,
    private val epochDay: () -> Long,
) : ViewModel() {
    private val _state = MutableStateFlow(AskState())
    val state: StateFlow<AskState> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            prefs.prefs.collect { p -> _state.update { it.copy(left = PrefsReducer.askLeft(p, epochDay()), limit = PrefsReducer.askLimit(p), isPro = p.isPro) } }
        }
    }

    fun onInput(text: String) = _state.update { it.copy(input = text.take(300), inputError = null) }
    fun dismissQuota() = _state.update { it.copy(quotaHit = false) }

    fun send(text: String = _state.value.input) {
        val q = text.trim()
        if (_state.value.sending) return
        if (q.isEmpty()) { _state.update { it.copy(inputError = "Type a question first.") }; return }
        viewModelScope.launch {
            val p = prefs.prefs.first()
            val language = p.language
            if (PrefsReducer.useAsk(p, epochDay()) == null) { _state.update { it.copy(quotaHit = true) }; return@launch }
            _state.update { it.copy(messages = it.messages + ChatMessage(true, q), input = "", sending = true) }
            repo.ask(q, language)
                .onSuccess { answer ->
                    prefs.update { cur -> PrefsReducer.useAsk(cur, epochDay()) ?: cur }
                    _state.update { it.copy(messages = it.messages + ChatMessage(false, answer), sending = false) }
                }
                .onFailure { e ->
                    // A failed answer does not use up a question.
                    _state.update { it.copy(messages = it.messages + ChatMessage(false, AppError.from(e).message, failed = true), sending = false) }
                }
        }
    }

    /** Re-asks the last question after a failure. */
    fun retryLast() {
        val last = _state.value.messages.lastOrNull { it.fromUser } ?: return
        _state.update { s -> s.copy(messages = s.messages.dropLastWhile { !it.fromUser }.dropLast(1)) }
        send(last.text)
    }

    companion object { val Factory = containerFactory { c, _ -> AskViewModel(c.football, c.prefs) { c.clock.today().toEpochDay() } } }
}

// ---------------- Auth ----------------

enum class AuthMode { SignIn, SignUp, Reset }

data class AuthForm(
    val mode: AuthMode = AuthMode.SignIn,
    val name: String = "",
    val email: String = "",
    val password: String = "",
    val confirm: String = "",
    val nameError: String? = null,
    val emailError: String? = null,
    val passwordError: String? = null,
    val confirmError: String? = null,
    val submitting: Boolean = false,
    val formError: String? = null,
    val resetSent: Boolean = false,
    val done: Boolean = false,
)

class AuthViewModel(private val auth: AuthRepository, initialMode: AuthMode = AuthMode.SignIn) : ViewModel() {
    private val _form = MutableStateFlow(AuthForm(mode = initialMode))
    val form: StateFlow<AuthForm> = _form.asStateFlow()

    val session = auth.session.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    fun setMode(m: AuthMode) = _form.update { AuthForm(mode = m, email = it.email) }
    fun onName(v: String) = _form.update { it.copy(name = v, nameError = null, formError = null) }
    fun onEmail(v: String) = _form.update { it.copy(email = v.trim(), emailError = null, formError = null) }
    fun onPassword(v: String) = _form.update { it.copy(password = v, passwordError = null, confirmError = null, formError = null) }
    fun onConfirm(v: String) = _form.update { it.copy(confirm = v, confirmError = null, formError = null) }

    /** Validates locally first, so the user sees every field error at once without a network call. */
    fun validate(): Boolean {
        val f = _form.value
        val nameErr = if (f.mode == AuthMode.SignUp) Validators.displayName(f.name) else null
        val emailErr = Validators.email(f.email)
        val pwErr = when (f.mode) { AuthMode.SignIn -> Validators.loginPassword(f.password); AuthMode.SignUp -> Validators.password(f.password); AuthMode.Reset -> null }
        val confirmErr = if (f.mode == AuthMode.SignUp) Validators.confirm(f.password, f.confirm) else null
        _form.update { it.copy(nameError = nameErr, emailError = emailErr, passwordError = pwErr, confirmError = confirmErr) }
        return listOf(nameErr, emailErr, pwErr, confirmErr).all { it == null }
    }

    fun submit() {
        if (_form.value.submitting || !validate()) return
        val f = _form.value
        _form.update { it.copy(submitting = true, formError = null) }
        viewModelScope.launch {
            val result: Result<Any> = when (f.mode) {
                AuthMode.SignIn -> auth.signIn(f.email, f.password)
                AuthMode.SignUp -> auth.signUp(f.name, f.email, f.password)
                AuthMode.Reset -> auth.sendPasswordReset(f.email)
            }
            result
                .onSuccess { _form.update { it.copy(submitting = false, password = "", confirm = "", resetSent = f.mode == AuthMode.Reset, done = f.mode != AuthMode.Reset) } }
                .onFailure { e -> _form.update { it.copy(submitting = false, formError = AppError.from(e).message) } }
        }
    }

    fun signOut() = viewModelScope.launch { auth.signOut() }

    companion object {
        fun factory(mode: AuthMode) = containerFactory { c: AppContainer, _ -> AuthViewModel(c.auth, mode) }
    }
}
