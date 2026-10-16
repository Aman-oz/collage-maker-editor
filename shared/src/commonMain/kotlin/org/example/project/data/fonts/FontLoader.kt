package org.example.project.data.fonts

import androidx.compose.ui.text.font.FontFamily
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.sync.withPermit

/** Downloads [url] once into a disk cache and turns the file into a [FontFamily]. One actual per platform. */
internal expect suspend fun loadFontFamily(url: String): FontFamily

/** Font downloads, shared by every font name the Text tool draws in its own font. */
object FontLoader {
    // More parallel downloads than this makes fonts.gstatic.com drop connections.
    private val downloads = Semaphore(4)
    private val lock = Mutex()
    private val memory = LinkedHashMap<String, FontFamily>() // insertion order = least recently used first
    private const val MaxInMemory = 120

    /** Returns null when the font can't be downloaded; the caller keeps the default font. */
    suspend fun load(url: String): FontFamily? {
        lock.withLock { memory.remove(url)?.also { memory[url] = it } }?.let { return it }
        val family = try {
            downloads.withPermit { loadFontFamily(url) }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            return null
        }
        lock.withLock {
            memory[url] = family
            if (memory.size > MaxInMemory) memory.remove(memory.keys.first())
        }
        return family
    }
}
