package org.example.project.ui.collage.geom

/**
 * A mutable 2D point mirroring `android.graphics.PointF`, so the collage geometry generators and
 * [GeometryUtils] — ported almost verbatim from the LAS app — keep working unchanged.
 *
 * Equality is by value (like `android.graphics.PointF`) because [PhotoItem.shrinkMap] keys a
 * `HashMap` by these points. Code that needs *reference* identity (the convex-hull / neighbour
 * checks in [GeometryUtils]) uses `===`/`!==` explicitly, so value equality here is safe.
 */
class PointF(var x: Float = 0f, var y: Float = 0f) {

    fun set(x: Float, y: Float) {
        this.x = x
        this.y = y
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is PointF) return false
        return x == other.x && y == other.y
    }

    override fun hashCode(): Int = 31 * x.toBits() + y.toBits()

    override fun toString(): String = "PointF($x, $y)"
}
