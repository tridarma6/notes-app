import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.cardview.widget.CardView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.notesapp.R
import com.example.notesapp.data.model.Note
import com.example.notesapp.databinding.ItemNoteBinding

enum class NoteDisplayMode {
    ALL, TRASH, FAVORITE, HIDDEN, CATEGORY
}
class NotesAdapter(
    private val onItemClick: (Note) -> Unit,
    private val onItemLongClick: (Note) -> Unit,
    private val onFavoriteToggle: (Note) -> Unit
) : ListAdapter<Note, NotesAdapter.NoteViewHolder>(DIFF_CALLBACK) {
    var displayMode: NoteDisplayMode = NoteDisplayMode.ALL
    inner class NoteViewHolder(private val binding: ItemNoteBinding) :
        RecyclerView.ViewHolder(binding.root) {

         // gunakan binding langsung

        fun bind(note: Note) {
            binding.noteTitle.text = note.title
            binding.noteContent.text = note.content

            binding.noteFavorite.visibility = if (note.isFavorite) View.VISIBLE else View.GONE

            // Ubah warna latar belakang card jika trashed
            val bgColor = when {
                note.isTrashed -> ContextCompat.getColor(binding.root.context, R.color.trash_background)
                note.isFavorite -> ContextCompat.getColor(binding.root.context, R.color.soft_yellow)
                note.isHidden -> ContextCompat.getColor(binding.root.context, R.color.soft_blue)
                displayMode == NoteDisplayMode.ALL -> ContextCompat.getColor(binding.root.context, R.color.soft_green)
                else -> ContextCompat.getColor(binding.root.context, R.color.normal_background)
            }
            (binding.root as CardView).setCardBackgroundColor(bgColor)


            // Icon toggle favorite
            binding.btnFavorite.setImageResource(
                if (note.isFavorite) R.drawable.ic_favorite_filled else R.drawable.ic_favorites
            )

            binding.btnFavorite.setOnClickListener {
                onFavoriteToggle(note)
            }

            binding.root.setOnClickListener {
                onItemClick(note)
            }

            binding.root.setOnLongClickListener {
                onItemLongClick(note)
                true
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): NoteViewHolder {
        val binding = ItemNoteBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return NoteViewHolder(binding)
    }

    override fun onBindViewHolder(holder: NoteViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    companion object {
        val DIFF_CALLBACK = object : DiffUtil.ItemCallback<Note>() {
            override fun areItemsTheSame(oldItem: Note, newItem: Note): Boolean {
                return oldItem.id == newItem.id
            }

            override fun areContentsTheSame(oldItem: Note, newItem: Note): Boolean {
                return oldItem == newItem
            }
        }
    }
}
