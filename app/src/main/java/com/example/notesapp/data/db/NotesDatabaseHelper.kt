// app/src/main/java/com/example/notesapp/data/database/NotesDatabaseHelper.kt
package com.example.notesapp.data.database

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import com.example.notesapp.data.dao.CategoryDao
import com.example.notesapp.data.dao.NoteDao
import com.example.notesapp.data.dao.EventDao // Import EventDao

class NotesDatabaseHelper(context: Context) :
    SQLiteOpenHelper(context, DATABASE_NAME, null, DATABASE_VERSION) {

    companion object {
        private const val DATABASE_NAME = "notesapp.db"
        private const val DATABASE_VERSION = 2 // Tingkatkan versi database
    }

    override fun onCreate(db: SQLiteDatabase?) {
        db?.execSQL(NoteDao.CREATE_TABLE)
        db?.execSQL(CategoryDao.CREATE_TABLE)
        db?.execSQL(EventDao.CREATE_TABLE_EVENTS) // Tambahkan pembuatan tabel events
    }

    override fun onUpgrade(db: SQLiteDatabase?, oldVersion: Int, newVersion: Int) {
        // Hapus tabel lama jika ada, lalu buat yang baru
        // Ini adalah strategi sederhana untuk upgrade. Untuk aplikasi produksi, pertimbangkan migrasi data.
        if (oldVersion < 2) {
            db?.execSQL("DROP TABLE IF EXISTS ${EventDao.TABLE_EVENTS}") // Hapus tabel events jika sudah ada
            db?.execSQL(EventDao.CREATE_TABLE_EVENTS) // Buat ulang tabel events
        }
        // Jika ada versi yang lebih baru di masa depan, tambahkan logika upgrade di sini
    }
}