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
import android.graphics.Color // Import ini untuk Color.parseColor
import androidx.core.content.ContextCompat
import com.example.notesapp.data.database.NotesDatabaseHelper
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout

class AddEditNoteActivity : AppCompatActivity() {

    private lateinit var binding: ActivityAddEditNoteBinding
    private lateinit var dbHelper: NotesDatabaseHelper
    private var editingNoteId: String? = null
    // Mengubah ini menjadi var agar bisa diinisialisasi ulang atau diisi nanti
    private lateinit var categoryMutableList: MutableList<Category>

    override fun onCreate(savedInstanceState: Bundle?) {
        Log.d("EntertoEdit", "Masuk ke edit")
        super.onCreate(savedInstanceState)
        binding = ActivityAddEditNoteBinding.inflate(layoutInflater)
        setContentView(binding.root)
        dbHelper = NotesDatabaseHelper(this)

        // Panggil setupCategorySpinner() *sebelum* mencoba mengakses categoryMutableList
        setupCategorySpinner()

        editingNoteId = intent.getStringExtra("EXTRA_NOTE_ID")
        Log.d("AddEditNoteActivity", "onCreate: editingNoteId received = $editingNoteId")

        // Cek apakah sedang edit
        if (editingNoteId != null) {
            val note = NoteDao.getById(dbHelper.readableDatabase, editingNoteId!!)
            note?.let {
                binding.editTitle.setText(it.title)
                binding.editContent.setText(it.content)

                // Set posisi spinner sesuai categoryId
                if (it.categoryId != null) {
                    val categoryPosition = categoryMutableList.indexOfFirst { category -> category.id == it.categoryId }
                    if (categoryPosition != -1) {
                        binding.spinnerCategory.setSelection(categoryPosition)
                    } else {
                        // Jika kategori tidak ditemukan, default ke kategori pertama (biasanya "Tidak ada Kategori" atau sejenisnya)
                        binding.spinnerCategory.setSelection(0)
                    }
                } else {
                    // Jika categoryId null, set ke posisi default (misalnya "Tidak ada Kategori")
                    binding.spinnerCategory.setSelection(0)
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

            // --- Bagian yang Diperbaiki ---
            var categoryIdToSave: Int? = null
            val selectedCategory = binding.spinnerCategory.selectedItem // Mengambil objek Category

            if (selectedCategory is Category) { // Pastikan ini adalah objek Category
                if (selectedCategory.id == -1) {
                    // Ini adalah "Tambah Kategori", biarkan categoryIdToSave tetap null
                    Log.d("SaveNote", "Selected 'Tambah Kategori', saving categoryId as null.")
                } else {
                    categoryIdToSave = selectedCategory.id // Ambil ID kategori yang sebenarnya
                    Log.d("SaveNote", "Selected category '${selectedCategory.name}', saving categoryId as ${selectedCategory.id}.")
                }
            } else {
                Log.e("SaveNote", "Selected item is not a Category object!")
                // Anda mungkin ingin memberikan feedback ke pengguna atau default ke null
            }


            if (title.isNotBlank()) {
                val note = Note(
                    id = editingNoteId ?: UUID.randomUUID().toString(),
                    title = title,
                    content = content,
                    categoryId = categoryIdToSave // Gunakan ID kategori yang sebenarnya atau null
                )

                if (editingNoteId == null) {
                    NoteDao.insert(dbHelper.writableDatabase, note)
                    Toast.makeText(this, "Catatan disimpan", Toast.LENGTH_SHORT).show()
                } else { // Ini adalah blok untuk update
                    val rowsAffected = NoteDao.update(dbHelper.writableDatabase, note)
                    if (rowsAffected > 0) {
                        Toast.makeText(this, "Catatan diperbarui", Toast.LENGTH_SHORT).show()
                    } else {
                        Toast.makeText(this, "Gagal memperbarui catatan (ID tidak ditemukan)", Toast.LENGTH_SHORT).show()
                    }
                }

                finish()
            } else {
                Toast.makeText(this, "Judul tidak boleh kosong", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun setupCategorySpinner() {
        val db = NotesDatabaseHelper(this).readableDatabase
        categoryMutableList = CategoryDao.getAll(db).toMutableList() // Akan ambil "Uncategorized" dari DB

        // *** HAPUS BARIS INI KARENA "Uncategorized" SUDAH DARI DB ***
        // categoryMutableList.add(0, Category(id = 0, name = "Uncategorized", colorHex = "#808080"))

        // Tambahkan "Tambah Kategori" di bagian akhir
        categoryMutableList.add(Category(id = -1, name = "Tambah Kategori", colorHex = "#000000"))

        val adapterSpinner = CategorySpinnerAdapter(this, categoryMutableList)
        binding.spinnerCategory.adapter = adapterSpinner

        binding.spinnerCategory.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>, view: View?, position: Int, id: Long) {
                val selectedItem = parent.getItemAtPosition(position)
                if (selectedItem is Category && selectedItem.id == -1) {
                    showAddCategoryDialog()
                }
                // Tambahan: Jika Anda ingin kategori "Uncategorized" menjadi default saat memilihnya
                // if (selectedItem is Category && selectedItem.name == "Uncategorized") {
                //     // Lakukan sesuatu jika "Uncategorized" dipilih
                // }
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
        val colorPreview = dialogView.findViewById<View>(R.id.colorPreview) // Dapatkan reference ke View preview

        // Listener untuk preview warna real-time
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
                        textInputLayoutColor.error = null // Hapus error jika format benar
                    } catch (e: IllegalArgumentException) {
                        // Ini tidak seharusnya terjadi jika regex sudah benar, tapi untuk jaga-jaga
                        colorPreview.visibility = View.GONE
                        textInputLayoutColor.error = "Format warna HEX tidak valid"
                    }
                } else {
                    colorPreview.visibility = View.GONE
                    // Hanya tampilkan error jika input mulai tidak sesuai format (misal, bukan '#')
                    if (colorString.isNotEmpty() && !colorString.startsWith("#")) {
                        textInputLayoutColor.error = "Harus dimulai dengan #"
                    } else if (colorString.length > 1 && !colorString.matches(Regex("^#[0-9A-Fa-f]*$"))) {
                        textInputLayoutColor.error = "Hanya huruf A-F dan angka 0-9"
                    } else if (colorString.length == 7 && !colorString.matches(Regex("^#[0-9A-Fa-f]{6}$"))) {
                        textInputLayoutColor.error = "Warna harus 6 karakter HEX setelah #"
                    } else {
                        textInputLayoutColor.error = null // Hapus error jika belum cukup karakter atau belum salah
                    }
                }
            }
        })


        val alertDialog = AlertDialog.Builder(this)
            .setView(dialogView) // Set view, judul sudah di layout
            .setPositiveButton("Simpan", null) // Set null untuk penanganan klik manual
            .setNegativeButton("Batal") { dialog, _ ->
                binding.spinnerCategory.setSelection(0)
                dialog.dismiss()
            }
            .create()

        // Menampilkan dialog
        alertDialog.show()

        // Mengambil referensi tombol setelah dialog ditampilkan
        val positiveButton = alertDialog.getButton(AlertDialog.BUTTON_POSITIVE)
        positiveButton.setOnClickListener {
            val name = editName.text.toString().trim() // Trim spasi juga untuk nama
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
                alertDialog.dismiss() // Tutup dialog hanya jika valid
            } else {
                // Toast.makeText(this, "Validasi gagal. Periksa input Anda.", Toast.LENGTH_LONG).show() // Pesan error sudah di TextInputLayout
            }
        }

        // Mengatur warna tombol dialog
        alertDialog.getButton(AlertDialog.BUTTON_POSITIVE)?.setTextColor(ContextCompat.getColor(this, R.color.green_active)) // Sesuaikan warna
        alertDialog.getButton(AlertDialog.BUTTON_NEGATIVE)?.setTextColor(ContextCompat.getColor(this, R.color.gray)) // Sesuaikan warna
    }
}