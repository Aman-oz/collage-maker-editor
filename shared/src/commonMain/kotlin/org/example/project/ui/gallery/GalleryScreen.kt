package org.example.project.ui.gallery

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.layout
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.onClick
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigationevent.NavigationEventInfo
import androidx.navigationevent.compose.NavigationBackHandler
import androidx.navigationevent.compose.rememberNavigationEventState
import io.github.fletchmckee.liquid.LiquidState
import io.github.fletchmckee.liquid.liquefiable
import io.github.fletchmckee.liquid.liquid
import io.github.fletchmckee.liquid.rememberLiquidState
import io.github.vinceglb.filekit.dialogs.FileKitMode
import io.github.vinceglb.filekit.dialogs.FileKitType
import io.github.vinceglb.filekit.dialogs.compose.rememberFilePickerLauncher
import io.github.vinceglb.filekit.path
import kotlin.math.abs
import kotlin.math.roundToInt
import kotlinx.coroutines.launch
import org.example.project.gallery.GalleryAccessStatus
import org.example.project.gallery.GalleryAlbum
import org.example.project.gallery.GalleryAlbumSection
import org.example.project.gallery.GalleryPhoto
import org.example.project.gallery.loadGalleryThumbnail
import org.example.project.gallery.rememberGalleryAccessState
import org.example.project.ui.common.SolidDoneButton
import org.example.project.ui.common.TopBarButtonSize
import org.example.project.ui.common.TopBarHeight
import org.example.project.ui.common.bubbleBurstRing
import org.example.project.ui.common.rememberBubbleClick
import org.example.project.ui.common.topBar
import org.example.project.ui.preview.ThemePreviews
import org.koin.compose.viewmodel.koinViewModel

/** The two sources offered by the segmented control at the top of the gallery. */
internal enum class GalleryTab(val label: String) { Photos("Photos"), Collections("Collections") }

private val BottomPillReserve = 96.dp
private val CircleButtonSize = TopBarButtonSize

/** Taller than the circle buttons beside it, so the tabs read as the bar's centrepiece. */
private val TabsHeight = CircleButtonSize + 12.dp
private val TabWidth = 112.dp
private val TabsPadding = 4.dp

/** How much the drop swells while held: past the capsule, like a drop lifted off the glass. */
private val TabDropHeldGrowthWidth = 22.dp
private val TabDropHeldGrowthHeight = 18.dp

/** How much the drop stretches per tab/second of travel, and the cap on it. */
private const val TabStretchPerVelocity = 0.07f
private const val TabMaxStretch = 0.45f

/** Bouncy enough to overshoot visibly, so a press feels like a drop swelling under the finger. */
private val TabDropSpring = spring<Float>(dampingRatio = 0.45f, stiffness = 420f)

/** Chases the finger while held: stiff enough to keep up, loose enough to stretch. */
private val TabFollowSpring = spring<Float>(dampingRatio = 0.7f, stiffness = 700f)

/** Drops the released drop onto its tab with a visible wobble. */
private val TabSettleSpring = spring<Float>(dampingRatio = 0.55f, stiffness = 320f)

private val PillHeight = 48.dp

/** How much the selection pill swells while pressed; it overflows its slot like a lifted drop. */
private val PillHeldGrowthWidth = 20.dp
private val PillHeldGrowthHeight = 14.dp

/** A count change swells the pill by this fraction of a press, then wobbles it back. */
private const val PillPulseGrowth = 0.45f
private val PillPulseSpring = spring<Float>(dampingRatio = 0.32f, stiffness = 380f)

@Composable
fun GalleryScreen(
    maxSelection: Int,
    onBack: () -> Unit,
    onImagesSelected: (List<String>) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: GalleryViewModel = koinViewModel(),
) {
    val loadState by viewModel.loadState.collectAsStateWithLifecycle()
    val albumsState by viewModel.albumsState.collectAsStateWithLifecycle()
    val albumPhotosState by viewModel.albumPhotosState.collectAsStateWithLifecycle()
    val accessState = rememberGalleryAccessState()
    val scope = rememberCoroutineScope()
    var selectedIds by remember { mutableStateOf(emptyList<String>()) }
    var isResolving by remember { mutableStateOf(false) }
    // Hides the denied dialog while the system picker is on screen; it comes back if that's cancelled.
    var isSystemPickerOpen by remember { mutableStateOf(false) }

    // Fallback when library access is denied: the system photo picker needs no permission.
    // Multi-select is only launched when maxSelection > 1; the 2..50 clamp keeps the always-created
    // launcher valid (Android's multi picker rejects a max of 1, FileKit rejects more than 50).
    val singlePhotoPicker = rememberFilePickerLauncher(type = FileKitType.Image) { file ->
        isSystemPickerOpen = false
        if (file != null) onImagesSelected(listOf(file.path))
    }
    val multiPhotoPicker = rememberFilePickerLauncher(
        type = FileKitType.Image,
        mode = FileKitMode.Multiple(maxItems = maxSelection.coerceIn(2, 50)),
    ) { files ->
        isSystemPickerOpen = false
        if (!files.isNullOrEmpty()) onImagesSelected(files.map { it.path })
    }

    LaunchedEffect(Unit) {
        if (accessState.status == GalleryAccessStatus.NotDetermined) {
            accessState.requestAccess()
        }
    }
    LaunchedEffect(accessState.status) {
        if (accessState.status == GalleryAccessStatus.Granted || accessState.status == GalleryAccessStatus.Limited) {
            viewModel.loadPhotosIfNeeded()
        }
    }

    fun confirmSelection(photoIds: List<String>) {
        isResolving = true
        scope.launch {
            val paths = viewModel.resolvePaths(photoIds)
            isResolving = false
            if (paths.isNotEmpty()) {
                onImagesSelected(paths)
            }
        }
    }

    GalleryContent(
        maxSelection = maxSelection,
        accessStatus = accessState.status,
        loadState = loadState,
        albumsState = albumsState,
        albumPhotosState = albumPhotosState,
        selectedIds = selectedIds,
        isResolving = isResolving,
        onBack = onBack,
        onRequestAccess = { accessState.requestAccess() },
        onOpenAlbum = { album -> viewModel.openAlbum(album.id) },
        onPhotoTapped = { photoId ->
            if (maxSelection <= 1) {
                confirmSelection(listOf(photoId))
            } else {
                selectedIds = when {
                    selectedIds.contains(photoId) -> selectedIds - photoId
                    selectedIds.size < maxSelection -> selectedIds + photoId
                    else -> selectedIds
                }
            }
        },
        onConfirm = { confirmSelection(selectedIds) },
        modifier = modifier,
    )

    if (accessState.status == GalleryAccessStatus.Denied && !isSystemPickerOpen) {
        PhotoAccessDeniedDialog(
            maxSelection = maxSelection,
            onPickPhotos = {
                isSystemPickerOpen = true
                if (maxSelection <= 1) singlePhotoPicker.launch() else multiPhotoPicker.launch()
            },
            onOpenSettings = accessState::openSettings,
            onDismiss = onBack,
        )
    }
}

/**
 * Shown whenever photo library access is denied, including when the user was denied earlier and
 * comes back: the system won't prompt again, so we offer the permissionless system picker or a
 * jump to Settings. Dismissing it leaves the gallery, since there is nothing to show behind it.
 */
@Composable
private fun PhotoAccessDeniedDialog(
    maxSelection: Int,
    onPickPhotos: () -> Unit,
    onOpenSettings: () -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(text = "Photo Access Denied", fontWeight = FontWeight.Bold) },
        text = {
            Text(
                text = "You can still pick ${if (maxSelection <= 1) "a photo" else "photos"} without " +
                    "giving access to your whole library.\n\nTo browse your gallery here, open Settings " +
                    "and allow Photos access for this app, then come back.",
            )
        },
        confirmButton = {
            TextButton(onClick = onPickPhotos) {
                Text(text = if (maxSelection <= 1) "Pick Photo" else "Pick Photos", color = MaterialTheme.colorScheme.primary)
            }
        },
        dismissButton = {
            TextButton(onClick = onOpenSettings) {
                Text(text = "Grant Permission", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        },
    )
}

/**
 * Layered rather than stacked: the grid fills the whole screen and scrolls *underneath* the top
 * bar and the bottom selection pill, so those can be Liquid Glass that samples and refracts the
 * photos behind them. Only the content layer is `liquefiable` — marking the glass itself would
 * make it sample its own output.
 */
@Composable
private fun GalleryContent(
    maxSelection: Int,
    accessStatus: GalleryAccessStatus,
    loadState: GalleryLoadState,
    albumsState: GalleryAlbumsState,
    albumPhotosState: GalleryLoadState,
    selectedIds: List<String>,
    isResolving: Boolean,
    onBack: () -> Unit,
    onRequestAccess: () -> Unit,
    onOpenAlbum: (GalleryAlbum) -> Unit,
    onPhotoTapped: (String) -> Unit,
    onConfirm: () -> Unit,
    modifier: Modifier = Modifier,
    initialTab: GalleryTab = GalleryTab.Photos,
) {
    val liquidState = rememberLiquidState()
    var selectedTab by remember { mutableStateOf(initialTab) }
    var collectionsRoute by remember { mutableStateOf<CollectionsRoute>(CollectionsRoute.Overview) }
    val isMultiSelect = maxSelection > 1
    val isInCollectionsChild = selectedTab == GalleryTab.Collections && collectionsRoute != CollectionsRoute.Overview
    val isShowingPhotoGrid = when (selectedTab) {
        GalleryTab.Photos -> loadState is GalleryLoadState.Ready
        GalleryTab.Collections -> collectionsRoute is CollectionsRoute.Album && albumPhotosState is GalleryLoadState.Ready
    }
    // Stays up while browsing albums once something is picked, so the count is never out of sight.
    val showSelectionPill = isMultiSelect && (isShowingPhotoGrid || selectedIds.isNotEmpty())

    // System back inside a drilled-in Collections view goes up a level instead of leaving the gallery.
    NavigationBackHandler(
        state = rememberNavigationEventState(NavigationEventInfo.None),
        isBackEnabled = isInCollectionsChild,
        onBackCompleted = { collectionsRoute = collectionsRoute.parent() },
    )

    fun openAlbum(album: GalleryAlbum) {
        onOpenAlbum(album)
        collectionsRoute = CollectionsRoute.Album(album, parent = collectionsRoute)
    }

    val safeInsets = WindowInsets.safeDrawing.asPaddingValues()
    val contentTop = safeInsets.calculateTopPadding() + TopBarHeight
    val contentBottom = safeInsets.calculateBottomPadding() + if (showSelectionPill) BottomPillReserve else 12.dp
    val gridPadding = PaddingValues(start = 12.dp, end = 12.dp, top = contentTop, bottom = contentBottom)

    Box(modifier = modifier.fillMaxSize()) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .liquefiable(liquidState),
            contentAlignment = Alignment.Center,
        ) {
            when {
                accessStatus == GalleryAccessStatus.Denied || accessStatus == GalleryAccessStatus.NotDetermined ->
                    PermissionRequestContent(onRequestAccess = onRequestAccess)

                selectedTab == GalleryTab.Collections -> when (val route = collectionsRoute) {
                    CollectionsRoute.Overview -> CollectionsOverview(
                        albumsState = albumsState,
                        // Rows scroll edge to edge, so no side gutter here; each row pads its own.
                        contentPadding = PaddingValues(top = contentTop, bottom = contentBottom),
                        onOpenSection = { collectionsRoute = CollectionsRoute.Section(it) },
                        onOpenAlbum = ::openAlbum,
                    )

                    is CollectionsRoute.Section -> AlbumSectionGrid(
                        section = route.section,
                        albums = (albumsState as? GalleryAlbumsState.Ready)?.albums.orEmpty()
                            .filter { it.section == route.section },
                        contentPadding = gridPadding,
                        onBack = { collectionsRoute = route.parent() },
                        onOpenAlbum = ::openAlbum,
                    )

                    is CollectionsRoute.Album -> when (albumPhotosState) {
                        is GalleryLoadState.Ready -> PhotoGrid(
                            photos = albumPhotosState.photos,
                            selectedIds = selectedIds,
                            canSelectMore = selectedIds.size < maxSelection,
                            showSelectionBadge = isMultiSelect,
                            contentPadding = gridPadding,
                            onPhotoTapped = onPhotoTapped,
                            header = { CollectionsBackHeader(title = route.album.name, onBack = { collectionsRoute = route.parent() }) },
                        )

                        GalleryLoadState.Loading -> CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)

                        GalleryLoadState.Empty -> CenteredMessage("This album has no photos.")
                    }
                }

                loadState is GalleryLoadState.Loading -> CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)

                loadState is GalleryLoadState.Empty -> CenteredMessage("No photos found on this device.")

                loadState is GalleryLoadState.Ready -> PhotoGrid(
                    photos = loadState.photos,
                    selectedIds = selectedIds,
                    canSelectMore = selectedIds.size < maxSelection,
                    showSelectionBadge = isMultiSelect,
                    contentPadding = gridPadding,
                    onPhotoTapped = onPhotoTapped,
                )
            }
        }

        GalleryTopBar(
            liquidState = liquidState,
            selectedTab = selectedTab,
            onTabSelected = { selectedTab = it },
            canConfirm = selectedIds.isNotEmpty(),
            onBack = onBack,
            onConfirm = onConfirm,
            modifier = Modifier
                .align(Alignment.TopCenter)
                .windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Top + WindowInsetsSides.Horizontal)),
        )

        if (showSelectionPill) {
            SelectionPill(
                liquidState = liquidState,
                selectedCount = selectedIds.size,
                onClick = onConfirm,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Bottom))
                    .padding(bottom = 16.dp),
            )
        }

        if (isResolving) {
            Box(
                modifier = Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.4f)),
                contentAlignment = Alignment.Center,
            ) {
                CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
            }
        }
    }
}

/**
 * The faint tint every glass element in this screen shares. Kept very low so the glass reads as
 * clear (photos stay sharp and recognisable through it) rather than frosted; it only needs to
 * lift the labels off a busy photo. In light mode it is an off-white a shade darker than the
 * background: a milky haze over photos, yet still a visible capsule over the white background,
 * where a pure white tint would vanish.
 */
@Composable
private fun glassTint(): Color =
    if (MaterialTheme.colorScheme.background.luminance() > 0.5f) {
        Color(0xFFDCDCE0).copy(alpha = 0.55f)
    } else {
        Color.Black.copy(alpha = 0.18f)
    }

/**
 * Clear Liquid Glass: almost no frost, with the look carried by refraction at the curved rim and
 * a bright edge highlight, so the content behind bends at the edges instead of blurring away.
 */
private fun Modifier.galleryGlass(liquidState: LiquidState, shape: Shape, tint: Color): Modifier =
    clip(shape).liquid(liquidState) {
        this.shape = shape
        frost = 2.dp
        curve = 0.4f
        refraction = 0.35f
        edge = 0.65f
        dispersion = 0.18f
        saturation = 1.2f
        this.tint = tint
    }

@Composable
private fun GalleryTopBar(
    liquidState: LiquidState,
    selectedTab: GalleryTab,
    onTabSelected: (GalleryTab) -> Unit,
    canConfirm: Boolean,
    onBack: () -> Unit,
    onConfirm: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier.topBar(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        GlassCircleButton(
            liquidState = liquidState,
            onClick = onBack,
            tint = glassTint(),
        ) {
            Icon(Icons.Filled.Close, contentDescription = "Close", tint = MaterialTheme.colorScheme.onSurface)
        }

        Spacer(modifier = Modifier.weight(1f))
        GlassSegmentedTabs(liquidState = liquidState, selectedTab = selectedTab, onTabSelected = onTabSelected)
        Spacer(modifier = Modifier.weight(1f))

        // Clear glass until something is picked, then the solid primary Done every screen uses.
        if (canConfirm) {
            SolidDoneButton(icon = Icons.Filled.Check, contentDescription = "Done", onClick = onConfirm)
        } else {
            GlassCircleButton(
                liquidState = liquidState,
                onClick = onConfirm,
                enabled = false,
                tint = glassTint(),
            ) {
                Icon(
                    Icons.Filled.Check,
                    contentDescription = "Done",
                    tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                )
            }
        }
    }
}

/** A glass circle with a bright rim, so it still reads as glass over a flat background. */
@Composable
private fun GlassCircleButton(
    liquidState: LiquidState,
    onClick: () -> Unit,
    tint: Color,
    enabled: Boolean = true,
    content: @Composable () -> Unit,
) {
    val isLight = MaterialTheme.colorScheme.background.luminance() > 0.5f
    val rim = Color.White.copy(alpha = if (isLight) 0.75f else 0.35f)
    val bubble = rememberBubbleClick()
    // The slot stays [CircleButtonSize]; the glass inside is resized in layout (Liquid samples from
    // layout bounds, so it can't take a graphicsLayer squish), overshooting on release.
    Box(
        modifier = Modifier
            .size(CircleButtonSize)
            .bubbleBurstRing(bubble, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)),
        contentAlignment = Alignment.Center,
    ) {
        Box(
            modifier = Modifier
                .layout { measurable, constraints ->
                    val side = (constraints.maxWidth * bubble.pressScale).roundToInt().coerceAtLeast(0)
                    val placeable = measurable.measure(Constraints.fixed(side, side))
                    layout(constraints.maxWidth, constraints.maxHeight) {
                        placeable.place((constraints.maxWidth - side) / 2, (constraints.maxHeight - side) / 2)
                    }
                }
                .galleryGlass(liquidState, CircleShape, tint)
                .border(1.dp, Brush.verticalGradient(listOf(rim, rim.copy(alpha = 0.08f))), CircleShape)
                .clickable(
                    interactionSource = bubble.interactionSource,
                    indication = null,
                    enabled = enabled,
                    onClick = { bubble.tap(onClick) },
                ),
            contentAlignment = Alignment.Center,
        ) {
            Box(
                modifier = Modifier.graphicsLayer {
                    scaleX = bubble.pressScale
                    scaleY = bubble.pressScale
                },
            ) { content() }
        }
    }
}

/**
 * A glass capsule holding a liquid selection drop behind the selected label, the gallery's take on
 * [org.example.project.ui.home.HomeBottomBar]'s bubble. A finger down anywhere on the capsule swells
 * the drop past the capsule's height into a clear lens that follows the finger (a tap on the other
 * tab flies it there); on release it snaps to the nearest tab, selects it and settles back. One
 * `pointerInput` owns all of it, so the labels carry only accessibility semantics.
 *
 * Tabs are a fixed equal width, so the drop's centre is just a fractional tab index. The drop is
 * sized and placed in layout, never with a `graphicsLayer` scale: Liquid samples the backdrop from
 * layout bounds, so a scaled layer would refract the wrong region.
 */
@Composable
private fun GlassSegmentedTabs(
    liquidState: LiquidState,
    selectedTab: GalleryTab,
    onTabSelected: (GalleryTab) -> Unit,
) {
    val isLight = MaterialTheme.colorScheme.background.luminance() > 0.5f
    val rim = Color.White.copy(alpha = if (isLight) 0.75f else 0.35f)
    val scope = rememberCoroutineScope()
    val position = remember { Animatable(selectedTab.ordinal.toFloat()) }
    var held by remember { mutableStateOf(false) }
    // The tab the held drop is over, so that label swells with it.
    val hoveredTab by remember { derivedStateOf { position.value.roundToInt() } }
    val currentSelectedTab by rememberUpdatedState(selectedTab)
    val currentOnTabSelected by rememberUpdatedState(onTabSelected)

    // Selection changed from outside (or by the release below): glide there, unless a finger has it.
    LaunchedEffect(selectedTab) {
        if (!held) position.animateTo(selectedTab.ordinal.toFloat(), TabSettleSpring)
    }

    Box(
        modifier = Modifier
            .height(TabsHeight)
            .width(TabWidth * GalleryTab.entries.size + TabsPadding * 2)
            .pointerInput(Unit) {
                val inset = TabsPadding.toPx()
                val lastTab = (GalleryTab.entries.size - 1).toFloat()
                fun tabAt(x: Float) = ((x - inset) / TabWidth.toPx() - 0.5f).coerceIn(0f, lastTab)
                awaitEachGesture {
                    val down = awaitFirstDown()
                    down.consume()
                    held = true
                    scope.launch { position.animateTo(tabAt(down.position.x), TabFollowSpring) }
                    var lastX = down.position.x
                    try {
                        while (true) {
                            val change = awaitPointerEvent().changes.firstOrNull { it.id == down.id } ?: break
                            lastX = change.position.x
                            if (!change.pressed) break
                            change.consume()
                            // A fresh animateTo carries the running velocity over, so the drop keeps
                            // its inertia (and its stretch) while following.
                            scope.launch { position.animateTo(tabAt(lastX), TabFollowSpring) }
                        }
                    } finally {
                        held = false
                    }
                    val tab = GalleryTab.entries[tabAt(lastX).roundToInt()]
                    scope.launch { position.animateTo(tab.ordinal.toFloat(), TabSettleSpring) }
                    if (tab != currentSelectedTab) currentOnTabSelected(tab)
                }
            },
    ) {
        Box(
            modifier = Modifier
                .matchParentSize()
                .galleryGlass(liquidState, CircleShape, glassTint())
                .border(1.dp, Brush.verticalGradient(listOf(rim, rim.copy(alpha = 0.08f))), CircleShape),
        )

        TabSelectionDrop(liquidState = liquidState, position = position, held = held, isLight = isLight)

        Row(modifier = Modifier.fillMaxSize().padding(horizontal = TabsPadding)) {
            GalleryTab.entries.forEach { tab ->
                val isSelected = tab == selectedTab
                // Magnified under the held drop, like text seen through water. Text isn't glass, so
                // a graphicsLayer scale is safe here.
                val labelScale by animateFloatAsState(
                    targetValue = if (held && hoveredTab == tab.ordinal) 1.12f else 1f,
                    animationSpec = TabDropSpring,
                )
                Box(
                    modifier = Modifier
                        .width(TabWidth)
                        .fillMaxHeight()
                        .semantics(mergeDescendants = true) {
                            role = Role.Tab
                            selected = isSelected
                            onClick(label = tab.label, action = {
                                onTabSelected(tab)
                                true
                            })
                        },
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = tab.label,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = if (isSelected) 1f else 0.7f),
                        maxLines = 1,
                        softWrap = false,
                        modifier = Modifier.graphicsLayer {
                            scaleX = labelScale
                            scaleY = labelScale
                        },
                    )
                }
            }
        }
    }
}

/**
 * The drop behind the selected tab, centred on [position] (a fractional tab index). It stretches
 * along its travel in proportion to its speed and squashes a little in height, so the volume reads
 * as conserved. While [held] it swells past the capsule into a clearer, more strongly refracting
 * lens; released, it settles back. One press progress drives size and material together.
 */
@Composable
private fun BoxScope.TabSelectionDrop(
    liquidState: LiquidState,
    position: Animatable<Float, *>,
    held: Boolean,
    isLight: Boolean,
) {
    val press by animateFloatAsState(if (held) 1f else 0f, TabDropSpring)
    // The spring overshoots slightly past 0 / 1; only the size may use the overshoot.
    val t = press.coerceIn(0f, 1f)
    // Resting it is a milky white pill; held it clears toward glass so the lens shows.
    val body = if (isLight) Color.White.copy(alpha = 0.72f - 0.4f * t) else Color.White.copy(alpha = 0.14f - 0.06f * t)
    val rim = Color.White.copy(alpha = if (isLight) 0.95f else 0.4f + 0.3f * t)
    val rimLow = if (isLight) Color(0xFF787880).copy(alpha = 0.25f + 0.1f * t) else rim.copy(alpha = rim.alpha * 0.4f)

    Box(
        modifier = Modifier
            .matchParentSize()
            .layout { measurable, constraints ->
                val stretch = (abs(position.velocity) * TabStretchPerVelocity).coerceAtMost(TabMaxStretch)
                val restHeight = TabsHeight.toPx() - TabsPadding.toPx() * 2
                val w = (TabWidth.toPx() * (1f + stretch) + TabDropHeldGrowthWidth.toPx() * press).roundToInt().coerceAtLeast(0)
                val h = (restHeight * (1f - stretch * 0.22f) + TabDropHeldGrowthHeight.toPx() * press).roundToInt().coerceAtLeast(0)
                val placeable = measurable.measure(Constraints.fixed(w, h))
                layout(constraints.maxWidth, constraints.maxHeight) {
                    val x = TabsPadding.toPx() + TabWidth.toPx() * (position.value + 0.5f) - w / 2f
                    placeable.place(x.roundToInt(), (constraints.maxHeight - h) / 2)
                }
            }
            .clip(CircleShape)
            .liquid(liquidState) {
                shape = CircleShape
                // Gentle, like the magnification of a real drop; no frost, whose blur drags the
                // transparent pixels outside the lens into the rim as a grey shadow.
                frost = 0.dp
                curve = 0.25f + 0.12f * t
                refraction = 0.18f + 0.12f * t
                edge = 0.35f + 0.25f * t
                dispersion = 0.1f * t
                saturation = 1.2f
                tint = body
            }
            // Over the glass as well: Liquid's tint alone renders far fainter than its alpha.
            .drawWithContent {
                drawContent()
                val corner = CornerRadius(size.height / 2f)
                drawRoundRect(color = body, cornerRadius = corner)
                // Light focused onto the drop's lower inside edge.
                drawRoundRect(
                    brush = Brush.verticalGradient(
                        0.6f to Color.White.copy(alpha = 0f),
                        1f to Color.White.copy(alpha = if (isLight) 0.35f + 0.2f * t else 0.08f + 0.06f * t),
                    ),
                    cornerRadius = corner,
                )
            }
            .border(
                width = 1.dp + 0.5.dp * t,
                brush = Brush.verticalGradient(0f to rim, 0.5f to rim.copy(alpha = rim.alpha * 0.2f), 1f to rimLow),
                shape = CircleShape,
            ),
    )
}

/**
 * The bottom "N Select Photos" pill, a liquid drop like the tabs' selection. Pressed, it swells
 * with an overshoot into a clearer, more strongly refracting lens and settles back on release;
 * each change of the count gives it a small wobble, as if the picked photo dripped into it.
 *
 * The swell is done in layout, never with a `graphicsLayer` scale (Liquid samples the backdrop
 * from layout bounds). The pill reports its resting size and places the grown glass centred over
 * it, so growing never pushes it off its bottom-centre anchor.
 */
@Composable
private fun SelectionPill(
    liquidState: LiquidState,
    selectedCount: Int,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val isLight = MaterialTheme.colorScheme.background.luminance() > 0.5f
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val press by animateFloatAsState(if (pressed) 1f else 0f, TabDropSpring)
    // 0 at rest; a count change kicks it up and a loose spring brings it back with an overshoot.
    val pulse = remember { Animatable(0f) }
    var lastCount by remember { mutableIntStateOf(selectedCount) }
    LaunchedEffect(selectedCount) {
        if (selectedCount == lastCount) return@LaunchedEffect
        lastCount = selectedCount
        pulse.animateTo(1f, tween(durationMillis = 90, easing = FastOutSlowInEasing))
        pulse.animateTo(0f, PillPulseSpring)
    }
    // The springs overshoot slightly past 0 / 1; only the size may use the overshoot.
    val t = press.coerceIn(0f, 1f)
    val restTint = glassTint()
    val rim = Color.White.copy(alpha = if (isLight) 0.85f else 0.35f + 0.3f * t)
    val rimLow = if (isLight) Color(0xFF787880).copy(alpha = 0.2f + 0.1f * t) else rim.copy(alpha = rim.alpha * 0.3f)
    val labelScale = 1f + 0.06f * press + 0.04f * pulse.value

    Box(
        modifier = modifier
            .layout { measurable, _ ->
                val baseHeight = PillHeight.roundToPx()
                // Rounded up a pixel so the label never gets less than its full width.
                val baseWidth = measurable.maxIntrinsicWidth(baseHeight) + 1
                val growth = press + PillPulseGrowth * pulse.value
                val w = (baseWidth + PillHeldGrowthWidth.toPx() * growth).roundToInt().coerceAtLeast(0)
                val h = (baseHeight + PillHeldGrowthHeight.toPx() * growth).roundToInt().coerceAtLeast(0)
                val placeable = measurable.measure(Constraints.fixed(w, h))
                layout(baseWidth, baseHeight) {
                    placeable.place((baseWidth - w) / 2, (baseHeight - h) / 2)
                }
            }
            .clip(CircleShape)
            .liquid(liquidState) {
                shape = CircleShape
                frost = 2.dp * (1f - t)
                curve = 0.4f + 0.12f * t
                refraction = 0.35f + 0.15f * t
                edge = 0.65f + 0.2f * t
                dispersion = 0.18f + 0.07f * t
                saturation = 1.2f
                tint = restTint.copy(alpha = restTint.alpha * (1f - 0.5f * t))
            }
            // Light focused onto the drop's lower inside edge, brighter as it swells.
            .drawWithContent {
                drawContent()
                drawRoundRect(
                    brush = Brush.verticalGradient(
                        0.6f to Color.White.copy(alpha = 0f),
                        1f to Color.White.copy(alpha = if (isLight) 0.3f + 0.25f * t else 0.06f + 0.08f * t),
                    ),
                    cornerRadius = CornerRadius(size.height / 2f),
                )
            }
            .border(
                width = 1.dp + 0.5.dp * t,
                brush = Brush.verticalGradient(0f to rim, 0.5f to rim.copy(alpha = rim.alpha * 0.2f), 1f to rimLow),
                shape = CircleShape,
            )
            .clickable(
                interactionSource = interaction,
                indication = null,
                enabled = selectedCount > 0,
                onClick = onClick,
            )
            .padding(horizontal = 24.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = if (selectedCount == 0) "Select Photos" else "$selectedCount Select Photos",
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurface,
            // The pill is sized from this label's intrinsic width, and rounding that to whole
            // pixels can leave it a hair short, which would wrap it mid-animation.
            maxLines = 1,
            softWrap = false,
            // Text isn't glass, so a graphicsLayer scale is safe here.
            modifier = Modifier.graphicsLayer {
                scaleX = labelScale
                scaleY = labelScale
            },
        )
    }
}

@Composable
internal fun CenteredMessage(text: String) {
    Text(
        text = text,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        style = MaterialTheme.typography.bodyLarge,
        textAlign = TextAlign.Center,
        modifier = Modifier.padding(24.dp),
    )
}

@Composable
private fun PhotoGrid(
    photos: List<GalleryPhoto>,
    selectedIds: List<String>,
    canSelectMore: Boolean,
    showSelectionBadge: Boolean,
    contentPadding: PaddingValues,
    onPhotoTapped: (String) -> Unit,
    header: (@Composable () -> Unit)? = null,
) {
    LazyVerticalGrid(
        columns = GridCells.Fixed(3),
        contentPadding = contentPadding,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier.fillMaxSize(),
    ) {
        if (header != null) {
            item(span = { GridItemSpan(maxLineSpan) }) { header() }
        }
        items(photos, key = { it.id }) { photo ->
            val isSelected = selectedIds.contains(photo.id)
            PhotoGridItem(
                photo = photo,
                isSelected = isSelected,
                showSelectionBadge = showSelectionBadge,
                canSelect = canSelectMore || isSelected,
                onClick = { onPhotoTapped(photo.id) },
            )
        }
    }
}

@Composable
private fun PhotoGridItem(
    photo: GalleryPhoto,
    isSelected: Boolean,
    showSelectionBadge: Boolean,
    canSelect: Boolean,
    onClick: () -> Unit,
) {
    var thumbnail by remember(photo.id) { mutableStateOf<ImageBitmap?>(null) }
    LaunchedEffect(photo.id) { thumbnail = loadGalleryThumbnail(photo.id) }

    Box(
        modifier = Modifier
            .aspectRatio(1f)
            .clip(RoundedCornerShape(8.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .then(
                if (isSelected) {
                    Modifier.border(width = 2.dp, color = MaterialTheme.colorScheme.primary, shape = RoundedCornerShape(8.dp))
                } else {
                    Modifier
                },
            )
            .clickable(enabled = canSelect, onClick = onClick),
    ) {
        thumbnail?.let { bitmap ->
            Image(
                bitmap = bitmap,
                contentDescription = "Photo",
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize(),
            )
        }

        if (isSelected && showSelectionBadge) {
            Box(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(6.dp)
                    .size(22.dp)
                    .background(MaterialTheme.colorScheme.primary, CircleShape)
                    .border(width = 1.5.dp, color = Color.White, shape = CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    Icons.Filled.Check,
                    contentDescription = "Selected",
                    tint = MaterialTheme.colorScheme.onPrimary,
                    modifier = Modifier.size(14.dp),
                )
            }
        } else if (!canSelect) {
            Box(modifier = Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.5f)))
        }
    }
}

@Composable
private fun PermissionRequestContent(onRequestAccess: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize().padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text(
            text = "Photo Access Needed",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground,
            textAlign = TextAlign.Center,
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "Allow access to your photo library to pick images.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
        Spacer(modifier = Modifier.height(24.dp))
        Button(
            onClick = onRequestAccess,
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
            ),
        ) {
            Text(text = "Grant Access", fontWeight = FontWeight.SemiBold)
        }
    }
}

// Thumbnails load through the platform gallery, so preview cells and album covers render as
// empty placeholders; the previews are for the glass chrome, layout and selection state.
private val PreviewPhotos = List(18) { GalleryPhoto(id = "photo-$it") }

private val PreviewAlbums = listOf(
    "Favourites", "Screenshots", "Selfies", "Portrait",
).mapIndexed { index, name ->
    GalleryAlbum("pinned-$index", name, GalleryAlbumSection.Pinned, coverPhotoId = "photo-$index", photoCount = 12)
} + listOf(
    "WhatsApp", "Snapchat", "Instagram", "Downloads",
).mapIndexed { index, name ->
    GalleryAlbum("album-$index", name, GalleryAlbumSection.Albums, coverPhotoId = "photo-${index + 4}", photoCount = 30)
}

@Composable
private fun GalleryContentForPreview(initialTab: GalleryTab, selectedIds: List<String>) {
    GalleryContent(
        maxSelection = 10,
        accessStatus = GalleryAccessStatus.Granted,
        loadState = GalleryLoadState.Ready(PreviewPhotos),
        albumsState = GalleryAlbumsState.Ready(PreviewAlbums),
        albumPhotosState = GalleryLoadState.Loading,
        selectedIds = selectedIds,
        isResolving = false,
        onBack = {},
        onRequestAccess = {},
        onOpenAlbum = {},
        onPhotoTapped = {},
        onConfirm = {},
        initialTab = initialTab,
    )
}

@Preview
@Composable
private fun GalleryPhotosPreview() {
    ThemePreviews {
        GalleryContentForPreview(GalleryTab.Photos, selectedIds = listOf("photo-1", "photo-3", "photo-7"))
    }
}

@Preview
@Composable
private fun GalleryCollectionsPreview() {
    ThemePreviews {
        GalleryContentForPreview(GalleryTab.Collections, selectedIds = emptyList())
    }
}
