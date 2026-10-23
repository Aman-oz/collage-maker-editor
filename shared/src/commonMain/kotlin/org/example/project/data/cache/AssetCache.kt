package org.example.project.data.cache

import io.ktor.client.HttpClient
import io.ktor.client.plugins.onDownload
import io.ktor.client.request.get
import io.ktor.client.statement.bodyAsText
import io.ktor.client.statement.readRawBytes
import io.ktor.http.isSuccess
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.sync.withPermit
import kotlinx.coroutines.withContext
import kotlinx.io.buffered
import kotlinx.io.files.Path
import kotlinx.io.files.SystemFileSystem
import kotlinx.io.readByteArray
import org.example.project.data.centraldata.CentralDataApi

/** The app's cache directory on this platform: the app's own cache dir on Android, `Caches` on iOS. */
internal expect fun platformCacheDir(): String

/** How far a download has got: [fraction] 0..1, or null while the server hasn't said how big it is. */
data class DownloadProgress(val fraction: Float?)

/**
 * A disk cache for the files the editor downloads on demand: the Text tool's fonts, the Stickers
 * tool's SVGs and the layout editor's frame and background pictures. A file is *kept* (written under `assets/`, read from there from then on, with no
 * network) only when a caller asks for it; otherwise it is fetched for this use alone, so a premium
 * item a non-subscriber is only trying out never lands on the disk.
 *
 * Each download runs in the cache's own scope and is shared by everyone asking for the same key, so
 * a cell scrolled away or a font picked over mid-download doesn't throw the bytes away: they are
 * still kept if anyone asked for that. [progress] reports each one while it runs, for the rings on
 * the grid cells.
 */
class AssetCache(private val client: HttpClient, rootDir: String = platformCacheDir()) {

    private val keptDir = Path(rootDir, "assets")
    // Files that are needed on disk but not kept (Android draws a font only from a file); emptied
    // on each launch.
    private val tempDir = Path(rootDir, "assets-tmp")

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private val mutex = Mutex()
    private val inFlight = mutableMapOf<String, Deferred<ByteArray>>()
    private val keepRequested = mutableSetOf<String>()
    // More parallel downloads than this makes fonts.gstatic.com drop connections.
    private val downloads = Semaphore(4)

    private val _progress = MutableStateFlow<Map<String, DownloadProgress>>(emptyMap())
    val progress: StateFlow<Map<String, DownloadProgress>> = _progress.asStateFlow()

    init {
        scope.launch(Dispatchers.IO) {
            runCatching {
                SystemFileSystem.createDirectories(keptDir)
                if (SystemFileSystem.exists(tempDir)) SystemFileSystem.list(tempDir).forEach { SystemFileSystem.delete(it, mustExist = false) }
                SystemFileSystem.createDirectories(tempDir)
            }
        }
    }

    // Bumped whenever a file is kept, so [keptOf] checks its key again.
    private val keptVersion = MutableStateFlow(0)

    /**
     * Whether [key] is kept on the disk, checked again each time anything is kept: the download
     * icon on a library's cells, gone once the item (or its whole pack, from the Shop) is kept.
     */
    fun keptOf(key: String): Flow<Boolean> =
        keptVersion.map { isKept(key) }.distinctUntilChanged().flowOn(Dispatchers.IO)

    /** [key]'s download, while it runs. */
    fun progressOf(key: String): Flow<DownloadProgress?> = progress.map { it[key] }.distinctUntilChanged()

    fun isKept(key: String): Boolean = SystemFileSystem.exists(keptPath(key))

    /** [key]'s kept bytes, or null when it isn't kept: a read of the disk only, never the network. */
    suspend fun keptBytes(key: String): ByteArray? = read(keptPath(key))

    /**
     * [key]'s bytes: from the disk if kept, else downloaded by [download] (by default, [key] is the
     * URL), and written to the disk when [keep]. [track] puts the download in [progress].
     */
    suspend fun bytes(
        key: String,
        keep: Boolean,
        track: Boolean = true,
        download: suspend (onProgress: (sent: Long, total: Long?) -> Unit) -> ByteArray = { fetch(key, it) },
    ): ByteArray {
        read(keptPath(key))?.let { return it }
        val running = mutex.withLock {
            if (keep) keepRequested += key
            inFlight.getOrPut(key) { scope.async { runDownload(key, track, download) } }
        }
        val bytes = running.await()
        // The download may have finished before this caller asked for it to be kept.
        if (keep && !isKept(key)) write(keptPath(key), bytes)
        return bytes
    }

    /**
     * A file holding [key]'s bytes, for an API that only reads files: the kept one, or a copy in the
     * temporary directory when not [keep].
     */
    suspend fun file(
        key: String,
        keep: Boolean,
        track: Boolean = true,
        download: suspend (onProgress: (sent: Long, total: Long?) -> Unit) -> ByteArray = { fetch(key, it) },
    ): String {
        val kept = keptPath(key)
        if (SystemFileSystem.exists(kept)) return kept.toString()
        val bytes = bytes(key, keep, track, download)
        if (keep) return kept.toString()
        val temp = Path(tempDir, fileName(key))
        if (!SystemFileSystem.exists(temp)) write(temp, bytes)
        return temp.toString()
    }

    /** Downloads [url] and keeps it, in the background: what a tap on a free item starts. */
    fun prefetch(url: String) {
        scope.launch { runCatching { bytes(url, keep = true) } }
    }

    /** Keeps [bytes], already in memory, as [key]'s file. */
    fun keep(key: String, bytes: ByteArray) {
        scope.launch { if (!isKept(key)) runCatching { write(keptPath(key), bytes) } }
    }

    private suspend fun runDownload(
        key: String,
        track: Boolean,
        download: suspend (onProgress: (sent: Long, total: Long?) -> Unit) -> ByteArray,
    ): ByteArray {
        try {
            if (track) _progress.update { it + (key to DownloadProgress(null)) }
            val bytes = downloads.withPermit {
                download { sent, total ->
                    if (track && total != null && total > 0) {
                        // In steps of 5%, so the cells aren't recomposed for every packet.
                        val fraction = ((sent.toFloat() / total) * 20f).toInt() / 20f
                        _progress.update { if (key in it) it + (key to DownloadProgress(fraction.coerceIn(0f, 1f))) else it }
                    }
                }
            }
            if (mutex.withLock { key in keepRequested }) runCatching { write(keptPath(key), bytes) }
            return bytes
        } finally {
            mutex.withLock {
                inFlight.remove(key)
                keepRequested -= key
            }
            if (track) _progress.update { it - key }
        }
    }

    /** [url] over HTTP; the central-data server's own files need its API key. */
    private suspend fun fetch(url: String, onProgress: (sent: Long, total: Long?) -> Unit): ByteArray {
        val response = client.get(url) {
            CentralDataApi.authorize(this, url)
            onDownload { sent, total -> onProgress(sent, total) }
        }
        if (!response.status.isSuccess()) error("Download failed (${response.status.value}): ${response.bodyAsText().take(100)}")
        return response.readRawBytes()
    }

    private fun keptPath(key: String) = Path(keptDir, fileName(key))

    private suspend fun read(path: Path): ByteArray? = withContext(Dispatchers.IO) {
        try {
            if (!SystemFileSystem.exists(path)) null else SystemFileSystem.source(path).buffered().use { it.readByteArray() }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            null
        }
    }

    /** Written beside [path] and moved into place, so a crash never leaves half a file under its name. */
    private suspend fun write(path: Path, bytes: ByteArray) = withContext(Dispatchers.IO) {
        val parent = path.parent ?: return@withContext
        SystemFileSystem.createDirectories(parent)
        val part = Path(parent, "${path.name}.${randomSuffix()}.part")
        SystemFileSystem.sink(part).buffered().use { it.write(bytes) }
        SystemFileSystem.atomicMove(part, path)
        if (parent == keptDir) keptVersion.update { it + 1 }
    }

    private fun randomSuffix() = kotlin.random.Random.nextLong().toULong().toString(16)
}

/**
 * A file name for [key] (a URL, or a URL with a `#suffix`): a 64-bit FNV-1a hash of it, plus the
 * URL's extension so the files are recognisable.
 */
internal fun fileName(key: String): String {
    var hash = 0xcbf29ce484222325uL
    for (char in key) {
        hash = hash xor char.code.toULong()
        hash *= 0x100000001b3uL
    }
    val extension = key.substringBefore('?').substringBefore('#').substringAfterLast('/').substringAfterLast('.', "")
        .takeIf { it.length in 1..4 && it.all(Char::isLetterOrDigit) }
    return hash.toString(16).padStart(16, '0') + (extension?.let { ".$it" } ?: "")
}
