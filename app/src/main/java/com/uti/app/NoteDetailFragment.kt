package com.example.serenoteapp.ui.fragment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import com.example.serenoteapp.data.Note
import com.example.serenoteapp.data.NoteDatabase
import com.example.serenoteapp.data.NoteRepository
import com.example.serenoteapp.databinding.FragmentNoteDetailBinding
import com.example.serenoteapp.viewmodel.NoteViewModel
import com.example.serenoteapp.viewmodel.NoteViewModelFactory

class NoteDetailFragment : Fragment() {

    private var _binding: FragmentNoteDetailBinding? = null
    private val binding get() = _binding!!

    private lateinit var noteViewModel: NoteViewModel
    private var noteId: Int? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentNoteDetailBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Setup ViewModel
        val dao = NoteDatabase.getDatabase(requireContext()).noteDao()
        val repository = NoteRepository(dao)
        val factory = NoteViewModelFactory(repository)
        noteViewModel = ViewModelProvider(this, factory)[NoteViewModel::class.java]

        // Ambil data dari arguments
        val title = arguments?.getString("noteTitle") ?: ""
        val content = arguments?.getString("noteContent") ?: ""
        noteId = arguments?.getInt("noteId")

        binding.etTitle.setText(title)
        binding.etContent.setText(content)

        // Tombol simpan/update
        binding.btnSave.setOnClickListener {
            val newTitle = binding.etTitle.text.toString()
            val newContent = binding.etContent.text.toString()

            if (newTitle.isNotBlank() && newContent.isNotBlank()) {
                val updatedNote = Note(
                    id = noteId ?: 0,
                    title = newTitle,
                    content = newContent,
                    createdAt = arguments?.getLong("noteCreatedAt") ?: System.currentTimeMillis(),
                    updatedAt = System.currentTimeMillis()
                )

                noteViewModel.update(updatedNote)
                Toast.makeText(requireContext(), "Catatan berhasil diperbarui", Toast.LENGTH_SHORT).show()
                parentFragmentManager.popBackStack() // Kembali ke daftar
            } else {
                Toast.makeText(requireContext(), "Judul dan isi tidak boleh kosong", Toast.LENGTH_SHORT).show()
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
