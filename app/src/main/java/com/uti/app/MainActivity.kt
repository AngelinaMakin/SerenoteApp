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
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.serenoteapp.adapter.NoteAdapter
import com.example.serenoteapp.data.Note
import com.example.serenoteapp.data.NoteDatabase
import com.example.serenoteapp.data.NoteRepository
import com.example.serenoteapp.viewmodel.NoteViewModel
import com.example.serenoteapp.viewmodel.NoteViewModelFactory
import com.google.gson.Gson
import java.io.File
import kotlinx.coroutines.launch
import androidx.lifecycle.lifecycleScope

class MainActivity : AppCompatActivity() {

    companion object {
        private const val CHANNEL_ID = "note_channel"
        private const val PREF_NAME  = "settings"
        private const val KEY_DARK   = "dark_mode"
    }

    private lateinit var adapter: NoteAdapter
    private lateinit var tvTotal: TextView
    private val categories = listOf("Semua", "Kuliah", "Pribadi", "Ide", "Umum")

    private val viewModel: NoteViewModel by viewModels {
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

        tvTotal = findViewById(R.id.tvTotalNotes)

        /* RecyclerView */
        adapter = NoteAdapter(
            onItemClick  = { /* handle click */ },
            onDeleteClick = { viewModel.deleteNote(it) }
        )
        findViewById<RecyclerView>(R.id.recyclerView).apply {
            layoutManager = LinearLayoutManager(this@MainActivity)
            adapter = this@MainActivity.adapter
        }

        /* Search */
        findViewById<SearchView>(R.id.searchView)
            .setOnQueryTextListener(object : SearchView.OnQueryTextListener {
                override fun onQueryTextSubmit(q: String?) = false
                override fun onQueryTextChange(text: String?): Boolean {
                    if (text.isNullOrBlank()) {
                        observeNotes(viewModel.getActiveNotes())
                    } else {
                        viewModel.searchNotes(text).observe(this@MainActivity) { updateUI(it) }
                    }
                    return true
                }
            })

        /* Spinner kategori */
        val spinner = findViewById<Spinner>(R.id.spinnerCategory)
        spinner.adapter =
            ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, categories)
        spinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(p: AdapterView<*>, v: View?, pos: Int, id: Long) {
                val src: LiveData<List<Note>> =
                    if (categories[pos] == "Semua")
                        viewModel.getActiveNotes()
                    else
                        viewModel.getNotesByCategory(categories[pos])
                observeNotes(src)
            }
            override fun onNothingSelected(parent: AdapterView<*>) {}
        }

        /* Switch dark mode */
        val switchDark = findViewById<SwitchCompat>(R.id.switchDarkMode)
        switchDark.isChecked = isDarkModeEnabled()
        switchDark.setOnCheckedChangeListener { _, checked ->
            saveDarkMode(checked)
            AppCompatDelegate.setDefaultNightMode(
                if (checked) AppCompatDelegate.MODE_NIGHT_YES
                else AppCompatDelegate.MODE_NIGHT_NO
            )
        }

        /* Tombol‑tombol */
        findViewById<Button>(R.id.btnDeleteAll).setOnClickListener { confirmAndDeleteAll() }
        findViewById<Button>(R.id.btnExport).setOnClickListener  { exportNotesWithPermission() }
        findViewById<Button>(R.id.btnReminder).setOnClickListener{ scheduleLatestNoteReminder() }
        findViewById<Button>(R.id.btnRestore).setOnClickListener { restoreNotes() }

        // Backup langsung di sini
        findViewById<Button>(R.id.btnBackup).setOnClickListener {
            viewModel.getActiveNotes().observe(this) { notes ->
                val json  = Gson().toJson(notes)
                val file  = File(getExternalFilesDir(null), "backup_catatan.json")
                file.writeText(json)
                Toast.makeText(
                    this,
                    "Backup disimpan: ${file.absolutePath}",
                    Toast.LENGTH_LONG
                ).show()
            }
        }

        /* Muat daftar awal */
        observeNotes(viewModel.getActiveNotes())
    }

    /* ------------------------------------------------------------ */
    /* LiveData helper                                              */
    /* ------------------------------------------------------------ */
    private fun observeNotes(src: LiveData<List<Note>>) {
        viewModel.getActiveNotes().removeObservers(this)
        categories.forEach { viewModel.getNotesByCategory(it).removeObservers(this) }
        src.observe(this) { updateUI(it) }
    }

    private fun updateUI(list: List<Note>) {
        tvTotal.text = "Total catatan: ${list.size}"
        adapter.setData(list)
    }

    /* ------------------------------------------------------------ */
    /* Dialog & izin                                                */
    /* ------------------------------------------------------------ */
    private fun confirmAndDeleteAll() = AlertDialog.Builder(this)
        .setTitle("Hapus semua catatan?")
        .setMessage("Tindakan ini tidak bisa dibatalkan.")
        .setPositiveButton("Hapus") { _, _ -> viewModel.deleteAllNotes() }
        .setNegativeButton("Batal", null)
        .show()

    private fun exportNotesWithPermission() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE)
            == PackageManager.PERMISSION_GRANTED
        ) viewModel.exportNotesToTxt(this)
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
            viewModel.scheduleReminder(this@MainActivity, note)
            Toast.makeText(this@MainActivity, "Reminder disetel!", Toast.LENGTH_SHORT).show()
        }
    }

    /* ------------------------------------------------------------ */
    /* Restore                                                      */
    /* ------------------------------------------------------------ */
    private fun restoreNotes() = viewModel.restoreNotes(this)

    /* ------------------------------------------------------------ */
    /* Dark Mode prefs                                              */
    /* ------------------------------------------------------------ */
    private fun isDarkModeEnabled() =
        getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
            .getBoolean(KEY_DARK, false)

    private fun saveDarkMode(enabled: Boolean) =
        getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
            .edit().putBoolean(KEY_DARK, enabled).apply()

    private fun applySavedDarkMode() =
        AppCompatDelegate.setDefaultNightMode(
            if (isDarkModeEnabled()) AppCompatDelegate.MODE_NIGHT_YES
            else AppCompatDelegate.MODE_NIGHT_NO
        )

    /* ------------------------------------------------------------ */
    /* Notifikasi utils                                             */
    /* ------------------------------------------------------------ */
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
        ) requestPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
    }

    private val requestPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
            if (!granted)
                Toast.makeText(this, "Izin notifikasi ditolak", Toast.LENGTH_SHORT).show()
        }
}
