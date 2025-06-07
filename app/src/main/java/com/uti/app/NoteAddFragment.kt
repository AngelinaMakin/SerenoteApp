package com.example.serenoteapp.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import com.example.serenoteapp.data.NoteDatabase
import com.example.serenoteapp.data.NoteRepository
import com.example.serenoteapp.databinding.FragmentNoteAddBinding
import com.example.serenoteapp.viewmodel.NoteViewModel
import com.example.serenoteapp.viewmodel.NoteViewModelFactory
import androidx.navigation.fragment.findNavController
import com.example.serenoteapp.data.Note


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


        binding.btnSave.setOnClickListener {
            val title = binding.etTitle.text.toString()
            val content = binding.etContent.text.toString()

            if (title.isNotBlank() && content.isNotBlank()) {
                val note = Note(
                    title = title,
                    content = content,
                    timestamp = System.currentTimeMillis()
                )
                noteViewModel.insertNote(note)
                findNavController().navigateUp()
            }
        }



        return binding.root
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
