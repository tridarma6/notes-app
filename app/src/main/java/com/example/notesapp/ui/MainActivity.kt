package com.example.notesapp.ui

import NotesAdapter
import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.GridLayoutManager
import com.example.notesapp.R
import com.example.notesapp.data.dao.NoteDao
import com.example.notesapp.data.db.NotesDatabaseHelper
import com.example.notesapp.data.model.Note
import com.example.notesapp.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var dbHelper: NotesDatabaseHelper
    private lateinit var adapter: NotesAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        dbHelper = NotesDatabaseHelper(this)
        adapter = NotesAdapter(
            onItemClick = { note ->
                val intent = Intent(this, AddEditNoteActivity::class.java).apply {
                    putExtra("EXTRA_NOTE_ID", note.id)
                }
                startActivity(intent)
            },
            onItemLongClick = { note ->
                showDeleteDialog(note)
            }
        )


        // Setup RecyclerView
        binding.recyclerRecentNotes.layoutManager = GridLayoutManager(this, 2)
        binding.recyclerRecentNotes.adapter = adapter

        binding.fabAddNote.setOnClickListener {
            val intent = Intent(this, AddEditNoteActivity::class.java)
            startActivity(intent)
        }

        insertDummyNotesIfEmpty()

        loadNotes()
    }

    override fun onResume() {
        super.onResume()
        loadNotes()
    }

    private fun loadNotes() {
        val db = dbHelper.readableDatabase
        val notes = NoteDao.getAll(db).filter { !it.isTrashed } // Hanya tampilkan yang tidak dihapus
        adapter.submitList(notes)
    }

    private fun insertDummyNotesIfEmpty() {
        val db = dbHelper.writableDatabase
        val notes = NoteDao.getAll(db)
        if (notes.isEmpty()) {
            val dummyNotes = listOf(
                Note(
                    title = "Getting Started",
                    content = "Selamat datang di aplikasi NotesApp.",
                    categoryId = 1,
                    isFavorite = true
                ),
                Note(
                    title = "UX Design",
                    content = "Tips dan trik untuk desain UX.",
                    categoryId = 2
                ),
                Note(
                    title = "Important",
                    content = "Catatan ini penting!",
                    categoryId = 3,
                    isFavorite = true
                )
            )
            dummyNotes.forEach { NoteDao.insert(db, it) }
        }
    }

    private fun showDeleteDialog(note: Note) {
        val dialog = AlertDialog.Builder(this)
            .setTitle("Hapus Catatan")
            .setMessage("Catatan ini akan dipindahkan ke tempat sampah. Lanjutkan?")
            .setPositiveButton("Hapus") { _, _ ->
                val updatedNote = note.copy(isTrashed = true)
                NoteDao.update(dbHelper.writableDatabase, updatedNote)
                loadNotes()
                Toast.makeText(this, "Catatan dipindahkan ke trash", Toast.LENGTH_SHORT).show()
            }
            .setNegativeButton("Batal", null)
            .create()

        dialog.show()

        // Ubah warna tombol
        dialog.getButton(AlertDialog.BUTTON_POSITIVE)?.setTextColor(getColor(R.color.red))
        dialog.getButton(AlertDialog.BUTTON_NEGATIVE)?.setTextColor(getColor(R.color.gray))
    }

}
