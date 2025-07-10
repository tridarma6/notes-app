// app/src/main/java/com/example/notesapp/adapter/CategoryManageAdapter.kt
package com.example.notesapp.adapter

import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.notesapp.R
import com.example.notesapp.data.model.Category

class CategoryManageAdapter(
    private val categories: MutableList<Category>, // Pakai MutableList agar bisa diupdate
    private val onEditClick: (Category) -> Unit,
    private val onDeleteClick: (Category) -> Unit
) : RecyclerView.Adapter<CategoryManageAdapter.CategoryViewHolder>() {

    inner class CategoryViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val categoryColorCircle: View = itemView.findViewById(R.id.categoryColorCircle)
        val categoryNameText: TextView = itemView.findViewById(R.id.categoryName)
        val btnEditCategory: ImageView = itemView.findViewById(R.id.btnEditCategory)
        val btnDeleteCategory: ImageView = itemView.findViewById(R.id.btnDeleteCategory)

        fun bind(category: Category) {
            btnEditCategory.setOnClickListener {
                onEditClick(category) // Panggil lambda onEditClick yang diteruskan dari MainActivity
            }

            btnDeleteCategory.setOnClickListener {
                onDeleteClick(category)
            }

            if (category.name == "Uncategorized" || category.id == 0) {
                btnDeleteCategory.visibility = View.GONE
                btnEditCategory.visibility = View.GONE
            } else {
                btnDeleteCategory.visibility = View.VISIBLE
                btnEditCategory.visibility = View.VISIBLE
            }
        }

    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CategoryViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_category, parent, false)
        return CategoryViewHolder(view)
    }

    override fun onBindViewHolder(holder: CategoryViewHolder, position: Int) {
        val category = categories[position]

        val drawable = holder.categoryColorCircle.background as? GradientDrawable
        if (drawable != null) {
            try {
                drawable.setColor(Color.parseColor(category.colorHex))
            } catch (e: IllegalArgumentException) {
                drawable.setColor(Color.GRAY)
            }
        }

        holder.categoryNameText.text = category.name

        if (category.id == 0) { // ID 0 untuk "Uncategorized"
            holder.btnEditCategory.visibility = View.GONE
            holder.btnDeleteCategory.visibility = View.GONE
        } else {
            holder.btnEditCategory.visibility = View.VISIBLE
            holder.btnDeleteCategory.visibility = View.VISIBLE

            holder.btnEditCategory.setOnClickListener {
                onEditClick(category)
            }

            holder.btnDeleteCategory.setOnClickListener {
                onDeleteClick(category)
            }
        }
    }

    override fun getItemCount(): Int = categories.size

    fun updateCategories(newCategories: List<Category>) {
        categories.clear()
        categories.addAll(newCategories)
        notifyDataSetChanged()
    }
}