package org.example.project.ui.frame

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.roundToInt
import org.example.project.i18n.tr
import org.example.project.ui.common.CenterFillSlider
import org.example.project.ui.common.ColorPickerFill
import org.example.project.ui.common.rememberColorPickerLauncher
import org.example.project.ui.common.wholeNumberLabel
import org.example.project.ui.common.SelectableSwatch
import org.example.project.ui.common.SwatchInnerCorner
import org.example.project.ui.common.ToolScaffold
import org.example.project.ui.preview.ThemePreviews
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.example.project.data.centraldata.CentralDataConfig
import org.example.project.data.network.NetworkImageLoader
import org.example.project.ui.common.PreventScreenCapture
import org.example.project.ui.common.ProFeatureBar
import org.example.project.ui.common.copyBitmap
import org.example.project.ui.common.drawImageScaled
import org.example.project.ui.layouteditor.ArtLibraryBody
import org.example.project.ui.layouteditor.FrameArtCellAspect
import org.example.project.ui.layouteditor.LayoutArt
import org.example.project.ui.layouteditor.LayoutArtLibrary
import org.example.project.ui.layouteditor.LayoutArtLoad
import org.example.project.ui.layouteditor.layoutChrome
import org.example.project.ui.layouteditor.rememberArtTransition
import org.example.project.ui.layouteditor.rememberLayoutArtImage
import org.example.project.ui.layouteditor.trimFrameArt
import org.example.project.ui.shop.ShopTab
import org.example.project.ui.text.LocalTextPanelSideBySide
import org.example.project.ui.text.PanelSegmentedTabs
import org.koin.compose.koinInject

/** The panel's two tabs: the plain border, and the server's decorative frames. */
private enum class FrameTab(private val englishLabel: String) {
    Color("Color"),
    Frame("Frame"),
    ;

    val label: String get() = tr(englishLabel)
}

/**
 * The editor's Frame tool, in two tabs like the layout editor's Frame panel. Color: a plain border
 * ([FrameColors], its width and corner radius). Frame: the server's decorative frames (the borders
 * library, [LayoutArtLibrary], read when the tab first opens), each trimmed of its transparent margin
 * and stretched over the whole photo, on top of the border. Tapping the frame in use takes it off.
 *
 * A free frame is kept on the disk when tapped. A premium one previews for anyone, with the Pro bar
 * under the photo, but Done sends a non-subscriber to the paywall; the picks are saveable, so they
 * are still there on the way back. Done downloads the frame's full picture before baking.
 */
@Composable
internal fun FrameTool(
    sourceImage: ImageBitmap,
    isPremium: Boolean,
    onClose: () -> Unit,
    onApply: (ImageBitmap) -> Unit,
    onOpenPremium: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val library: LayoutArtLibrary = koinInject()
    val loader: NetworkImageLoader = koinInject()
    val scope = rememberCoroutineScope()
    var frameLoad by remember { mutableStateOf<LayoutArtLoad>(LayoutArtLoad.Loading) }
    var loadAttempt by remember { mutableIntStateOf(0) }
    var libraryWanted by remember { mutableStateOf(false) }
    LaunchedEffect(libraryWanted, loadAttempt) {
        if (!libraryWanted || frameLoad is LayoutArtLoad.Ready) return@LaunchedEffect
        frameLoad = LayoutArtLoad.Loading
        frameLoad = try {
            LayoutArtLoad.Ready(library.getCategories(CentralDataConfig.BORDERS_SUPER_CATEGORY_ID))
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            LayoutArtLoad.Failed
        }
    }
    var baking by remember { mutableStateOf(false) }

    FrameContent(
        sourceImage = sourceImage,
        isSubscribed = isPremium,
        frameLoad = frameLoad,
        onFrameLibraryShown = { libraryWanted = true },
        onRetryFrames = { loadAttempt++ },
        onFrameArtPicked = { art ->
            // A premium frame a non-subscriber is only trying out isn't kept on the disk.
            if (!art.isPremium || isPremium) scope.launch { loader.loadAsset(art.imageUrl, keep = true) }
        },
        onBack = onClose,
        onOpenPremium = onOpenPremium,
        onApply = { color, width, cornerRadius, art, canvasWidthPx ->
            when {
                art?.isPremium == true && !isPremium -> onOpenPremium()
                baking -> Unit
                else -> {
                    baking = true
                    scope.launch {
                        val artImage = art?.let { frame ->
                            loader.loadAsset(frame.imageUrl, keep = !frame.isPremium || isPremium)
                                ?.let { withContext(Dispatchers.Default) { trimFrameArt(copyBitmap(it)) } }
                        }
                        val baked = withContext(Dispatchers.Default) {
                            bakeFrame(
                                source = sourceImage,
                                color = color,
                                width = width,
                                cornerRadius = cornerRadius,
                                previewCanvasWidthPx = canvasWidthPx,
                                frameArt = artImage,
                            )
                        }
                        baking = false
                        onApply(baked)
                    }
                }
            }
        },
        modifier = modifier,
    )
}

/** Under the photo, one height for both tabs, so the photo doesn't move when they switch. */
private val FramePanelBodyHeight = 250.dp

@Composable
private fun FrameContent(
    sourceImage: ImageBitmap?,
    isSubscribed: Boolean,
    frameLoad: LayoutArtLoad,
    onFrameLibraryShown: () -> Unit,
    onRetryFrames: () -> Unit,
    onFrameArtPicked: (LayoutArt) -> Unit,
    onBack: () -> Unit,
    onOpenPremium: () -> Unit,
    onApply: (color: Color, width: Float, cornerRadius: Float, art: LayoutArt?, canvasWidthPx: Float) -> Unit,
    modifier: Modifier = Modifier,
) {
    val scheme = MaterialTheme.colorScheme
    // Saveable, so a premium frame's trip to the paywall comes back to the same picks.
    var tab by rememberSaveable(stateSaver = FrameTabSaver) { mutableStateOf(FrameTab.Color) }
    var selectedColor by rememberSaveable(stateSaver = FrameColorOptionSaver) { mutableStateOf(FrameColors.first()) }
    var width by rememberSaveable { mutableFloatStateOf(FrameWidthDefault) }
    var cornerRadius by rememberSaveable { mutableFloatStateOf(FrameCornerRadiusDefault) }
    var frameArt by rememberSaveable(stateSaver = LayoutArtSaver) { mutableStateOf<LayoutArt?>(null) }
    var displayedImageWidthPx by remember { mutableFloatStateOf(0f) }
    LaunchedEffect(tab) { if (tab == FrameTab.Frame) onFrameLibraryShown() }
    val artImage = rememberArtTransition(rememberLayoutArtImage(frameArt, trim = true))
    val onPremiumFrame = frameArt?.isPremium == true && !isSubscribed

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
                    // aspectRatio sizes the box to the photo itself, so the frame canvas's bounds are
                    // the image bounds (drawn at Offset.Zero) and the rounded clip follows its edges.
                    Box(
                        modifier = Modifier
                            .aspectRatio(sourceImage.width.toFloat() / sourceImage.height)
                            .clip(RoundedCornerShape(16.dp))
                            .onSizeChanged { displayedImageWidthPx = it.width.toFloat() },
                    ) {
                        Image(
                            bitmap = sourceImage,
                            contentDescription = tr("Photo preview"),
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Fit,
                        )
                        Canvas(modifier = Modifier.fillMaxSize()) {
                            drawFrame(
                                imageOffset = Offset.Zero,
                                imageSize = size,
                                color = selectedColor.color,
                                widthPx = width,
                                cornerRadiusPx = cornerRadius,
                            )
                            // The decorative frame over the border, fading in over the one it replaced.
                            val progress = artImage.progress.value
                            val dst = IntSize(size.width.roundToInt(), size.height.roundToInt())
                            artImage.previous?.takeIf { progress < 1f }?.let { drawImageScaled(it, IntOffset.Zero, dst, alpha = 1f - progress) }
                            artImage.current?.let { drawImageScaled(it, IntOffset.Zero, dst, alpha = progress) }
                        }
                    }
                } else {
                    Text(
                        text = tr("No image to frame"),
                        color = scheme.onSurface,
                        style = MaterialTheme.typography.bodyLarge,
                    )
                }
            }
            // Under the photo while a non-subscriber tries a premium frame; Done leads to the paywall too.
            ProFeatureBar(visible = onPremiumFrame, onClick = onOpenPremium)
            PreventScreenCapture(enabled = onPremiumFrame)
        }

        ToolPanel(
            title = tr("Frame"),
            onClose = onBack,
            onDone = { onApply(selectedColor.color, width, cornerRadius, frameArt, displayedImageWidthPx) },
            doneEnabled = sourceImage != null,
        ) {
            PanelSegmentedTabs(
                labels = FrameTab.entries.map { it.label },
                selectedIndex = tab.ordinal,
                onSelected = { tab = FrameTab.entries[it] },
                modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 8.dp),
            )
            val bodyModifier = if (sideBySide) Modifier.fillMaxWidth().weight(1f) else Modifier.fillMaxWidth().height(FramePanelBodyHeight)
            when (tab) {
                FrameTab.Color -> Column(modifier = bodyModifier, verticalArrangement = Arrangement.Center) {
                    FrameSlider(
                        label = tr("Width"),
                        value = width,
                        onValueChange = { width = it },
                        range = FrameWidthRange,
                    )
                    FrameSlider(
                        label = tr("Corner Radius"),
                        value = cornerRadius,
                        onValueChange = { cornerRadius = it },
                        range = FrameCornerRadiusRange,
                    )
                    FrameColorRow(selected = selectedColor, onSelected = { selectedColor = it })
                }
                FrameTab.Frame -> CompositionLocalProvider(LocalTextPanelSideBySide provides sideBySide) {
                    ArtLibraryBody(
                        load = frameLoad,
                        selected = frameArt,
                        cellAspect = FrameArtCellAspect,
                        contentScale = ContentScale.Fit,
                        failedMessage = tr("Couldn't load frames"),
                        shopTab = ShopTab.Borders,
                        chrome = layoutChrome(),
                        onSelected = { art ->
                            frameArt = art.takeUnless { it.id == frameArt?.id }
                            frameArt?.let(onFrameArtPicked)
                        },
                        onRetry = onRetryFrames,
                        modifier = bodyModifier.padding(top = 8.dp, bottom = 12.dp),
                    )
                }
            }
        }
    }
}

private val FrameTabSaver: Saver<FrameTab, String> = Saver(save = { it.name }, restore = { FrameTab.valueOf(it) })

/** A colour option as its label and ARGB, so a custom colour comes back too. */
private val FrameColorOptionSaver: Saver<FrameColorOption, String> = Saver(
    save = { "${it.color.toArgb()}\t${it.label}" },
    restore = { saved ->
        val argb = saved.substringBefore('\t').toIntOrNull()
        val label = saved.substringAfter('\t')
        FrameColors.firstOrNull { it.label == label } ?: argb?.let { FrameColorOption(label, Color(it)) }
    },
)

/** A server frame by its fields, none of which holds a tab; "" for none. */
private val LayoutArtSaver: Saver<LayoutArt?, String> = Saver(
    save = { art -> art?.let { listOf(it.id, it.name, it.thumbnailUrl, it.imageUrl, it.aspectRatio, it.isPremium).joinToString("\t") } ?: "" },
    restore = { saved ->
        val f = saved.split('\t')
        if (f.size < 6) null else LayoutArt(f[0], f[1], f[2], f[3], f[4].toFloatOrNull() ?: 1f, f[5].toBoolean())
    },
)

/** A "label ··· value" row over an accent slider, filling from the range's start. */
@Composable
private fun FrameSlider(
    label: String,
    value: Float,
    onValueChange: (Float) -> Unit,
    range: ClosedFloatingPointRange<Float>,
    modifier: Modifier = Modifier,
) {
    val scheme = MaterialTheme.colorScheme
    Column(modifier = modifier) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 16.dp, end = 16.dp, top = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = label,
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                color = scheme.onSurface,
                modifier = Modifier.weight(1f),
            )
            Text(
                text = "${value.roundToInt()}",
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                color = scheme.primary,
            )
        }
        CenterFillSlider(
            value = value,
            onValueChange = onValueChange,
            range = range,
            referenceValue = range.start,
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
    }
}

/** Leads with the custom colour swatch, ringed while the frame is a colour outside [FrameColors]. */
@Composable
private fun FrameColorRow(selected: FrameColorOption, onSelected: (FrameColorOption) -> Unit) {
    val pickCustomColor = rememberColorPickerLauncher(
        // "None" is transparent, which is no colour to open a picker on.
        initial = if (selected.color == FrameColorNone) Color.White else selected.color,
        onPicked = { onSelected(FrameColorOption("Custom", it)) },
    )
    LazyRow(
        contentPadding = PaddingValues(start = 12.dp, end = 12.dp, top = 8.dp, bottom = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        item {
            SelectableSwatch(selected = selected !in FrameColors, size = 52.dp, onClick = pickCustomColor) {
                ColorPickerFill(Modifier.fillMaxSize())
            }
        }
        items(FrameColors) { option ->
            SelectableSwatch(selected = option == selected, size = 52.dp, onClick = { onSelected(option) }) {
                FrameColorSwatch(option)
            }
        }
    }
}

/**
 * A miniature framed photo: a soft neutral gradient "photo" inside a border of the option's
 * color. The "None" option gets a faint outline crossed by a diagonal slash instead.
 */
@Composable
private fun FrameColorSwatch(option: FrameColorOption) {
    val scheme = MaterialTheme.colorScheme
    val shape = RoundedCornerShape(SwatchInnerCorner)
    val isNone = option.color == FrameColorNone
    val outline = scheme.onSurface.copy(alpha = 0.15f)
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(listOf(scheme.onSurface.copy(alpha = 0.02f), scheme.onSurface.copy(alpha = 0.1f))))
            .border(width = if (isNone) 1.dp else 3.dp, color = if (isNone) outline else option.color, shape = shape)
            // A white border on a light surface would vanish, so every border gets a hairline edge.
            .border(width = 1.dp, color = outline, shape = shape),
    ) {
        if (isNone) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                drawLine(
                    color = scheme.onSurface.copy(alpha = 0.35f),
                    start = Offset(size.width * 0.2f, size.height * 0.8f),
                    end = Offset(size.width * 0.8f, size.height * 0.2f),
                    strokeWidth = 1.5.dp.toPx(),
                )
            }
        }
    }
}

@Preview
@Composable
private fun FrameScreenPreview() {
    ThemePreviews {
        FrameContent(
            sourceImage = ImageBitmap(360, 480),
            isSubscribed = false,
            frameLoad = LayoutArtLoad.Loading,
            onFrameLibraryShown = {},
            onRetryFrames = {},
            onFrameArtPicked = {},
            onBack = {},
            onOpenPremium = {},
            onApply = { _, _, _, _, _ -> },
        )
    }
}
