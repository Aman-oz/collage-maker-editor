package org.example.project.ui.ratio

import androidx.compose.ui.graphics.ImageBitmap
import androidx.lifecycle.ViewModel
import org.example.project.data.ImageEditSession

class RatioViewModel(private val session: ImageEditSession) : ViewModel() {

    /** Snapshot of the image being edited. Ratio screen is only reachable once this is non-null. */
    val sourceImage: ImageBitmap? get() = session.image.value

    fun applyRatio(bitmap: ImageBitmap) {
        session.set(bitmap)
    }
}
