package org.example.project.data.network

import androidx.compose.ui.graphics.ImageBitmap
import io.ktor.client.HttpClient
import io.ktor.client.request.get
import io.ktor.client.statement.readRawBytes
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.async
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import org.example.project.data.centraldata.CentralDataApi

/**
 * Downloads and decodes network images (collage frame backgrounds, layout thumbnails and the
 * sticker library's SVGs) into
 * [ImageBitmap]s, memoizing each URL so a bitmap is fetched and decoded once per process.
 *
 * This is a deliberately small in-house loader rather than a full image library: the collage picker
 * only shows a handful of thumbnails at a time and needs each frame's bitmap in hand anyway (to bake
 * it into the final collage), so a URL→bitmap cache covers the need without pulling in another
 * multiplatform dependency.
 *
 * Downloads run in parallel, so a strip of thumbnails fills in together rather than one by one; the
 * [Mutex] only guards the maps. Callers asking for a URL that is already downloading share that
 * download. It runs in the loader's own scope, so a thumbnail scrolled away mid-download still lands
 * in the cache. A failed download is not cached, so the next request tries again.
 */
class NetworkImageLoader(private val client: HttpClient) {

    private val cache = mutableMapOf<String, ImageBitmap>()
    private val inFlight = mutableMapOf<String, Deferred<ImageBitmap?>>()
    private val svgSources = mutableMapOf<String, ByteArray>()
    private val mutex = Mutex()
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    /**
     * Returns the decoded bitmap for [url], or `null` if it could not be fetched/decoded. An SVG has
     * no pixel size of its own, so it is drawn with its longer side [svgSidePx]; each size is cached
     * on its own, from one download.
     */
    suspend fun load(url: String, svgSidePx: Int = DefaultSvgSidePx): ImageBitmap? {
        val svg = isSvgUrl(url)
        val key = if (svg) "$url#$svgSidePx" else url
        val download = mutex.withLock {
            cache[key]?.let { return it }
            inFlight.getOrPut(key) {
                scope.async {
                    val bitmap = runCatching {
                        if (svg) rasterizeSvg(svgSource(url), svgSidePx) else decodeImageBitmap(fetch(url))
                    }.getOrNull()
                    mutex.withLock {
                        inFlight.remove(key)
                        if (bitmap != null) cache[key] = bitmap
                    }
                    bitmap
                }
            }
        }
        return download.await()
    }

    /**
     * [url]'s SVG drawn with its longer side [sidePx], without keeping the bitmap: for a one-off
     * full-resolution bake, which would only fill the cache. The SVG source itself is kept.
     */
    suspend fun renderSvg(url: String, sidePx: Int): ImageBitmap? =
        runCatching { rasterizeSvg(svgSource(url), sidePx) }.getOrNull()

    /** An SVG's bytes, downloaded once: they are small, and each new size is drawn from them. */
    private suspend fun svgSource(url: String): ByteArray =
        mutex.withLock { svgSources[url] } ?: fetch(url).also { bytes -> mutex.withLock { svgSources[url] = bytes } }

    // The central-data server's images need its API key; other hosts get no header.
    private suspend fun fetch(url: String): ByteArray =
        client.get(url) { CentralDataApi.authorize(this, url) }.readRawBytes()

    private companion object {
        const val DefaultSvgSidePx = 256
    }
}
