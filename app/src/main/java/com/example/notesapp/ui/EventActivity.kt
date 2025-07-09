package com.example.notesapp.ui

import android.content.Intent
import android.os.Bundle
import android.widget.CalendarView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.notesapp.R
import com.example.notesapp.databinding.ActivityEventBinding // Import Binding
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

class EventActivity : AppCompatActivity() {

    private lateinit var binding: ActivityEventBinding
    private lateinit var calendarView: CalendarView
    private lateinit var selectedDateText: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityEventBinding.inflate(layoutInflater)
        setContentView(binding.root)

        calendarView = binding.calendarView
        selectedDateText = binding.selectedDateText

        val today = Calendar.getInstance()
        val dateFormat = SimpleDateFormat("dd MMMM yyyy", Locale("id", "ID"))
        selectedDateText.text = "Selected Date: ${dateFormat.format(today.time)}"


        calendarView.setOnDateChangeListener { view, year, month, dayOfMonth ->
            val calendar = Calendar.getInstance()
            calendar.set(year, month, dayOfMonth)

            val selectedDate = dateFormat.format(calendar.time)
            selectedDateText.text = "Selected Date: $selectedDate"

            Toast.makeText(this, "Date selected: $selectedDate", Toast.LENGTH_SHORT).show()
        }

        // --- Perbarui ini untuk menggunakan binding.bottomNavigation ---
        binding.bottomNavigation.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.menu_notes -> {
                    val intent = Intent(this, MainActivity::class.java).apply {
                        flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
                    }
                    startActivity(intent)
                    finish()
                    true
                }
                R.id.menu_event -> {
                    // Anda sudah berada di EventActivity, tidak perlu melakukan apa-apa
                    true
                }
                R.id.menu_setting -> {
                    val intent = Intent(this, SettingsActivity::class.java)
                    startActivity(intent)
                    true
                }
                else -> false
            }
        }

        // Opsional: Atur item terpilih saat EventActivity pertama kali dibuka
        binding.bottomNavigation.selectedItemId = R.id.menu_event
    }
}