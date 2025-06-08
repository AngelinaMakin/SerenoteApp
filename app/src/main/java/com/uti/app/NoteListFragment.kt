package com.example.serenoteapp.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.serenoteapp.R
import com.example.serenoteapp.adapter.NoteAdapter
import com.example.serenoteapp.data.NoteDatabase
import com.example.serenoteapp.data.NoteRepository
import com.example.serenoteapp.databinding.FragmentNoteListBinding
import com.example.serenoteapp.viewmodel.NoteViewModel
import com.example.serenoteapp.viewmodel.NoteViewModelFactory
import kotlinx.coroutines.flow.collectLatest
import androidx.recyclerview.widget.DividerItemDecoration


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

        noteAdapter = NoteAdapter(
            onItemClick = { selectedNote ->
                val action = NoteListFragmentDirections.actionNoteListFragmentToNoteAddFragment(selectedNote)
                findNavController().navigate(action)
            },
            onDeleteClick = { selectedNote ->
                noteViewModel.deleteNote(selectedNote)
            }
        )

        binding.rvNotes.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = noteAdapter

            addItemDecoration(
                DividerItemDecoration(requireContext(), DividerItemDecoration.VERTICAL)
            )
        }


        //Observe StateFlow catatan dan tampilkan dengan NoteAdapter
        lifecycleScope.launchWhenStarted {
            noteViewModel.allNotes.collectLatest { notes ->
                noteAdapter.submitList(notes)
            }
        }

        binding.fabAdd.setOnClickListener {
            val action = NoteListFragmentDirections.actionNoteListFragmentToNoteAddFragment(null)
            findNavController().navigate(action)
        }

        return binding.root
    }
}

//updatean seluruh