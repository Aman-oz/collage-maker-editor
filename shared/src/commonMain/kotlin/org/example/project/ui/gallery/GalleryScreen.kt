package org.example.project.ui.gallery

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
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeContentPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.launch
import org.example.project.gallery.GalleryAccessStatus
import org.example.project.gallery.GalleryPhoto
import org.example.project.gallery.loadGalleryThumbnail
import org.example.project.gallery.rememberGalleryAccessState
import org.example.project.ui.common.AccentPillButton
import org.example.project.ui.common.EditorAccent
import org.example.project.ui.common.EditorBackground
import org.example.project.ui.common.EditorCircleIconButton
import org.example.project.ui.common.EditorControlBackground
import org.example.project.ui.common.EditorLabelTint
import org.example.project.ui.common.EditorOnAccent
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun GalleryScreen(
    maxSelection: Int,
    onBack: () -> Unit,
    onImagesSelected: (List<String>) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: GalleryViewModel = koinViewModel(),
) {
    val loadState by viewModel.loadState.collectAsStateWithLifecycle()
    val accessState = rememberGalleryAccessState()
    val scope = rememberCoroutineScope()
    var selectedIds by remember { mutableStateOf(emptyList<String>()) }
    var isResolving by remember { mutableStateOf(false) }

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
        selectedIds = selectedIds,
        isResolving = isResolving,
        onBack = onBack,
        onRequestAccess = { accessState.requestAccess() },
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
}

@Composable
private fun GalleryContent(
    maxSelection: Int,
    accessStatus: GalleryAccessStatus,
    loadState: GalleryLoadState,
    selectedIds: List<String>,
    isResolving: Boolean,
    onBack: () -> Unit,
    onRequestAccess: () -> Unit,
    onPhotoTapped: (String) -> Unit,
    onConfirm: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(EditorBackground)
            .safeContentPadding(),
    ) {
        GalleryTopBar(
            title = if (maxSelection <= 1) "Select Photo" else "Select Photos",
            subtitle = if (maxSelection > 1) "${selectedIds.size} of $maxSelection selected" else null,
            onBack = onBack,
        )

        Box(modifier = Modifier.fillMaxWidth().weight(1f), contentAlignment = Alignment.Center) {
            when {
                accessStatus == GalleryAccessStatus.Denied || accessStatus == GalleryAccessStatus.NotDetermined ->
                    PermissionRequestContent(onRequestAccess = onRequestAccess)

                loadState is GalleryLoadState.Loading -> CircularProgressIndicator(color = EditorAccent)

                loadState is GalleryLoadState.Empty -> Text(
                    text = "No photos found on this device.",
                    color = EditorLabelTint,
                    style = MaterialTheme.typography.bodyLarge,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(24.dp),
                )

                loadState is GalleryLoadState.Ready -> PhotoGrid(
                    photos = loadState.photos,
                    selectedIds = selectedIds,
                    canSelectMore = selectedIds.size < maxSelection,
                    showSelectionBadge = maxSelection > 1,
                    onPhotoTapped = onPhotoTapped,
                )
            }

            if (isResolving) {
                Box(
                    modifier = Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.6f)),
                    contentAlignment = Alignment.Center,
                ) {
                    CircularProgressIndicator(color = EditorAccent)
                }
            }
        }

        if (maxSelection > 1 && loadState is GalleryLoadState.Ready) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(16.dp),
                horizontalArrangement = Arrangement.Center,
            ) {
                AccentPillButton(
                    text = "Use ${selectedIds.size} Photo${if (selectedIds.size == 1) "" else "s"}",
                    onClick = onConfirm,
                    enabled = selectedIds.isNotEmpty(),
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }
    }
}

@Composable
private fun GalleryTopBar(title: String, subtitle: String?, onBack: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        EditorCircleIconButton(icon = Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", onClick = onBack)

        Column(modifier = Modifier.weight(1f).padding(horizontal = 4.dp)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                textAlign = TextAlign.Center,
                maxLines = 1,
                modifier = Modifier.fillMaxWidth(),
            )
            if (subtitle != null) {
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.labelMedium,
                    color = EditorAccent,
                    textAlign = TextAlign.Center,
                    maxLines = 1,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }

        // Keeps the title centered against the back button on the other side.
        Spacer(modifier = Modifier.size(36.dp))
    }
}

@Composable
private fun PhotoGrid(
    photos: List<GalleryPhoto>,
    selectedIds: List<String>,
    canSelectMore: Boolean,
    showSelectionBadge: Boolean,
    onPhotoTapped: (String) -> Unit,
) {
    LazyVerticalGrid(
        columns = GridCells.Fixed(3),
        contentPadding = PaddingValues(4.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
        modifier = Modifier.fillMaxSize(),
    ) {
        items(photos, key = { it.id }) { photo ->
            val isSelected = selectedIds.contains(photo.id)
            PhotoGridItem(
                photo = photo,
                isSelected = isSelected,
                selectionIndex = selectedIds.indexOf(photo.id) + 1,
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
    selectionIndex: Int,
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
            .background(EditorControlBackground)
            .then(
                if (isSelected) {
                    Modifier.border(width = 3.dp, color = EditorAccent, shape = RoundedCornerShape(8.dp))
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

        if (isSelected) {
            Box(modifier = Modifier.fillMaxSize().background(EditorAccent.copy(alpha = 0.3f)))
            if (showSelectionBadge) {
                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(6.dp)
                        .size(22.dp)
                        .background(EditorAccent, CircleShape),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = "$selectionIndex",
                        color = EditorOnAccent,
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.labelSmall,
                    )
                }
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
            color = Color.White,
            textAlign = TextAlign.Center,
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "Allow access to your photo library to pick images.",
            style = MaterialTheme.typography.bodyMedium,
            color = EditorLabelTint,
            textAlign = TextAlign.Center,
        )
        Spacer(modifier = Modifier.height(24.dp))
        AccentPillButton(text = "Grant Access", onClick = onRequestAccess)
    }
}
