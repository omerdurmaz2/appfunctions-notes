package dev.omerdurmaz.notes.appfunctions

import androidx.annotation.RequiresApi
import androidx.appfunctions.AppFunctionAccessLevel
import androidx.appfunctions.AppFunctionDeclaration
import androidx.appfunctions.AppFunctionElementNotFoundException
import androidx.appfunctions.AppFunctionInvalidArgumentException
import androidx.appfunctions.AppFunctionService
import androidx.appfunctions.AppFunctionServiceEntryPoint
import androidx.appfunctions.ExperimentalAppFunctionsApi
import androidx.appfunctions.metadata.AppFunctionMetadata
import dev.omerdurmaz.notes.NotesApp
import dev.omerdurmaz.notes.data.NoteEntity

@RequiresApi(36)
@AppFunctionServiceEntryPoint(
    serviceName = "NotesAppFunctionService",
    appFunctionXmlFileName = "notes_app_functions",
)
abstract class BaseNotesAppFunctionService : AppFunctionService() {

    // Room's suspend DAO methods move off the main thread on their own.
    private val notes by lazy { (application as NotesApp).notes }

    /**
     * Create a new note in the user's notes.
     * Safe to retry: if a note with the same title and content already exists,
     * that note is returned and no duplicate is created.
     *
     * @param title Short title, 1 to 100 characters, e.g. "Groceries".
     * @param content Body text of the note, e.g. "Milk, eggs, coffee".
     * @return The new note, or the identical note that already existed.
     */
    @AppFunctionDeclaration(isDescribedByKDoc = true)
    suspend fun createNote(title: String, content: String): Note {
        val cleanTitle = title.trim()
        if (cleanTitle.isEmpty() || cleanTitle.length > 100) {
            throw AppFunctionInvalidArgumentException(
                "title must be 1 to 100 characters, got ${cleanTitle.length}. " +
                    "Ask the user for a shorter title.",
            )
        }
        return notes.createIfAbsent(cleanTitle, content).toNote()
    }

    /**
     * Search the user's notes for a word or phrase in the title or body. Read-only.
     * Call this first to get the noteId that other functions need.
     *
     * @param query Text to look for, e.g. "dentist" or "Izmir".
     * @param maxResults How many notes to return, 1 to 20. 0 means the default, 10.
     * @return Matching notes, newest first. Empty if nothing matches.
     */
    @AppFunctionDeclaration(isDescribedByKDoc = true)
    suspend fun searchNotes(query: String, maxResults: Int = 0): List<Note> {
        if (query.isBlank()) {
            throw AppFunctionInvalidArgumentException(
                "query is empty. Pass a word from the note, e.g. \"dentist\".",
            )
        }
        val limit = if (maxResults == 0) 10 else maxResults
        if (limit !in 1..20) {
            throw AppFunctionInvalidArgumentException(
                "maxResults must be 1 to 20, got $maxResults.",
            )
        }
        return notes.search(query.trim(), limit).map { it.toNote() }
    }

    /**
     * Move a note to the trash. The user can restore it from the trash in the app.
     * Safe to retry: moving a note that is already in the trash returns it again.
     *
     * @param noteId The noteId of a note returned by searchNotes.
     * @return The note that is now in the trash.
     */
    @AppFunctionDeclaration(isDescribedByKDoc = true)
    suspend fun moveNoteToTrash(noteId: String): Note =
        notes.moveToTrash(noteId)?.toNote()
            ?: throw AppFunctionElementNotFoundException(
                "No note with noteId $noteId. Call searchNotes and use a noteId from its results.",
            )

    /**
     * Permanently delete every note in the trash. This can't be undone.
     *
     * @return How many notes were deleted.
     */
    @OptIn(ExperimentalAppFunctionsApi::class)
    @AppFunctionAccessLevel(
        level = AppFunctionMetadata.ACCESS_LEVEL_SELF,
        isCompatEnforcementEnabled = true,
    )
    @AppFunctionDeclaration(isDescribedByKDoc = true)
    suspend fun emptyTrash(): Int = notes.emptyTrash()

    private fun NoteEntity.toNote() = Note(noteId = id, title = title, content = body)
}
