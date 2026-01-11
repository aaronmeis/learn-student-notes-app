package com.studentnotes.data.repository

import com.studentnotes.data.local.Flashcard
import com.studentnotes.data.local.FlashcardDao
import kotlinx.coroutines.flow.Flow

class FlashcardRepository(private val flashcardDao: FlashcardDao) {

    fun getFlashcardsByNoteId(noteId: Long): Flow<List<Flashcard>> =
        flashcardDao.getFlashcardsByNoteId(noteId)

    fun getAllFlashcards(): Flow<List<Flashcard>> = flashcardDao.getAllFlashcards()

    suspend fun insertFlashcard(flashcard: Flashcard): Long =
        flashcardDao.insertFlashcard(flashcard)

    suspend fun insertFlashcards(flashcards: List<Flashcard>) =
        flashcardDao.insertFlashcards(flashcards)

    suspend fun deleteFlashcard(flashcard: Flashcard) =
        flashcardDao.deleteFlashcard(flashcard)

    suspend fun deleteFlashcardsByNoteId(noteId: Long) =
        flashcardDao.deleteFlashcardsByNoteId(noteId)
}
