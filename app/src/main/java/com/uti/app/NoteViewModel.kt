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
            repository.getAllNotes().collectLatest {
                _allNotes.value = it
            }
        }
    }

    /* ---------- CRUD ------------ */
    fun insertNote(note: Note) = viewModelScope.launch {
        repository.insertNote(note)
    }

    fun updateNote(note: Note) = viewModelScope.launch {
        repository.updateNote(note)
    }

    fun deleteNote(note: Note) = viewModelScope.launch {
        repository.deleteNote(note)
    }

    fun deleteAllNotes() = viewModelScope.launch {
        repository.deleteAllNotes()
    }

    /* ---------- Search ----------- */
    fun searchNotes(query: String): LiveData<List<Note>> {
        return repository.searchNotes(query)
    }

    /* ---------- Export to TXT ---------- */
    fun exportNotesToTxt(context: Context) {
        val file = File(context.getExternalFilesDir(null), "catatan_serenote.txt")
        file.writeText(_allNotes.value.joinToString("\n\n") {
            "Judul: ${it.title}\nIsi: ${it.content}\n" +
                    "Terakhir Diupdate: ${formatDate(it.updatedAt)}"
        })
        Toast.makeText(context, "Berhasil export ke ${file.name}", Toast.LENGTH_SHORT).show()
    }

    /* ---------- Backup to JSON ---------- */
    fun backupNotes(context: Context, notes: List<Note>) {
        val gson = Gson()
        val json = gson.toJson(notes)
        val file = File(context.getExternalFilesDir(null), "backup_notes.json")
        file.writeText(json)
        Toast.makeText(context, "Backup disimpan!", Toast.LENGTH_SHORT).show()
    }

    /* ---------- Reminder (parameter: note, context) ---------- */
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

    /* ---------- Reminder (overload: context, note) ---------- */
    fun scheduleReminder(
        context: Context,
        note: Note,
        delayMillis: Long = TimeUnit.HOURS.toMillis(1)
    ) = scheduleReminder(note, context, delayMillis)

    /* ---------- Utils ----------- */
    private fun formatDate(ts: Long): String =
        SimpleDateFormat("dd MMM yyyy HH:mm", Locale.getDefault()).format(Date(ts))
}
