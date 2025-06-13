package com.example.serenoteapp

import android.os.Bundle
import android.widget.Button
import androidx.activity.viewModels
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.serenoteapp.viewmodel.NoteViewModel
import com.example.serenoteapp.viewmodel.NoteViewModelFactory
import com.example.serenoteapp.data.NoteRepository
import com.example.serenoteapp.data.NoteDatabase
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity() {

    private val noteViewModel: NoteViewModel by viewModels {
        NoteViewModelFactory(NoteRepository(NoteDatabase.getDatabase(this).noteDao()))
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // Tombol hapus semua catatan
        findViewById<Button>(R.id.btnDeleteAll).setOnClickListener {
            AlertDialog.Builder(this)
                .setTitle("Konfirmasi")
                .setMessage("Yakin ingin menghapus semua catatan?")
                .setPositiveButton("Ya") { _, _ ->
                    noteViewModel.deleteAllNotes()
                }
                .setNegativeButton("Batal", null)
                .show()
        }

        // Tombol export catatan ke TXT
        findViewById<Button>(R.id.btnExport).setOnClickListener {
            lifecycleScope.launch {
                noteViewModel.allNotes.collectLatest { notes ->
                    noteViewModel.exportNotesToTxt(this@MainActivity)
                }
            }
        }
    }
}
