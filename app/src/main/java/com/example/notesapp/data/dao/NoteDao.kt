package com.example.notesapp.data.dao

import android.content.ContentValues
import android.database.sqlite.SQLiteDatabase
import com.example.notesapp.data.model.Note

object NoteDao {
    const val TABLE_NAME = "notes"

    const val COLUMN_ID = "id"
    const val COLUMN_TITLE = "title"
    const val COLUMN_CONTENT = "content"
    const val COLUMN_CATEGORY_ID = "category_id"
    const val COLUMN_IS_FAVORITE = "is_favorite"
    const val COLUMN_IS_HIDDEN = "is_hidden"
    const val COLUMN_IS_TRASHED = "is_trashed"
    const val COLUMN_CREATED_AT = "created_at"
    const val COLUMN_UPDATED_AT = "updated_at"

    val CREATE_TABLE = """
        CREATE TABLE $TABLE_NAME (
            $COLUMN_ID TEXT PRIMARY KEY,
            $COLUMN_TITLE TEXT,
            $COLUMN_CONTENT TEXT,
            $COLUMN_CATEGORY_ID INTEGER,
            $COLUMN_IS_FAVORITE INTEGER,
            $COLUMN_IS_HIDDEN INTEGER,
            $COLUMN_IS_TRASHED INTEGER,
            $COLUMN_CREATED_AT INTEGER,
            $COLUMN_UPDATED_AT INTEGER
        )
    """.trimIndent()

    fun insert(db: SQLiteDatabase, note: Note): Boolean {
        val values = ContentValues().apply {
            put(COLUMN_ID, note.id)
            put(COLUMN_TITLE, note.title)
            put(COLUMN_CONTENT, note.content)
            put(COLUMN_CATEGORY_ID, note.categoryId)
            put(COLUMN_IS_FAVORITE, if (note.isFavorite) 1 else 0)
            put(COLUMN_IS_HIDDEN, if (note.isHidden) 1 else 0)
            put(COLUMN_IS_TRASHED, if (note.isTrashed) 1 else 0)
            put(COLUMN_CREATED_AT, note.createdAt)
            put(COLUMN_UPDATED_AT, note.updatedAt)
        }
        return db.insert(TABLE_NAME, null, values) != -1L
    }

    fun getAll(db: SQLiteDatabase): List<Note> {
        val notes = mutableListOf<Note>()
        val cursor = db.rawQuery("SELECT * FROM $TABLE_NAME ORDER BY $COLUMN_UPDATED_AT DESC", null)
        if (cursor.moveToFirst()) {
            do {
                notes.add(
                    Note(
                        id = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_ID)),
                        title = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_TITLE)),
                        content = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_CONTENT)),
                        categoryId = cursor.getInt(cursor.getColumnIndexOrThrow(COLUMN_CATEGORY_ID)),
                        isFavorite = cursor.getInt(cursor.getColumnIndexOrThrow(COLUMN_IS_FAVORITE)) == 1,
                        isHidden = cursor.getInt(cursor.getColumnIndexOrThrow(COLUMN_IS_HIDDEN)) == 1,
                        isTrashed = cursor.getInt(cursor.getColumnIndexOrThrow(COLUMN_IS_TRASHED)) == 1,
                        createdAt = cursor.getLong(cursor.getColumnIndexOrThrow(COLUMN_CREATED_AT)),
                        updatedAt = cursor.getLong(cursor.getColumnIndexOrThrow(COLUMN_UPDATED_AT))
                    )
                )
            } while (cursor.moveToNext())
        }
        cursor.close()
        return notes
    }

    fun deleteById(db: SQLiteDatabase, noteId: String): Boolean {
        return db.delete(TABLE_NAME, "$COLUMN_ID=?", arrayOf(noteId)) > 0
    }

    fun update(db: SQLiteDatabase, note: Note): Boolean {
        val values = ContentValues().apply {
            put(COLUMN_TITLE, note.title)
            put(COLUMN_CONTENT, note.content)
            put(COLUMN_CATEGORY_ID, note.categoryId)
            put(COLUMN_IS_FAVORITE, if (note.isFavorite) 1 else 0)
            put(COLUMN_IS_HIDDEN, if (note.isHidden) 1 else 0)
            put(COLUMN_IS_TRASHED, if (note.isTrashed) 1 else 0)
            put(COLUMN_UPDATED_AT, note.updatedAt)
        }
        return db.update(TABLE_NAME, values, "$COLUMN_ID=?", arrayOf(note.id)) > 0
    }
}