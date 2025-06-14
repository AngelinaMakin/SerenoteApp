package com.example.serenoteapp.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.Toast
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.serenoteapp.R
import com.example.serenoteapp.data.Note
import com.example.serenoteapp.databinding.ItemNoteBinding
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

class NoteAdapter(
    private val onItemClick: (Note) -> Unit = {},
    private val onDeleteClick: (Note) -> Unit = {},
    private val onNoteUpdated: (Note) -> Unit = {}
) : ListAdapter<Note, NoteAdapter.NoteViewHolder>(DIFF_CALLBACK) {

    private var fullList = emptyList<Note>()

    inner class NoteViewHolder(private val binding: ItemNoteBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(note: Note) = with(binding) {
            tvTitle.text = note.title
            tvContent.text = note.content
            tvDate.text = formatDate(note.timestamp)
            tvUpdatedAt.text = "Diupdate: ${formatDate(note.updatedAt)}"

            ivPin.visibility = if (note.isPinned) android.view.View.VISIBLE else android.view.View.GONE

            root.setOnClickListener {
                onItemClick(note)
            }

            btnDelete.setOnClickListener {
                onDeleteClick(note)
            }

            root.setOnLongClickListener {
                val pinned = !note.isPinned
                val updatedNote = note.copy(isPinned = pinned, updatedAt = System.currentTimeMillis())
                onNoteUpdated(updatedNote)
                true
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): NoteViewHolder {
        val binding = ItemNoteBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return NoteViewHolder(binding)
    }

    override fun onBindViewHolder(holder: NoteViewHolder, position: Int) {
        val note = getItem(position)
        holder.bind(note)

        // Tombol Export
        holder.itemView.findViewById<ImageButton>(R.id.btnExport).setOnClickListener {
            val fileName = "${note.title}.txt"
            val fileContent = "Judul: ${note.title}\nIsi:\n${note.content}"
            val file = File(holder.itemView.context.getExternalFilesDir(null), fileName)
            file.writeText(fileContent)
            Toast.makeText(
                holder.itemView.context,
                "Disimpan ke ${file.absolutePath}",
                Toast.LENGTH_SHORT
            ).show()
        }

        // Long click pada itemView untuk arsipkan
        holder.itemView.setOnLongClickListener {
            val archivedNote = note.copy(isArchived = true, updatedAt = System.currentTimeMillis())
            onNoteUpdated(archivedNote)
            true
        }

        // Animasi sederhana
        holder.itemView.alpha = 0f
        holder.itemView.animate().alpha(1f).setDuration(300).start()
    }

    fun setData(newList: List<Note>) {
        fullList = newList
        submitList(newList)
    }

    fun filter(query: String) {
        val filtered = if (query.isBlank()) fullList else {
            fullList.filter {
                it.title.contains(query, ignoreCase = true) ||
                        it.content.contains(query, ignoreCase = true)
            }
        }
        submitList(filtered)
    }

    private fun formatDate(ts: Long): String =
        SimpleDateFormat("dd MMM yyyy HH:mm", Locale.getDefault()).format(Date(ts))

    companion object {
        private val DIFF_CALLBACK = object : DiffUtil.ItemCallback<Note>() {
            override fun areItemsTheSame(oldItem: Note, newItem: Note): Boolean =
                oldItem.id == newItem.id

            override fun areContentsTheSame(oldItem: Note, newItem: Note): Boolean =
                oldItem == newItem
        }
    }
}
