package com.studentnotes

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.ui.Modifier
import com.studentnotes.ui.theme.StudentNotesTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            StudentNotesTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    StudentNotesApp(
                        modifier = Modifier.padding(innerPadding)
                    )
                }
            }
        }
    }
}
