package com.innovativeinside.pitchside.data

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.innovativeinside.pitchside.core.AppError
import com.innovativeinside.pitchside.data.model.NewsEdition
import com.innovativeinside.pitchside.data.news.NewsImages
import com.innovativeinside.pitchside.data.news.NewsText
import com.innovativeinside.pitchside.data.news.NinetyMinNewsSource
import com.innovativeinside.pitchside.data.news.NinetyMinRssParser
import kotlinx.coroutines.runBlocking
import okhttp3.OkHttpClient
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.util.concurrent.TimeUnit

/** Fixture: the first five items of https://www.90min.com/fr/feed, saved 2026-11-13 (test resources). */
private fun fixture(): String = NinetyMinNewsTest::class.java.getResource("/fixtures/90min_fr_sample.xml")!!.readText()

/** XmlPullParser comes from the Android framework, so these run on Robolectric. */
@RunWith(AndroidJUnit4::class)
class NinetyMinNewsTest {

    @Test fun parsesTheRealFeedFixture() {
        val items = NinetyMinRssParser.parse(fixture().byteInputStream(), NewsEdition.France)
        assertEquals(5, items.size)
        val first = items.first()
        assertEquals("fr-01kxeqw25b8q", first.id)
        assertTrue(first.title.startsWith("France - Espagne : « On a peur de personne »"))
        assertEquals("Ilies Peeters", first.author)
        assertEquals("fr", first.language)
        assertEquals(NewsEdition.France, first.edition)
        assertEquals("90min", first.source)
        // Mon, 13 Jul 2026 22:02:57 +0000
        assertEquals(1_783_980_177_000L, first.publishedMs)
        assertTrue(first.imageUrl!!.startsWith("https://images2.minutemediacdn.com/"))
        assertEquals("France Press Conference - FIFA World Cup 2026 | Stacy Revere/GettyImages", first.imageCaption)
        // Tracking parameter removed from the story link.
        assertEquals("https://www.90min.com/fr/france-espagne-on-a-peur-de-personne-warren-zaire-emery-affiche-les-ambitions-des-bleus-avant-la-demi", first.url)
        // Entities decoded (&#34; in the title, &#39; in the image caption).
        assertTrue(items[1].title.contains("\"Les propos racistes sont inadmissibles\""))
        assertTrue(items[2].imageCaption!!.contains("l'équipe de France"))
        // Newest first.
        assertEquals(items.sortedByDescending { it.publishedMs }, items)
        // Truncated descriptions end cleanly.
        assertTrue(items.all { it.dek.isEmpty() || it.dek.last() in ".!?…»\")”’" })
    }

    @Test fun toleratesCdataDoubleEncodingAndMissingFields() {
        val xml = """<?xml version="1.0"?><rss xmlns:media="http://search.yahoo.com/mrss/"><channel>
            <item><title><![CDATA[Juve &amp;#39;in crisi&#39; <b>oggi</b>]]></title><link>https://www.90min.com/it/a?utm_source=RSS</link>
              <guid>abc 1</guid><pubDate>not a date</pubDate><description>Breve.</description></item>
            <item><title></title><link>https://x</link></item>
            <item><title>No link</title></item>
        </channel></rss>"""
        val items = NinetyMinRssParser.parse(xml.byteInputStream(), NewsEdition.Italy)
        assertEquals(1, items.size)
        val i = items.single()
        assertEquals("Juve 'in crisi' oggi", i.title)
        assertEquals("it-abc_1", i.id)
        assertEquals(0L, i.publishedMs)
        assertNull(i.imageUrl)
        assertEquals("Breve.", i.dek)
        assertEquals("it", i.language)
    }

    @Test fun textHelpers() {
        assertEquals("a & b \"c\" 'd' é", NewsText.clean("a &amp; b &#34;c&#34; &#x27;d&#39; &eacute;"))
        assertEquals("Il a dit que…", NewsText.snippet("Il a dit que la"))
        assertEquals("Fin.", NewsText.snippet("Fin."))
        assertNull(NewsText.parseRfc822("yesterday"))
        val url = "https://images2.minutemediacdn.com/image/upload/c_crop,w_5459,h_3070,x_0,y_387/c_fill,w_2880,ar_16:9,f_auto/x.jpg"
        assertEquals("https://images2.minutemediacdn.com/image/upload/c_crop,w_5459,h_3070,x_0,y_387/c_fill,w_720,ar_16:9,f_auto/x.jpg", NewsImages.sized(url, 700))
        assertEquals("https://other.cdn/x.jpg", NewsImages.sized("https://other.cdn/x.jpg", 300))
    }
}

/** The OkHttp source against a local MockWebServer: no real network in tests. */
@RunWith(AndroidJUnit4::class)
class NinetyMinNewsSourceTest {
    private val server = MockWebServer()
    private lateinit var source: NinetyMinNewsSource

    @Before fun setUp() {
        server.start()
        val client = OkHttpClient.Builder().readTimeout(2, TimeUnit.SECONDS).callTimeout(4, TimeUnit.SECONDS).build()
        source = NinetyMinNewsSource(client, server.url("/").toString())
    }

    @After fun tearDown() { runCatching { server.shutdown() } }

    @Test fun fetchesTheEditionPathAndParses() = runBlocking {
        server.enqueue(MockResponse().setBody(fixture()).setHeader("Content-Type", "application/rss+xml"))
        val items = source.fetch(NewsEdition.Brazil)
        assertEquals("/pt-BR/feed", server.takeRequest().path)
        assertEquals(5, items.size)
        assertTrue(items.all { it.id.startsWith("br-") && it.edition == NewsEdition.Brazil })
    }

    @Test fun mapsHttpAndParseFailures() = runBlocking {
        server.enqueue(MockResponse().setResponseCode(404))
        server.enqueue(MockResponse().setResponseCode(503))
        server.enqueue(MockResponse().setBody("<html><body>Down for maintenance</body></html>"))
        expect<AppError.NotFound> { source.fetch(NewsEdition.Spain) }
        expect<AppError.Server> { source.fetch(NewsEdition.Spain) }
        expect<AppError.Parse> { source.fetch(NewsEdition.Spain) }
    }

    @Test fun offlineIsANetworkError() = runBlocking {
        server.shutdown()
        expect<AppError.Network> { source.fetch(NewsEdition.France) }
    }

    private inline fun <reified E : AppError> expect(block: () -> Unit) {
        try { block(); fail("expected ${E::class.simpleName}") } catch (e: AppError) { assertTrue("got $e", e is E) }
    }
}
