package org.example.project.ui.emoji

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.OpenInFull
import androidx.compose.material.icons.outlined.EmojiEmotions
import androidx.compose.material.icons.outlined.EmojiFoodBeverage
import androidx.compose.material.icons.outlined.EmojiObjects
import androidx.compose.material.icons.outlined.EmojiSymbols
import androidx.compose.material.icons.outlined.EmojiTransportation
import androidx.compose.material.icons.outlined.Flag
import androidx.compose.material.icons.outlined.Pets
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material.icons.outlined.SportsSoccer
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.min
import kotlin.math.roundToInt
import org.example.project.ui.common.ToolTopBar
import org.example.project.ui.preview.ThemePreviews
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun EmojiScreen(
    onBack: () -> Unit,
    onApplied: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: EmojiViewModel = koinViewModel(),
) {
    val textMeasurer = rememberTextMeasurer()
    EmojiContent(
        sourceImage = viewModel.sourceImage,
        onBack = onBack,
        onDone = { placedEmojis, canvasWidthPx ->
            viewModel.sourceImage?.let { image ->
                viewModel.applyEmojis(
                    bakeEmojis(
                        source = image,
                        textMeasurer = textMeasurer,
                        placedEmojis = placedEmojis,
                        previewCanvasWidthPx = canvasWidthPx,
                    ),
                )
            }
            onApplied()
        },
        modifier = modifier,
    )
}

@Composable
private fun EmojiContent(
    sourceImage: ImageBitmap?,
    onBack: () -> Unit,
    onDone: (placedEmojis: List<PlacedEmoji>, canvasWidthPx: Float) -> Unit,
    modifier: Modifier = Modifier,
) {
    val scheme = MaterialTheme.colorScheme
    // null selects the Recent tab, which isn't an EmojiCategory because it has no fixed emoji set.
    var selectedCategory by remember { mutableStateOf<EmojiCategory?>(EmojiCategory.Smileys) }
    var recentEmojis by remember { mutableStateOf(emptyList<String>()) }
    var placedEmojis by remember { mutableStateOf(emptyList<PlacedEmoji>()) }
    var selectedEmojiId by remember { mutableStateOf<Long?>(null) }
    var nextId by remember { mutableLongStateOf(0L) }
    var displayedImageWidthPx by remember { mutableStateOf(0f) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(scheme.surface)
            .safeDrawingPadding(),
    ) {
        ToolTopBar(
            title = "Emoji",
            onClose = onBack,
            onDone = { onDone(placedEmojis, displayedImageWidthPx) },
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
                BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
                    val density = LocalDensity.current
                    val boxWidthPx = with(density) { maxWidth.toPx() }
                    val boxHeightPx = with(density) { maxHeight.toPx() }
                    val bitmapWidth = sourceImage.width.toFloat()
                    val bitmapHeight = sourceImage.height.toFloat()
                    val fitScale = min(boxWidthPx / bitmapWidth, boxHeightPx / bitmapHeight)
                    val imageWidthPx = bitmapWidth * fitScale
                    val imageHeightPx = bitmapHeight * fitScale
                    val imageOffsetPx = Offset((boxWidthPx - imageWidthPx) / 2f, (boxHeightPx - imageHeightPx) / 2f)
                    displayedImageWidthPx = imageWidthPx

                    // Sized to the fitted photo (not the whole box) so the rounded clip hugs the photo.
                    Image(
                        bitmap = sourceImage,
                        contentDescription = "Photo preview",
                        modifier = Modifier
                            .offset { IntOffset(imageOffsetPx.x.roundToInt(), imageOffsetPx.y.roundToInt()) }
                            .size(with(density) { imageWidthPx.toDp() }, with(density) { imageHeightPx.toDp() })
                            .clip(RoundedCornerShape(16.dp)),
                        contentScale = ContentScale.FillBounds,
                    )

                    // Tapping empty canvas space deselects whatever sticker is selected.
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .pointerInput(Unit) {
                                detectTapGestures(onTap = { selectedEmojiId = null })
                            },
                    )

                    for (placed in placedEmojis) {
                        key(placed.id) {
                            PlacedEmojiView(
                                placed = placed,
                                selected = placed.id == selectedEmojiId,
                                imageOffsetPx = imageOffsetPx,
                                imageWidthPx = imageWidthPx,
                                imageHeightPx = imageHeightPx,
                                onSelectToggle = {
                                    selectedEmojiId = if (selectedEmojiId == placed.id) null else placed.id
                                },
                                onMove = { fractionDelta ->
                                    placedEmojis = placedEmojis.map {
                                        if (it.id == placed.id) {
                                            it.copy(
                                                offsetFraction = Offset(
                                                    (it.offsetFraction.x + fractionDelta.x).coerceIn(0f, 1f),
                                                    (it.offsetFraction.y + fractionDelta.y).coerceIn(0f, 1f),
                                                ),
                                            )
                                        } else {
                                            it
                                        }
                                    }
                                },
                                onResize = { newScale ->
                                    placedEmojis = placedEmojis.map {
                                        if (it.id == placed.id) it.copy(scale = newScale.coerceIn(EmojiScaleRange)) else it
                                    }
                                },
                                onDelete = {
                                    placedEmojis = placedEmojis.filterNot { it.id == placed.id }
                                    selectedEmojiId = null
                                },
                            )
                        }
                    }
                }
            } else {
                Text(
                    text = "No image to add emoji to",
                    color = scheme.onSurface,
                    style = MaterialTheme.typography.bodyLarge,
                )
            }
        }

        EmojiCategoryTabs(selected = selectedCategory, onSelected = { selectedCategory = it })

        EmojiGrid(
            emojis = selectedCategory?.let { EmojisByCategory[it].orEmpty() } ?: recentEmojis,
            onEmojiTapped = { emoji ->
                val id = nextId
                nextId += 1
                placedEmojis = placedEmojis + PlacedEmoji(id = id, emoji = emoji)
                selectedEmojiId = id
                recentEmojis = (listOf(emoji) + (recentEmojis - emoji)).take(MaxRecentEmojis)
            },
        )
    }
}

@Composable
private fun BoxScope.PlacedEmojiView(
    placed: PlacedEmoji,
    selected: Boolean,
    imageOffsetPx: Offset,
    imageWidthPx: Float,
    imageHeightPx: Float,
    onSelectToggle: () -> Unit,
    onMove: (fractionDelta: Offset) -> Unit,
    onResize: (newScale: Float) -> Unit,
    onDelete: () -> Unit,
) {
    val scheme = MaterialTheme.colorScheme
    val density = LocalDensity.current
    val fontSize = (EmojiBaseSizeSp * placed.scale).sp
    // An emoji glyph is wider and taller than its font size, so a box exactly fontSize wide would
    // clip it; the extra room also keeps the selection border off the glyph.
    val boxPx = with(density) { fontSize.toPx() } * EmojiBoxToFontRatio
    val boxDp = with(density) { boxPx.toDp() }
    val centerPx = Offset(
        imageOffsetPx.x + placed.offsetFraction.x * imageWidthPx,
        imageOffsetPx.y + placed.offsetFraction.y * imageHeightPx,
    )
    // The gesture blocks below outlive recompositions, so they read these instead of the
    // values captured when the gesture started.
    val currentScale by rememberUpdatedState(placed.scale)
    val currentBoxPx by rememberUpdatedState(boxPx)
    val currentOnMove by rememberUpdatedState(onMove)
    val currentOnResize by rememberUpdatedState(onResize)
    val currentOnSelectToggle by rememberUpdatedState(onSelectToggle)

    Box(
        modifier = Modifier
            .align(Alignment.TopStart)
            .offset {
                IntOffset(
                    (centerPx.x - boxPx / 2f).roundToInt(),
                    (centerPx.y - boxPx / 2f).roundToInt(),
                )
            }
            .size(boxDp)
            .then(
                if (selected) {
                    Modifier.border(width = 1.5.dp, color = scheme.primary, shape = RoundedCornerShape(8.dp))
                } else {
                    Modifier
                },
            )
            .pointerInput(placed.id) {
                detectTapGestures(onTap = { currentOnSelectToggle() })
            }
            .pointerInput(placed.id, imageWidthPx, imageHeightPx) {
                detectDragGestures { change, dragAmount ->
                    change.consume()
                    currentOnMove(Offset(dragAmount.x / imageWidthPx, dragAmount.y / imageHeightPx))
                }
            },
        contentAlignment = Alignment.Center,
    ) {
        // Unbounded so the glyph is never clipped and stays centered on its own measured size,
        // the same way bakeEmojis centers it.
        Text(
            text = placed.emoji,
            fontSize = fontSize,
            maxLines = 1,
            softWrap = false,
            modifier = Modifier.wrapContentSize(unbounded = true),
        )

        if (selected) {
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .offset(x = HandleHang, y = -HandleHang)
                    .size(HandleSize)
                    .clip(CircleShape)
                    .background(Color(0xFFE53935))
                    .clickable(onClick = onDelete),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = Icons.Filled.Close,
                    contentDescription = "Delete sticker",
                    tint = Color.White,
                    modifier = Modifier.size(14.dp),
                )
            }

            Box(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .offset(x = HandleHang, y = HandleHang)
                    .size(HandleSize)
                    .clip(CircleShape)
                    .background(scheme.primary)
                    .pointerInput(placed.id) {
                        // Scale follows the finger's distance from the emoji's center, relative to
                        // where the drag started. The handle itself moves as the box grows, so the
                        // center is recomputed in the handle's local space on every event: the
                        // handle's top-left sits at (box - handle + hang) inside the box.
                        val handlePx = HandleSize.toPx()
                        val hangPx = HandleHang.toPx()
                        fun distanceFromCenter(localPosition: Offset): Float {
                            val centerLocal = handlePx - hangPx - currentBoxPx / 2f
                            return (localPosition - Offset(centerLocal, centerLocal)).getDistance()
                        }
                        var startDistance = 0f
                        var startScale = 1f
                        detectDragGestures(
                            onDragStart = { position ->
                                startDistance = distanceFromCenter(position)
                                startScale = currentScale
                            },
                        ) { change, _ ->
                            change.consume()
                            if (startDistance > 0f) {
                                currentOnResize(startScale * distanceFromCenter(change.position) / startDistance)
                            }
                        }
                    },
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = Icons.Filled.OpenInFull,
                    contentDescription = "Resize sticker",
                    tint = scheme.onPrimary,
                    modifier = Modifier.size(12.dp),
                )
            }
        }
    }
}

private const val EmojiBoxToFontRatio = 1.4f
private val HandleSize = 24.dp
private val HandleHang = 10.dp

private const val MaxRecentEmojis = 40

private val EmojiCategory.icon: ImageVector
    get() = when (this) {
        EmojiCategory.Smileys -> Icons.Outlined.EmojiEmotions
        EmojiCategory.Animals -> Icons.Outlined.Pets
        EmojiCategory.Food -> Icons.Outlined.EmojiFoodBeverage
        EmojiCategory.Activities -> Icons.Outlined.SportsSoccer
        EmojiCategory.Travel -> Icons.Outlined.EmojiTransportation
        EmojiCategory.Objects -> Icons.Outlined.EmojiObjects
        EmojiCategory.Symbols -> Icons.Outlined.EmojiSymbols
        EmojiCategory.Flags -> Icons.Outlined.Flag
    }

/** One icon per category, Recent first; they all fit on a phone width so the row doesn't scroll. */
@Composable
private fun EmojiCategoryTabs(selected: EmojiCategory?, onSelected: (EmojiCategory?) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 10.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        EmojiCategoryTab(
            icon = Icons.Outlined.Schedule,
            contentDescription = "Recent",
            selected = selected == null,
            onClick = { onSelected(null) },
        )
        EmojiCategory.entries.forEach { category ->
            EmojiCategoryTab(
                icon = category.icon,
                contentDescription = category.label,
                selected = category == selected,
                onClick = { onSelected(category) },
            )
        }
    }
}

@Composable
private fun EmojiCategoryTab(icon: ImageVector, contentDescription: String, selected: Boolean, onClick: () -> Unit) {
    val scheme = MaterialTheme.colorScheme
    Box(
        modifier = Modifier
            .size(32.dp)
            .clip(CircleShape)
            .background(if (selected) scheme.onSurface else Color.Transparent)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            tint = if (selected) scheme.surface else scheme.onSurface.copy(alpha = 0.7f),
            modifier = Modifier.size(20.dp),
        )
    }
}

@Composable
private fun EmojiGrid(emojis: List<String>, onEmojiTapped: (String) -> Unit) {
    val modifier = Modifier
        .fillMaxWidth()
        .height(180.dp)
    if (emojis.isEmpty()) {
        Box(modifier = modifier, contentAlignment = Alignment.Center) {
            Text(
                text = "Emojis you add will show up here",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
            )
        }
        return
    }
    LazyVerticalGrid(
        columns = GridCells.Fixed(10),
        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
        modifier = modifier,
    ) {
        items(emojis) { emoji ->
            Box(
                modifier = Modifier
                    .aspectRatio(1f)
                    .clip(RoundedCornerShape(8.dp))
                    .clickable(onClick = { onEmojiTapped(emoji) }),
                contentAlignment = Alignment.Center,
            ) {
                Text(text = emoji, fontSize = 24.sp)
            }
        }
    }
}

@Preview
@Composable
private fun EmojiScreenPreview() {
    ThemePreviews {
        EmojiContent(sourceImage = ImageBitmap(360, 480), onBack = {}, onDone = { _, _ -> })
    }
}
