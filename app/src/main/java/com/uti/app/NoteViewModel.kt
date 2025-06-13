package com.example.serenoteapp.viewmodel

import android.content.Context
import android.widget.Toast
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.workDataOf
import com.example.serenoteapp.data.Note
import com.example.serenoteapp.data.NoteRepository
import com.example.serenoteapp.worker.ReminderWorker
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

    private val _allNotes = MutableStateFlow<List<Note>>(emptyList())
    val allNotes: StateFlow<List<Note>> = _allNotes.asStateFlow()

    init {
        viewModelScope.launch {
            repository.getAllNotes().collectLatest { notes ->
                _allNotes.value = notes
            }
        }
    }

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

    fun exportNotesToTxt(context: Context) {
        val notes = _allNotes.value
        val fileName = "catatan_serenote.txt"
        val file = File(context.getExternalFilesDir(null), fileName)
        file.writeText(notes.joinToString("\n\n") {
            "Judul: ${it.title}\nIsi: ${it.content}\nTerakhir Diupdate: ${formatDate(it.updatedAt)}"
        })
        Toast.makeText(context, "Berhasil export ke $fileName", Toast.LENGTH_SHORT).show()
    }

    fun scheduleReminder(note: Note, context: Context) {
        val work = OneTimeWorkRequestBuilder<ReminderWorker>()
            .setInputData(
                workDataOf(
                    "title" to note.title,
                    "content" to note.content
                )
            )
            .setInitialDelay(1, TimeUnit.HOURS)  // Reminder dalam 1 jam
            .build()

        WorkManager.getInstance(context).enqueue(work)
    }

    private fun formatDate(timestamp: Long): String {
        val sdf = SimpleDateFormat("dd MMM yyyy HH:mm", Locale.getDefault())
        return sdf.format(Date(timestamp))
    }
}
