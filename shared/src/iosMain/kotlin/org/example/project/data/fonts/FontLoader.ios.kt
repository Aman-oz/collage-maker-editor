package org.example.project.data.fonts

import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.platform.Font
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.addressOf
import kotlinx.cinterop.usePinned
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.withContext
import platform.Foundation.NSCachesDirectory
import platform.Foundation.NSData
import platform.Foundation.NSSearchPathForDirectoriesInDomains
import platform.Foundation.NSURL
import platform.Foundation.NSUserDomainMask
import platform.Foundation.dataWithContentsOfFile
import platform.Foundation.dataWithContentsOfURL
import platform.Foundation.writeToFile
import platform.posix.memcpy

private val cacheDir: String =
    NSSearchPathForDirectoriesInDomains(NSCachesDirectory, NSUserDomainMask, true).first() as String

@OptIn(ExperimentalForeignApi::class)
internal actual suspend fun loadFontFamily(url: String): FontFamily = withContext(Dispatchers.IO) {
    val path = "$cacheDir/gfonts_" + url.substringAfter("/s/").replace('/', '_')
    val data = NSData.dataWithContentsOfFile(path)
        ?: (NSURL.URLWithString(url)?.let { NSData.dataWithContentsOfURL(it) } ?: error("Download failed: $url"))
            .also { it.writeToFile(path, atomically = true) } // atomic write: no half-downloaded files in the cache
    val bytes = ByteArray(data.length.toInt())
    if (bytes.isNotEmpty()) bytes.usePinned { memcpy(it.addressOf(0), data.bytes, data.length) }
    FontFamily(Font(identity = path, data = bytes))
}
