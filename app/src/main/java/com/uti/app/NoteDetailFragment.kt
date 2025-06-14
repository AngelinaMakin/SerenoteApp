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
import java.text.SimpleDateFormat
import java.util.*

class NoteDetailFragment : Fragment() {

    private var _binding: FragmentNoteDetailBinding? = null
    private val binding get() = _binding!!

    private lateinit var noteViewModel: NoteViewModel
    private var note: Note? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentNoteDetailBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Init ViewModel
        val dao = NoteDatabase.getDatabase(requireContext()).noteDao()
        val factory = NoteViewModelFactory(NoteRepository(dao))
        noteViewModel = ViewModelProvider(this, factory)[NoteViewModel::class.java]

        /* ──────────────── Ambil data dari arguments ──────────────── */
        arguments?.let { bundle ->
            val id        = bundle.getInt("noteId")
            val title     = bundle.getString("noteTitle") ?: ""
            val content   = bundle.getString("noteContent") ?: ""
            val createdAt = bundle.getLong("noteCreatedAt")
            val updatedAt = bundle.getLong("noteUpdatedAt")

            note = Note(id, title, content, createdAt, updatedAt)

            // Isi UI
            binding.etTitle.setText(title)
            binding.etContent.setText(content)
            binding.tvCreatedAt.text  = "Dibuat: ${formatDate(createdAt)}"
            binding.tvUpdatedAt.text  = "Terakhir diubah: ${formatDate(updatedAt)}"
        }

        /* ──────────────── Tombol SIMPAN (btnSave) ──────────────── */
        binding.btnSave.setOnClickListener {
            val newTitle   = binding.etTitle.text.toString().trim()
            val newContent = binding.etContent.text.toString().trim()

            if (newTitle.isEmpty() || newContent.isEmpty()) {
                Toast.makeText(requireContext(), "Judul dan isi tidak boleh kosong", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            note?.let { current ->
                val updatedNote = current.copy(
                    title     = newTitle,
                    content   = newContent,
                    updatedAt = System.currentTimeMillis()
                )
                noteViewModel.update(updatedNote)
                Toast.makeText(requireContext(), "Catatan diperbarui", Toast.LENGTH_SHORT).show()
                parentFragmentManager.popBackStack()
            }
        }

        /* ──────────────── Tombol Kembali ──────────────── */
        binding.btnBack.setOnClickListener {
            requireActivity().onBackPressedDispatcher.onBackPressed()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    private fun formatDate(ts: Long): String =
        SimpleDateFormat("dd MMM yyyy, HH:mm", Locale.getDefault()).format(Date(ts))
}
