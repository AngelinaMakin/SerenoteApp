package com.example.serenoteapp.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.serenoteapp.data.Note
import com.example.serenoteapp.databinding.ItemNoteBinding

class NoteAdapter : RecyclerView.Adapter<NoteAdapter.NoteViewHolder>() {

    private var noteList = listOf<Note>()

    inner class NoteViewHolder(val binding: ItemNoteBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): NoteViewHolder {
        val binding = ItemNoteBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return NoteViewHolder(binding)
    }

    override fun onBindViewHolder(holder: NoteViewHolder, position: Int) {
        val currentNote = noteList[position]
        // TODO: Isi binding dengan data dari currentNote, contoh:
        // holder.binding.titleTextView.text = currentNote.title
    }

    override fun getItemCount(): Int = noteList.size

    fun setNotes(notes: List<Note>) {
        noteList = notes
        notifyDataSetChanged()
    }
}
