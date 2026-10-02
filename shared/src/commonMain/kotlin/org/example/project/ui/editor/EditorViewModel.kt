package org.example.project.ui.editor

import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.graphics.ImageBitmap
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.github.vinceglb.filekit.PlatformFile
import io.github.vinceglb.filekit.dialogs.compose.util.toImageBitmap
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.example.project.data.AppSettings
import org.example.project.data.ImageEditSession
import org.example.project.i18n.tr

sealed interface EditorUiState {
    data object Loading : EditorUiState
    data class Ready(
        val image: ImageBitmap,
        val canUndo: Boolean = false,
        val canRedo: Boolean = false,
    ) : EditorUiState
    data class Error(val message: String) : EditorUiState
}

/**
 * Decodes the picked image so the editing tools can work on it, and keeps [uiState] in sync with
 * [ImageEditSession], the working copy the save screen reads.
 *
 * Undo/redo is built on that same observation: every tool's result goes to the session
 * ([applyEdit]), so each new bitmap the session emits is recorded as one step in an [EditHistory] —
 * no tool needs to know history exists. Undo/redo write the restored bitmap back to the session
 * too, so the save screen and the next tool see it.
 *
 * [imagePath] is passed in from the navigation key via Koin's `parametersOf`. When it is `null` the
 * image was already put in the session by another editor (e.g. a baked collage), so nothing is
 * decoded here.
 */
class EditorViewModel(
    private val imagePath: String?,
    private val session: ImageEditSession,
    settings: AppSettings,
) : ViewModel() {

    /**
     * Whether the user is subscribed; some tools (Filter, Overlay, Adjust, Text, Sticker) show
     * everyone else the paywall on Done.
     */
    val isPremium: StateFlow<Boolean> = settings.isPremium

    /**
     * The tool open inside the editor, or `null`. It lives here rather than in the screen because
     * the paywall (opened from a tool's Done) covers the editor, and a covered nav entry
     * leaves composition and loses its `remember` state: the tool has to still be open on return.
     */
    internal val toolSession = mutableStateOf<ToolSession?>(null)

    private val _uiState = MutableStateFlow<EditorUiState>(EditorUiState.Loading)

    /**
     * Whether the session holds *this* editor's image. Until the picked file is decoded, the
     * session may still hold the previous photo; showing it (or letting a tool auto-open on it)
     * would edit the wrong image.
     */
    private var imageLoaded = imagePath == null
    val uiState: StateFlow<EditorUiState> = _uiState.asStateFlow()

    /** `null` until this editor's image is in the session; its first bitmap is the base state. */
    private var history: EditHistory<ImageBitmap>? = null

    init {
        loadImage()
        observeSession()
    }

    private fun loadImage() {
        if (imagePath == null) {
            // The session is in-memory only, so after process death there is nothing to restore.
            if (session.image.value == null) _uiState.value = EditorUiState.Error(tr("This image is no longer available"))
            return
        }
        viewModelScope.launch {
            runCatching { PlatformFile(imagePath).toImageBitmap() }
                .onSuccess {
                    imageLoaded = true
                    session.set(it)
                }
                .onFailure { _uiState.value = EditorUiState.Error(it.message ?: tr("Could not open this image")) }
        }
    }

    private fun observeSession() {
        viewModelScope.launch {
            session.image.collect { bitmap ->
                if (bitmap == null || !imageLoaded) return@collect
                val current = history
                history = when {
                    current == null -> EditHistory(bitmap)
                    // Our own undo/redo write echoing back from the session — already recorded.
                    bitmap === current.current -> current
                    else -> current.push(bitmap)
                }
                publish()
            }
        }
    }

    /**
     * Commits the result of a tool (they all open inside the editor). It goes through the session,
     * so [observeSession] records it as one undoable step.
     */
    fun applyEdit(bitmap: ImageBitmap) {
        session.set(bitmap)
    }

    private var premiumOfferShown = false

    /**
     * Whether the editor's Done should open the paywall instead of going on to save: `true` once,
     * for a non-subscriber with more than [FreeEditCount] tool edits applied. It is an offer, not a
     * gate, so asking marks it as made and the next Done goes through either way.
     */
    fun consumePremiumOffer(): Boolean {
        val edits = history?.appliedEdits ?: 0
        if (isPremium.value || premiumOfferShown || edits <= FreeEditCount) return false
        premiumOfferShown = true
        return true
    }

    fun undo() = restore { it.undo() }

    fun redo() = restore { it.redo() }

    private inline fun restore(step: (EditHistory<ImageBitmap>) -> EditHistory<ImageBitmap>) {
        val current = history ?: return
        val next = step(current)
        if (next === current) return
        // Update history before writing, so the collector recognizes the echo instead of pushing it.
        history = next
        session.set(next.current)
        publish()
    }

    private fun publish() {
        val current = history ?: return
        _uiState.value = EditorUiState.Ready(current.current, current.canUndo, current.canRedo)
    }
}
