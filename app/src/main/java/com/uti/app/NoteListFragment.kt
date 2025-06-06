package com.example.serenoteapp.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.serenoteapp.adapter.NoteAdapter
import com.example.serenoteapp.data.NoteDatabase
import com.example.serenoteapp.data.NoteRepository
import com.example.serenoteapp.databinding.FragmentNoteListBinding
import com.example.serenoteapp.viewmodel.NoteViewModel
import com.example.serenoteapp.viewmodel.NoteViewModelFactory

class NoteListFragment : Fragment() {

    private lateinit var binding: FragmentNoteListBinding
    private lateinit var noteAdapter: NoteAdapter

    private val noteViewModel: NoteViewModel by viewModels {
        NoteViewModelFactory(
            NoteRepository(NoteDatabase.getDatabase(requireContext()).noteDao())
        )
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentNoteListBinding.inflate(inflater, container, false)

        noteAdapter = NoteAdapter() // buat adapter

        binding.rvNotes.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = noteAdapter
        }
        return binding.root
    }
}
