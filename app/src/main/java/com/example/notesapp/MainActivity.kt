package com.example.notesapp

import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView

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
            Note("Getting Started", "Lorem ipsum dolor sit amet..."),
            Note("UX Design", "Lorem ipsum dolor sit amet..."),
            Note("Important", "Lorem ipsum dolor sit amet...")
        )
    }
}
