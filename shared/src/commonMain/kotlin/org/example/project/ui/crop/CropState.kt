package org.example.project.ui.crop

import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.ImageBitmap
import org.example.project.ui.rotate.bakeQuarterTurnsAndFlip

/**
 * One crop edit in progress: the bitmap being cropped, the chosen ratio and the crop rect (in that
 * bitmap's pixel space).
 *
 * It is a holder rather than `remember`ed locals so the two crop UIs — the full-screen [CropContent]
 * and the editor's [CropTool] — share one implementation of the edit, and [CropStage] can be
 * handed the whole of it.
 */
@Stable
internal class CropState(sourceImage: ImageBitmap?) {

    var image by mutableStateOf(sourceImage)
        private set

    var selectedOption by mutableStateOf(AspectRatioOptions.first())
        private set

    var cropRect by mutableStateOf(sourceImage?.let(::fullImageRect))
        private set

    val canCrop: Boolean get() = image != null && cropRect != null

    /** Picks a ratio chip. A fixed ratio re-centers the rect; free-form keeps it where it is. */
    fun selectOption(option: AspectRatioOption) {
        selectedOption = option
        val image = image
        val ratio = option.ratio
        if (image != null && ratio != null) cropRect = centeredRectForRatio(image, ratio)
    }

    fun updateCropRect(rect: Rect) {
        cropRect = rect
    }

    /**
     * Rotates/flips the working bitmap itself (these are exact pixel moves), so the crop rect, the
     * canvas and [crop] all keep working in that bitmap's own coordinate space. The rect is
     * re-seeded for the new bitmap, keeping the chosen ratio.
     */
    fun transform(quarterTurns: Int, flipHorizontal: Boolean, flipVertical: Boolean) {
        val transformed = bakeQuarterTurnsAndFlip(image ?: return, quarterTurns, flipHorizontal, flipVertical)
        image = transformed
        cropRect = selectedOption.ratio?.let { centeredRectForRatio(transformed, it) } ?: fullImageRect(transformed)
    }

    /** The cropped bitmap, or `null` when there is nothing to crop. */
    fun crop(): ImageBitmap? {
        val image = image ?: return null
        val rect = cropRect ?: return null
        return cropImageBitmap(image, rect.toIntRectClamped(image))
    }
}
