package org.example.project.ui.overlay

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
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
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Block
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
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
import kotlinx.coroutines.launch
import org.example.project.i18n.tr
import org.example.project.ui.filter.FilterPreviewLongSide
import org.example.project.ui.filter.FilterThumbnailLongSide
import org.example.project.ui.filter.FilterThumbnails
import org.example.project.ui.filter.ToolCategoryTabs
import org.example.project.ui.filter.rememberFilterThumbnails
import org.example.project.ui.filter.rememberRendered
import org.example.project.ui.filter.rememberScaledImage
import org.example.project.ui.filter.thumbnail
import org.example.project.ui.common.CenterFillSlider
import org.example.project.ui.common.PreventScreenCapture
import org.example.project.ui.common.ProFeatureBar
import org.example.project.ui.common.shimmer
import org.example.project.ui.common.wholeNumberLabel
import org.example.project.ui.common.ToolScaffold
import org.example.project.ui.preview.ThemePreviews
import org.example.project.ui.shop.LocalOpenShop
import org.example.project.ui.shop.ShopPicks
import org.example.project.ui.shop.ShopTab
import org.example.project.ui.stickers.LibrarySearchButton
import org.koin.compose.koinInject
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.vectorResource
import photocollagemaker.shared.generated.resources.Res
import photocollagemaker.shared.generated.resources.ic_before_after
import photocollagemaker.shared.generated.resources.ic_premium_icon

private val OverlayIntensityRange = 0f..100f
private val ChipShape = RoundedCornerShape(6.dp)

@Composable
internal fun OverlayTool(
    sourceImage: ImageBitmap,
    isPremium: Boolean,
    onClose: () -> Unit,
    onApply: (ImageBitmap) -> Unit,
    onOpenPremium: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val scope = rememberCoroutineScope()
    // Done renders the photo at its own size, which takes a moment; Done waits rather than twice.
    var baking by remember { mutableStateOf(false) }
    val openShop = LocalOpenShop.current
    // "Use" on a pack in the Shop, which the search button opened: the tool opens on that tab.
    val picks: ShopPicks = koinInject()
    val pick by picks.pick.collectAsState()
    val shopCategory = pick?.takeIf { it.tab == ShopTab.Overlays }?.let { p -> OverlayCategory.entries.firstOrNull { it.name == p.categoryId } }
    OverlayContent(
        sourceImage = sourceImage,
        isSubscribed = isPremium,
        onOpenPremium = onOpenPremium,
        onOpenShop = { openShop(ShopTab.Overlays) },
        shopCategory = shopCategory,
        onShopPickTaken = { picks.take(ShopTab.Overlays) },
        onBack = onClose,
        busy = baking,
        onApply = { preset, intensity ->
            when {
                // "None", or an overlay faded all the way out, leaves the photo as it was.
                preset == null || intensity <= 0f -> onClose()
                // A premium overlay previews for everyone, but Done sends a non-subscriber to the
                // paywall. The tool stays open underneath, so Done applies it once they come back
                // subscribed.
                preset.isPremium && !isPremium -> onOpenPremium()
                !baking -> {
                    baking = true
                    scope.launch {
                        val baked = bakeOverlay(sourceImage, preset, intensity / 100f)
                        baking = false
                        onApply(baked)
                    }
                }
            }
        },
        modifier = modifier,
    )
}

@Composable
private fun OverlayContent(
    sourceImage: ImageBitmap?,
    isSubscribed: Boolean,
    onOpenPremium: () -> Unit,
    onBack: () -> Unit,
    onApply: (OverlayPreset?, Float) -> Unit,
    modifier: Modifier = Modifier,
    busy: Boolean = false,
    onOpenShop: () -> Unit = {},
    shopCategory: OverlayCategory? = null,
    onShopPickTaken: () -> Unit = {},
) {
    val scheme = MaterialTheme.colorScheme
    // Saveable, unlike most tools' edits: the paywall covers the editor, which drops plain
    // `remember` state, and the overlay picked before subscribing has to still be there after.
    // Saved by name: an enum is not saveable on every platform, a String is.
    var selectedCategoryName by rememberSaveable { mutableStateOf(OverlayCategory.Effect.name) }
    val selectedCategory = OverlayCategory.entries.firstOrNull { it.name == selectedCategoryName } ?: OverlayCategory.Effect
    // null is the "None" chip: the photo is shown untouched.
    var selectedPresetKey by rememberSaveable { mutableStateOf<String?>(null) }
    val selectedPreset = selectedPresetKey?.let { key -> OverlayPresets.firstOrNull { it.key == key } }
    var intensity by rememberSaveable { mutableFloatStateOf(OverlayIntensityRange.endInclusive) }
    var comparing by remember { mutableStateOf(false) }
    LaunchedEffect(shopCategory) {
        val category = shopCategory ?: return@LaunchedEffect
        onShopPickTaken()
        selectedCategoryName = category.name
    }

    // The blends run on the CPU (see runFilterSteps), so the stage shows the photo cut to
    // FilterPreviewLongSide with the picked overlay's render over it at the slider's strength, and
    // each chip renders its own thumbnail once, all off the main thread.
    val previewSource = rememberScaledImage(sourceImage, FilterPreviewLongSide)
    val rendered = rememberRendered(previewSource, selectedPreset?.key) { image ->
        selectedPreset?.let { renderOverlay(image, it) } ?: image
    }
    val thumbnailSource = rememberScaledImage(sourceImage, FilterThumbnailLongSide)
    val thumbnails = rememberFilterThumbnails(thumbnailSource)

    ToolScaffold(modifier = modifier) {
        Column(
            modifier = Modifier
                .toolStage()
                .background(scheme.onSurface.copy(alpha = 0.08f)),
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .padding(20.dp),
                contentAlignment = Alignment.Center,
            ) {
                if (sourceImage != null) {
                    // aspectRatio sizes the stage to the photo itself, so the overlay canvas covers
                    // exactly the photo and the rounded clip follows its edges.
                    Box(
                        modifier = Modifier
                            .aspectRatio(sourceImage.width.toFloat() / sourceImage.height)
                            .clip(RoundedCornerShape(16.dp)),
                    ) {
                        Image(
                            bitmap = previewSource ?: sourceImage,
                            contentDescription = tr("Photo preview"),
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Fit,
                        )
                        if (rendered != null && selectedPreset != null && !comparing) {
                            Image(
                                bitmap = rendered.image,
                                contentDescription = null,
                                modifier = Modifier.fillMaxSize(),
                                contentScale = ContentScale.Fit,
                                alpha = intensity / OverlayIntensityRange.endInclusive,
                            )
                        }
                        val rendering = selectedPreset != null && rendered?.key != selectedPreset.key
                        if (rendering || busy) {
                            CircularProgressIndicator(
                                color = Color.White,
                                strokeWidth = 2.5.dp,
                                modifier = Modifier.align(Alignment.Center).size(28.dp),
                            )
                        }
                    }
                } else {
                    Text(
                        text = tr("No image to overlay"),
                        color = scheme.onSurface,
                        style = MaterialTheme.typography.bodyLarge,
                    )
                }
            }
            // Under the photo while a non-subscriber previews a premium overlay (one that Done would
            // apply, so not faded out); it is part of the stage so it fades in and out with it, and
            // leads to the paywall like Done does.
            val tryingPremium = selectedPreset?.isPremium == true && intensity > 0f && !isSubscribed
            ProFeatureBar(visible = tryingPremium)
            PreventScreenCapture(enabled = tryingPremium)
        }

        ToolPanel(
            title = tr("Overlay"),
            onClose = onBack,
            onDone = { onApply(selectedPreset, intensity) },
            doneEnabled = sourceImage != null && !busy,
        ) {
            // The tabs scroll; the compare button stays pinned at the row's end. Beside the photo it
            // moves to the column's bottom-right corner instead.
            Row(
                modifier = Modifier.fillMaxWidth().padding(top = 8.dp, end = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                ToolCategoryTabs(
                    categories = OverlayCategory.entries,
                    selected = selectedCategory,
                    label = { it.label },
                    onSelected = { selectedCategoryName = it.name },
                    modifier = Modifier.weight(1f),
                )
                if (!sideBySide) CompareIconButton(onComparingChange = { comparing = it })
            }

            CenterFillSlider(
                value = intensity,
                onValueChange = { intensity = it },
                range = OverlayIntensityRange,
                referenceValue = OverlayIntensityRange.start,
                trackColor = scheme.onSurface.copy(alpha = 0.12f),
                fillColor = scheme.primary,
                thumbColor = scheme.primary,
                thumbWidth = 32.dp,
                thumbHeight = 18.dp,
                horizontalPadding = 12.dp,
                glassThumb = true,
                glassTint = if (scheme.surface.luminance() > 0.5f) Color.White.copy(alpha = 0.35f) else Color.White.copy(alpha = 0.12f),
                valueLabel = ::wholeNumberLabel,
            )

            OverlayPresetStrip(
                category = selectedCategory,
                thumbnails = thumbnails,
                thumbnailSource = thumbnailSource,
                selected = selectedPreset,
                onSelected = { selectedPresetKey = it?.key },
                onSearch = onOpenShop,
            )
            if (sideBySide) {
                CompareIconButton(
                    onComparingChange = { comparing = it },
                    modifier = Modifier.align(Alignment.End).padding(end = 12.dp, bottom = 12.dp),
                )
            }
        }
    }
}

/** Press-and-hold icon that shows the photo without the overlay while held. */
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
            modifier = Modifier.size(22.dp),
        )
    }
}

@Composable
private fun OverlayPresetStrip(
    category: OverlayCategory,
    thumbnails: FilterThumbnails,
    thumbnailSource: ImageBitmap?,
    selected: OverlayPreset?,
    onSelected: (OverlayPreset?) -> Unit,
    onSearch: () -> Unit,
) {
    val presets = remember(category) { OverlayPresets.filter { it.category == category } }
    val listState = rememberLazyListState()
    // A tab picked (or opened from the Shop) starts at its first overlay.
    LaunchedEffect(category) { listState.scrollToItem(0) }
    LazyRow(
        state = listState,
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 4.dp, bottom = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        // Opens the Shop's Overlay tab, as the Stickers tool's search button opens its own.
        item(key = "search") { LibrarySearchButton(onClick = onSearch, size = 44.dp) }
        item(key = "none") {
            NoneChip(selected = selected == null, onClick = { onSelected(null) })
        }
        items(presets, key = { it.key }) { preset ->
            OverlayPresetChip(
                preset = preset,
                thumbnail = thumbnails.thumbnail(preset.key, thumbnailSource) { renderOverlay(it, preset, FilterThumbnailLongSide) },
                selected = preset == selected,
                onClick = { onSelected(preset) },
            )
        }
    }
}

/** Same outer size as [OverlayPresetChip], with an icon and label instead of a swatch. */
@Composable
private fun NoneChip(selected: Boolean, onClick: () -> Unit) {
    val scheme = MaterialTheme.colorScheme
    Column(
        modifier = Modifier
            .chipFrame(selected, scheme.primary, onClick)
            .padding(3.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp)
                .clip(RoundedCornerShape(4.dp))
                .background(scheme.primary.copy(alpha = 0.12f)),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = Icons.Outlined.Block,
                contentDescription = null,
                tint = scheme.onSurface.copy(alpha = 0.6f),
                modifier = Modifier.size(22.dp),
            )
        }
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = tr("None"),
            color = scheme.onSurface,
            fontSize = 11.sp,
            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium,
            textAlign = TextAlign.Center,
            maxLines = 1,
        )
    }
}

/** The photo with the overlay on it, unlabeled; it shimmers until it has rendered. */
@Composable
private fun OverlayPresetChip(preset: OverlayPreset, thumbnail: ImageBitmap?, selected: Boolean, onClick: () -> Unit) {
    val scheme = MaterialTheme.colorScheme
    Box(
        modifier = Modifier
            .chipFrame(selected, scheme.primary, onClick)
            .padding(if (selected) 3.dp else 0.dp),
    ) {
        val shape = if (selected) RoundedCornerShape(4.dp) else ChipShape
        if (thumbnail != null) {
            Image(
                bitmap = thumbnail,
                contentDescription = null,
                modifier = Modifier.fillMaxSize().clip(shape),
                contentScale = ContentScale.Crop,
            )
        } else {
            Box(Modifier.fillMaxSize().shimmer(shape))
        }
        if (preset.isPremium) {
            // Inside the swatch rather than hanging off its corner: the chip clips its content.
            Image(
                painter = painterResource(Res.drawable.ic_premium_icon),
                contentDescription = tr("Premium"),
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(3.dp)
                    .size(14.dp),
            )
        }
    }
}

private fun Modifier.chipFrame(selected: Boolean, accent: Color, onClick: () -> Unit): Modifier = this
    .width(54.dp)
    .height(74.dp)
    .clip(ChipShape)
    .then(if (selected) Modifier.border(1.5.dp, accent, ChipShape) else Modifier)
    .clickable(onClick = onClick)

@Preview
@Composable
private fun OverlayScreenPreview() {
    ThemePreviews {
        OverlayContent(sourceImage = ImageBitmap(360, 480), isSubscribed = false, onOpenPremium = {}, onBack = {}, onApply = { _, _ -> })
    }
}
