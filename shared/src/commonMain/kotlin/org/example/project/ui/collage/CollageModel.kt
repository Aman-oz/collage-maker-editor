package org.example.project.ui.collage

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import org.example.project.ui.collage.geom.TemplateItem

/**
 * Everything needed to render (and later bake) the collage.
 *
 * @param template the selected layout geometry (from `collages.json` + the frame generators).
 * @param images decoded photos keyed by slot index; a slot with no entry shows an "add" affordance.
 * @param space the border gap between slots (the LAS `space`), as a preview-relative slider value.
 * @param corner the slot corner radius (the LAS `corner`), as a preview-relative slider value.
 * @param backgroundColor the colour shown behind/between the slots.
 */
data class CollageState(
    val template: TemplateItem,
    val images: Map<Int, ImageBitmap> = emptyMap(),
    val space: Float = 6f,
    val corner: Float = 0f,
    val backgroundColor: Color = Color.White,
)
