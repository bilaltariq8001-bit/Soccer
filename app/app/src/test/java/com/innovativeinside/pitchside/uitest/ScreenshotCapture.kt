package com.innovativeinside.pitchside.uitest

import android.graphics.Bitmap
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.activity.ComponentActivity
import android.graphics.Canvas
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.innovativeinside.pitchside.core.UiState
import com.innovativeinside.pitchside.data.model.EventKind
import com.innovativeinside.pitchside.data.model.MatchEvent
import com.innovativeinside.pitchside.data.model.StatRow
import com.innovativeinside.pitchside.data.repository.MatchDetail
import com.innovativeinside.pitchside.ui.components.LocalReducedMotion
import com.innovativeinside.pitchside.ui.preview.PreviewData
import com.innovativeinside.pitchside.ui.preview.PreviewHost
import com.innovativeinside.pitchside.ui.screens.HomeActions
import com.innovativeinside.pitchside.ui.screens.HomeContent
import com.innovativeinside.pitchside.ui.screens.MatchContent
import com.innovativeinside.pitchside.ui.screens.MatchNav
import com.innovativeinside.pitchside.ui.screens.NewsContent
import com.innovativeinside.pitchside.ui.vm.NewsState
import java.io.File
import org.junit.Assume.assumeTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TestRule
import org.junit.runners.model.Statement
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/**
 * Renders key screens to PNG for design review. Off by default; run with PITCHSIDE_SHOTS=<dir>.
 * Uses Robolectric's native graphics; no extra dependency.
 */
@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = "w400dp-h2400dp-xhdpi")
class ScreenshotCapture {
    private val out: String? = System.getenv("PITCHSIDE_SHOTS")

    /** Skips before the compose rule starts an activity (the native runtime is x86_64/macOS only). */
    @get:Rule(order = 0) val gate = TestRule { base, _ ->
        object : Statement() { override fun evaluate() { assumeTrue("set PITCHSIDE_SHOTS to render", !out.isNullOrBlank()); base.evaluate() } }
    }
    @get:Rule(order = 1) val rule = createAndroidComposeRule<ComponentActivity>()

    private fun shot(name: String, dark: Boolean, content: @Composable () -> Unit) {
        rule.setContent {
            PreviewHost(dark = dark) {
                CompositionLocalProvider(LocalReducedMotion provides true) { Column(Modifier.fillMaxSize()) { content() } }
            }
        }
        rule.waitForIdle()
        // Software draw of the window (captureToImage's redraw wait times out on slow emulated hosts).
        val view = rule.activity.window.decorView
        val bmp = Bitmap.createBitmap(view.width, view.height, Bitmap.Config.ARGB_8888)
        rule.runOnUiThread { view.draw(Canvas(bmp)) }
        File(out!!).mkdirs()
        File(out, "$name.png").outputStream().use { bmp.compress(Bitmap.CompressFormat.PNG, 100, it) }
    }

    private val actions = HomeActions({}, {}, {}, {}, {}, {}, {}, {})
    private val threeLive = PreviewData.home.copy(
        matches = listOf(
            PreviewData.live,
            PreviewData.live.copy(id = "l2", home = PreviewData.live.home.copy(id = "hc", name = "Harbor City", short = "HC", color = 0xFF1F5FBF), away = PreviewData.live.away.copy(id = "rr", name = "Redmont", short = "RR", color = 0xFFB0262F), homeScore = 0, awayScore = 0, label = "33'"),
            PreviewData.live.copy(id = "l3", leagueId = "cc", leagueName = "Continental Cup", home = PreviewData.live.home.copy(id = "kb", name = "Kingsbridge", short = "KB", color = 0xFF8A5A00), away = PreviewData.live.away.copy(id = "os", name = "Olympia SC", short = "OS", color = 0xFF2E7D4F), homeScore = 1, awayScore = 1, label = "HT"),
            PreviewData.upcoming, PreviewData.finished,
        ),
    )
    private val detail = MatchDetail(
        PreviewData.live,
        listOf(MatchEvent(23, null, true, EventKind.Goal, "Leo Marsh", "Assist T. Ellis"), MatchEvent(38, null, false, EventKind.YellowCard, "Sam Okafor", "Foul"), MatchEvent(51, null, false, EventKind.Goal, "Sam Okafor", ""), MatchEvent(67, null, true, EventKind.Goal, "Theo Ellis", "Header")),
        listOf(StatRow("Possession", 58, 42, "58%", "42%"), StatRow("Shots", 8, 5, "8", "5"), StatRow("On target", 4, 2, "4", "2")),
        null, PreviewData.table,
    )

    @Test fun homeLight() = shot("home_light", false) { HomeContent(UiState.Success(threeLive), actions, {}, mapOf("m1" to detail.stats)) }
    @Test fun homeDark() = shot("home_dark", true) { HomeContent(UiState.Success(threeLive), actions, {}, mapOf("m1" to detail.stats)) }
    @Test fun newsLight() = shot("news_light", false) { NewsContent(NewsState(items = PreviewData.news, loading = false), {}, {}, {}, {}) }
    @Test fun matchLight() = shot("match_light", false) { MatchContent(UiState.Success(detail), MatchNav({}, {}, {}, {}, {})) {} }
    @Test fun matchDark() = shot("match_dark", true) { MatchContent(UiState.Success(detail), MatchNav({}, {}, {}, {}, {})) {} }
}
