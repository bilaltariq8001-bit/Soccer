package com.innovativeinside.pitchside.ui

import app.cash.turbine.test
import com.innovativeinside.pitchside.core.AppError
import com.innovativeinside.pitchside.core.UiState
import com.innovativeinside.pitchside.data.auth.InMemoryAuthRepository
import com.innovativeinside.pitchside.data.mock.MockBehavior
import com.innovativeinside.pitchside.data.mock.MockFootballApi
import com.innovativeinside.pitchside.data.model.Status
import com.innovativeinside.pitchside.data.prefs.InMemoryPrefsRepository
import com.innovativeinside.pitchside.data.prefs.UserPrefs
import com.innovativeinside.pitchside.data.repository.DefaultFootballRepository
import com.innovativeinside.pitchside.testutil.FixedClock
import com.innovativeinside.pitchside.testutil.MainDispatcherRule
import com.innovativeinside.pitchside.testutil.TestDispatchers
import com.innovativeinside.pitchside.ui.vm.AskViewModel
import com.innovativeinside.pitchside.ui.vm.AuthMode
import com.innovativeinside.pitchside.ui.vm.AuthViewModel
import com.innovativeinside.pitchside.ui.vm.HomeViewModel
import com.innovativeinside.pitchside.ui.vm.MatchDetailViewModel
import com.innovativeinside.pitchside.ui.vm.MatchFilter
import com.innovativeinside.pitchside.ui.vm.MatchesViewModel
import com.innovativeinside.pitchside.ui.vm.NewsViewModel
import com.innovativeinside.pitchside.ui.vm.SearchState
import com.innovativeinside.pitchside.ui.vm.SearchViewModel
import com.innovativeinside.pitchside.ui.vm.filterMatches
import com.innovativeinside.pitchside.ui.vm.groupByLeague
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class ViewModelTest {
    @get:Rule val main = MainDispatcherRule()

    private val clock = FixedClock()
    private val behavior = MockBehavior().apply { latencyMs = 100 }
    private val repo by lazy { DefaultFootballRepository(MockFootballApi(clock, behavior), TestDispatchers(main.dispatcher), clock) }

    @Test fun homeGoesLoadingThenSuccessAndPollsWhileLive() = runTest(main.dispatcher) {
        val vm = HomeViewModel(repo) { clock.today() }
        assertEquals(UiState.Loading, vm.state.value)
        advanceTimeBy(1_000)
        val data = (vm.state.value as UiState.Success).data
        assertEquals(3, data.live.size)
        assertTrue(data.news.isNotEmpty() && data.table.isNotEmpty() && data.leagues.isNotEmpty())
        // While matches are live the screen refreshes itself in the background every 30 s, without a spinner.
        clock.now += 60_000
        behavior.offline = true
        advanceTimeBy(31_000)
        assertTrue("background refresh never replaces data with an error", vm.state.value is UiState.Success)
        vm.viewModelScope_cancel()
    }

    @Test fun homeErrorThenRetryRecovers() = runTest(main.dispatcher) {
        behavior.offline = true
        val vm = HomeViewModel(repo) { clock.today() }
        advanceTimeBy(5_000)
        val err = vm.state.value as UiState.Error
        assertTrue(err.error is AppError.Network)
        behavior.offline = false
        vm.retry()
        assertEquals(UiState.Loading, vm.state.value)
        advanceTimeBy(1_000)
        assertTrue(vm.state.value is UiState.Success)
        vm.viewModelScope_cancel()
    }

    @Test fun refreshFailureKeepsDataAndReportsBanner() = runTest(main.dispatcher) {
        val vm = MatchesViewModel(repo) { clock.today() }
        advanceTimeBy(1_000)
        assertTrue(vm.state.value is UiState.Success)
        behavior.serverErrors = true
        vm.refresh()
        runCurrent()
        assertTrue(vm.refreshing.value)
        advanceTimeBy(5_000)
        assertFalse(vm.refreshing.value)
        assertTrue(vm.state.value is UiState.Success)
        assertTrue(vm.refreshError.value is AppError.Server)
        vm.dismissRefreshError()
        assertNull(vm.refreshError.value)
        vm.viewModelScope_cancel()
    }

    @Test fun matchesDateChangeAndEmptyDay() = runTest(main.dispatcher) {
        val vm = MatchesViewModel(repo) { clock.today() }
        advanceTimeBy(1_000)
        vm.selectDate(clock.today().plusDays(20))
        assertEquals(UiState.Loading, vm.state.value)
        advanceTimeBy(1_000)
        assertEquals(UiState.Empty, vm.state.value)
        vm.selectDate(clock.today().minusDays(1))
        advanceTimeBy(1_000)
        val list = (vm.state.value as UiState.Success).data
        assertTrue(list.all { it.status == Status.Finished })
        vm.setFilter(MatchFilter.Live)
        assertEquals(MatchFilter.Live, vm.filter.value)
        vm.viewModelScope_cancel()
    }

    @Test fun filterAndGroupHelpers() = runTest(main.dispatcher) {
        val all = repo.matchesOn(clock.today()).getOrThrow()
        assertEquals(3, filterMatches(all, MatchFilter.Live, emptyList(), emptyList()).size)
        assertEquals(listOf("m1"), filterMatches(all, MatchFilter.MyTeams, listOf("ng"), emptyList()).map { it.id })
        assertEquals(setOf("m5", "m6"), filterMatches(all, MatchFilter.MyTeams, emptyList(), listOf("al")).map { it.id }.toSet())
        val groups = groupByLeague(all, listOf("st"))
        assertEquals("Second Tier", groups.first().first)
        assertEquals(all.size, groups.sumOf { it.second.size })
    }

    @Test fun matchDetailNotFoundIsNotRetryable() = runTest(main.dispatcher) {
        val vm = MatchDetailViewModel(repo, "nope")
        advanceTimeBy(5_000)
        val e = (vm.state.value as UiState.Error).error
        assertTrue(e is AppError.NotFound)
        assertFalse(e.retryable)
    }

    @Test fun matchDetailBlankIdFailsCleanly() = runTest(main.dispatcher) {
        val vm = MatchDetailViewModel(repo, "")
        advanceTimeBy(5_000)
        assertTrue(vm.state.value is UiState.Error)
    }

    @Test fun searchDebouncesAndCancelsStaleQueries() = runTest(main.dispatcher) {
        val vm = SearchViewModel(repo, debounceMs = 300)
        vm.state.test {
            assertEquals(SearchState.Idle, awaitItem())
            vm.onQuery("n")
            vm.onQuery("no")
            vm.onQuery("nor")
            advanceTimeBy(299)
            expectNoEvents()
            advanceTimeBy(2)
            assertEquals(SearchState.Loading, awaitItem())
            val r = awaitItem() as SearchState.Results
            assertEquals("nor", r.query)
            assertTrue(r.results.teams.any { it.id == "ng" })
            vm.onQuery("zzzz")
            assertEquals(SearchState.Loading, awaitItem())
            assertEquals(SearchState.Empty("zzzz"), awaitItem())
            behavior.offline = true
            vm.retry()
            assertEquals(SearchState.Loading, awaitItem())
            assertTrue(awaitItem() is SearchState.Error)
            behavior.offline = false
            vm.onQuery("")
            assertEquals(SearchState.Idle, awaitItem())
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test fun newsPaginationAndCategory() = runTest(main.dispatcher) {
        val vm = NewsViewModel(repo)
        advanceTimeBy(5_000)
        assertEquals(4, vm.state.value.items.size)
        assertNotNull(vm.state.value.nextCursor)
        vm.loadMore()
        assertTrue(vm.state.value.loadingMore)
        advanceTimeBy(5_000)
        assertEquals(8, vm.state.value.items.size)
        assertTrue(vm.state.value.endReached)
        vm.loadMore() // no-op at the end
        assertFalse(vm.state.value.loadingMore)
        vm.setCategory(com.innovativeinside.pitchside.data.model.NewsCategory.Transfers)
        advanceTimeBy(5_000)
        assertTrue(vm.state.value.items.all { it.category == com.innovativeinside.pitchside.data.model.NewsCategory.Transfers })
        behavior.offline = true
        vm.refresh()
        advanceTimeBy(5_000)
        assertTrue(vm.state.value.error is AppError.Network)
    }

    @Test fun askUsesQuotaOnlyOnSuccessAndStopsAtLimit() = runTest(main.dispatcher) {
        val prefs = InMemoryPrefsRepository(UserPrefs(askDay = clock.today().toEpochDay(), askUsed = UserPrefs.FREE_ASK_LIMIT - 1))
        val vm = AskViewModel(repo, prefs) { clock.today().toEpochDay() }
        advanceTimeBy(5_000)
        assertEquals(1, vm.state.value.left)
        vm.send("   ")
        assertEquals("Type a question first.", vm.state.value.inputError)

        behavior.offline = true
        vm.send("Who leads the table?")
        advanceTimeBy(5_000)
        assertTrue(vm.state.value.messages.last().failed)
        assertEquals("failed answers don't use a question", 1, vm.state.value.left)

        behavior.offline = false
        vm.retryLast()
        advanceTimeBy(5_000)
        assertFalse(vm.state.value.messages.last().failed)
        assertEquals(2, vm.state.value.messages.size)
        assertEquals(0, vm.state.value.left)

        vm.send("Redmont form")
        advanceTimeBy(5_000)
        assertTrue(vm.state.value.quotaHit)
        vm.dismissQuota()
        assertFalse(vm.state.value.quotaHit)
    }

    @Test fun authValidatesThenSignsUpSignsOutAndSignsIn() = runTest(main.dispatcher) {
        val auth = InMemoryAuthRepository(clock)
        val vm = AuthViewModel(auth, AuthMode.SignUp)
        vm.submit()
        val f = vm.form.value
        assertNotNull(f.nameError); assertNotNull(f.emailError); assertNotNull(f.passwordError)
        assertFalse(f.submitting)

        vm.onName("Muhammad"); vm.onEmail("fan@pitchside.app"); vm.onPassword("goal2026"); vm.onConfirm("goal2027")
        vm.submit()
        assertEquals("Passwords don't match.", vm.form.value.confirmError)
        vm.onConfirm("goal2026")
        vm.submit()
        advanceTimeBy(5_000)
        assertTrue(vm.form.value.done)
        assertNotNull(auth.accessToken())

        auth.signOut()
        assertNull(auth.accessToken())

        val login = AuthViewModel(auth, AuthMode.SignIn)
        login.onEmail("fan@pitchside.app"); login.onPassword("wrongpass1")
        login.submit(); advanceTimeBy(5_000)
        assertEquals("That password isn't right. Try again or reset it.", login.form.value.formError)
        login.onPassword("goal2026")
        login.submit(); advanceTimeBy(5_000)
        assertTrue(login.form.value.done)

        val reset = AuthViewModel(auth, AuthMode.Reset)
        reset.onEmail("someone@else.com"); reset.submit(); advanceTimeBy(5_000)
        assertTrue(reset.form.value.resetSent)
    }
}

/** Clears the ViewModel (cancels viewModelScope) so runTest doesn't spin on the live-refresh loop. */
@Suppress("RestrictedApi")
private fun androidx.lifecycle.ViewModel.viewModelScope_cancel() {
    val store = androidx.lifecycle.ViewModelStore()
    store.put("vm", this)
    store.clear()
}
