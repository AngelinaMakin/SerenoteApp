package com.example.serenoteapp.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.AnimationUtils
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

    /* ---------- ViewHolder ---------- */
    inner class NoteViewHolder(private val binding: ItemNoteBinding) :
        RecyclerView.ViewHolder(binding.root) {

        private val context get() = itemView.context    // shorthand

        fun bind(note: Note) = with(binding) {
            /* --- isi data --- */
            tvTitle.text       = note.title
            tvContent.text     = note.content
            tvDate.text        = formatDate(note.timestamp)
            tvUpdatedAt.text   = "Diupdate: ${formatDate(note.updatedAt)}"
            ivPin.visibility   = if (note.isPinned) View.VISIBLE else View.GONE

            /* --- click, long‑click --- */
            root.setOnClickListener        { onItemClick(note) }
            btnDelete.setOnClickListener   { onDeleteClick(note) }

            // Toggle pin via long‑press
            root.setOnLongClickListener {
                val updated = note.copy(
                    isPinned   = !note.isPinned,
                    updatedAt  = System.currentTimeMillis()
                )
                onNoteUpdated(updated)
                true
            }

            // Export button
            btnExport.setOnClickListener {
                val file = File(context.getExternalFilesDir(null), "${note.title}.txt")
                file.writeText("Judul: ${note.title}\nIsi:\n${note.content}")
                Toast.makeText(context, "Catatan berhasil diekspor!", Toast.LENGTH_SHORT).show()
            }

            // Arsip lewat long‑press seluruh card (selain root long press di atas)
            btnExport.setOnLongClickListener {
                val archived = note.copy(isArchived = true, updatedAt = System.currentTimeMillis())
                onNoteUpdated(archived)
                true
            }

            /* --- animasi fade‑in --- */
            itemView.startAnimation(
                AnimationUtils.loadAnimation(context, R.anim.fade_in)
            )
        }
    }

    /* ---------- Adapter overrides ---------- */
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): NoteViewHolder {
        val binding = ItemNoteBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return NoteViewHolder(binding)
    }

    override fun onBindViewHolder(holder: NoteViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    /* ---------- Public helpers ---------- */
    fun setData(newList: List<Note>) {
        fullList = newList
        submitList(newList)
    }

    fun filter(query: String) {
        val filtered = if (query.isBlank()) fullList else
            fullList.filter {
                it.title.contains(query, true) || it.content.contains(query, true)
            }
        submitList(filtered)
    }

    /* ---------- Utils ---------- */
    private fun formatDate(ts: Long): String =
        SimpleDateFormat("dd MMM yyyy HH:mm", Locale.getDefault()).format(Date(ts))

    companion object {
        private val DIFF_CALLBACK = object : DiffUtil.ItemCallback<Note>() {
            override fun areItemsTheSame(o: Note, n: Note) = o.id == n.id
            override fun areContentsTheSame(o: Note, n: Note) = o == n
        }
    }
}
