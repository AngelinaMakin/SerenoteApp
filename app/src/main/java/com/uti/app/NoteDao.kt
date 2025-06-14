package com.example.serenoteapp.data

import androidx.lifecycle.LiveData
import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface NoteDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertNote(note: Note)

    @Update
    suspend fun updateNote(note: Note)

    @Delete
    suspend fun deleteNote(note: Note)

    /* ---------- Query ---------- */

    @Query("SELECT * FROM notes ORDER BY timestamp DESC")
    fun getAllNotesFlow(): Flow<List<Note>>

    @Query("SELECT * FROM notes ORDER BY isPinned DESC, updatedAt DESC")
    fun getAllNotes(): LiveData<List<Note>>

    @Query("SELECT * FROM notes WHERE isArchived = 0 ORDER BY isPinned DESC, updatedAt DESC")
    fun getActiveNotes(): LiveData<List<Note>>

    @Query("SELECT * FROM notes WHERE isArchived = 0 ORDER BY isPinned DESC, updatedAt DESC")
    fun getNotes(): LiveData<List<Note>>  // <- Tambahan

    @Query("SELECT * FROM notes WHERE category = :category AND isArchived = 0")
    fun getNotesByCategory(category: String): LiveData<List<Note>>

    @Query("""
        SELECT * FROM notes
        WHERE (title LIKE '%' || :query || '%' OR content LIKE '%' || :query || '%')
          AND isArchived = 0
        ORDER BY updatedAt DESC
    """)
    fun searchNotes(query: String): LiveData<List<Note>>

    @Query("DELETE FROM notes")
    suspend fun deleteAllNotes()
}
