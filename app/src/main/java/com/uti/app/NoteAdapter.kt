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
    private val onNoteUpdated: (Note) -> Unit = {}      // digunakan untuk pin, archive, dsb
) : ListAdapter<Note, NoteAdapter.NoteViewHolder>(DIFF_CALLBACK) {

    private var fullList = emptyList<Note>()

    /* ---------------- ViewHolder ---------------- */
    inner class NoteViewHolder(private val binding: ItemNoteBinding)
        : RecyclerView.ViewHolder(binding.root) {

        private val context get() = itemView.context

        fun bind(note: Note) = with(binding) {

            /* --- isi data --- */
            tvTitle.text     = note.title
            tvContent.text   = note.content
            tvDate.text      = formatDate(note.timestamp)
            tvUpdatedAt.text = "Diupdate: ${formatDate(note.updatedAt)}"
            ivPin.visibility = if (note.isPinned) View.VISIBLE else View.GONE
            itemView.findViewById<ImageView>(R.id.ivLock).visibility =
                if (note.isLocked) View.VISIBLE else View.GONE

            /* --- aksi klik --- */
            root.setOnClickListener   { onItemClick(note) }
            btnDelete.setOnClickListener { onDeleteClick(note) }

            /* Toggle pin via long‑press */
            root.setOnLongClickListener {
                onNoteUpdated(note.copy(
                    isPinned   = !note.isPinned,
                    updatedAt  = System.currentTimeMillis()
                ))
                true
            }

            /* ------- Tombol Export ------- */
            btnExport.setOnClickListener {
                val file = File(context.getExternalFilesDir(null), "${note.title}.txt")
                file.writeText("Judul: ${note.title}\nIsi:\n${note.content}")
                Toast.makeText(context, "Catatan berhasil diekspor!", Toast.LENGTH_SHORT).show()
            }

            /* Arsip cepat via long‑press tombol Export */
            btnExport.setOnLongClickListener {
                onNoteUpdated(note.copy(isArchived = true, updatedAt = System.currentTimeMillis()))
                true
            }

            /* ------- Tombol Archive terpisah ------- */
            btnArchive.setOnClickListener {
                onNoteUpdated(note.copy(isArchived = true, updatedAt = System.currentTimeMillis()))
                Toast.makeText(context, "Catatan diarsipkan", Toast.LENGTH_SHORT).show()
            }

            /* ------- Tombol Reminder ------- */
            btnReminder.setOnClickListener {
                val now = Calendar.getInstance()
                TimePickerDialog(
                    context,
                    { _, hour, minute ->
                        val schedule = Calendar.getInstance().apply {
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
                            .setExact(AlarmManager.RTC_WAKEUP, schedule.timeInMillis, pending)

                        Toast.makeText(context, "Pengingat diatur!", Toast.LENGTH_SHORT).show()
                    },
                    now.get(Calendar.HOUR_OF_DAY),
                    now.get(Calendar.MINUTE),
                    true
                ).show()
            }

            /* --- animasi fade‑in --- */
            itemView.startAnimation(AnimationUtils.loadAnimation(context, R.anim.fade_in))
        }
    }

    /* ---------------- Adapter overrides ---------------- */
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): NoteViewHolder {
        val binding = ItemNoteBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return NoteViewHolder(binding)
    }

    override fun onBindViewHolder(holder: NoteViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    /* ---------------- public helpers ---------------- */
    fun setData(newList: List<Note>) {
        fullList = newList
        submitList(newList)
    }

    fun filter(query: String) {
        val filtered = if (query.isBlank()) fullList else
            fullList.filter { it.title.contains(query, true) || it.content.contains(query, true) }
        submitList(filtered)
    }

    /* ---------------- utils ---------------- */
    private fun formatDate(ts: Long): String =
        SimpleDateFormat("dd MMM yyyy HH:mm", Locale.getDefault()).format(Date(ts))

    companion object {
        private val DIFF_CALLBACK = object : DiffUtil.ItemCallback<Note>() {
            override fun areItemsTheSame(o: Note, n: Note) = o.id == n.id
            override fun areContentsTheSame(o: Note, n: Note) = o == n
        }
    }
}
