package org.example.project.ui.common

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.drag
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.layer.drawLayer
import androidx.compose.ui.graphics.rememberGraphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigationevent.NavigationEventInfo
import androidx.navigationevent.compose.NavigationBackHandler
import androidx.navigationevent.compose.rememberNavigationEventState
import kotlinx.coroutines.launch
import org.example.project.i18n.tr

/**
 * Picks a colour off the screen: [pick] hands [onPicked] the colour the user chose, or `null` if
 * they backed out (or the screen couldn't be captured).
 */
internal fun interface Eyedropper {
    fun pick(onPicked: (Color?) -> Unit)
}

/** The app's [Eyedropper], or `null` outside an [EyedropperHost] (previews), where there is none. */
internal val LocalEyedropper = staticCompositionLocalOf<Eyedropper?> { null }

private class EyedropperSession(val screen: ImageBitmap, val onPicked: (Color?) -> Unit)

/** Source pixels across the loupe; odd, so one of them is the centre. */
private const val LoupePixels = 15
private val LoupeSize = 112.dp
private val LoupeLift = 84.dp

/**
 * Hosts [content] with an [Eyedropper]. Whatever is on screen is recorded into a graphics layer
 * every frame, so a pick can snapshot it whole (the photo, a canvas, the editor around it) without
 * any screen having to hand its picture over. A dialog is a window of its own and isn't in it,
 * which is why the colour picker hides itself while the eyedropper is out.
 */
@Composable
internal fun EyedropperHost(modifier: Modifier = Modifier, content: @Composable () -> Unit) {
    val layer = rememberGraphicsLayer()
    val scope = rememberCoroutineScope()
    var session by remember { mutableStateOf<EyedropperSession?>(null) }
    val eyedropper = remember(layer) {
        Eyedropper { onPicked ->
            scope.launch {
                val screen = runCatching { layer.toImageBitmap().toReadable() }.getOrNull()
                if (screen == null || screen.width == 0 || screen.height == 0) {
                    onPicked(null)
                } else {
                    session = EyedropperSession(screen, onPicked)
                }
            }
        }
    }
    Box(modifier = modifier) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .drawWithContent {
                    layer.record { this@drawWithContent.drawContent() }
                    drawLayer(layer)
                },
        ) {
            CompositionLocalProvider(LocalEyedropper provides eyedropper) { content() }
        }
        session?.let { current ->
            EyedropperOverlay(
                screen = current.screen,
                onFinish = { color ->
                    session = null
                    current.onPicked(color)
                },
            )
        }
    }
}

/**
 * The eyedropper over the screen: a loupe that follows the finger (held above it, so the finger
 * doesn't cover what it magnifies) and shows the pixel under it, ringed in its colour. Lifting the
 * finger picks it; back gives up.
 */
@Composable
private fun EyedropperOverlay(screen: ImageBitmap, onFinish: (Color?) -> Unit) {
    var point by remember { mutableStateOf<Offset?>(null) }
    NavigationBackHandler(
        state = rememberNavigationEventState(NavigationEventInfo.None),
        isBackEnabled = true,
        onBackCompleted = { onFinish(null) },
    )
    Box(
        modifier = Modifier
            .fillMaxSize()
            .pointerInput(screen) {
                awaitEachGesture {
                    val down = awaitFirstDown()
                    down.consume()
                    point = down.position
                    var last = down.position
                    drag(down.id) { change ->
                        change.consume()
                        last = change.position
                        point = last
                    }
                    onFinish(screen.colorAt(screenPixel(last, screen, size)))
                }
            },
    ) {
        val shown = point
        if (shown == null) {
            Text(
                text = tr("Drag to pick a color"),
                color = Color.White,
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .safeDrawingPadding()
                    .padding(top = 24.dp)
                    .background(Color.Black.copy(alpha = 0.6f), CircleShape)
                    .padding(horizontal = 16.dp, vertical = 8.dp),
            )
        }
        Canvas(modifier = Modifier.fillMaxSize()) {
            if (shown == null) return@Canvas
            val pixel = screenPixel(shown, screen, IntSize(size.width.toInt(), size.height.toInt()))
            val color = screen.colorAt(pixel)
            val diameter = LoupeSize.toPx()
            val radius = diameter / 2f
            val lift = LoupeLift.toPx()
            // Above the finger, or below it when that would leave the top of the screen.
            val center = Offset(
                shown.x.coerceIn(radius, size.width - radius),
                if (shown.y - lift - radius >= 0f) shown.y - lift else shown.y + lift,
            )
            val topLeft = Offset(center.x - radius, center.y - radius)
            val half = LoupePixels / 2
            val source = IntOffset(
                (pixel.x - half).coerceIn(0, (screen.width - LoupePixels).coerceAtLeast(0)),
                (pixel.y - half).coerceIn(0, (screen.height - LoupePixels).coerceAtLeast(0)),
            )
            val cell = diameter / LoupePixels
            val loupe = Path().apply { addOval(Rect(topLeft, Size(diameter, diameter))) }
            clipPath(loupe) {
                drawRect(Color.Black, topLeft, Size(diameter, diameter))
                drawImage(
                    screen,
                    srcOffset = source,
                    srcSize = IntSize(minOf(LoupePixels, screen.width), minOf(LoupePixels, screen.height)),
                    dstOffset = IntOffset(topLeft.x.toInt(), topLeft.y.toInt()),
                    dstSize = IntSize(diameter.toInt(), diameter.toInt()),
                    filterQuality = FilterQuality.None,
                )
                // The picked pixel's cell, which near an edge isn't the loupe's middle one.
                val cellTopLeft = Offset(topLeft.x + (pixel.x - source.x) * cell, topLeft.y + (pixel.y - source.y) * cell)
                drawRect(Color.Black.copy(alpha = 0.5f), cellTopLeft, Size(cell, cell), style = Stroke(3.dp.toPx()))
                drawRect(Color.White, cellTopLeft, Size(cell, cell), style = Stroke(1.5.dp.toPx()))
            }
            drawCircle(color, radius - 4.dp.toPx(), center, style = Stroke(8.dp.toPx()))
            drawCircle(Color.White, radius, center, style = Stroke(2.dp.toPx()))
            drawCircle(Color.Black.copy(alpha = 0.3f), radius + 1.dp.toPx(), center, style = Stroke(1.dp.toPx()))
        }
    }
}

/** The [screen] pixel under [position] in a box of [size] (the snapshot is of the same box). */
private fun screenPixel(position: Offset, screen: ImageBitmap, size: IntSize): IntOffset = IntOffset(
    (position.x * screen.width / size.width.coerceAtLeast(1)).toInt().coerceIn(0, screen.width - 1),
    (position.y * screen.height / size.height.coerceAtLeast(1)).toInt().coerceIn(0, screen.height - 1),
)

/** The colour shown at [pixel], always opaque: what's on screen is what the eye picked. */
private fun ImageBitmap.colorAt(pixel: IntOffset): Color {
    val argb = IntArray(1)
    readPixels(argb, startX = pixel.x, startY = pixel.y, width = 1, height = 1)
    return Color(argb[0]).copy(alpha = 1f)
}
