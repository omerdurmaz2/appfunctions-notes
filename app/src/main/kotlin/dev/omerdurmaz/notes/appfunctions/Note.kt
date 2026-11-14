package dev.omerdurmaz.notes.appfunctions

import androidx.appfunctions.AppFunctionSerializable

/** A note from the user's notes app. */
@AppFunctionSerializable(isDescribedByKDoc = true)
data class Note(
    /** Stable ID of the note. Pass it to functions that take a noteId. */
    val noteId: String,
    /** Title shown in the note list, e.g. "Groceries". */
    val title: String,
    /** Full text of the note. */
    val content: String,
)
