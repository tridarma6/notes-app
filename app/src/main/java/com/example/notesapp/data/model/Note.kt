package com.example.notesapp.data.model

import java.io.Serializable
import java.util.UUID

data class Note(
    val id: String = UUID.randomUUID().toString(),
    var title: String,
    var content: String,
    var categoryId: Int? = null,
    var isFavorite: Boolean = false,
    var isHidden: Boolean = false,
    var isTrashed: Boolean = false,
    var createdAt: Long = System.currentTimeMillis(),
    var updatedAt: Long = System.currentTimeMillis()
) : Serializable