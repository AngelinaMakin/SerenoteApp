package com.example.serenoteapp.data

import kotlinx.coroutines.flow.Flow

class NoteRepository(private val noteDao: NoteDao) : INoteRepository {

    override suspend fun insertNote(note: Note) {
        noteDao.insertNote(note)
    }
//Pastikan getAllNotes di repository mengembalikan Flow
    override fun getAllNotes(): Flow<List<Note>> = noteDao.getAllNotes()

    override suspend fun updateNote(note: Note) {
        noteDao.updateNote(note)
    }

    override suspend fun deleteNote(note: Note) {
        noteDao.deleteNote(note)
    }
}
