package com.example.serenoteapp.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.widget.SearchView
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
import android.view.inputmethod.InputMethodManager
import android.content.Context


class NoteListFragment : Fragment() {

    private var _binding: FragmentNoteListBinding? = null
    private val binding get() = _binding!!
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
        _binding = FragmentNoteListBinding.inflate(inflater, container, false)


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

        binding.searchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?): Boolean {
                query?.let { noteAdapter.filter(it) }

                val imm = requireContext().getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
                imm.hideSoftInputFromWindow(binding.searchView.windowToken, 0)

                return true
            }

            override fun onQueryTextChange(newText: String?): Boolean {
                newText?.let { noteAdapter.filter(it)
                    binding.tvEmptyState.visibility = if (noteAdapter.itemCount == 0) View.VISIBLE else View.GONE
                }

                return true
            }
        })

            //Observe StateFlow catatan dan tampilkan dengan NoteAdapter
        lifecycleScope.launchWhenStarted {
            noteViewModel.allNotes.collectLatest { notes ->
                binding.progressBar.visibility = View.GONE //tampilkan progressbar
                noteAdapter.setData(notes)

                if (notes.isEmpty()) {
                    binding.tvEmptyState.visibility = View.VISIBLE
                } else {
                    binding.tvEmptyState.visibility = View.GONE
                }

            }
        }

        binding.fabAdd.setOnClickListener {
            val action = NoteListFragmentDirections.actionNoteListFragmentToNoteAddFragment(null)
            findNavController().navigate(action)
        }

        return binding.root
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null

        }
}
