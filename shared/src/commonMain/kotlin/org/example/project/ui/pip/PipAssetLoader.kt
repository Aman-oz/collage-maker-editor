package org.example.project.ui.pip

import androidx.compose.ui.graphics.ImageBitmap
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import org.example.project.data.network.decodeImageBitmap
import org.example.project.ui.common.copyBitmap
import photocollagemaker.shared.generated.resources.Res

/**
 * Decodes the bundled PIP images out of compose resources. FileKit only decodes files on disk, so
 * the bytes go through [decodeImageBitmap] (both platforms handle the WebP frames and previews),
 * then [copyBitmap] so they redraw reliably as the source of a scaling draw.
 *
 * Only the small picker previews are memoized, for the grid to scroll back without re-decoding;
 * a frame and its masks are held by the editor that loaded them.
 */
class PipAssetLoader {

    private val previews = mutableMapOf<String, ImageBitmap>()
    private val mutex = Mutex()

    /** [template]'s picker thumbnail, or null if it could not be decoded. */
    suspend fun preview(template: PipTemplate): ImageBitmap? {
        val path = template.previewPath
        previews[path]?.let { return it }
        return mutex.withLock {
            // Re-check inside the lock: another tile may have decoded it while we waited.
            previews[path] ?: decode(path)?.also { previews[path] = it }
        }
    }

    /** [template]'s frame and every slot mask, or null if any of them failed to decode. */
    suspend fun frameAssets(template: PipTemplate): PipFrameAssets? {
        val frame = decode(template.framePath) ?: return null
        val masks = template.slots.indices.map { index -> decode(template.maskPath(index)) ?: return null }
        return PipFrameAssets(frame, masks)
    }

    private suspend fun decode(path: String): ImageBitmap? = runCatching {
        val bytes = Res.readBytes(path)
        withContext(Dispatchers.Default) { copyBitmap(decodeImageBitmap(bytes)) }
    }.getOrNull()
}

/**
 * A template's decoded images: the transparent [frame] drawn over everything, and one alpha mask
 * per slot, in slot order. The frame's pixel size is the space [PipSlot] coordinates live in.
 */
class PipFrameAssets(val frame: ImageBitmap, val masks: List<ImageBitmap>)
