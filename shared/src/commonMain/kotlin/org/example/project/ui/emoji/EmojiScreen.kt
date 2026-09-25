package org.example.project.ui.emoji

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.OpenInFull
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.foundation.rememberScrollState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.min
import kotlin.math.roundToInt
import org.example.project.ui.common.AccentPillButton
import org.example.project.ui.common.EditorAccent
import org.example.project.ui.common.EditorBackground
import org.example.project.ui.common.EditorCanvasBackground
import org.example.project.ui.common.EditorCircleIconButton
import org.example.project.ui.common.EditorControlBackground
import org.example.project.ui.common.EditorLabelTint
import org.example.project.ui.common.EditorOnAccent
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
    var selectedCategory by remember { mutableStateOf(EmojiCategory.Smileys) }
    var placedEmojis by remember { mutableStateOf(emptyList<PlacedEmoji>()) }
    var selectedEmojiId by remember { mutableStateOf<Long?>(null) }
    var nextId by remember { mutableLongStateOf(0L) }
    var displayedImageWidthPx by remember { mutableStateOf(0f) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(EditorBackground)
            .safeDrawingPadding(),
    ) {
        EmojiTopBar(
            onBack = onBack,
            onDone = { onDone(placedEmojis, displayedImageWidthPx) },
            doneEnabled = sourceImage != null,
        )

        BoxWithConstraints(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .padding(16.dp)
                .clip(RoundedCornerShape(24.dp))
                .background(EditorCanvasBackground),
            contentAlignment = Alignment.Center,
        ) {
            if (sourceImage != null) {
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

                Image(
                    bitmap = sourceImage,
                    contentDescription = "Photo preview",
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Fit,
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
            } else {
                Text(
                    text = "No image to add emoji to",
                    color = Color.White,
                    style = MaterialTheme.typography.bodyLarge,
                )
            }
        }

        EmojiCategoryTabs(selected = selectedCategory, onSelected = { selectedCategory = it })

        EmojiGrid(
            category = selectedCategory,
            onEmojiTapped = { emoji ->
                val id = nextId
                nextId += 1
                placedEmojis = placedEmojis + PlacedEmoji(id = id, emoji = emoji)
                selectedEmojiId = id
            },
        )
    }
}

@Composable
private fun EmojiTopBar(onBack: () -> Unit, onDone: () -> Unit, doneEnabled: Boolean) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        EditorCircleIconButton(icon = Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", onClick = onBack)

        Text(
            text = "Emoji",
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

        AccentPillButton(text = "Done", onClick = onDone, enabled = doneEnabled)
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
    val density = LocalDensity.current
    val sizePx = with(density) { (EmojiBaseSizeSp * placed.scale).sp.toPx() }
    val sizeDp = with(density) { sizePx.toDp() }
    val centerPx = Offset(
        imageOffsetPx.x + placed.offsetFraction.x * imageWidthPx,
        imageOffsetPx.y + placed.offsetFraction.y * imageHeightPx,
    )

    Box(
        modifier = Modifier
            .align(Alignment.TopStart)
            .offset {
                IntOffset(
                    (centerPx.x - sizePx / 2f).roundToInt(),
                    (centerPx.y - sizePx / 2f).roundToInt(),
                )
            }
            .size(sizeDp)
            .then(
                if (selected) {
                    Modifier.border(width = 1.5.dp, color = EditorAccent, shape = RoundedCornerShape(4.dp))
                } else {
                    Modifier
                },
            )
            .pointerInput(placed.id) {
                detectTapGestures(onTap = { onSelectToggle() })
            }
            .pointerInput(placed.id, imageWidthPx, imageHeightPx) {
                detectDragGestures { change, dragAmount ->
                    change.consume()
                    onMove(Offset(dragAmount.x / imageWidthPx, dragAmount.y / imageHeightPx))
                }
            },
        contentAlignment = Alignment.Center,
    ) {
        Text(text = placed.emoji, fontSize = (EmojiBaseSizeSp * placed.scale).sp)

        if (selected) {
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .offset(x = 10.dp, y = (-10).dp)
                    .size(24.dp)
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
                    .offset(x = 10.dp, y = 10.dp)
                    .size(24.dp)
                    .clip(CircleShape)
                    .background(EditorAccent)
                    .pointerInput(placed.id) {
                        val baseSizePx = with(density) { EmojiBaseSizeSp.sp.toPx() }
                        detectDragGestures { change, dragAmount ->
                            change.consume()
                            val scaleDelta = (dragAmount.x + dragAmount.y) / (2f * baseSizePx)
                            onResize(placed.scale + scaleDelta)
                        }
                    },
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = Icons.Filled.OpenInFull,
                    contentDescription = "Resize sticker",
                    tint = EditorOnAccent,
                    modifier = Modifier.size(12.dp),
                )
            }
        }
    }
}

@Composable
private fun EmojiCategoryTabs(selected: EmojiCategory, onSelected: (EmojiCategory) -> Unit) {
    Column {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.spacedBy(20.dp),
        ) {
            EmojiCategory.entries.forEach { category ->
                EmojiCategoryTab(
                    category = category,
                    selected = category == selected,
                    onClick = { onSelected(category) },
                )
            }
        }
        HorizontalDivider(color = EditorControlBackground, thickness = 1.dp)
    }
}

@Composable
private fun EmojiCategoryTab(category: EmojiCategory, selected: Boolean, onClick: () -> Unit) {
    Column(
        modifier = Modifier.clickable(onClick = onClick),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = category.label,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
            color = if (selected) EditorAccent else EditorLabelTint,
            maxLines = 1,
        )
        Box(
            modifier = Modifier
                .padding(top = 4.dp)
                .height(2.dp)
                .size(width = if (selected) 20.dp else 0.dp, height = 2.dp)
                .background(EditorAccent),
        )
    }
}

@Composable
private fun EmojiGrid(category: EmojiCategory, onEmojiTapped: (String) -> Unit) {
    val emojis = EmojisByCategory[category].orEmpty()
    LazyVerticalGrid(
        columns = GridCells.Fixed(7),
        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.Center,
        modifier = Modifier
            .fillMaxWidth()
            .height(180.dp),
    ) {
        items(emojis) { emoji ->
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .clickable(onClick = { onEmojiTapped(emoji) }),
                contentAlignment = Alignment.Center,
            ) {
                Text(text = emoji, fontSize = 26.sp)
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
