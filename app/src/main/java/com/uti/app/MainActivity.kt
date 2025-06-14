package com.example.serenoteapp

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.view.View
import android.widget.*
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.appcompat.widget.SearchView
import androidx.appcompat.widget.SwitchCompat
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.LiveData
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
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
        private const val PREF_NAME  = "settings"
        private const val KEY_DARK   = "dark_mode"
    }

    private lateinit var adapter: NoteAdapter
    private lateinit var tvTotal: TextView
    private val categories = listOf("Semua", "Kuliah", "Pribadi", "Ide", "Umum")

    private val noteViewModel: NoteViewModel by viewModels {
        NoteViewModelFactory(
            NoteRepository(NoteDatabase.getDatabase(this).noteDao())
        )
    }

    /* ------------------------------------------------------------ */
    /* onCreate                                                     */
    /* ------------------------------------------------------------ */
    override fun onCreate(savedInstanceState: Bundle?) {
        applySavedDarkMode()
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        createNotificationChannelIfNeeded()
        requestNotifPermissionIfNeeded()

        /* ---------- View refs ---------- */
        tvTotal = findViewById(R.id.tvTotalNotes)

        /* ---------- RecyclerView ---------- */
        adapter = NoteAdapter(
            onItemClick = { /* buka detail */ },
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
                if (newText.isNullOrBlank()) {
                    observeNotes(noteViewModel.getActiveNotes())
                } else {
                    noteViewModel.searchNotes(newText).observe(this@MainActivity) { list ->
                        updateUI(list)
                    }
                }
                return true
            }
        })

        /* ---------- Spinner kategori ---------- */
        val spinner = findViewById<Spinner>(R.id.spinnerCategory)
        spinner.adapter =
            ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, categories)
        spinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(p: AdapterView<*>, v: View?, pos: Int, id: Long) {
                val src: LiveData<List<Note>> =
                    if (categories[pos] == "Semua")
                        noteViewModel.getActiveNotes()
                    else
                        noteViewModel.getNotesByCategory(categories[pos])
                observeNotes(src)
            }

            override fun onNothingSelected(parent: AdapterView<*>) {}
        }

        /* ---------- Switch Dark Mode ---------- */
        val switchDark = findViewById<SwitchCompat>(R.id.switchDarkMode)
        switchDark.isChecked = isDarkModeEnabled()
        switchDark.setOnCheckedChangeListener { _, checked ->
            saveDarkMode(checked)
            AppCompatDelegate.setDefaultNightMode(
                if (checked) AppCompatDelegate.MODE_NIGHT_YES
                else AppCompatDelegate.MODE_NIGHT_NO
            )
        }

        /* ---------- Tombol‑tombol ---------- */
        findViewById<Button>(R.id.btnDeleteAll).setOnClickListener { confirmAndDeleteAll() }
        findViewById<Button>(R.id.btnExport).setOnClickListener  { exportNotesWithPermission() }
        findViewById<Button>(R.id.btnReminder).setOnClickListener{ scheduleLatestNoteReminder() }
        findViewById<Button>(R.id.btnBackup).setOnClickListener { backupNotes() }
        findViewById<Button>(R.id.btnRestore).setOnClickListener { restoreNotes() }

        /* ---------- Muat daftar awal ---------- */
        observeNotes(noteViewModel.getActiveNotes())
    }

    /* ------------------------------------------------------------ */
    /* LiveData helper                                              */
    /* ------------------------------------------------------------ */
    private fun observeNotes(src: LiveData<List<Note>>) {
        // Hilangkan observer lain agar tidak ganda
        noteViewModel.getActiveNotes().removeObservers(this)
        categories.forEach { noteViewModel.getNotesByCategory(it).removeObservers(this) }
        src.observe(this) { list -> updateUI(list) }
    }

    private fun updateUI(list: List<Note>) {
        tvTotal.text = "Total catatan: ${list.size}"
        adapter.setData(list)        // ganti setData sesuai NoteAdapter kamu
    }

    /* ------------------------------------------------------------ */
    /* Dialog & izin                                                */
    /* ------------------------------------------------------------ */
    private fun confirmAndDeleteAll() = AlertDialog.Builder(this)
        .setTitle("Hapus semua catatan?")
        .setMessage("Tindakan ini tidak bisa dibatalkan.")
        .setPositiveButton("Hapus") { _, _ -> noteViewModel.deleteAllNotes() }
        .setNegativeButton("Batal", null)
        .show()

    private fun exportNotesWithPermission() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE)
            == PackageManager.PERMISSION_GRANTED
        ) noteViewModel.exportNotesToTxt(this)
        else ActivityCompat.requestPermissions(
            this, arrayOf(Manifest.permission.WRITE_EXTERNAL_STORAGE), 101
        )
    }

    /* ------------------------------------------------------------ */
    /* Reminder                                                     */
    /* ------------------------------------------------------------ */
    private fun scheduleLatestNoteReminder() {
        lifecycleScope.launch {
            val note = adapter.currentList.lastOrNull() ?: return@launch
            noteViewModel.scheduleReminder(this@MainActivity, note)
            Toast.makeText(this@MainActivity, "Reminder disetel!", Toast.LENGTH_SHORT).show()
        }
    }

    /* ------------------------------------------------------------ */
    /* Backup & Restore                                             */
    /* ------------------------------------------------------------ */
    private fun backupNotes() = noteViewModel.backupNotes(this, adapter.currentList)
    private fun restoreNotes() = noteViewModel.restoreNotes(this)

    /* ------------------------------------------------------------ */
    /* Dark Mode prefs                                              */
    /* ------------------------------------------------------------ */
    private fun isDarkModeEnabled() =
        getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE).getBoolean(KEY_DARK, false)

    private fun saveDarkMode(enabled: Boolean) =
        getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
            .edit().putBoolean(KEY_DARK, enabled).apply()

    private fun applySavedDarkMode() {
        AppCompatDelegate.setDefaultNightMode(
            if (isDarkModeEnabled()) AppCompatDelegate.MODE_NIGHT_YES
            else AppCompatDelegate.MODE_NIGHT_NO
        )
    }

    /* ------------------------------------------------------------ */
    /* Notifikasi utils                                             */
    /* ------------------------------------------------------------ */
    private fun createNotificationChannelIfNeeded() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val mgr = getSystemService(NotificationManager::class.java)
            if (mgr.getNotificationChannel(CHANNEL_ID) == null) {
                mgr.createNotificationChannel(
                    NotificationChannel(CHANNEL_ID, "Note Reminder",
                        NotificationManager.IMPORTANCE_HIGH)
                )
            }
        }
    }

    private fun requestNotifPermissionIfNeeded() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
            != PackageManager.PERMISSION_GRANTED
        ) requestPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
    }

    private val requestPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
            if (!granted) Toast.makeText(this, "Izin notifikasi ditolak", Toast.LENGTH_SHORT).show()
        }
}
