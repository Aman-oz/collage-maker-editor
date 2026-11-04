package org.example.project.ui.overlay

import androidx.compose.ui.graphics.ImageBitmap
import org.example.project.ui.filter.mixFiltered
import org.example.project.ui.filter.renderFilterSteps

/**
 * [source] with [preset] blended over it at full strength, the texture cut to [textureMaxSide] (a
 * thumbnail's). Off the main thread, like the filters, which share its engine.
 */
internal suspend fun renderOverlay(source: ImageBitmap, preset: OverlayPreset, textureMaxSide: Int? = null): ImageBitmap =
    renderFilterSteps(source, preset.steps, textureMaxSide)

/**
 * Renders [source] with [preset] at [intensity] (0f..1f), at its own size: the same render the
 * live preview shows, mixed with the photo the way the preview's slider does, so they match.
 */
internal suspend fun bakeOverlay(source: ImageBitmap, preset: OverlayPreset, intensity: Float): ImageBitmap =
    mixFiltered(source, renderOverlay(source, preset), intensity)
