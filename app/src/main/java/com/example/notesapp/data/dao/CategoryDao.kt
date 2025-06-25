package com.example.notesapp.data.dao

import android.content.ContentValues
import android.database.sqlite.SQLiteDatabase
import com.example.notesapp.data.model.Category

object CategoryDao {
    const val TABLE_NAME = "category"

    const val COLUMN_ID = "id"
    const val COLUMN_NAME = "name"
    const val COLUMN_COLOR = "color"

    val CREATE_TABLE = """
        CREATE TABLE $TABLE_NAME (
            $COLUMN_ID INTEGER PRIMARY KEY AUTOINCREMENT,
            $COLUMN_NAME TEXT NOT NULL,
            $COLUMN_COLOR TEXT
        )
    """.trimIndent()

    fun prepopulateCategories(db: SQLiteDatabase) {
        val defaultCategories = listOf("Personal", "Work", "Idea", "Important")
        defaultCategories.forEach {
            val values = ContentValues().apply {
                put(COLUMN_NAME, it)
                put(COLUMN_COLOR, "#FFD700")
            }
            db.insert(TABLE_NAME, null, values)
        }
    }

    fun getAll(db: SQLiteDatabase): List<Category> {
        val list = mutableListOf<Category>()
        val cursor = db.rawQuery("SELECT * FROM $TABLE_NAME", null)
        if (cursor.moveToFirst()) {
            do {
                list.add(
                    Category(
                        id = cursor.getInt(cursor.getColumnIndexOrThrow(COLUMN_ID)),
                        name = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_NAME)),
                        colorHex = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_COLOR))
                    )
                )
            } while (cursor.moveToNext())
        }
        cursor.close()
        return list
    }

    fun insert(db: SQLiteDatabase, category: Category): Boolean {
        val values = ContentValues().apply {
            put(COLUMN_NAME, category.name)
            put(COLUMN_COLOR, category.colorHex)
        }
        return db.insert(TABLE_NAME, null, values) != -1L
    }
}