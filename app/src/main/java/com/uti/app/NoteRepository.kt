package com.example.serenoteapp.data

import android.content.Context
import com.google.gson.Gson
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import java.io.File

class NoteRepository(private val dao: NoteDao) {

    // Flow untuk observer di ViewModel
    fun getAllNotesFlow(): Flow<List<Note>> = dao.getAllNotesFlow()

    // READ
    fun getActiveNotes(): Flow<List<Note>> = dao.getActiveNotes()
    fun getNotesByCategory(category: String): Flow<List<Note>> = dao.getNotesByCategory(category)
    fun searchNotes(query: String): Flow<List<Note>> = dao.searchNotes(query)

    fun getNotesSortedByTitle(): Flow<List<Note>> = dao.getNotesSortedByTitle()
    fun getNotesSortedByNewest(): Flow<List<Note>> = dao.getNotesSortedByNewest()

    // WRITE
    suspend fun insertNote(note: Note) = dao.insertNote(note)

    suspend fun insertAll(notes: List<Note>) = dao.insertAll(notes)

    suspend fun updateNote(note: Note) = dao.updateNote(note)
    suspend fun deleteNote(note: Note) = dao.deleteNote(note)
    suspend fun deleteAllNotes() = dao.deleteAllNotes()

    // EXPORT ke .txt
    suspend fun exportNotesToTxt(context: Context) = withContext(Dispatchers.IO) {
        val notes = dao.getActiveNotes().first()
        val file = File(context.getExternalFilesDir(null), "catatan_export.txt")
        file.writeText(notes.joinToString("\n\n") {
            "Judul: ${it.title}\nIsi:\n${it.content}"
        })
    }

    // RESTORE dari backup JSON
    suspend fun restoreNotes(context: Context) = withContext(Dispatchers.IO) {
        val file = File(context.getExternalFilesDir(null), "backup_catatan.json")
        if (!file.exists()) throw IllegalStateException("File backup tidak ditemukan")

        val json = file.readText()
        val notes = Gson().fromJson(json, Array<Note>::class.java).toList()
        insertAll(notes.map { it.copy(id = 0) }) // gunakan insertAll yang baru
    }
}
