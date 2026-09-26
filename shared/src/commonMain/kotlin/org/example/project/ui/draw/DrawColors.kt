package org.example.project.ui.draw

import androidx.compose.ui.graphics.Color

/** One selectable paint color, drawn as a filled rounded-square swatch. */
internal data class DrawColorOption(val label: String, val color: Color)

internal val DrawColors: List<DrawColorOption> = listOf(
    DrawColorOption("Charcoal", Color(0xFF1F2A30)),
    DrawColorOption("Blue", Color(0xFF3B8FD0)),
    DrawColorOption("Navy", Color(0xFF14163F)),
    DrawColorOption("Red", Color(0xFFEF4360)),
    DrawColorOption("Purple", Color(0xFF9B3FE0)),
    DrawColorOption("Amber", Color(0xFFF0AA40)),
    DrawColorOption("White", Color.White),
    DrawColorOption("Black", Color.Black),
    DrawColorOption("Gray", Color(0xFF8E8E93)),
    DrawColorOption("Orange", Color(0xFFFF8C42)),
    DrawColorOption("Yellow", Color(0xFFFFE135)),
    DrawColorOption("Green", Color(0xFF4CAF50)),
    DrawColorOption("Teal", Color(0xFF14B8A6)),
    DrawColorOption("Pink", Color(0xFFEC4899)),
)

internal val DrawColorDefault: DrawColorOption = DrawColors.first()
