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
import com.example.notesapp.data.database.NotesDatabaseHelper
import com.example.notesapp.data.model.Category
import com.example.notesapp.data.model.Note
import com.example.notesapp.databinding.ActivityMainBinding
import android.content.Context
import android.text.Editable
import android.text.TextWatcher
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

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
    private var searchQuery: String = ""
    private var selectedCategoryId: Long? = null
    private var selectedCategoryName: String? = null
    private var searchJob: Job? = null

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
                    // Pastikan ORIGINAL_DISPLAY_MODE dan SELECTED_CATEGORY_ID diteruskan di sini
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
                // Refresh tampilan saat ini setelah toggle favorite
                refreshCurrentView()
            }
        )

        categoryList = CategoryDao.getAll(dbHelper.readableDatabase)
        categoryAdapter = CategoriesAdapter(categoryList) { selectedCategory ->
            Toast.makeText(this, "Kategori: ${selectedCategory.name}", Toast.LENGTH_SHORT).show()
            // Modifikasi: Sekarang kita simpan kategori yang dipilih dan panggil refreshCurrentView
            this.selectedCategoryId = selectedCategory.id.toLong()
            this.selectedCategoryName = selectedCategory.name
            currentDisplayMode = NoteDisplayMode.CATEGORY
            refreshCurrentView() // Panggil refreshCurrentView untuk memuat catatan kategori dan menerapkan pencarian
            resetAllQuickButtons()
            setQuickButtonState(binding.btnCategory, true, R.color.brown_active)
            isViewingTrash = false
            isViewingFavorite = false
            isViewingHidden = false
        }
        binding.recyclerCategory.adapter = categoryAdapter
        binding.recyclerCategory.layoutManager = LinearLayoutManager(this, LinearLayoutManager.VERTICAL, false)
        binding.recyclerCategory.visibility = View.GONE


        binding.recyclerRecentNotes.layoutManager = GridLayoutManager(this, 2)
        binding.recyclerRecentNotes.adapter = adapter
        binding.recyclerRecentNotes.visibility = View.VISIBLE


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
                if (position == RecyclerView.NO_POSITION || position >= adapter.currentList.size) {
                    return makeMovementFlags(0, 0)
                }
                val note = adapter.currentList[position]

                var swipeFlags = 0

                if (!note.isHidden && !note.isTrashed) {
                    swipeFlags = swipeFlags or ItemTouchHelper.LEFT
                }

                if (isViewingHidden && note.isHidden) {
                    swipeFlags = swipeFlags or ItemTouchHelper.RIGHT
                }

                return makeMovementFlags(0, swipeFlags)
            }

            override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {
                val position = viewHolder.adapterPosition
                if (position == RecyclerView.NO_POSITION || position >= adapter.currentList.size) {
                    adapter.notifyItemChanged(position)
                    return
                }
                val note = adapter.currentList[position]

                if (direction == ItemTouchHelper.LEFT) {
                    hideNote(note)
                } else if (direction == ItemTouchHelper.RIGHT) {
                    unhideNote(note)
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
                val position = viewHolder.adapterPosition
                val note =
                    if (position != RecyclerView.NO_POSITION && position < adapter.currentList.size) {
                        adapter.currentList[position]
                    } else {
                        super.onChildDraw(c, recyclerView, viewHolder, dX, dY, actionState, isCurrentlyActive)
                        return
                    }

                if (dX < 0) { // Swipe ke kiri
                    if (!note.isHidden && !note.isTrashed) {
                        val paint = Paint().apply { color = ContextCompat.getColor(context, R.color.blue_active) }
                        c.drawRect(itemView.right + dX, itemView.top.toFloat(), itemView.right.toFloat(), itemView.bottom.toFloat(), paint)

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
                            color = Color.WHITE; textSize = 40f; isAntiAlias = true; typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                        }
                        c.drawText("Hide note?", itemView.right - 250f, itemView.top + itemView.height / 2f + 15f, textPaint)
                    }
                }
                if (dX > 0 && isViewingHidden && note.isHidden) {
                    val paint = Paint().apply { color = ContextCompat.getColor(context, R.color.soft_green) }
                    c.drawRect(itemView.left.toFloat(), itemView.top.toFloat(), itemView.left + dX, itemView.bottom.toFloat(), paint)

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
                        color = Color.WHITE; textSize = 40f; isAntiAlias = true; typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                    }
                    c.drawText("Unhide note?", itemView.left + 100f, itemView.top + itemView.height / 2f + 15f, textPaint)
                }
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

        // --- Listener untuk EditText searchBar ---
        binding.searchBar.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                searchJob?.cancel() // Batalkan job pencarian sebelumnya
                searchJob = lifecycleScope.launch {
                    delay(300) // Tunggu 300ms setelah user berhenti mengetik
                    searchQuery = s.toString().trim()
                    refreshCurrentView() // Panggil refresh
                }
            }
            override fun afterTextChanged(s: Editable?) {}
        })

        // Untuk menangani saat tombol Enter/Search di keyboard ditekan
        binding.searchBar.setOnEditorActionListener { v, actionId, event ->
            if (actionId == EditorInfo.IME_ACTION_SEARCH || actionId == EditorInfo.IME_ACTION_DONE) {
                searchJob?.cancel() // Batalkan debounce jika Enter ditekan
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
        // --- AKHIR BARU: Listener untuk EditText searchBar ---

        // --- Listener untuk Quick Filter Buttons (PASTIKAN INI ADALAH SATU-SATUNYA SET LISTENER) ---
        binding.btnTrash.setOnClickListener {
            currentDisplayMode = NoteDisplayMode.TRASH
            refreshCurrentView()
            resetAllQuickButtons()
            setQuickButtonState(binding.btnTrash, true, R.color.red_active)
            isViewingTrash = true
            isViewingFavorite = false
            isViewingHidden = false
            binding.recyclerCategory.visibility = View.GONE
            binding.recyclerRecentNotes.visibility = View.VISIBLE
        }

        binding.btnCategory.setOnClickListener {
            currentDisplayMode = NoteDisplayMode.CATEGORY
            selectedCategoryId = null
            selectedCategoryName = null
            refreshCurrentView()
            resetAllQuickButtons()
            setQuickButtonState(binding.btnCategory, true, R.color.brown_active)
            isViewingTrash = false
            isViewingFavorite = false
            isViewingHidden = false
        }

        binding.btnFavorite.setOnClickListener {
            currentDisplayMode = NoteDisplayMode.FAVORITE
            refreshCurrentView()
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
            binding.recyclerCategory.visibility = View.GONE
            binding.recyclerRecentNotes.visibility = View.VISIBLE
        }

        // --- Listener untuk Bottom Navigation (PASTIKAN INI ADALAH SATU-SATUNYA SET LISTENER) ---
        binding.bottomNavigation.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.menu_notes -> {
                    currentDisplayMode = NoteDisplayMode.ALL
                    refreshCurrentView()
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
        // --- AKHIR Listener untuk Bottom Navigation ---

        // Panggil ini di akhir onCreate untuk menampilkan catatan awal
        currentDisplayMode = NoteDisplayMode.ALL // Pastikan mode awal ALL
        refreshCurrentView() // Panggil refresh untuk memuat data awal dan menerapkan pencarian
    }


    override fun onResume() {
        super.onResume()
        // Reset query saat kembali ke MainActivity untuk menghindari filter yang tidak diinginkan
        // jika pengguna tidak melihat search bar sebelumnya
        searchQuery = binding.searchBar.text.toString().trim() // Ambil teks jika ada
        refreshCurrentView()
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
        val db = dbHelper.readableDatabase
        var baseNotes: List<Note> = emptyList()
        var currentTitle: String = ""

        when (currentDisplayMode) {
            NoteDisplayMode.ALL -> {
                baseNotes = NoteDao.getAll(db).filter { !it.isTrashed && !it.isHidden }
                currentTitle = getString(R.string.recent_notes)
                binding.recyclerRecentNotes.visibility = View.VISIBLE
                binding.recyclerCategory.visibility = View.GONE
                selectedCategoryId = null
            }
            NoteDisplayMode.TRASH -> {
                baseNotes = NoteDao.getAll(db).filter { it.isTrashed }
                currentTitle = "Trashed Notes"
                binding.recyclerRecentNotes.visibility = View.VISIBLE
                binding.recyclerCategory.visibility = View.GONE
                selectedCategoryId = null
            }
            NoteDisplayMode.FAVORITE -> {
                baseNotes = NoteDao.getAll(db).filter { it.isFavorite && !it.isTrashed && !it.isHidden }
                currentTitle = "Favorite Notes"
                binding.recyclerRecentNotes.visibility = View.VISIBLE
                binding.recyclerCategory.visibility = View.GONE
                selectedCategoryId = null
            }
            NoteDisplayMode.HIDDEN -> {
                baseNotes = NoteDao.getAll(db).filter { it.isHidden }
                currentTitle = "Hidden Notes"
                binding.recyclerRecentNotes.visibility = View.VISIBLE
                binding.recyclerCategory.visibility = View.GONE
                selectedCategoryId = null
            }
            NoteDisplayMode.CATEGORY -> {
                if (selectedCategoryId != null) {
                    baseNotes = NoteDao.getAll(db).filter {
                        it.categoryId?.toLong() == selectedCategoryId && !it.isTrashed && !it.isHidden
                    }
                    currentTitle = "Notes in ${selectedCategoryName ?: "Category"}"
                    binding.recyclerRecentNotes.visibility = View.VISIBLE
                    binding.recyclerCategory.visibility = View.GONE
                } else {
                    binding.recentNotesTitle.text = getString(R.string.categories)
                    binding.recyclerRecentNotes.visibility = View.GONE
                    binding.recyclerCategory.visibility = View.VISIBLE
                    adapter.submitList(emptyList())
                    return
                }
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

        adapter.submitList(finalNotes)
        adapter.displayMode = currentDisplayMode
        binding.recentNotesTitle.text = currentTitle
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
