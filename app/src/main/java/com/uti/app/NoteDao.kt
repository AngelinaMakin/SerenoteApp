package com.yourapp.serenoteapp.data

import androidx.lifecycle.LiveData
import androidx.room.*

@Dao
interface NoteDao {

    @Query("SELECT * FROM note_table ORDER BY timestamp DESC")
    fun getAllNotes(): LiveData<List<Note>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(note: Note)

    @Update
    suspend fun update(note: Note)

    @Delete
    suspend fun delete(note: Note)
}
