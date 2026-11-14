package dev.omerdurmaz.notes

import android.content.Context
import androidx.appfunctions.AppFunctionData
import androidx.appfunctions.AppFunctionInvalidArgumentException
import androidx.appfunctions.AppFunctionSearchSpec
import androidx.appfunctions.ExecuteAppFunctionRequest
import androidx.appfunctions.ExecuteAppFunctionResponse
import androidx.appfunctions.ExecuteAppFunctionResponse.Success
import androidx.appfunctions.testing.AppFunctionTestRule
import androidx.test.core.app.ApplicationProvider
import dev.omerdurmaz.notes.appfunctions.Note
import dev.omerdurmaz.notes.appfunctions.NotesAppFunctionService
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class NotesAppFunctionsJvmTest {
    private val context = ApplicationProvider.getApplicationContext<Context>()

    @get:Rule
    val appFunctions = AppFunctionTestRule(context)

    private val ownFunctions =
        AppFunctionSearchSpec(packageNames = setOf(context.packageName))

    private suspend fun call(
        functionId: String,
        params: AppFunctionData.Builder.() -> Unit,
    ): ExecuteAppFunctionResponse {
        val manager = appFunctions.getAppFunctionManager()
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
    fun searchNotes_blankQueryTellsTheCallerWhatToDo() = runBlocking {
        val response = call(NotesAppFunctionService.FUNCTION_ID_SEARCH_NOTES) {
            setString("query", " ")
        }
        val error = (response as ExecuteAppFunctionResponse.Error).error
        assertTrue(error is AppFunctionInvalidArgumentException)
        assertTrue(error.errorMessage.orEmpty().contains("dentist"))
    }

    @Test
    fun searchNotes_findsWhatCreateNoteSaved() = runBlocking {
        call(NotesAppFunctionService.FUNCTION_ID_CREATE_NOTE) {
            setString("title", "Dentist")
            setString("content", "Thursday 10:30, bring the X-ray")
        }
        // maxResults is left out on purpose: the function gets 0 and uses its default.
        val response = call(NotesAppFunctionService.FUNCTION_ID_SEARCH_NOTES) {
            setString("query", "x-ray")
        }
        val found = (response as Success).returnValue
            .getAppFunctionDataList(Success.PROPERTY_RETURN_VALUE)!!
            .map { it.deserialize(Note::class.java) }
        assertEquals(listOf("Dentist"), found.map { it.title })
    }
}
