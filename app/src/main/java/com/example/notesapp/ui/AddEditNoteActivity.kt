package com.example.notesapp.ui

import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.notesapp.data.dao.NoteDao
import com.example.notesapp.data.db.NotesDatabaseHelper
import com.example.notesapp.data.model.Note
import com.example.notesapp.databinding.ActivityAddEditNoteBinding
import java.util.*

class AddEditNoteActivity : AppCompatActivity() {

    private lateinit var binding: ActivityAddEditNoteBinding
    private lateinit var dbHelper: NotesDatabaseHelper
    private var editingNoteId: String? = null  // ID untuk mendeteksi mode edit

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAddEditNoteBinding.inflate(layoutInflater)
        setContentView(binding.root)

        dbHelper = NotesDatabaseHelper(this)

        // Cek apakah ada data note yang dikirim dari MainActivity
        editingNoteId = intent.getStringExtra("EXTRA_NOTE_ID")
        if (editingNoteId != null) {
            val note = NoteDao.getById(dbHelper.readableDatabase, editingNoteId!!)
            note?.let {
                binding.editTitle.setText(it.title)
                binding.editContent.setText(it.content)
            }
        }

        // Tombol back
        binding.btnBack.setOnClickListener {
            finish()
        }

        // Tombol simpan
        binding.btnSave.setOnClickListener {
            val title = binding.editTitle.text.toString()
            val content = binding.editContent.text.toString()

            if (title.isNotBlank()) {
                val note = Note(
                    id = editingNoteId ?: UUID.randomUUID().toString(),
                    title = title,
                    content = content
                )

                if (editingNoteId == null) {
                    NoteDao.insert(dbHelper.writableDatabase, note)
                    Toast.makeText(this, "Catatan disimpan", Toast.LENGTH_SHORT).show()
                } else {
                    NoteDao.update(dbHelper.writableDatabase, note)
                    Toast.makeText(this, "Catatan diperbarui", Toast.LENGTH_SHORT).show()
                }

                finish()
            } else {
                Toast.makeText(this, "Judul tidak boleh kosong", Toast.LENGTH_SHORT).show()
            }
        }
    }
}
