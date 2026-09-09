package org.example.project.ui.emoji

import androidx.compose.ui.graphics.ImageBitmap
import androidx.lifecycle.ViewModel
import org.example.project.data.ImageEditSession

class EmojiViewModel(private val session: ImageEditSession) : ViewModel() {

    /** Snapshot of the image being edited. Emoji screen is only reachable once this is non-null. */
    val sourceImage: ImageBitmap? get() = session.image.value

    fun applyEmojis(bitmap: ImageBitmap) {
        session.set(bitmap)
    }
}
