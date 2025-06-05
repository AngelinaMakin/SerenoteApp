package com.example.serenoteapp

import android.os.Bundle
import android.widget.EditText
import android.widget.LinearLayout
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.serenoteapp.adapter.NoteAdapter
import com.example.serenoteapp.data.NoteDatabase
import com.example.serenoteapp.databinding.ActivityMainBinding
import com.example.serenoteapp.model.Note
import com.example.serenoteapp.repository.NoteRepository
import com.example.serenoteapp.viewmodel.NoteViewModel
import com.example.serenoteapp.viewmodel.NoteViewModelFactory



class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var noteViewModel: NoteViewModel
    private lateinit var noteAdapter: NoteAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Setup ViewModel
        val dao = NoteDatabase.getDatabase(this).noteDao()
        val repository = NoteRepository(dao)
        val factory = NoteViewModelFactory(repository)
        noteViewModel = ViewModelProvider(this, factory)[NoteViewModel::class.java]

        // Setup Adapter
        noteAdapter = NoteAdapter(
            onDeleteClick = { note -> noteViewModel.delete(note) },
            onEditClick = { note -> showEditDialog(note) }
        )

        binding.recyclerView.apply {
            layoutManager = LinearLayoutManager(this@MainActivity)
            adapter = noteAdapter
        }

        // Observe notes
        noteViewModel.allNotes.observe(this) { notes ->
            noteAdapter.submitList(notes)
        }

        binding.fabAdd.setOnClickListener {
            showAddDialog()
        }
    }

    private fun showAddDialog() {
        val editTitle = EditText(this).apply { hint = "Judul catatan" }
        val editContent = EditText(this).apply { hint = "Isi catatan" }

        val layout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(16, 16, 16, 16)
            addView(editTitle)
            addView(editContent)
        }

        AlertDialog.Builder(this)
            .setTitle("Tambah Catatan")
            .setView(layout)
            .setPositiveButton("Simpan") { _, _ ->
                val title = editTitle.text.toString()
                val content = editContent.text.toString()
                if (title.isNotEmpty() && content.isNotEmpty()) {
                    val note = Note(title = title, content = content)
                    noteViewModel.insert(note)
                }
            }
            .setNegativeButton("Batal", null)
            .show()
    }

    private fun showEditDialog(note: Note) {
        val editTitle = EditText(this).apply { setText(note.title) }
        val editContent = EditText(this).apply { setText(note.content) }

        val layout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(16, 16, 16, 16)
            addView(editTitle)
            addView(editContent)
        }

        AlertDialog.Builder(this)
            .setTitle("Edit Catatan")
            .setView(layout)
            .setPositiveButton("Update") { _, _ ->
                val newTitle = editTitle.text.toString()
                val newContent = editContent.text.toString()
                if (newTitle.isNotEmpty() && newContent.isNotEmpty()) {
                    val updatedNote = note.copy(title = newTitle, content = newContent)
                    noteViewModel.update(updatedNote)
                }
            }
            .setNegativeButton("Batal", null)
            .show()
    }
}
