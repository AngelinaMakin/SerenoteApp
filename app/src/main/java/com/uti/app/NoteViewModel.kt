package com.example.serenoteapp.viewmodel

import androidx.lifecycle.*
import com.example.serenoteapp.data.Note
import com.example.serenoteapp.data.NoteRepository
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

    /* ---------- Read wrappers ---------- */
    fun getActiveNotes()             = repo.getActiveNotes()
    fun getNotesByCategory(cat: String) = repo.getNotesByCategory(cat)
    fun searchNotes(q: String)       = repo.searchNotes(q)

    /* ---------- Write wrappers ---------- */
    fun insertNote(n: Note)          = viewModelScope.launch { repo.insertNote(n) }
    fun updateNote(n: Note)          = viewModelScope.launch { repo.updateNote(n) }
    fun deleteNote(n: Note)          = viewModelScope.launch { repo.deleteNote(n) }
    fun deleteAllNotes()             = viewModelScope.launch { repo.deleteAllNotes() }

    fun getLatestNote(): Note? = _allNotes.value?.maxByOrNull { it.updatedAt }
}
