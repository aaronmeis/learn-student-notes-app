package com.studentnotes

import android.app.Application
import com.studentnotes.data.local.StudentNotesDatabase

class StudentNotesApplication : Application() {

    val database: StudentNotesDatabase by lazy {
        StudentNotesDatabase.getDatabase(this)
    }
}
