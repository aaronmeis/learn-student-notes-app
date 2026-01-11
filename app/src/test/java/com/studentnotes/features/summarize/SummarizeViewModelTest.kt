package com.studentnotes.features.summarize

import app.cash.turbine.test
import com.studentnotes.data.local.FakeNoteDao
import com.studentnotes.inference.FakeInferenceClient
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class SummarizeViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var fakeNoteDao: FakeNoteDao
    private lateinit var fakeInferenceClient: FakeInferenceClient
    private lateinit var viewModel: SummarizeViewModel

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        fakeNoteDao = FakeNoteDao()
        fakeInferenceClient = FakeInferenceClient()
        viewModel = SummarizeViewModel(fakeNoteDao, fakeInferenceClient)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `initial state is empty`() = runTest {
        viewModel.uiState.test {
            val state = awaitItem()
            assertEquals("", state.title)
            assertEquals("", state.noteText)
            assertEquals("", state.summary)
            assertFalse(state.isLoading)
            assertFalse(state.isSaved)
            assertNull(state.error)
        }
    }

    @Test
    fun `updateTitle updates title in state`() = runTest {
        viewModel.uiState.test {
            awaitItem() // initial state

            viewModel.updateTitle("My Note Title")
            val state = awaitItem()

            assertEquals("My Note Title", state.title)
        }
    }

    @Test
    fun `updateNoteText updates noteText in state`() = runTest {
        viewModel.uiState.test {
            awaitItem() // initial state

            viewModel.updateNoteText("This is my note content.")
            val state = awaitItem()

            assertEquals("This is my note content.", state.noteText)
        }
    }

    @Test
    fun `summarize with empty text shows error`() = runTest {
        viewModel.uiState.test {
            awaitItem() // initial state

            viewModel.summarize()
            val state = awaitItem()

            assertEquals("Please enter some text to summarize", state.error)
            assertFalse(state.isLoading)
            assertEquals(0, fakeInferenceClient.summarizeCallCount)
        }
    }

    @Test
    fun `summarize shows loading state then success`() = runTest {
        fakeInferenceClient.summarizeResponse = "This is the AI summary."

        viewModel.uiState.test {
            awaitItem() // initial state

            viewModel.updateNoteText("Some note content to summarize.")
            awaitItem() // text updated

            viewModel.summarize()

            // Loading state
            val loadingState = awaitItem()
            assertTrue(loadingState.isLoading)
            assertNull(loadingState.error)

            // Advance coroutine
            testDispatcher.scheduler.advanceUntilIdle()

            // Success state
            val successState = awaitItem()
            assertFalse(successState.isLoading)
            assertEquals("This is the AI summary.", successState.summary)
            assertNull(successState.error)
            assertEquals(1, fakeInferenceClient.summarizeCallCount)
            assertEquals("Some note content to summarize.", fakeInferenceClient.lastSummarizeInput)
        }
    }

    @Test
    fun `summarize shows error on failure`() = runTest {
        fakeInferenceClient.shouldThrowError = true
        fakeInferenceClient.errorMessage = "Network connection failed"

        viewModel.uiState.test {
            awaitItem() // initial state

            viewModel.updateNoteText("Some content")
            awaitItem() // text updated

            viewModel.summarize()

            // Loading state
            val loadingState = awaitItem()
            assertTrue(loadingState.isLoading)

            // Advance coroutine
            testDispatcher.scheduler.advanceUntilIdle()

            // Error state
            val errorState = awaitItem()
            assertFalse(errorState.isLoading)
            assertEquals("Network connection failed", errorState.error)
            assertEquals("", errorState.summary)
        }
    }

    @Test
    fun `saveNote with empty content shows error`() = runTest {
        var savedCalled = false

        viewModel.uiState.test {
            awaitItem() // initial state

            viewModel.saveNote { savedCalled = true }
            val state = awaitItem()

            assertEquals("Cannot save empty note", state.error)
            assertFalse(savedCalled)
        }
    }

    @Test
    fun `saveNote saves note and calls callback`() = runTest {
        var savedCalled = false

        viewModel.uiState.test {
            awaitItem() // initial state

            viewModel.updateTitle("Test Title")
            awaitItem()

            viewModel.updateNoteText("Test content for saving.")
            awaitItem()

            viewModel.saveNote { savedCalled = true }

            // Advance coroutine
            testDispatcher.scheduler.advanceUntilIdle()

            val savedState = awaitItem()
            assertTrue(savedState.isSaved)
            assertTrue(savedCalled)
        }

        // Verify note was saved to DAO
        fakeNoteDao.getAllNotes().test {
            val notes = awaitItem()
            assertEquals(1, notes.size)
            assertEquals("Test Title", notes[0].title)
            assertEquals("Test content for saving.", notes[0].content)
        }
    }

    @Test
    fun `saveNote uses truncated content as title when title is blank`() = runTest {
        viewModel.uiState.test {
            awaitItem() // initial state

            viewModel.updateNoteText("This is a very long note content that should be truncated for the title.")
            awaitItem()

            viewModel.saveNote { }

            testDispatcher.scheduler.advanceUntilIdle()
            awaitItem() // saved state
        }

        fakeNoteDao.getAllNotes().test {
            val notes = awaitItem()
            assertEquals(1, notes.size)
            assertTrue(notes[0].title.length <= 53) // 50 chars + "..."
            assertTrue(notes[0].title.endsWith("..."))
        }
    }

    @Test
    fun `saveNote includes summary when available`() = runTest {
        fakeInferenceClient.summarizeResponse = "Generated summary"

        viewModel.uiState.test {
            awaitItem() // initial state

            viewModel.updateTitle("Note with Summary")
            awaitItem()

            viewModel.updateNoteText("Content to summarize")
            awaitItem()

            viewModel.summarize()
            awaitItem() // loading
            testDispatcher.scheduler.advanceUntilIdle()
            awaitItem() // summary received

            viewModel.saveNote { }
            testDispatcher.scheduler.advanceUntilIdle()
            awaitItem() // saved
        }

        fakeNoteDao.getAllNotes().test {
            val notes = awaitItem()
            assertEquals(1, notes.size)
            assertEquals("Generated summary", notes[0].summary)
        }
    }

    @Test
    fun `clearAll resets state to initial`() = runTest {
        viewModel.uiState.test {
            awaitItem() // initial state

            viewModel.updateTitle("Title")
            awaitItem()

            viewModel.updateNoteText("Content")
            awaitItem()

            viewModel.clearAll()
            val clearedState = awaitItem()

            assertEquals("", clearedState.title)
            assertEquals("", clearedState.noteText)
            assertEquals("", clearedState.summary)
            assertFalse(clearedState.isLoading)
            assertFalse(clearedState.isSaved)
            assertNull(clearedState.error)
        }
    }
}
