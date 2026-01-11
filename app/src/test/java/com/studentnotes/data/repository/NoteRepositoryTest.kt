package com.studentnotes.data.repository

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import app.cash.turbine.test
import com.studentnotes.data.local.Note
import com.studentnotes.data.local.NoteDao
import com.studentnotes.data.local.StudentNotesDatabase
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28], manifest = Config.NONE)
class NoteRepositoryTest {

    private lateinit var database: StudentNotesDatabase
    private lateinit var noteDao: NoteDao
    private lateinit var repository: NoteRepository

    @Before
    fun setup() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        database = Room.inMemoryDatabaseBuilder(context, StudentNotesDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        noteDao = database.noteDao()
        repository = NoteRepository(noteDao)
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun `insertNote and getAllNotes returns inserted notes`() = runTest {
        val note1 = Note(title = "Note 1", content = "Content 1")
        val note2 = Note(title = "Note 2", content = "Content 2")

        repository.insertNote(note1)
        repository.insertNote(note2)

        repository.getAllNotes().test {
            val notes = awaitItem()
            assertEquals(2, notes.size)
            assertTrue(notes.any { it.title == "Note 1" })
            assertTrue(notes.any { it.title == "Note 2" })
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `getAllNotes returns notes sorted by updatedAt descending`() = runTest {
        val oldNote = Note(title = "Old Note", content = "Old", updatedAt = 1000L)
        val newNote = Note(title = "New Note", content = "New", updatedAt = 2000L)

        repository.insertNote(oldNote)
        repository.insertNote(newNote)

        repository.getAllNotes().test {
            val notes = awaitItem()
            assertEquals(2, notes.size)
            assertEquals("New Note", notes[0].title)
            assertEquals("Old Note", notes[1].title)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `getNoteById returns correct note`() = runTest {
        val note = Note(title = "Test Note", content = "Test Content")
        val insertedId = repository.insertNote(note)

        repository.getNoteById(insertedId).test {
            val foundNote = awaitItem()
            assertNotNull(foundNote)
            assertEquals("Test Note", foundNote?.title)
            assertEquals("Test Content", foundNote?.content)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `getNoteById returns null for non-existent id`() = runTest {
        repository.getNoteById(999L).test {
            val foundNote = awaitItem()
            assertNull(foundNote)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `getNoteByIdOnce returns correct note`() = runTest {
        val note = Note(title = "Sync Note", content = "Sync Content")
        val insertedId = repository.insertNote(note)

        val foundNote = repository.getNoteByIdOnce(insertedId)

        assertNotNull(foundNote)
        assertEquals("Sync Note", foundNote?.title)
    }

    @Test
    fun `updateNote updates existing note`() = runTest {
        val note = Note(title = "Original", content = "Original Content")
        val insertedId = repository.insertNote(note)

        val updatedNote = Note(
            id = insertedId,
            title = "Updated",
            content = "Updated Content",
            updatedAt = System.currentTimeMillis()
        )
        repository.updateNote(updatedNote)

        val foundNote = repository.getNoteByIdOnce(insertedId)
        assertNotNull(foundNote)
        assertEquals("Updated", foundNote?.title)
        assertEquals("Updated Content", foundNote?.content)
    }

    @Test
    fun `deleteNote removes note from database`() = runTest {
        val note = Note(title = "To Delete", content = "Content")
        val insertedId = repository.insertNote(note)

        val insertedNote = repository.getNoteByIdOnce(insertedId)
        assertNotNull(insertedNote)

        repository.deleteNote(insertedNote!!)

        val deletedNote = repository.getNoteByIdOnce(insertedId)
        assertNull(deletedNote)
    }

    @Test
    fun `deleteNoteById removes note from database`() = runTest {
        val note = Note(title = "To Delete By Id", content = "Content")
        val insertedId = repository.insertNote(note)

        repository.deleteNoteById(insertedId)

        val deletedNote = repository.getNoteByIdOnce(insertedId)
        assertNull(deletedNote)
    }

    @Test
    fun `searchNotes finds notes by title`() = runTest {
        repository.insertNote(Note(title = "Biology Notes", content = "Cells and DNA"))
        repository.insertNote(Note(title = "Math Notes", content = "Algebra and calculus"))
        repository.insertNote(Note(title = "History", content = "World War II"))

        repository.searchNotes("Notes").test {
            val results = awaitItem()
            assertEquals(2, results.size)
            assertTrue(results.all { it.title.contains("Notes") })
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `searchNotes finds notes by content`() = runTest {
        repository.insertNote(Note(title = "Science", content = "Biology and chemistry"))
        repository.insertNote(Note(title = "Math", content = "Numbers and equations"))
        repository.insertNote(Note(title = "Bio Class", content = "Cell division"))

        repository.searchNotes("biology").test {
            val results = awaitItem()
            assertEquals(1, results.size)
            assertEquals("Science", results[0].title)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `searchNotes is case insensitive`() = runTest {
        repository.insertNote(Note(title = "KOTLIN", content = "Programming"))
        repository.insertNote(Note(title = "kotlin basics", content = "Learning"))
        repository.insertNote(Note(title = "Java", content = "Also kotlin compatible"))

        repository.searchNotes("Kotlin").test {
            val results = awaitItem()
            assertEquals(3, results.size)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `insertNote with summary persists summary`() = runTest {
        val note = Note(
            title = "Summarized Note",
            content = "Long content here",
            summary = "This is the summary"
        )
        val insertedId = repository.insertNote(note)

        val foundNote = repository.getNoteByIdOnce(insertedId)
        assertNotNull(foundNote)
        assertEquals("This is the summary", foundNote?.summary)
    }

    @Test
    fun `getAllNotes flow updates when notes change`() = runTest {
        repository.getAllNotes().test {
            // Initially empty
            assertEquals(0, awaitItem().size)

            // Insert a note
            val note1 = Note(title = "Note 1", content = "Content 1")
            repository.insertNote(note1)
            assertEquals(1, awaitItem().size)

            // Insert another note
            val note2 = Note(title = "Note 2", content = "Content 2")
            repository.insertNote(note2)
            assertEquals(2, awaitItem().size)

            cancelAndIgnoreRemainingEvents()
        }
    }
}
