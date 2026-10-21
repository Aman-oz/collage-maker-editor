package org.example.project.data.cache

import io.ktor.client.HttpClient
import kotlin.random.Random
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.async
import kotlinx.coroutines.test.runTest
import kotlinx.io.files.Path
import kotlinx.io.files.SystemFileSystem
import kotlinx.io.files.SystemTemporaryDirectory

class AssetCacheTest {

    private val root = Path(SystemTemporaryDirectory, "asset-cache-test-" + Random.nextLong().toULong().toString(16))
    private val client = HttpClient()
    private val cache = AssetCache(client, root.toString())

    @AfterTest
    fun cleanUp() {
        client.close()
        fun delete(path: Path) {
            if (SystemFileSystem.metadataOrNull(path)?.isDirectory == true) SystemFileSystem.list(path).forEach(::delete)
            SystemFileSystem.delete(path, mustExist = false)
        }
        delete(root)
    }

    @Test
    fun aKeptFileIsReadBackWithoutDownloading() = runTest {
        var downloads = 0
        val bytes = cache.bytes("https://x/a.svg", keep = true) { downloads++; byteArrayOf(1, 2, 3) }
        assertTrue(cache.isKept("https://x/a.svg"))
        val again = cache.bytes("https://x/a.svg", keep = false) { downloads++; byteArrayOf(9) }
        assertContentEquals(bytes, again)
        assertEquals(1, downloads)
    }

    @Test
    fun aFileNotAskedToBeKeptStaysOffTheDisk() = runTest {
        cache.bytes("https://x/premium.svg", keep = false) { byteArrayOf(4) }
        assertFalse(cache.isKept("https://x/premium.svg"))
        val file = cache.file("https://x/premium.ttf", keep = false) { byteArrayOf(5) }
        assertTrue(SystemFileSystem.exists(Path(file)))
        assertFalse(cache.isKept("https://x/premium.ttf"))
    }

    @Test
    fun callersAskingTogetherShareOneDownload() = runTest {
        var downloads = 0
        val gate = CompletableDeferred<Unit>()
        val first = async { cache.bytes("https://x/b.ttf", keep = false) { downloads++; gate.await(); byteArrayOf(7) } }
        val second = async { cache.bytes("https://x/b.ttf", keep = true) { downloads++; byteArrayOf(8) } }
        gate.complete(Unit)
        assertContentEquals(byteArrayOf(7), first.await())
        assertContentEquals(byteArrayOf(7), second.await())
        assertEquals(1, downloads)
        assertTrue(cache.isKept("https://x/b.ttf"))
    }

    @Test
    fun fileName_isStableKeepsTheExtensionAndTellsKeysApart() {
        assertEquals(fileName("https://x/s/a.ttf"), fileName("https://x/s/a.ttf"))
        assertTrue(fileName("https://x/s/a.ttf").endsWith(".ttf"))
        assertTrue(fileName("https://x/s/a.svg?v=2").endsWith(".svg"))
        assertNotEquals(fileName("https://x/s/a.ttf"), fileName("https://x/s/a.ttf#preview"))
        assertFalse(fileName("https://x/font?kit=abc").contains('?'))
    }
}
