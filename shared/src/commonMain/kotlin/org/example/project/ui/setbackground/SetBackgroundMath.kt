package org.example.project.ui.setbackground

import androidx.compose.ui.graphics.Color

/**
 * Colour swatches. The Colour panel is a two-row horizontal grid that fills column by column, so
 * the list is ordered as (top, bottom) pairs: a colour and its deeper shade.
 */
internal val BackdropSolidColors = listOf(
    Color.White, Color(0xFFE5E5EA),
    Color(0xFF1F2933), Color.Black,
    Color(0xFF1A8FD1), Color(0xFF136C94),
    Color(0xFF14204F), Color(0xFF050B4A),
    Color(0xFFF25565), Color(0xFFCB1F2E),
    Color(0xFFEE3F8C), Color(0xFFC8105A),
    Color(0xFF9747F5), Color(0xFF7B2FE0),
    Color(0xFFF6AE45), Color(0xFFE08A1E),
    Color(0xFF4CAF50), Color(0xFF2E7D32),
    Color(0xFF14B8A6), Color(0xFF0F766E),
)

/** Gradient presets for the Background panel, drawn top-left → bottom-right. */
internal val BackdropGradients = listOf(
    listOf(Color(0xFFFF9A9E), Color(0xFFFAD0C4)),
    listOf(Color(0xFFA18CD1), Color(0xFFFBC2EB)),
    listOf(Color(0xFF84FAB0), Color(0xFF8FD3F4)),
    listOf(Color(0xFFFCCB90), Color(0xFFD57EEB)),
    listOf(Color(0xFF43E97B), Color(0xFF38F9D7)),
    listOf(Color(0xFF4FACFE), Color(0xFF00F2FE)),
    listOf(Color(0xFFFA709A), Color(0xFFFEE140)),
    listOf(Color(0xFF30CFD0), Color(0xFF330867)),
    listOf(Color(0xFF667EEA), Color(0xFF764BA2)),
    listOf(Color(0xFF0F2027), Color(0xFF2C5364)),
)
