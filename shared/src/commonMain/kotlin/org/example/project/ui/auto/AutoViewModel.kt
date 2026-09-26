package org.example.project.ui.auto

import androidx.compose.ui.graphics.ImageBitmap
import androidx.lifecycle.ViewModel
import org.example.project.data.ImageEditSession

class AutoViewModel(private val session: ImageEditSession) : ViewModel() {

    /** Snapshot of the image being edited. Auto screen is only reachable once this is non-null. */
    val sourceImage: ImageBitmap? get() = session.image.value

    fun apply(bitmap: ImageBitmap) {
        session.set(bitmap)
    }
}
