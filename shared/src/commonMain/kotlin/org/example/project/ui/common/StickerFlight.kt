package org.example.project.ui.common

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Text
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
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.layout
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.unit.sp
import androidx.compose.ui.unit.toSize
import kotlin.math.roundToInt

private const val StickerFlightDurationMs = 420

/** The glyph size in `EmojiGrid`'s cells, where every flight starts. */
internal const val EmojiGridFontSizeSp = 24f

/** How high above the straight line the flight arcs, as a fraction of the distance travelled. */
private const val StickerFlightArcFraction = 0.25f

/**
 * A tapped emoji flying from its grid cell to the spot on the canvas where its new sticker appears,
 * a shared-element-style hand-off between an emoji grid and the canvas that places it (the freestyle
 * layers, or the photo editor's Emoji tool).
 *
 * The sticker itself is only added once the flight lands ([launch]'s `onLanded`), so the flying copy
 * and the real sticker are never on screen at once. Both editors add new stickers centered at scale
 * 1, so the flight ends at the target's center at [landingFontSizeSp] — exactly where and how big
 * the sticker then draws.
 */
@Stable
internal class StickerFlights(val landingFontSizeSp: Float) {
    internal class Flight(val id: Long, val emoji: String, val fromBoundsInRoot: Rect, val onLanded: () -> Unit)

    internal val flights = mutableStateListOf<Flight>()
    private var nextId = 0L

    /** The landing canvas's bounds in root coordinates, reported by [stickerFlightTarget]. */
    internal var canvasBoundsInRoot by mutableStateOf(Rect.Zero)

    /**
     * Flies [emoji] from [fromBoundsInRoot] (the tapped cell) to the canvas center, then calls
     * [onLanded]. Without a known canvas it lands straight away.
     */
    fun launch(emoji: String, fromBoundsInRoot: Rect, onLanded: () -> Unit) {
        if (canvasBoundsInRoot.isEmpty || fromBoundsInRoot.isEmpty) {
            onLanded()
            return
        }
        flights += Flight(nextId++, emoji, fromBoundsInRoot, onLanded)
    }
}

@Composable
internal fun rememberStickerFlights(landingFontSizeSp: Float): StickerFlights =
    remember(landingFontSizeSp) { StickerFlights(landingFontSizeSp) }

/** Records the canvas's root bounds as the landing target for [flights]. */
internal fun Modifier.stickerFlightTarget(flights: StickerFlights?): Modifier =
    if (flights == null) {
        this
    } else {
        onGloballyPositioned { flights.canvasBoundsInRoot = Rect(it.positionInRoot(), it.size.toSize()) }
    }

/**
 * Draws every in-flight sticker. Place it last in the screen's root box so the stickers fly over
 * both the panel and the canvas.
 */
@Composable
internal fun StickerFlightOverlay(flights: StickerFlights, gridFontSizeSp: Float = EmojiGridFontSizeSp) {
    // Flights are in root coordinates; the overlay may not sit at the root's origin.
    var overlayOrigin by remember { mutableStateOf(Offset.Zero) }
    Box(modifier = Modifier.fillMaxSize().onGloballyPositioned { overlayOrigin = it.positionInRoot() }) {
        for (flight in flights.flights) {
            key(flight.id) {
                FlyingSticker(
                    flight = flight,
                    from = flight.fromBoundsInRoot.center - overlayOrigin,
                    to = flights.canvasBoundsInRoot.center - overlayOrigin,
                    fontSizeSp = flights.landingFontSizeSp,
                    startScale = gridFontSizeSp / flights.landingFontSizeSp,
                    onFinished = { flights.flights.remove(flight) },
                )
            }
        }
    }
}

@Composable
private fun FlyingSticker(
    flight: StickerFlights.Flight,
    from: Offset,
    to: Offset,
    fontSizeSp: Float,
    startScale: Float,
    onFinished: () -> Unit,
) {
    val progress = remember { Animatable(0f) }
    val currentOnFinished by rememberUpdatedState(onFinished)

    LaunchedEffect(Unit) {
        var landed = false
        try {
            progress.animateTo(1f, tween(StickerFlightDurationMs, easing = FastOutSlowInEasing))
            flight.onLanded()
            landed = true
            // Hold the copy for a frame so the layer, which arrives through the ViewModel's state,
            // has drawn before the copy goes — otherwise the sticker blinks out for a frame.
            withFrameNanos {}
            withFrameNanos {}
            currentOnFinished()
        } finally {
            // Leaving the screen mid-flight still adds the sticker the user tapped.
            if (!landed) flight.onLanded()
        }
    }

    Text(
        text = flight.emoji,
        fontSize = fontSizeSp.sp,
        maxLines = 1,
        softWrap = false,
        modifier = Modifier
            // Centers the text on the flight's current point along a quadratic arc from [from] to [to].
            .layout { measurable, constraints ->
                val placeable = measurable.measure(constraints.copy(minWidth = 0, minHeight = 0))
                layout(constraints.maxWidth, constraints.maxHeight) {
                    val t = progress.value
                    val control = Offset(
                        (from.x + to.x) / 2f,
                        minOf(from.y, to.y) - (to - from).getDistance() * StickerFlightArcFraction,
                    )
                    val u = 1f - t
                    val point = from * (u * u) + control * (2f * u * t) + to * (t * t)
                    placeable.place((point.x - placeable.width / 2f).roundToInt(), (point.y - placeable.height / 2f).roundToInt())
                }
            }
            .graphicsLayer {
                val scale = startScale + (1f - startScale) * progress.value
                scaleX = scale
                scaleY = scale
            },
    )
}
