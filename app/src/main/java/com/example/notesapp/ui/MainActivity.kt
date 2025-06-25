package com.example.notesapp.ui

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.GridLayoutManager
import com.example.notesapp.adapter.NotesAdapter
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
        adapter = NotesAdapter()

        binding.recyclerRecentNotes.layoutManager = GridLayoutManager(this, 2)
        binding.recyclerRecentNotes.adapter = adapter

        insertDummyNotesIfEmpty()

        // Load data
        loadNotes()
    }

    private fun loadNotes() {
        val db = dbHelper.readableDatabase
        val notes = NoteDao.getAll(db)
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
}
