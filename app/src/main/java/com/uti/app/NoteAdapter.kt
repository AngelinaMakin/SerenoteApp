package com.example.serenoteapp.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.serenoteapp.data.Note
import com.example.serenoteapp.databinding.ItemNoteBinding
import java.text.SimpleDateFormat
import java.util.*

class NoteAdapter(
    private val onItemClick: (Note) -> Unit = {},
    private val onDeleteClick: (Note) -> Unit = {},
    private val onNoteUpdated: (Note) -> Unit = {}  // listener untuk update note (pin & archive)
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

            // Long click pada root untuk toggle pin
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

        // Long click pada itemView (selain root) untuk arsipkan note
        holder.itemView.setOnLongClickListener {
            val archivedNote = note.copy(isArchived = true, updatedAt = System.currentTimeMillis())
            onNoteUpdated(archivedNote)
            true
        }

        // Animasi sederhana
        holder.itemView.alpha = 0f
        holder.itemView.animate().alpha(1f).setDuration(300).start()
    }

    /* ---------- helper ---------- */
    fun setData(newList: List<Note>) {
        fullList = newList
        submitList(newList)
    }

    fun filter(query: String) {
        val filtered = if (query.isBlank()) fullList else
            fullList.filter {
                it.title.contains(query, ignoreCase = true) || it.content.contains(query, ignoreCase = true)
            }
        submitList(filtered)
    }

    private fun formatDate(ts: Long): String =
        SimpleDateFormat("dd MMM yyyy HH:mm", Locale.getDefault()).format(Date(ts))

    companion object {
        private val DIFF_CALLBACK = object : DiffUtil.ItemCallback<Note>() {
            override fun areItemsTheSame(o: Note, n: Note) = o.id == n.id
            override fun areContentsTheSame(o: Note, n: Note) = o == n
        }
    }
}
