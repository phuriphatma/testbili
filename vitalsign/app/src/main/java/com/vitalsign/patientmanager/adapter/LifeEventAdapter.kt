package com.vitalsign.patientmanager.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.vitalsign.patientmanager.databinding.ItemLifeEventBinding
import com.vitalsign.patientmanager.model.LifeEvent

class LifeEventAdapter(
    private val events: List<LifeEvent>,
    private val onAddEventClick: ((Int) -> Unit)? = null
) : RecyclerView.Adapter<LifeEventAdapter.LifeEventViewHolder>() {
    
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): LifeEventViewHolder {
        val binding = ItemLifeEventBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return LifeEventViewHolder(binding)
    }
    
    override fun onBindViewHolder(holder: LifeEventViewHolder, position: Int) {
        holder.bind(events[position])
    }
    
    override fun getItemCount(): Int = events.size
    
    inner class LifeEventViewHolder(
        private val binding: ItemLifeEventBinding
    ) : RecyclerView.ViewHolder(binding.root) {
        
        fun bind(event: LifeEvent) {
            binding.textAge.text = event.age.toString()
            binding.textYear.text = event.year.toString()
            binding.textBuddhistYear.text = event.getBuddhistYear().toString()
            
            if (event.description.isNotEmpty()) {
                binding.textEvent.text = event.description
                binding.textEvent.alpha = 1.0f
                binding.root.isClickable = false
                binding.root.setOnClickListener(null)
                binding.root.background = null
            } else {
                binding.textEvent.text = "No events recorded - Tap to add"
                binding.textEvent.alpha = 0.7f
                
                // Make clickable for adding events
                binding.root.isClickable = true
                binding.root.setOnClickListener {
                    onAddEventClick?.invoke(event.age)
                }
                
                // Add visual feedback for clickable state
                binding.root.background = android.graphics.drawable.RippleDrawable(
                    android.content.res.ColorStateList.valueOf(
                        android.graphics.Color.parseColor("#E3F2FD")
                    ),
                    null,
                    null
                )
            }
            
            // Show month and day with better formatting
            val dateInfo = StringBuilder()
            if (event.month != null && event.day != null) {
                // Create a calendar to get the month name and day of week
                val calendar = java.util.Calendar.getInstance()
                calendar.set(event.year, event.month - 1, event.day) // month is 0-based
                
                val monthNames = arrayOf(
                    "Jan", "Feb", "Mar", "Apr", "May", "Jun",
                    "Jul", "Aug", "Sep", "Oct", "Nov", "Dec"
                )
                val dayNames = arrayOf("Sun", "Mon", "Tue", "Wed", "Thu", "Fri", "Sat")
                
                val monthName = monthNames[calendar.get(java.util.Calendar.MONTH)]
                val dayName = dayNames[calendar.get(java.util.Calendar.DAY_OF_WEEK) - 1]
                
                dateInfo.append("$monthName ${event.day}, $dayName")
            } else if (event.month != null) {
                val monthNames = arrayOf(
                    "Jan", "Feb", "Mar", "Apr", "May", "Jun",
                    "Jul", "Aug", "Sep", "Oct", "Nov", "Dec"
                )
                val monthName = monthNames[event.month - 1]
                dateInfo.append("$monthName ${event.year}")
            } else if (event.day != null) {
                dateInfo.append("Day ${event.day}")
            }
            
            binding.textDate.text = dateInfo.toString()
            binding.textDate.visibility = if (dateInfo.isNotEmpty()) android.view.View.VISIBLE else android.view.View.GONE
        }
    }
}
