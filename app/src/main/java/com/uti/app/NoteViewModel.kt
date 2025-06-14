package com.example.serenoteapp.viewmodel

import android.content.Context
import android.widget.Toast
import androidx.lifecycle.LiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.work.Data
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import com.example.serenoteapp.data.Note
import com.example.serenoteapp.data.NoteRepository
import com.example.serenoteapp.worker.ReminderWorker
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import java.io.File
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.TimeUnit

class NoteViewModel(private val repository: NoteRepository) : ViewModel() {

    /* ---------- State ----------- */
    private val _allNotes = MutableStateFlow<List<Note>>(emptyList())
    val allNotes: StateFlow<List<Note>> = _allNotes.asStateFlow()

    init {
        viewModelScope.launch {
            repository.getAllNotes().collectLatest { _allNotes.value = it }
        }
    }

    /* ---------- CRUD ------------ */
    fun insertNote(note: Note)  = viewModelScope.launch { repository.insertNote(note) }
    fun updateNote(note: Note)  = viewModelScope.launch { repository.updateNote(note) }
    fun deleteNote(note: Note)  = viewModelScope.launch { repository.deleteNote(note) }
    fun deleteAllNotes()        = viewModelScope.launch { repository.deleteAllNotes() }

    /* ---------- Search ----------- */
    fun searchNotes(query: String): LiveData<List<Note>> = repository.searchNotes(query)

    /* ---------- Export ---------- */
    fun exportNotesToTxt(context: Context) {
        val file = File(context.getExternalFilesDir(null), "catatan_serenote.txt")
        file.writeText(_allNotes.value.joinToString("\n\n") {
            "Judul: ${it.title}\nIsi: ${it.content}\nTerakhir Diupdate: ${formatDate(it.updatedAt)}"
        })
        Toast.makeText(context, "Berhasil export ke ${file.name}", Toast.LENGTH_SHORT).show()
    }

    /* ---------- Backup ---------- */
    fun backupNotes(context: Context, notes: List<Note>) {
        val json  = Gson().toJson(notes)
        val file  = File(context.getExternalFilesDir(null), "backup_notes.json")
        file.writeText(json)
        Toast.makeText(context, "Backup disimpan!", Toast.LENGTH_SHORT).show()
    }

    /* ---------- Restore (pakai try / catch) ---------- */
    fun restoreNotes(context: Context) {
        try {
            val file = File(context.getExternalFilesDir(null), "backup_notes.json")
            if (file.exists()) {
                val json  = file.readText()
                val type  = object : TypeToken<List<Note>>() {}.type
                val notes : List<Note> = Gson().fromJson(json, type)

                viewModelScope.launch {
                    notes.forEach { repository.insertNote(it.copy(id = 0)) }
                }
                Toast.makeText(context, "Restore berhasil!", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(context, "File backup tidak ditemukan!", Toast.LENGTH_SHORT).show()
            }
        } catch (e: Exception) {
            Toast.makeText(context, "Gagal restore: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }

    /* ---------- Reminder -------- */
    fun scheduleReminder(
        note: Note,
        context: Context,
        delayMillis: Long = TimeUnit.HOURS.toMillis(1)
    ) {
        val data = Data.Builder()
            .putString("title", note.title)
            .putString("content", note.content)
            .build()

        val request = OneTimeWorkRequestBuilder<ReminderWorker>()
            .setInputData(data)
            .setInitialDelay(delayMillis, TimeUnit.MILLISECONDS)
            .build()

        WorkManager.getInstance(context).enqueue(request)
    }

    /* overload untuk kemudahan pemanggilan */
    fun scheduleReminder(
        context: Context,
        note: Note,
        delayMillis: Long = TimeUnit.HOURS.toMillis(1)
    ) = scheduleReminder(note, context, delayMillis)

    /* ---------- Utils ----------- */
    private fun formatDate(ts: Long): String =
        SimpleDateFormat("dd MMM yyyy HH:mm", Locale.getDefault()).format(Date(ts))
}
