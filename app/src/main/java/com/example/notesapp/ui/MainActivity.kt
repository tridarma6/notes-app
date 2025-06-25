package com.example.notesapp.ui

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.notesapp.adapter.NotesAdapter
import com.example.notesapp.R
import com.example.notesapp.data.model.Note

class MainActivity : AppCompatActivity() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: NotesAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        recyclerView = findViewById(R.id.recyclerRecentNotes)
        recyclerView.layoutManager = GridLayoutManager(this, 2)
        adapter = NotesAdapter(getDummyNotes())
        recyclerView.adapter = adapter
    }

    private fun getDummyNotes(): List<Note> {
        return listOf(
            Note(
                title = "Getting Started",
                content = "Lorem ipsum dolor sit amet...",
                categoryId = 1, // Misal 1 untuk 'Personal'
                isFavorite = true
            ),
            Note(
                title = "UX Design",
                content = "Tips and tricks on user experience design...",
                categoryId = 2, // Misal 2 untuk 'Work'
                isHidden = false
            ),
            Note(
                title = "Important",
                content = "This note is very important.",
                categoryId = 3, // Misal 3 untuk 'Important'
                isTrashed = false,
                isFavorite = true
            )
        )
    }

}