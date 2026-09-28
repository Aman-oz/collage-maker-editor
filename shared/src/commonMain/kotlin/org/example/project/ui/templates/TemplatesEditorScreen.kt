package org.example.project.ui.templates

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.Image
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.vinceglb.filekit.dialogs.FileKitType
import io.github.vinceglb.filekit.dialogs.compose.rememberFilePickerLauncher
import io.github.vinceglb.filekit.path
import kotlinx.coroutines.delay
import org.example.project.ui.common.GlassButtonStyle
import org.example.project.ui.common.GlassTopBarButton
import org.example.project.ui.common.NetworkImage
import org.example.project.ui.common.navSharedElement
import org.example.project.ui.common.templateFrameKey
import org.example.project.ui.common.topBar
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.parameter.parametersOf

@Composable
fun TemplatesEditorScreen(
    frame: TemplateFrame,
    onBack: () -> Unit,
    onDone: () -> Unit,
    modifier: Modifier = Modifier,
    // The Frames flow reuses this editor under its own name.
    title: String = "Templates",
    viewModel: TemplatesEditorViewModel = koinViewModel { parametersOf(frame) },
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    var pendingSlotIndex by remember { mutableStateOf<Int?>(null) }
    // The slot the user tapped last: outlined on the canvas, and the target of "Change Image".
    var selectedSlotIndex by remember { mutableStateOf<Int?>(null) }
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
                .safeDrawingPadding(),
        ) {
            EditorTopBar(
                title = title,
                onBack = onBack,
                onDone = { if (viewModel.applyTemplate()) onDone() },
            )

            Box(modifier = Modifier.fillMaxWidth().weight(1f), contentAlignment = Alignment.Center) {
                when (val state = uiState) {
                    TemplatesEditorUiState.Loading -> FramePlaceholder(frame)

                    is TemplatesEditorUiState.Ready -> TemplatePreview(
                        frame = frame,
                        state = state,
                        selectedSlotIndex = selectedSlotIndex,
                        onSlotTap = { index ->
                            if (index !in state.images) {
                                // An empty slot has nothing to select for; go straight to filling it.
                                selectedSlotIndex = index
                                pickInto(index)
                            } else {
                                // Tapping the selected photo again deselects it.
                                selectedSlotIndex = if (selectedSlotIndex == index) null else index
                            }
                        },
                        onSlotDoubleTap = { index ->
                            selectedSlotIndex = index
                            pickInto(index)
                        },
                        onSlotTransform = viewModel::transformSlot,
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
                text = "Tap a photo to select it, double tap to replace it, pinch to zoom",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
            )

            val selectedHasImage = selectedSlotIndex?.let { index ->
                (uiState as? TemplatesEditorUiState.Ready)?.images?.containsKey(index)
            } == true
            BottomActions(
                enabled = uiState is TemplatesEditorUiState.Ready,
                imageLabel = if (selectedHasImage) "Change Image" else "Add Image",
                onImageAction = {
                    val idx = selectedSlotIndex ?: viewModel.firstEmptySlotIndex()
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

/**
 * The template canvas in three layers: the slot photos at the back, the decorative frame over them
 * (its transparent windows reveal the photos, and it has no pointer input so touches fall through to
 * the slots), and the selected slot's outline on top so the frame can't hide it.
 */
@Composable
private fun TemplatePreview(
    frame: TemplateFrame,
    state: TemplatesEditorUiState.Ready,
    selectedSlotIndex: Int?,
    onSlotTap: (Int) -> Unit,
    onSlotDoubleTap: (Int) -> Unit,
    onSlotTransform: (index: Int, panX: Float, panY: Float, zoom: Float, slotWidth: Float, slotHeight: Float) -> Unit,
) {
    val frameImage = state.frameImage
    val aspect = frameImage.width.toFloat() / frameImage.height.toFloat()
    val slots = remember(frame, frameImage) {
        frame.normalizedSlots(frameImage.width.toFloat(), frameImage.height.toFloat())
    }

    BoxWithConstraints(
        modifier = Modifier
            // No fillMaxWidth: that pins the width, so a tall frame would overflow the space between
            // the top bar and the bottom actions. Alone, aspectRatio fits whichever dimension binds.
            .aspectRatio(aspect)
            .navSharedElement(templateFrameKey(frame.id)),
    ) {
        val totalW = maxWidth
        val totalH = maxHeight
        fun Modifier.slotBounds(slot: NormalizedSlot): Modifier = this
            .offset(x = totalW * slot.left, y = totalH * slot.top)
            .size(width = totalW * slot.width, height = totalH * slot.height)
            .rotate(slot.rotation)

        for (slot in slots) {
            SlotPhoto(
                image = state.images[slot.index],
                transform = state.transforms[slot.index] ?: SlotTransform(),
                onTap = { onSlotTap(slot.index) },
                onDoubleTap = { onSlotDoubleTap(slot.index) },
                onTransform = { panX, panY, zoom, w, h -> onSlotTransform(slot.index, panX, panY, zoom, w, h) },
                modifier = Modifier.slotBounds(slot),
            )
        }

        Image(
            bitmap = frameImage,
            contentDescription = null,
            contentScale = ContentScale.FillBounds,
            modifier = Modifier.fillMaxSize(),
        )

        slots.firstOrNull { it.index == selectedSlotIndex }?.let { slot ->
            Box(modifier = Modifier.slotBounds(slot).border(2.dp, MaterialTheme.colorScheme.primary))
        }
    }
}

/** One slot's photo (or its empty "+" placeholder), with tap, double-tap and pinch/pan handling. */
@Composable
private fun SlotPhoto(
    image: ImageBitmap?,
    transform: SlotTransform,
    onTap: () -> Unit,
    onDoubleTap: () -> Unit,
    onTransform: (panX: Float, panY: Float, zoom: Float, slotWidth: Float, slotHeight: Float) -> Unit,
    modifier: Modifier = Modifier,
) {
    // The pointerInput blocks outlive recompositions, so read the latest callbacks through state.
    val currentOnTap by rememberUpdatedState(onTap)
    val currentOnDoubleTap by rememberUpdatedState(onDoubleTap)
    val currentOnTransform by rememberUpdatedState(onTransform)
    Box(
        modifier = modifier
            .clip(RectangleShape)
            .pointerInput(Unit) {
                detectTapGestures(onTap = { currentOnTap() }, onDoubleTap = { currentOnDoubleTap() })
            }
            .pointerInput(Unit) {
                // Pan arrives in the slot's own (rotated) coordinates, matching SlotTransform's offsets.
                detectTransformGestures { _, pan, zoom, _ ->
                    currentOnTransform(pan.x, pan.y, zoom, size.width.toFloat(), size.height.toFloat())
                }
            },
        contentAlignment = Alignment.Center,
    ) {
        if (image != null) {
            Image(
                bitmap = image,
                contentDescription = "Template photo",
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer {
                        scaleX = transform.scale
                        scaleY = transform.scale
                        translationX = transform.offsetX * size.width
                        translationY = transform.offsetY * size.height
                    },
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

@Composable
private fun FramePlaceholder(frame: TemplateFrame) {
    // Shows the grid thumbnail (already in the image cache) while the full-size frame downloads, so
    // the shared-element transition from the Templates grid lands on the same picture.
    Box(
        modifier = Modifier
            .aspectRatio(frame.layout.aspectRatio)
            .navSharedElement(templateFrameKey(frame.id))
            .clip(RoundedCornerShape(4.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant),
        contentAlignment = Alignment.Center,
    ) {
        NetworkImage(
            url = frame.thumbnailUrl,
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize(),
        )
        CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
    }
}

@Composable
private fun EditorTopBar(title: String, onBack: () -> Unit, onDone: () -> Unit) {
    Row(
        modifier = Modifier.topBar(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        GlassTopBarButton(
            icon = Icons.AutoMirrored.Filled.ArrowBack,
            contentDescription = "Back",
            onClick = onBack,
            contentColor = MaterialTheme.colorScheme.onBackground,
        )
        Text(
            text = title,
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onBackground,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f).padding(horizontal = 16.dp),
        )
        // The primary glass ✓ every other editor uses for Done.
        GlassTopBarButton(
            icon = Icons.Filled.Check,
            contentDescription = "Done",
            onClick = onDone,
            style = GlassButtonStyle.Primary,
        )
    }
}

@Composable
private fun BottomActions(
    enabled: Boolean,
    imageLabel: String,
    onImageAction: () -> Unit,
    onChangeFrame: () -> Unit,
    modifier: Modifier = Modifier,
) {
    // Buttons' default 24dp side padding leaves too little room for icon + label on narrow phones.
    val contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp)
    Row(modifier = modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        Button(
            onClick = onImageAction,
            enabled = enabled,
            modifier = Modifier.weight(1f),
            shape = RoundedCornerShape(50),
            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
            contentPadding = contentPadding,
        ) {
            Icon(Icons.Filled.Image, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(Modifier.size(8.dp))
            Text(
                imageLabel,
                modifier = Modifier.padding(vertical = 6.dp),
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        Button(
            onClick = onChangeFrame,
            modifier = Modifier.weight(1f),
            shape = RoundedCornerShape(50),
            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
            contentPadding = contentPadding,
        ) {
            Icon(Icons.Filled.GridView, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(Modifier.size(8.dp))
            Text(
                "Change Frame",
                modifier = Modifier.padding(vertical = 6.dp),
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
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
