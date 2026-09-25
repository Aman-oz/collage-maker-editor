package org.example.project.ui.collage

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import org.example.project.ui.collage.geom.TemplateItem

/** The canvas shapes offered by the Ratio tab; [aspect] is width / height. */
enum class CollageRatio(val label: String, val aspect: Float) {
    Square("1:1", 1f),
    Landscape("16:9", 16f / 9f),
    Portrait("4:5", 4f / 5f),
    Story("9:16", 9f / 16f),
}

/**
 * Everything needed to render (and later bake) the collage.
 *
 * @param template the selected layout geometry (from `collages.json` + the frame generators).
 * @param images decoded photos keyed by slot index; a slot with no entry shows an "add" affordance.
 * @param space the border gap between slots (the LAS `space`), as a preview-relative slider value.
 * @param corner the slot corner radius (the LAS `corner`), as a preview-relative slider value.
 * @param backgroundColor the colour shown behind/between the slots.
 * @param ratio the canvas aspect ratio; slot geometry is normalized, so it stretches to fit.
 */
data class CollageState(
    val template: TemplateItem,
    val images: Map<Int, ImageBitmap> = emptyMap(),
    val space: Float = 6f,
    val corner: Float = 0f,
    val backgroundColor: Color = Color.White,
    val ratio: CollageRatio = CollageRatio.Square,
)
