package org.example.project.data.fonts

import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.net.URL

// On Android java.io.tmpdir is the app's own cache dir, so no Context is needed
private val cacheDir = File(System.getProperty("java.io.tmpdir"), "gfonts").apply { mkdirs() }

internal actual suspend fun loadFontFamily(url: String): FontFamily = withContext(Dispatchers.IO) {
    FontFamily(Font(cachedFontFile(url)))
}

private fun cachedFontFile(url: String): File {
    val file = File(cacheDir, url.substringAfter("/s/").replace('/', '_'))
    if (!file.exists()) {
        // unique temp name, renamed only once complete: a dropped connection or two rows
        // fetching the same font at once can never leave a broken file under the real name
        val part = File(file.path + "." + System.nanoTime() + ".part")
        val connection = URL(url).openConnection().apply { connectTimeout = 15_000; readTimeout = 30_000 }
        connection.getInputStream().use { input -> part.outputStream().use { input.copyTo(it) } }
        if (!part.renameTo(file)) part.delete() // another download finished first
    }
    return file
}
