package com.example.serenoteapp.data

import android.os.Parcelable
import androidx.room.Entity
import androidx.room.PrimaryKey
import kotlinx.parcelize.Parcelize

@Parcelize
@Entity(tableName = "notes")
data class Note(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,

    val title: String,
    val content: String,

    val timestamp: Long,
    val createdAt: Long = 0L,
    val updatedAt: Long = 0L,

    val category: String = "Umum",
    val isPinned: Boolean = false,

    val isArchived: Boolean = false  // properti baru untuk status arsip
) : Parcelable
