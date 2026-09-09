package org.example.project.ui.filter

import androidx.compose.ui.graphics.ImageBitmap
import androidx.lifecycle.ViewModel
import org.example.project.data.ImageEditSession

class FilterViewModel(private val session: ImageEditSession) : ViewModel() {

    /** Snapshot of the image being edited. Filter screen is only reachable once this is non-null. */
    val sourceImage: ImageBitmap? get() = session.image.value

    fun applyFilter(bitmap: ImageBitmap) {
        session.set(bitmap)
    }
}
