package com.example.projects.notepad

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.*
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.projects.ui.theme.CalculatorTheme

// --- Nested Routes for Notepad Feature ---
object NotepadRoutes {
    const val NOTE_LIST = "note_list"
    const val NOTE_DETAIL = "note_detail"
}

// ----------------------------------------------------------------------
// --- Main Composablesss (Source of ViewModel) ---
// ----------------------------------------------------------------------
@Composable
fun NotepadScreen(
    viewModel: NotepadViewModel = viewModel() // ViewModel is initialized ONCE here
) {
    val state by viewModel.state.collectAsState()
    val notepadNavController = rememberNavController()

    CalculatorTheme {
        NavHost(
            navController = notepadNavController,
            startDestination = NotepadRoutes.NOTE_LIST
        ) {
            composable(NotepadRoutes.NOTE_LIST) {
                NoteListScreen(
                    state = state,
                    onNoteClick = { noteId ->
                        viewModel.selectNote(noteId)
                        notepadNavController.navigate(NotepadRoutes.NOTE_DETAIL)
                    },
                    onAddNewNoteClick = {
                        viewModel.addNewNote()
                        notepadNavController.navigate(NotepadRoutes.NOTE_DETAIL)
                    },
                    onTogglePin = viewModel::togglePinNote
                )
            }
            composable(NotepadRoutes.NOTE_DETAIL) {
                state.selectedNoteId?.let { noteId ->
                    // CRITICAL FIX: Use key to stabilize navigation to detail screen
                    key(noteId) {
                        NoteDetailScreen(
                            state = state,
                            viewModel = viewModel,
                            onBackClick = {
                                // CRITICAL FIX: Ensure save/cleanup happens safely before navigation
                                viewModel.clearSelectedNote()
                                notepadNavController.popBackStack()
                            },
                            onDeleteClick = {
                                viewModel.deleteNote(noteId)
                                notepadNavController.popBackStack()
                            }
                        )
                    }
                }
                // FALLBACK: If selectedNoteId is null here (due to a bad state/crash), immediately navigate back.
                if (state.selectedNoteId == null) {
                    notepadNavController.popBackStack(NotepadRoutes.NOTE_LIST, inclusive = true)
                }
            }
        }
    }
}


// ----------------------------------------------------------------------
// --- 1. Note List UI (Cleaned) ---
// ----------------------------------------------------------------------
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NoteListScreen(
    state: NotepadState,
    onNoteClick: (String) -> Unit,
    onAddNewNoteClick: () -> Unit,
    onTogglePin: (String) -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Notes", fontWeight = FontWeight.SemiBold) },
                actions = {
                    IconButton(onClick = onAddNewNoteClick) {
                        Icon(Icons.AutoMirrored.Filled.NoteAdd, contentDescription = "New Note")
                    }
                    IconButton(onClick = { /* Handle search action */ }) {
                        Icon(Icons.Filled.Search, contentDescription = "Search")
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = onAddNewNoteClick) {
                Icon(Icons.Filled.Add, contentDescription = "Add new note")
            }
        },
        containerColor = MaterialTheme.colorScheme.surface
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(state.notes, key = { it.id }) { note ->
                NoteListItem(
                    note = note,
                    onClick = { onNoteClick(note.id) },
                    onTogglePin = { onTogglePin(note.id) }
                )
            }
            item { Spacer(modifier = Modifier.height(60.dp)) }
        }
    }
}


@Composable
fun NoteListItem(
    note: Note,
    onClick: () -> Unit,
    onTogglePin: () -> Unit
) {
    ElevatedCard(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        elevation = CardDefaults.elevatedCardElevation(2.dp),
        colors = CardDefaults.elevatedCardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = note.title.ifEmpty { "Untitled" },
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = note.content.ifEmpty { "Empty note" },
                    style = MaterialTheme.typography.bodyMedium,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 4.dp)
                )
                Text(
                    text = note.formattedDate,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.outline,
                    modifier = Modifier.padding(top = 8.dp)
                )
            }
            IconButton(onClick = onTogglePin) {
                Icon(
                    imageVector = if (note.isPinned) Icons.Filled.Star else Icons.Filled.StarBorder,
                    contentDescription = if (note.isPinned) "Unpin Note" else "Pin Note",
                    tint = if (note.isPinned) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}


// ----------------------------------------------------------------------
// --- 2. Note Detail UI (CRASH-PROOFED) ---
// ----------------------------------------------------------------------
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NoteDetailScreen(
    state: NotepadState,
    viewModel: NotepadViewModel,
    onBackClick: () -> Unit,
    onDeleteClick: () -> Unit
) {
    // CRITICAL CRASH FIX: Guard against null data during navigation
    val currentNote = state.currentNote
    if (currentNote == null) {
        onBackClick() // Call back to exit gracefully if state is invalid
        return
    }

    Scaffold(
        topBar = {
            Column {
                // Primary Action Bar
                TopAppBar(
                    title = { /* Empty Title */ },
                    navigationIcon = {
                        IconButton(onClick = onBackClick) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                        }
                    },
                    actions = {
                        // Save (Activity 2 requirement)
                        IconButton(onClick = viewModel::saveNote) {
                            Icon(Icons.Filled.Save, contentDescription = "Save")
                        }
                        // Undo/Redo (Document History)
                        IconButton(onClick = viewModel::undo) {
                            Icon(Icons.AutoMirrored.Filled.Undo, contentDescription = "Undo")
                        }
                        IconButton(onClick = viewModel::redo) {
                            Icon(Icons.AutoMirrored.Filled.Redo, contentDescription = "Redo")
                        }
                        // Pin (Metadata Action)
                        IconButton(onClick = { viewModel.togglePinNote(currentNote.id) }) {
                            Icon(
                                imageVector = Icons.Filled.PushPin,
                                contentDescription = if (currentNote.isPinned) "Unpin" else "Pin",
                                tint = if (currentNote.isPinned) MaterialTheme.colorScheme.primary else LocalContentColor.current.copy(alpha = 0.6f)
                            )
                        }
                        // Delete (Activity 2 requirement)
                        IconButton(onClick = onDeleteClick) {
                            Icon(Icons.Filled.Delete, contentDescription = "Delete Note")
                        }
                    }
                )

                // Title Area
                Box(modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
                ) {
                    BasicTextField(
                        value = state.currentNoteTitle,
                        onValueChange = viewModel::onNoteTitleChanged,
                        textStyle = MaterialTheme.typography.headlineSmall.copy(
                            color = MaterialTheme.colorScheme.onSurface
                        ),
                        singleLine = true,
                        cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                        decorationBox = { innerTextField ->
                            if (state.currentNoteTitle.isEmpty()) {
                                Text(
                                    text = "Title",
                                    style = MaterialTheme.typography.headlineSmall,
                                    color = MaterialTheme.colorScheme.outline
                                )
                            }
                            innerTextField()
                        },
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                // Secondary Controls: Style, Copy/Paste, and Size
                NotepadControls(
                    state = state,
                    viewModel = viewModel,
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(MaterialTheme.colorScheme.surfaceContainer)
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                )
            }
        },
        containerColor = MaterialTheme.colorScheme.surface
    ) { paddingValues ->
        // --- Note Content Input Area ---
        BasicTextField(
            value = state.currentNoteContent,
            onValueChange = viewModel::onNoteContentChanged,
            // Apply dynamic style and size
            textStyle = TextStyle(
                color = MaterialTheme.colorScheme.onSurface,
                fontSize = state.currentNoteFontSize,
                fontWeight = if (state.currentNoteIsBold) FontWeight.Bold else FontWeight.Normal,
                fontStyle = if (state.currentNoteIsItalic) FontStyle.Italic else FontStyle.Normal
            ),
            cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
            decorationBox = { innerTextField ->
                if (state.currentNoteContent.isEmpty()) {
                    Text(
                        text = "Start writing here...",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.outline
                    )
                }
                innerTextField()
            },
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 16.dp, vertical = 8.dp)
        )
    }
}

// --- Notepad Controls ---
@Composable
private fun NotepadControls(
    state: NotepadState,
    viewModel: NotepadViewModel,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.height(IntrinsicSize.Min),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        LazyRow(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            modifier = Modifier.weight(1f, fill = false)
        ) {
            // 1. Style Toggles (bold, italic, normal)











































































































































































































































































































            item {
                StyleToggleButton(
                    icon = Icons.Filled.FormatBold,
                    contentDescription = "Bold",
                    isSelected = state.currentNoteIsBold,
                    onClick = viewModel::toggleBold
                )
            }
            item {
                StyleToggleButton(
                    icon = Icons.Filled.FormatItalic,
                    contentDescription = "Italic",
                    isSelected = state.currentNoteIsItalic,
                    onClick = viewModel::toggleItalic
                )
            }
            item {
                TextButton(
                    onClick = viewModel::resetStyle,
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp)
                ) {
                    Text("Normal", style = MaterialTheme.typography.labelMedium)
                }
            }

            // 2. Separator
            item { CustomVerticalDivider(modifier = Modifier
                .padding(horizontal = 4.dp)
                .height(24.dp)) }

            // 3. Clipboard Actions (Cut, Copy, Paste)
            item {
                IconButton(onClick = viewModel::cut) {
                    Icon(Icons.Filled.ContentCut, contentDescription = "Cut")
                }
            }
            item {
                IconButton(onClick = viewModel::copy) {
                    Icon(Icons.Filled.ContentCopy, contentDescription = "Copy")
                }
            }
            item {
                IconButton(onClick = viewModel::paste) {
                    Icon(Icons.Filled.ContentPaste, contentDescription = "Paste")
                }
            }
        }

        // RIGHT SECTION: Font Size Controls (Change size requirement)
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.End
        ) {
            // Separator before Size Controls
            CustomVerticalDivider(modifier = Modifier
                .padding(horizontal = 8.dp)
                .fillMaxHeight(0.8f))

            IconButton(
                onClick = { viewModel.changeFontSize(false) },
                enabled = state.currentNoteFontSize.value > 12f,
                modifier = Modifier.size(36.dp)
            ) {
                Icon(Icons.Filled.Remove, contentDescription = "Decrease Font Size", modifier = Modifier.size(20.dp))
            }
            Text(
                text = "${state.currentNoteFontSize.value.toInt()}sp",
                style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold),
                modifier = Modifier.padding(horizontal = 4.dp)
            )
            IconButton(
                onClick = { viewModel.changeFontSize(true) },
                enabled = state.currentNoteFontSize.value < 30f,
                modifier = Modifier.size(36.dp)
            ) {
                Icon(Icons.Filled.Add, contentDescription = "Increase Font Size", modifier = Modifier.size(20.dp))
            }
        }
    }
}

@Composable
private fun StyleToggleButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    contentDescription: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    IconButton(
        onClick = onClick,
        colors = IconButtonDefaults.iconButtonColors(
            containerColor = if (isSelected) MaterialTheme.colorScheme.primaryContainer else Color.Transparent,
            contentColor = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant
        )
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
        )
    }
}

@Composable
fun CustomVerticalDivider(modifier: Modifier = Modifier, color: Color = MaterialTheme.colorScheme.outlineVariant) {
    // Replaced deprecated Divider with HorizontalDivider used vertically
    HorizontalDivider(
        color = color,
        modifier = modifier
            .width(1.dp)
            .fillMaxHeight()
    )
}