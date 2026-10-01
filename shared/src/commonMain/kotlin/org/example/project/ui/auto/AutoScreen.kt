package org.example.project.ui.auto

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import org.example.project.ui.common.ToolTopBar
import org.example.project.ui.common.UndoRedoButton
import org.example.project.ui.preview.ThemePreviews
import org.jetbrains.compose.resources.vectorResource
import org.koin.compose.viewmodel.koinViewModel
import photocollagemaker.shared.generated.resources.Res
import photocollagemaker.shared.generated.resources.ic_before_after
import photocollagemaker.shared.generated.resources.ic_redo
import photocollagemaker.shared.generated.resources.ic_undo

/** One undoable state of the Auto tool: whether the auto correction is currently applied. */
private data class AutoEdit(val applied: Boolean)

@Composable
fun AutoScreen(
    onBack: () -> Unit,
    onApplied: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: AutoViewModel = koinViewModel(),
) {
    val sourceImage = viewModel.sourceImage
    // Sampling reads pixels, so it runs once per image rather than on every recomposition.
    val autoMatrix = remember(sourceImage) { sourceImage?.let(::autoColorMatrix) }

    AutoContent(
        sourceImage = sourceImage,
        autoMatrix = autoMatrix,
        onBack = onBack,
        onDone = { applied ->
            if (sourceImage != null && autoMatrix != null && applied) {
                viewModel.apply(bakeAuto(sourceImage, autoMatrix))
            }
            onApplied()
        },
        modifier = modifier,
    )
}

@Composable
private fun AutoContent(
    sourceImage: ImageBitmap?,
    autoMatrix: FloatArray?,
    onBack: () -> Unit,
    onDone: (applied: Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    val scheme = MaterialTheme.colorScheme
    // The screen opens with the correction already applied, so the untouched photo starts out
    // on the undo stack.
    var edit by remember { mutableStateOf(AutoEdit(applied = true)) }
    var undoStack by remember { mutableStateOf(listOf(AutoEdit(applied = false))) }
    var redoStack by remember { mutableStateOf(emptyList<AutoEdit>()) }
    var comparing by remember { mutableStateOf(false) }

    val colorFilter = remember(autoMatrix) { autoMatrix?.let { ColorFilter.colorMatrix(ColorMatrix(it)) } }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(scheme.surface)
            .safeDrawingPadding(),
    ) {
        ToolTopBar(
            title = "Auto",
            onClose = onBack,
            onDone = { onDone(edit.applied) },
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
                    colorFilter = if (edit.applied && !comparing) colorFilter else null,
                )
            } else {
                Text(
                    text = "No image to enhance",
                    color = scheme.onSurface,
                    style = MaterialTheme.typography.bodyLarge,
                )
            }
        }

        AutoBottomBar(
            undoEnabled = undoStack.isNotEmpty(),
            redoEnabled = redoStack.isNotEmpty(),
            onUndo = {
                redoStack = redoStack + edit
                edit = undoStack.last()
                undoStack = undoStack.dropLast(1)
            },
            onRedo = {
                undoStack = undoStack + edit
                edit = redoStack.last()
                redoStack = redoStack.dropLast(1)
            },
            onComparingChange = { comparing = it },
        )
    }
}

@Composable
private fun AutoBottomBar(
    undoEnabled: Boolean,
    redoEnabled: Boolean,
    onUndo: () -> Unit,
    onRedo: () -> Unit,
    onComparingChange: (Boolean) -> Unit,
) {
    val content = MaterialTheme.colorScheme.onSurface
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        UndoRedoButton(
            icon = vectorResource(Res.drawable.ic_undo),
            contentDescription = "Undo",
            enabled = undoEnabled,
            onClick = onUndo,
            tint = content,
        )
        UndoRedoButton(
            icon = vectorResource(Res.drawable.ic_redo),
            contentDescription = "Redo",
            enabled = redoEnabled,
            onClick = onRedo,
            tint = content,
        )
        Spacer(modifier = Modifier.weight(1f))
        BarIcon(
            icon = vectorResource(Res.drawable.ic_before_after),
            contentDescription = "Press and hold to compare with the original",
            tint = content,
            modifier = Modifier.pointerInput(onComparingChange) {
                detectTapGestures(
                    onPress = {
                        onComparingChange(true)
                        tryAwaitRelease()
                        onComparingChange(false)
                    },
                )
            },
        )
    }
}

@Composable
private fun BarIcon(icon: ImageVector, contentDescription: String, tint: Color, modifier: Modifier = Modifier) {
    Box(
        modifier = Modifier
            .size(40.dp)
            .clip(CircleShape)
            .then(modifier),
        contentAlignment = Alignment.Center,
    ) {
        Icon(imageVector = icon, contentDescription = contentDescription, tint = tint, modifier = Modifier.size(20.dp))
    }
}

@Preview
@Composable
private fun AutoScreenPreview() {
    ThemePreviews {
        AutoContent(
            sourceImage = ImageBitmap(360, 420),
            autoMatrix = null,
            onBack = {},
            onDone = {},
        )
    }
}
