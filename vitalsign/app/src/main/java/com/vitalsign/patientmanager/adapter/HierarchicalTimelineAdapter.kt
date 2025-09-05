package com.vitalsign.patientmanager.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.vitalsign.patientmanager.databinding.ItemTimelineYearBinding
import com.vitalsign.patientmanager.databinding.ItemTimelineMonthBinding
import com.vitalsign.patientmanager.databinding.ItemTimelineDayBinding
import com.vitalsign.patientmanager.model.TimelineItem
import com.vitalsign.patientmanager.model.LifeEvent

class HierarchicalTimelineAdapter(
    private var timelineItems: MutableList<TimelineItem> = mutableListOf(),
    private val onAddEventClick: ((Int, Int?, Int?) -> Unit)? = null,
    private val onExpandClick: ((TimelineItem) -> Unit)? = null
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {
    
    private var showMonthColumn = false
    private var showDayColumn = false
    
    companion object {
        private const val VIEW_TYPE_YEAR = 0
        private const val VIEW_TYPE_MONTH = 1
        private const val VIEW_TYPE_DAY = 2
    }
    
    override fun getItemViewType(position: Int): Int {
        return when (timelineItems[position]) {
            is TimelineItem.YearItem -> VIEW_TYPE_YEAR
            is TimelineItem.MonthItem -> VIEW_TYPE_MONTH
            is TimelineItem.DayItem -> VIEW_TYPE_DAY
        }
    }
    
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return when (viewType) {
            VIEW_TYPE_YEAR -> {
                val binding = ItemTimelineYearBinding.inflate(
                    LayoutInflater.from(parent.context), parent, false
                )
                YearViewHolder(binding)
            }
            VIEW_TYPE_MONTH -> {
                val binding = ItemTimelineMonthBinding.inflate(
                    LayoutInflater.from(parent.context), parent, false
                )
                MonthViewHolder(binding)
            }
            VIEW_TYPE_DAY -> {
                val binding = ItemTimelineDayBinding.inflate(
                    LayoutInflater.from(parent.context), parent, false
                )
                DayViewHolder(binding)
            }
            else -> throw IllegalArgumentException("Unknown view type: $viewType")
        }
    }
    
    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val item = timelineItems[position]
        when (holder) {
            is YearViewHolder -> holder.bind(item as TimelineItem.YearItem)
            is MonthViewHolder -> holder.bind(item as TimelineItem.MonthItem)
            is DayViewHolder -> holder.bind(item as TimelineItem.DayItem)
        }
    }
    
    override fun getItemCount(): Int = timelineItems.size
    
    fun updateTimelineItems(newItems: List<TimelineItem>) {
        timelineItems.clear()
        timelineItems.addAll(newItems)
        
        // Update column visibility based on timeline items
        showMonthColumn = timelineItems.any { it is TimelineItem.MonthItem || it is TimelineItem.DayItem }
        showDayColumn = timelineItems.any { it is TimelineItem.DayItem }
        
        notifyDataSetChanged()
    }
    
    fun updateColumnVisibility(showMonth: Boolean, showDay: Boolean) {
        showMonthColumn = showMonth
        showDayColumn = showDay
        notifyDataSetChanged()
    }
    
    inner class YearViewHolder(
        private val binding: ItemTimelineYearBinding
    ) : RecyclerView.ViewHolder(binding.root) {
        
        fun bind(item: TimelineItem.YearItem) {
            binding.textAge.text = item.age.toString()
            binding.textYear.text = item.year.toString()
            binding.textBuddhistYear.text = item.getBuddhistYear().toString()
            
            // Show/hide month and day columns
            binding.textMonth.visibility = if (showMonthColumn) android.view.View.VISIBLE else android.view.View.GONE
            binding.textDay.visibility = if (showDayColumn) android.view.View.VISIBLE else android.view.View.GONE
            
            // Clear month and day text for year rows
            binding.textMonth.text = ""
            binding.textDay.text = ""
            
            // Handle expand/collapse button
            binding.buttonExpandMonths.apply {
                visibility = android.view.View.VISIBLE // Always show for adding events
                text = if (item.isExpanded) "−" else "+"
                setOnClickListener {
                    onExpandClick?.invoke(item)
                }
            }
            
            // Handle event display and clicking
            if (item.event?.description?.isNotEmpty() == true) {
                binding.textEvent.text = item.event.description
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
                    onAddEventClick?.invoke(item.age, null, null)
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
            
            // Show date info if month/day exists in the event
            val event = item.event
            val dateInfo = StringBuilder()
            if (event?.month != null && event.day != null) {
                val monthNames = arrayOf(
                    "Jan", "Feb", "Mar", "Apr", "May", "Jun",
                    "Jul", "Aug", "Sep", "Oct", "Nov", "Dec"
                )
                val monthName = monthNames[event.month - 1]
                dateInfo.append("$monthName ${event.day}")
            } else if (event?.month != null) {
                val monthNames = arrayOf(
                    "Jan", "Feb", "Mar", "Apr", "May", "Jun",
                    "Jul", "Aug", "Sep", "Oct", "Nov", "Dec"
                )
                val monthName = monthNames[event.month - 1]
                dateInfo.append("$monthName ${item.year}")
            }
            
            binding.textDate.text = dateInfo.toString()
            binding.textDate.visibility = if (dateInfo.isNotEmpty()) android.view.View.VISIBLE else android.view.View.GONE
        }
    }
    
    inner class MonthViewHolder(
        private val binding: ItemTimelineMonthBinding
    ) : RecyclerView.ViewHolder(binding.root) {
        
        fun bind(item: TimelineItem.MonthItem) {
            binding.textMonth.text = item.getShortMonthName()
            
            // Show/hide day column
            binding.textDay.visibility = if (showDayColumn) android.view.View.VISIBLE else android.view.View.GONE
            
            // Clear day text for month rows
            binding.textDay.text = ""
            
            // Handle expand/collapse button for days
            binding.buttonExpandDays.apply {
                visibility = android.view.View.VISIBLE // Always show for adding events
                text = if (item.isExpanded) "−" else "+"
                setOnClickListener {
                    onExpandClick?.invoke(item)
                }
            }
            
            // Handle event display and clicking
            if (item.event?.description?.isNotEmpty() == true) {
                binding.textEvent.text = item.event.description
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
                    onAddEventClick?.invoke(item.age, item.month, null)
                }
                
                // Add visual feedback for clickable state
                binding.root.background = android.graphics.drawable.RippleDrawable(
                    android.content.res.ColorStateList.valueOf(
                        android.graphics.Color.parseColor("#E8F5E8")
                    ),
                    null,
                    null
                )
            }
        }
    }
    
    inner class DayViewHolder(
        private val binding: ItemTimelineDayBinding
    ) : RecyclerView.ViewHolder(binding.root) {
        
        fun bind(item: TimelineItem.DayItem) {
            binding.textDay.text = item.day.toString()
            
            // Handle event display and clicking
            if (item.event?.description?.isNotEmpty() == true) {
                binding.textEvent.text = item.event.description
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
                    onAddEventClick?.invoke(item.age, item.month, item.day)
                }
                
                // Add visual feedback for clickable state
                binding.root.background = android.graphics.drawable.RippleDrawable(
                    android.content.res.ColorStateList.valueOf(
                        android.graphics.Color.parseColor("#FFF3E0")
                    ),
                    null,
                    null
                )
            }
        }
    }
}
