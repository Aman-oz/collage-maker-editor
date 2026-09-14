package org.example.project.ui.gallery

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.example.project.gallery.GalleryPhoto
import org.example.project.gallery.loadGalleryPhotos
import org.example.project.gallery.resolveGalleryImagePath

sealed interface GalleryLoadState {
    data object Loading : GalleryLoadState
    data class Ready(val photos: List<GalleryPhoto>) : GalleryLoadState
    data object Empty : GalleryLoadState
}

class GalleryViewModel : ViewModel() {

    private val _loadState = MutableStateFlow<GalleryLoadState>(GalleryLoadState.Loading)
    val loadState: StateFlow<GalleryLoadState> = _loadState.asStateFlow()

    private var hasLoaded = false

    /** Safe to call more than once — only queries the device library the first time. */
    fun loadPhotosIfNeeded() {
        if (hasLoaded) return
        hasLoaded = true
        viewModelScope.launch {
            val photos = loadGalleryPhotos()
            _loadState.value = if (photos.isEmpty()) GalleryLoadState.Empty else GalleryLoadState.Ready(photos)
        }
    }

    /** Resolves every selected photo to a `PlatformFile`-compatible path, in parallel. */
    suspend fun resolvePaths(photoIds: List<String>): List<String> = coroutineScope {
        photoIds.map { id -> async { resolveGalleryImagePath(id) } }.awaitAll().filterNotNull()
    }
}
