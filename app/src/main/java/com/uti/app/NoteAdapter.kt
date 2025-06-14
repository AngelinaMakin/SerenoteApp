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
import androidx.core.content.ContextCompat            // ✅ tambahkan
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
) : ListAdapter<Note, NoteAdapter.NoteViewHolder>(DIFF) {

    private var fullList = emptyList<Note>()

    /* ---------------- ViewHolder ---------------- */
    inner class NoteViewHolder(private val binding: ItemNoteBinding) :
        RecyclerView.ViewHolder(binding.root) {

        private val ctx get() = itemView.context

        fun bind(note: Note) = with(binding) {

            /* ---------- tampilkan data ---------- */
            tvTitle.text     = note.title
            tvContent.text   = note.content
            tvDate.text      = formatDate(note.timestamp)
            tvUpdatedAt.text = "Diupdate: ${formatDate(note.updatedAt)}"
            ivPin.visibility = if (note.isPinned) View.VISIBLE else View.GONE

            // ikon kunci
            itemView.findViewById<ImageView>(R.id.ivLock).visibility =
                if (note.isLocked) View.VISIBLE else View.GONE

            /* ---------- ikon pin dinamis ---------- */
            val btnPin = itemView.findViewById<ImageButton>(R.id.btnPin)
            btnPin.setImageResource(R.drawable.ic_star)
            btnPin.setColorFilter(
                ContextCompat.getColor(
                    ctx,
                    if (note.isPinned) R.color.teal_200 else R.color.gray
                )
            )

            /* ---------- aksi klik ---------- */
            root.setOnClickListener { onItemClick(note) }
            btnDelete.setOnClickListener { onDeleteClick(note) }

            // toggle pin
            btnPin.setOnClickListener {
                onNoteUpdated(note.copy(isPinned = !note.isPinned,
                    updatedAt = System.currentTimeMillis()))
            }
            root.setOnLongClickListener {
                onNoteUpdated(note.copy(isPinned = !note.isPinned,
                    updatedAt = System.currentTimeMillis()))
                true
            }

            /* ---------- export ---------- */
            btnExport.setOnClickListener {
                val file = File(ctx.getExternalFilesDir(null), "${note.title}.txt")
                file.writeText("Judul: ${note.title}\nIsi:\n${note.content}")
                Toast.makeText(ctx, "Catatan berhasil diekspor!", Toast.LENGTH_SHORT).show()
            }
            btnExport.setOnLongClickListener {
                onNoteUpdated(note.copy(isArchived = true, updatedAt = System.currentTimeMillis()))
                true
            }

            /* ---------- archive ---------- */
            btnArchive.setOnClickListener {
                onNoteUpdated(note.copy(isArchived = true, updatedAt = System.currentTimeMillis()))
                Toast.makeText(ctx, "Catatan diarsipkan", Toast.LENGTH_SHORT).show()
            }

            /* ---------- reminder ---------- */
            btnReminder.setOnClickListener {
                val now = Calendar.getInstance()
                TimePickerDialog(
                    ctx,
                    { _, h, m ->
                        val at = Calendar.getInstance().apply {
                            set(Calendar.HOUR_OF_DAY, h)
                            set(Calendar.MINUTE, m)
                            set(Calendar.SECOND, 0)
                            set(Calendar.MILLISECOND, 0)
                        }
                        val intent = Intent(ctx, ReminderReceiver::class.java).apply {
                            putExtra("title", note.title)
                            putExtra("content", note.content)
                        }
                        val pending = PendingIntent.getBroadcast(
                            ctx, note.id, intent,
                            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                        )
                        (ctx.getSystemService(Context.ALARM_SERVICE) as AlarmManager)
                            .setExact(AlarmManager.RTC_WAKEUP, at.timeInMillis, pending)
                        Toast.makeText(ctx, "Pengingat diatur!", Toast.LENGTH_SHORT).show()
                    },
                    now[Calendar.HOUR_OF_DAY],
                    now[Calendar.MINUTE],
                    true
                ).show()
            }

            /* ---------- animasi ---------- */
            itemView.startAnimation(AnimationUtils.loadAnimation(ctx, R.anim.fade_in))
        }
    }

    /* ---------------- adapter overrides ---------------- */
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): NoteViewHolder =
        NoteViewHolder(ItemNoteBinding.inflate(LayoutInflater.from(parent.context), parent, false))

    override fun onBindViewHolder(holder: NoteViewHolder, position: Int) =
        holder.bind(getItem(position))

    /* ---------------- helpers ---------------- */
    fun setData(newList: List<Note>) {
        fullList = newList
        submitList(newList)
    }

    fun filter(query: String) {
        val filtered = if (query.isBlank()) fullList
        else fullList.filter { it.title.contains(query, true) || it.content.contains(query, true) }
        submitList(filtered)
    }

    private fun formatDate(ts: Long): String =
        SimpleDateFormat("dd MMM yyyy HH:mm", Locale.getDefault()).format(Date(ts))

    companion object {
        private val DIFF = object : DiffUtil.ItemCallback<Note>() {
            override fun areItemsTheSame(o: Note, n: Note) = o.id == n.id
            override fun areContentsTheSame(o: Note, n: Note) = o == n
        }
    }
}
