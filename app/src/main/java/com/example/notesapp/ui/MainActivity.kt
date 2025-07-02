package com.example.notesapp.ui

import NotesAdapter
import android.content.Intent
import android.graphics.drawable.Drawable
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.Toast
import androidx.annotation.ColorInt
import androidx.annotation.ColorRes
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.GridLayoutManager
import com.example.notesapp.R
import com.example.notesapp.data.dao.NoteDao
import com.example.notesapp.data.db.NotesDatabaseHelper
import com.example.notesapp.data.model.Note
import com.example.notesapp.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var dbHelper: NotesDatabaseHelper
    private lateinit var adapter: NotesAdapter
    private var isViewingTrash = false
    private var isViewingFavorite = false
    private var isViewingHidden = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        dbHelper = NotesDatabaseHelper(this)
        adapter = NotesAdapter(
            onItemClick = { note ->
                val intent = Intent(this, AddEditNoteActivity::class.java).apply {
                    putExtra("EXTRA_NOTE_ID", note.id)
                }
                startActivity(intent)
            },
            onItemLongClick = { note ->
                if (isViewingTrash) {
                    showPermanentDeleteDialog(note)
                } else {
                    showDeleteDialog(note)
                }
            },
            onFavoriteToggle = { note ->
                val updatedNote = note.copy(isFavorite = !note.isFavorite)
                NoteDao.update(dbHelper.writableDatabase, updatedNote)
                val updatedList = adapter.currentList.map {
                    if (it.id == note.id) updatedNote else it
                }
                adapter.submitList(updatedList)

            }
        )


        // Setup RecyclerView
        binding.recyclerRecentNotes.layoutManager = GridLayoutManager(this, 2)
        binding.recyclerRecentNotes.adapter = adapter

        binding.fabAddNote.setOnClickListener {
            val intent = Intent(this, AddEditNoteActivity::class.java)
            startActivity(intent)
        }

        binding.btnTrash.setOnClickListener {
            val db = dbHelper.readableDatabase
            val trashedNotes = NoteDao.getAll(db).filter { it.isTrashed }
            adapter.submitList(trashedNotes)
            binding.recentNotesTitle.text = "Trashed Notes"
            val red = ContextCompat.getColor(this, R.color.red_active)
            Log.d("DEBUG", "Setting bg color: $red")  // contoh hasil: -1234567

            resetAllQuickButtons()
            setQuickButtonState(binding.btnTrash, true, R.color.red_active)
            isViewingTrash = true
            isViewingFavorite = false
            isViewingHidden = false
        }

        binding.btnFavorite.setOnClickListener {
            val db = dbHelper.readableDatabase
            val favoriteNotes = NoteDao.getAll(db).filter { it.isFavorite and !it.isTrashed }
            adapter.submitList(favoriteNotes)
            binding.recentNotesTitle.text = "Favorite Notes"
            resetAllQuickButtons()
            setQuickButtonState(binding.btnFavorite, true, R.color.yellow_active)
            isViewingTrash = false
            isViewingFavorite = true
            isViewingHidden = false

        }

        binding.btnHidden.setOnClickListener {
            val db = dbHelper.readableDatabase
            val hiddenNotes = NoteDao.getAll(db).filter { it.isHidden and !it.isTrashed }
            adapter.submitList(hiddenNotes)
            binding.recentNotesTitle.text = "Hidden Notes"
            resetAllQuickButtons()
            setQuickButtonState(binding.btnHidden, true, R.color.blue_active)
            isViewingTrash = false
            isViewingFavorite = false
            isViewingHidden = true

        }

        binding.btnAllNotes.setOnClickListener {
            loadNotes() // tampilkan semua yang tidak di-trash
            binding.recentNotesTitle.text = getString(R.string.recent_notes)

            binding.recyclerRecentNotes.setBackgroundColor(
                ContextCompat.getColor(this, android.R.color.transparent)
            )

            resetAllQuickButtons()
            setQuickButtonState(binding.btnAllNotes, true, R.color.green_active)
            isViewingTrash = false
            isViewingFavorite = false
            isViewingHidden = false
        }



        insertDummyNotesIfEmpty()

        loadNotes()
    }

    override fun onResume() {
        super.onResume()
        loadNotes()
    }

    private fun loadNotes() {
        val db = dbHelper.readableDatabase
        val notes = NoteDao.getAll(db).filter { !it.isTrashed and !it.isHidden } // Hanya tampilkan yang tidak dihapus
        adapter.submitList(notes)
    }

    private fun insertDummyNotesIfEmpty() {
        val db = dbHelper.writableDatabase
        val notes = NoteDao.getAll(db)
        if (notes.isEmpty()) {
            val dummyNotes = listOf(
                Note(
                    title = "Getting Started",
                    content = "Selamat datang di aplikasi NotesApp.",
                    categoryId = 1,
                    isFavorite = true
                ),
                Note(
                    title = "UX Design",
                    content = "Tips dan trik untuk desain UX.",
                    categoryId = 2
                ),
                Note(
                    title = "Important",
                    content = "Catatan ini penting!",
                    categoryId = 3,
                    isFavorite = true
                )
            )
            dummyNotes.forEach { NoteDao.insert(db, it) }
        }
    }

    private fun showDeleteDialog(note: Note) {
        val dialog = AlertDialog.Builder(this)
            .setTitle("Hapus Catatan")
            .setMessage("Catatan ini akan dipindahkan ke tempat sampah. Lanjutkan?")
            .setPositiveButton("Hapus") { _, _ ->
                val updatedNote = note.copy(isTrashed = true)
                NoteDao.update(dbHelper.writableDatabase, updatedNote)
                loadNotes()
                Toast.makeText(this, "Catatan dipindahkan ke trash", Toast.LENGTH_SHORT).show()
            }
            .setNegativeButton("Batal", null)
            .create()

        dialog.show()

        // Ubah warna tombol
        dialog.getButton(AlertDialog.BUTTON_POSITIVE)?.setTextColor(getColor(R.color.red))
        dialog.getButton(AlertDialog.BUTTON_NEGATIVE)?.setTextColor(getColor(R.color.gray))
    }
    private fun showPermanentDeleteDialog(note: Note) {
        val dialog = AlertDialog.Builder(this)
            .setTitle("Hapus Permanen")
            .setMessage("Catatan ini akan dihapus secara permanen dan tidak bisa dikembalikan. Lanjutkan?")
            .setPositiveButton("Hapus") { _, _ ->
                NoteDao.deletePermanent(dbHelper.writableDatabase, note.id)
                loadTrashedNotes()
                Toast.makeText(this, "Catatan dihapus permanen", Toast.LENGTH_SHORT).show()
            }
            .setNegativeButton("Batal", null)
            .create()

        dialog.show()
        // Ubah warna tombol
        dialog.getButton(AlertDialog.BUTTON_POSITIVE)?.setTextColor(getColor(R.color.red))
        dialog.getButton(AlertDialog.BUTTON_NEGATIVE)?.setTextColor(getColor(R.color.gray))
    }

    private fun loadTrashedNotes() {
        val db = dbHelper.readableDatabase
        val trashedNotes = NoteDao.getAll(db).filter { it.isTrashed }
        adapter.submitList(trashedNotes)
    }


    private fun createRoundedBackground(@ColorInt bgColor: Int): Drawable {
        return GradientDrawable().apply {
            shape = GradientDrawable.RECTANGLE
            cornerRadius = 16f
            setColor(bgColor)
            setStroke(2, bgColor)
        }
    }
    private fun setQuickButtonState(button: Button, isActive: Boolean, @ColorRes activeColorRes: Int) {
        if (isActive) {
            val bgDrawable = createRoundedBackground(ContextCompat.getColor(this, activeColorRes))
            button.invalidate()
            button.background = bgDrawable
            button.setTextColor(ContextCompat.getColor(this, R.color.white))
        } else {
            button.setBackgroundResource(R.drawable.button_background) // default XML background
            button.setTextColor(ContextCompat.getColor(this, R.color.button_text_normal))
        }
    }
    private fun resetAllQuickButtons() {
        val buttons = listOf(
            binding.btnAllNotes,
            binding.btnTrash,
            binding.btnFavorite,
            binding.btnHidden,
            binding.btnCategory
        )
        buttons.forEach {
            setQuickButtonState(it, false, R.color.green_active) // warna tidak penting di sini karena state false
        }
    }

}
