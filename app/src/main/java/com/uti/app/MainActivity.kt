package com.example.serenoteapp

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.widget.Button
import android.widget.SearchView
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.asLiveData
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.serenoteapp.adapter.NoteAdapter
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

    private lateinit var adapter: NoteAdapter

    private val noteViewModel: NoteViewModel by viewModels {
        NoteViewModelFactory(
            NoteRepository(NoteDatabase.getDatabase(this).noteDao())
        )
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        createNotificationChannelIfNeeded()
        requestNotifPermissionIfNeeded()

        /* ---------- RecyclerView ---------- */
        adapter = NoteAdapter(
            onItemClick = { /* buka detail jika perlu */ },
            onDeleteClick = { noteViewModel.deleteNote(it) }
        )
        findViewById<RecyclerView>(R.id.recyclerView).apply {
            layoutManager = LinearLayoutManager(this@MainActivity)
            adapter = this@MainActivity.adapter
        }

        /* ---------- SearchView ---------- */
        findViewById<SearchView>(R.id.searchView).setOnQueryTextListener(object :
            SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(q: String?) = false
            override fun onQueryTextChange(text: String?): Boolean {
                adapter.filter(text ?: "")
                return true
            }
        })

        /* ---------- Observe list ---------- */
        lifecycleScope.launch {
            noteViewModel.allNotes.collectLatest { adapter.setData(it) }
        }

        /* ---------- Tombol CRUD / I/O ---------- */
        findViewById<Button>(R.id.btnDeleteAll).setOnClickListener { confirmAndDeleteAll() }
        findViewById<Button>(R.id.btnExport).setOnClickListener  { exportNotesWithPermission() }
        findViewById<Button>(R.id.btnReminder).setOnClickListener{ scheduleLatestNoteReminder() }

        /* Backup */
        findViewById<Button>(R.id.btnBackup).setOnClickListener {
            noteViewModel.allNotes.asLiveData().observe(this) { list ->
                noteViewModel.backupNotes(this, list)
            }
        }

        /* Restore */
        findViewById<Button>(R.id.btnRestore).setOnClickListener {
            noteViewModel.restoreNotes(this)
        }
    }

    /* ---------- helper & permission (isi sama spt sebelumnya) ---------- */
    // confirmAndDeleteAll(), exportNotesWithPermission(), scheduleLatestNoteReminder(),
    // createNotificationChannelIfNeeded(), requestNotifPermissionIfNeeded(),
    // onRequestPermissionsResult()  --> tetap gunakan versi lama kamu
}
