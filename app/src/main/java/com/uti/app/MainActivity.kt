package com.example.serenoteapp

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.widget.Button
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import com.example.serenoteapp.data.Note
import com.example.serenoteapp.data.NoteDatabase
import com.example.serenoteapp.data.NoteRepository
import com.example.serenoteapp.viewmodel.NoteViewModel
import com.example.serenoteapp.viewmodel.NoteViewModelFactory
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity() {

    companion object {
        private const val REQ_WRITE_STORAGE = 100
        private const val REQ_POST_NOTIF   = 200
    }

    private val noteViewModel: NoteViewModel by viewModels {
        NoteViewModelFactory(
            NoteRepository(NoteDatabase.getDatabase(this).noteDao())
        )
    }

    private var latestNotes: List<Note> = emptyList()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        createNotificationChannelIfNeeded()
        requestNotifPermissionIfNeeded()

        collectAllNotes()

        val btnDeleteAll  = findViewById<Button>(R.id.btnDeleteAll)
        val btnExport     = findViewById<Button>(R.id.btnExport)
        val btnReminder   = findViewById<Button>(R.id.btnReminder)

        btnDeleteAll.setOnClickListener { confirmAndDeleteAll() }
        btnExport.setOnClickListener    { exportNotesWithPermission() }
        btnReminder.setOnClickListener  { scheduleLatestNoteReminder() }
    }

    /* ---------------------------------------------------------------------- */
    /*  Helper‑helper private                                                 */
    /* ---------------------------------------------------------------------- */

    private fun collectAllNotes() {
        lifecycleScope.launch {
            noteViewModel.allNotes.collectLatest { latestNotes = it }
        }
    }

    private fun confirmAndDeleteAll() {
        if (latestNotes.isEmpty()) {
            Toast.makeText(this, "Tidak ada catatan untuk dihapus", Toast.LENGTH_SHORT).show()
            return
        }
        AlertDialog.Builder(this)
            .setTitle("Hapus Semua Catatan?")
            .setMessage("Apakah kamu yakin ingin menghapus semua catatan?")
            .setPositiveButton("Ya") { _, _ -> noteViewModel.deleteAllNotes() }
            .setNegativeButton("Batal", null)
            .show()
    }

    private fun exportNotesWithPermission() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q &&
            ContextCompat.checkSelfPermission(
                this, Manifest.permission.WRITE_EXTERNAL_STORAGE
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(
                this, arrayOf(Manifest.permission.WRITE_EXTERNAL_STORAGE), REQ_WRITE_STORAGE
            )
        } else {
            noteViewModel.exportNotesToTxt(this)
        }
    }

    private fun scheduleLatestNoteReminder() {
        latestNotes.lastOrNull()?.let { note ->
            noteViewModel.scheduleReminder(note, this)
            Toast.makeText(this, "Pengingat dibuat untuk \"${note.title}\"", Toast.LENGTH_SHORT).show()
        } ?: Toast.makeText(this, "Belum ada catatan", Toast.LENGTH_SHORT).show()
    }

    private fun createNotificationChannelIfNeeded() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                "note_channel",
                "Reminder Catatan",
                NotificationManager.IMPORTANCE_HIGH
            )
            getSystemService(NotificationManager::class.java)
                .createNotificationChannel(channel)
        }
    }

    private fun requestNotifPermissionIfNeeded() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(
                this, Manifest.permission.POST_NOTIFICATIONS
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(
                this, arrayOf(Manifest.permission.POST_NOTIFICATIONS), REQ_POST_NOTIF
            )
        }
    }

    /* ---------------------------------------------------------------------- */
    /*  Permission callback                                                   */
    /* ---------------------------------------------------------------------- */

    override fun onRequestPermissionsResult(
        requestCode: Int, permissions: Array<out String>, grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        when (requestCode) {
            REQ_WRITE_STORAGE ->
                if (grantResults.firstOrNull() == PackageManager.PERMISSION_GRANTED) {
                    noteViewModel.exportNotesToTxt(this)
                } else {
                    Toast.makeText(this, "Izin penyimpanan ditolak", Toast.LENGTH_SHORT).show()
                }
            REQ_POST_NOTIF -> { /* tidak wajib aksi lanjutan */ }
        }
    }
} 