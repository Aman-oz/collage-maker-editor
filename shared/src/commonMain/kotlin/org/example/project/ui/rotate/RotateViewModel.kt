package org.example.project.ui.rotate

import androidx.compose.ui.graphics.ImageBitmap
import androidx.lifecycle.ViewModel
import org.example.project.data.ImageEditSession

class RotateViewModel(private val session: ImageEditSession) : ViewModel() {

    /** Snapshot of the image being edited. Rotate screen is only reachable once this is non-null. */
    val sourceImage: ImageBitmap? get() = session.image.value

    fun applyRotation(bitmap: ImageBitmap) {
        session.set(bitmap)
    }
}
