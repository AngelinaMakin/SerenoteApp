package com.example.serenoteapp.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import com.example.serenoteapp.data.Note
import com.example.serenoteapp.data.NoteDatabase
import com.example.serenoteapp.data.NoteRepository
import com.example.serenoteapp.databinding.FragmentNoteAddBinding
import com.example.serenoteapp.viewmodel.NoteViewModel
import com.example.serenoteapp.viewmodel.NoteViewModelFactory

class NoteAddFragment : Fragment() {

    private var _binding: FragmentNoteAddBinding? = null
    private val binding get() = _binding!!

    private val noteViewModel: NoteViewModel by viewModels {
        NoteViewModelFactory(
            NoteRepository(NoteDatabase.getDatabase(requireContext()).noteDao())
        )
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _binding = FragmentNoteAddBinding.inflate(inflater, container, false)

        val noteArg = arguments?.let { NoteAddFragmentArgs.fromBundle(it).note }

        noteArg?.let { note ->
            binding.etTitle.setText(note.title)
            binding.etContent.setText(note.content)
            binding.etCategory.setText(note.category) // ✅ jika edit, isi kategori lama
        }

        binding.btnSave.setOnClickListener {
            val title = binding.etTitle.text.toString()
            val content = binding.etContent.text.toString()
            val category = binding.etCategory.text.toString().ifBlank { "Umum" } // ✅ default kategori

            val updatedNote = noteArg?.copy(
                title = title,
                content = content,
                category = category,
                updatedAt = System.currentTimeMillis()
            )

            if (updatedNote != null) {
                noteViewModel.updateNote(updatedNote)
            } else {
                val newNote = Note(
                    title = title,
                    content = content,
                    category = category,
                    timestamp = System.currentTimeMillis(),
                    createdAt = System.currentTimeMillis(),
                    updatedAt = System.currentTimeMillis()
                )
                noteViewModel.insertNote(newNote)
            }

            findNavController().popBackStack()
        }

        return binding.root
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
