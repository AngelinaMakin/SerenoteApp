package com.example.serenoteapp.ui.fragment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.example.serenoteapp.databinding.FragmentNoteDetailBinding
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

        val noteId = arguments?.getInt("noteId")
        val noteTitle = arguments?.getString("noteTitle")
        val noteContent = arguments?.getString("noteContent")
        val createdAt = arguments?.getLong("noteCreatedAt") ?: 0L
        val updatedAt = arguments?.getLong("noteUpdatedAt") ?: 0L

        // Format tanggal
        val createdDate = SimpleDateFormat("dd MMM yyyy HH:mm", Locale.getDefault()).format(Date(createdAt))
        val updatedDate = SimpleDateFormat("dd MMM yyyy HH:mm", Locale.getDefault()).format(Date(updatedAt))

        // Set data ke tampilan
        binding.tvTitle.text = noteTitle
        binding.tvContent.text = noteContent
        binding.tvCreatedAt.text = "Dibuat: $createdDate"
        binding.tvUpdatedAt.text = "Diperbarui: $updatedDate"
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}