package com.example.notesapp.data.dao

import android.content.ContentValues
import android.database.sqlite.SQLiteDatabase
import com.example.notesapp.data.db.NotesDatabaseHelper
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
        val categories = mutableListOf<Category>()
        val cursor = db.rawQuery("SELECT * FROM $TABLE_NAME ORDER BY $COLUMN_ID ASC", null)
        if (cursor.moveToFirst()) {
            do {
                val category = Category(
                    id = cursor.getInt(cursor.getColumnIndexOrThrow("id")),
                    name = cursor.getString(cursor.getColumnIndexOrThrow("name")),
                    colorHex = cursor.getString(cursor.getColumnIndexOrThrow("color"))
                )
                categories.add(category)
            } while (cursor.moveToNext())
        }
        cursor.close()
        return categories
    }

    fun getAllNames(db: SQLiteDatabase): List<String> {
        val names = mutableListOf<String>()
        val cursor = db.rawQuery("SELECT $COLUMN_NAME FROM $TABLE_NAME", null)
        if (cursor.moveToFirst()) {
            do {
                names.add(cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_NAME)))
            } while (cursor.moveToNext())
        }
        cursor.close()
        return names
    }

    fun getNameById(dbHelper: NotesDatabaseHelper, id: Int?): String {
        val db = dbHelper.readableDatabase
        val cursor = db.rawQuery(
            "SELECT $COLUMN_NAME FROM $TABLE_NAME WHERE $COLUMN_ID = ?",
            arrayOf(id.toString())
        )

        var name = ""
        if (cursor.moveToFirst()) {
            name = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_NAME))
        }
        cursor.close()
        return name
    }

    fun insert(db: SQLiteDatabase, category: Category): Boolean {
        val values = ContentValues().apply {
            put(COLUMN_NAME, category.name)
            put(COLUMN_COLOR, category.colorHex)
        }
        return db.insert(TABLE_NAME, null, values) != -1L
    }

    fun deleteAll(db: SQLiteDatabase){
        db.delete(TABLE_NAME, null, null) // menghapus semua baris di tabel
    }

}