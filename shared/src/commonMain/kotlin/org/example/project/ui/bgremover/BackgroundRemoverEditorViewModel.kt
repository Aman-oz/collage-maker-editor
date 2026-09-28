package org.example.project.ui.bgremover

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.ImageBitmap
import androidx.lifecycle.ViewModel
import org.example.project.data.ImageEditSession
import org.example.project.ui.common.copyBitmap

/**
 * Reads the cropped photo from [ImageEditSession] and writes the cut-out back on Done.
 *
 * Unlike most tools, the undo/redo stacks live here rather than in the composable: Done pushes
 * Set Background over this screen, which takes it out of composition, and Back must come back to
 * the same erase history instead of starting over.
 */
class BackgroundRemoverEditorViewModel(session: ImageEditSession) : ViewModel() {

    private val session = session

    /**
     * The cropped photo, captured once and copied so it redraws reliably (see [copyBitmap]).
     * Done replaces the session image with the cut-out, and replaying [ops] over that instead of
     * the photo would leave Recover nothing to bring back.
     */
    val sourceImage: ImageBitmap? = session.image.value?.let(::copyBitmap)

    /** Applied erase ops, oldest first, replayed over [sourceImage]. */
    internal var ops by mutableStateOf(emptyList<EraseOp>())
        private set

    /** Undone ops, the most recently undone last. */
    internal var redoOps by mutableStateOf(emptyList<EraseOp>())
        private set

    internal fun push(op: EraseOp) {
        ops = ops + op
        redoOps = emptyList()
    }

    fun undo() {
        val last = ops.lastOrNull() ?: return
        redoOps = redoOps + last
        ops = ops.dropLast(1)
    }

    fun redo() {
        val next = redoOps.lastOrNull() ?: return
        ops = ops + next
        redoOps = redoOps.dropLast(1)
    }

    fun applyResult(bitmap: ImageBitmap) {
        session.set(bitmap)
    }
}
