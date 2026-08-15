package com.formix.spatial.history

class UndoManager {
    private val undoStack = ArrayDeque<Command>()
    private val redoStack = ArrayDeque<Command>()
    private val maxHistory = 100

    var onChanged: (() -> Unit)? = null

    /** Applies [command] immediately and pushes it onto the undo stack. */
    fun execute(command: Command) {
        command.apply()
        undoStack.addLast(command)
        if (undoStack.size > maxHistory) undoStack.removeFirst()
        redoStack.clear()
        onChanged?.invoke()
    }

    fun undo() {
        val cmd = undoStack.removeLastOrNull() ?: return
        cmd.revert()
        redoStack.addLast(cmd)
        onChanged?.invoke()
    }

    fun redo() {
        val cmd = redoStack.removeLastOrNull() ?: return
        cmd.apply()
        undoStack.addLast(cmd)
        onChanged?.invoke()
    }

    fun canUndo() = undoStack.isNotEmpty()
    fun canRedo() = redoStack.isNotEmpty()
}
