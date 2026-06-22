package com.example.welive.intervention

data class ScreenRegion(
    val left: Int,
    val top: Int,
    val right: Int,
    val bottom: Int
) {
    val width: Int
        get() = (right - left).coerceAtLeast(0)

    val height: Int
        get() = (bottom - top).coerceAtLeast(0)

    fun contains(x: Int, y: Int): Boolean {
        return x in left until right && y in top until bottom
    }
}
