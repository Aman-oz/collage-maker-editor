package org.example.project.data

import androidx.compose.ui.graphics.ImageBitmap
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Holds the photo currently being edited so the editor and its tool screens (crop, filter, ...)
 * share one in-memory working copy instead of passing image data through navigation arguments.
 */
class ImageEditSession {
    private val _image = MutableStateFlow<ImageBitmap?>(null)
    val image: StateFlow<ImageBitmap?> = _image.asStateFlow()

    fun set(bitmap: ImageBitmap) {
        _image.value = bitmap
    }
}
