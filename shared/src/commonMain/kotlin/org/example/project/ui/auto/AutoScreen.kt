package org.example.project.ui.auto

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
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
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
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
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.example.project.i18n.tr
import org.example.project.ui.common.ToolScaffold
import org.example.project.ui.common.UndoRedoButton
import org.example.project.ui.preview.ThemePreviews
import org.jetbrains.compose.resources.vectorResource
import photocollagemaker.shared.generated.resources.Res
import photocollagemaker.shared.generated.resources.ic_before_after
import photocollagemaker.shared.generated.resources.ic_redo
import photocollagemaker.shared.generated.resources.ic_undo

/** One undoable state of the Auto tool: whether the auto correction is currently applied. */
private data class AutoEdit(val applied: Boolean)

@Composable
internal fun AutoTool(
    sourceImage: ImageBitmap,
    onClose: () -> Unit,
    onApply: (ImageBitmap) -> Unit,
    modifier: Modifier = Modifier,
) {
    // Sampled off the main thread. A photo straight off disk is decoded lazily on iOS, so reading
    // its pixels during the first composition would stall the frame the tool opens on.
    val autoMatrix by produceState<FloatArray?>(null, sourceImage) {
        value = withContext(Dispatchers.Default) { autoColorMatrix(sourceImage) }
    }
    val scope = rememberCoroutineScope()
    var applying by remember { mutableStateOf(false) }

    AutoContent(
        sourceImage = sourceImage,
        autoMatrix = autoMatrix,
        onBack = onClose,
        onDone = { applied ->
            if (!applied) {
                onClose()
            } else if (!applying) {
                applying = true
                // The full-resolution bake is kept off the main thread too, so Done doesn't freeze
                // the editor right before the tool slides away.
                scope.launch {
                    val baked = withContext(Dispatchers.Default) {
                        bakeAuto(sourceImage, autoMatrix ?: autoColorMatrix(sourceImage))
                    }
                    onApply(baked)
                }
            }
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
                    colorFilter = if (edit.applied && !comparing) colorFilter else null,
                )
            } else {
                Text(
                    text = tr("No image to enhance"),
                    color = scheme.onSurface,
                    style = MaterialTheme.typography.bodyLarge,
                )
            }
        }

        ToolPanel(
            title = tr("Auto"),
            onClose = onBack,
            onDone = { onDone(edit.applied) },
            doneEnabled = sourceImage != null,
        ) {
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
            contentDescription = tr("Undo"),
            enabled = undoEnabled,
            onClick = onUndo,
            tint = content,
        )
        UndoRedoButton(
            icon = vectorResource(Res.drawable.ic_redo),
            contentDescription = tr("Redo"),
            enabled = redoEnabled,
            onClick = onRedo,
            tint = content,
        )
        Spacer(modifier = Modifier.weight(1f))
        BarIcon(
            icon = vectorResource(Res.drawable.ic_before_after),
            contentDescription = tr("Press and hold to compare with the original"),
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
