// app/src/main/java/com/example/notesapp/ui/adapter/EventAdapter.kt
package com.example.notesapp.ui.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.notesapp.R
import com.example.notesapp.data.model.Event

class EventAdapter(
    private var events: List<Event>,
    private val onEventClick: (Event) -> Unit,
    private val onEventLongClick: (Event) -> Unit
) : RecyclerView.Adapter<EventAdapter.EventViewHolder>() {

    class EventViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val titleTextView: TextView = itemView.findViewById(R.id.eventTitleTextView)
        val descriptionTextView: TextView = itemView.findViewById(R.id.eventDescriptionTextView)
        val timeTextView: TextView = itemView.findViewById(R.id.eventTimeTextView)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): EventViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_event, parent, false)
        return EventViewHolder(view)
    }

    override fun onBindViewHolder(holder: EventViewHolder, position: Int) {
        val event = events[position]
        holder.titleTextView.text = event.title
        holder.descriptionTextView.text = event.description
        holder.timeTextView.text = event.time ?: "Sepanjang Hari" // Tampilkan "Sepanjang Hari" jika waktu null

        holder.itemView.setOnClickListener { onEventClick(event) }
        holder.itemView.setOnLongClickListener {
            onEventLongClick(event)
            true
        }
    }

    override fun getItemCount(): Int = events.size

    fun updateEvents(newEvents: List<Event>) {
        events = newEvents
        notifyDataSetChanged()
    }
}