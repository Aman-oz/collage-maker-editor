package org.example.project.data.network

import androidx.compose.ui.graphics.ImageBitmap
import io.ktor.client.HttpClient
import io.ktor.client.request.get
import io.ktor.client.statement.readRawBytes
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * Downloads and decodes network images (collage frame backgrounds and layout thumbnails) into
 * [ImageBitmap]s, memoizing each URL so a bitmap is fetched and decoded once per process.
 *
 * This is a deliberately small in-house loader rather than a full image library: the collage picker
 * only shows a handful of thumbnails at a time and needs each frame's bitmap in hand anyway (to bake
 * it into the final collage), so a URL→bitmap cache guarded by a [Mutex] covers the need without
 * pulling in another multiplatform dependency.
 */
class NetworkImageLoader(private val client: HttpClient) {

    private val cache = mutableMapOf<String, ImageBitmap>()
    private val mutex = Mutex()

    /** Returns the decoded bitmap for [url], or `null` if it could not be fetched/decoded. */
    suspend fun load(url: String): ImageBitmap? {
        cache[url]?.let { return it }
        return mutex.withLock {
            // Re-check inside the lock: another caller may have populated it while we waited.
            cache[url] ?: runCatching {
                val bytes = client.get(url).readRawBytes()
                decodeImageBitmap(bytes)
            }.getOrNull()?.also { cache[url] = it }
        }
    }
}
