package com.innovativeinside.pitchside

import androidx.compose.material3.Text
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.innovativeinside.pitchside.core.AppError
import com.innovativeinside.pitchside.core.UiState
import com.innovativeinside.pitchside.ui.components.StateContent
import com.innovativeinside.pitchside.ui.preview.PreviewHost
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/** On-device check of the shared error/retry treatment. Run: ./gradlew :app:connectedDebugAndroidTest */
@RunWith(AndroidJUnit4::class)
class StateContentInstrumentedTest {
    @get:Rule val rule = createComposeRule()

    @Test fun errorShowsRetryAndCallsBack() {
        var retries = 0
        rule.setContent { PreviewHost { StateContent<String>(UiState.Error(AppError.Server(503)), { retries++ }) { Text(it) } } }
        rule.onNodeWithText("Something went wrong").assertExists()
        rule.onNodeWithTag("retry_button").performClick()
        assertEquals(1, retries)
    }
}
