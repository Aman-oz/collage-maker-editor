package org.example.project.ui.adjust

import androidx.compose.ui.graphics.ImageBitmap
import androidx.lifecycle.ViewModel
import org.example.project.data.ImageEditSession

class AdjustViewModel(private val session: ImageEditSession) : ViewModel() {

    /** Snapshot of the image being edited. Adjust screen is only reachable once this is non-null. */
    val sourceImage: ImageBitmap? get() = session.image.value

    fun applyAdjustments(bitmap: ImageBitmap) {
        session.set(bitmap)
    }
}
