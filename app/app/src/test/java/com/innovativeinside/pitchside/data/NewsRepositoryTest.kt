package com.innovativeinside.pitchside.data

import com.innovativeinside.pitchside.core.AppError
import com.innovativeinside.pitchside.data.mock.MockBehavior
import com.innovativeinside.pitchside.data.mock.MockFootballApi
import com.innovativeinside.pitchside.data.model.NewsCategory
import com.innovativeinside.pitchside.data.model.NewsEdition
import com.innovativeinside.pitchside.data.model.NewsItem
import com.innovativeinside.pitchside.data.news.NewsFeedSource
import com.innovativeinside.pitchside.data.repository.DefaultFootballRepository
import com.innovativeinside.pitchside.testutil.FixedClock
import com.innovativeinside.pitchside.testutil.MainDispatcherRule
import com.innovativeinside.pitchside.testutil.TestDispatchers
import com.innovativeinside.pitchside.ui.vm.NewsViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

/** A feed source with [perEdition] stories per edition, counting fetches and failing editions on demand. */
private class FakeFeed(private val perEdition: Int = 30) : NewsFeedSource {
    val fetches = mutableMapOf<NewsEdition, Int>()
    val failing = mutableSetOf<NewsEdition>()
    override suspend fun fetch(edition: NewsEdition): List<NewsItem> {
        fetches[edition] = (fetches[edition] ?: 0) + 1
        if (edition in failing) throw AppError.Network()
        return (0 until perEdition).map { i ->
            NewsItem(
                "${edition.code}-g$i", NewsCategory.Reports, "${edition.label} story $i", "Dek $i.", "", "90min",
                // Editions interleave in time so the merged list mixes them.
                publishedMs = 1_000_000_000L - i * 10_000L - edition.ordinal * 1_000L, readMinutes = 1,
                url = "https://www.90min.com/${edition.code}/$i", imageUrl = "https://img/$i.jpg", edition = edition, language = edition.language,
            )
        }.sortedByDescending { it.publishedMs }
    }
}

@OptIn(ExperimentalCoroutinesApi::class)
class NewsRepositoryTest {
    @get:Rule val main = MainDispatcherRule()
    private val clock = FixedClock()
    private val feed = FakeFeed()
    private fun repo(source: NewsFeedSource? = feed) =
        DefaultFootballRepository(MockFootballApi(clock, MockBehavior().apply { latencyMs = 0 }), TestDispatchers(StandardTestDispatcher()), clock, newsSource = source)

    @Test fun allMergesEditionsNewestFirstAndPages() = runTest {
        val r = repo()
        val p1 = r.headlines(null).getOrThrow()
        assertEquals(20, p1.items.size)
        assertEquals("20", p1.nextCursor)
        assertEquals(p1.items.sortedByDescending { it.publishedMs }, p1.items)
        assertEquals(NewsEdition.entries.toSet(), p1.items.take(4).mapNotNull { it.edition }.toSet())
        // 4 editions x 30 stories = 120: pages of 20, the last one has no cursor.
        var cursor = p1.nextCursor
        var total = p1.items.size
        while (cursor != null) { val p = r.headlines(null, cursor).getOrThrow(); total += p.items.size; cursor = p.nextCursor }
        assertEquals(120, total)
        // Each edition was downloaded once; paging is served from the cache.
        assertTrue(feed.fetches.values.all { it == 1 })
    }

    @Test fun editionFilterAndRefreshBypassesCache() = runTest {
        val r = repo()
        val it1 = r.headlines(NewsEdition.Italy).getOrThrow()
        assertTrue(it1.items.all { it.edition == NewsEdition.Italy })
        r.headlines(NewsEdition.Italy)
        assertEquals(1, feed.fetches[NewsEdition.Italy])
        r.headlines(NewsEdition.Italy, refresh = true)
        assertEquals(2, feed.fetches[NewsEdition.Italy])
        clock.now += 11 * 60_000 // past the 10 minute news TTL
        r.headlines(NewsEdition.Italy)
        assertEquals(3, feed.fetches[NewsEdition.Italy])
    }

    @Test fun oneEditionDownStillShowsTheRestButAllDownIsAnError() = runTest {
        val r = repo()
        feed.failing += NewsEdition.Spain
        val page = r.headlines(null).getOrThrow()
        assertTrue(page.items.none { it.edition == NewsEdition.Spain })
        assertTrue(r.headlines(NewsEdition.Spain).exceptionOrNull() is AppError.Network)
        feed.failing += NewsEdition.entries
        val all = repo(FakeFeed().apply { failing += NewsEdition.entries }).headlines(null)
        assertTrue(all.exceptionOrNull() is AppError.Network)
    }

    @Test fun articleComesFromTheFeedAndSurvivesACacheClear() = runTest {
        val r = repo()
        val first = r.headlines(NewsEdition.Brazil).getOrThrow().items.first()
        assertEquals(first, r.article(first.id).getOrThrow())
        // New repository instance = process death: the id carries the edition, so it reloads just that feed.
        val fresh = FakeFeed()
        val r2 = repo(fresh)
        assertEquals(first.title, r2.article(first.id).getOrThrow().title)
        assertEquals(mapOf(NewsEdition.Brazil to 1), fresh.fetches)
        assertTrue(r2.article("br-missing").exceptionOrNull() is AppError.NotFound)
    }

    @Test fun withoutAFeedSourceFallsBackToTheApi() = runTest {
        val page = repo(null).headlines(NewsEdition.France).getOrThrow()
        assertTrue(page.items.isNotEmpty())
        assertTrue(page.items.all { it.edition == null })
        // The old category contract still works.
        assertTrue(repo(null).news(NewsCategory.Transfers).getOrThrow().items.all { it.category == NewsCategory.Transfers })
    }

    @Test fun newsViewModelSwitchesEditionsAndPagesOnDemand() = runTest(main.dispatcher) {
        val r = DefaultFootballRepository(MockFootballApi(clock, MockBehavior().apply { latencyMs = 0 }), TestDispatchers(main.dispatcher), clock, newsSource = feed)
        val vm = NewsViewModel(r)
        advanceUntilIdle()
        assertEquals(20, vm.state.value.items.size)
        assertNull(vm.state.value.edition)
        vm.loadMore()
        advanceUntilIdle()
        assertEquals(40, vm.state.value.items.size)
        vm.setEdition(NewsEdition.France)
        advanceUntilIdle()
        assertEquals(NewsEdition.France, vm.state.value.edition)
        assertTrue(vm.state.value.items.all { it.edition == NewsEdition.France })
        feed.failing += NewsEdition.France
        vm.retry()
        advanceUntilIdle()
        assertTrue(vm.state.value.error is AppError.Network)
    }
}
