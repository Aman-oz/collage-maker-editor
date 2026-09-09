package org.example.project.ui.text

import androidx.compose.ui.graphics.ImageBitmap
import androidx.lifecycle.ViewModel
import org.example.project.data.ImageEditSession

class TextViewModel(private val session: ImageEditSession) : ViewModel() {

    /** Snapshot of the image being edited. Text screen is only reachable once this is non-null. */
    val sourceImage: ImageBitmap? get() = session.image.value

    fun applyText(bitmap: ImageBitmap) {
        session.set(bitmap)
    }
}
