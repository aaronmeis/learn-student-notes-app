package com.studentnotes

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.studentnotes.data.local.StudentNotesDatabase
import com.studentnotes.features.noteslist.NotesListScreen
import com.studentnotes.features.noteslist.NotesListViewModel
import com.studentnotes.features.summarize.SummarizeScreen
import com.studentnotes.features.summarize.SummarizeViewModel
import com.studentnotes.inference.OllamaClient

sealed class Screen(val route: String) {
    data object NotesList : Screen("notes_list")
    data object NewNote : Screen("new_note")
    data object NoteDetail : Screen("note/{noteId}") {
        fun createRoute(noteId: Long) = "note/$noteId"
    }
    data object Flashcards : Screen("flashcards")
    data object QA : Screen("qa")
}

@Composable
fun StudentNotesApp(modifier: Modifier = Modifier) {
    val navController = rememberNavController()
    val context = LocalContext.current

    // Get database and DAO
    val database = remember { StudentNotesDatabase.getDatabase(context) }
    val noteDao = remember { database.noteDao() }

    // Create OllamaClient instance
    val ollamaClient = remember { OllamaClient() }

    NavHost(
        navController = navController,
        startDestination = Screen.NotesList.route,
        modifier = modifier.fillMaxSize()
    ) {
        composable(Screen.NotesList.route) {
            val viewModel: NotesListViewModel = viewModel(
                factory = NotesListViewModel.factory(noteDao)
            )
            NotesListScreen(
                viewModel = viewModel,
                onAddNote = { navController.navigate(Screen.NewNote.route) },
                onNoteClick = { noteId ->
                    navController.navigate(Screen.NoteDetail.createRoute(noteId))
                }
            )
        }

        composable(Screen.NewNote.route) {
            val viewModel: SummarizeViewModel = viewModel(
                factory = SummarizeViewModel.factory(noteDao, ollamaClient)
            )
            SummarizeScreen(
                viewModel = viewModel,
                onNavigateBack = { navController.popBackStack() }
            )
        }

        composable(Screen.NoteDetail.route) { backStackEntry ->
            val noteId = backStackEntry.arguments?.getString("noteId")?.toLongOrNull()
            // TODO: Implement note detail/edit screen
            Text("Note Detail: $noteId")
        }

        composable(Screen.Flashcards.route) {
            // Placeholder - will be replaced with FlashcardsScreen
            Text("Flashcards screen placeholder")
        }

        composable(Screen.QA.route) {
            // Placeholder - will be replaced with QAScreen
            Text("Q&A screen placeholder")
        }
    }
}
