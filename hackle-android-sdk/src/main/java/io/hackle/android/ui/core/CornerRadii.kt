package io.hackle.android.ui.core

/*
 *   2          3
 * 1 ┌──────────┐ 4
 *   │          │
 *   │          │
 * 8 └──────────┘ 5
 *   7          6
 */
internal data class CornerRadii(
    val v1: Float,
    val v2: Float,
    val v3: Float,
    val v4: Float,
    val v5: Float,
    val v6: Float,
    val v7: Float,
    val v8: Float
) {
    fun toFloatArray(): FloatArray {
        return floatArrayOf(v1, v2, v3, v4, v5, v6, v7, v8)
    }

    companion object {
        val ZERO = of(0f)

        fun of(radius: Float): CornerRadii {
            return CornerRadii(radius, radius, radius, radius, radius, radius, radius, radius)
        }

        fun of(topLeft: Float, topRight: Float, bottomRight: Float, bottomLeft: Float): CornerRadii {
            return CornerRadii(
                topLeft,
                topLeft,
                topRight,
                topRight,
                bottomRight,
                bottomRight,
                bottomLeft,
                bottomLeft
            )
        }
    }
}
