package com.example.serenoteapp.repository

import com.example.serenoteapp.data.NoteDao
import com.example.serenoteapp.model.Note

class NoteRepository(private val noteDao: NoteDao) {
    val allNotes = noteDao.getAllNotes()

    suspend fun insert(note: Note) = noteDao.insert(note)
    suspend fun update(note: Note) = noteDao.update(note)
    suspend fun delete(note: Note) = noteDao.delete(note)
}
