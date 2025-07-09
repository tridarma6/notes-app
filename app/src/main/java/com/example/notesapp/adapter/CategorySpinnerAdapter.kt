package com.example.notesapp.adapter

import android.content.Context
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import com.example.notesapp.R
import com.example.notesapp.data.model.Category
import android.widget.TextView

class CategorySpinnerAdapter(
    private val ctx: Context,
    private val categories: List<Category>
) : ArrayAdapter<Category>(ctx, 0, categories) {

    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
        return createStyledView(position, convertView, parent)
    }

    override fun getDropDownView(position: Int, convertView: View?, parent: ViewGroup): View {
        return createStyledView(position, convertView, parent)
    }

    private fun createStyledView(position: Int, convertView: View?, parent: ViewGroup): View {
        val inflater = LayoutInflater.from(ctx)
        val view = convertView ?: inflater.inflate(R.layout.item_spinner_category, parent, false)
        val textView = view.findViewById<TextView>(R.id.textCategory)
        val category = categories[position]

        textView.text = category.name

        // Buat shape background dengan radius dan warna
        val drawable = GradientDrawable().apply {
            shape = GradientDrawable.RECTANGLE
            cornerRadius = 24f
            try {
                setColor(Color.parseColor(category.colorHex))
            } catch (e: Exception) {
                setColor(Color.GRAY) // fallback jika colorHex invalid
            }
        }

        textView.background = drawable

        return view
    }
}


