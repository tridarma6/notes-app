package com.example.notesapp.ui

import CategoriesAdapter
import NoteDisplayMode
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
    // Tambahkan variabel ini untuk melacak mode tampilan saat ini
    private var currentDisplayMode: NoteDisplayMode = NoteDisplayMode.ALL

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

    // Gunakan ActivityResultLauncher untuk AddEditNoteActivity juga
    private val addEditNoteLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        // Ketika kembali dari AddEditNoteActivity, kita ingin kembali ke tampilan "All Notes"
        // Tidak perlu cek result code di sini jika hanya untuk refresh tampilan utama
        resetToAllNotesView()
    }


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        dbHelper = NotesDatabaseHelper(this)

        insertDummyCategoriesIfEmpty()
        insertDummyNotesIfEmpty()


        adapter = NotesAdapter(
            onItemClick = { note ->
                // Gunakan launcher baru di sini
                val intent = Intent(this, AddEditNoteActivity::class.java).apply {
                    putExtra("EXTRA_NOTE_ID", note.id)
                }
                addEditNoteLauncher.launch(intent)
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
                // Refresh tampilan saat ini setelah toggle favorite
                refreshCurrentView()
            }
        )

        categoryList = CategoryDao.getAll(dbHelper.readableDatabase)
        categoryAdapter = CategoriesAdapter(categoryList) { selectedCategory ->
            Toast.makeText(this, "Kategori: ${selectedCategory.name}", Toast.LENGTH_SHORT).show()
            val db = dbHelper.readableDatabase
            val filteredNotes = NoteDao.getAll(db).filter { note ->
                note.categoryId == selectedCategory.id && !note.isTrashed && !note.isHidden
            }
            adapter.displayMode = NoteDisplayMode.ALL
            adapter.submitList(filteredNotes)

            binding.recyclerCategory.visibility = View.GONE
            binding.recyclerRecentNotes.visibility = View.VISIBLE
            binding.recentNotesTitle.text = "Notes in ${selectedCategory.name}"
            resetAllQuickButtons() // Reset tombol quick filter
            currentDisplayMode = NoteDisplayMode.CATEGORY // Set mode
        }
        binding.recyclerCategory.adapter = categoryAdapter
        binding.recyclerCategory.layoutManager = LinearLayoutManager(this, LinearLayoutManager.VERTICAL, false)
        binding.recyclerCategory.visibility = View.GONE // Default sembunyikan


        binding.recyclerRecentNotes.layoutManager = GridLayoutManager(this, 2)
        binding.recyclerRecentNotes.adapter = adapter
        binding.recyclerRecentNotes.visibility = View.VISIBLE


        val swipeCallback = object : ItemTouchHelper.SimpleCallback(0, ItemTouchHelper.LEFT or ItemTouchHelper.RIGHT) {

            override fun onMove(
                recyclerView: RecyclerView,
                viewHolder: RecyclerView.ViewHolder,
                target: RecyclerView.ViewHolder
            ): Boolean = false

            // --- BARU: Metode ini akan menentukan arah swipe yang diizinkan ---
            override fun getMovementFlags(
                recyclerView: RecyclerView,
                viewHolder: RecyclerView.ViewHolder
            ): Int {
                val position = viewHolder.adapterPosition
                // Pastikan posisi valid sebelum mengakses adapter.currentList
                if (position == RecyclerView.NO_POSITION || position >= adapter.currentList.size) {
                    return makeMovementFlags(0, 0) // Tidak ada swipe jika posisi tidak valid
                }
                val note = adapter.currentList[position]

                var swipeFlags = 0

                // Izinkan swipe ke KIRI (untuk Sembunyikan) HANYA JIKA:
                // 1. Catatan belum disembunyikan (!note.isHidden)
                // 2. Catatan belum di tempat sampah (!note.isTrashed)
                if (!note.isHidden && !note.isTrashed) {
                    swipeFlags = swipeFlags or ItemTouchHelper.LEFT
                }

                // Izinkan swipe ke KANAN (untuk Tampilkan/Unhide) HANYA JIKA:
                // 1. Kita sedang melihat mode hidden (isViewingHidden)
                // 2. Catatan tersebut memang dalam status hidden (note.isHidden)
                if (isViewingHidden && note.isHidden) {
                    swipeFlags = swipeFlags or ItemTouchHelper.RIGHT
                }

                return makeMovementFlags(0, swipeFlags) // 0 untuk drag, swipeFlags untuk swipe
            }
            // --- AKHIR BARU ---

            override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {
                val position = viewHolder.adapterPosition
                // Pastikan posisi valid sebelum mengakses adapter.currentList
                if (position == RecyclerView.NO_POSITION || position >= adapter.currentList.size) {
                    adapter.notifyItemChanged(position) // Reset tampilan jika ada error
                    return
                }
                val note = adapter.currentList[position]

                // Logika di sini sudah benar, karena getMovementFlags sudah memfilter.
                // Namun, kita bisa menambahkan pengecekan defensif lagi jika diperlukan.
                if (direction == ItemTouchHelper.LEFT) {
                    hideNote(note) // note.isHidden dan note.isTrashed sudah dijamin false oleh getMovementFlags
                } else if (direction == ItemTouchHelper.RIGHT) {
                    unhideNote(note) // isViewingHidden dan note.isHidden sudah dijamin true oleh getMovementFlags
                }
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
                val position = viewHolder.adapterPosition // Dapatkan posisi item
                // Dapatkan catatan lagi untuk pengecekan kondisi visual
                val note =
                    if (position != RecyclerView.NO_POSITION && position < adapter.currentList.size) {
                        adapter.currentList[position]
                    } else {
                        // Item mungkin sudah dihapus atau tidak valid, jangan gambar
                        super.onChildDraw(
                            c,
                            recyclerView,
                            viewHolder,
                            dX,
                            dY,
                            actionState,
                            isCurrentlyActive
                        )
                        return
                    }


                if (dX < 0) { // Swipe ke kiri
                    // Pastikan visual hanya muncul jika swipe diizinkan untuk menyembunyikan
                    if (!note.isHidden && !note.isTrashed) { // <--- TAMBAHKAN KONDISI INI DI SINI
                        val paint = Paint().apply {
                            color = ContextCompat.getColor(context, R.color.blue_active)
                        }

                        c.drawRect(
                            itemView.right + dX, itemView.top.toFloat(),
                            itemView.right.toFloat(), itemView.bottom.toFloat(), paint
                        )

                        val icon = ContextCompat.getDrawable(context, R.drawable.ic_hidden)
                        icon?.let {
                            val iconSize = 48
                            val iconTop = itemView.top + (itemView.height - iconSize) / 2
                            val iconLeft = itemView.right - iconSize - 32
                            val iconRight = itemView.right - 32
                            val iconBottom = iconTop + iconSize

                            it.setBounds(iconLeft, iconTop, iconRight, iconBottom)
                            it.draw(c)
                        }

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
                }
                // Perbaiki kondisi swipe kanan di onChildDraw
                // Hanya gambar jika memang sedang di tampilan hidden DAN catatan itu hidden
                if (dX > 0 && isViewingHidden && note.isHidden) { // <--- PERBAIKI KONDISI INI
                    val paint = Paint().apply {
                        color = ContextCompat.getColor(context, R.color.soft_green)
                    }

                    c.drawRect(
                        itemView.left.toFloat(), itemView.top.toFloat(),
                        itemView.left + dX, itemView.bottom.toFloat(), paint
                    )

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

                    val textPaint = Paint().apply {
                        color = Color.WHITE
                        textSize = 40f
                        isAntiAlias = true
                        typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                    }
                    c.drawText(
                        "Unhide note?",
                        itemView.left + 100f,
                        itemView.top + itemView.height / 2f + 15f,
                        textPaint
                    )
                }
                super.onChildDraw(
                    c,
                    recyclerView,
                    viewHolder,
                    dX,
                    dY,
                    actionState,
                    isCurrentlyActive
                )
            }
        }

        ItemTouchHelper(swipeCallback).attachToRecyclerView(binding.recyclerRecentNotes)

        binding.fabAddNote.setOnClickListener {
            // Gunakan launcher baru di sini
            val intent = Intent(this, AddEditNoteActivity::class.java)
            addEditNoteLauncher.launch(intent)
        }

        binding.btnTrash.setOnClickListener {
            loadTrashedNotes()
            adapter.displayMode = NoteDisplayMode.TRASH
            binding.recentNotesTitle.text = "Trashed Notes"

            resetAllQuickButtons()
            setQuickButtonState(binding.btnTrash, true, R.color.red_active)
            isViewingTrash = true
            isViewingFavorite = false
            isViewingHidden = false
            binding.recyclerCategory.visibility = View.GONE
            binding.recyclerRecentNotes.visibility = View.VISIBLE
            currentDisplayMode = NoteDisplayMode.TRASH // Set mode
        }

        binding.btnCategory.setOnClickListener {
            binding.recyclerRecentNotes.visibility = View.GONE
            binding.recyclerCategory.visibility = View.VISIBLE

            val db = dbHelper.readableDatabase
            categoryList = CategoryDao.getAll(db)
            (binding.recyclerCategory.adapter as CategoriesAdapter).updateCategories(categoryList)


            adapter.displayMode = NoteDisplayMode.CATEGORY
            resetAllQuickButtons()
            setQuickButtonState(binding.btnCategory, true, R.color.brown_active)
            binding.recentNotesTitle.text = getString(R.string.categories)
            isViewingTrash = false
            isViewingFavorite = false
            isViewingHidden = false
            currentDisplayMode = NoteDisplayMode.CATEGORY // Set mode
        }

        binding.btnFavorite.setOnClickListener {
            val db = dbHelper.readableDatabase
            val favoriteNotes = NoteDao.getAll(db).filter { it.isFavorite && !it.isTrashed && !it.isHidden}
            adapter.displayMode = NoteDisplayMode.FAVORITE
            adapter.submitList(favoriteNotes)
            binding.recentNotesTitle.text = "Favorite Notes"
            resetAllQuickButtons()
            setQuickButtonState(binding.btnFavorite, true, R.color.yellow_active)
            isViewingTrash = false // Perbaiki: set ini ke false
            isViewingFavorite = true
            isViewingHidden = false
            binding.recyclerCategory.visibility = View.GONE
            binding.recyclerRecentNotes.visibility = View.VISIBLE
            currentDisplayMode = NoteDisplayMode.FAVORITE // Set mode
        }

        binding.btnHidden.setOnClickListener {
            verifyPinLauncher.launch(Intent(this, VerifyPinActivity::class.java))
            binding.recyclerCategory.visibility = View.GONE
            binding.recyclerRecentNotes.visibility = View.VISIBLE
            // currentDisplayMode akan diatur di showHiddenNotes setelah verifikasi PIN
        }

        binding.btnAllNotes.setOnClickListener {
            resetToAllNotesView() // Panggil metode baru
        }

        binding.bottomNavigation.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.menu_notes -> {
                    resetToAllNotesView()
                    true
                }
                R.id.menu_event -> {
                    val intent = Intent(this, EventActivity::class.java)
                    startActivity(intent)
                    true
                }
                R.id.menu_setting -> {
                    val intent = Intent(this, SettingsActivity::class.java)
                    startActivity(intent)
                    true
                }
                else -> false
            }
        }

        // Panggil ini di akhir onCreate untuk menampilkan catatan awal
        resetToAllNotesView()
    }


    override fun onResume() {
        super.onResume()
        // Kita tidak lagi mengandalkan isViewingX di onResume untuk refresh utama
        // Sebaliknya, jika currentDisplayMode adalah ALL (normal), kita refresh
        // Jika tidak, kita biarkan refreshCurrentView() yang menangani
        if (currentDisplayMode == NoteDisplayMode.ALL) {
            loadNotes()
        }
        // Pastikan daftar kategori juga diperbarui setiap kali onResume dipanggil
        val db = dbHelper.readableDatabase
        categoryList = CategoryDao.getAll(db)
        (binding.recyclerCategory.adapter as CategoriesAdapter).updateCategories(categoryList)
    }

    private fun loadNotes() {
        val db = dbHelper.readableDatabase
        val notes = NoteDao.getAll(db).filter { !it.isTrashed && !it.isHidden }
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
        currentDisplayMode = NoteDisplayMode.HIDDEN // Set mode
    }

    private fun hideNote(note: Note) {
        val updatedNote = note.copy(isHidden = true)
        NoteDao.update(dbHelper.writableDatabase, updatedNote)
        refreshCurrentView()
        Toast.makeText(this, "Catatan disembunyikan", Toast.LENGTH_SHORT).show()
    }

    private fun unhideNote(note: Note) {
        val updatedNote = note.copy(isHidden = false)
        NoteDao.update(dbHelper.writableDatabase, updatedNote)
        refreshCurrentView()
        Toast.makeText(this, "Catatan ditampilkan", Toast.LENGTH_SHORT).show()
    }

    private fun refreshCurrentView() {
        when (currentDisplayMode) { // Gunakan currentDisplayMode
            NoteDisplayMode.TRASH -> loadTrashedNotes()
            NoteDisplayMode.FAVORITE -> {
                val db = dbHelper.readableDatabase
                val favoriteNotes = NoteDao.getAll(db).filter { it.isFavorite && !it.isTrashed && !it.isHidden }
                adapter.submitList(favoriteNotes)
            }
            NoteDisplayMode.HIDDEN -> showHiddenNotes()
            else -> loadNotes() // Default ke ALL
        }
    }

    // --- Metode Baru untuk Reset ke Tampilan "All Notes" ---
    private fun resetToAllNotesView() {
        loadNotes()
        binding.recentNotesTitle.text = getString(R.string.recent_notes)

        binding.recyclerRecentNotes.setBackgroundColor(
            ContextCompat.getColor(this, android.R.color.transparent)
        )

        resetAllQuickButtons()
        setQuickButtonState(binding.btnAllNotes, true, R.color.green_active)
        isViewingTrash = false
        isViewingFavorite = false
        isViewingHidden = false
        currentDisplayMode = NoteDisplayMode.ALL // Pastikan mode diatur ke ALL
        binding.recyclerCategory.visibility = View.GONE
        binding.recyclerRecentNotes.visibility = View.VISIBLE
    }


    private fun insertDummyNotesIfEmpty() {
        val db = dbHelper.writableDatabase
        val notes = NoteDao.getAll(db)
        if (notes.isEmpty()) {
            val uncategorizedId = CategoryDao.getByName(db, "Uncategorized")?.id
            val foodId = CategoryDao.getByName(db, "Food")?.id
            val billId = CategoryDao.getByName(db, "Bill")?.id

            val dummyNotes = listOf(
                Note(
                    title = "Getting Started",
                    content = "Selamat datang di aplikasi NotesApp.",
                    categoryId = uncategorizedId,
                    isFavorite = true
                ),
                Note(
                    title = "UX Design",
                    content = "Tips dan trik untuk desain UX.",
                    categoryId = foodId,
                    isHidden = true
                ),
                Note(
                    title = "Important",
                    content = "Catatan ini penting!",
                    categoryId = billId,
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
                    name = "Uncategorized",
                    colorHex = "#9E9E9E"
                ),
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
                // Refresh tampilan saat ini setelah memindahkan ke trash
                refreshCurrentView()
                Toast.makeText(this, "Catatan dipindahkan ke trash", Toast.LENGTH_SHORT).show()
            }
            .setNegativeButton("Batal", null)
            .create()

        dialog.show()

        dialog.getButton(AlertDialog.BUTTON_POSITIVE)?.setTextColor(getColor(R.color.red))
        dialog.getButton(AlertDialog.BUTTON_NEGATIVE)?.setTextColor(getColor(R.color.gray))
    }
    private fun showPermanentDeleteDialog(note: Note) {
        val dialog = AlertDialog.Builder(this)
            .setTitle("Hapus Permanen")
            .setMessage("Catatan ini akan dihapus secara permanen dan tidak bisa dikembalikan. Lanjutkan?")
            .setPositiveButton("Hapus") { _, _ ->
                NoteDao.deletePermanent(dbHelper.writableDatabase, note.id)
                // Refresh tampilan saat ini setelah menghapus permanen
                refreshCurrentView()
                Toast.makeText(this, "Catatan dihapus permanen", Toast.LENGTH_SHORT).show()
            }
            .setNegativeButton("Batal", null)
            .create()

        dialog.show()
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
            button.setBackgroundResource(R.drawable.button_background)
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
            setQuickButtonState(it, false, R.color.green_active)
        }
    }

    // Anda mungkin juga memiliki NoteDisplayMode enum, pastikan itu sudah ada
    // Jika belum, tambahkan ini di luar kelas MainActivity atau di file terpisah

}
