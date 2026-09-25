package org.example.project.ui.gallery

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
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
import kotlinx.coroutines.launch
import org.example.project.gallery.GalleryAccessStatus
import org.example.project.gallery.GalleryAlbum
import org.example.project.gallery.GalleryAlbumSection
import org.example.project.gallery.GalleryPhoto
import org.example.project.gallery.loadGalleryThumbnail
import org.example.project.gallery.rememberGalleryAccessState
import org.example.project.ui.preview.ThemePreviews
import org.koin.compose.viewmodel.koinViewModel

/** The two sources offered by the segmented control at the top of the gallery. */
internal enum class GalleryTab(val label: String) { Photos("Photos"), Collections("Collections") }

private val TopBarHeight = 60.dp
private val BottomPillReserve = 96.dp
private val CircleButtonSize = 44.dp
private val TabWidth = 104.dp

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
 * lift the labels off a busy photo.
 */
@Composable
private fun glassTint(): Color =
    if (MaterialTheme.colorScheme.background.luminance() > 0.5f) {
        Color.White.copy(alpha = 0.2f)
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
        modifier = modifier.fillMaxWidth().height(TopBarHeight).padding(horizontal = 16.dp),
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

        // Disabled glass until something is picked, then a filled primary button.
        GlassCircleButton(
            liquidState = liquidState,
            onClick = onConfirm,
            enabled = canConfirm,
            tint = if (canConfirm) MaterialTheme.colorScheme.primary else glassTint(),
        ) {
            Icon(
                Icons.Filled.Check,
                contentDescription = "Done",
                tint = if (canConfirm) {
                    MaterialTheme.colorScheme.onPrimary
                } else {
                    MaterialTheme.colorScheme.onSurface.copy(alpha = 0.35f)
                },
            )
        }
    }
}

@Composable
private fun GlassCircleButton(
    liquidState: LiquidState,
    onClick: () -> Unit,
    tint: Color,
    enabled: Boolean = true,
    content: @Composable () -> Unit,
) {
    Box(
        modifier = Modifier
            .size(CircleButtonSize)
            .galleryGlass(liquidState, CircleShape, tint)
            .clickable(enabled = enabled, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        content()
    }
}

/**
 * A glass capsule holding a second, sharper glass bubble behind the selected label. Tabs are a
 * fixed equal width so the bubble can slide by a plain offset instead of measuring each label.
 */
@Composable
private fun GlassSegmentedTabs(
    liquidState: LiquidState,
    selectedTab: GalleryTab,
    onTabSelected: (GalleryTab) -> Unit,
) {
    val indicatorOffset by animateDpAsState(
        targetValue = TabWidth * selectedTab.ordinal,
        animationSpec = spring(dampingRatio = 0.7f, stiffness = 380f),
    )
    val bubbleTint = if (MaterialTheme.colorScheme.background.luminance() > 0.5f) {
        Color.White.copy(alpha = 0.45f)
    } else {
        Color.White.copy(alpha = 0.14f)
    }

    Box(
        modifier = Modifier
            .height(CircleButtonSize)
            .galleryGlass(liquidState, CircleShape, glassTint())
            .padding(4.dp),
    ) {
        Box(
            modifier = Modifier
                .offset(x = indicatorOffset)
                .width(TabWidth)
                .fillMaxHeight()
                .clip(CircleShape)
                .liquid(liquidState) {
                    shape = CircleShape
                    frost = 2.dp
                    curve = 0.45f
                    refraction = 0.4f
                    edge = 0.7f
                    dispersion = 0.15f
                    saturation = 1.25f
                    tint = bubbleTint
                },
        )

        Row(modifier = Modifier.fillMaxHeight()) {
            GalleryTab.entries.forEach { tab ->
                val isSelected = tab == selectedTab
                Box(
                    modifier = Modifier
                        .width(TabWidth)
                        .fillMaxHeight()
                        .clip(CircleShape)
                        .clickable { onTabSelected(tab) },
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = tab.label,
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = if (isSelected) 1f else 0.75f),
                    )
                }
            }
        }
    }
}

@Composable
private fun SelectionPill(
    liquidState: LiquidState,
    selectedCount: Int,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .height(48.dp)
            .galleryGlass(liquidState, CircleShape, glassTint())
            .clickable(enabled = selectedCount > 0, onClick = onClick)
            .padding(horizontal = 24.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = if (selectedCount == 0) "Select Photos" else "$selectedCount Select Photos",
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurface,
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
