package org.example.project.ui.frame

import androidx.compose.ui.graphics.Color

/** One selectable frame color: [label] for accessibility/tests, [color] drawn as the border. */
internal data class FrameColorOption(val label: String, val color: Color)

/** [FrameColorOption.color] used by the "None" option — renders no border at all. */
internal val FrameColorNone = Color.Transparent

internal val FrameColors: List<FrameColorOption> = listOf(
    FrameColorOption("White", Color.White),
    FrameColorOption("Black", Color(0xFF1C1C1E)),
    FrameColorOption("Red", Color(0xFFEF1B36)),
    FrameColorOption("Orange", Color(0xFFFF7A3D)),
    FrameColorOption("Amber", Color(0xFFFFC23D)),
    FrameColorOption("Green", Color(0xFF3DDC6A)),
    FrameColorOption("Blue", Color(0xFF2F3DFF)),
    FrameColorOption("Purple", Color(0xFF9333EA)),
    FrameColorOption("Pink", Color(0xFFEC4899)),
    FrameColorOption("Teal", Color(0xFF14B8A6)),
    FrameColorOption("Cyan", Color(0xFF22D3EE)),
    FrameColorOption("Silver", Color(0xFFC0C0C8)),
    FrameColorOption("Gray", Color(0xFF8E8E93)),
    FrameColorOption("Cream", Color(0xFFF5E6C8)),
    FrameColorOption("Brown", Color(0xFF8B5E34)),
    FrameColorOption("Gold", Color(0xFFD4AF37)),
    FrameColorOption("None", FrameColorNone),
)
