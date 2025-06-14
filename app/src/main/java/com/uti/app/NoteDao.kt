package com.example.serenoteapp.data

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

    // ✅ Fungsi untuk getAllNotesFlow()
    @Query("SELECT * FROM notes ORDER BY updatedAt DESC")
    fun getAllNotesFlow(): Flow<List<Note>>

    // ✅ Fungsi untuk getActiveNotes()
    @Query("SELECT * FROM notes WHERE isArchived = 0 ORDER BY updatedAt DESC")
    fun getActiveNotes(): Flow<List<Note>>

    // ✅ Fungsi untuk getNotesByCategory()
    @Query("SELECT * FROM notes WHERE category = :category ORDER BY updatedAt DESC")
    fun getNotesByCategory(category: String): Flow<List<Note>>

    // ✅ Fungsi untuk searchNotes()
    @Query("SELECT * FROM notes WHERE title LIKE '%' || :query || '%' OR content LIKE '%' || :query || '%' ORDER BY updatedAt DESC")
    fun searchNotes(query: String): Flow<List<Note>>

    // ✅ Fungsi untuk deleteAllNotes()
    @Query("DELETE FROM notes")
    suspend fun deleteAllNotes()
}
