package org.example.project.ui.ratio

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.min
import org.example.project.ui.common.CenterFillSlider
import org.example.project.ui.common.ToolTopBar
import org.example.project.ui.crop.AspectRatioOptions
import org.example.project.ui.crop.AspectRatioStrip
import org.example.project.ui.preview.ThemePreviews
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun RatioScreen(
    onBack: () -> Unit,
    onApplied: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: RatioViewModel = koinViewModel(),
) {
    RatioContent(
        sourceImage = viewModel.sourceImage,
        onBack = onBack,
        onApply = { bitmap ->
            viewModel.applyRatio(bitmap)
            onApplied()
        },
        modifier = modifier,
    )
}

@Composable
private fun RatioContent(
    sourceImage: ImageBitmap?,
    onBack: () -> Unit,
    onApply: (ImageBitmap) -> Unit,
    modifier: Modifier = Modifier,
) {
    val scheme = MaterialTheme.colorScheme
    var selected by remember { mutableStateOf(AspectRatioOptions.first()) }
    var paddingPercent by remember { mutableFloatStateOf(0f) }

    // Recomputed only when the ratio changes. Padding stays a live layout inset on the preview and
    // is baked once, on Done, so dragging the slider never re-renders the full-resolution photo.
    val reframed = remember(sourceImage, selected) {
        sourceImage?.let { reframeImage(it, selected.ratio) }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(scheme.surface)
            .safeDrawingPadding(),
    ) {
        ToolTopBar(
            title = "Ratio",
            onClose = onBack,
            doneEnabled = reframed != null,
            onDone = { reframed?.let { onApply(padImage(it, paddingPercent)) } },
        )

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .background(scheme.onSurface.copy(alpha = 0.08f))
                .padding(24.dp),
            contentAlignment = Alignment.Center,
        ) {
            if (reframed != null) {
                // Sized to the bitmap's own aspect so the percent padding measures the same
                // shorter side the bake uses; Crop mirrors padImage's center-crop into the inset.
                BoxWithConstraints(
                    modifier = Modifier
                        .aspectRatio(reframed.width.toFloat() / reframed.height)
                        .background(RatioPaddingColor),
                ) {
                    Image(
                        bitmap = reframed,
                        contentDescription = "Ratio preview",
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(min(maxWidth, maxHeight) * paddingPercent / 100f),
                        contentScale = if (paddingPercent > 0f) ContentScale.Crop else ContentScale.Fit,
                    )
                }
            } else {
                Text(text = "No image to reframe", color = scheme.onSurface)
            }
        }

        CenterFillSlider(
            value = paddingPercent,
            onValueChange = { paddingPercent = it },
            range = PaddingPercentRange,
            referenceValue = PaddingPercentRange.start,
            trackColor = scheme.onSurface.copy(alpha = 0.12f),
            fillColor = scheme.primary,
            thumbColor = scheme.primary,
            thumbWidth = 26.dp,
            thumbHeight = 14.dp,
            horizontalPadding = 12.dp,
            glassThumb = true,
            glassTint = if (scheme.surface.luminance() > 0.5f) Color.White.copy(alpha = 0.35f) else Color.White.copy(alpha = 0.12f),
            modifier = Modifier.padding(top = 12.dp),
        )

        AspectRatioStrip(
            options = AspectRatioOptions,
            selected = selected,
            onSelected = { selected = it },
        )
    }
}

@Preview
@Composable
private fun RatioScreenPreview() {
    ThemePreviews {
        RatioContent(sourceImage = ImageBitmap(360, 480), onBack = {}, onApply = {})
    }
}
