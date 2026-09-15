package org.example.project.ui.collage.geom

/**
 * A mutable rectangle mirroring the subset of `android.graphics.RectF` the collage geometry uses
 * (`set`, `left/top/right/bottom`, `width()`, `height()`), so the ported LAS generators compile
 * unchanged. Coordinates are normalized `0f..1f` of the collage in [PhotoItem.bound].
 */
class RectF(
    var left: Float = 0f,
    var top: Float = 0f,
    var right: Float = 0f,
    var bottom: Float = 0f,
) {
    operator fun set(left: Float, top: Float, right: Float, bottom: Float) {
        this.left = left
        this.top = top
        this.right = right
        this.bottom = bottom
    }

    fun width(): Float = right - left

    fun height(): Float = bottom - top
}
