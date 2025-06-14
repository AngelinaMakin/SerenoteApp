package com.example.serenoteapp.viewmodel

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.widget.Toast
import androidx.lifecycle.*
import com.example.serenoteapp.data.Note
import com.example.serenoteapp.data.NoteRepository
import com.example.serenoteapp.receiver.ReminderReceiver
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class NoteViewModel(private val repo: NoteRepository) : ViewModel() {

    private val _allNotes = MutableLiveData<List<Note>>()
    val allNotes: LiveData<List<Note>> = _allNotes

    init {
        viewModelScope.launch {
            repo.getAllNotesFlow().collectLatest { _allNotes.postValue(it) }
        }
    }

    // READ
    fun getActiveNotes() = repo.getActiveNotes()
    fun getNotesByCategory(category: String) = repo.getNotesByCategory(category)
    fun searchNotes(query: String) = repo.searchNotes(query)

    // WRITE
    fun insertNote(note: Note) = viewModelScope.launch { repo.insertNote(note) }
    fun updateNote(note: Note) = viewModelScope.launch { repo.updateNote(note) }
    fun deleteNote(note: Note) = viewModelScope.launch { repo.deleteNote(note) }
    fun deleteAllNotes() = viewModelScope.launch { repo.deleteAllNotes() }

    // GETTER
    fun getLatestNote(): Note? = _allNotes.value?.maxByOrNull { it.updatedAt }

    // REMINDER
    fun scheduleReminder(context: Context, note: Note) = viewModelScope.launch {
        val intent = Intent(context, ReminderReceiver::class.java).apply {
            putExtra("noteTitle", note.title)
            putExtra("noteContent", note.content)
        }
        val pendingIntent = PendingIntent.getBroadcast(
            context, note.id, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val triggerTime = System.currentTimeMillis() + 60 * 60 * 1000L // 1 jam
        alarmManager.setExact(AlarmManager.RTC_WAKEUP, triggerTime, pendingIntent)
    }

    // EXPORT
    fun exportNotesToTxt(context: Context) = viewModelScope.launch {
        repo.exportNotesToTxt(context)
        Toast.makeText(context, "Catatan diekspor!", Toast.LENGTH_SHORT).show()
    }

    // RESTORE
    fun restoreNotes(context: Context) = viewModelScope.launch {
        try {
            repo.restoreNotes(context)
            Toast.makeText(context, "Restore berhasil", Toast.LENGTH_SHORT).show()
        } catch (e: Exception) {
            Toast.makeText(context, e.message ?: "Restore gagal", Toast.LENGTH_SHORT).show()
        }
    }
}


fun insertNote(note: Note) {
    viewModelScope.launch {
        val newNote = note.copy(
            createdAt = System.currentTimeMillis(),
            updatedAt = System.currentTimeMillis()
        )
        repository.insertNote(newNote)
    }
}

fun updateNote(note: Note) {
    viewModelScope.launch {
        val updated = note.copy(updatedAt = System.currentTimeMillis())
        repository.updateNote(updated)
    }
}
