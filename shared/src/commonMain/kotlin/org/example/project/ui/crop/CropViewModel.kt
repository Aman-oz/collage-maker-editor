package org.example.project.ui.crop

import androidx.compose.ui.graphics.ImageBitmap
import androidx.lifecycle.ViewModel
import org.example.project.data.ImageEditSession

class CropViewModel(private val session: ImageEditSession) : ViewModel() {

    /** Snapshot of the image being edited. Crop screen is only reachable once this is non-null. */
    val sourceImage: ImageBitmap? get() = session.image.value

    fun applyCrop(bitmap: ImageBitmap) {
        session.set(bitmap)
    }
}
