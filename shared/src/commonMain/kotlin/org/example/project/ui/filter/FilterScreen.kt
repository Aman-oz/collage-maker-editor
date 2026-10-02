package org.example.project.ui.filter

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Block
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.example.project.i18n.tr
import org.example.project.ui.common.CenterFillSlider
import org.example.project.ui.common.wholeNumberLabel
import org.example.project.ui.common.ToolScaffold
import org.example.project.ui.preview.ThemePreviews
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.vectorResource
import photocollagemaker.shared.generated.resources.Res
import photocollagemaker.shared.generated.resources.ic_before_after
import photocollagemaker.shared.generated.resources.ic_premium_icon

private val ChipShape = RoundedCornerShape(6.dp)

@Composable
internal fun FilterTool(
    sourceImage: ImageBitmap,
    isPremium: Boolean,
    onClose: () -> Unit,
    onApply: (ImageBitmap) -> Unit,
    onOpenPremium: () -> Unit,
    modifier: Modifier = Modifier,
) {
    FilterContent(
        sourceImage = sourceImage,
        onBack = onClose,
        onApply = { filter, intensity ->
            // A premium filter previews for everyone, but Done sends a non-subscriber to the paywall.
            // The tool stays open underneath, so Done applies it once they come back subscribed.
            if (filter.isPremium && !isPremium) {
                onOpenPremium()
            } else {
                onApply(bakeFilter(sourceImage, filter, intensity))
            }
        },
        modifier = modifier,
    )
}

@Composable
private fun FilterContent(
    sourceImage: ImageBitmap?,
    onBack: () -> Unit,
    onApply: (filter: PhotoFilter, intensity: Float) -> Unit,
    modifier: Modifier = Modifier,
) {
    val scheme = MaterialTheme.colorScheme
    // Saveable, unlike the other tools' edits: the paywall covers the editor, which drops plain
    // `remember` state, and the filter picked before subscribing has to still be there after.
    var selectedLabel by rememberSaveable { mutableStateOf(PhotoFilters.first().label) }
    val selectedFilter = PhotoFilters.firstOrNull { it.label == selectedLabel } ?: PhotoFilters.first()
    var intensity by rememberSaveable { mutableFloatStateOf(1f) }
    var comparing by remember { mutableStateOf(false) }

    ToolScaffold(modifier = modifier) {
        Box(
            modifier = Modifier
                .toolStage()
                .background(scheme.onSurface.copy(alpha = 0.08f))
                .padding(20.dp),
            contentAlignment = Alignment.Center,
        ) {
            if (sourceImage != null) {
                // Live, temporary preview — the actual bitmap is only baked when Done is tapped.
                // aspectRatio sizes the Image to the photo itself, so the rounded clip follows the
                // photo's edges instead of the letterboxed stage.
                Image(
                    bitmap = sourceImage,
                    contentDescription = tr("Photo preview"),
                    modifier = Modifier
                        .aspectRatio(sourceImage.width.toFloat() / sourceImage.height)
                        .clip(RoundedCornerShape(16.dp)),
                    contentScale = ContentScale.Fit,
                    colorFilter = if (comparing) null else selectedFilter.toColorFilter(intensity),
                )
            } else {
                Text(
                    text = tr("No image to filter"),
                    color = scheme.onSurface,
                    style = MaterialTheme.typography.bodyLarge,
                )
            }
        }

        ToolPanel(
            title = tr("Filters"),
            onClose = onBack,
            onDone = { onApply(selectedFilter, intensity) },
            doneEnabled = sourceImage != null,
        ) {
            CompareIconButton(
                onComparingChange = { comparing = it },
                modifier = Modifier
                    .align(Alignment.End)
                    .padding(top = 8.dp, end = 12.dp),
            )

            CenterFillSlider(
                value = intensity,
                onValueChange = { intensity = it },
                range = 0f..1f,
                trackColor = scheme.onSurface.copy(alpha = 0.12f),
                fillColor = scheme.primary,
                thumbColor = scheme.primary,
                thumbWidth = 32.dp,
                thumbHeight = 18.dp,
                horizontalPadding = 12.dp,
                glassThumb = true,
                glassTint = if (scheme.surface.luminance() > 0.5f) Color.White.copy(alpha = 0.35f) else Color.White.copy(alpha = 0.12f),
                valueLabel = { wholeNumberLabel(it * 100f) },
            )

            FilterStrip(
                sourceImage = sourceImage,
                selected = selectedFilter,
                onSelected = { selectedLabel = it.label },
            )
        }
    }
}

/** Press-and-hold icon that shows the unfiltered photo while held. */
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
            contentDescription = tr("Press and hold to compare with the original"),
            tint = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.size(20.dp),
        )
    }
}

@Composable
private fun FilterStrip(
    sourceImage: ImageBitmap?,
    selected: PhotoFilter,
    onSelected: (PhotoFilter) -> Unit,
) {
    LazyRow(
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 4.dp, bottom = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        items(PhotoFilters) { filter ->
            FilterChip(
                filter = filter,
                sourceImage = sourceImage,
                selected = filter == selected,
                onClick = { onSelected(filter) },
            )
        }
    }
}

/**
 * A thumbnail with its label underneath. The selected chip gets an accent outline around both,
 * so the thumbnail and label read as one tile.
 */
@Composable
private fun FilterChip(
    filter: PhotoFilter,
    sourceImage: ImageBitmap?,
    selected: Boolean,
    onClick: () -> Unit,
) {
    val scheme = MaterialTheme.colorScheme
    Column(
        modifier = Modifier
            .width(54.dp)
            .clip(ChipShape)
            .then(if (selected) Modifier.border(1.5.dp, scheme.primary, ChipShape) else Modifier)
            .clickable(onClick = onClick)
            .padding(3.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp)
                .clip(RoundedCornerShape(4.dp))
                .background(scheme.primary.copy(alpha = 0.12f)),
            contentAlignment = Alignment.Center,
        ) {
            if (sourceImage != null && !filter.isNoneOption) {
                Image(
                    bitmap = sourceImage,
                    contentDescription = tr(filter.label),
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop,
                    colorFilter = filter.toColorFilter(),
                )
            } else {
                Icon(
                    imageVector = Icons.Outlined.Block,
                    contentDescription = tr(filter.label),
                    tint = scheme.onSurface.copy(alpha = 0.6f),
                    modifier = Modifier.size(22.dp),
                )
            }
            if (filter.isPremium) {
                // Inside the thumbnail rather than hanging off its corner: the chip clips its content.
                Image(
                    painter = painterResource(Res.drawable.ic_premium_icon),
                    contentDescription = tr("Premium"),
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(2.dp)
                        .size(14.dp),
                )
            }
        }
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = tr(filter.label),
            color = scheme.onSurface,
            fontSize = 11.sp,
            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium,
            textAlign = TextAlign.Center,
            maxLines = 1,
        )
    }
}

@Preview
@Composable
private fun FilterScreenPreview() {
    ThemePreviews {
        FilterContent(sourceImage = ImageBitmap(360, 480), onBack = {}, onApply = { _, _ -> })
    }
}
