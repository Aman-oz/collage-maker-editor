package org.example.project.data.fonts

import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.platform.Font
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.withContext
import kotlinx.io.buffered
import kotlinx.io.files.Path
import kotlinx.io.files.SystemFileSystem
import kotlinx.io.readByteArray

internal actual suspend fun fontFamilyFromFile(path: String): FontFamily = withContext(Dispatchers.IO) {
    val bytes = SystemFileSystem.source(Path(path)).buffered().use { it.readByteArray() }
    FontFamily(Font(identity = path, data = bytes))
}
