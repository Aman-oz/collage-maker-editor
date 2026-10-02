package org.example.project.ui.save

import androidx.compose.ui.graphics.ImageBitmap
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.github.vinceglb.filekit.FileKit
import io.github.vinceglb.filekit.ImageFormat
import io.github.vinceglb.filekit.cacheDir
import io.github.vinceglb.filekit.div
import io.github.vinceglb.filekit.path
import io.github.vinceglb.filekit.write
import io.github.vinceglb.filekit.dialogs.compose.util.encodeToByteArray
import io.github.vinceglb.filekit.saveImageToGallery
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.example.project.data.AppSettings
import org.example.project.data.ImageEditSession
import org.example.project.data.ProjectsRepository
import org.example.project.data.SavedImageHandoff
import kotlin.time.Clock
import org.example.project.i18n.tr

sealed interface SaveStatus {
    data object Idle : SaveStatus
    data object Saving : SaveStatus
    /** [imagePath] is the cache copy of the exported file, handed on to the share screen. */
    data class Saved(val imagePath: String) : SaveStatus
    data class Failed(val message: String) : SaveStatus
}

private const val JpegQuality = 95

/** Exports the session's finished image to the device photo library, optionally watermarked. */
class SaveImageViewModel(
    private val session: ImageEditSession,
    settings: AppSettings,
    private val projects: ProjectsRepository,
    private val handoff: SavedImageHandoff,
) : ViewModel() {

    /** Premium users get no watermark, so the screen offers a single clean save. */
    val isPremium: StateFlow<Boolean> = settings.isPremium

    /** Snapshot of the finished image. The screen is only reachable once this is non-null. */
    val image: ImageBitmap? get() = session.image.value

    private val _status = MutableStateFlow<SaveStatus>(SaveStatus.Idle)
    val status: StateFlow<SaveStatus> = _status.asStateFlow()

    /** Saves [image], stamped with [watermark] when it is non-null. */
    fun save(watermark: ImageBitmap?) {
        val source = image ?: return
        if (_status.value == SaveStatus.Saving) return
        _status.value = SaveStatus.Saving
        viewModelScope.launch {
            _status.value = runCatching {
                val output = withContext(Dispatchers.Default) {
                    if (watermark != null) bakeWatermark(source, watermark) else source
                }
                val bytes = withContext(Dispatchers.Default) { output.encodeToByteArray(ImageFormat.JPEG, JpegQuality) }
                val filename = "collage_${Clock.System.now().toEpochMilliseconds()}.jpg"
                FileKit.saveImageToGallery(bytes, filename).getOrThrow()
                projects.add(bytes, filename)
                // The gallery copy has no stable path we can share on both platforms, so keep our own.
                val shareCopy = FileKit.cacheDir / filename
                shareCopy.write(bytes)
                // The share screen shows this very bitmap from its first frame (see SavedImageHandoff).
                handoff.put(shareCopy.path, output)
                shareCopy.path
            }.fold(
                onSuccess = { SaveStatus.Saved(it) },
                onFailure = { SaveStatus.Failed(it.message ?: tr("Could not save the image")) },
            )
        }
    }

    /** Resets a finished [SaveStatus.Saved]/[SaveStatus.Failed] once the screen has shown it. */
    fun consumeStatus() {
        if (_status.value != SaveStatus.Saving) _status.value = SaveStatus.Idle
    }
}
