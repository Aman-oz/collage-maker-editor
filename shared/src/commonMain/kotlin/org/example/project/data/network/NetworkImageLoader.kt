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
 * Downloads and decodes network images (collage frame backgrounds and layout thumbnails) into
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
    private val mutex = Mutex()
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    /** Returns the decoded bitmap for [url], or `null` if it could not be fetched/decoded. */
    suspend fun load(url: String): ImageBitmap? {
        val download = mutex.withLock {
            cache[url]?.let { return it }
            inFlight.getOrPut(url) {
                scope.async {
                    val bitmap = runCatching {
                        // The central-data server's images need its API key; other hosts get no header.
                        val bytes = client.get(url) { CentralDataApi.authorize(this, url) }.readRawBytes()
                        decodeImageBitmap(bytes)
                    }.getOrNull()
                    mutex.withLock {
                        inFlight.remove(url)
                        if (bitmap != null) cache[url] = bitmap
                    }
                    bitmap
                }
            }
        }
        return download.await()
    }
}
