package com.studentnotes.data.local

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map

class FakeNoteDao : NoteDao {

    private val notes = mutableListOf<Note>()
    private val notesFlow = MutableStateFlow<List<Note>>(emptyList())
    private var nextId = 1L

    override fun getAllNotes(): Flow<List<Note>> {
        return notesFlow.map { it.sortedByDescending { note -> note.updatedAt } }
    }

    override fun getNoteById(id: Long): Flow<Note?> {
        return notesFlow.map { list -> list.find { it.id == id } }
    }

    override suspend fun getNoteByIdOnce(id: Long): Note? {
        return notes.find { it.id == id }
    }

    override suspend fun insertNote(note: Note): Long {
        val id = if (note.id == 0L) nextId++ else note.id
        val noteWithId = note.copy(id = id)
        notes.removeIf { it.id == id }
        notes.add(noteWithId)
        notesFlow.value = notes.toList()
        return id
    }

    override suspend fun updateNote(note: Note) {
        val index = notes.indexOfFirst { it.id == note.id }
        if (index >= 0) {
            notes[index] = note
            notesFlow.value = notes.toList()
        }
    }

    override suspend fun deleteNote(note: Note) {
        notes.removeIf { it.id == note.id }
        notesFlow.value = notes.toList()
    }

    override suspend fun deleteNoteById(id: Long) {
        notes.removeIf { it.id == id }
        notesFlow.value = notes.toList()
    }

    override fun searchNotes(query: String): Flow<List<Note>> {
        return notesFlow.map { list ->
            list.filter {
                it.title.contains(query, ignoreCase = true) ||
                    it.content.contains(query, ignoreCase = true)
            }.sortedByDescending { it.updatedAt }
        }
    }

    fun clear() {
        notes.clear()
        notesFlow.value = emptyList()
        nextId = 1L
    }
}
