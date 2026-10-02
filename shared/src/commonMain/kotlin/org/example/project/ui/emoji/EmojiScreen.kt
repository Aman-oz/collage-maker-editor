package org.example.project.ui.emoji

import androidx.compose.foundation.Image
import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
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
import androidx.compose.runtime.saveable.listSaver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.onPlaced
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.unit.toSize
import kotlin.math.min
import kotlin.math.roundToInt
import org.example.project.i18n.tr
import org.example.project.ui.common.EmojiGridFontSizeSp
import org.example.project.ui.common.StickerFlightOverlay
import org.example.project.ui.common.ToolScaffold
import org.example.project.ui.common.rememberSpringBounce
import org.example.project.ui.common.rememberStickerFlights
import org.example.project.ui.common.springBounce
import org.example.project.ui.common.stickerFlightTarget
import org.example.project.ui.preview.ThemePreviews

@Composable
internal fun EmojiTool(
    sourceImage: ImageBitmap,
    isPremium: Boolean,
    onClose: () -> Unit,
    onApply: (ImageBitmap) -> Unit,
    onOpenPremium: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val textMeasurer = rememberTextMeasurer()
    EmojiContent(
        sourceImage = sourceImage,
        isPremium = isPremium,
        onBack = onClose,
        onOpenPremium = onOpenPremium,
        onDone = { placedEmojis, canvasWidthPx ->
            onApply(
                bakeEmojis(
                    source = sourceImage,
                    textMeasurer = textMeasurer,
                    placedEmojis = placedEmojis,
                    previewCanvasWidthPx = canvasWidthPx,
                ),
            )
        },
        modifier = modifier,
    )
}

/** The placed emojis, saved as each one's id, glyph, center (x, y) and scale end to end. */
private val PlacedEmojisSaver = listSaver<List<PlacedEmoji>, Any>(
    save = { placed -> placed.flatMap { listOf(it.id, it.emoji, it.offsetFraction.x, it.offsetFraction.y, it.scale) } },
    restore = { saved ->
        saved.chunked(5).map { (id, emoji, x, y, scale) ->
            PlacedEmoji(id as Long, emoji as String, Offset(x as Float, y as Float), scale as Float)
        }
    },
)

private val StringListSaver = listSaver<List<String>, String>(save = { it }, restore = { it })

@Composable
private fun EmojiContent(
    sourceImage: ImageBitmap?,
    isPremium: Boolean,
    onBack: () -> Unit,
    onOpenPremium: () -> Unit,
    onDone: (placedEmojis: List<PlacedEmoji>, canvasWidthPx: Float) -> Unit,
    modifier: Modifier = Modifier,
) {
    val scheme = MaterialTheme.colorScheme
    // null selects the Recent tab, which isn't an EmojiCategory because it has no fixed emoji set.
    // The edit is saveable, unlike most tools': the paywall covers the editor, which drops plain
    // `remember` state, and the emojis placed before it have to still be there after.
    var selectedCategory by rememberSaveable { mutableStateOf<EmojiCategory?>(EmojiCategory.Smileys) }
    var recentEmojis by rememberSaveable(stateSaver = StringListSaver) { mutableStateOf(emptyList()) }
    var placedEmojis by rememberSaveable(stateSaver = PlacedEmojisSaver) { mutableStateOf(emptyList()) }
    var selectedEmojiId by remember { mutableStateOf<Long?>(null) }
    var nextId by rememberSaveable { mutableLongStateOf(0L) }
    // The paywall is an offer, not a gate: it is shown once per edit, on the first Done with more
    // than [FreeEmojiLimit] emojis placed, and the next Done applies either way.
    var paywallShown by rememberSaveable { mutableStateOf(false) }
    var displayedImageWidthPx by remember { mutableStateOf(0f) }
    val stickerFlights = rememberStickerFlights(EmojiBaseSizeSp)

    // The box only exists so the flight overlay can draw over both the photo and the grid.
    Box(modifier = modifier.fillMaxSize()) {
        ToolScaffold {
            Box(
                modifier = Modifier
                    .toolStage()
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
                            contentDescription = tr("Photo preview"),
                            modifier = Modifier
                                .offset { IntOffset(imageOffsetPx.x.roundToInt(), imageOffsetPx.y.roundToInt()) }
                                .size(with(density) { imageWidthPx.toDp() }, with(density) { imageHeightPx.toDp() })
                                .stickerFlightTarget(stickerFlights)
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
                        text = tr("No image to add emoji to"),
                        color = scheme.onSurface,
                        style = MaterialTheme.typography.bodyLarge,
                    )
                }
            }

            ToolPanel(
                title = tr("Emoji"),
                onClose = onBack,
                onDone = {
                    if (!isPremium && !paywallShown && placedEmojis.size > FreeEmojiLimit) {
                        paywallShown = true
                        onOpenPremium()
                    } else {
                        onDone(placedEmojis, displayedImageWidthPx)
                    }
                },
                doneEnabled = sourceImage != null,
            ) {
                EmojiCategoryTabs(selected = selectedCategory, onSelected = { selectedCategory = it })

                EmojiGrid(
                    emojis = selectedCategory?.let { EmojisByCategory[it].orEmpty() } ?: recentEmojis,
                    onEmojiTapped = { emoji, cellBounds ->
                        stickerFlights.launch(emoji, cellBounds) {
                            val id = nextId
                            nextId += 1
                            placedEmojis = placedEmojis + PlacedEmoji(id = id, emoji = emoji)
                            selectedEmojiId = id
                        }
                        recentEmojis = (listOf(emoji) + (recentEmojis - emoji)).take(MaxRecentEmojis)
                    },
                )
            }
        }

        StickerFlightOverlay(stickerFlights)
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
                    contentDescription = tr("Delete sticker"),
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
                    contentDescription = tr("Resize sticker"),
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

internal const val MaxRecentEmojis = 40

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
internal fun EmojiCategoryTabs(selected: EmojiCategory?, onSelected: (EmojiCategory?) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 10.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        EmojiCategoryTab(
            icon = Icons.Outlined.Schedule,
            contentDescription = tr("Recent"),
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
    val bounce = rememberSpringBounce()
    Box(
        modifier = Modifier
            .springBounce(bounce)
            .size(32.dp)
            .clip(CircleShape)
            .background(if (selected) scheme.onSurface else Color.Transparent)
            .clickable(
                interactionSource = bounce.interactionSource,
                indication = LocalIndication.current,
                onClick = onClick,
            ),
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

/**
 * Also shown by the freestyle editor's Stickers panel, which passes its own height via [modifier].
 * [onEmojiTapped] also gets the tapped cell's bounds in root coordinates, where the panel's sticker
 * flight to the canvas starts.
 */
@Composable
internal fun EmojiGrid(
    emojis: List<String>,
    onEmojiTapped: (emoji: String, cellBoundsInRoot: Rect) -> Unit,
    modifier: Modifier = Modifier.fillMaxWidth().height(180.dp),
) {
    if (emojis.isEmpty()) {
        Box(modifier = modifier, contentAlignment = Alignment.Center) {
            Text(
                text = tr("Emojis you add will show up here"),
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
            // A plain holder, not state: the coordinates are only read when the cell is tapped.
            val cell = remember { arrayOfNulls<LayoutCoordinates>(1) }
            val bounce = rememberSpringBounce()
            Box(
                modifier = Modifier
                    .springBounce(bounce)
                    .aspectRatio(1f)
                    .onPlaced { cell[0] = it }
                    .clip(RoundedCornerShape(8.dp))
                    .clickable(
                        interactionSource = bounce.interactionSource,
                        indication = LocalIndication.current,
                        onClick = {
                            val coordinates = cell[0]
                            val bounds = if (coordinates?.isAttached == true) {
                                Rect(coordinates.positionInRoot(), coordinates.size.toSize())
                            } else {
                                Rect.Zero
                            }
                            onEmojiTapped(emoji, bounds)
                        },
                    ),
                contentAlignment = Alignment.Center,
            ) {
                Text(text = emoji, fontSize = EmojiGridFontSizeSp.sp)
            }
        }
    }
}

@Preview
@Composable
private fun EmojiScreenPreview() {
    ThemePreviews {
        EmojiContent(
            sourceImage = ImageBitmap(360, 480),
            isPremium = false,
            onBack = {},
            onOpenPremium = {},
            onDone = { _, _ -> },
        )
    }
}
