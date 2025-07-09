// app/src/main/java/com/example/notesapp/data/dao/EventDao.kt
package com.example.notesapp.data.dao

import android.content.ContentValues
import android.database.Cursor
import android.database.sqlite.SQLiteDatabase
import com.example.notesapp.data.model.Event

object EventDao {
    const val TABLE_EVENTS = "events"
    const val COLUMN_ID = "id"
    const val COLUMN_TITLE = "title"
    const val COLUMN_DESCRIPTION = "description"
    const val COLUMN_DATE = "date"
    const val COLUMN_TIME = "time"

    // SQL untuk membuat tabel events
    const val CREATE_TABLE_EVENTS = """
        CREATE TABLE $TABLE_EVENTS (
            $COLUMN_ID INTEGER PRIMARY KEY AUTOINCREMENT,
            $COLUMN_TITLE TEXT NOT NULL,
            $COLUMN_DESCRIPTION TEXT,
            $COLUMN_DATE TEXT NOT NULL,
            $COLUMN_TIME TEXT
        )
    """

    fun insert(db: SQLiteDatabase, event: Event): Long {
        val values = ContentValues().apply {
            put(COLUMN_TITLE, event.title)
            put(COLUMN_DESCRIPTION, event.description)
            put(COLUMN_DATE, event.date)
            put(COLUMN_TIME, event.time)
        }
        return db.insert(TABLE_EVENTS, null, values)
    }

    fun getEventsByDate(db: SQLiteDatabase, date: String): List<Event> {
        val events = mutableListOf<Event>()
        val cursor: Cursor = db.query(
            TABLE_EVENTS,
            arrayOf(COLUMN_ID, COLUMN_TITLE, COLUMN_DESCRIPTION, COLUMN_DATE, COLUMN_TIME),
            "$COLUMN_DATE = ?",
            arrayOf(date),
            null,
            null,
            "$COLUMN_TIME ASC" // Urutkan berdasarkan waktu
        )

        with(cursor) {
            while (moveToNext()) {
                val id = getInt(getColumnIndexOrThrow(COLUMN_ID))
                val title = getString(getColumnIndexOrThrow(COLUMN_TITLE))
                val description = getString(getColumnIndexOrThrow(COLUMN_DESCRIPTION))
                val eventDate = getString(getColumnIndexOrThrow(COLUMN_DATE))
                val eventTime = getString(getColumnIndexOrThrow(COLUMN_TIME))
                events.add(Event(id, title, description, eventDate, eventTime))
            }
        }
        cursor.close()
        return events
    }

    fun delete(db: SQLiteDatabase, eventId: Int): Int {
        return db.delete(TABLE_EVENTS, "$COLUMN_ID = ?", arrayOf(eventId.toString()))
    }

    fun update(db: SQLiteDatabase, event: Event): Int {
        val values = ContentValues().apply {
            put(COLUMN_TITLE, event.title)
            put(COLUMN_DESCRIPTION, event.description)
            put(COLUMN_DATE, event.date)
            put(COLUMN_TIME, event.time)
        }
        return db.update(TABLE_EVENTS, values, "$COLUMN_ID = ?", arrayOf(event.id.toString()))
    }
}