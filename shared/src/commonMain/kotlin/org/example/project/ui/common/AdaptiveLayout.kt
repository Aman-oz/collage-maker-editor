package org.example.project.ui.common

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

/**
 * The widest a phone-shaped column of content (forms, lists of rows, the paywall, onboarding) is
 * allowed to grow. On a tablet such content stretched edge to edge reads badly — rows become long
 * thin bars and buttons span the whole screen — so it stays this wide, centred, instead.
 */
val ReadableContentMaxWidth = 600.dp

/**
 * Fills the available width up to [ReadableContentMaxWidth] and centres itself in whatever is left.
 * A no-op on a phone, where the screen is narrower than the cap. Backgrounds that should still reach
 * the screen edges belong on the parent, not after this modifier.
 */
fun Modifier.readableWidth(): Modifier =
    fillMaxWidth()
        .wrapContentWidth()
        .widthIn(max = ReadableContentMaxWidth)
        .fillMaxWidth()

/**
 * The smallest cell of a photo/thumbnail grid. Chosen so a 360dp phone (less 16dp padding each side)
 * still fits three columns, as the grids were designed, while a tablet gets as many as fit instead of
 * three huge cells.
 */
val PhotoGridMinCellSize = 100.dp
