package org.example.project.ui.editor

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class EditHistoryTest {

    @Test
    fun newHistory_hasNothingToUndoOrRedo() {
        val history = EditHistory("a")
        assertFalse(history.canUndo)
        assertFalse(history.canRedo)
        assertEquals(history, history.undo())
        assertEquals(history, history.redo())
    }

    @Test
    fun undoThenRedo_walksBackAndForward() {
        val history = EditHistory("a").push("b").push("c")

        val undone = history.undo()
        assertEquals("b", undone.current)
        assertTrue(undone.canRedo)

        val undoneTwice = undone.undo()
        assertEquals("a", undoneTwice.current)
        assertFalse(undoneTwice.canUndo)

        assertEquals("b", undoneTwice.redo().current)
        assertEquals("c", undoneTwice.redo().redo().current)
        assertFalse(undoneTwice.redo().redo().canRedo)
    }

    @Test
    fun pushAfterUndo_discardsRedoBranch() {
        val history = EditHistory("a").push("b").undo().push("c")
        assertEquals("c", history.current)
        assertFalse(history.canRedo)
        assertEquals("a", history.undo().current)
    }

    @Test
    fun push_dropsOldestBeyondMaxSize() {
        var history = EditHistory(0, maxSize = 3)
        for (i in 1..5) history = history.push(i)
        assertEquals(listOf(2, 3, 4), history.undoStack)
        assertEquals(2, history.undo().undo().undo().current)
        assertFalse(history.undo().undo().undo().canUndo)
    }
}
