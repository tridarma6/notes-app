package com.example.notesapp.ui

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.widget.AdapterView
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.example.notesapp.R
import com.example.notesapp.adapter.CategorySpinnerAdapter
import com.example.notesapp.data.dao.CategoryDao
import com.example.notesapp.data.dao.NoteDao
import com.example.notesapp.data.model.Category
import com.example.notesapp.data.model.Note
import com.example.notesapp.databinding.ActivityAddEditNoteBinding
import java.util.*
import android.text.Editable
import android.text.TextWatcher
import android.graphics.Color
import androidx.core.content.ContextCompat
import com.example.notesapp.data.database.NotesDatabaseHelper
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout

class AddEditNoteActivity : AppCompatActivity() {

    private lateinit var binding: ActivityAddEditNoteBinding
    private lateinit var dbHelper: NotesDatabaseHelper
    private var editingNoteId: String? = null
    private lateinit var categoryMutableList: MutableList<Category>
    private var existingNote: Note? = null // *** TAMBAHKAN VARIABEL INI ***

    override fun onCreate(savedInstanceState: Bundle?) {
        Log.d("EntertoEdit", "Masuk ke edit")
        super.onCreate(savedInstanceState)
        binding = ActivityAddEditNoteBinding.inflate(layoutInflater)
        setContentView(binding.root)
        dbHelper = NotesDatabaseHelper(this)

        setupCategorySpinner()

        editingNoteId = intent.getStringExtra("EXTRA_NOTE_ID")
        Log.d("AddEditNoteActivity", "onCreate: editingNoteId received = $editingNoteId")

        if (editingNoteId != null) {
            // Ambil catatan yang sudah ada dan simpan di existingNote
            val note = NoteDao.getById(dbHelper.readableDatabase, editingNoteId!!)
            note?.let {
                existingNote = it // *** SIMPAN CATATAN YANG ADA DI existingNote ***
                binding.editTitle.setText(it.title)
                binding.editContent.setText(it.content)

                if (it.categoryId != null) {
                    val categoryPosition = categoryMutableList.indexOfFirst { category -> category.id == it.categoryId }
                    if (categoryPosition != -1) {
                        binding.spinnerCategory.setSelection(categoryPosition)
                    } else {
                        binding.spinnerCategory.setSelection(0)
                    }
                } else {
                    binding.spinnerCategory.setSelection(0)
                }
            }
        }

        binding.btnBack.setOnClickListener {
            finish()
        }

        binding.btnSave.setOnClickListener {
            val title = binding.editTitle.text.toString()
            val content = binding.editContent.text.toString()

            var categoryIdToSave: Int? = null
            val selectedCategory = binding.spinnerCategory.selectedItem

            if (selectedCategory is Category) {
                if (selectedCategory.id == -1) {
                    Log.d("SaveNote", "Selected 'Tambah Kategori', saving categoryId as null.")
                } else {
                    categoryIdToSave = selectedCategory.id
                    Log.d("SaveNote", "Selected category '${selectedCategory.name}', saving categoryId as ${selectedCategory.id}.")
                }
            } else {
                Log.e("SaveNote", "Selected item is not a Category object!")
            }


            if (title.isNotBlank()) {
                val db = dbHelper.writableDatabase

                if (editingNoteId == null) { // Mode Tambah Catatan Baru
                    val newNote = Note(
                        id = UUID.randomUUID().toString(),
                        title = title,
                        content = content,
                        categoryId = categoryIdToSave
                        // isTrashed, isHidden, isFavorite akan menggunakan nilai default false
                    )
                    NoteDao.insert(db, newNote)
                    Toast.makeText(this, "Catatan disimpan", Toast.LENGTH_SHORT).show()
                } else { // Mode Update Catatan yang Ada
                    existingNote?.let { originalNote -> // *** Gunakan existingNote di sini ***
                        val updatedNote = originalNote.copy( // Gunakan fungsi copy untuk mempertahankan properti lain
                            title = title,
                            content = content,
                            categoryId = categoryIdToSave
                            // isTrashed, isHidden, isFavorite akan tetap sama karena dicopy dari originalNote
                        )
                        val rowsAffected = NoteDao.update(db, updatedNote)
                        if (rowsAffected > 0) {
                            Toast.makeText(this, "Catatan diperbarui", Toast.LENGTH_SHORT).show()
                        } else {
                            Toast.makeText(this, "Gagal memperbarui catatan (ID tidak ditemukan)", Toast.LENGTH_SHORT).show()
                        }
                    } ?: run {
                        // Ini terjadi jika editingNoteId ada tapi catatan tidak ditemukan di DB.
                        // Seharusnya tidak terjadi jika alur normal.
                        Toast.makeText(this, "Error: Catatan asli tidak ditemukan untuk diperbarui.", Toast.LENGTH_LONG).show()
                        Log.e("AddEditNoteActivity", "Attempted to update a note with ID $editingNoteId but original note not found.")
                    }
                }

                finish()
            } else {
                Toast.makeText(this, "Judul tidak boleh kosong", Toast.LENGTH_SHORT).show()
            }
        }
    }

    // ... (kode setupCategorySpinner() dan showAddCategoryDialog() tidak berubah) ...
    private fun setupCategorySpinner() {
        val db = NotesDatabaseHelper(this).readableDatabase
        categoryMutableList = CategoryDao.getAll(db).toMutableList()

        categoryMutableList.add(Category(id = -1, name = "Tambah Kategori", colorHex = "#000000"))

        val adapterSpinner = CategorySpinnerAdapter(this, categoryMutableList)
        binding.spinnerCategory.adapter = adapterSpinner

        binding.spinnerCategory.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>, view: View?, position: Int, id: Long) {
                val selectedItem = parent.getItemAtPosition(position)
                if (selectedItem is Category && selectedItem.id == -1) {
                    showAddCategoryDialog()
                }
            }
            override fun onNothingSelected(parent: AdapterView<*>) {}
        }
    }

    private fun showAddCategoryDialog() {
        val dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_add_category, null)
        val textInputLayoutName = dialogView.findViewById<TextInputLayout>(R.id.textInputLayoutCategoryName)
        val editName = dialogView.findViewById<TextInputEditText>(R.id.editCategoryName)
        val textInputLayoutColor = dialogView.findViewById<TextInputLayout>(R.id.textInputLayoutCategoryColor)
        val editColor = dialogView.findViewById<TextInputEditText>(R.id.editCategoryColor)
        val colorPreview = dialogView.findViewById<View>(R.id.colorPreview)

        editColor.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}

            override fun afterTextChanged(s: Editable?) {
                val colorString = s.toString().trim()
                if (colorString.matches(Regex("^#[0-9A-Fa-f]{6}$"))) {
                    try {
                        val colorInt = Color.parseColor(colorString)
                        colorPreview.setBackgroundColor(colorInt)
                        colorPreview.visibility = View.VISIBLE
                        textInputLayoutColor.error = null
                    } catch (e: IllegalArgumentException) {
                        colorPreview.visibility = View.GONE
                        textInputLayoutColor.error = "Format warna HEX tidak valid"
                    }
                } else {
                    colorPreview.visibility = View.GONE
                    if (colorString.isNotEmpty() && !colorString.startsWith("#")) {
                        textInputLayoutColor.error = "Harus dimulai dengan #"
                    } else if (colorString.length > 1 && !colorString.matches(Regex("^#[0-9A-Fa-f]*$"))) {
                        textInputLayoutColor.error = "Hanya huruf A-F dan angka 0-9"
                    } else if (colorString.length == 7 && !colorString.matches(Regex("^#[0-9A-Fa-f]{6}$"))) {
                        textInputLayoutColor.error = "Warna harus 6 karakter HEX setelah #"
                    } else {
                        textInputLayoutColor.error = null
                    }
                }
            }
        })

        val alertDialog = AlertDialog.Builder(this)
            .setView(dialogView)
            .setPositiveButton("Simpan", null)
            .setNegativeButton("Batal") { dialog, _ ->
                binding.spinnerCategory.setSelection(0)
                dialog.dismiss()
            }
            .create()

        alertDialog.show()

        val positiveButton = alertDialog.getButton(AlertDialog.BUTTON_POSITIVE)
        positiveButton.setOnClickListener {
            val name = editName.text.toString().trim()
            val colorHex = editColor.text.toString().trim()

            var isValid = true

            if (name.isBlank()) {
                textInputLayoutName.error = "Nama kategori tidak boleh kosong"
                isValid = false
            } else {
                textInputLayoutName.error = null
            }

            if (!colorHex.matches(Regex("^#[0-9A-Fa-f]{6}$"))) {
                textInputLayoutColor.error = "Warna harus dalam format HEX (#RRGGBB)"
                isValid = false
            } else {
                textInputLayoutColor.error = null
            }

            if (isValid) {
                val category = Category(name = name, colorHex = colorHex)
                val db = NotesDatabaseHelper(this).writableDatabase
                CategoryDao.insert(db, category)
                setupCategorySpinner()
                val newCategoryPosition = categoryMutableList.indexOfFirst { it.name == name && it.colorHex == colorHex }
                if (newCategoryPosition != -1) {
                    binding.spinnerCategory.setSelection(newCategoryPosition)
                }
                Toast.makeText(this, "Kategori '$name' berhasil ditambahkan", Toast.LENGTH_SHORT).show()
                alertDialog.dismiss()
            }
        }

        alertDialog.getButton(AlertDialog.BUTTON_POSITIVE)?.setTextColor(ContextCompat.getColor(this, R.color.green_active))
        alertDialog.getButton(AlertDialog.BUTTON_NEGATIVE)?.setTextColor(ContextCompat.getColor(this, R.color.gray))
    }
}