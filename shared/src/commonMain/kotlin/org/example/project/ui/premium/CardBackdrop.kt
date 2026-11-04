package org.example.project.ui.premium

import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.produceState
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Canvas
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import kotlin.math.roundToInt
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.example.project.ui.blur.gaussianBlurArgb
import org.example.project.ui.common.copyBitmap
import org.example.project.ui.common.imageBitmapFromArgb
import org.jetbrains.compose.resources.imageResource

/** Longest side the backdrops are blurred at: they fill the header heavily blurred, so more adds nothing. */
private const val BackdropWorkingSide = 160

/** The backdrops' blur, in [BackdropWorkingSide] pixels: heavy, so only the card's colours remain. */
private const val BackdropBlurSigma = 10f

/**
 * The blurred picture of the card at [current] (an index into the cards [backdrops] was made from),
 * filling the box. It crossfades over a slide's length as the centred card changes, so it fades from
 * one card to the next while they move. Dimmed a little, so the cards stand off it, with its foot
 * faded into the page.
 */
@Composable
internal fun CardBackdrop(backdrops: List<ImageBitmap>?, current: Int, modifier: Modifier = Modifier) {
    val background = MaterialTheme.colorScheme.background
    Box(modifier = modifier) {
        Crossfade(
            targetState = backdrops?.get(current),
            animationSpec = tween(HeroSlideMillis),
            modifier = Modifier.fillMaxSize(),
            label = "cardBackdrop",
        ) { backdrop ->
            if (backdrop != null) {
                Image(
                    bitmap = backdrop,
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop,
                    filterQuality = FilterQuality.Low,
                )
            }
        }
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        0f to Color.Black.copy(alpha = 0.15f),
                        0.7f to Color.Black.copy(alpha = 0.05f),
                        1f to background,
                    ),
                ),
        )
    }
}

/**
 * Each card's picture shrunk and blurred hard, made once off the main thread; null until they all
 * are. Blurred here rather than with `Modifier.blur`, which does nothing before Android 12.
 */
@Composable
internal fun rememberBlurredBackdrops(cards: List<FeatureCard>): List<ImageBitmap>? {
    val images = cards.map { imageResource(it.image) }
    val state = produceState<List<ImageBitmap>?>(initialValue = null, images) {
        value = withContext(Dispatchers.Default) { images.map(::blurredBackdrop) }
    }
    return state.value
}

private fun blurredBackdrop(image: ImageBitmap): ImageBitmap {
    val scale = BackdropWorkingSide.toFloat() / maxOf(image.width, image.height)
    val width = (image.width * scale).roundToInt().coerceAtLeast(1)
    val height = (image.height * scale).roundToInt().coerceAtLeast(1)
    val small = ImageBitmap(width, height)
    Canvas(small).drawImageRect(
        // Copied first: a decoded resource doesn't redraw reliably as the source of a scaling draw.
        image = copyBitmap(image),
        srcOffset = IntOffset.Zero,
        srcSize = IntSize(image.width, image.height),
        dstOffset = IntOffset.Zero,
        dstSize = IntSize(width, height),
        paint = Paint().apply { filterQuality = FilterQuality.High },
    )
    val pixels = IntArray(width * height)
    small.readPixels(pixels)
    return imageBitmapFromArgb(gaussianBlurArgb(pixels, width, height, BackdropBlurSigma), width, height)
}
