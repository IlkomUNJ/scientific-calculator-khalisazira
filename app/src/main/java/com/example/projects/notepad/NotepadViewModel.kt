package com.example.projects.notepad

import androidx.compose.ui.unit.sp
import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import java.util.Date
import java.util.Calendar

class NotepadViewModel : ViewModel() {

    private val _state = MutableStateFlow(NotepadState(
        notes = getSampleNotes()
    ))
    val state: StateFlow<NotepadState> = _state.asStateFlow()

    private fun getOffsetDate(offsetAmount: Int, field: Int): Date {
        val calendar = Calendar.getInstance()
        calendar.add(field, offsetAmount)
        return calendar.time
    }

    // --- Note List Operations ---

    fun addNewNote() {
        val newNote = Note(title = "", content = "")
        _state.update {
            it.copy(
                notes = listOf(newNote) + it.notes,
                selectedNoteId = newNote.id,
                currentNoteTitle = newNote.title,
                currentNoteContent = newNote.content,
                currentNoteFontSize = 16.sp,
                currentNoteIsBold = false,
                currentNoteIsItalic = false
            )
        }
    }

    // CRASH FIX: Simplified selectNote for thread safety and stability.
    fun selectNote(noteId: String) {
        _state.update {
            val note = it.notes.firstOrNull { n -> n.id == noteId }

            if (note == null) {
                return@update it
            }

            it.copy(
                selectedNoteId = noteId,
                currentNoteTitle = note.title,
                currentNoteContent = note.content
            )
        }
    }

    fun updateNote(noteId: String, newTitle: String, newContent: String) {
        _state.update { currentState ->
            val updatedNotes = currentState.notes.map { note ->
                if (note.id == noteId) {
                    note.copy(
                        title = newTitle.ifBlank { "New Note" },
                        content = newContent,
                        lastModifiedTime = Date()
                    )
                } else note
            }.sortedWith(compareByDescending<Note> { it.isPinned }.thenByDescending { it.lastModifiedTime })

            currentState.copy(notes = updatedNotes)
        }
    }

    fun togglePinNote(noteId: String) {
        _state.update { currentState ->
            val updatedNotes = currentState.notes.map { note ->
                if (note.id == noteId) {
                    note.copy(isPinned = !note.isPinned)
                } else note
            }.sortedWith(compareByDescending<Note> { it.isPinned }.thenByDescending { it.lastModifiedTime })

            currentState.copy(notes = updatedNotes)
        }
    }

    fun deleteNote(noteId: String) {
        _state.update {
            it.copy(
                notes = it.notes.filter { note -> note.id != noteId },
                selectedNoteId = if (it.selectedNoteId == noteId) null else it.selectedNoteId
            )
        }
    }

    fun clearSelectedNote() {
        _state.value.selectedNoteId?.let { noteId ->
            updateNote(noteId, _state.value.currentNoteTitle, _state.value.currentNoteContent)
        }
        _state.update { it.copy(selectedNoteId = null) }
    }

    // --- Text Editing Operations ---

    fun onNoteTitleChanged(newTitle: String) = _state.update { it.copy(currentNoteTitle = newTitle) }
    fun onNoteContentChanged(newContent: String) = _state.update { it.copy(currentNoteContent = newContent) }
    fun toggleBold() = _state.update { it.copy(currentNoteIsBold = !it.currentNoteIsBold) }
    fun toggleItalic() = _state.update { it.copy(currentNoteIsItalic = !it.currentNoteIsItalic) }
    fun changeFontSize(increase: Boolean) {
        _state.update {
            val currentSize = it.currentNoteFontSize.value
            val newSize = if (increase) currentSize + 2 else currentSize - 2
            it.copy(currentNoteFontSize = newSize.coerceIn(12f, 30f).sp)
        }
    }
    fun resetStyle() = _state.update { it.copy(currentNoteIsBold = false, currentNoteIsItalic = false) }

    // --- Stub Functions --- (For Cut/Copy/Paste/Save)
    fun saveNote() { /* Implementation stub */ }
    fun undo() { /* Implementation stub */ }
    fun redo() { /* Implementation stub */ }
    fun cut() { /* Implementation stub */ }
    fun copy() { /* Implementation stub */ }
    fun paste() { /* Implementation stub */ }


    private fun getSampleNotes(): List<Note> {
        return listOf(
            Note(
                title = "to-do list",
                content = "Buy groceries, call dentist.",
                lastModifiedTime = getOffsetDate(-10, Calendar.MINUTE)
            ),
            Note(
                title = "don't forget",
                content = "Pay bills by Friday.",
                lastModifiedTime = getOffsetDate(-30, Calendar.MINUTE),
                isPinned = true
            ),
            Note(
                title = "office adress",
                content = "123 Main St, Suite 400",
                lastModifiedTime = getOffsetDate(-1, Calendar.HOUR_OF_DAY)
            ),
        ).sortedWith(compareByDescending<Note> { it.isPinned }.thenByDescending { it.lastModifiedTime })
    }
}