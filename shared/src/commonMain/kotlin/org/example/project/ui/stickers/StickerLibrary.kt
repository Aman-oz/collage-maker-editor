package org.example.project.ui.stickers

import androidx.compose.foundation.Image
import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.FlipToBack
import androidx.compose.material.icons.outlined.FlipToFront
import androidx.compose.material.icons.outlined.SwapHoriz
import androidx.compose.material.icons.outlined.SwapVert
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onPlaced
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigationevent.NavigationEventInfo
import androidx.navigationevent.compose.NavigationBackHandler
import androidx.navigationevent.compose.rememberNavigationEventState
import io.github.fletchmckee.liquid.LiquidState
import io.github.fletchmckee.liquid.liquefiable
import io.github.fletchmckee.liquid.rememberLiquidState
import kotlin.math.roundToInt
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch
import androidx.compose.runtime.collectAsState
import org.example.project.data.cache.AssetCache
import org.example.project.data.network.NetworkImageLoader
import org.example.project.ui.common.DownloadRing
import org.example.project.ui.common.shimmer
import org.example.project.i18n.tr
import org.example.project.ui.common.NetworkImage
import org.example.project.ui.common.PreventScreenCapture
import org.example.project.ui.common.ProFeatureBar
import org.example.project.ui.common.ToolScaffold
import org.example.project.ui.common.rememberSpringBounce
import org.example.project.ui.common.springBounce
import org.example.project.ui.preview.ThemePreviews
import org.example.project.ui.text.GenieDirection
import org.example.project.ui.text.GlassGenieMenu
import org.example.project.ui.text.MenuRow
import org.example.project.ui.text.TextSelectionBox
import org.example.project.ui.text.TextSelectionToolbar
import org.example.project.ui.text.TextToolbarHeight
import org.example.project.ui.text.cascadeIn
import org.example.project.ui.text.rotatedHalfSize
import org.example.project.ui.text.snapTextRotation
import org.jetbrains.compose.resources.painterResource
import org.koin.compose.koinInject
import photocollagemaker.shared.generated.resources.Res
import photocollagemaker.shared.generated.resources.ic_premium_icon

// The Stickers tool's library: the sticker grid, the sub-category tiles and category pills under
// it, and the selection toolbar's ⋯ menu.

private val StickerCellShape = RoundedCornerShape(16.dp)
private val TileShape = RoundedCornerShape(12.dp)

private val TopShadowHeight = 10.dp

/**
 * A shadow cast up over whatever is above, plus a hairline on the edge: the look of an elevated tray
 * along one side only, which `Modifier.shadow` can't do (it shades all four).
 */
internal fun Modifier.topShadow(color: Color): Modifier = drawBehind {
    val shadowHeight = TopShadowHeight.toPx()
    drawRect(
        brush = Brush.verticalGradient(
            colors = listOf(Color.Transparent, color.copy(alpha = 0.10f)),
            startY = -shadowHeight,
            endY = 0f,
        ),
        topLeft = Offset(0f, -shadowHeight),
        size = Size(size.width, shadowHeight),
    )
    drawLine(color.copy(alpha = 0.16f), Offset(0f, 0f), Offset(size.width, 0f), strokeWidth = 1.dp.toPx())
}

/** About one and a half rows of stickers, the next row peeking to show the grid scrolls. */
internal val StickerGridHeight = 160.dp

/** The tile's stickers, four or so to a row, each with a crown when premium. */
@Composable
internal fun StickerGrid(
    stickers: StickerLoad<List<StickerAsset>>,
    selectedAssetId: String?,
    isSubscribed: Boolean,
    onRetry: () -> Unit,
    onStickerTapped: (asset: StickerAsset, image: ImageBitmap?, imageBoundsInRoot: Rect) -> Unit,
    modifier: Modifier = Modifier,
) {
    val scheme = MaterialTheme.colorScheme
    when (stickers) {
        StickerLoad.Loading -> StickerGridPlaceholder(modifier)

        StickerLoad.Failed -> Column(
            modifier = modifier,
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(tr("Couldn't load stickers"), color = scheme.onSurface.copy(alpha = 0.7f), style = MaterialTheme.typography.bodyMedium)
            TextButton(onClick = onRetry) { Text(tr("Retry")) }
        }

        is StickerLoad.Ready -> if (stickers.value.isEmpty()) {
            Box(modifier, contentAlignment = Alignment.Center) {
                Text(tr("No stickers here yet"), color = scheme.onSurface.copy(alpha = 0.6f), style = MaterialTheme.typography.bodyMedium)
            }
        } else {
            LazyVerticalGrid(
                columns = GridCells.Adaptive(78.dp),
                contentPadding = PaddingValues(start = 14.dp, end = 14.dp, top = 6.dp, bottom = 14.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = modifier,
            ) {
                items(stickers.value, key = { it.id }) { asset ->
                    StickerCell(
                        asset = asset,
                        selected = asset.id == selectedAssetId,
                        // A premium sticker a non-subscriber is only trying out isn't kept on the disk.
                        keepOnTap = !asset.isPremium || isSubscribed,
                        onClick = { image, bounds -> onStickerTapped(asset, image, bounds) },
                    )
                }
            }
        }
    }
}

/** The sticker grid before its stickers have come in: shimmering tiles in the same places. */
@Composable
private fun StickerGridPlaceholder(modifier: Modifier) {
    LazyVerticalGrid(
        columns = GridCells.Adaptive(78.dp),
        contentPadding = PaddingValues(start = 14.dp, end = 14.dp, top = 6.dp, bottom = 14.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        userScrollEnabled = false,
        modifier = modifier,
    ) {
        items(12) { Box(modifier = Modifier.aspectRatio(1f).shimmer(StickerCellShape)) }
    }
}

/**
 * One sticker, large in its tile. It loads its own picture (rather than through `NetworkImage`) so
 * a tap can hand the picture and where it is drawn to the flight onto the photo.
 *
 * A tap also keeps the sticker's SVG on the disk when [keepOnTap], with a ring in the corner while it
 * downloads; the sticker flies onto the photo meanwhile, as ever, and is redrawn sharp once it is in.
 */
@Composable
private fun StickerCell(
    asset: StickerAsset,
    selected: Boolean,
    keepOnTap: Boolean,
    onClick: (image: ImageBitmap?, imageBoundsInRoot: Rect) -> Unit,
) {
    val scheme = MaterialTheme.colorScheme
    val loader: NetworkImageLoader = koinInject()
    val assetCache: AssetCache = koinInject()
    val scope = rememberCoroutineScope()
    val download by remember(asset.imageUrl) { assetCache.progressOf(asset.imageUrl) }.collectAsState(null)
    val bounce = rememberSpringBounce()
    var image by remember(asset.thumbnailUrl) { mutableStateOf<ImageBitmap?>(null) }
    LaunchedEffect(asset.thumbnailUrl) { image = loader.load(asset.thumbnailUrl) }
    // A plain holder, not state: only read when the cell is tapped.
    val imageCoordinates = remember { arrayOfNulls<LayoutCoordinates>(1) }
    Box(
        modifier = Modifier
            .springBounce(bounce)
            .aspectRatio(1f)
            .clip(StickerCellShape)
            .background(if (selected) scheme.primary.copy(alpha = 0.12f) else scheme.onSurface.copy(alpha = 0.09f))
            .then(if (selected) Modifier.border(1.5.dp, scheme.primary, StickerCellShape) else Modifier)
            .clickable(interactionSource = bounce.interactionSource, indication = LocalIndication.current) {
                val coordinates = imageCoordinates[0]
                val bounds = if (coordinates?.isAttached == true) coordinates.boundsInRoot() else Rect.Zero
                if (keepOnTap) scope.launch { loader.keepSvg(asset.imageUrl) }
                onClick(image, bounds)
            },
        contentAlignment = Alignment.Center,
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(6.dp)
                .onPlaced { imageCoordinates[0] = it },
            contentAlignment = Alignment.Center,
        ) {
            val loaded = image
            if (loaded != null) {
                Image(bitmap = loaded, contentDescription = asset.name, contentScale = ContentScale.Fit, modifier = Modifier.fillMaxSize())
            } else {
                Box(modifier = Modifier.fillMaxSize().shimmer(RoundedCornerShape(10.dp)))
            }
        }
        if (asset.isPremium) PremiumBadge(Modifier.align(Alignment.TopEnd).padding(5.dp))
        DownloadRing(progress = download, modifier = Modifier.align(Alignment.BottomEnd).padding(5.dp))
    }
}

/**
 * The category's sub-category tiles. The server gives them no picture, so each shows the first
 * sticker in it ([thumbnailOf]), fetched as the tile comes into view.
 */
@Composable
internal fun SubcategoryRow(
    subcategories: StickerLoad<List<StickerSubcategory>>,
    selectedId: String?,
    thumbnailOf: suspend (StickerSubcategory) -> String?,
    onSelected: (String) -> Unit,
) {
    val tiles = (subcategories as? StickerLoad.Ready)?.value.orEmpty()
    LazyRow(
        modifier = Modifier.fillMaxWidth().height(TileHeight + 16.dp),
        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        items(tiles, key = { it.id }) { tile ->
            SubcategoryTile(tile = tile, selected = tile.id == selectedId, thumbnailOf = thumbnailOf, onClick = { onSelected(tile.id) })
        }
    }
}

private val TileHeight = 60.dp

@Composable
private fun SubcategoryTile(
    tile: StickerSubcategory,
    selected: Boolean,
    thumbnailOf: suspend (StickerSubcategory) -> String?,
    onClick: () -> Unit,
) {
    val scheme = MaterialTheme.colorScheme
    val bounce = rememberSpringBounce()
    var thumbnail by remember(tile.id) { mutableStateOf<String?>(null) }
    LaunchedEffect(tile.id) { thumbnail = thumbnailOf(tile) }
    Column(
        modifier = Modifier
            .springBounce(bounce)
            .width(92.dp)
            .height(TileHeight)
            .clip(TileShape)
            .background(if (selected) scheme.primary.copy(alpha = 0.12f) else scheme.onSurface.copy(alpha = 0.07f))
            .then(if (selected) Modifier.border(1.5.dp, scheme.primary, TileShape) else Modifier)
            .clickable(interactionSource = bounce.interactionSource, indication = LocalIndication.current, onClick = onClick)
            .padding(horizontal = 6.dp, vertical = 5.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Box(modifier = Modifier.size(28.dp), contentAlignment = Alignment.Center) {
            thumbnail?.let { NetworkImage(url = it, contentDescription = null, modifier = Modifier.fillMaxSize()) }
        }
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            text = tile.name,
            color = scheme.onSurface,
            fontSize = 10.sp,
            // A line just the text's own height: the theme's taller one cut the label off at the tile's foot.
            lineHeight = 12.sp,
            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium,
            textAlign = TextAlign.Center,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

/**
 * The premium crown on a dark round backing, as the Templates and Frames grids show it, so it reads
 * on any sticker.
 */
@Composable
internal fun PremiumBadge(modifier: Modifier = Modifier, size: Dp = 22.dp) {
    Box(
        modifier = modifier
            .size(size)
            .clip(CircleShape)
            .background(Color.Black.copy(alpha = 0.5f)),
        contentAlignment = Alignment.Center,
    ) {
        Image(
            painter = painterResource(Res.drawable.ic_premium_icon),
            contentDescription = tr("Premium"),
            modifier = Modifier.size(size * 0.7f),
        )
    }
}

/** The category pills along the bottom; the picked one is tinted with the brand colour. */
@Composable
internal fun CategoryRow(
    categories: StickerLoad<List<StickerCategory>>,
    selectedId: String?,
    onSelected: (String) -> Unit,
) {
    val scheme = MaterialTheme.colorScheme
    val pills = (categories as? StickerLoad.Ready)?.value.orEmpty()
    LazyRow(
        modifier = Modifier.fillMaxWidth(),
        contentPadding = PaddingValues(start = 12.dp, end = 12.dp, top = 2.dp, bottom = 10.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        items(pills, key = { it.id }) { pill ->
            val selected = pill.id == selectedId
            val bounce = rememberSpringBounce()
            Box(
                modifier = Modifier
                    .springBounce(bounce)
                    .height(30.dp)
                    .clip(RoundedCornerShape(50))
                    .background(if (selected) scheme.primary.copy(alpha = 0.16f) else scheme.onSurface.copy(alpha = 0.07f))
                    .clickable(interactionSource = bounce.interactionSource, indication = LocalIndication.current) { onSelected(pill.id) }
                    .padding(horizontal = 16.dp),
                contentAlignment = Alignment.Center,
            ) {
                // Server names, not keys: they pass through untranslated.
                Text(
                    text = pill.name,
                    color = if (selected) scheme.primary else scheme.onSurface,
                    fontSize = 13.sp,
                    fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium,
                    maxLines = 1,
                )
            }
        }
    }
}

/** The ⋯ menu's actions, top to bottom. */
internal enum class StickerMoreAction { FlipHorizontally, FlipVertically, ToFront, ToBack }

private val MoreMenuWidth = 200.dp
private val MoreMenuRowHeight = 42.dp

/** The selection toolbar's ⋯ menu, a [GlassGenieMenu] unfolding down from the ⋯ like the Text tool's. */
@Composable
internal fun StickerMoreMenu(
    open: Boolean,
    anchor: Rect?,
    liquidState: LiquidState,
    onAction: (StickerMoreAction) -> Unit,
    onDismiss: () -> Unit,
) {
    val color = MaterialTheme.colorScheme.onSurface
    val actions = StickerMoreAction.entries
    GlassGenieMenu(
        open = open,
        anchor = anchor,
        liquidState = liquidState,
        rowCount = actions.size,
        rowHeight = MoreMenuRowHeight,
        width = MoreMenuWidth,
        direction = GenieDirection.Down,
        onDismiss = onDismiss,
    ) { cascade ->
        actions.forEachIndexed { index, action ->
            val (label, icon) = when (action) {
                StickerMoreAction.FlipHorizontally -> tr("Flip horizontally") to Icons.Outlined.SwapHoriz
                StickerMoreAction.FlipVertically -> tr("Flip vertically") to Icons.Outlined.SwapVert
                StickerMoreAction.ToFront -> tr("To front") to Icons.Outlined.FlipToFront
                StickerMoreAction.ToBack -> tr("To back") to Icons.Outlined.FlipToBack
            }
            MenuRow(
                label = label,
                icon = icon,
                color = color,
                height = MoreMenuRowHeight,
                showDivider = index > 0,
                enabled = open,
                dimmed = false,
                onClick = { onAction(action) },
                modifier = Modifier.cascadeIn(cascade, index, actions.size, riseDp = -10f),
            )
        }
    }
}

