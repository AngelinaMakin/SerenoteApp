package com.example.serenoteapp.adapter

import android.app.AlarmManager
import android.app.PendingIntent
import android.app.TimePickerDialog
import android.content.Context
import android.content.Intent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.AnimationUtils
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.Toast
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.serenoteapp.R
import com.example.serenoteapp.data.Note
import com.example.serenoteapp.databinding.ItemNoteBinding
import com.example.serenoteapp.receiver.ReminderReceiver
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

class NoteAdapter(
    private val onItemClick:   (Note) -> Unit = {},
    private val onDeleteClick: (Note) -> Unit = {},
    private val onNoteUpdated: (Note) -> Unit = {}
) : ListAdapter<Note, NoteAdapter.NoteViewHolder>(DIFF_CALLBACK) {

    private var fullList = emptyList<Note>()

    inner class NoteViewHolder(private val binding: ItemNoteBinding)
        : RecyclerView.ViewHolder(binding.root) {

        private val context get() = itemView.context

        fun bind(note: Note) = with(binding) {
            // Tampilkan data
            tvTitle.text     = note.title
            tvContent.text   = note.content
            tvDate.text      = formatDate(note.timestamp)
            tvUpdatedAt.text = "Diupdate: ${formatDate(note.updatedAt)}"
            ivPin.visibility = if (note.isPinned) View.VISIBLE else View.GONE
            itemView.findViewById<ImageView>(R.id.ivLock).visibility =
                if (note.isLocked) View.VISIBLE else View.GONE

            // Atur ikon pin (1 ikon saja)
            val btnPin = itemView.findViewById<ImageButton>(R.id.btnPin)
            btnPin.setImageResource(R.drawable.ic_pin)
            btnPin.setColorFilter(
                if (note.isPinned)
                    context.getColor(R.color.teal_200)
                else
                    context.getColor(R.color.gray)
            )

            // Aksi klik
            root.setOnClickListener { onItemClick(note) }
            btnDelete.setOnClickListener { onDeleteClick(note) }

            btnPin.setOnClickListener {
                onNoteUpdated(note.copy(isPinned = !note.isPinned, updatedAt = System.currentTimeMillis()))
            }

            root.setOnLongClickListener {
                onNoteUpdated(note.copy(isPinned = !note.isPinned, updatedAt = System.currentTimeMillis()))
                true
            }

            // Export
            btnExport.setOnClickListener {
                val file = File(context.getExternalFilesDir(null), "${note.title}.txt")
                file.writeText("Judul: ${note.title}\nIsi:\n${note.content}")
                Toast.makeText(context, "Catatan berhasil diekspor!", Toast.LENGTH_SHORT).show()
            }
            btnExport.setOnLongClickListener {
                onNoteUpdated(note.copy(isArchived = true, updatedAt = System.currentTimeMillis()))
                true
            }

            // Archive
            btnArchive.setOnClickListener {
                onNoteUpdated(note.copy(isArchived = true, updatedAt = System.currentTimeMillis()))
                Toast.makeText(context, "Catatan diarsipkan", Toast.LENGTH_SHORT).show()
            }

            // Reminder
            btnReminder.setOnClickListener {
                val now = Calendar.getInstance()
                TimePickerDialog(
                    context,
                    { _, hour, minute ->
                        val at = Calendar.getInstance().apply {
                            set(Calendar.HOUR_OF_DAY, hour)
                            set(Calendar.MINUTE, minute)
                            set(Calendar.SECOND, 0)
                            set(Calendar.MILLISECOND, 0)
                        }
                        val intent = Intent(context, ReminderReceiver::class.java).apply {
                            putExtra("title", note.title)
                            putExtra("content", note.content)
                        }
                        val pending = PendingIntent.getBroadcast(
                            context, note.id, intent,
                            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                        )
                        (context.getSystemService(Context.ALARM_SERVICE) as AlarmManager)
                            .setExact(AlarmManager.RTC_WAKEUP, at.timeInMillis, pending)

                        Toast.makeText(context, "Pengingat diatur!", Toast.LENGTH_SHORT).show()
                    },
                    now.get(Calendar.HOUR_OF_DAY),
                    now.get(Calendar.MINUTE),
                    true
                ).show()
            }

            // Animasi
            itemView.startAnimation(AnimationUtils.loadAnimation(context, R.anim.fade_in))
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): NoteViewHolder {
        val binding = ItemNoteBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return NoteViewHolder(binding)
    }

    override fun onBindViewHolder(holder: NoteViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    fun setData(newList: List<Note>) {
        fullList = newList
        submitList(newList)
    }

    fun filter(query: String) {
        val filtered = if (query.isBlank()) fullList else
            fullList.filter { it.title.contains(query, true) || it.content.contains(query, true) }
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
