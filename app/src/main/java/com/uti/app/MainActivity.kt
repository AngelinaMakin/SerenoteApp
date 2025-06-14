package com.example.serenoteapp

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.text.InputType
import android.view.View
import android.widget.*
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.appcompat.widget.SearchView
import androidx.appcompat.widget.SwitchCompat
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
import com.google.gson.Gson
import kotlinx.coroutines.launch
import java.io.File

class MainActivity : AppCompatActivity() {

    companion object {
        private const val CHANNEL_ID = "note_channel"
        private const val PREF_NAME = "settings"
        private const val KEY_DARK = "dark_mode"
    }

    private lateinit var adapter: NoteAdapter
    private lateinit var tvTotal: TextView
    private val categories = listOf("Semua", "Kuliah", "Pribadi", "Ide", "Umum")

    private val viewModel: NoteViewModel by viewModels {
        NoteViewModelFactory(NoteRepository(NoteDatabase.getDatabase(this).noteDao()))
    }

    // ──────────────────────────────────────────────────────────────
    // FIX: simpan LiveData yang sedang di‑observe agar bisa dilepas
    // ──────────────────────────────────────────────────────────────
    private var currentSource: LiveData<List<Note>>? = null   // FIX

    @RequiresApi(Build.VERSION_CODES.M)
    override fun onCreate(savedInstanceState: Bundle?) {
        applySavedDarkMode()
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        createNotificationChannelIfNeeded()
        requestNotifPermissionIfNeeded()

        tvTotal = findViewById(R.id.tvTotalNotes)

        adapter = NoteAdapter(
            onItemClick = { handleNoteClick(it) },
            onDeleteClick = { viewModel.deleteNote(it) }
        )

        findViewById<RecyclerView>(R.id.recyclerView).apply {
            layoutManager = LinearLayoutManager(this@MainActivity)
            adapter = this@MainActivity.adapter
        }

        setupSearchView()
        setupCategorySpinner()
        setupButtons()
        observeNotes(viewModel.getActiveNotes())
    }

    private fun setupSearchView() {
        findViewById<SearchView>(R.id.searchView).setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?) = false
            override fun onQueryTextChange(text: String?): Boolean {
                val source = if (text.isNullOrBlank()) {
                    viewModel.getActiveNotes()
                } else {
                    viewModel.searchNotes(text)
                }
                observeNotes(source)
                return true
            }
        })
    }

    private fun setupCategorySpinner() {
        val spinner = findViewById<Spinner>(R.id.spinnerCategory)
        spinner.adapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, categories)
        spinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>, view: View?, position: Int, id: Long) {
                val selected = categories[position]
                val source = if (selected == "Semua") viewModel.getActiveNotes() else viewModel.getNotesByCategory(selected)
                observeNotes(source)
            }
            override fun onNothingSelected(parent: AdapterView<*>) {}
        }
    }

    @RequiresApi(Build.VERSION_CODES.M)
    private fun setupButtons() {
        findViewById<SwitchCompat>(R.id.switchDarkMode).apply {
            isChecked = isDarkModeEnabled()
            setOnCheckedChangeListener { _, isChecked ->
                saveDarkMode(isChecked)
                AppCompatDelegate.setDefaultNightMode(
                    if (isChecked) AppCompatDelegate.MODE_NIGHT_YES else AppCompatDelegate.MODE_NIGHT_NO
                )
            }
        }

        findViewById<Button>(R.id.btnDeleteAll).setOnClickListener { confirmAndDeleteAll() }
        findViewById<Button>(R.id.btnExport).setOnClickListener { exportNotesWithPermission() }
        findViewById<Button>(R.id.btnReminder).setOnClickListener { scheduleLatestNoteReminder() }
        findViewById<Button>(R.id.btnRestore).setOnClickListener { restoreNotes() }
        findViewById<Button>(R.id.btnBackup).setOnClickListener {
            viewModel.getActiveNotes().observe(this) { notes ->
                val json = Gson().toJson(notes)
                val file = File(getExternalFilesDir(null), "backup_catatan.json")
                file.writeText(json)
                Toast.makeText(this, "Backup disimpan: ${file.absolutePath}", Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun handleNoteClick(note: Note) {
        if (note.isLocked) {
            showPinDialog { isCorrect ->
                if (isCorrect) openDetail(note)
                else Toast.makeText(this, "PIN salah!", Toast.LENGTH_SHORT).show()
            }
        } else {
            openDetail(note)
        }
    }

    private fun showPinDialog(onResult: (Boolean) -> Unit) {
        val input = EditText(this).apply {
            inputType = InputType.TYPE_CLASS_NUMBER or InputType.TYPE_NUMBER_VARIATION_PASSWORD
            hint = "Masukkan PIN"
        }

        AlertDialog.Builder(this)
            .setTitle("Terkunci")
            .setView(input)
            .setPositiveButton("OK") { _, _ ->
                val storedPin = getSharedPreferences(PREF_NAME, MODE_PRIVATE)
                    .getString("pin_code", "1234")
                onResult(input.text.toString() == storedPin)
            }
            .setNegativeButton("Batal", null)
            .show()
    }

    private fun openDetail(note: Note) {
        // TODO: Navigasi ke detail jika sudah pakai Fragment
        Toast.makeText(this, "Buka detail: ${note.title}", Toast.LENGTH_SHORT).show()
    }

    // ──────────────────────────────────────────────────────────────
    // FIX: observasi LiveData tanpa error removeObservers
    // ──────────────────────────────────────────────────────────────
    private fun observeNotes(src: LiveData<List<Note>>) {
        currentSource?.removeObservers(this)  // lepaskan observer lama
        currentSource = src                   // simpan LiveData baru
        src.observe(this) { updateUI(it) }    // observe LiveData baru
    }

    private fun updateUI(list: List<Note>) {
        tvTotal.text = "Total catatan: ${list.size}"
        adapter.setData(list)
    }

    private fun confirmAndDeleteAll() {
        AlertDialog.Builder(this)
            .setTitle("Hapus semua catatan?")
            .setMessage("Tindakan ini tidak bisa dibatalkan.")
            .setPositiveButton("Hapus") { _, _ -> viewModel.deleteAllNotes() }
            .setNegativeButton("Batal", null)
            .show()
    }

    @RequiresApi(Build.VERSION_CODES.M)
    private fun exportNotesWithPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q ||
            ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED
        ) {
            viewModel.exportNotesToTxt(this)
        } else {
            requestPermissions(arrayOf(Manifest.permission.WRITE_EXTERNAL_STORAGE), 101)
        }
    }

    private fun scheduleLatestNoteReminder() {
        lifecycleScope.launch {
            val note = adapter.currentList.lastOrNull() ?: return@launch
            viewModel.scheduleReminder(this@MainActivity, note)
            Toast.makeText(this@MainActivity, "Reminder disetel!", Toast.LENGTH_SHORT).show()
        }
    }

    private fun restoreNotes() {
        viewModel.restoreNotes(this)
    }

    private fun isDarkModeEnabled(): Boolean {
        return getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
            .getBoolean(KEY_DARK, false)
    }

    private fun saveDarkMode(enabled: Boolean) {
        getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
            .edit().putBoolean(KEY_DARK, enabled).apply()
    }

    private fun applySavedDarkMode() {
        AppCompatDelegate.setDefaultNightMode(
            if (isDarkModeEnabled()) AppCompatDelegate.MODE_NIGHT_YES
            else AppCompatDelegate.MODE_NIGHT_NO
        )
    }

    private fun createNotificationChannelIfNeeded() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val manager = getSystemService(NotificationManager::class.java)
            if (manager.getNotificationChannel(CHANNEL_ID) == null) {
                val channel = NotificationChannel(
                    CHANNEL_ID,
                    "Note Reminder",
                    NotificationManager.IMPORTANCE_HIGH
                )
                manager.createNotificationChannel(channel)
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
            // boleh tambahkan aksi lain jika perlu
        }

    override fun onBackPressed() {
        super.onBackPressed()
        overridePendingTransition(R.anim.fade_in, R.anim.slide_out_right)
    }
}
