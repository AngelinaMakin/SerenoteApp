package com.example.serenoteapp

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.view.View
import android.widget.*
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.LiveData
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.appcompat.widget.SearchView
import com.example.serenoteapp.adapter.NoteAdapter
import com.example.serenoteapp.data.Note
import com.example.serenoteapp.data.NoteDatabase
import com.example.serenoteapp.data.NoteRepository
import com.example.serenoteapp.viewmodel.NoteViewModel
import com.example.serenoteapp.viewmodel.NoteViewModelFactory
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity() {

    companion object {
        private const val CHANNEL_ID = "note_channel"
    }

    private lateinit var adapter: NoteAdapter
    private val categories = listOf("Semua", "Kuliah", "Pribadi", "Ide", "Umum")

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
            onItemClick = { /* bisa buka detail */ },
            onDeleteClick = { noteViewModel.deleteNote(it) }
        )
        findViewById<RecyclerView>(R.id.recyclerView).apply {
            layoutManager = LinearLayoutManager(this@MainActivity)
            adapter = this@MainActivity.adapter
        }

        /* ---------- SearchView ---------- */
        findViewById<SearchView>(R.id.searchView).setOnQueryTextListener(object :
            SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?) = false

            override fun onQueryTextChange(newText: String?): Boolean {
                if (!newText.isNullOrEmpty()) {
                    noteViewModel.searchNotes(newText).observe(this@MainActivity) { notes ->
                        adapter.setData(notes)
                    }
                } else {
                    observeNotes(noteViewModel.getActiveNotes())
                }
                return true
            }
        })

        /* ---------- Spinner Kategori ---------- */
        val spinner = findViewById<Spinner>(R.id.spinnerCategory)
        spinner.adapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, categories)

        spinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>, view: View?, position: Int, id: Long) {
                val selected = categories[position]
                observeNotes(
                    if (selected == "Semua")
                        noteViewModel.getActiveNotes()
                    else
                        noteViewModel.getNotesByCategory(selected)
                )
            }

            override fun onNothingSelected(parent: AdapterView<*>) {}
        }

        observeNotes(noteViewModel.getActiveNotes())

        /* ---------- Tombol‑tombol ---------- */
        findViewById<Button>(R.id.btnDeleteAll).setOnClickListener { confirmAndDeleteAll() }
        findViewById<Button>(R.id.btnExport).setOnClickListener { exportNotesWithPermission() }
        findViewById<Button>(R.id.btnReminder).setOnClickListener { scheduleLatestNoteReminder() }
        findViewById<Button>(R.id.btnBackup).setOnClickListener { backupNotes() }
        findViewById<Button>(R.id.btnRestore).setOnClickListener { restoreNotes() }
    }

    private fun observeNotes(source: LiveData<List<Note>>) {
        noteViewModel.getActiveNotes().removeObservers(this)
        categories.forEach { cat -> noteViewModel.getNotesByCategory(cat).removeObservers(this) }
        source.observe(this) { list -> adapter.setData(list) }
    }

    private fun confirmAndDeleteAll() {
        AlertDialog.Builder(this)
            .setTitle("Hapus semua catatan?")
            .setMessage("Tindakan ini tidak bisa dibatalkan.")
            .setPositiveButton("Hapus") { _, _ -> noteViewModel.deleteAllNotes() }
            .setNegativeButton("Batal", null)
            .show()
    }

    private fun exportNotesWithPermission() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE)
            == PackageManager.PERMISSION_GRANTED
        ) {
            noteViewModel.exportNotesToTxt(this)
        } else {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.WRITE_EXTERNAL_STORAGE),
                101
            )
        }
    }

    private fun scheduleLatestNoteReminder() {
        lifecycleScope.launch {
            val note = noteViewModel.allNotes.value.lastOrNull() ?: return@launch
            noteViewModel.scheduleReminder(this@MainActivity, note)
            Toast.makeText(this@MainActivity, "Reminder disetel!", Toast.LENGTH_SHORT).show()
        }
    }

    private fun backupNotes() {
        lifecycleScope.launch {
            val list = noteViewModel.allNotes.value
            noteViewModel.backupNotes(this@MainActivity, list)
        }
    }

    private fun restoreNotes() = noteViewModel.restoreNotes(this)

    private fun createNotificationChannelIfNeeded() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val mgr = getSystemService(NotificationManager::class.java)
            if (mgr.getNotificationChannel(CHANNEL_ID) == null) {
                mgr.createNotificationChannel(
                    NotificationChannel(
                        CHANNEL_ID,
                        "Note Reminder",
                        NotificationManager.IMPORTANCE_HIGH
                    )
                )
            }
        }
    }

    private fun requestNotifPermissionIfNeeded() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
            != PackageManager.PERMISSION_GRANTED
        ) {
            requestPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }

    private val requestPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
            if (!granted) {
                Toast.makeText(this, "Izin notifikasi ditolak", Toast.LENGTH_SHORT).show()
            }
        }
}
