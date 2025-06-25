package com.example.notesapp.data.db

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import com.example.notesapp.data.dao.CategoryDao
import com.example.notesapp.data.dao.NoteDao

class NotesDatabaseHelper(context: Context) :
    SQLiteOpenHelper(context, DATABASE_NAME, null, DATABASE_VERSION) {

    companion object {
        const val DATABASE_NAME = "notes.db"
        const val DATABASE_VERSION = 1
    }

    override fun onCreate(db: SQLiteDatabase) {
        db.execSQL(NoteDao.CREATE_TABLE)
        db.execSQL(CategoryDao.CREATE_TABLE)
        CategoryDao.prepopulateCategories(db)
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        db.execSQL("DROP TABLE IF EXISTS ${NoteDao.TABLE_NAME}")
        db.execSQL("DROP TABLE IF EXISTS ${CategoryDao.TABLE_NAME}")
        onCreate(db)
    }
}