package org.example.project.ui.reveal

import androidx.compose.ui.graphics.ImageBitmap
import androidx.lifecycle.ViewModel
import org.example.project.data.ImageEditSession

/**
 * Backs the reveal-based tools (Splash, s-Blur, s-Splash): it exposes the working image from the
 * shared [ImageEditSession] and writes the composited result back on Done. One definition is enough
 * because each tool destination gets its own nav-scoped instance.
 */
class RevealEditViewModel(private val session: ImageEditSession) : ViewModel() {

    /** Snapshot of the image being edited; the reveal screens are only reachable once non-null. */
    val sourceImage: ImageBitmap? get() = session.image.value

    fun applyResult(bitmap: ImageBitmap) {
        session.set(bitmap)
    }
}
