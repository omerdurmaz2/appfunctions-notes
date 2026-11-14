package dev.omerdurmaz.notes.appfunctions

import android.content.Context
import android.os.Build
import androidx.appfunctions.AppFunctionManager
import androidx.appfunctions.AppFunctionName

/** Turns createNote off and on at runtime, e.g. from a toggle or Remote Config. */
suspend fun setCreateNoteEnabled(context: Context, enabled: Boolean) {
    if (Build.VERSION.SDK_INT < 36) return
    AppFunctionManager.getInstance(context)?.setAppFunctionEnabled(
        NotesAppFunctionService.FUNCTION_ID_CREATE_NOTE,
        if (enabled) {
            AppFunctionManager.APP_FUNCTION_STATE_DEFAULT
        } else {
            AppFunctionManager.APP_FUNCTION_STATE_DISABLED
        },
    )
}

suspend fun isCreateNoteEnabled(context: Context): Boolean {
    if (Build.VERSION.SDK_INT < 36) return false
    val manager = AppFunctionManager.getInstance(context) ?: return false
    val name = AppFunctionName(
        context.packageName,
        NotesAppFunctionService.FUNCTION_ID_CREATE_NOTE,
    )
    return manager.getAppFunctionStates(listOf(name)).single().isEnabled
}
