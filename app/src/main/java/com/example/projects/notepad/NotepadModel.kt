package com.example.projects.notepad

import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.sp
import java.util.Date
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.UUID

/**
 * Represents a single note entry.
 */
data class Note(
    val id: String = UUID.randomUUID().toString(),
    var title: String = "",       // Ensure default value
    var content: String = "",     // Ensure default value
    val creationTime: Date = Date(),
    var lastModifiedTime: Date = Date(),
    var isPinned: Boolean = false
) {
    private val formatter = SimpleDateFormat("MM/dd/yy HH:mm", Locale.getDefault())
    val formattedDate: String
        get() = formatter.format(lastModifiedTime)
}

/**
 * Represents the overall state of the Notepad feature.
 */
data class NotepadState(
    val notes: List<Note> = emptyList(),
    val selectedNoteId: String? = null,
    val currentNoteTitle: String = "",
    val currentNoteContent: String = "",
    val currentNoteFontSize: TextUnit = 16.sp,
    val currentNoteIsBold: Boolean = false,
    val currentNoteIsItalic: Boolean = false,
) {
    val currentNote: Note?
        get() = notes.firstOrNull { it.id == selectedNoteId }
}