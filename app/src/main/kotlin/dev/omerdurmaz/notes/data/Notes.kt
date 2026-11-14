package dev.omerdurmaz.notes.data

import androidx.room.Dao
import androidx.room.Database
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.RoomDatabase
import androidx.room.Transaction
import kotlinx.coroutines.flow.Flow
import java.util.UUID

@Entity(tableName = "notes")
data class NoteEntity(
    @PrimaryKey val id: String,
    val title: String,
    val body: String,
    val createdAt: Long,
    val trashedAt: Long? = null,
)

@Dao
abstract class NoteDao {

    @Query("SELECT * FROM notes WHERE trashedAt IS NULL ORDER BY createdAt DESC")
    abstract fun observeNotes(): Flow<List<NoteEntity>>

    @Query("SELECT * FROM notes WHERE trashedAt IS NOT NULL ORDER BY trashedAt DESC")
    abstract fun observeTrash(): Flow<List<NoteEntity>>

    @Query(
        """
        SELECT * FROM notes
        WHERE trashedAt IS NULL AND (title LIKE '%' || :text || '%' OR body LIKE '%' || :text || '%')
        ORDER BY createdAt DESC
        LIMIT :limit
        """,
    )
    abstract suspend fun search(text: String, limit: Int): List<NoteEntity>

    @Query("SELECT * FROM notes WHERE id = :id")
    abstract suspend fun get(id: String): NoteEntity?

    /** Returns the matching note if it exists, so a retried create doesn't add a copy. */
    @Transaction
    open suspend fun createIfAbsent(title: String, body: String): NoteEntity =
        findExact(title, body)
            ?: NoteEntity(UUID.randomUUID().toString(), title, body, System.currentTimeMillis())
                .also { insert(it) }

    /** Null if there is no such note. Trashing a note twice returns it both times. */
    @Transaction
    open suspend fun moveToTrash(id: String): NoteEntity? {
        val note = get(id) ?: return null
        if (note.trashedAt == null) setTrashedAt(id, System.currentTimeMillis())
        return get(id)
    }

    @Query("UPDATE notes SET trashedAt = NULL WHERE id = :id")
    abstract suspend fun restore(id: String)

    @Query("DELETE FROM notes WHERE trashedAt IS NOT NULL")
    abstract suspend fun emptyTrash(): Int

    @Query("SELECT * FROM notes WHERE trashedAt IS NULL AND title = :title AND body = :body LIMIT 1")
    protected abstract suspend fun findExact(title: String, body: String): NoteEntity?

    @Insert
    protected abstract suspend fun insert(note: NoteEntity)

    @Query("UPDATE notes SET trashedAt = :time WHERE id = :id")
    protected abstract suspend fun setTrashedAt(id: String, time: Long)
}

@Database(entities = [NoteEntity::class], version = 1, exportSchema = false)
abstract class NotesDatabase : RoomDatabase() {
    abstract fun notes(): NoteDao
}
