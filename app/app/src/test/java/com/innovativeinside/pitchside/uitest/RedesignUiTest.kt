package com.innovativeinside.pitchside.uitest

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.ui.unit.dp
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performSemanticsAction
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.onNodeWithContentDescription
import com.innovativeinside.pitchside.ui.components.PillTabs
import com.innovativeinside.pitchside.ui.components.ScreenHeader
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.setValue
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.innovativeinside.pitchside.core.UiState
import com.innovativeinside.pitchside.data.model.Match
import com.innovativeinside.pitchside.data.model.NewsEdition

import com.innovativeinside.pitchside.ui.components.LocalReducedMotion
import com.innovativeinside.pitchside.ui.preview.PreviewData
import com.innovativeinside.pitchside.ui.preview.PreviewHost
import com.innovativeinside.pitchside.ui.screens.HomeActions
import com.innovativeinside.pitchside.ui.screens.HomeContent
import com.innovativeinside.pitchside.ui.screens.NewsContent
import com.innovativeinside.pitchside.ui.vm.NewsState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

private fun hasDescriptionContaining(s: String) = SemanticsMatcher("description contains $s") { n ->
    n.config.getOrElse(SemanticsProperties.ContentDescription) { emptyList() }.any { it.contains(s) }
}

/** Home ("Football Today") with 0, 1 and 3 live matches, and the News list. Runs on Robolectric with motion reduced. */
@RunWith(AndroidJUnit4::class)
class RedesignUiTest {
    @get:Rule val rule = createComposeRule()

    private val opened = mutableListOf<String>()
    private val actions = HomeActions({ opened += "match:$it" }, {}, {}, {}, { opened += "article:$it" }, { opened += "league:$it" }, {}, { opened += "matches" }, openAccount = { opened += "account" })

    private fun liveMatches(n: Int): List<Match> {
        val base = PreviewData.live
        val extra = listOf(
            base.copy(id = "l2", home = base.home.copy(id = "hc", name = "Harbor City", short = "HC"), away = base.away.copy(id = "rr", name = "Redmont", short = "RR"), homeScore = 0, awayScore = 0, label = "33'", brief = ""),
            base.copy(id = "l3", home = base.home.copy(id = "kb", name = "Kingsbridge", short = "KB"), away = base.away.copy(id = "os", name = "Olympia SC", short = "OS"), homeScore = 3, awayScore = 0, label = "81'", brief = ""),
        )
        return (listOf(base) + extra).take(n)
    }

    private fun setHome(liveCount: Int) {
        val matches = liveMatches(liveCount) + PreviewData.upcoming + PreviewData.finished
        rule.setContent {
            PreviewHost(dark = false) {
                CompositionLocalProvider(LocalReducedMotion provides true) {
                    Box(Modifier.size(412.dp, 915.dp)) { HomeContent(UiState.Success(PreviewData.home.copy(matches = matches)), actions, {}, modifier = Modifier.fillMaxSize()) }
                }
            }
        }
    }

    @Test fun homeWithNoLiveMatchShowsQuietStateAndNoHero() {
        setHome(0)
        rule.onNodeWithTag("no_live").assertExists()
        rule.onNodeWithTag("live_hero").assertDoesNotExist()
        rule.onNodeWithTag("live_count").assertDoesNotExist()
        rule.onNodeWithText("Nothing live right now").assertExists()
        // The rest of the hub still renders.
        rule.onNodeWithTag("today_rail").assertExists()
        rule.onNodeWithTag("highlights").assertExists()
        rule.onNodeWithTag("see_all_today").assertExists()
        // Standings, news and the matchday cards moved off Home (one screen, no scrolling).
        rule.onNodeWithTag("bento").assertDoesNotExist()
        rule.onNodeWithTag("mini_standings_news").assertDoesNotExist()
    }

    @Test fun homeWithOneLiveMatchShowsHeroWithoutMiniTiles() {
        setHome(1)
        rule.onNodeWithTag("live_hero").assertIsDisplayed()
        rule.onNodeWithText("1 live", substring = true).assertExists()
        // One live match: a single hero card, no pager.
        rule.onNodeWithTag("live_pager").assertDoesNotExist()
        rule.onNode(hasDescriptionContaining("Featured match: Northgate 2, Riverside 1, 74th minute, live")).assertExists()
        rule.onNodeWithTag("live_hero").performClick()
        assertEquals(listOf("match:m1"), opened)
    }

    @Test fun homeWithThreeLiveMatchesPromotesAMiniTile() {
        setHome(3)
        rule.onNodeWithText("3 live", substring = true).assertExists()
        // Several live matches: a pager with the next card peeking.
        rule.onNodeWithTag("live_pager").assertExists()
        // Followed team (Northgate, "ng") leads the hero.
        rule.onNode(hasDescriptionContaining("Featured match: Northgate")).assertExists()
        rule.onNode(hasDescriptionContaining("Kingsbridge 3, Olympia SC 0, 81st minute, live. Show as featured match")).performSemanticsAction(SemanticsActions.OnClick)
        rule.waitForIdle()
        rule.onNode(hasDescriptionContaining("Featured match: Kingsbridge")).assertExists()
        rule.onNode(hasDescriptionContaining("Northgate 2, Riverside 1, 74th minute, live. Show as featured match")).assertExists()
    }

    @Test fun newsListShowsEditionsLeadAndCompactStories() {
        var picked: NewsEdition? = null
        rule.setContent {
            PreviewHost(dark = false) {
                CompositionLocalProvider(LocalReducedMotion provides true) {
                    NewsContent(NewsState(items = PreviewData.news, loading = false), { picked = it }, {}, {}, { opened += "article:$it" })
                }
            }
        }
        listOf("All", "France", "Italy", "Spain", "Brazil").forEach { rule.onNodeWithText(it).assertExists() }
        rule.onNodeWithText(PreviewData.news.first().title).assertExists()
        assertTrue(rule.onAllNodesWithText("90min", substring = true).fetchSemanticsNodes().isNotEmpty())
        rule.onNodeWithText("Italy").performClick()
        assertEquals(NewsEdition.Italy, picked)
        rule.onNode(hasDescriptionContaining(PreviewData.news[1].title)).performClick()
        assertEquals("article:${PreviewData.news[1].id}", opened.last())
    }

    @Test fun newsErrorOffersRetry() {
        var retried = 0
        rule.setContent {
            PreviewHost(dark = false) {
                NewsContent(NewsState(loading = false, error = com.innovativeinside.pitchside.core.AppError.Network()), {}, { retried++ }, {}, {})
            }
        }
        rule.onNodeWithText("You're offline").assertExists()
        rule.onNodeWithTag("retry_button").performClick()
        assertEquals(1, retried)
    }

    @Test fun homeHeaderShowsTitleDateAndOpensAccountFromAvatar() {
        setHome(1)
        rule.onNodeWithText("FOOTBALL TODAY").assertExists()
        rule.onNodeWithText("TOP MATCHES").assertExists()
        rule.onNodeWithText("TODAY", substring = true).assertExists()
        rule.onNodeWithContentDescription("Account and settings").performClick()
        assertEquals("account", opened.last())
    }

    @Test fun seeAllTodayOpensMatches() {
        setHome(0)
        rule.onNodeWithTag("see_all_today").performClick()
        assertEquals("matches", opened.last())
    }

    @Test fun pillTabsSelectAndUppercaseLabels() {
        var picked by androidx.compose.runtime.mutableIntStateOf(0)
        rule.setContent { PreviewHost { PillTabs(listOf("Overview", "Stats", "Lineups"), picked, { picked = it }) } }
        rule.onNodeWithText("STATS").performClick()
        assertEquals(1, picked)
    }
}
