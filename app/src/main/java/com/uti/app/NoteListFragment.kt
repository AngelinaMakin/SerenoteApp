package com.example.serenoteapp.ui.fragment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.serenoteapp.adapter.NoteAdapter
import com.example.serenoteapp.data.NoteDatabase
import com.example.serenoteapp.data.NoteRepository
import com.example.serenoteapp.databinding.FragmentNoteListBinding
import com.example.serenoteapp.data.Note
import com.example.serenoteapp.viewmodel.NoteViewModel
import com.example.serenoteapp.viewmodel.NoteViewModelFactory

class NoteListFragment : Fragment() {

    private var _binding: FragmentNoteListBinding? = null
    private val binding get() = _binding!!

    private lateinit var noteAdapter: NoteAdapter
    private lateinit var noteViewModel: NoteViewModel

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentNoteListBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Inisialisasi ViewModel
        val dao = NoteDatabase.getDatabase(requireContext()).noteDao()
        val repository = NoteRepository(dao)
        val factory = NoteViewModelFactory(repository)
        noteViewModel = ViewModelProvider(this, factory)[NoteViewModel::class.java]

        // Inisialisasi Adapter
        noteAdapter = NoteAdapter(
            onItemClick = { note -> showNoteDetail(note) },
            onDeleteClick = { note -> noteViewModel.delete(note) },
            onNoteUpdated = { note -> noteViewModel.update(note) }
        )

        // Setup RecyclerView
        binding.rvNoteList.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = noteAdapter
        }

        // Observasi data LiveData
        noteViewModel.allNotes.observe(viewLifecycleOwner) { notes ->
            binding.progressBar.visibility = View.GONE
            noteAdapter.setData(notes)

            binding.tvNoteCount.text = "Jumlah Catatan: ${notes.size}"
            binding.tvEmptyState.visibility = if (notes.isEmpty()) View.VISIBLE else View.GONE
        }

        // Klik tambah catatan
        binding.fabAddNote.setOnClickListener {
            Toast.makeText(requireContext(), "Tambah Catatan ditekan", Toast.LENGTH_SHORT).show()
            // Navigasi ke NoteAddFragment bisa ditambah di sini kalau pakai Navigation Component
        }
    }

    // Fungsi untuk menampilkan detail catatan
    private fun showNoteDetail(note: Note) {
        Toast.makeText(requireContext(), "Judul: ${note.title}", Toast.LENGTH_SHORT).show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
