package org.example.project.ui.common

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import org.example.project.data.cache.DownloadProgress

/**
 * A placeholder's loading shimmer: a faint block with a lighter band sweeping across it. The band is
 * read only while drawing, so it redraws the placeholder without recomposing it.
 */
@Composable
internal fun Modifier.shimmer(shape: Shape = RoundedCornerShape(8.dp)): Modifier {
    val base = MaterialTheme.colorScheme.onSurface
    val sweep by rememberInfiniteTransition(label = "shimmer").animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(1100, easing = LinearEasing)),
        label = "shimmerSweep",
    )
    return clip(shape).drawBehind {
        drawRect(base.copy(alpha = 0.08f))
        val band = size.width.coerceAtLeast(size.height) * 0.6f
        val start = -band + (size.width + band * 2f) * sweep
        drawRect(
            Brush.linearGradient(
                colors = listOf(Color.Transparent, base.copy(alpha = 0.10f), Color.Transparent),
                start = Offset(start, 0f),
                end = Offset(start + band, size.height),
            ),
        )
    }
}

/**
 * A small ring on a grid cell while its item downloads into the cache: it grows in, fills as the
 * bytes arrive (spins while the size is unknown), shows full for a moment once done and shrinks
 * away. It only watches: the item is usable meanwhile.
 */
@Composable
internal fun DownloadRing(progress: DownloadProgress?, modifier: Modifier = Modifier, size: Dp = 22.dp) {
    val scheme = MaterialTheme.colorScheme
    // The last progress seen, held (filled) a moment after the download ends.
    var shown by remember { mutableStateOf(progress) }
    LaunchedEffect(progress) {
        if (progress != null) {
            shown = progress
        } else if (shown != null) {
            shown = DownloadProgress(1f)
            delay(350)
            shown = null
        }
    }
    AnimatedVisibility(
        visible = shown != null,
        enter = fadeIn(tween(160)) + scaleIn(tween(220), initialScale = 0.5f),
        exit = fadeOut(tween(180)) + scaleOut(tween(220), targetScale = 0.5f),
        modifier = modifier,
    ) {
        val fraction = shown?.fraction
        val animated by animateFloatAsState(fraction ?: 0f, tween(250), label = "downloadRing")
        Box(
            modifier = Modifier
                .size(size)
                .background(scheme.surface.copy(alpha = 0.92f), CircleShape)
                .clip(CircleShape)
                .padding(3.dp),
        ) {
            if (fraction == null) {
                CircularProgressIndicator(color = scheme.primary, strokeWidth = 2.dp, modifier = Modifier.fillMaxSize())
            } else {
                CircularProgressIndicator(
                    progress = { animated },
                    color = scheme.primary,
                    trackColor = scheme.primary.copy(alpha = 0.18f),
                    strokeWidth = 2.dp,
                    modifier = Modifier.fillMaxSize(),
                )
            }
        }
    }
}
