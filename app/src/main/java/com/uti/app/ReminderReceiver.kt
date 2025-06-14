package com.example.serenoteapp.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.widget.Toast

class ReminderReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val title = intent.getStringExtra("title")
        val content = intent.getStringExtra("content")

        // Tampilkan notifikasi / Toast
        Toast.makeText(context, "Reminder: $title\n$content", Toast.LENGTH_LONG).show()

        // Kamu bisa tambahkan fitur notifikasi nanti di sini
    }
}
