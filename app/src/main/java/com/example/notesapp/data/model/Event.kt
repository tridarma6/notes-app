package com.example.notesapp.data.model

data class Event(
    val id: Int = 0,
    val title: String,
    val description: String,
    val date: String, // Format YYYY-MM-DD
    val time: String? = null // Opsional, format HH:MM
)