package org.example.project.ui.frame

import androidx.compose.ui.graphics.ImageBitmap
import androidx.lifecycle.ViewModel
import org.example.project.data.ImageEditSession

class FrameViewModel(private val session: ImageEditSession) : ViewModel() {

    /** Snapshot of the image being edited. Frame screen is only reachable once this is non-null. */
    val sourceImage: ImageBitmap? get() = session.image.value

    fun applyFrame(bitmap: ImageBitmap) {
        session.set(bitmap)
    }
}
