package org.example.project.ui.filter

private val IdentityMatrix = floatArrayOf(
    1f, 0f, 0f, 0f, 0f,
    0f, 1f, 0f, 0f, 0f,
    0f, 0f, 1f, 0f, 0f,
    0f, 0f, 0f, 1f, 0f,
)

/**
 * Scales a 4x5 color [matrix] toward the identity by [intensity] (0..1). A color matrix is linear
 * in its entries, so lerping the matrix gives the same result as lerping the filtered and original
 * pixels, but costs nothing per pixel and still fits in a single `ColorFilter`.
 */
internal fun blendWithIdentity(matrix: FloatArray, intensity: Float): FloatArray {
    val t = intensity.coerceIn(0f, 1f)
    return FloatArray(matrix.size) { i -> IdentityMatrix[i] + (matrix[i] - IdentityMatrix[i]) * t }
}
