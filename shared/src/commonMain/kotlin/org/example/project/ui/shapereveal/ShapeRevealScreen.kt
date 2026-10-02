package org.example.project.ui.shapereveal

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import kotlin.math.max
import kotlin.math.min
import org.example.project.i18n.tr
import org.example.project.ui.blur.BlurLevelDefault
import org.example.project.ui.blur.BlurLevelMax
import org.example.project.ui.blur.BlurLevelMin
import org.example.project.ui.blur.BlurLevelStepper
import org.example.project.ui.common.RevealShape
import org.example.project.ui.common.ToolScaffold
import org.example.project.ui.common.copyBitmap
import org.example.project.ui.preview.ThemePreviews

/** s-Blur: blur the whole photo, then move/scale/rotate a shape to reveal the original sharpness. */
@Composable
internal fun SelectiveBlurTool(sourceImage: ImageBitmap, onClose: () -> Unit, onApply: (ImageBitmap) -> Unit, modifier: Modifier = Modifier) {
    ShapeRevealContent(sourceImage = sourceImage, title = tr("s-Blur"), effect = RevealEffect.Blur, onBack = onClose, onDone = onApply, modifier = modifier)
}

/** s-Splash: grayscale the whole photo, then move/scale/rotate a shape to reveal the original colour. */
@Composable
internal fun SelectiveSplashTool(sourceImage: ImageBitmap, onClose: () -> Unit, onApply: (ImageBitmap) -> Unit, modifier: Modifier = Modifier) {
    ShapeRevealContent(sourceImage = sourceImage, title = tr("s-Splash"), effect = RevealEffect.Grayscale, onBack = onClose, onDone = onApply, modifier = modifier)
}

@Composable
private fun ShapeRevealContent(
    sourceImage: ImageBitmap?,
    title: String,
    effect: RevealEffect,
    onBack: () -> Unit,
    onDone: (ImageBitmap) -> Unit,
    modifier: Modifier = Modifier,
) {
    var placement by remember { mutableStateOf(ShapePlacement(shape = RevealShape.Circle)) }
    var blurLevel by remember { mutableIntStateOf(BlurLevelDefault) }

    val baseImage = remember(sourceImage, effect, blurLevel) {
        sourceImage?.let { buildEffectBitmap(it, effect, blurLevel) }
    }
    val revealImage = remember(sourceImage) { sourceImage?.let { copyBitmap(it) } }
    // Glyph shapes are measured per draw at many sizes (canvas, thumbnails, bake); a larger
    // cache than the default 8 keeps the thumbnail row from evicting the canvas's layout.
    val textMeasurer = rememberTextMeasurer(cacheSize = 48)

    val scheme = MaterialTheme.colorScheme

    ToolScaffold(modifier = modifier) {
        Box(
            modifier = Modifier
                .toolStage()
                .background(scheme.onSurface.copy(alpha = 0.08f))
                .padding(20.dp),
            contentAlignment = Alignment.Center,
        ) {
            if (sourceImage != null && baseImage != null && revealImage != null) {
                // aspectRatio sizes the canvas to the photo itself, so the rounded clip follows the
                // photo's edges and the placement fractions map straight onto the canvas.
                BoxWithConstraints(
                    modifier = Modifier
                        .aspectRatio(sourceImage.width.toFloat() / sourceImage.height)
                        .clip(RoundedCornerShape(24.dp)),
                ) {
                    val density = LocalDensity.current
                    val imageWidthPx = with(density) { maxWidth.toPx() }
                    val imageHeightPx = with(density) { maxHeight.toPx() }
                    val imageSizePx = Size(imageWidthPx, imageHeightPx)
                    val strokePx = with(density) { 1.dp.toPx() }

                    Canvas(
                        modifier = Modifier
                            .fillMaxSize()
                            .pointerInput(imageWidthPx, imageHeightPx) {
                                detectTransformGestures { _, pan, zoom, rotation ->
                                    placement = placement.copy(
                                        centerXFraction = (placement.centerXFraction + pan.x / imageWidthPx).coerceIn(0f, 1f),
                                        centerYFraction = (placement.centerYFraction + pan.y / imageHeightPx).coerceIn(0f, 1f),
                                        sizeFraction = (placement.sizeFraction * zoom).coerceIn(0.12f, 2.5f),
                                        rotationDegrees = placement.rotationDegrees + rotation,
                                    )
                                }
                            },
                    ) {
                        drawShapeReveal(base = baseImage, reveal = revealImage, placement = placement, imageOffset = Offset.Zero, imageSize = imageSizePx, textMeasurer = textMeasurer)
                        drawShapeOutline(
                            placement = placement,
                            imageOffset = Offset.Zero,
                            imageSize = imageSizePx,
                            textMeasurer = textMeasurer,
                            color = Color.White.copy(alpha = 0.8f),
                            strokeWidthPx = strokePx,
                        )
                    }
                }
            } else {
                Text(tr("No image to edit"), color = scheme.onSurface, style = MaterialTheme.typography.bodyLarge)
            }
        }

        ToolPanel(
            title = title,
            onClose = onBack,
            onDone = { if (baseImage != null && revealImage != null) onDone(bakeShapeReveal(baseImage, revealImage, placement, textMeasurer)) },
            doneEnabled = baseImage != null && revealImage != null,
        ) {
            Spacer(modifier = Modifier.height(16.dp))

            if (effect == RevealEffect.Blur) {
                BlurLevelStepper(value = blurLevel, onValueChange = { blurLevel = it.coerceIn(BlurLevelMin, BlurLevelMax) })
                Spacer(modifier = Modifier.height(8.dp))
            }

            ShapePickerRow(
                base = baseImage,
                reveal = revealImage,
                textMeasurer = textMeasurer,
                selected = placement.shape,
                onSelected = { placement = placement.copy(shape = it) },
            )

            Spacer(modifier = Modifier.height(8.dp))
        }
    }
}

private val ThumbnailShape = RoundedCornerShape(8.dp)

/** One thumbnail per shape, each previewing the actual photo revealed through that shape. */
@Composable
private fun ShapePickerRow(
    base: ImageBitmap?,
    reveal: ImageBitmap?,
    textMeasurer: TextMeasurer,
    selected: RevealShape,
    onSelected: (RevealShape) -> Unit,
) {
    val scheme = MaterialTheme.colorScheme
    LazyRow(
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        items(RevealShape.entries) { shape ->
            val isSelected = shape == selected
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .then(if (isSelected) Modifier.border(2.dp, scheme.primary, ThumbnailShape) else Modifier)
                    .padding(if (isSelected) 3.dp else 0.dp)
                    .clip(ThumbnailShape)
                    .background(scheme.onSurface.copy(alpha = 0.08f))
                    .clickable { onSelected(shape) },
            ) {
                if (base != null && reveal != null) {
                    Canvas(modifier = Modifier.fillMaxSize()) {
                        // Center-crop the photo into the square thumbnail (the Box clip trims the
                        // overflow) and size the shape to the visible square, not the image width.
                        val fill = max(size.width / base.width, size.height / base.height)
                        val imageSize = Size(base.width * fill, base.height * fill)
                        val imageOffset = Offset((size.width - imageSize.width) / 2f, (size.height - imageSize.height) / 2f)
                        val thumbPlacement = ShapePlacement(
                            shape = shape,
                            sizeFraction = 0.8f * min(size.width, size.height) / imageSize.width,
                        )
                        drawShapeReveal(base = base, reveal = reveal, placement = thumbPlacement, imageOffset = imageOffset, imageSize = imageSize, textMeasurer = textMeasurer)
                    }
                }
            }
        }
    }
}

@Preview
@Composable
private fun ShapeRevealPreview() {
    ThemePreviews {
        ShapeRevealContent(sourceImage = ImageBitmap(360, 480), title = tr("s-Blur"), effect = RevealEffect.Blur, onBack = {}, onDone = {})
    }
}
