package com.example.serenoteapp.ui.fragment

import android.app.*
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.text.InputType
import android.view.*
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatDelegate
import androidx.appcompat.widget.SearchView
import androidx.appcompat.widget.SwitchCompat
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.serenoteapp.R
import com.example.serenoteapp.adapter.NoteAdapter
import com.example.serenoteapp.data.Note
import com.example.serenoteapp.data.NoteDatabase
import com.example.serenoteapp.data.NoteRepository
import com.example.serenoteapp.databinding.FragmentNoteListBinding
import com.example.serenoteapp.viewmodel.NoteViewModel
import com.example.serenoteapp.viewmodel.NoteViewModelFactory
import com.example.serenoteapp.receiver.ReminderReceiver
import com.google.gson.Gson
import kotlinx.coroutines.*
import java.io.File
import java.util.*


class NoteListFragment : Fragment() {

    private var _binding: FragmentNoteListBinding? = null
    private val binding get() = _binding!!

    private lateinit var noteAdapter: NoteAdapter
    private lateinit var noteViewModel: NoteViewModel

    private val gson = Gson()
    private val PREF_NAME = "settings"
    private val KEY_DARK = "dark_mode"

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentNoteListBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupViewModel()
        setupRecyclerView()
        observeNotes()
        setupListeners()
    }

    private fun setupViewModel() {
        val dao = NoteDatabase.getDatabase(requireContext()).noteDao()
        val repository = NoteRepository(dao)
        val factory = NoteViewModelFactory(repository)
        noteViewModel = ViewModelProvider(this, factory)[NoteViewModel::class.java]
    }

    private fun setupRecyclerView() {
        noteAdapter = NoteAdapter(
            onItemClick = { showNoteDetail(it) },
            onDeleteClick = { noteViewModel.delete(it) },
            onNoteUpdated = { noteViewModel.update(it) }
        )

        binding.rvNoteList.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = noteAdapter
        }
    }

    private fun observeNotes() {
        noteViewModel.allNotes.observe(viewLifecycleOwner) { notes ->
            binding.progressBar.visibility = View.GONE
            noteAdapter.setData(notes)
            binding.tvNoteCount.text = "Jumlah Catatan: ${notes.size}"
            binding.tvEmptyState.visibility = if (notes.isEmpty()) View.VISIBLE else View.GONE
        }
    }

    private fun setupListeners() {
        binding.fabAddNote.setOnClickListener {
            val action = NoteListFragmentDirections.actionNoteListFragmentToNoteAddFragment(null)
            findNavController().navigate(action)
        }

        // SearchView
        binding.searchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?) = false
            override fun onQueryTextChange(newText: String?): Boolean {
                noteViewModel.searchNotes(newText ?: "")
                return true
            }
        })

        // Spinner Filter
        binding.spinnerFilter.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, pos: Int, id: Long) {
                when (pos) {
                    0 -> noteViewModel.loadAllNotes()
                    1 -> noteViewModel.sortByTitle()
                    2 -> noteViewModel.sortByNewest()
                }
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }

        // Switch Dark Mode
        val sharedPref = requireActivity().getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        binding.switchDarkMode.isChecked = sharedPref.getBoolean(KEY_DARK, false)

        binding.switchDarkMode.setOnCheckedChangeListener { _, isChecked ->
            sharedPref.edit().putBoolean(KEY_DARK, isChecked).apply()
            AppCompatDelegate.setDefaultNightMode(
                if (isChecked) AppCompatDelegate.MODE_NIGHT_YES
                else AppCompatDelegate.MODE_NIGHT_NO
            )
        }

        // Backup Button
        binding.btnBackup.setOnClickListener {
            backupNotesToFile()
        }

        // Import Button
        binding.btnImport.setOnClickListener {
            importNotesFromFile()
        }

        // Reminder Button
        binding.btnReminder.setOnClickListener {
            showReminderDialog()
        }
    }

    private fun backupNotesToFile() {
        CoroutineScope(Dispatchers.IO).launch {
            val notes = noteViewModel.allNotes.value ?: emptyList()
            val json = gson.toJson(notes)
            val file = File(requireContext().filesDir, "backup_notes.json")
            file.writeText(json)

            withContext(Dispatchers.Main) {
                Toast.makeText(requireContext(), "Backup berhasil", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun importNotesFromFile() {
        CoroutineScope(Dispatchers.IO).launch {
            val file = File(requireContext().filesDir, "backup_notes.json")
            if (file.exists()) {
                val json = file.readText()
                val notes = gson.fromJson(json, Array<Note>::class.java).toList()
                noteViewModel.insertAll(notes)

                withContext(Dispatchers.Main) {
                    Toast.makeText(requireContext(), "Import berhasil", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun showReminderDialog() {
        val builder = AlertDialog.Builder(requireContext())
        builder.setTitle("Atur pengingat")

        val input = EditText(requireContext()).apply {
            hint = "Waktu dalam detik"
            inputType = InputType.TYPE_CLASS_NUMBER
        }

        builder.setView(input)

        builder.setPositiveButton("Set") { _, _ ->
            val timeInSeconds = input.text.toString().toIntOrNull() ?: return@setPositiveButton
            scheduleReminder(timeInSeconds)
        }

        builder.setNegativeButton("Batal", null)
        builder.show()
    }

    private fun scheduleReminder(seconds: Int) {
        val intent = Intent(requireContext(), ReminderReceiver::class.java)
        val pendingIntent = PendingIntent.getBroadcast(
            requireContext(), 0, intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val alarmManager = requireContext().getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val triggerAt = System.currentTimeMillis() + seconds * 1000
        alarmManager.setExact(AlarmManager.RTC_WAKEUP, triggerAt, pendingIntent)

        Toast.makeText(requireContext(), "Pengingat disetel $seconds detik dari sekarang", Toast.LENGTH_SHORT).show()
    }

    private fun showNoteDetail(note: Note) {
        val action = NoteListFragmentDirections.actionNoteListFragmentToNoteAddFragment(note)
        findNavController().navigate(action)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}