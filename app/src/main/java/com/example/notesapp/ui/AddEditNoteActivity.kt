package com.example.notesapp.ui

import android.os.Bundle
import android.widget.ArrayAdapter
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
    private var editingNoteId: String? = null

    // Dummy kategori (nama ke ID mapping)
    private val categoryList = listOf("Uncategorized", "Food", "Bill", "Work", "Idea", "Health")

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAddEditNoteBinding.inflate(layoutInflater)
        setContentView(binding.root)

        dbHelper = NotesDatabaseHelper(this)

        // Setup spinner kategori
        val adapterSpinner = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, categoryList)
        binding.spinnerCategory.adapter = adapterSpinner

        // Cek apakah sedang edit
        editingNoteId = intent.getStringExtra("EXTRA_NOTE_ID")
        if (editingNoteId != null) {
            val note = NoteDao.getById(dbHelper.readableDatabase, editingNoteId!!)
            note?.let {
                binding.editTitle.setText(it.title)
                binding.editContent.setText(it.content)

                // Set posisi spinner sesuai categoryId
                if (it.categoryId != null && it.categoryId in categoryList.indices) {
                    binding.spinnerCategory.setSelection(it.categoryId!!)
                }
            }
        }

        // Tombol kembali
        binding.btnBack.setOnClickListener {
            finish()
        }

        // Tombol simpan
        binding.btnSave.setOnClickListener {
            val title = binding.editTitle.text.toString()
            val content = binding.editContent.text.toString()
            val categoryIndex = binding.spinnerCategory.selectedItemPosition

            if (title.isNotBlank()) {
                val note = Note(
                    id = editingNoteId ?: UUID.randomUUID().toString(),
                    title = title,
                    content = content,
                    categoryId = categoryIndex
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
