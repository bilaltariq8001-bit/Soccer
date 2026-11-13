package com.innovativeinside.pitchside.uitest

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performSemanticsAction
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextInput
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.innovativeinside.pitchside.MainActivity
import com.innovativeinside.pitchside.PitchsideApp
import com.innovativeinside.pitchside.core.AppError
import com.innovativeinside.pitchside.core.UiState
import com.innovativeinside.pitchside.data.auth.InMemoryAuthRepository
import com.innovativeinside.pitchside.data.prefs.UserPrefs
import com.innovativeinside.pitchside.ui.components.StateContent
import com.innovativeinside.pitchside.ui.preview.InMemoryContainer
import com.innovativeinside.pitchside.ui.preview.PreviewHost
import com.innovativeinside.pitchside.ui.screens.AuthRoute
import com.innovativeinside.pitchside.ui.vm.AuthMode
import com.innovativeinside.pitchside.ui.vm.AuthViewModel
import com.innovativeinside.pitchside.core.SystemClock
import androidx.compose.material3.Text
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

/** Onboarded user with one followed team, on the mock API with no latency. */
class OnboardedTestApp : PitchsideApp() {
    override fun onCreate() { super.onCreate(); overrideContainer(InMemoryContainer(UserPrefs(onboarded = true, teams = listOf("ng")))) }
}

class NewUserTestApp : PitchsideApp() {
    override fun onCreate() { super.onCreate(); overrideContainer(InMemoryContainer(UserPrefs())) }
}

@RunWith(AndroidJUnit4::class)
class StateContentTest {
    @get:Rule val rule = createComposeRule()

    @Test fun loadingEmptyErrorAndSuccess() {
        var state by androidx.compose.runtime.mutableStateOf<UiState<String>>(UiState.Loading)
        var retries = 0
        rule.setContent { PreviewHost { StateContent(state, { retries++ }) { Text("Loaded $it") } } }
        rule.onNodeWithTag("state_loading").assertExists()

        state = UiState.Empty
        rule.onNodeWithTag("state_empty").assertExists()
        rule.onNodeWithText("Nothing here yet").assertIsDisplayed()

        state = UiState.Error(AppError.Network())
        rule.onNodeWithText("You're offline").assertIsDisplayed()
        rule.onNodeWithTag("retry_button").performClick()
        assertEquals(1, retries)

        state = UiState.Error(AppError.NotFound("This match"))
        rule.onNodeWithText("This match is no longer available.").assertIsDisplayed()
        rule.onNodeWithTag("retry_button").assertDoesNotExist()

        state = UiState.Success("ok")
        rule.onNodeWithText("Loaded ok").assertIsDisplayed()
    }
}

@RunWith(AndroidJUnit4::class)
class AuthScreenTest {
    @get:Rule val rule = createComposeRule()

    @Test fun signUpShowsFieldErrorsThenSucceeds() {
        val auth = InMemoryAuthRepository(SystemClock)
        val vm = AuthViewModel(auth, AuthMode.SignUp)
        var done = false
        rule.setContent { PreviewHost { AuthRoute(AuthMode.SignUp, {}, { done = true }, vm) } }

        rule.onNodeWithTag("auth_submit").performClick()
        rule.onNodeWithTag("error_email").assertExists()
        rule.onNodeWithTag("error_name").assertExists()

        rule.onNodeWithTag("field_name").performTextInput("Muhammad")
        rule.onNodeWithTag("field_email").performTextInput("fan@pitchside.app")
        rule.onNodeWithTag("field_password").performTextInput("goal2026")
        rule.onNodeWithTag("field_confirm").performTextInput("goal2026")
        rule.onNodeWithTag("error_email").assertDoesNotExist()
        rule.onNodeWithTag("auth_submit").performClick()
        rule.waitUntil(5_000) { done }
        assertTrue(done)
    }

    @Test fun forgotPasswordFlow() {
        val vm = AuthViewModel(InMemoryAuthRepository(SystemClock), AuthMode.SignIn)
        rule.setContent { PreviewHost { AuthRoute(AuthMode.SignIn, {}, {}, vm) } }
        rule.onNodeWithText("Forgot password?").performClick()
        rule.onNodeWithTag("field_email").performTextInput("fan@pitchside.app")
        rule.onNodeWithTag("auth_submit").performClick()
        rule.waitUntil(5_000) { rule.onAllNodesWithText("If an account uses that email, a reset link is on its way.").fetchSemanticsNodes().isNotEmpty() }
    }
}

@RunWith(AndroidJUnit4::class)
@Config(application = OnboardedTestApp::class)
class AppNavigationTest {
    @get:Rule val rule = createAndroidComposeRule<MainActivity>()

    private fun waitText(t: String) = rule.waitUntil(10_000) { rule.onAllNodesWithText(t, substring = true).fetchSemanticsNodes().isNotEmpty() }

    @Test fun launchHomeThenTabsAndMatchDetail() {
        waitText("Live now")
        rule.onNodeWithTag("live_hero").assertExists()
        rule.onNodeWithText("Football today").assertExists()

        rule.onNodeWithTag("nav_matches").performClick()
        waitText("Yesterday")
        rule.onNodeWithText("Live").performClick()
        waitText("Premier Division")

        rule.onNodeWithTag("nav_leagues").performClick()
        waitText("Top scorers")
        rule.onNodeWithText("Top scorers").performSemanticsAction(SemanticsActions.OnClick)
        waitText("Leo Marsh")
        rule.onNodeWithContentDescription("Open Premier Division").performClick()
        waitText("Teams")
        rule.activity.onBackPressedDispatcher.onBackPressed()
        waitText("Tables, fixtures and top scorers".uppercase())

        rule.onNodeWithTag("nav_news").performClick()
        waitText("FROM 90MIN")
        rule.onNodeWithTag("nav_following").performClick()
        waitText("free follows")
        assertTrue(rule.onAllNodesWithText("Northgate").fetchSemanticsNodes().isNotEmpty())
    }

    @Test fun backOnHomeAsksBeforeExit() {
        waitText("Live now")
        rule.activity.onBackPressedDispatcher.onBackPressed()
        waitText("Leave Pitchside?")
        rule.onNodeWithText("Cancel").performClick()
        rule.onNodeWithText("Leave Pitchside?").assertDoesNotExist()
    }
}

@RunWith(AndroidJUnit4::class)
@Config(application = NewUserTestApp::class)
class OnboardingFlowTest {
    @get:Rule val rule = createAndroidComposeRule<MainActivity>()
    private fun waitText(t: String) = rule.waitUntil(10_000) { rule.onAllNodesWithText(t, substring = true).fetchSemanticsNodes().isNotEmpty() }

    @Test fun newUserCompletesOnboardingAndLandsHome() {
        waitText("Get started")
        rule.onNodeWithText("Get started").performClick()
        waitText("Choose your language")
        rule.onNodeWithText("Continue").performClick()
        waitText("Who do you follow?")
        waitText("Northgate")
        rule.onNodeWithText("Northgate").performScrollTo().performClick()
        waitText("1 selected")
        rule.onNodeWithText("Continue · 1 selected").performClick()
        waitText("How do you usually watch?")
        rule.onNodeWithText("Continue").performClick()
        waitText("What should we alert you about?")
        rule.onNodeWithText("Not now").performClick()
        waitText("Build my home")
        rule.onNodeWithText("Build my home").performClick()
        waitText("Live now")
    }
}
