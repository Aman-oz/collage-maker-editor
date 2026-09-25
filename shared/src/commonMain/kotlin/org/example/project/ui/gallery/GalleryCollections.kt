package org.example.project.ui.gallery

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import org.example.project.gallery.GalleryAlbum
import org.example.project.gallery.GalleryAlbumSection
import org.example.project.gallery.loadGalleryThumbnail

/**
 * Where the Collections tab is: the sectioned overview, one section expanded into a grid (via a
 * section header's chevron), or one album's photos. A plain in-screen stack rather than nav
 * destinations, so the photo selection carries across all of them and the Photos tab.
 */
internal sealed interface CollectionsRoute {
    data object Overview : CollectionsRoute
    data class Section(val section: GalleryAlbumSection) : CollectionsRoute
    data class Album(val album: GalleryAlbum, val parent: CollectionsRoute) : CollectionsRoute
}

/** One level up: an album returns to whichever view it was opened from. */
internal fun CollectionsRoute.parent(): CollectionsRoute = when (this) {
    CollectionsRoute.Overview, is CollectionsRoute.Section -> CollectionsRoute.Overview
    is CollectionsRoute.Album -> parent
}

internal val GalleryAlbumSection.title: String
    get() = when (this) {
        GalleryAlbumSection.Pinned -> "Pinned"
        GalleryAlbumSection.Albums -> "Albums"
    }

private val AlbumCardSize = 108.dp
private val AlbumCardShape = RoundedCornerShape(14.dp)
private val ScreenGutter = 12.dp

/** Every section with at least one album, in enum order, each as a horizontally scrolling row. */
@Composable
internal fun CollectionsOverview(
    albumsState: GalleryAlbumsState,
    contentPadding: PaddingValues,
    onOpenSection: (GalleryAlbumSection) -> Unit,
    onOpenAlbum: (GalleryAlbum) -> Unit,
) {
    when (albumsState) {
        GalleryAlbumsState.Loading -> CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)

        GalleryAlbumsState.Empty -> CenteredMessage("No albums found on this device.")

        is GalleryAlbumsState.Ready -> {
            val sections = albumsState.albums.groupBy { it.section }
            LazyColumn(
                contentPadding = contentPadding,
                verticalArrangement = Arrangement.spacedBy(20.dp),
                modifier = Modifier.fillMaxSize(),
            ) {
                GalleryAlbumSection.entries.forEach { section ->
                    val albums = sections[section].orEmpty()
                    if (albums.isNotEmpty()) {
                        item(key = section.name) {
                            AlbumSectionRow(
                                title = section.title,
                                albums = albums,
                                onOpenSection = { onOpenSection(section) },
                                onOpenAlbum = onOpenAlbum,
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun AlbumSectionRow(
    title: String,
    albums: List<GalleryAlbum>,
    onOpenSection: () -> Unit,
    onOpenAlbum: (GalleryAlbum) -> Unit,
) {
    Column {
        Row(
            modifier = Modifier
                .padding(horizontal = ScreenGutter - 4.dp)
                .clip(RoundedCornerShape(8.dp))
                .clickable(onClick = onOpenSection)
                .padding(horizontal = 4.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground,
            )
            Icon(
                Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = "See all $title",
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(22.dp),
            )
        }

        LazyRow(
            contentPadding = PaddingValues(horizontal = ScreenGutter),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            modifier = Modifier.padding(top = 6.dp),
        ) {
            items(albums, key = { it.id }) { album ->
                AlbumCard(album = album, onClick = { onOpenAlbum(album) }, modifier = Modifier.width(AlbumCardSize))
            }
        }
    }
}

/** One section's albums as a 3-column grid, reached from that section header's chevron. */
@Composable
internal fun AlbumSectionGrid(
    section: GalleryAlbumSection,
    albums: List<GalleryAlbum>,
    contentPadding: PaddingValues,
    onBack: () -> Unit,
    onOpenAlbum: (GalleryAlbum) -> Unit,
) {
    LazyVerticalGrid(
        columns = GridCells.Fixed(3),
        contentPadding = contentPadding,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier.fillMaxSize(),
    ) {
        item(span = { GridItemSpan(maxLineSpan) }) {
            CollectionsBackHeader(title = section.title, onBack = onBack)
        }
        items(albums, key = { it.id }) { album ->
            AlbumCard(album = album, onClick = { onOpenAlbum(album) })
        }
    }
}

/** "‹ Title" row that leads a drilled-in Collections view and pops back one level. */
@Composable
internal fun CollectionsBackHeader(title: String, onBack: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Row(
            modifier = Modifier
                .clip(RoundedCornerShape(8.dp))
                .clickable(onClick = onBack)
                .padding(end = 8.dp, top = 4.dp, bottom = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                Icons.AutoMirrored.Filled.KeyboardArrowLeft,
                contentDescription = "Back to collections",
                tint = MaterialTheme.colorScheme.onBackground,
                modifier = Modifier.size(26.dp),
            )
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

/** Square cover with the album name over a bottom scrim, so white text stays readable on any photo. */
@Composable
private fun AlbumCard(album: GalleryAlbum, onClick: () -> Unit, modifier: Modifier = Modifier) {
    var cover by remember(album.coverPhotoId) { mutableStateOf<ImageBitmap?>(null) }
    LaunchedEffect(album.coverPhotoId) { cover = loadGalleryThumbnail(album.coverPhotoId) }

    Box(
        modifier = modifier
            .aspectRatio(1f)
            .clip(AlbumCardShape)
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .clickable(onClick = onClick),
    ) {
        cover?.let { bitmap ->
            Image(
                bitmap = bitmap,
                contentDescription = album.name,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize(),
            )
        }

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        0.5f to Color.Transparent,
                        1f to Color.Black.copy(alpha = 0.55f),
                    ),
                ),
        )

        Text(
            text = album.name,
            style = MaterialTheme.typography.labelLarge,
            color = Color.White,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(horizontal = 8.dp, vertical = 7.dp),
        )
    }
}
