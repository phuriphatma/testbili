package com.vitalsign.patientmanager.model

/**
 * Represents different types of items in the hierarchical timeline
 */
sealed class TimelineItem {
    abstract val level: Int // 0 = year, 1 = month, 2 = day
    abstract val id: String
    
    data class YearItem(
        val age: Int,
        val year: Int,
        val event: LifeEvent?,
        val isExpanded: Boolean = false,
        val hasMonths: Boolean = false
    ) : TimelineItem() {
        override val level: Int = 0
        override val id: String = "year_$age"
        
        fun getBuddhistYear(): Int = year + 543
    }
    
    data class MonthItem(
        val age: Int,
        val year: Int,
        val month: Int,
        val event: LifeEvent?,
        val isExpanded: Boolean = false,
        val hasDays: Boolean = false
    ) : TimelineItem() {
        override val level: Int = 1
        override val id: String = "month_${age}_$month"
        
        fun getMonthName(): String {
            val monthNames = arrayOf(
                "January", "February", "March", "April", "May", "June",
                "July", "August", "September", "October", "November", "December"
            )
            return monthNames[month - 1]
        }
        
        fun getShortMonthName(): String {
            val monthNames = arrayOf(
                "Jan", "Feb", "Mar", "Apr", "May", "Jun",
                "Jul", "Aug", "Sep", "Oct", "Nov", "Dec"
            )
            return monthNames[month - 1]
        }
    }
    
    data class DayItem(
        val age: Int,
        val year: Int,
        val month: Int,
        val day: Int,
        val event: LifeEvent?
    ) : TimelineItem() {
        override val level: Int = 2
        override val id: String = "day_${age}_${month}_$day"
        
        fun getDayOfWeek(): String {
            val calendar = java.util.Calendar.getInstance()
            calendar.set(year, month - 1, day) // month is 0-based in Calendar
            val dayNames = arrayOf("Sun", "Mon", "Tue", "Wed", "Thu", "Fri", "Sat")
            return dayNames[calendar.get(java.util.Calendar.DAY_OF_WEEK) - 1]
        }
        
        fun getFormattedDate(): String {
            val monthItem = MonthItem(age, year, month, null)
            return "${monthItem.getShortMonthName()} $day, ${getDayOfWeek()}"
        }
    }
}
