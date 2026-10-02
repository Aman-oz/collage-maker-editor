package org.example.project.ui.gallery

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.Job
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.example.project.gallery.GalleryAlbum
import org.example.project.gallery.GalleryPhoto
import org.example.project.gallery.loadGalleryAlbumPhotos
import org.example.project.gallery.loadGalleryAlbums
import org.example.project.gallery.loadGalleryPhotos
import org.example.project.gallery.resolveGalleryImagePath

sealed interface GalleryLoadState {
    data object Loading : GalleryLoadState
    data class Ready(val photos: List<GalleryPhoto>) : GalleryLoadState
    data object Empty : GalleryLoadState
}

sealed interface GalleryAlbumsState {
    data object Loading : GalleryAlbumsState
    data class Ready(val albums: List<GalleryAlbum>) : GalleryAlbumsState
    data object Empty : GalleryAlbumsState
}

class GalleryViewModel : ViewModel() {

    private val _loadState = MutableStateFlow<GalleryLoadState>(GalleryLoadState.Loading)
    val loadState: StateFlow<GalleryLoadState> = _loadState.asStateFlow()

    private val _albumsState = MutableStateFlow<GalleryAlbumsState>(GalleryAlbumsState.Loading)
    val albumsState: StateFlow<GalleryAlbumsState> = _albumsState.asStateFlow()

    /** Photos of the album currently opened from Collections; reset to Loading on each open. */
    private val _albumPhotosState = MutableStateFlow<GalleryLoadState>(GalleryLoadState.Loading)
    val albumPhotosState: StateFlow<GalleryLoadState> = _albumPhotosState.asStateFlow()

    /**
     * Photos taken with the gallery's camera cell in a multi-photo pick, newest first. Their
     * [GalleryPhoto.id] is already the captured file's path, since they are not in the library.
     */
    private val _capturedPhotos = MutableStateFlow(emptyList<GalleryPhoto>())
    val capturedPhotos: StateFlow<List<GalleryPhoto>> = _capturedPhotos.asStateFlow()

    private var hasLoaded = false
    private var albumPhotosJob: Job? = null

    /**
     * Safe to call more than once — only queries the device library the first time. Albums load
     * after photos (and in the same coroutine) because on iOS both share one asset lookup cache.
     */
    fun loadPhotosIfNeeded() {
        if (hasLoaded) return
        hasLoaded = true
        viewModelScope.launch {
            val photos = loadGalleryPhotos()
            _loadState.value = if (photos.isEmpty()) GalleryLoadState.Empty else GalleryLoadState.Ready(photos)

            val albums = loadGalleryAlbums()
            _albumsState.value = if (albums.isEmpty()) GalleryAlbumsState.Empty else GalleryAlbumsState.Ready(albums)
        }
    }

    fun openAlbum(albumId: String) {
        albumPhotosJob?.cancel()
        _albumPhotosState.value = GalleryLoadState.Loading
        albumPhotosJob = viewModelScope.launch {
            val photos = loadGalleryAlbumPhotos(albumId)
            _albumPhotosState.value = if (photos.isEmpty()) GalleryLoadState.Empty else GalleryLoadState.Ready(photos)
        }
    }

    fun addCapturedPhoto(path: String) {
        _capturedPhotos.update { listOf(GalleryPhoto(path)) + it }
    }

    /**
     * Resolves every selected photo to a `PlatformFile`-compatible path, in parallel. A captured
     * photo's id is its path already.
     */
    suspend fun resolvePaths(photoIds: List<String>): List<String> = coroutineScope {
        val capturedPaths = _capturedPhotos.value.map { it.id }.toSet()
        photoIds.map { id -> async { if (id in capturedPaths) id else resolveGalleryImagePath(id) } }
            .awaitAll()
            .filterNotNull()
    }
}
