package dev.omerdurmaz.notes

import androidx.appfunctions.AppFunctionData
import androidx.appfunctions.AppFunctionInvalidArgumentException
import androidx.appfunctions.AppFunctionManager
import androidx.appfunctions.AppFunctionSearchSpec
import androidx.appfunctions.ExecuteAppFunctionRequest
import androidx.appfunctions.ExecuteAppFunctionResponse
import androidx.appfunctions.ExecuteAppFunctionResponse.Success
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.SdkSuppress
import androidx.test.platform.app.InstrumentationRegistry
import dev.omerdurmaz.notes.appfunctions.Note
import dev.omerdurmaz.notes.appfunctions.NotesAppFunctionService
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@SdkSuppress(minSdkVersion = 36)
@RunWith(AndroidJUnit4::class)
class NotesAppFunctionsTest {
    private val context = InstrumentationRegistry.getInstrumentation().targetContext
    private val manager = checkNotNull(AppFunctionManager.getInstance(context))

    private val ownFunctions =
        AppFunctionSearchSpec(packageNames = setOf(context.packageName))

    private suspend fun call(
        functionId: String,
        params: AppFunctionData.Builder.() -> Unit,
    ): ExecuteAppFunctionResponse {
        val metadata = manager
            .searchAppFunctions(ownFunctions)
            .single { it.id == functionId }
        val data = AppFunctionData.Builder(metadata.parameters, metadata.components)
            .apply(params)
            .build()
        return manager.executeAppFunction(
            ExecuteAppFunctionRequest(context.packageName, functionId, data),
        )
    }

    private fun ExecuteAppFunctionResponse.note(): Note =
        (this as Success).returnValue
            .getAppFunctionData(Success.PROPERTY_RETURN_VALUE)!!
            .deserialize(Note::class.java)

    @Test
    fun createNote_retryDoesNotDuplicate() = runBlocking {
        val create: AppFunctionData.Builder.() -> Unit = {
            setString("title", "Groceries")
            setString("content", "Milk, eggs, coffee")
        }
        val first = call(NotesAppFunctionService.FUNCTION_ID_CREATE_NOTE, create).note()
        val retry = call(NotesAppFunctionService.FUNCTION_ID_CREATE_NOTE, create).note()
        assertEquals(first.noteId, retry.noteId)
    }

    @Test
    fun appMetadata_instructionsReachTheCaller() = runBlocking {
        val packageMetadata = manager
            .searchAppFunctions(ownFunctions)
            .first()
            .packageMetadata
        val appMetadata = checkNotNull(packageMetadata)
            .resolveAppFunctionAppMetadata(context)
        assertTrue(checkNotNull(appMetadata).instructions.startsWith("Personal notes app."))
    }

    @Test
    fun searchNotes_blankQueryTellsTheCallerWhatToDo() = runBlocking {
        val response = call(NotesAppFunctionService.FUNCTION_ID_SEARCH_NOTES) {
            setString("query", " ")
        }
        val error = (response as ExecuteAppFunctionResponse.Error).error
        assertTrue(error is AppFunctionInvalidArgumentException)
        assertTrue(error.errorMessage.orEmpty().contains("dentist"))
    }
}
