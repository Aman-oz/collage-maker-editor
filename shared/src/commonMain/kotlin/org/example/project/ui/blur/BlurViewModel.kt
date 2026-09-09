package org.example.project.ui.blur

import androidx.compose.ui.graphics.ImageBitmap
import androidx.lifecycle.ViewModel
import org.example.project.data.ImageEditSession

class BlurViewModel(private val session: ImageEditSession) : ViewModel() {

    /** Snapshot of the image being edited. Blur screen is only reachable once this is non-null. */
    val sourceImage: ImageBitmap? get() = session.image.value

    fun applyBlur(bitmap: ImageBitmap) {
        session.set(bitmap)
    }
}
