package org.example.project.data.fonts

import androidx.compose.ui.text.font.FontFamily
import io.ktor.client.HttpClient
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.parameter
import io.ktor.client.statement.bodyAsText
import io.ktor.client.statement.readRawBytes
import io.ktor.http.HttpHeaders
import io.ktor.http.isSuccess
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import org.example.project.data.cache.AssetCache

/** The font in the file at [path] (Android draws a font only from a file). One actual per platform. */
internal expect suspend fun fontFamilyFromFile(path: String): FontFamily

/**
 * The Text tool's fonts, in two sizes:
 *
 * - [preview]: the family's name alone, for the lists. Google Fonts serves a font cut down to just
 *   the letters asked for (`css2?family=…&text=…`, ~10 KB rather than ~100 KB), so a grid of names
 *   doesn't download every whole font. It is kept on the disk: it is small, and the lists then show
 *   at once next time. Once the whole font is kept, that is used instead.
 * - [load]: the whole font, for the text itself, which is what picking a font downloads. [AssetCache]
 *   keeps it on the disk when asked, so it is there at once from then on.
 *
 * Both are remembered in memory too (least recently used dropped first).
 */
class FontLoader(private val cache: AssetCache, private val client: HttpClient) {

    private val lock = Mutex()
    private val memory = LinkedHashMap<String, FontFamily>() // insertion order = least recently used first

    /** The whole of [font], kept on the disk when [keep]; null when it can't be downloaded. */
    suspend fun load(font: FontEntry, keep: Boolean): FontFamily? = remembered(font.fileUrl) {
        fontFamilyFromFile(cache.file(font.fileUrl, keep))
    }

    /**
     * [font] cut down to the letters of [text] (its own name unless said otherwise), for drawing that
     * text; null when it can't be had.
     */
    suspend fun preview(font: FontEntry, text: String = font.family): FontFamily? {
        lock.withLock { memory[font.fileUrl] }?.let { return it }
        if (cache.isKept(font.fileUrl)) load(font, keep = true)?.let { return it }
        val key = previewKey(font, text)
        return remembered(key) {
            fontFamilyFromFile(cache.file(key, keep = true, track = false) { previewBytes(font, text) })
        }
            // Google can't cut some families (one with no regular weight, say): fetch it whole, for now.
            ?: remembered(font.fileUrl) { fontFamilyFromFile(cache.file(font.fileUrl, keep = false, track = false)) }
    }

    /** Asks Google Fonts for [font] cut to [text]'s letters: a stylesheet naming the cut file, then the file. */
    private suspend fun previewBytes(font: FontEntry, text: String): ByteArray {
        val css = client.get("https://fonts.googleapis.com/css2") {
            parameter("family", font.family)
            parameter("text", text.toSet().sorted().joinToString(""))
            // An old browser's agent gets TrueType, which both platforms read, rather than WOFF2.
            header(HttpHeaders.UserAgent, "Mozilla/4.0")
        }
        if (!css.status.isSuccess()) error("No preview for ${font.family} (${css.status.value})")
        val url = PreviewFontUrl.find(css.bodyAsText())?.groupValues?.get(1) ?: error("No preview for ${font.family}")
        val file = client.get(url)
        if (!file.status.isSuccess()) error("No preview for ${font.family} (${file.status.value})")
        return file.readRawBytes()
    }

    private suspend fun remembered(key: String, open: suspend () -> FontFamily): FontFamily? {
        lock.withLock { memory.remove(key)?.also { memory[key] = it } }?.let { return it }
        val family = try {
            open()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            return null
        }
        lock.withLock {
            memory[key] = family
            if (memory.size > MaxInMemory) memory.remove(memory.keys.first())
        }
        return family
    }

    private companion object {
        const val MaxInMemory = 200
        val PreviewFontUrl = Regex("""url\((https://[^)\s]+)\)""")

        // The name's cut keeps its old key, so previews already on the disk are still found.
        fun previewKey(font: FontEntry, text: String) =
            if (text == font.family) font.fileUrl + "#preview"
            else font.fileUrl + "#preview:" + text.toSet().sorted().joinToString("").hashCode()
    }
}
