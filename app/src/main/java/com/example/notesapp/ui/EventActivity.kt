package com.example.notesapp.ui

import android.content.Intent
import android.os.Bundle
import android.widget.CalendarView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.notesapp.R
import com.example.notesapp.data.database.NotesDatabaseHelper
import com.example.notesapp.data.dao.EventDao
import com.example.notesapp.data.model.Event
import com.example.notesapp.databinding.ActivityEventBinding
import com.example.notesapp.ui.adapter.EventAdapter
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import android.app.AlertDialog
import android.graphics.Color
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import androidx.core.content.ContextCompat


class EventActivity : AppCompatActivity() {

    private lateinit var binding: ActivityEventBinding
    private lateinit var calendarView: CalendarView
    private lateinit var selectedDateText: TextView
    private lateinit var eventsRecyclerView: RecyclerView
    private lateinit var noEventsTextView: TextView // Untuk pesan "Tidak ada event"
    private lateinit var eventAdapter: EventAdapter
    private lateinit var dbHelper: NotesDatabaseHelper
    private lateinit var selectedDate: String // Untuk menyimpan tanggal yang sedang dipilih

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityEventBinding.inflate(layoutInflater)
        setContentView(binding.root)

        dbHelper = NotesDatabaseHelper(this)

        calendarView = binding.calendarView
        selectedDateText = binding.selectedDateText
        eventsRecyclerView = binding.eventsRecyclerView
        noEventsTextView = binding.noEventsTextView
        val addEventFab = binding.addEventFab // Referensi FAB

        // Inisialisasi RecyclerView
        eventAdapter = EventAdapter(emptyList(), { event ->
            // Handle klik pada event (misal: edit event)
            Toast.makeText(this, "Event clicked: ${event.title}", Toast.LENGTH_SHORT).show()
            showAddEventDialog(event) // Buka dialog untuk edit
        }, { event ->
            // Handle long click pada event (misal: delete event)
            showDeleteEventConfirmationDialog(event)
        })
        eventsRecyclerView.layoutManager = LinearLayoutManager(this)
        eventsRecyclerView.adapter = eventAdapter

        // Set tanggal awal saat activity dibuat (hari ini)
        val today = Calendar.getInstance()
        val dateFormat = SimpleDateFormat("dd MMMM yyyy", Locale("id", "ID"))
        val dbDateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()) // Format untuk DB
        selectedDate = dbDateFormat.format(today.time) // Simpan tanggal hari ini dalam format DB
        selectedDateText.text = "Selected Date: ${dateFormat.format(today.time)}"
        displayEventsForSelectedDate() // Tampilkan event untuk hari ini

        calendarView.setOnDateChangeListener { view, year, month, dayOfMonth ->
            val calendar = Calendar.getInstance()
            calendar.set(year, month, dayOfMonth)

            val displayDate = dateFormat.format(calendar.time)
            selectedDateText.text = "Selected Date: $displayDate"

            selectedDate = dbDateFormat.format(calendar.time) // Update tanggal yang dipilih
            displayEventsForSelectedDate() // Tampilkan event untuk tanggal yang baru dipilih
        }

        // Listener untuk Floating Action Button
        addEventFab.setOnClickListener {
            showAddEventDialog() // Panggil dialog untuk menambah event baru
        }

        // --- BOTTOM NAVIGATION LOGIC (sudah ada dari sebelumnya, pastikan konsisten) ---
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
                    true // Sudah di EventActivity
                }
                R.id.menu_setting -> {
                    val intent = Intent(this, SettingsActivity::class.java)
                    startActivity(intent)
                    true
                }
                else -> false
            }
        }
        binding.bottomNavigation.selectedItemId = R.id.menu_event
    }

    // --- Fungsi untuk menampilkan event berdasarkan tanggal yang dipilih ---
    private fun displayEventsForSelectedDate() {
        val db = dbHelper.readableDatabase
        val events = EventDao.getEventsByDate(db, selectedDate)
        eventAdapter.updateEvents(events)

        if (events.isEmpty()) {
            noEventsTextView.visibility = View.VISIBLE
            eventsRecyclerView.visibility = View.GONE
        } else {
            noEventsTextView.visibility = View.GONE
            eventsRecyclerView.visibility = View.VISIBLE
        }
    }

    // --- Fungsi untuk menampilkan dialog tambah/edit event ---
    private fun showAddEventDialog(eventToEdit: Event? = null) {
        val dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_add_event, null)
        val textInputLayoutTitle = dialogView.findViewById<TextInputLayout>(R.id.textInputLayoutEventTitle)
        val editTitle = dialogView.findViewById<TextInputEditText>(R.id.editEventTitle)
        val textInputLayoutDescription = dialogView.findViewById<TextInputLayout>(R.id.textInputLayoutEventDescription)
        val editDescription = dialogView.findViewById<TextInputEditText>(R.id.editEventDescription)
        val textInputLayoutTime = dialogView.findViewById<TextInputLayout>(R.id.textInputLayoutEventTime)
        val editTime = dialogView.findViewById<TextInputEditText>(R.id.editEventTime)

        // Isi data jika sedang dalam mode edit
        eventToEdit?.let {
            editTitle.setText(it.title)
            editDescription.setText(it.description)
            editTime.setText(it.time)
            dialogView.findViewById<TextView>(R.id.textInputLayoutEventTitle).text = "Edit Event" // Ganti judul dialog
        }

        // Tambahkan TextWatcher untuk format waktu (opsional, tapi bagus untuk UX)
        editTime.addTextChangedListener(object : TextWatcher {
            private var isFormatting = false
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}

            override fun afterTextChanged(s: Editable?) {
                if (isFormatting) return
                isFormatting = true

                val input = s.toString().replace(":", "")
                if (input.length >= 2 && !s.toString().contains(":")) {
                    s?.insert(2, ":")
                }
                isFormatting = false
            }
        })


        val alertDialog = AlertDialog.Builder(this)
            .setView(dialogView)
            .setPositiveButton("Simpan", null) // Null agar bisa handle klik manual
            .setNegativeButton("Batal") { dialog, _ ->
                dialog.dismiss()
            }
            .create()

        alertDialog.show()

        alertDialog.getButton(AlertDialog.BUTTON_POSITIVE)?.setTextColor(ContextCompat.getColor(this, R.color.green_active))
        alertDialog.getButton(AlertDialog.BUTTON_NEGATIVE)?.setTextColor(ContextCompat.getColor(this, R.color.gray))

        val positiveButton = alertDialog.getButton(AlertDialog.BUTTON_POSITIVE)
        positiveButton.setOnClickListener {
            val title = editTitle.text.toString().trim()
            val description = editDescription.text.toString().trim()
            val time = editTime.text.toString().trim()

            var isValid = true

            if (title.isBlank()) {
                textInputLayoutTitle.error = "Judul event tidak boleh kosong"
                isValid = false
            } else {
                textInputLayoutTitle.error = null
            }

            // Validasi format waktu (opsional: HH:MM)
            if (time.isNotBlank() && !time.matches(Regex("^([01]?[0-9]|2[0-3]):[0-5][0-9]$"))) {
                textInputLayoutTime.error = "Format waktu harus HH:MM (misal: 14:30)"
                isValid = false
            } else {
                textInputLayoutTime.error = null
            }

            if (isValid) {
                val db = dbHelper.writableDatabase
                if (eventToEdit == null) { // Mode tambah baru
                    val newEvent = Event(
                        title = title,
                        description = description,
                        date = selectedDate, // Gunakan tanggal yang sedang dipilih
                        time = if (time.isNotBlank()) time else null
                    )
                    EventDao.insert(db, newEvent)
                    Toast.makeText(this, "Event '$title' berhasil ditambahkan!", Toast.LENGTH_SHORT).show()
                } else { // Mode edit
                    val updatedEvent = eventToEdit.copy(
                        title = title,
                        description = description,
                        time = if (time.isNotBlank()) time else null
                    )
                    EventDao.update(db, updatedEvent)
                    Toast.makeText(this, "Event '$title' berhasil diperbarui!", Toast.LENGTH_SHORT).show()
                }
                displayEventsForSelectedDate() // Refresh daftar event
                alertDialog.dismiss()
            }
        }
    }

    private fun showDeleteEventConfirmationDialog(event: Event) {
        val alertDialog = AlertDialog.Builder(this)
            .setTitle("Hapus Event")
            .setMessage("Apakah Anda yakin ingin menghapus event '${event.title}'?")
            .setPositiveButton("Hapus") { _, _ ->
                val db = dbHelper.writableDatabase
                val rowsAffected = EventDao.delete(db, event.id)
                if (rowsAffected > 0) {
                    Toast.makeText(this, "Event '${event.title}' berhasil dihapus.", Toast.LENGTH_SHORT).show()
                    displayEventsForSelectedDate() // Refresh daftar event
                } else {
                    Toast.makeText(this, "Gagal menghapus event.", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("Batal") { dialog, _ ->
                dialog.dismiss()
            }
            .show()

        alertDialog.getButton(androidx.appcompat.app.AlertDialog.BUTTON_POSITIVE)?.setTextColor(ContextCompat.getColor(this, R.color.red_active))
        alertDialog.getButton(androidx.appcompat.app.AlertDialog.BUTTON_NEGATIVE)?.setTextColor(ContextCompat.getColor(this, R.color.gray))
    }
}