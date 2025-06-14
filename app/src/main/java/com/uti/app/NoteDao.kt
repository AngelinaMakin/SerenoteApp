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

    // Versi Flow (lama, tetap dipertahankan)
    @Query("SELECT * FROM notes ORDER BY timestamp DESC")
    fun getAllNotesFlow(): Flow<List<Note>>

    // Versi baru: LiveData + urut berdasarkan isPinned dan updatedAt (semua note)
    @Query("SELECT * FROM notes ORDER BY isPinned DESC, updatedAt DESC")
    fun getAllNotes(): LiveData<List<Note>>

    // Query untuk mendapatkan hanya note yang aktif (tidak diarsipkan)
    @Query("SELECT * FROM notes WHERE isArchived = 0 ORDER BY isPinned DESC, updatedAt DESC")
    fun getActiveNotes(): LiveData<List<Note>>

    // ✅ Query berdasarkan kategori dan tidak diarsipkan
    @Query("SELECT * FROM notes WHERE category = :category AND isArchived = 0")
    fun getNotesByCategory(category: String): LiveData<List<Note>>

    @Query("DELETE FROM notes")
    suspend fun deleteAllNotes()

    @Query("SELECT * FROM notes WHERE title LIKE :query OR content LIKE :query ORDER BY updatedAt DESC")
    fun searchNotes(query: String): LiveData<List<Note>>
}
