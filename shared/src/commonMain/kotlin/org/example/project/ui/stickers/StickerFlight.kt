package org.example.project.ui.stickers

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.layout
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.unit.Constraints
import kotlin.math.min
import kotlin.math.roundToInt

private const val StickerFlightDurationMs = 520

/** Leaves briskly and settles gently into place, so the landing reads as one soft motion. */
private val StickerFlightEasing = CubicBezierEasing(0.3f, 0f, 0.1f, 1f)

/** How high above the straight line the flight arcs, as a fraction of the distance travelled. */
private const val StickerFlightArcFraction = 0.18f

/**
 * A tapped sticker flying from its grid cell to the place on the photo where it is added: it grows
 * from the picture's size in the cell to its size on the photo along a gentle arc. The sticker is
 * only added once the flight lands, so the flying copy and the placed sticker are never both shown.
 *
 * Unlike the Emoji tool's `StickerFlights`, which flies a glyph to the canvas centre at a fixed font
 * size, this flies a picture between two rectangles, since a sticker lands at a size of its own.
 */
@Stable
internal class StickerImageFlights {
    internal class Flight(
        val id: Long,
        val image: ImageBitmap,
        val fromInRoot: Rect,
        val toInRoot: Rect,
        val onLanded: () -> Unit,
    )

    internal val flights = mutableStateListOf<Flight>()
    private var nextId = 0L

    /** Flies [image] from [fromInRoot] to [toInRoot], then calls [onLanded]; lands at once without both. */
    fun launch(image: ImageBitmap?, fromInRoot: Rect, toInRoot: Rect, onLanded: () -> Unit) {
        if (image == null || fromInRoot.isEmpty || toInRoot.isEmpty) {
            onLanded()
            return
        }
        flights += Flight(nextId++, image, fromInRoot, toInRoot, onLanded)
    }
}

/** [Rect] of an [aspectRatio] (width / height) picture fitted and centred in [bounds], as `ContentScale.Fit` draws it. */
internal fun fitRect(bounds: Rect, aspectRatio: Float): Rect {
    if (bounds.isEmpty || aspectRatio <= 0f) return bounds
    val width = min(bounds.width, bounds.height * aspectRatio)
    return Rect(bounds.center - Offset(width / 2f, width / aspectRatio / 2f), Size(width, width / aspectRatio))
}

/** Draws every sticker in flight. Place it last in the tool's root box, so they fly over panel and photo. */
@Composable
internal fun StickerImageFlightOverlay(flights: StickerImageFlights) {
    var origin by remember { mutableStateOf(Offset.Zero) }
    Box(modifier = Modifier.fillMaxSize().onGloballyPositioned { origin = it.positionInRoot() }) {
        for (flight in flights.flights) {
            key(flight.id) {
                FlyingStickerImage(
                    flight = flight,
                    from = flight.fromInRoot.translate(-origin),
                    to = flight.toInRoot.translate(-origin),
                    onFinished = { flights.flights.remove(flight) },
                )
            }
        }
    }
}

@Composable
private fun FlyingStickerImage(flight: StickerImageFlights.Flight, from: Rect, to: Rect, onFinished: () -> Unit) {
    val progress = remember { Animatable(0f) }
    val currentOnFinished by rememberUpdatedState(onFinished)

    LaunchedEffect(Unit) {
        var landed = false
        try {
            progress.animateTo(1f, tween(StickerFlightDurationMs, easing = StickerFlightEasing))
            flight.onLanded()
            landed = true
            // Hold the copy for two frames so the placed sticker has drawn before it goes; otherwise
            // the sticker blinks out for a frame.
            withFrameNanos {}
            withFrameNanos {}
            currentOnFinished()
        } finally {
            // Leaving the tool mid-flight still places the sticker that was tapped.
            if (!landed) flight.onLanded()
        }
    }

    Image(
        bitmap = flight.image,
        contentDescription = null,
        contentScale = ContentScale.Fit,
        modifier = Modifier.layout { measurable, constraints ->
            // Read in the layout pass, so each frame only re-places the picture: no recomposition.
            val t = progress.value
            val width = from.width + (to.width - from.width) * t
            val height = from.height + (to.height - from.height) * t
            val control = Offset(
                (from.center.x + to.center.x) / 2f,
                min(from.center.y, to.center.y) - (to.center - from.center).getDistance() * StickerFlightArcFraction,
            )
            val u = 1f - t
            val center = from.center * (u * u) + control * (2f * u * t) + to.center * (t * t)
            val placeable = measurable.measure(Constraints.fixed(width.roundToInt().coerceAtLeast(1), height.roundToInt().coerceAtLeast(1)))
            layout(constraints.maxWidth, constraints.maxHeight) {
                placeable.place((center.x - placeable.width / 2f).roundToInt(), (center.y - placeable.height / 2f).roundToInt())
            }
        },
    )
}
