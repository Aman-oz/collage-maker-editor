package org.example.project.data

import androidx.compose.ui.graphics.ImageBitmap

/**
 * Carries the bitmap the save screen just exported over to the share screen, which is otherwise
 * handed only the exported file's path. Decoding that file takes a few frames, and a shared-element
 * transition needs its target on screen from the first one, so the share screen starts from this
 * bitmap instead and only decodes the file when there is nothing here (the back stack restored
 * after process death).
 *
 * It holds at most one image, and [take] gives it up, so a full-resolution bitmap is not kept alive
 * past the screen that shows it.
 */
class SavedImageHandoff {
    private var path: String? = null
    private var image: ImageBitmap? = null

    fun put(path: String, image: ImageBitmap) {
        this.path = path
        this.image = image
    }

    /** The image exported to [path], if it is the one being held; it is released either way. */
    fun take(path: String): ImageBitmap? {
        val held = image.takeIf { this.path == path }
        this.path = null
        image = null
        return held
    }
}
