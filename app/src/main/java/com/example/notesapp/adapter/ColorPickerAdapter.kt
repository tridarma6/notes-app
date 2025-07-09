// app/src/main/java/com/example/notesapp/ui/adapter/ColorPickerAdapter.kt
package com.example.notesapp.ui.adapter

import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.recyclerview.widget.RecyclerView
import com.example.notesapp.R

class ColorPickerAdapter(
    private val colors: List<String>, // Daftar kode HEX warna (misal: "#FF0000")
    private val onColorSelected: (String) -> Unit
) : RecyclerView.Adapter<ColorPickerAdapter.ColorViewHolder>() {

    private var selectedPosition = RecyclerView.NO_POSITION // Melacak posisi warna yang dipilih

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ColorViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_color_circle, parent, false)
        return ColorViewHolder(view)
    }

    override fun onBindViewHolder(holder: ColorViewHolder, position: Int) {
        val colorHex = colors[position]

        // Atur warna lingkaran
        val drawable = holder.colorCircleView.background as? GradientDrawable
        drawable?.setColor(Color.parseColor(colorHex))

        // Tampilkan/sembunyikan indikator centang
        holder.selectionIndicator.visibility = if (position == selectedPosition) View.VISIBLE else View.GONE

        holder.itemView.setOnClickListener {
            // Update posisi yang dipilih dan beritahu adapter untuk refresh
            val oldSelectedPosition = selectedPosition
            selectedPosition = holder.adapterPosition
            notifyItemChanged(oldSelectedPosition) // Refresh item sebelumnya yang dipilih
            notifyItemChanged(selectedPosition)   // Refresh item yang baru dipilih

            onColorSelected(colorHex) // Beri tahu listener warna yang dipilih
        }
    }

    override fun getItemCount(): Int = colors.size

    fun setSelectedColor(colorHex: String) {
        val newPosition = colors.indexOf(colorHex)
        if (newPosition != -1 && newPosition != selectedPosition) {
            val oldSelectedPosition = selectedPosition
            selectedPosition = newPosition
            notifyItemChanged(oldSelectedPosition)
            notifyItemChanged(selectedPosition)
        }
    }

    class ColorViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val colorCircleView: View = itemView.findViewById(R.id.colorCircleView)
        val selectionIndicator: ImageView = itemView.findViewById(R.id.selectionIndicator)
    }
}