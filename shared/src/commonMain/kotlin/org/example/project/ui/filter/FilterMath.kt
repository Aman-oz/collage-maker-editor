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

/**
 * Composes 4x5 color matrices so the result applies [first] and then each of [rest] in order. Each
 * matrix is treated as a 5x5 affine transform with an implicit `[0 0 0 0 1]` bottom row, which is
 * what lets the offset column carry through. Lets a filter be written as a readable chain of
 * simple steps (saturate, then contrast, then tint) and still bake to a single `ColorFilter`.
 */
internal fun concatColorMatrices(first: FloatArray, vararg rest: FloatArray): FloatArray =
    rest.fold(first) { applied, next ->
        FloatArray(20) { index ->
            val row = index / 5
            val col = index % 5
            var sum = 0f
            for (k in 0 until 4) sum += next[row * 5 + k] * applied[k * 5 + col]
            if (col == 4) sum += next[row * 5 + 4]
            sum
        }
    }
