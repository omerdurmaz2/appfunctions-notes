package dev.omerdurmaz.notes

import android.app.Application
import androidx.room.Room
import dev.omerdurmaz.notes.data.NoteDao
import dev.omerdurmaz.notes.data.NotesDatabase

class NotesApp : Application() {

    private val database by lazy {
        Room.databaseBuilder(this, NotesDatabase::class.java, "notes.db").build()
    }

    val notes: NoteDao get() = database.notes()
}
