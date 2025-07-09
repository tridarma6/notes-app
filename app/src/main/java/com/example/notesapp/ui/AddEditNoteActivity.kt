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
import com.example.notesapp.data.db.NotesDatabaseHelper
import com.example.notesapp.data.model.Category
import com.example.notesapp.data.model.Note
import com.example.notesapp.databinding.ActivityAddEditNoteBinding
import java.util.*

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
        val editName = dialogView.findViewById<EditText>(R.id.editCategoryName)
        val editColor = dialogView.findViewById<EditText>(R.id.editCategoryColor)

        AlertDialog.Builder(this)
            .setTitle("Tambah Kategori Baru")
            .setView(dialogView)
            .setPositiveButton("Simpan") { _, _ ->
                val name = editName.text.toString()
                // Validasi format warna Hex
                val colorHex = editColor.text.toString().trim() // Trim spasi
                if (name.isNotBlank() && colorHex.matches(Regex("^#[0-9A-Fa-f]{6}$"))) {
                    val category = Category(name = name, colorHex = colorHex)
                    val db = NotesDatabaseHelper(this).writableDatabase
                    CategoryDao.insert(db, category)
                    setupCategorySpinner() // Refresh isi spinner setelah menambah
                    // Atur spinner ke kategori yang baru ditambahkan
                    val newCategoryPosition = categoryMutableList.indexOfFirst { it.name == name && it.colorHex == colorHex }
                    if (newCategoryPosition != -1) {
                        binding.spinnerCategory.setSelection(newCategoryPosition)
                    }
                } else {
                    Toast.makeText(this, "Nama tidak boleh kosong dan Warna harus dalam format HEX (#RRGGBB)", Toast.LENGTH_LONG).show()
                }
            }
            .setNegativeButton("Batal") { dialog, _ ->
                // Jika dibatalkan, kembalikan pilihan spinner ke item sebelumnya atau "Tidak Ada Kategori"
                // Ini penting agar "Tambah Kategori" tidak tetap terpilih
                binding.spinnerCategory.setSelection(0) // Default ke "Tidak Ada Kategori"
                dialog.dismiss()
            }
            .show()
    }
}