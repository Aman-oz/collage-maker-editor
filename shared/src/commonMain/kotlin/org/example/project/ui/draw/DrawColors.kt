package org.example.project.ui.draw

import androidx.compose.ui.graphics.Color

/** One selectable paint color, drawn as a filled circle swatch. */
internal data class DrawColorOption(val label: String, val color: Color)

internal val DrawColors: List<DrawColorOption> = listOf(
    DrawColorOption("White", Color.White),
    DrawColorOption("Black", Color.Black),
    DrawColorOption("Silver", Color(0xFFC0C0C8)),
    DrawColorOption("Gray", Color(0xFF8E8E93)),
    DrawColorOption("Red", Color(0xFFEF4444)),
    DrawColorOption("Crimson", Color(0xFFD6336C)),
    DrawColorOption("Orange", Color(0xFFFF8C42)),
    DrawColorOption("Gold", Color(0xFFFFB300)),
    DrawColorOption("Yellow", Color(0xFFFFE135)),
    DrawColorOption("Green", Color(0xFF4CAF50)),
    DrawColorOption("Teal", Color(0xFF14B8A6)),
    DrawColorOption("Blue", Color(0xFF3B82F6)),
    DrawColorOption("Indigo", Color(0xFF6366F1)),
    DrawColorOption("Purple", Color(0xFF9333EA)),
    DrawColorOption("Pink", Color(0xFFEC4899)),
)

internal val DrawColorDefault: DrawColorOption = DrawColors.first { it.label == "Red" }
