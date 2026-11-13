package com.innovativeinside.pitchside.data.news

import android.util.Xml
import com.innovativeinside.pitchside.core.AppError
import com.innovativeinside.pitchside.data.model.NewsCategory
import com.innovativeinside.pitchside.data.model.NewsEdition
import com.innovativeinside.pitchside.data.model.NewsItem
import java.io.IOException
import java.io.InputStream
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.util.concurrent.TimeUnit
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlinx.coroutines.suspendCancellableCoroutine
import okhttp3.Call
import okhttp3.Callback
import okhttp3.HttpUrl
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import org.xmlpull.v1.XmlPullParser
import org.xmlpull.v1.XmlPullParserException

/**
 * A source of news stories for one edition. The app uses [NinetyMinNewsSource] (the public 90min RSS
 * feeds); tests and previews plug in a fixture source. Implementations throw [AppError] on failure.
 */
fun interface NewsFeedSource {
    suspend fun fetch(edition: NewsEdition): List<NewsItem>
}

/**
 * Reads the 90min RSS 2.0 feeds (https://www.90min.com/fr/feed and siblings) with OkHttp and parses them
 * with Android's built-in XmlPullParser. Only what the feed publishes is shown: headline, the short
 * description, author, date, image and a link to the full story on 90min. Article bodies are not scraped.
 */
class NinetyMinNewsSource(
    private val client: OkHttpClient = defaultClient(),
    baseUrl: String = "https://www.90min.com/",
) : NewsFeedSource {
    private val base: HttpUrl = baseUrl.trimEnd('/').plus("/").toHttpUrl()

    override suspend fun fetch(edition: NewsEdition): List<NewsItem> {
        val url = base.resolve(edition.path) ?: throw AppError.NotFound("This news edition")
        val request = Request.Builder().url(url).header("Accept", "application/rss+xml, application/xml;q=0.9, */*;q=0.5").get().build()
        val response = try { client.newCall(request).await() } catch (e: IOException) { throw AppError.from(e) }
        response.use { r ->
            if (!r.isSuccessful) throw AppError.fromHttp(r.code)
            val body = r.body ?: throw AppError.Parse()
            return try {
                NinetyMinRssParser.parse(body.byteStream(), edition)
            } catch (e: XmlPullParserException) {
                throw AppError.Parse(e)
            } catch (e: IOException) {
                throw AppError.from(e)
            }
        }
    }

    private suspend fun Call.await(): Response = suspendCancellableCoroutine { cont ->
        cont.invokeOnCancellation { runCatching { cancel() } }
        enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) { if (cont.isActive) cont.resumeWithException(e) }
            override fun onResponse(call: Call, response: Response) { cont.resume(response) }
        })
    }

    companion object {
        fun defaultClient(): OkHttpClient = OkHttpClient.Builder()
            .connectTimeout(10, TimeUnit.SECONDS)
            .readTimeout(15, TimeUnit.SECONDS)
            .callTimeout(25, TimeUnit.SECONDS)
            .build()
    }
}

/** Parses a 90min RSS document into [NewsItem]s. Pure apart from Android's XmlPullParser. */
object NinetyMinRssParser {
    private const val SOURCE = "90min"

    fun parse(input: InputStream, edition: NewsEdition): List<NewsItem> {
        val parser = Xml.newPullParser()
        parser.setFeature(XmlPullParser.FEATURE_PROCESS_NAMESPACES, false)
        parser.setInput(input, null)
        val items = mutableListOf<NewsItem>()
        var sawRss = false
        var event = parser.eventType
        while (event != XmlPullParser.END_DOCUMENT) {
            if (event == XmlPullParser.START_TAG) {
                if (parser.depth == 1) sawRss = parser.name == "rss"
                if (parser.name == "item") readItem(parser, edition)?.let(items::add)
            }
            event = parser.next()
        }
        // An HTML maintenance page served with 200 is not a feed: report it instead of showing "no stories".
        if (!sawRss) throw XmlPullParserException("Not an RSS document")
        return items.distinctBy { it.id }.sortedByDescending { it.publishedMs }
    }

    private fun readItem(p: XmlPullParser, edition: NewsEdition): NewsItem? {
        var title = ""
        var link = ""
        var author: String? = null
        var guid = ""
        var description = ""
        var pubDate = ""
        var language: String? = null
        var image: String? = null
        var caption: String? = null
        val depth = p.depth
        while (true) {
            val e = p.next()
            if (e == XmlPullParser.END_DOCUMENT) break
            if (e == XmlPullParser.END_TAG && p.depth == depth && p.name == "item") break
            if (e != XmlPullParser.START_TAG) continue
            when (p.name) {
                "title" -> title = p.readText()
                "link" -> link = p.readText()
                "author" -> author = p.readText().ifBlank { null }
                "guid" -> guid = p.readText()
                "description" -> description = p.readText()
                "pubDate" -> pubDate = p.readText()
                "language" -> language = p.readText().ifBlank { null }
                "media:thumbnail", "thumbnail", "media:content" -> if (image == null) {
                    image = p.getAttributeValue(null, "url")?.takeIf { it.startsWith("http") }
                    caption = p.getAttributeValue(null, "caption")?.let(NewsText::clean)?.ifBlank { null }
                }
                "flatplan:author" -> if (author == null) author = p.getAttributeValue(null, "name")?.ifBlank { null }
            }
        }
        val cleanTitle = NewsText.clean(title)
        if (cleanTitle.isBlank() || link.isBlank()) return null
        val key = guid.ifBlank { link }.trim()
        val dek = NewsText.snippet(NewsText.clean(description))
        return NewsItem(
            id = "${edition.code}-${NewsText.idSafe(key)}",
            category = NewsCategory.Reports,
            title = cleanTitle,
            dek = dek,
            body = "",
            source = SOURCE,
            publishedMs = NewsText.parseRfc822(pubDate) ?: 0L,
            readMinutes = 1,
            url = NewsText.stripTracking(link.trim()),
            imageUrl = image,
            imageCaption = caption,
            edition = edition,
            author = author?.let(NewsText::clean),
            language = language ?: edition.language,
        )
    }

    /** Text of the current element (CDATA included); leaves the parser on its END_TAG. */
    private fun XmlPullParser.readText(): String {
        val sb = StringBuilder()
        val d = depth
        while (true) {
            val e = next()
            when {
                e == XmlPullParser.TEXT || e == XmlPullParser.CDSECT -> sb.append(text)
                e == XmlPullParser.END_TAG && depth == d -> break
                e == XmlPullParser.END_DOCUMENT -> break
            }
        }
        return sb.toString()
    }
}

/** Text helpers for feed content, kept free of Android so they are unit tested on the JVM. */
object NewsText {
    private val tags = Regex("<[^>]+>")
    private val spaces = Regex("\\s+")
    private val numeric = Regex("&#(x?)([0-9a-fA-F]+);")
    private val named = mapOf(
        "&amp;" to "&", "&quot;" to "\"", "&apos;" to "'", "&#39;" to "'", "&lt;" to "<", "&gt;" to ">", "&nbsp;" to " ",
        "&laquo;" to "«", "&raquo;" to "»", "&hellip;" to "…", "&rsquo;" to "’", "&lsquo;" to "‘", "&ldquo;" to "“", "&rdquo;" to "”",
        "&ndash;" to "–", "&mdash;" to "—", "&eacute;" to "é", "&egrave;" to "è", "&agrave;" to "à", "&ccedil;" to "ç",
    )

    /** Strips tags, decodes leftover HTML entities (feeds sometimes double-encode) and collapses whitespace. */
    fun clean(s: String): String {
        var out = s.replace(tags, " ")
        repeat(2) { // twice handles "&amp;#39;"
            out = numeric.replace(out) { m ->
                val code = m.groupValues[2].toIntOrNull(if (m.groupValues[1].isNotEmpty()) 16 else 10)
                if (code != null && code in 1..0x10FFFF) String(Character.toChars(code)) else m.value
            }
            named.forEach { (k, v) -> out = out.replace(k, v) }
        }
        return out.replace(spaces, " ").trim()
    }

    /** 90min cuts descriptions at a fixed length mid-word; end them cleanly with an ellipsis. */
    fun snippet(s: String): String {
        if (s.isBlank()) return ""
        val last = s.trimEnd().last()
        if (last in ".!?…»\")”’") return s
        val cut = s.substring(0, s.lastIndexOf(' ').takeIf { it > s.length / 2 } ?: s.length).trimEnd(',', ';', ':', ' ', '-', '–')
        return "$cut…"
    }

    fun parseRfc822(s: String): Long? = runCatching { ZonedDateTime.parse(s.trim(), DateTimeFormatter.RFC_1123_DATE_TIME).toInstant().toEpochMilli() }.getOrNull()

    /** Removes the utm_source=RSS tracking parameter from story links. */
    fun stripTracking(url: String): String = url.replace(Regex("[?&]utm_source=RSS$"), "")

    /** Keeps ids safe for a navigation route segment. */
    fun idSafe(key: String): String = key.replace(Regex("[^A-Za-z0-9_-]"), "_").take(64)
}

/** Image helpers: 90min serves 2880px-wide images; ask its CDN for the size the UI draws. */
object NewsImages {
    private val width = Regex("c_fill,w_\\d+")

    fun sized(url: String?, widthPx: Int): String? {
        if (url == null) return null
        val bucket = when {
            widthPx <= 360 -> 360
            widthPx <= 720 -> 720
            widthPx <= 1080 -> 1080
            else -> 1440
        }
        return if (width.containsMatchIn(url)) url.replace(width, "c_fill,w_$bucket") else url
    }
}
