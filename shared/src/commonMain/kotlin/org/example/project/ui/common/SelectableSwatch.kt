package org.example.project.ui.common

import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/** Corner radius of a [SelectableSwatch]'s content; its selection ring is this plus the gap. */
internal val SwatchInnerCorner = 10.dp

/**
 * Rounded-square picker slot of [size] used by the theme-aware tool screens (Draw colors and
 * mosaics, Frame colors). Selected, it gains an accent ring with a small gap around [content];
 * unselected, the content fills the slot minus that same gap so rows don't shift when the
 * selection moves.
 */
@Composable
internal fun SelectableSwatch(selected: Boolean, size: Dp, onClick: () -> Unit, content: @Composable () -> Unit) {
    val ring = RoundedCornerShape(SwatchInnerCorner + 4.dp)
    Box(
        modifier = Modifier
            .size(size)
            .clip(ring)
            .then(if (selected) Modifier.border(2.dp, MaterialTheme.colorScheme.primary, ring) else Modifier)
            .clickable(onClick = onClick)
            .padding(4.dp)
            .clip(RoundedCornerShape(SwatchInnerCorner)),
        contentAlignment = Alignment.Center,
    ) {
        content()
    }
}
