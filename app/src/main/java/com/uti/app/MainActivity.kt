package com.example.serenoteapp

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.widget.Button
import androidx.activity.viewModels
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import com.example.serenoteapp.viewmodel.NoteViewModel
import com.example.serenoteapp.viewmodel.NoteViewModelFactory
import com.example.serenoteapp.data.NoteRepository
import com.example.serenoteapp.data.NoteDatabase
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity() {

    private val noteViewModel: NoteViewModel by viewModels {
        NoteViewModelFactory(NoteRepository(NoteDatabase.getDatabase(this).noteDao()))
    }

    private var latestNotes: List<com.example.serenoteapp.data.Note> = emptyList()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // Observasi semua catatan dan simpan sementara
        lifecycleScope.launch {
            noteViewModel.allNotes.collectLatest { notes ->
                latestNotes = notes
            }
        }

        // Tombol hapus semua catatan
        findViewById<Button>(R.id.btnDeleteAll).setOnClickListener {
            AlertDialog.Builder(this)
                .setTitle("Konfirmasi")
                .setMessage("Yakin ingin menghapus semua catatan?")
                .setPositiveButton("Ya") { _, _ ->
                    noteViewModel.deleteAllNotes()
                }
                .setNegativeButton("Batal", null)
                .show()
        }

        // Tombol export catatan ke TXT
        findViewById<Button>(R.id.btnExport).setOnClickListener {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M &&
                ContextCompat.checkSelfPermission(
                    this,
                    Manifest.permission.WRITE_EXTERNAL_STORAGE
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                requestPermissions(arrayOf(Manifest.permission.WRITE_EXTERNAL_STORAGE), 100)
            } else {
                noteViewModel.exportNotesToTxt(this, latestNotes)
            }
        }
    }

    // Callback izin setelah requestPermissions
    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == 100 && grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            noteViewModel.exportNotesToTxt(this, latestNotes)
        }
    }
}
