package org.example.project.ui.adjust

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
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
import androidx.compose.material.icons.automirrored.filled.ArrowBack
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.roundToInt
import org.example.project.ui.common.AccentPillButton
import org.example.project.ui.common.CenterFillSlider
import org.example.project.ui.common.CompareButton
import org.example.project.ui.common.EditorAccent
import org.example.project.ui.common.EditorBackground
import org.example.project.ui.common.EditorCanvasBackground
import org.example.project.ui.common.EditorCircleIconButton
import org.example.project.ui.common.EditorControlBackground
import org.example.project.ui.common.EditorIconTint
import org.example.project.ui.common.EditorLabelTint
import org.example.project.ui.common.EditorOnAccent
import org.example.project.ui.preview.ThemePreviews
import org.koin.compose.viewmodel.koinViewModel

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
    var values by remember { mutableStateOf(AdjustValues()) }
    var selected by remember { mutableStateOf(AdjustmentType.Brightness) }
    var showOriginal by remember { mutableStateOf(false) }

    val combinedMatrix = remember(values) { values.toColorMatrix() }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(EditorBackground)
            .safeDrawingPadding(),
    ) {
        AdjustTopBar(
            onBack = onBack,
            onApply = { onApply(combinedMatrix) },
            applyEnabled = sourceImage != null,
        )

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .padding(16.dp)
                .clip(RoundedCornerShape(24.dp))
                .background(EditorCanvasBackground),
            contentAlignment = Alignment.Center,
        ) {
            if (sourceImage != null) {
                Image(
                    bitmap = sourceImage,
                    contentDescription = "Photo preview",
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Fit,
                    colorFilter = if (showOriginal) null else ColorFilter.colorMatrix(ColorMatrix(combinedMatrix)),
                )
            } else {
                Text(
                    text = "No image to adjust",
                    color = Color.White,
                    style = MaterialTheme.typography.bodyLarge,
                )
            }
        }

        AdjustControlRow(
            selected = selected,
            value = values[selected],
            onComparePressedChange = { showOriginal = it },
        )

        CenterFillSlider(
            value = values[selected],
            onValueChange = { values = values.with(selected, it) },
            range = AdjustRange,
        )

        AdjustmentTypeRow(
            selected = selected,
            values = values,
            onSelected = { selected = it },
        )
    }
}

@Composable
private fun AdjustTopBar(onBack: () -> Unit, onApply: () -> Unit, applyEnabled: Boolean) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        EditorCircleIconButton(icon = Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", onClick = onBack)

        Text(
            text = "Adjust",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = Color.White,
            textAlign = TextAlign.Center,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier
                .weight(1f)
                .padding(horizontal = 4.dp),
        )

        AccentPillButton(text = "Apply", onClick = onApply, enabled = applyEnabled)
    }
}

@Composable
private fun AdjustControlRow(
    selected: AdjustmentType,
    value: Float,
    onComparePressedChange: (Boolean) -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = selected.label,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = Color.White,
            modifier = Modifier.weight(1f),
        )
        Text(
            text = formatAdjustValue(value),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = EditorAccent,
            modifier = Modifier.padding(end = 12.dp),
        )
        CompareButton(onPressedChange = onComparePressedChange)
    }
}

private fun formatAdjustValue(value: Float): String {
    val rounded = value.roundToInt()
    return if (rounded > 0) "+$rounded" else "$rounded"
}

@Composable
private fun AdjustmentTypeRow(
    selected: AdjustmentType,
    values: AdjustValues,
    onSelected: (AdjustmentType) -> Unit,
) {
    LazyRow(
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(20.dp),
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
    Column(
        modifier = Modifier
            .width(72.dp)
            .clickable(onClick = onClick),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(
            modifier = Modifier
                .size(48.dp)
                .clip(CircleShape)
                .background(if (selected) EditorAccent else EditorControlBackground),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = type.icon,
                contentDescription = type.label,
                tint = if (selected) EditorOnAccent else EditorIconTint,
            )
        }
        Box(modifier = Modifier.height(6.dp))
        Text(
            text = type.label,
            style = MaterialTheme.typography.labelMedium,
            fontSize = 10.sp,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
            color = if (selected) EditorAccent else EditorLabelTint,
            textAlign = TextAlign.Center,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        Box(modifier = Modifier.height(6.dp)) {
            if (active) {
                Box(
                    modifier = Modifier
                        .align(Alignment.Center)
                        .size(4.dp)
                        .clip(CircleShape)
                        .background(EditorAccent),
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
