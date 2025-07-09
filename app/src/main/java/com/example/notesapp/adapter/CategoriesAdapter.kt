import android.graphics.Color
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.cardview.widget.CardView
import androidx.recyclerview.widget.RecyclerView
import com.example.notesapp.R
import com.example.notesapp.data.model.Category

class CategoriesAdapter(
    private var categories: List<Category>,
    private val onItemClick: (Category) -> Unit
) : RecyclerView.Adapter<CategoriesAdapter.CategoryViewHolder>() {

    inner class CategoryViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val categoryName: TextView = itemView.findViewById(R.id.categoryName)
        private val cardView: CardView = itemView.findViewById(R.id.cardCategory)

        fun bind(category: Category) {
            categoryName.text = category.name

            // Set background color dari field colorHex
            try {
                cardView.setCardBackgroundColor(Color.parseColor(category.colorHex))
            } catch (e: IllegalArgumentException) {
                // fallback warna default jika format hex salah
                cardView.setCardBackgroundColor(Color.LTGRAY)
            }

            itemView.setOnClickListener {
                onItemClick(category)
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CategoryViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_category, parent, false) // <-- PASTIKAN INI item_category
        return CategoryViewHolder(view)
    }


    fun updateCategories(newCategories: List<Category>) {
        this.categories = newCategories
        Log.d("CategoriesAdapter", "Adapter diperbarui dengan ${newCategories.size} kategori.")
        newCategories.forEach { category ->
            Log.d("CategoriesAdapter", "Kategori adapter: ${category.name}")
        }
        notifyDataSetChanged()
    }

    override fun onBindViewHolder(holder: CategoryViewHolder, position: Int) {
        holder.bind(categories[position])
    }

    override fun getItemCount(): Int = categories.size
}
