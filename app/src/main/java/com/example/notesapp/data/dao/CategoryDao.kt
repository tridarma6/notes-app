package com.example.notesapp.data.dao

import android.content.ContentValues
import android.database.Cursor
import android.database.sqlite.SQLiteDatabase
import com.example.notesapp.data.database.NotesDatabaseHelper
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

    // *** TAMBAHKAN METODE INI ***
    fun getByName(db: SQLiteDatabase, name: String): Category? {
        var category: Category? = null
        val cursor = db.rawQuery(
            "SELECT * FROM $TABLE_NAME WHERE $COLUMN_NAME = ?",
            arrayOf(name)
        )
        if (cursor.moveToFirst()) {
            category = Category(
                id = cursor.getInt(cursor.getColumnIndexOrThrow(COLUMN_ID)),
                name = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_NAME)),
                colorHex = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_COLOR))
            )
        }
        cursor.close()
        return category
    }

    fun insert(db: SQLiteDatabase, category: Category): Boolean {
        val values = ContentValues().apply {
            put(COLUMN_NAME, category.name)
            put(COLUMN_COLOR, category.colorHex)
        }
        return db.insert(TABLE_NAME, null, values) != -1L
    }

    fun update(db: SQLiteDatabase, category: Category): Int {
        val values = ContentValues().apply {
            put(COLUMN_NAME, category.name)
            put(COLUMN_COLOR, category.colorHex)
        }
        return db.update(TABLE_NAME, values, "$COLUMN_ID = ?", arrayOf(category.id.toString()))
    }

    fun delete(db: SQLiteDatabase, categoryId: Int): Int {
        return db.delete(TABLE_NAME, "$COLUMN_ID = ?", arrayOf(categoryId.toString()))
    }

    fun getCategoryById(db: SQLiteDatabase, categoryId: Int): Category? {
        // Tangani kasus "Uncategorized" yang ID-nya 0
        if (categoryId == 0) {
            return Category(id = 0, name = "Uncategorized", colorHex = "#808080")
        }

        val cursor: Cursor = db.query(
            TABLE_NAME,
            arrayOf(COLUMN_ID, COLUMN_NAME, COLUMN_COLOR),
            "$COLUMN_ID = ?",
            arrayOf(categoryId.toString()),
            null,
            null,
            null
        )

        var category: Category? = null
        with(cursor) {
            if (moveToFirst()) {
                val id = getInt(getColumnIndexOrThrow(COLUMN_ID))
                val name = getString(getColumnIndexOrThrow(COLUMN_NAME))
                val colorHex = getString(getColumnIndexOrThrow(COLUMN_COLOR))
                category = Category(id, name, colorHex)
            }
        }
        cursor.close()
        return category
    }

}