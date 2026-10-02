package org.example.project.ui.editor

/**
 * How many past states the editor keeps for undo. Each one is a full-resolution bitmap, so the
 * history is capped to bound memory; the oldest step is dropped first.
 */
internal const val MaxEditHistory = 20

/**
 * How many tool edits anyone can save. With more than this applied, the editor's first Done shows a
 * non-subscriber the paywall (see [EditorViewModel.consumePremiumOffer]).
 */
internal const val FreeEditCount = 3

/**
 * Immutable undo/redo history around a [current] value. Every operation returns a new history.
 *
 * @param undoStack past values, oldest first; the last one is what [undo] restores.
 * @param redoStack undone values, the last one is what [redo] restores.
 */
internal data class EditHistory<T>(
    val current: T,
    val undoStack: List<T> = emptyList(),
    val redoStack: List<T> = emptyList(),
    val maxSize: Int = MaxEditHistory,
) {
    val canUndo: Boolean get() = undoStack.isNotEmpty()
    val canRedo: Boolean get() = redoStack.isNotEmpty()

    /** The edits in effect on [current]: undone ones don't count, redone ones do again. */
    val appliedEdits: Int get() = undoStack.size

    /** Records a new edit: the old [current] becomes undoable, and any redo branch is discarded. */
    fun push(value: T): EditHistory<T> = copy(
        current = value,
        undoStack = (undoStack + current).takeLast(maxSize),
        redoStack = emptyList(),
    )

    fun undo(): EditHistory<T> {
        if (!canUndo) return this
        return copy(
            current = undoStack.last(),
            undoStack = undoStack.dropLast(1),
            redoStack = redoStack + current,
        )
    }

    fun redo(): EditHistory<T> {
        if (!canRedo) return this
        return copy(
            current = redoStack.last(),
            undoStack = undoStack + current,
            redoStack = redoStack.dropLast(1),
        )
    }
}
