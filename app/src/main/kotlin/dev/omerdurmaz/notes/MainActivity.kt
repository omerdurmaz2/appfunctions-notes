package dev.omerdurmaz.notes

import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Card
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.omerdurmaz.notes.appfunctions.isCreateNoteEnabled
import dev.omerdurmaz.notes.appfunctions.setCreateNoteEnabled
import dev.omerdurmaz.notes.data.NoteDao
import dev.omerdurmaz.notes.data.NoteEntity
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val notes = (application as NotesApp).notes
        setContent {
            val colors = if (isSystemInDarkTheme()) darkColorScheme() else lightColorScheme()
            MaterialTheme(colorScheme = colors) { NotesScreen(notes) }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun NotesScreen(dao: NoteDao) {
    val notes by remember { dao.observeNotes() }.collectAsStateWithLifecycle(emptyList())
    val trash by remember { dao.observeTrash() }.collectAsStateWithLifecycle(emptyList())
    val scope = rememberCoroutineScope()

    Scaffold(topBar = { TopAppBar(title = { Text(stringResource(R.string.app_name)) }) }) { padding ->
        LazyColumn(
            contentPadding = PaddingValues(16.dp, padding.calculateTopPadding() + 8.dp, 16.dp, 16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            if (Build.VERSION.SDK_INT >= 36) item { CreateNoteSwitch() }
            if (notes.isEmpty()) item { Text(stringResource(R.string.empty_hint)) }
            items(notes, key = { it.id }) { note ->
                NoteCard(note, stringResource(R.string.move_to_trash)) {
                    scope.launch { dao.moveToTrash(note.id) }
                }
            }
            if (trash.isNotEmpty()) {
                item {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            stringResource(R.string.trash),
                            style = MaterialTheme.typography.titleMedium,
                            modifier = Modifier.weight(1f),
                        )
                        TextButton(onClick = { scope.launch { dao.emptyTrash() } }) {
                            Text(stringResource(R.string.empty_trash))
                        }
                    }
                }
                items(trash, key = { it.id }) { note ->
                    NoteCard(note, stringResource(R.string.restore)) {
                        scope.launch { dao.restore(note.id) }
                    }
                }
            }
        }
    }
}

@Composable
private fun NoteCard(note: NoteEntity, actionLabel: String, onAction: () -> Unit) {
    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.fillMaxWidth().padding(16.dp)) {
            Text(note.title, style = MaterialTheme.typography.titleMedium)
            Text(note.body, style = MaterialTheme.typography.bodyMedium)
            TextButton(onClick = onAction, modifier = Modifier.align(Alignment.End)) {
                Text(actionLabel)
            }
        }
    }
}

/** The kill switch from the article, wired to a toggle so you can watch it work. */
@Composable
private fun CreateNoteSwitch() {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var enabled by remember { mutableStateOf<Boolean?>(null) }
    LaunchedEffect(Unit) { enabled = isCreateNoteEnabled(context) }

    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(stringResource(R.string.allow_create), Modifier.weight(1f))
        Switch(
            checked = enabled == true,
            enabled = enabled != null,
            onCheckedChange = { checked ->
                scope.launch {
                    setCreateNoteEnabled(context, checked)
                    enabled = isCreateNoteEnabled(context)
                }
            },
        )
    }
}
