package org.example.project.ui.templates

import kotlinx.serialization.Serializable

// --- Domain models used by the UI (Serializable so the selected frame can be a nav argument) ---

/** The aspect family of a template thumbnail; drives its cell shape in the masonry grid. */
@Serializable
enum class TemplateLayout(val aspectRatio: Float) {
    // width / height, matching the LAS dimensionRatios (0.67:1, 1.34:1, 1:1).
    Portrait(0.67f),
    Landscape(1.34f),
    Square(1f),
}

/** A selectable category shown as a pill in the category row (rvCategory). */
data class TemplateCategory(val id: String, val name: String)

/** One photo slot on a template, in frame-image pixel space; [x]/[y] are the slot center. */
@Serializable
data class TemplateSlot(
    val index: Int,
    val x: Float,
    val y: Float,
    val width: Float,
    val height: Float,
    val rotation: Float,
    val elevation: Float,
)

/** A template shown in the masonry grid (rvFrames) and edited in the Templates editor. */
@Serializable
data class TemplateFrame(
    val id: String,
    val thumbnailUrl: String,
    val imageUrl: String,
    val layout: TemplateLayout,
    val isPremium: Boolean,
    val slots: List<TemplateSlot> = emptyList(),
) {
    val slotCount: Int get() = slots.size
}
