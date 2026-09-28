package org.example.project.ui.bgremover

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.ImageBitmap
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch
import org.example.project.data.AppSettings
import org.example.project.data.ImageEditSession
import org.example.project.data.bgremover.BackgroundRemoverApi
import org.example.project.data.bgremover.BackgroundRemoverException
import org.example.project.ui.common.copyBitmap

/**
 * Reads the cropped photo from [ImageEditSession] and writes the cut-out back on Done.
 *
 * Unlike most tools, the undo/redo stacks live here rather than in the composable: Done pushes
 * Set Background over this screen, which takes it out of composition, and Back must come back to
 * the same erase history instead of starting over.
 */
class BackgroundRemoverEditorViewModel internal constructor(
    private val session: ImageEditSession,
    private val api: BackgroundRemoverApi,
    settings: AppSettings,
) : ViewModel() {

    /** AI Magic is a premium feature. */
    val isPremium: StateFlow<Boolean> = settings.isPremium

    /**
     * True while an AI removal is uploading/downloading. It runs here rather than in the screen so
     * it survives recomposition and a covered entry, and is cancelled when the entry is popped.
     */
    var aiRunning by mutableStateOf(false)
        private set

    private val _messages = Channel<String>(Channel.BUFFERED)

    /** One-off messages (AI failures) for the screen's snackbar. */
    val messages: Flow<String> = _messages.receiveAsFlow()

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

    /** Whether an AI cut-out is currently applied (undoing it lets the user run AI again). */
    internal val hasAiCutOut: Boolean get() = ops.any { it is EraseOp.AiCutOut }

    /**
     * Runs the AI remover on [sourceImage] and pushes its answer as an [EraseOp.AiCutOut] on top of
     * the current edits. Like the LAS app it runs once: while a cut-out is applied, a second tap
     * only says so.
     */
    fun removeBackgroundWithAi() {
        val photo = sourceImage ?: return
        if (aiRunning) return
        if (hasAiCutOut) {
            _messages.trySend("Background is already removed")
            return
        }
        aiRunning = true
        viewModelScope.launch {
            try {
                val cutOut = api.removeBackground(photo)
                // A network-decoded bitmap does not redraw reliably as a scaled draw source.
                push(EraseOp.AiCutOut(copyBitmap(cutOut)))
            } catch (e: BackgroundRemoverException) {
                _messages.trySend(aiErrorMessage(e))
            } finally {
                aiRunning = false
            }
        }
    }

    private fun aiErrorMessage(error: BackgroundRemoverException): String = when (error) {
        is BackgroundRemoverException.NotConfigured -> "AI background remover is not set up"
        is BackgroundRemoverException.Server -> "Could not remove the background. Please try again"
        is BackgroundRemoverException.Network -> "No internet connection. Please try again"
    }

    fun applyResult(bitmap: ImageBitmap) {
        session.set(bitmap)
    }
}
