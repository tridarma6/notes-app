package com.example.notesapp.ui

import CategoriesAdapter
import NotesAdapter
import android.content.Intent
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import android.graphics.drawable.Drawable
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.ColorInt
import androidx.annotation.ColorRes
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.notesapp.R
import com.example.notesapp.data.dao.CategoryDao
import com.example.notesapp.data.dao.NoteDao
import com.example.notesapp.data.db.NotesDatabaseHelper
import com.example.notesapp.data.model.Category
import com.example.notesapp.data.model.Note
import com.example.notesapp.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var dbHelper: NotesDatabaseHelper
    private lateinit var adapter: NotesAdapter
    private lateinit var categoryAdapter: CategoriesAdapter
    private lateinit var categoryList: List<Category>
    private var isViewingTrash = false
    private var isViewingFavorite = false
    private var isViewingHidden = false
    companion object {
        const val REQUEST_VERIFY_PIN = 1001
    }
    private val verifyPinLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == RESULT_OK) {
            val isVerified = result.data?.getBooleanExtra("isVerified", false) ?: false
            if (isVerified) {
                showHiddenNotes()
            }
        }
    }

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
        categoryList = CategoryDao.getAll(dbHelper.readableDatabase)
        categoryAdapter = CategoriesAdapter(categoryList) { selectedCategory ->
            Toast.makeText(this, "Kategori: ${selectedCategory.name}", Toast.LENGTH_SHORT).show()
            // TODO: Filter note berdasarkan kategori jika dibutuhkan
        }
        binding.recyclerCategory.adapter = categoryAdapter
        binding.recyclerCategory.layoutManager = LinearLayoutManager(this)

        // Setup RecyclerView
        binding.recyclerRecentNotes.layoutManager = GridLayoutManager(this, 2)
        binding.recyclerRecentNotes.adapter = adapter
        val swipeCallback = object : ItemTouchHelper.SimpleCallback(0, ItemTouchHelper.LEFT or ItemTouchHelper.RIGHT) {
            override fun onMove(
                recyclerView: RecyclerView,
                viewHolder: RecyclerView.ViewHolder,
                target: RecyclerView.ViewHolder
            ): Boolean = false

            override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {
                val position = viewHolder.adapterPosition
                val note = adapter.currentList[position]

                if (direction == ItemTouchHelper.LEFT && !note.isHidden && !note.isTrashed) {
                    // Hide Note
                    hideNote(note)
                } else if (direction == ItemTouchHelper.RIGHT && isViewingHidden) {
                    // Unhide Note
                    unhideNote(note)
                }

                refreshCurrentView()
            }


            override fun onChildDraw(
                c: Canvas,
                recyclerView: RecyclerView,
                viewHolder: RecyclerView.ViewHolder,
                dX: Float, dY: Float,
                actionState: Int, isCurrentlyActive: Boolean
            ) {
                val itemView = viewHolder.itemView
                val context = itemView.context

                if (dX < 0) { // Swipe ke kiri
                    val paint = Paint().apply {
                        color = ContextCompat.getColor(context, R.color.blue_active)
                    }

                    // Gambar latar belakang biru
                    c.drawRect(
                        itemView.right + dX, itemView.top.toFloat(),
                        itemView.right.toFloat(), itemView.bottom.toFloat(), paint
                    )

                    // Gambar ikon
                    val icon = ContextCompat.getDrawable(context, R.drawable.ic_hidden)
                    icon?.let {
                        val iconSize = 48 // Ukuran kecil (px)
                        val iconTop = itemView.top + (itemView.height - iconSize) / 2
                        val iconLeft = itemView.right - iconSize - 32
                        val iconRight = itemView.right - 32
                        val iconBottom = iconTop + iconSize

                        it.setBounds(iconLeft, iconTop, iconRight, iconBottom)
                        it.draw(c)
                    }

                    // Tampilkan teks "Hide note?"
                    val textPaint = Paint().apply {
                        color = Color.WHITE
                        textSize = 40f
                        isAntiAlias = true
                        typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                    }
                    val text = "Hide note?"
                    val textX = itemView.right - 250f
                    val textY = itemView.top + itemView.height / 2f + 15f
                    c.drawText(text, textX, textY, textPaint)
                }
                if (dX > 0 && isViewingHidden) {
                    val paint = Paint().apply {
                        color = ContextCompat.getColor(context, R.color.soft_green)
                    }

                    c.drawRect(
                        itemView.left.toFloat(), itemView.top.toFloat(),
                        itemView.left + dX, itemView.bottom.toFloat(), paint
                    )

                    // Gambar icon unhide (misal mata terbuka)
                    val icon = ContextCompat.getDrawable(context, R.drawable.ic_hidden)
                    icon?.let {
                        val iconSize = 48
                        val iconTop = itemView.top + (itemView.height - iconSize) / 2
                        val iconLeft = itemView.left + 32
                        val iconRight = iconLeft + iconSize
                        val iconBottom = iconTop + iconSize

                        it.setBounds(iconLeft, iconTop, iconRight, iconBottom)
                        it.draw(c)
                    }

                    // Teks "Unhide note?"
                    val textPaint = Paint().apply {
                        color = Color.WHITE
                        textSize = 40f
                        isAntiAlias = true
                        typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                    }
                    c.drawText("Unhide note?", itemView.left + 100f, itemView.top + itemView.height / 2f + 15f, textPaint)
                }
                super.onChildDraw(c, recyclerView, viewHolder, dX, dY, actionState, isCurrentlyActive)
            }

        }

        ItemTouchHelper(swipeCallback).attachToRecyclerView(binding.recyclerRecentNotes)

        binding.fabAddNote.setOnClickListener {
            val intent = Intent(this, AddEditNoteActivity::class.java)
            startActivity(intent)
        }

        binding.btnTrash.setOnClickListener {
            val db = dbHelper.readableDatabase
            val trashedNotes = NoteDao.getAll(db).filter { it.isTrashed }
            adapter.displayMode = NoteDisplayMode.TRASH
            adapter.submitList(trashedNotes)
            binding.recentNotesTitle.text = "Trashed Notes"

            resetAllQuickButtons()
            setQuickButtonState(binding.btnTrash, true, R.color.red_active)
            isViewingTrash = true
            isViewingFavorite = false
            isViewingHidden = false
            binding.recyclerCategory.visibility = View.GONE
            binding.recyclerRecentNotes.visibility = View.VISIBLE
        }

        binding.btnCategory.setOnClickListener {
            binding.recyclerRecentNotes.visibility = View.GONE
            binding.recyclerCategory.visibility = View.VISIBLE
            val db = dbHelper.readableDatabase
            val categories = CategoryDao.getAll(db)
            adapter.displayMode = NoteDisplayMode.CATEGORY
            resetAllQuickButtons()
            setQuickButtonState(binding.btnCategory, true, R.color.brown_active)
            binding.recentNotesTitle.text = getString(R.string.categories)
            isViewingTrash = false
            isViewingFavorite = false
            isViewingHidden = false

        }
        binding.btnFavorite.setOnClickListener {
            val db = dbHelper.readableDatabase
            val favoriteNotes = NoteDao.getAll(db).filter { it.isFavorite && !it.isTrashed && !it.isHidden}
            adapter.displayMode = NoteDisplayMode.FAVORITE
            adapter.submitList(favoriteNotes)
            binding.recentNotesTitle.text = "Favorite Notes"
            resetAllQuickButtons()
            setQuickButtonState(binding.btnFavorite, true, R.color.yellow_active)
            isViewingTrash = false
            isViewingFavorite = true
            isViewingHidden = false
            binding.recyclerCategory.visibility = View.GONE
            binding.recyclerRecentNotes.visibility = View.VISIBLE
        }

        binding.btnHidden.setOnClickListener {
            val intent = Intent(this, VerifyPinActivity::class.java)
            verifyPinLauncher.launch(Intent(this, VerifyPinActivity::class.java))
            binding.recyclerCategory.visibility = View.GONE
            binding.recyclerRecentNotes.visibility = View.VISIBLE
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
            binding.recyclerCategory.visibility = View.GONE
            binding.recyclerRecentNotes.visibility = View.VISIBLE
        }
        binding.bottomNavigation.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.menu_notes -> {
                    // Tampilkan Home (All Notes)
                    loadNotes()
                    true
                }
                R.id.menu_setting -> {
                    // Arahkan ke SettingsActivity
                    val intent = Intent(this, SettingsActivity::class.java)
                    startActivity(intent)
                    true
                }
                else -> false
            }
        }

//
        insertDummyNotesIfEmpty()
        insertDummyCategoriesIfEmpty()


        loadNotes()
    }


    override fun onResume() {
        super.onResume()
        if (!isViewingHidden && !isViewingTrash && !isViewingFavorite) {
            loadNotes()
        }
    }

    private fun loadNotes() {
        val db = dbHelper.readableDatabase
        val notes = NoteDao.getAll(db).filter { !it.isTrashed and !it.isHidden } // Hanya tampilkan yang tidak dihapus
        adapter.displayMode = NoteDisplayMode.ALL
        adapter.submitList(notes)
    }
    private fun showHiddenNotes(){
        val db = dbHelper.readableDatabase
        val hiddenNotes = NoteDao.getAll(db).filter { it.isHidden }
        adapter.displayMode = NoteDisplayMode.HIDDEN
        Log.d("MainActivity", "Jumlah hidden notes: ${hiddenNotes.size}")
        adapter.submitList(hiddenNotes)
        binding.recentNotesTitle.text = "Hidden Notes"
        resetAllQuickButtons()
        setQuickButtonState(binding.btnHidden, true, R.color.blue_active)
        isViewingTrash = false
        isViewingFavorite = false
        isViewingHidden = true
    }

    private fun hideNote(note: Note) {
        val updatedNote = note.copy(isHidden = true)
        NoteDao.update(dbHelper.writableDatabase, updatedNote)
        val updatedList = adapter.currentList.filter { it.id != note.id }
        adapter.submitList(updatedList)
        Toast.makeText(this, "Catatan disembunyikan", Toast.LENGTH_SHORT).show()
    }

    private fun unhideNote(note: Note) {
        val updatedNote = note.copy(isHidden = false)
        NoteDao.update(dbHelper.writableDatabase, updatedNote)
        val updatedList = adapter.currentList.filter { it.id != note.id }
        adapter.submitList(updatedList)
        Toast.makeText(this, "Catatan ditampilkan", Toast.LENGTH_SHORT).show()
    }

    private fun refreshCurrentView() {
        when {
            isViewingTrash -> loadTrashedNotes()
            isViewingFavorite -> {
                val db = dbHelper.readableDatabase
                val favoriteNotes = NoteDao.getAll(db).filter { it.isFavorite && !it.isTrashed && !it.isHidden }
                adapter.submitList(favoriteNotes)
            }
            isViewingHidden -> showHiddenNotes()
            else -> loadNotes()
        }
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
                    categoryId = 2,
                    isHidden = true
                ),
                Note(
                    title = "Important",
                    content = "Catatan ini penting!",
                    categoryId = 3,
                    isTrashed = true
                )
            )
            dummyNotes.forEach { NoteDao.insert(db, it) }
        }
    }
    private fun insertDummyCategoriesIfEmpty(){
        val db = dbHelper.writableDatabase
        val categories = CategoryDao.getAll(db)
        if (categories.isEmpty()){
            val dummyCategory = listOf(
                Category(
                    name = "Food",
                    colorHex = "#FF6F61"
                ),
                Category(
                    name = "Bill",
                    colorHex = "#FFD700"
                ),
                Category(
                    name = "Work",
                    colorHex = "#4B9CD3"
                ),
                Category(
                    name = "Idea",
                    colorHex = "#9C27B0"
                ),
                Category(
                    name = "Health",
                    colorHex = "#4CAF50"
                )
            )
            dummyCategory.forEach { CategoryDao.insert(db, it) }
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
        adapter.displayMode = NoteDisplayMode.TRASH
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
