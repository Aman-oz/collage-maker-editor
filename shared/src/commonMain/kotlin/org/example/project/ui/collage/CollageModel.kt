package org.example.project.ui.collage

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap

/**
 * One image slot within a [CollageTemplate], its bounds normalized to `0f..1f` of the collage
 * square so the same template renders at any preview size or bake resolution.
 */
data class CollageSlot(val index: Int, val left: Float, val top: Float, val right: Float, val bottom: Float) {
    val width: Float get() = right - left
    val height: Float get() = bottom - top
}

/** One selectable slot layout for a given [imageCount]. */
data class CollageTemplate(val id: String, val label: String, val imageCount: Int, val slots: List<CollageSlot>)

/** An image placed into one of the current template's slots. */
data class CollageSlotImage(val slotIndex: Int, val image: ImageBitmap)

/** Everything needed to render (and later bake) the collage. */
data class CollageState(
    val template: CollageTemplate,
    val images: List<CollageSlotImage> = emptyList(),
    val borderWidth: Float = 8f,
    val borderColor: Color = Color.White,
    val cornerRadius: Float = 0f,
)
