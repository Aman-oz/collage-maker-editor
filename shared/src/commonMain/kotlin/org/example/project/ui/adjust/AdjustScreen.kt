package org.example.project.ui.adjust

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ColorMatrix
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.example.project.ui.common.CenterFillSlider
import org.example.project.ui.common.ToolTopBar
import org.example.project.ui.preview.ThemePreviews
import org.jetbrains.compose.resources.vectorResource
import org.koin.compose.viewmodel.koinViewModel
import photocollagemaker.shared.generated.resources.Res
import photocollagemaker.shared.generated.resources.ic_before_after

private val AdjustRange = -100f..100f

@Composable
fun AdjustScreen(
    onBack: () -> Unit,
    onApplied: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: AdjustViewModel = koinViewModel(),
) {
    AdjustContent(
        sourceImage = viewModel.sourceImage,
        onBack = onBack,
        onApply = { matrix ->
            viewModel.sourceImage?.let { image -> viewModel.applyAdjustments(bakeAdjustments(image, matrix)) }
            onApplied()
        },
        modifier = modifier,
    )
}

@Composable
private fun AdjustContent(
    sourceImage: ImageBitmap?,
    onBack: () -> Unit,
    onApply: (FloatArray) -> Unit,
    modifier: Modifier = Modifier,
) {
    val scheme = MaterialTheme.colorScheme
    var values by remember { mutableStateOf(AdjustValues()) }
    var selected by remember { mutableStateOf(AdjustmentType.Brightness) }
    var comparing by remember { mutableStateOf(false) }

    val combinedMatrix = remember(values) { values.toColorMatrix() }
    val colorFilter = remember(combinedMatrix) { ColorFilter.colorMatrix(ColorMatrix(combinedMatrix)) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(scheme.surface)
            .safeDrawingPadding(),
    ) {
        ToolTopBar(
            title = "Adjust",
            onClose = onBack,
            onDone = { onApply(combinedMatrix) },
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
            if (sourceImage != null) {
                // aspectRatio sizes the Image to the photo itself, so the rounded clip follows the
                // photo's edges instead of the letterboxed stage.
                Image(
                    bitmap = sourceImage,
                    contentDescription = "Photo preview",
                    modifier = Modifier
                        .aspectRatio(sourceImage.width.toFloat() / sourceImage.height)
                        .clip(RoundedCornerShape(16.dp)),
                    contentScale = ContentScale.Fit,
                    colorFilter = if (comparing) null else colorFilter,
                )
            } else {
                Text(
                    text = "No image to adjust",
                    color = scheme.onSurface,
                    style = MaterialTheme.typography.bodyLarge,
                )
            }
        }

        CompareIconButton(
            onComparingChange = { comparing = it },
            modifier = Modifier
                .align(Alignment.End)
                .padding(top = 8.dp, end = 12.dp),
        )

        CenterFillSlider(
            value = values[selected],
            onValueChange = { values = values.with(selected, it) },
            range = AdjustRange,
            trackColor = scheme.onSurface.copy(alpha = 0.12f),
            fillColor = scheme.primary,
            thumbColor = scheme.primary,
            thumbWidth = 32.dp,
            thumbHeight = 18.dp,
            horizontalPadding = 12.dp,
            glassThumb = true,
            glassTint = if (scheme.surface.luminance() > 0.5f) Color.White.copy(alpha = 0.35f) else Color.White.copy(alpha = 0.12f),
        )

        AdjustmentTypeRow(
            selected = selected,
            values = values,
            onSelected = { selected = it },
        )
    }
}

/** Press-and-hold icon that shows the unadjusted photo while held. */
@Composable
private fun CompareIconButton(onComparingChange: (Boolean) -> Unit, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .size(40.dp)
            .clip(CircleShape)
            .pointerInput(onComparingChange) {
                detectTapGestures(
                    onPress = {
                        onComparingChange(true)
                        tryAwaitRelease()
                        onComparingChange(false)
                    },
                )
            },
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = vectorResource(Res.drawable.ic_before_after),
            contentDescription = "Press and hold to compare with the original",
            tint = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.size(20.dp),
        )
    }
}

@Composable
private fun AdjustmentTypeRow(
    selected: AdjustmentType,
    values: AdjustValues,
    onSelected: (AdjustmentType) -> Unit,
) {
    LazyRow(
        contentPadding = PaddingValues(start = 8.dp, end = 8.dp, top = 4.dp, bottom = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        items(AdjustmentType.entries) { type ->
            AdjustmentTypeItem(
                type = type,
                selected = type == selected,
                active = values[type] != 0f,
                onClick = { onSelected(type) },
            )
        }
    }
}

@Composable
private fun AdjustmentTypeItem(type: AdjustmentType, selected: Boolean, active: Boolean, onClick: () -> Unit) {
    val scheme = MaterialTheme.colorScheme
    val tint = if (selected) scheme.primary else scheme.onSurface
    Column(
        modifier = Modifier
            .width(72.dp)
            .clip(RoundedCornerShape(8.dp))
            .clickable(onClick = onClick)
            .padding(vertical = 6.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Icon(
            imageVector = type.icon,
            contentDescription = null,
            tint = tint,
            modifier = Modifier.size(26.dp),
        )
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = type.label,
            fontSize = 11.sp,
            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium,
            color = tint,
            textAlign = TextAlign.Center,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        // Marks adjustments that have been changed from zero, so edits on other tabs stay visible.
        Box(modifier = Modifier.padding(top = 4.dp).height(4.dp)) {
            if (active) {
                Box(
                    modifier = Modifier
                        .size(4.dp)
                        .clip(CircleShape)
                        .background(scheme.primary),
                )
            }
        }
    }
}

@Preview
@Composable
private fun AdjustScreenPreview() {
    ThemePreviews {
        AdjustContent(sourceImage = ImageBitmap(360, 480), onBack = {}, onApply = {})
    }
}
