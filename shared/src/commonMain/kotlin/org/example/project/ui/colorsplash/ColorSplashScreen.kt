package org.example.project.ui.colorsplash

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
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
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.roundToInt
import org.example.project.ui.common.CenterFillSlider
import org.example.project.ui.common.MaskBrushSizeDefault
import org.example.project.ui.common.MaskBrushSizeRange
import org.example.project.ui.common.MaskStroke
import org.example.project.ui.common.ToolTopBar
import org.example.project.ui.common.bakeMaskReveal
import org.example.project.ui.common.buildStrokePath
import org.example.project.ui.common.copyBitmap
import org.example.project.ui.common.drawMaskReveal
import org.example.project.ui.common.grayscaleBitmap
import org.example.project.ui.common.maskBrushRadiusFraction
import org.example.project.ui.common.strokeToPath
import org.example.project.ui.preview.ThemePreviews
import org.example.project.ui.reveal.RevealEditViewModel
import org.koin.compose.viewmodel.koinViewModel
import org.jetbrains.compose.resources.vectorResource
import photocollagemaker.shared.generated.resources.Res
import photocollagemaker.shared.generated.resources.ic_redo
import photocollagemaker.shared.generated.resources.ic_undo

/**
 * Color Splash (the LAS `SplashFragment` with `isSplashView=true`): the whole photo starts grayscale
 * and the user brushes to bring the original **colour** back. Same brush/reveal mechanic as the Blur
 * tool, only the base layer differs (grayscale instead of blur).
 */
@Composable
fun ColorSplashScreen(
    onBack: () -> Unit,
    onApplied: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: RevealEditViewModel = koinViewModel(),
) {
    ColorSplashContent(
        sourceImage = viewModel.sourceImage,
        onBack = onBack,
        onDone = { base, reveal, strokes ->
            viewModel.applyResult(bakeMaskReveal(base = base, reveal = reveal, strokes = strokes))
            onApplied()
        },
        modifier = modifier,
    )
}

@Composable
private fun ColorSplashContent(
    sourceImage: ImageBitmap?,
    onBack: () -> Unit,
    onDone: (base: ImageBitmap, reveal: ImageBitmap, strokes: List<MaskStroke>) -> Unit,
    modifier: Modifier = Modifier,
) {
    var strokes by remember { mutableStateOf(emptyList<MaskStroke>()) }
    var redoStack by remember { mutableStateOf(emptyList<MaskStroke>()) }
    var brushSize by remember { mutableFloatStateOf(MaskBrushSizeDefault) }
    var currentStroke by remember { mutableStateOf(emptyList<Offset>()) }
    var isDragging by remember { mutableStateOf(false) }
    var isAdjustingBrush by remember { mutableStateOf(false) }
    var cursorPositionPx by remember { mutableStateOf(Offset.Zero) }

    val grayImage = remember(sourceImage) { sourceImage?.let { grayscaleBitmap(it) } }
    val colorImage = remember(sourceImage) { sourceImage?.let { copyBitmap(it) } }

    val scheme = MaterialTheme.colorScheme

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(scheme.surface)
            .safeDrawingPadding(),
    ) {
        ToolTopBar(
            title = "Splash",
            onClose = onBack,
            onDone = { if (grayImage != null && colorImage != null) onDone(grayImage, colorImage, strokes) },
            doneEnabled = sourceImage != null,
        )

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .background(scheme.onSurface.copy(alpha = 0.08f))
                .padding(20.dp),
            contentAlignment = Alignment.Center,
        ) {
            if (sourceImage != null && grayImage != null && colorImage != null) {
                // aspectRatio sizes the canvas to the photo itself, so the rounded clip follows the
                // photo's edges and stroke fractions map straight onto the canvas with no letterbox.
                BoxWithConstraints(
                    modifier = Modifier
                        .aspectRatio(sourceImage.width.toFloat() / sourceImage.height)
                        .clip(RoundedCornerShape(16.dp)),
                ) {
                    val density = LocalDensity.current
                    val imageWidthPx = with(density) { maxWidth.toPx() }
                    val imageHeightPx = with(density) { maxHeight.toPx() }
                    val imageSizePx = Size(imageWidthPx, imageHeightPx)
                    val brushRadiusPx = maskBrushRadiusFraction(brushSize) * imageWidthPx
                    val imageCenterPx = Offset(imageWidthPx / 2f, imageHeightPx / 2f)

                    fun fractionFor(p: Offset) = Offset(
                        (p.x / imageWidthPx).coerceIn(0f, 1f),
                        (p.y / imageHeightPx).coerceIn(0f, 1f),
                    )

                    Canvas(
                        modifier = Modifier
                            .fillMaxSize()
                            .pointerInput(imageWidthPx, imageHeightPx) {
                                detectDragGestures(
                                    onDragStart = { position ->
                                        isDragging = true
                                        cursorPositionPx = position
                                        currentStroke = listOf(fractionFor(position))
                                    },
                                    onDragEnd = {
                                        isDragging = false
                                        if (currentStroke.isNotEmpty()) {
                                            strokes = strokes + MaskStroke(currentStroke, maskBrushRadiusFraction(brushSize))
                                            redoStack = emptyList()
                                        }
                                        currentStroke = emptyList()
                                    },
                                    onDragCancel = {
                                        isDragging = false
                                        currentStroke = emptyList()
                                    },
                                    onDrag = { change, _ ->
                                        change.consume()
                                        cursorPositionPx = change.position
                                        currentStroke = currentStroke + fractionFor(change.position)
                                    },
                                )
                            },
                    ) {
                        val committed = strokes.map { strokeToPath(it, Offset.Zero, imageSizePx) }
                        val live = if (currentStroke.isNotEmpty()) {
                            listOf(buildStrokePath(currentStroke, Offset.Zero, imageSizePx, brushRadiusPx))
                        } else {
                            emptyList()
                        }
                        drawMaskReveal(
                            base = grayImage,
                            reveal = colorImage,
                            imageOffset = Offset.Zero,
                            imageSize = imageSizePx,
                            revealPaths = committed + live,
                        )
                        if (isDragging || isAdjustingBrush) {
                            drawCircle(
                                color = Color.White,
                                radius = brushRadiusPx,
                                center = if (isDragging) cursorPositionPx else imageCenterPx,
                                style = Stroke(width = 2.dp.toPx()),
                            )
                        }
                    }
                }
            } else {
                Text("No image to edit", color = scheme.onSurface, style = MaterialTheme.typography.bodyLarge)
            }
        }

        Spacer(modifier = Modifier.height(28.dp))

        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center) {
            OutlinedSquareIconButton(
                icon = vectorResource(Res.drawable.ic_undo),
                contentDescription = "Undo",
                enabled = strokes.isNotEmpty(),
                onClick = {
                    val last = strokes.lastOrNull() ?: return@OutlinedSquareIconButton
                    redoStack = redoStack + last
                    strokes = strokes.dropLast(1)
                },
            )
            Spacer(modifier = Modifier.width(8.dp))
            OutlinedSquareIconButton(
                icon = vectorResource(Res.drawable.ic_redo),
                contentDescription = "Redo",
                enabled = redoStack.isNotEmpty(),
                onClick = {
                    val next = redoStack.lastOrNull() ?: return@OutlinedSquareIconButton
                    strokes = strokes + next
                    redoStack = redoStack.dropLast(1)
                },
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = "Brush Size",
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                color = scheme.onSurface,
                modifier = Modifier.weight(1f),
            )
            Text(
                text = "${brushSize.roundToInt()}",
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                color = scheme.primary,
            )
        }
        CenterFillSlider(
            value = brushSize,
            onValueChange = { brushSize = it },
            range = MaskBrushSizeRange,
            referenceValue = MaskBrushSizeRange.start,
            onDraggingChange = { isAdjustingBrush = it },
            trackColor = scheme.onSurface.copy(alpha = 0.12f),
            fillColor = scheme.primary,
            thumbColor = scheme.primary,
            thumbWidth = 26.dp,
            thumbHeight = 14.dp,
            horizontalPadding = 12.dp,
            glassThumb = true,
            glassTint = if (scheme.surface.luminance() > 0.5f) Color.White.copy(alpha = 0.35f) else Color.White.copy(alpha = 0.12f),
        )

        Spacer(modifier = Modifier.height(20.dp))
    }
}

private val SquareButtonShape = RoundedCornerShape(10.dp)

/** Outlined rounded-square undo/redo button with a faint fill; dims outline and glyph when disabled. */
@Composable
private fun OutlinedSquareIconButton(
    icon: ImageVector,
    contentDescription: String,
    enabled: Boolean,
    onClick: () -> Unit,
) {
    val onSurface = MaterialTheme.colorScheme.onSurface
    val tint = if (enabled) onSurface else onSurface.copy(alpha = 0.35f)
    Box(
        modifier = Modifier
            .size(36.dp)
            .clip(SquareButtonShape)
            .background(onSurface.copy(alpha = 0.03f))
            .border(width = 1.dp, color = onSurface.copy(alpha = if (enabled) 0.6f else 0.25f), shape = SquareButtonShape)
            .clickable(enabled = enabled, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            tint = tint,
            modifier = Modifier.size(18.dp),
        )
    }
}

@Preview
@Composable
private fun ColorSplashScreenPreview() {
    ThemePreviews {
        ColorSplashContent(sourceImage = ImageBitmap(360, 480), onBack = {}, onDone = { _, _, _ -> })
    }
}
