package com.example.notesapp.ui

import CategoriesAdapter
import NotesAdapter
import NoteDisplayMode
import android.content.Context
import android.content.Intent
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import android.graphics.drawable.Drawable
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import android.widget.Button
import android.widget.TextView // Import TextView untuk dialog
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.ColorInt
import androidx.annotation.ColorRes
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.notesapp.R
import com.example.notesapp.adapter.CategoryManageAdapter // Adapter baru untuk manajemen
import com.example.notesapp.data.dao.CategoryDao
import com.example.notesapp.data.dao.NoteDao
import com.example.notesapp.data.database.NotesDatabaseHelper
import com.example.notesapp.data.model.Category
import com.example.notesapp.data.model.Note
import com.example.notesapp.databinding.ActivityMainBinding
import com.example.notesapp.ui.adapter.ColorPickerAdapter
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

// --- Pastikan enum NoteDisplayMode ada ---
enum class NoteDisplayMode {
    ALL, TRASH, FAVORITE, HIDDEN, CATEGORY, MANAGE_CATEGORIES
}

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var dbHelper: NotesDatabaseHelper
    private lateinit var notesAdapter: NotesAdapter // Mengubah nama variabel dari 'adapter' menjadi 'notesAdapter' agar lebih jelas
    private lateinit var categoryFilterAdapter: CategoriesAdapter // Untuk RecyclerView kategori filter (yang sebelumnya)
    private lateinit var categoryManageAdapter: CategoryManageAdapter // PERUBAHAN UTAMA: Adapter untuk manajemen kategori
    private var isViewingTrash = false
    private var isViewingFavorite = false
    private var isViewingHidden = false
    private var currentDisplayMode: NoteDisplayMode = NoteDisplayMode.ALL
    private var searchQuery: String = ""
    private var selectedCategoryId: Long? = null
    private var selectedCategoryName: String? = null
    private var searchJob: Job? = null
    private var selectedCategoryColor: String? = null // Untuk dialog edit/add category

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

    private val addEditNoteLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        resetToAllNotesView()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        dbHelper = NotesDatabaseHelper(this)

        insertDummyCategoriesIfEmpty()
        insertDummyNotesIfEmpty()

        notesAdapter = NotesAdapter(
            onItemClick = { note ->
                val intent = Intent(this, AddEditNoteActivity::class.java).apply {
                    putExtra("EXTRA_NOTE_ID", note.id)
                    putExtra("ORIGINAL_DISPLAY_MODE", currentDisplayMode.name)
                    if (currentDisplayMode == NoteDisplayMode.CATEGORY && selectedCategoryId != null) {
                        putExtra("SELECTED_CATEGORY_ID", selectedCategoryId)
                    }
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
                refreshCurrentView()
            }
        )
        binding.recyclerRecentNotes.layoutManager = GridLayoutManager(this, 2)
        binding.recyclerRecentNotes.adapter = notesAdapter
        binding.recyclerRecentNotes.visibility = View.VISIBLE

        // Inisialisasi categoryFilterAdapter (untuk tampilan filter)
        categoryFilterAdapter = CategoriesAdapter(emptyList()) { selectedCategory ->
            Toast.makeText(this, "Kategori Filter: ${selectedCategory.name}", Toast.LENGTH_SHORT).show()
            this.selectedCategoryId = selectedCategory.id.toLong()
            this.selectedCategoryName = selectedCategory.name
            currentDisplayMode = NoteDisplayMode.CATEGORY
            refreshCurrentView()
            resetAllQuickButtons()
            setQuickButtonState(binding.btnCategory, true, R.color.brown_active)
            isViewingTrash = false
            isViewingFavorite = false
            isViewingHidden = false
            // Pastikan recyclerCategory kembali tersembunyi setelah memilih filter
            binding.recyclerCategory.visibility = View.GONE
            binding.recyclerRecentNotes.visibility = View.VISIBLE
        }
        // Atur layout manager untuk RecyclerView Kategori Filter
        binding.recyclerCategory.layoutManager = LinearLayoutManager(this, LinearLayoutManager.VERTICAL, false)
        // Adapter akan diatur saat dialog manajemen kategori dipilih atau saat btnCategory diklik

        // Inisialisasi categoryManageAdapter (untuk manajemen kategori)
        categoryManageAdapter = CategoryManageAdapter(
            mutableListOf(),
            onEditClick = { category ->
                showAddEditCategoryDialog(category) // Panggil dialog untuk edit
            },
            onDeleteClick = { category ->
                showDeleteCategoryConfirmationDialog(category) // Panggil dialog konfirmasi hapus
            }
        )

        val swipeCallback = object : ItemTouchHelper.SimpleCallback(0, ItemTouchHelper.LEFT or ItemTouchHelper.RIGHT) {
            override fun onMove(
                recyclerView: RecyclerView,
                viewHolder: RecyclerView.ViewHolder,
                target: RecyclerView.ViewHolder
            ): Boolean = false

            override fun getMovementFlags(
                recyclerView: RecyclerView,
                viewHolder: RecyclerView.ViewHolder
            ): Int {
                val position = viewHolder.adapterPosition
                if (position == RecyclerView.NO_POSITION || position >= notesAdapter.currentList.size) {
                    return makeMovementFlags(0, 0)
                }
                val note = notesAdapter.currentList[position]

                var swipeFlags = 0
                // Swipe ke kiri: Sembunyikan catatan (jika tidak tersembunyi dan tidak di tempat sampah)
                if (!note.isHidden && !note.isTrashed) {
                    swipeFlags = swipeFlags or ItemTouchHelper.LEFT
                }
                // Swipe ke kanan: Tampilkan catatan (jika tersembunyi)
                if (isViewingHidden && note.isHidden) {
                    swipeFlags = swipeFlags or ItemTouchHelper.RIGHT
                }
                // --- PERUBAHAN UTAMA: Tambahkan swipe ke kanan untuk mengembalikan dari trash ---
                if (isViewingTrash && note.isTrashed) {
                    swipeFlags = swipeFlags or ItemTouchHelper.RIGHT // Restore note from trash
                }
                // --- END PERUBAHAN UTAMA ---
                return makeMovementFlags(0, swipeFlags)
            }

            override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {
                val position = viewHolder.adapterPosition
                if (position == RecyclerView.NO_POSITION || position >= notesAdapter.currentList.size) {
                    notesAdapter.notifyItemChanged(position)
                    return
                }
                val note = notesAdapter.currentList[position]

                if (direction == ItemTouchHelper.LEFT) {
                    // Logic untuk hide note
                    if (!note.isHidden && !note.isTrashed) { // Pastikan hanya berlaku untuk catatan non-hidden/non-trashed
                        hideNote(note)
                    } else {
                        // Jika bukan kondisi untuk hide, kembalikan item ke posisi semula
                        notesAdapter.notifyItemChanged(position)
                    }
                } else if (direction == ItemTouchHelper.RIGHT) {
                    // Logic untuk unhide note
                    if (isViewingHidden && note.isHidden) {
                        unhideNote(note)
                    }
                    // --- PERUBAHAN UTAMA: Panggil restoreNote jika sedang di trash ---
                    else if (isViewingTrash && note.isTrashed) {
                        restoreNote(note)
                    }
                    // --- END PERUBAHAN UTAMA ---
                    else {
                        // Jika bukan kondisi untuk unhide atau restore, kembalikan item
                        notesAdapter.notifyItemChanged(position)
                    }
                }
            }

            override fun onChildDraw(
                c: Canvas, recyclerView: RecyclerView, viewHolder: RecyclerView.ViewHolder,
                dX: Float, dY: Float, actionState: Int, isCurrentlyActive: Boolean
            ) {
                val itemView = viewHolder.itemView
                val context = itemView.context
                val position = viewHolder.adapterPosition
                val note =
                    if (position != RecyclerView.NO_POSITION && position < notesAdapter.currentList.size) {
                        notesAdapter.currentList[position]
                    } else {
                        super.onChildDraw(c, recyclerView, viewHolder, dX, dY, actionState, isCurrentlyActive)
                        return
                    }

                val textPaint = Paint().apply {
                    color = Color.WHITE; textSize = 40f; isAntiAlias = true; typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                }
                val iconSize = 48
                val textYOffset = (itemView.height / 2f) + (textPaint.textSize / 3)

                // Swipe ke kiri: Hide note
                if (dX < 0 && !note.isHidden && !note.isTrashed) {
                    val paint = Paint().apply { color = ContextCompat.getColor(context, R.color.blue_active) }
                    c.drawRect(itemView.right + dX, itemView.top.toFloat(), itemView.right.toFloat(), itemView.bottom.toFloat(), paint)

                    val icon = ContextCompat.getDrawable(context, R.drawable.ic_hidden) // Pastikan Anda memiliki ikon ini
                    icon?.let {
                        val iconLeft = itemView.right - iconSize - 32
                        val iconTop = itemView.top + (itemView.height - iconSize) / 2
                        it.setBounds(iconLeft, iconTop, iconLeft + iconSize, iconTop + iconSize)
                        it.draw(c)
                    }
                    c.drawText("Hide note?", itemView.right - 250f, textYOffset, textPaint)
                }
                // Swipe ke kanan: Unhide note (jika sedang melihat hidden notes)
                else if (dX > 0 && isViewingHidden && note.isHidden) {
                    val paint = Paint().apply { color = ContextCompat.getColor(context, R.color.soft_green) } // Gunakan warna hijau
                    c.drawRect(itemView.left.toFloat(), itemView.top.toFloat(), itemView.left + dX, itemView.bottom.toFloat(), paint)

                    val icon = ContextCompat.getDrawable(context, R.drawable.ic_eye) // Anda mungkin ingin ikon "terlihat"
                    icon?.let {
                        val iconLeft = itemView.left + 32
                        val iconTop = itemView.top + (itemView.height - iconSize) / 2
                        it.setBounds(iconLeft, iconTop, iconLeft + iconSize, iconTop + iconSize)
                        it.draw(c)
                    }
                    c.drawText("Unhide note?", itemView.left + 100f, textYOffset, textPaint)
                }
                // --- PERUBAHAN UTAMA: Swipe ke kanan untuk Restore note (jika sedang melihat trash notes) ---
                else if (dX > 0 && isViewingTrash && note.isTrashed) {
                    val paint = Paint().apply { color = ContextCompat.getColor(context, R.color.soft_blue) } // Warna biru untuk restore
                    c.drawRect(itemView.left.toFloat(), itemView.top.toFloat(), itemView.left + dX, itemView.bottom.toFloat(), paint)

                    val icon = ContextCompat.getDrawable(context, R.drawable.ic_restore) // Anda butuh ikon restore
                    icon?.let {
                        val iconLeft = itemView.left + 32
                        val iconTop = itemView.top + (itemView.height - iconSize) / 2
                        it.setBounds(iconLeft, iconTop, iconLeft + iconSize, iconTop + iconSize)
                        it.draw(c)
                    }
                    c.drawText("Restore note?", itemView.left + 100f, textYOffset, textPaint)
                }
                // --- END PERUBAHAN UTAMA ---
                super.onChildDraw(c, recyclerView, viewHolder, dX, dY, actionState, isCurrentlyActive)
            }
        }
        ItemTouchHelper(swipeCallback).attachToRecyclerView(binding.recyclerRecentNotes)

        binding.fabAddNote.setOnClickListener {
            val intent = Intent(this, AddEditNoteActivity::class.java).apply {
                putExtra("ORIGINAL_DISPLAY_MODE", currentDisplayMode.name)
                if (currentDisplayMode == NoteDisplayMode.CATEGORY && selectedCategoryId != null) {
                    putExtra("SELECTED_CATEGORY_ID", selectedCategoryId)
                }
            }
            addEditNoteLauncher.launch(intent)
        }

        binding.searchBar.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                searchJob?.cancel()
                searchJob = lifecycleScope.launch {
                    delay(300)
                    searchQuery = s.toString().trim()
                    refreshCurrentView()
                }
            }
            override fun afterTextChanged(s: Editable?) {}
        })

        binding.searchBar.setOnEditorActionListener { v, actionId, event ->
            if (actionId == EditorInfo.IME_ACTION_SEARCH || actionId == EditorInfo.IME_ACTION_DONE) {
                searchJob?.cancel()
                searchQuery = v.text.toString().trim()
                refreshCurrentView()
                val imm = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
                imm.hideSoftInputFromWindow(v.windowToken, 0)
                binding.searchBar.clearFocus()
                true
            } else {
                false
            }
        }

        binding.btnTrash.setOnClickListener {
            currentDisplayMode = NoteDisplayMode.TRASH
            refreshCurrentView()
            resetAllQuickButtons()
            setQuickButtonState(binding.btnTrash, true, R.color.red_active)
            isViewingTrash = true
            isViewingFavorite = false
            isViewingHidden = false
        }

        // --- START PERUBAHAN UTAMA: Listener untuk btnCategory yang Multifungsi ---
        binding.btnCategory.setOnClickListener {
            showCategorySelectionDialog()
        }
        // --- END PERUBAHAN UTAMA ---

        binding.btnFavorite.setOnClickListener {
            currentDisplayMode = NoteDisplayMode.FAVORITE
            refreshCurrentView()
            resetAllQuickButtons()
            setQuickButtonState(binding.btnFavorite, true, R.color.yellow_active)
            isViewingTrash = false
            isViewingFavorite = true
            isViewingHidden = false
        }

        binding.btnHidden.setOnClickListener {
            val intent = Intent(this, VerifyPinActivity::class.java)
            verifyPinLauncher.launch(intent)
        }

        binding.btnAllNotes.setOnClickListener {
            currentDisplayMode = NoteDisplayMode.ALL
            refreshCurrentView()
            resetAllQuickButtons()
            setQuickButtonState(binding.btnAllNotes, true, R.color.green_active)
            isViewingTrash = false
            isViewingFavorite = false
            isViewingHidden = false
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

        resetToAllNotesView() // Panggil ini di akhir onCreate untuk menampilkan catatan awal
    }


    override fun onResume() {
        super.onResume()
        searchQuery = binding.searchBar.text.toString().trim()
        refreshCurrentView()

        // Perbarui adapter kategori yang sesuai berdasarkan mode tampilan saat ini
        val db = dbHelper.readableDatabase
        when (currentDisplayMode) {
            NoteDisplayMode.CATEGORY -> {
                // Saat dalam mode CATEGORY (filter), pastikan categoryFilterAdapter yang aktif
                val categoriesForFilter = CategoryDao.getAll(db)
                (binding.recyclerCategory.adapter as? CategoriesAdapter)?.updateCategories(categoriesForFilter)
            }
            NoteDisplayMode.MANAGE_CATEGORIES -> {
                // Saat dalam mode MANAGE_CATEGORIES, pastikan categoryManageAdapter yang aktif
                loadCategoriesForManage()
            }
            else -> {
                // Untuk mode catatan lainnya, tidak perlu update kategori jika recyclerCategory tersembunyi
            }
        }
    }

    private fun refreshCurrentView() {
        val db = dbHelper.readableDatabase
        var baseNotes: List<Note> = emptyList()
        var currentTitle: String = ""

        // Selalu sembunyikan kedua RecyclerViews terlebih dahulu, lalu tampilkan yang benar
        binding.recyclerRecentNotes.visibility = View.GONE
        binding.recyclerCategory.visibility = View.GONE

        when (currentDisplayMode) {
            NoteDisplayMode.ALL -> {
                baseNotes = NoteDao.getAll(db).filter { !it.isTrashed && !it.isHidden }
                currentTitle = getString(R.string.recent_notes)
                binding.recyclerRecentNotes.visibility = View.VISIBLE
                selectedCategoryId = null
                notesAdapter.displayMode = NoteDisplayMode.ALL // Pastikan displayMode diatur
                if (binding.recyclerRecentNotes.adapter != notesAdapter) { // Periksa dan set adapter notes
                    binding.recyclerRecentNotes.adapter = notesAdapter
                }
            }
            NoteDisplayMode.TRASH -> {
                baseNotes = NoteDao.getAll(db).filter { it.isTrashed }
                currentTitle = "Trashed Notes"
                binding.recyclerRecentNotes.visibility = View.VISIBLE
                selectedCategoryId = null
                notesAdapter.displayMode = NoteDisplayMode.TRASH // Pastikan displayMode diatur
                if (binding.recyclerRecentNotes.adapter != notesAdapter) { // Periksa dan set adapter notes
                    binding.recyclerRecentNotes.adapter = notesAdapter
                }
            }
            NoteDisplayMode.FAVORITE -> {
                baseNotes = NoteDao.getAll(db).filter { it.isFavorite && !it.isTrashed && !it.isHidden }
                currentTitle = "Favorite Notes"
                binding.recyclerRecentNotes.visibility = View.VISIBLE
                selectedCategoryId = null
                notesAdapter.displayMode = NoteDisplayMode.FAVORITE // Pastikan displayMode diatur
                if (binding.recyclerRecentNotes.adapter != notesAdapter) { // Periksa dan set adapter notes
                    binding.recyclerRecentNotes.adapter = notesAdapter
                }
            }
            NoteDisplayMode.HIDDEN -> {
                baseNotes = NoteDao.getAll(db).filter { it.isHidden }
                currentTitle = "Hidden Notes"
                binding.recyclerRecentNotes.visibility = View.VISIBLE
                selectedCategoryId = null
                notesAdapter.displayMode = NoteDisplayMode.HIDDEN // Pastikan displayMode diatur
                if (binding.recyclerRecentNotes.adapter != notesAdapter) { // Periksa dan set adapter notes
                    binding.recyclerRecentNotes.adapter = notesAdapter
                }
            }
            NoteDisplayMode.CATEGORY -> {
                // Jika selectedCategoryId masih null, berarti kita baru klik btnCategory dan ingin menampilkan daftar filter
                if (selectedCategoryId == null) {
                    currentTitle = getString(R.string.categories)
                    binding.recyclerCategory.visibility = View.VISIBLE
                    // Set adapter ke categoryFilterAdapter
                    if (binding.recyclerCategory.adapter != categoryFilterAdapter) { // Periksa dan set adapter filter
                        binding.recyclerCategory.adapter = categoryFilterAdapter
                    }
                    val categoriesForFilter = CategoryDao.getAll(db)
                    categoryFilterAdapter.updateCategories(categoriesForFilter)
                    notesAdapter.submitList(emptyList()) // Kosongkan daftar catatan
                    binding.recentNotesTitle.text = currentTitle
                    return // Keluar dari fungsi karena kita tidak menampilkan catatan
                } else {
                    // Jika kategori sudah dipilih, tampilkan catatan yang terfilter
                    baseNotes = NoteDao.getAll(db).filter {
                        it.categoryId?.toLong() == selectedCategoryId && !it.isTrashed && !it.isHidden
                    }
                    currentTitle = "Notes in ${selectedCategoryName ?: "Category"}"
                    binding.recyclerRecentNotes.visibility = View.VISIBLE
                    notesAdapter.displayMode = NoteDisplayMode.CATEGORY // Pastikan displayMode diatur
                    if (binding.recyclerRecentNotes.adapter != notesAdapter) { // Periksa dan set adapter notes
                        binding.recyclerRecentNotes.adapter = notesAdapter
                    }
                }
            }
            NoteDisplayMode.MANAGE_CATEGORIES -> {
                currentTitle = "Manage Categories"
                binding.recyclerCategory.visibility = View.VISIBLE
                // Set adapter ke categoryManageAdapter
                if (binding.recyclerCategory.adapter != categoryManageAdapter) { // Periksa dan set adapter manage
                    binding.recyclerCategory.adapter = categoryManageAdapter
                }
                loadCategoriesForManage() // Memuat dan memperbarui data untuk manajemen
                notesAdapter.submitList(emptyList()) // Kosongkan daftar catatan
                binding.recentNotesTitle.text = currentTitle
                return // Keluar dari fungsi karena kita tidak menampilkan catatan
            }
        }

        val finalNotes = if (searchQuery.isNotBlank()) {
            baseNotes.filter { note ->
                note.title.contains(searchQuery, ignoreCase = true) ||
                        note.content.contains(searchQuery, ignoreCase = true)
            }
        } else {
            baseNotes
        }

        notesAdapter.submitList(finalNotes)
        binding.recentNotesTitle.text = currentTitle
    }

    private fun showHiddenNotes(){
        currentDisplayMode = NoteDisplayMode.HIDDEN
        refreshCurrentView()
        resetAllQuickButtons()
        setQuickButtonState(binding.btnHidden, true, R.color.blue_active)
        isViewingTrash = false
        isViewingFavorite = false
        isViewingHidden = true
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

    // --- PERUBAHAN UTAMA: Fungsi baru untuk mengembalikan catatan dari trash ---
    private fun restoreNote(note: Note) {
        val updatedNote = note.copy(isTrashed = false)
        NoteDao.update(dbHelper.writableDatabase, updatedNote)
        refreshCurrentView()
        Toast.makeText(this, "Catatan berhasil dipulihkan", Toast.LENGTH_SHORT).show()
    }
    // --- END PERUBAHAN UTAMA ---

    private fun resetToAllNotesView() {
        currentDisplayMode = NoteDisplayMode.ALL
        refreshCurrentView() // Ini akan menangani visibilitas dan judul
        resetAllQuickButtons()
        setQuickButtonState(binding.btnAllNotes, true, R.color.green_active)
        isViewingTrash = false
        isViewingFavorite = false
        isViewingHidden = false
        selectedCategoryId = null // Reset selected category
        selectedCategoryName = null
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
                refreshCurrentView()
                Toast.makeText(this, "Catatan dihapus permanen", Toast.LENGTH_SHORT).show()
            }
            .setNegativeButton("Batal", null)
            .create()

        dialog.show()
        dialog.getButton(AlertDialog.BUTTON_POSITIVE)?.setTextColor(getColor(R.color.red))
        dialog.getButton(AlertDialog.BUTTON_NEGATIVE)?.setTextColor(getColor(R.color.gray))
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

    private fun loadCategoriesForManage() {
        val db = dbHelper.readableDatabase
        // Ambil semua kategori, tapi saring "Uncategorized" (id = 0) karena tidak bisa diedit/dihapus
        val categories = CategoryDao.getAll(db).filter { it.id != 0 }
        // --- Pastikan baris ini terpanggil ---
        categoryManageAdapter.updateCategories(categories as MutableList<Category>) // Pastikan di-cast ke MutableList jika adapter membutuhkannya
    }

    private fun showAddEditCategoryDialog(categoryToEdit: Category? = null) {
        val dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_add_category, null)
        val dialogTitleTextView = dialogView.findViewById<TextView>(R.id.dialogTitleTextView)
        val textInputLayoutName = dialogView.findViewById<TextInputLayout>(R.id.textInputLayoutCategoryName)
        val editName = dialogView.findViewById<TextInputEditText>(R.id.editCategoryName)
        val colorPickerRecyclerView = dialogView.findViewById<RecyclerView>(R.id.colorPickerRecyclerView)
        val colorSelectionWarning = dialogView.findViewById<TextView>(R.id.colorSelectionWarning)

        selectedCategoryColor = null

        categoryToEdit?.let {
            dialogTitleTextView?.text = "Edit Kategori"
            editName.setText(it.name)
            selectedCategoryColor = it.colorHex
        } ?: run {
            dialogTitleTextView?.text = "Tambah Kategori Baru"
        }

        val predefinedColors = listOf(
            "#F44336", "#E91E63", "#9C27B0", "#673AB7", "#3F51B5", "#2196F3",
            "#03A9F4", "#00BCD4", "#009688", "#4CAF50", "#8BC34A", "#CDDC39",
            "#FFEB3B", "#FFC107", "#FF9800", "#FF5722", "#795548", "#9E9E9E", "#607D8B"
        )

        val colorAdapter = ColorPickerAdapter(predefinedColors) { colorHex ->
            selectedCategoryColor = colorHex
            colorSelectionWarning.visibility = View.GONE
        }

        colorPickerRecyclerView.layoutManager = LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false)
        colorPickerRecyclerView.adapter = colorAdapter

        if (selectedCategoryColor == null && predefinedColors.isNotEmpty()) {
            selectedCategoryColor = predefinedColors[0]
            colorAdapter.setSelectedColor(predefinedColors[0])
        } else if (selectedCategoryColor != null) {
            colorAdapter.setSelectedColor(selectedCategoryColor!!)
        }

        val alertDialog = AlertDialog.Builder(this)
            .setView(dialogView)
            .setPositiveButton("Simpan", null)
            .setNegativeButton("Batal") { dialog, _ ->
                dialog.dismiss()
            }
            .create()

        alertDialog.show()

        val positiveButton = alertDialog.getButton(AlertDialog.BUTTON_POSITIVE)
        positiveButton.setOnClickListener {
            val name = editName.text.toString().trim()
            val colorHex = selectedCategoryColor

            var isValid = true
            if (name.isBlank()) {
                textInputLayoutName.error = "Nama kategori tidak boleh kosong"
                isValid = false
            } else {
                textInputLayoutName.error = null
            }

            if (colorHex.isNullOrBlank()) {
                colorSelectionWarning.visibility = View.VISIBLE
                isValid = false
            } else {
                colorSelectionWarning.visibility = View.GONE
            }

            if (isValid) {
                val db = dbHelper.writableDatabase
                if (categoryToEdit == null) { // Mode Tambah Baru
                    val category = Category(name = name, colorHex = colorHex!!)
                    CategoryDao.insert(db, category)
                    Toast.makeText(this, "Kategori '$name' berhasil ditambahkan", Toast.LENGTH_SHORT).show()
                } else { // Mode Edit
                    val updatedCategory = categoryToEdit.copy(name = name, colorHex = colorHex!!)
                    CategoryDao.update(db, updatedCategory)
                    Toast.makeText(this, "Kategori '${name}' berhasil diperbarui", Toast.LENGTH_SHORT).show()
                }
                loadCategoriesForManage() // Muat ulang daftar kategori di RecyclerView manajemen
                alertDialog.dismiss()
            }
        }
        alertDialog.getButton(AlertDialog.BUTTON_POSITIVE)?.setTextColor(ContextCompat.getColor(this, R.color.green_active))
        alertDialog.getButton(AlertDialog.BUTTON_NEGATIVE)?.setTextColor(ContextCompat.getColor(this, R.color.gray))
    }

    private fun showDeleteCategoryConfirmationDialog(category: Category) {
        val db = dbHelper.writableDatabase
        val notesUsingCategory = NoteDao.getNotesCountByCategory(db, category.id)

        if (notesUsingCategory > 0) {
            val alertDialog = AlertDialog.Builder(this)
                .setTitle("Tidak Dapat Menghapus Kategori")
                .setMessage("Kategori '${category.name}' digunakan oleh $notesUsingCategory catatan. Harap pindahkan catatan ini ke kategori lain sebelum menghapus kategori ini.")
                .setPositiveButton("OK") { dialog, _ -> dialog.dismiss() }
                .show()

            alertDialog.getButton(android.app.AlertDialog.BUTTON_POSITIVE)?.setTextColor(ContextCompat.getColor(this, R.color.red_active))
            alertDialog.getButton(android.app.AlertDialog.BUTTON_NEGATIVE)?.setTextColor(ContextCompat.getColor(this, R.color.gray))
        } else {
            val alertDialog = AlertDialog.Builder(this)
                .setTitle("Hapus Kategori")
                .setMessage("Apakah Anda yakin ingin menghapus kategori '${category.name}'?")
                .setPositiveButton("Hapus") { _, _ ->
                    val rowsAffected = CategoryDao.delete(db, category.id)
                    if (rowsAffected > 0) {
                        Toast.makeText(this, "Kategori '${category.name}' berhasil dihapus.", Toast.LENGTH_SHORT).show()
                        NoteDao.updateCategoryToNull(db, category.id)
                        loadCategoriesForManage() // Muat ulang daftar kategori manajemen
                        refreshCurrentView() // Muat ulang catatan juga, jika ada yang affected
                    } else {
                        Toast.makeText(this, "Gagal menghapus kategori.", Toast.LENGTH_SHORT).show()
                    }
                }
                .setNegativeButton("Batal") { dialog, _ ->
                    dialog.dismiss()
                }
                .show()
            alertDialog.getButton(android.app.AlertDialog.BUTTON_POSITIVE)?.setTextColor(ContextCompat.getColor(this, R.color.red_active))
            alertDialog.getButton(android.app.AlertDialog.BUTTON_NEGATIVE)?.setTextColor(ContextCompat.getColor(this, R.color.gray))
        }
    }

    private fun showCategorySelectionDialog() {
        val options = arrayOf("View Categories (Filter Notes)", "Manage Categories (Add/Edit/Delete)")

        val alertDialog = AlertDialog.Builder(this)
            .setTitle("Choose Category Action")
            .setItems(options) { dialog, which ->
                when (which) {
                    0 -> { // View Categories (Filter Notes)
                        currentDisplayMode = NoteDisplayMode.CATEGORY
                        resetAllQuickButtons()
                        setQuickButtonState(binding.btnCategory, true, R.color.brown_active)
                        isViewingTrash = false
                        isViewingFavorite = false
                        isViewingHidden = false
                        selectedCategoryId = null // Reset agar menampilkan semua kategori untuk filter
                        refreshCurrentView() // Ini akan menampilkan recyclerCategory dengan categoryFilterAdapter
                    }
                    1 -> { // Manage Categories (Add/Edit/Delete)
                        currentDisplayMode = NoteDisplayMode.MANAGE_CATEGORIES
                        resetAllQuickButtons()
                        setQuickButtonState(binding.btnCategory, true, R.color.brown_active) // Tetap tandai btnCategory aktif
                        isViewingTrash = false
                        isViewingFavorite = false
                        isViewingHidden = false
                        selectedCategoryId = null // Reset selected category
                        refreshCurrentView() // Ini akan menampilkan recyclerCategory dengan categoryManageAdapter
                    }
                }
                dialog.dismiss()
            }
            .setNegativeButton("Cancel") { dialog, _ ->
                dialog.dismiss()
            }
            .show()

        alertDialog.getButton(android.app.AlertDialog.BUTTON_POSITIVE)?.setTextColor(ContextCompat.getColor(this, R.color.red_active))
        alertDialog.getButton(android.app.AlertDialog.BUTTON_NEGATIVE)?.setTextColor(ContextCompat.getColor(this, R.color.gray))
    }
}