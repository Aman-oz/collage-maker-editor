package org.example.project.ui.adjust

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
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
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.listSaver
import androidx.compose.runtime.saveable.rememberSaveable
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
import org.example.project.i18n.tr
import org.example.project.ui.common.CenterFillSlider
import org.example.project.ui.common.ToolScaffold
import org.example.project.ui.common.UndoRedoButton
import org.example.project.ui.preview.ThemePreviews
import org.jetbrains.compose.resources.vectorResource
import photocollagemaker.shared.generated.resources.Res
import photocollagemaker.shared.generated.resources.ic_before_after
import photocollagemaker.shared.generated.resources.ic_redo
import photocollagemaker.shared.generated.resources.ic_undo

private val AdjustRange = -100f..100f

@Composable
internal fun AdjustTool(
    sourceImage: ImageBitmap,
    isPremium: Boolean,
    onClose: () -> Unit,
    onApply: (ImageBitmap) -> Unit,
    onOpenPremium: () -> Unit,
    modifier: Modifier = Modifier,
) {
    AdjustContent(
        sourceImage = sourceImage,
        isPremium = isPremium,
        onBack = onClose,
        onApply = { matrix -> onApply(bakeAdjustments(sourceImage, matrix)) },
        onOpenPremium = onOpenPremium,
        modifier = modifier,
    )
}

private val AdjustValuesSaver = listSaver<AdjustValues, Float>(
    save = { it.toFloatList() },
    restore = { adjustValuesOf(it) },
)

/** An undo/redo stack, saved as its values' floats end to end. */
private val AdjustStackSaver = listSaver<List<AdjustValues>, Float>(
    save = { stack -> stack.flatMap { it.toFloatList() } },
    restore = { floats -> floats.chunked(AdjustmentType.entries.size).map(::adjustValuesOf) },
)

@Composable
private fun AdjustContent(
    sourceImage: ImageBitmap?,
    isPremium: Boolean,
    onBack: () -> Unit,
    onApply: (FloatArray) -> Unit,
    onOpenPremium: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val scheme = MaterialTheme.colorScheme
    // The edit is saveable, unlike most tools': the paywall covers the editor, which drops plain
    // `remember` state, and the adjustments made before it have to still be there after.
    var values by rememberSaveable(stateSaver = AdjustValuesSaver) { mutableStateOf(AdjustValues()) }
    var undoStack by rememberSaveable(stateSaver = AdjustStackSaver) { mutableStateOf(emptyList()) }
    var redoStack by rememberSaveable(stateSaver = AdjustStackSaver) { mutableStateOf(emptyList()) }
    // The paywall is an offer, not a gate: it is shown once per edit, on the first Done with
    // [PremiumAdjustmentCount] or more adjustments, and the next Done applies either way.
    var paywallShown by rememberSaveable { mutableStateOf(false) }
    // The values as they were when the drag in progress began. One whole drag is one undo step,
    // however many values it passes through on the way.
    var dragStart by remember { mutableStateOf<AdjustValues?>(null) }
    var selected by rememberSaveable { mutableStateOf(AdjustmentType.Brightness) }
    var comparing by remember { mutableStateOf(false) }

    fun commit(previous: AdjustValues) {
        if (previous == values) return
        undoStack = undoStack + previous
        redoStack = emptyList()
    }

    /** Swaps [values] for [restored] and shows the adjustment that just changed, so the step is visible. */
    fun restore(restored: AdjustValues) {
        changedAdjustment(values, restored)?.let { selected = it }
        values = restored
    }

    fun undo() {
        val previous = undoStack.lastOrNull() ?: return
        redoStack = redoStack + values
        undoStack = undoStack.dropLast(1)
        restore(previous)
    }

    fun redo() {
        val next = redoStack.lastOrNull() ?: return
        undoStack = undoStack + values
        redoStack = redoStack.dropLast(1)
        restore(next)
    }

    val combinedMatrix = remember(values) { values.toColorMatrix() }
    val colorFilter = remember(combinedMatrix) { ColorFilter.colorMatrix(ColorMatrix(combinedMatrix)) }

    ToolScaffold(modifier = modifier) {
        Box(
            modifier = Modifier
                .toolStage()
                .background(scheme.onSurface.copy(alpha = 0.08f))
                .padding(20.dp),
            contentAlignment = Alignment.Center,
        ) {
            if (sourceImage != null) {
                // aspectRatio sizes the Image to the photo itself, so the rounded clip follows the
                // photo's edges instead of the letterboxed stage.
                Image(
                    bitmap = sourceImage,
                    contentDescription = tr("Photo preview"),
                    modifier = Modifier
                        .aspectRatio(sourceImage.width.toFloat() / sourceImage.height)
                        .clip(RoundedCornerShape(16.dp)),
                    contentScale = ContentScale.Fit,
                    colorFilter = if (comparing) null else colorFilter,
                )
            } else {
                Text(
                    text = tr("No image to adjust"),
                    color = scheme.onSurface,
                    style = MaterialTheme.typography.bodyLarge,
                )
            }
        }

        ToolPanel(
            title = tr("Adjust"),
            onClose = onBack,
            onDone = {
                if (!isPremium && !paywallShown && values.changedCount() >= PremiumAdjustmentCount) {
                    paywallShown = true
                    onOpenPremium()
                } else {
                    onApply(combinedMatrix)
                }
            },
            doneEnabled = sourceImage != null,
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 12.dp, top = 8.dp, end = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                UndoRedoButton(
                    icon = vectorResource(Res.drawable.ic_undo),
                    contentDescription = tr("Undo"),
                    enabled = undoStack.isNotEmpty(),
                    onClick = ::undo,
                )
                UndoRedoButton(
                    icon = vectorResource(Res.drawable.ic_redo),
                    contentDescription = tr("Redo"),
                    enabled = redoStack.isNotEmpty(),
                    onClick = ::redo,
                )
                Spacer(modifier = Modifier.weight(1f))
                CompareIconButton(onComparingChange = { comparing = it })
            }

            CenterFillSlider(
                value = values[selected],
                onValueChange = { value ->
                    val previous = values
                    values = values.with(selected, value)
                    // A tap on the track changes the value with no drag around it: its own step.
                    if (dragStart == null) commit(previous)
                },
                onDraggingChange = { dragging ->
                    if (dragging) {
                        dragStart = values
                    } else {
                        dragStart?.let(::commit)
                        dragStart = null
                    }
                },
                valueLabel = ::signedAdjustLabel,
                range = AdjustRange,
                trackColor = scheme.onSurface.copy(alpha = 0.12f),
                fillColor = scheme.primary,
                thumbColor = scheme.primary,
                thumbWidth = 32.dp,
                thumbHeight = 18.dp,
                horizontalPadding = 12.dp,
                glassThumb = true,
                glassTint = if (scheme.surface.luminance() > 0.5f) Color.White.copy(alpha = 0.35f) else Color.White.copy(alpha = 0.12f),
                modifier = Modifier.padding(top = 8.dp),
            )

            AdjustmentTypeRow(
                selected = selected,
                values = values,
                onSelected = { selected = it },
            )
        }
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
            contentDescription = tr("Press and hold to compare with the original"),
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
        AdjustContent(sourceImage = ImageBitmap(360, 480), isPremium = false, onBack = {}, onApply = {}, onOpenPremium = {})
    }
}
