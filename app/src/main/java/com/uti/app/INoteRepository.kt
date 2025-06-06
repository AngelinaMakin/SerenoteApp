package com.example.serenoteapp.data

import kotlinx.coroutines.flow.Flow

interface INoteRepository {
    suspend fun insertNote(note: Note)
    fun getAllNotes(): Flow<List<Note>>
    suspend fun updateNote(note: Note)
    suspend fun deleteNote(note: Note)
}

