package com.example.serenoteapp

import android.os.Bundle
import android.widget.Button
import androidx.activity.viewModels
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.example.serenoteapp.viewmodel.NoteViewModel
import com.example.serenoteapp.viewmodel.NoteViewModelFactory
import com.example.serenoteapp.data.NoteRepository
import com.example.serenoteapp.data.NoteDatabase

class MainActivity : AppCompatActivity() {

    private val noteViewModel: NoteViewModel by viewModels {
        NoteViewModelFactory(NoteRepository(NoteDatabase.getDatabase(this).noteDao()))
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

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
    }
}
