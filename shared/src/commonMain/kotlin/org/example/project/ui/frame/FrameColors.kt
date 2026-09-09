package org.example.project.ui.frame

import androidx.compose.ui.graphics.Color

/** One selectable frame color: [label] for accessibility/tests, [color] drawn as the border. */
internal data class FrameColorOption(val label: String, val color: Color)

/** [FrameColorOption.color] used by the "None" option — renders no border at all. */
internal val FrameColorNone = Color.Transparent

internal val FrameColors: List<FrameColorOption> = listOf(
    FrameColorOption("White", Color.White),
    FrameColorOption("None", FrameColorNone),
    FrameColorOption("Silver", Color(0xFFC0C0C8)),
    FrameColorOption("Gray", Color(0xFF8E8E93)),
    FrameColorOption("Red", Color(0xFFEF4444)),
    FrameColorOption("Orange", Color(0xFFFF8C42)),
    FrameColorOption("Amber", Color(0xFFFFB300)),
    FrameColorOption("Yellow", Color(0xFFFFE135)),
    FrameColorOption("Green", Color(0xFF4CAF50)),
    FrameColorOption("Teal", Color(0xFF14B8A6)),
    FrameColorOption("Cyan", Color(0xFF22D3EE)),
    FrameColorOption("Blue", Color(0xFF3B82F6)),
    FrameColorOption("Indigo", Color(0xFF6366F1)),
    FrameColorOption("Purple", Color(0xFF9333EA)),
    FrameColorOption("Magenta", Color(0xFFD946EF)),
    FrameColorOption("Pink", Color(0xFFEC4899)),
    FrameColorOption("Cream", Color(0xFFF5E6C8)),
    FrameColorOption("Brown", Color(0xFF8B5E34)),
    FrameColorOption("Gold", Color(0xFFD4AF37)),
)
