package org.example.project.ui.overlay

import androidx.compose.ui.graphics.ImageBitmap
import androidx.lifecycle.ViewModel
import org.example.project.data.ImageEditSession

class OverlayViewModel(private val session: ImageEditSession) : ViewModel() {

    /** Snapshot of the image being edited. Overlay screen is only reachable once this is non-null. */
    val sourceImage: ImageBitmap? get() = session.image.value

    fun applyOverlay(bitmap: ImageBitmap) {
        session.set(bitmap)
    }
}
