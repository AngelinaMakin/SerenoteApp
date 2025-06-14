package com.example.serenoteapp.data

import androidx.lifecycle.LiveData
import kotlinx.coroutines.flow.Flow

class NoteRepository(private val dao: NoteDao) {

    /* ---------- Read ---------- */
    fun getAllNotesFlow(): Flow<List<Note>>          = dao.getAllNotesFlow()
    fun getActiveNotes(): LiveData<List<Note>>       = dao.getActiveNotes()
    fun getNotesByCategory(cat: String)              = dao.getNotesByCategory(cat)
    fun searchNotes(q: String)                       = dao.searchNotes(q)

    /* ---------- Write ---------- */
    suspend fun insertNote(n: Note)                  = dao.insertNote(n)
    suspend fun updateNote(n: Note)                  = dao.updateNote(n)
    suspend fun deleteNote(n: Note)                  = dao.deleteNote(n)
    suspend fun deleteAllNotes()                     = dao.deleteAllNotes()
}
