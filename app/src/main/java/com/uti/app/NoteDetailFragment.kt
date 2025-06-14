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
import java.util.Date
import java.util.Locale

class NoteDetailFragment : Fragment() {

    private var _binding: FragmentNoteDetailBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentNoteDetailBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        /* ────────────────────────
         * Ambil data dari argument
         * ──────────────────────── */
        val noteId        = arguments?.getInt("noteId")      ?: return   // jika null, keluar
        val originalTitle = arguments?.getString("noteTitle") ?: ""
        val originalContent = arguments?.getString("noteContent") ?: ""
        val createdAt     = arguments?.getLong("noteCreatedAt") ?: 0L
        val updatedAtArg  = arguments?.getLong("noteUpdatedAt") ?: 0L
        val nowUpdatedAt  = System.currentTimeMillis()

        /* ────────────────────────
         * Tampilkan data awal
         * ──────────────────────── */
        // EditText (kamu perlu pastikan etTitle & etContent ada di XML)
        binding.etTitle.setText(originalTitle)
        binding.etContent.setText(originalContent)

        // TextView tanggal buat & update
        val sdf = SimpleDateFormat("dd MMM yyyy HH:mm", Locale.getDefault())
        binding.tvCreatedAt.text   = "Dibuat: ${sdf.format(Date(createdAt))}"
        binding.tvUpdatedAt.text   = "Diperbarui: ${sdf.format(Date(updatedAtArg))}"

        /* ────────────────────────
         * Inisialisasi ViewModel
         * ──────────────────────── */
        val dao       = NoteDatabase.getDatabase(requireContext()).noteDao()
        val repo      = NoteRepository(dao)
        val factory   = NoteViewModelFactory(repo)
        val noteVm    = ViewModelProvider(this, factory)[NoteViewModel::class.java]

        /* ────────────────────────
         * Tombol Update
         * ──────────────────────── */
        binding.btnUpdateNote.setOnClickListener {
            val updatedTitle   = binding.etTitle.text.toString()
            val updatedContent = binding.etContent.text.toString()

            if (updatedTitle.isNotEmpty() && updatedContent.isNotEmpty()) {
                val updatedNote = Note(
                    id        = noteId,
                    title     = updatedTitle,
                    content   = updatedContent,
                    createdAt = createdAt,
                    updatedAt = nowUpdatedAt
                )

                noteVm.update(updatedNote)
                Toast.makeText(requireContext(), "Catatan diperbarui", Toast.LENGTH_SHORT).show()
                parentFragmentManager.popBackStack() // kembali ke list
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
