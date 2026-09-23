package org.example.project.ui.templates

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeContentPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.vinceglb.filekit.dialogs.FileKitType
import io.github.vinceglb.filekit.dialogs.compose.rememberFilePickerLauncher
import io.github.vinceglb.filekit.path
import kotlinx.coroutines.delay
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.parameter.parametersOf

@Composable
fun TemplatesEditorScreen(
    frame: TemplateFrame,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: TemplatesEditorViewModel = koinViewModel { parametersOf(frame) },
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    var pendingSlotIndex by remember { mutableStateOf<Int?>(null) }
    var toastMessage by remember { mutableStateOf<String?>(null) }
    LaunchedEffect(Unit) { viewModel.messages.collect { toastMessage = it } }

    val picker = rememberFilePickerLauncher(type = FileKitType.Image) { file ->
        val slot = pendingSlotIndex
        if (file != null && slot != null) viewModel.setSlotImage(slot, file.path)
        pendingSlotIndex = null
    }
    fun pickInto(slotIndex: Int) {
        pendingSlotIndex = slotIndex
        picker.launch()
    }

    Box(modifier = modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .safeContentPadding(),
        ) {
            EditorTopBar(
                onBack = onBack,
                onDone = { if (viewModel.applyTemplate()) onBack() },
            )

            Box(modifier = Modifier.fillMaxWidth().weight(1f), contentAlignment = Alignment.Center) {
                when (val state = uiState) {
                    TemplatesEditorUiState.Loading -> FramePlaceholder(frame.layout.aspectRatio)

                    is TemplatesEditorUiState.Ready -> TemplatePreview(
                        frame = frame,
                        state = state,
                        onSlotTap = { index -> if (index !in state.images) pickInto(index) },
                        onSlotLongPress = { index -> pickInto(index) },
                    )

                    is TemplatesEditorUiState.Error -> Text(
                        text = state.message,
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.error,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(24.dp),
                    )
                }
            }

            Text(
                text = "Long press on the image to edit it!",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
            )

            BottomActions(
                enabled = uiState is TemplatesEditorUiState.Ready,
                onAddImage = {
                    val idx = viewModel.firstEmptySlotIndex()
                    if (idx == null) toastMessage = "All slots are filled" else pickInto(idx)
                },
                onChangeFrame = onBack,
                modifier = Modifier.padding(16.dp),
            )
        }

        EditorToast(
            message = toastMessage,
            onDismissed = { toastMessage = null },
            modifier = Modifier.align(Alignment.BottomCenter),
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun TemplatePreview(
    frame: TemplateFrame,
    state: TemplatesEditorUiState.Ready,
    onSlotTap: (Int) -> Unit,
    onSlotLongPress: (Int) -> Unit,
) {
    val frameImage = state.frameImage
    val aspect = frameImage.width.toFloat() / frameImage.height.toFloat()
    val slots = remember(frame, frameImage) {
        frame.normalizedSlots(frameImage.width.toFloat(), frameImage.height.toFloat())
    }

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(aspect),
    ) {
        // Decorative frame behind the photo slots (as NewFrameEditor layers them).
        Image(
            bitmap = frameImage,
            contentDescription = null,
            contentScale = ContentScale.FillBounds,
            modifier = Modifier.fillMaxSize(),
        )

        val totalW = maxWidth
        val totalH = maxHeight
        for (slot in slots) {
            val image = state.images[slot.index]
            Box(
                modifier = Modifier
                    .offset(x = totalW * slot.left, y = totalH * slot.top)
                    .size(width = totalW * slot.width, height = totalH * slot.height)
                    .rotate(slot.rotation)
                    .clip(RectangleShape)
                    .combinedClickable(
                        onClick = { onSlotTap(slot.index) },
                        onLongClick = { onSlotLongPress(slot.index) },
                    ),
                contentAlignment = Alignment.Center,
            ) {
                if (image != null) {
                    Image(
                        bitmap = image,
                        contentDescription = "Template photo",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize(),
                    )
                } else {
                    Box(
                        modifier = Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.28f)),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(Icons.Filled.Add, contentDescription = "Add photo", tint = Color.White, modifier = Modifier.size(26.dp))
                    }
                }
            }
        }
    }
}

@Composable
private fun FramePlaceholder(aspectRatio: Float) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(aspectRatio)
            .clip(RoundedCornerShape(4.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant),
        contentAlignment = Alignment.Center,
    ) {
        CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
    }
}

@Composable
private fun EditorTopBar(onBack: () -> Unit, onDone: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        IconButton(onClick = onBack) {
            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = MaterialTheme.colorScheme.onBackground)
        }
        Text(
            text = "Templates",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onBackground,
            modifier = Modifier.weight(1f).padding(start = 8.dp),
        )
        Button(
            onClick = onDone,
            shape = RoundedCornerShape(50),
            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
        ) {
            Text("Done", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun BottomActions(enabled: Boolean, onAddImage: () -> Unit, onChangeFrame: () -> Unit, modifier: Modifier = Modifier) {
    Row(modifier = modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        Button(
            onClick = onAddImage,
            enabled = enabled,
            modifier = Modifier.weight(1f),
            shape = RoundedCornerShape(50),
            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
        ) {
            Icon(Icons.Filled.Image, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(Modifier.size(8.dp))
            Text("Add Image", modifier = Modifier.padding(vertical = 6.dp), fontWeight = FontWeight.Bold)
        }
        Button(
            onClick = onChangeFrame,
            modifier = Modifier.weight(1f),
            shape = RoundedCornerShape(50),
            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
        ) {
            Icon(Icons.Filled.GridView, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(Modifier.size(8.dp))
            Text("Change Frame", modifier = Modifier.padding(vertical = 6.dp), fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun EditorToast(message: String?, onDismissed: () -> Unit, modifier: Modifier = Modifier) {
    LaunchedEffect(message) {
        if (message != null) {
            delay(2200)
            onDismissed()
        }
    }
    AnimatedVisibility(visible = message != null, enter = fadeIn(), exit = fadeOut(), modifier = modifier.padding(bottom = 96.dp, start = 24.dp, end = 24.dp)) {
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(50))
                .background(Color.Black.copy(alpha = 0.85f))
                .padding(horizontal = 20.dp, vertical = 12.dp),
        ) {
            Text(message.orEmpty(), style = MaterialTheme.typography.bodyMedium, color = Color.White, textAlign = TextAlign.Center)
        }
    }
}
