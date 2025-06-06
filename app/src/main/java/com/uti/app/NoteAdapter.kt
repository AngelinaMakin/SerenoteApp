package com.example.serenoteapp.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.serenoteapp.data.Note
import com.example.serenoteapp.databinding.ItemNoteBinding

class NoteAdapter(private val onItemClick: (Note) -> Unit) :
    RecyclerView.Adapter<NoteAdapter.NoteViewHolder>() {

    private var noteList = listOf<Note>()

    inner class NoteViewHolder(val binding: ItemNoteBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): NoteViewHolder {
        val binding = ItemNoteBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return NoteViewHolder(binding)
    }

    override fun onBindViewHolder(holder: NoteViewHolder, position: Int) {
        val currentNote = noteList[position]
        holder.binding.apply {
            tvTitle.text = currentNote.title
            tvContent.text = currentNote.content
            root.setOnClickListener {
                onItemClick(currentNote)
            }
        }
    }

    override fun getItemCount(): Int = noteList.size

    fun setNotes(notes: List<Note>) {
        noteList = notes
        notifyDataSetChanged()
    }
}
