package com.vitalsign.patientmanager.utils

import com.vitalsign.patientmanager.model.LifeEvent
import com.vitalsign.patientmanager.model.TimelineItem
import java.util.*

class TimelineManager {
    
    private val expandedYears = mutableSetOf<Int>()
    private val expandedMonths = mutableSetOf<String>() // Format: "age_month"
    
    fun generateTimelineItems(events: List<LifeEvent>, birthYear: Int, maxAge: Int): List<TimelineItem> {
        val timelineItems = mutableListOf<TimelineItem>()
        
        // Group events by age, month, and day
        val eventsByAge = events.groupBy { it.age }
        val eventsByAgeMonth = events.filter { it.month != null }
            .groupBy { "${it.age}_${it.month}" }
        val eventsByAgeMonthDay = events.filter { it.month != null && it.day != null }
            .groupBy { "${it.age}_${it.month}_${it.day}" }
        
        for (age in 0..maxAge) {
            val ageEvents = eventsByAge[age] ?: emptyList()
            val yearEvent = ageEvents.find { it.month == null && it.day == null }
            
            // Check if this age has events with months
            val hasMonthEvents = ageEvents.any { it.month != null }
            
            // Add year item
            val yearItem = TimelineItem.YearItem(
                age = age,
                year = birthYear + age,
                event = yearEvent,
                isExpanded = expandedYears.contains(age),
                hasMonths = hasMonthEvents
            )
            timelineItems.add(yearItem)
            
            // Add month items if expanded
            if (yearItem.isExpanded) {
                if (hasMonthEvents) {
                    // Show months that have events
                    val monthsWithEvents = ageEvents.filter { it.month != null }
                        .groupBy { it.month!! }
                        .keys.sorted()
                    
                    for (month in monthsWithEvents) {
                        val monthEvents = ageEvents.filter { it.month == month }
                        val monthEvent = monthEvents.find { it.day == null }
                        val monthKey = "${age}_$month"
                        
                        // Check if this month has events with days
                        val hasDayEvents = monthEvents.any { it.day != null }
                        
                        val monthItem = TimelineItem.MonthItem(
                            age = age,
                            year = birthYear + age,
                            month = month,
                            event = monthEvent,
                            isExpanded = expandedMonths.contains(monthKey),
                            hasDays = hasDayEvents
                        )
                        timelineItems.add(monthItem)
                        
                        // Add day items if expanded
                        if (monthItem.isExpanded) {
                            if (hasDayEvents) {
                                // Show days that have events
                                val daysWithEvents = monthEvents.filter { it.day != null }
                                    .groupBy { it.day!! }
                                    .keys.sorted()
                                
                                for (day in daysWithEvents) {
                                    val dayEvent = monthEvents.find { it.day == day }
                                    
                                    val dayItem = TimelineItem.DayItem(
                                        age = age,
                                        year = birthYear + age,
                                        month = month,
                                        day = day,
                                        event = dayEvent
                                    )
                                    timelineItems.add(dayItem)
                                }
                            } else {
                                // Show all days of the month for adding new events
                                val cal = Calendar.getInstance()
                                cal.set(birthYear + age, month - 1, 1)
                                val daysInMonth = cal.getActualMaximum(Calendar.DAY_OF_MONTH)
                                
                                for (day in 1..daysInMonth) {
                                    val dayItem = TimelineItem.DayItem(
                                        age = age,
                                        year = birthYear + age,
                                        month = month,
                                        day = day,
                                        event = null
                                    )
                                    timelineItems.add(dayItem)
                                }
                            }
                        }
                    }
                } else {
                    // Show all months for adding new events
                    for (month in 1..12) {
                        val monthItem = TimelineItem.MonthItem(
                            age = age,
                            year = birthYear + age,
                            month = month,
                            event = null,
                            isExpanded = false,
                            hasDays = false
                        )
                        timelineItems.add(monthItem)
                    }
                }
            }
        }
        
        return timelineItems
    }
    
    fun toggleExpansion(item: TimelineItem): Boolean {
        return when (item) {
            is TimelineItem.YearItem -> {
                if (expandedYears.contains(item.age)) {
                    expandedYears.remove(item.age)
                    // Also collapse all months for this year
                    val monthKeysToRemove = expandedMonths.filter { 
                        it.startsWith("${item.age}_") 
                    }
                    expandedMonths.removeAll(monthKeysToRemove.toSet())
                    false
                } else {
                    expandedYears.add(item.age)
                    true
                }
            }
            is TimelineItem.MonthItem -> {
                val monthKey = "${item.age}_${item.month}"
                if (expandedMonths.contains(monthKey)) {
                    expandedMonths.remove(monthKey)
                    false
                } else {
                    expandedMonths.add(monthKey)
                    true
                }
            }
            is TimelineItem.DayItem -> false // Days don't expand
        }
    }
    
    fun isExpanded(item: TimelineItem): Boolean {
        return when (item) {
            is TimelineItem.YearItem -> expandedYears.contains(item.age)
            is TimelineItem.MonthItem -> expandedMonths.contains("${item.age}_${item.month}")
            is TimelineItem.DayItem -> false
        }
    }
    
    fun generateTimelineWithPatientEvents(events: List<LifeEvent>, patientBirthYear: Int?): List<TimelineItem> {
        android.util.Log.d("TimelineManager", "=== generateTimelineWithPatientEvents Debug ===")
        android.util.Log.d("TimelineManager", "Patient birth year: $patientBirthYear")
        android.util.Log.d("TimelineManager", "Events count: ${events.size}")
        
        if (patientBirthYear == null) {
            android.util.Log.d("TimelineManager", "No birth year provided, returning empty list")
            return emptyList()
        }
        
        val currentYear = Calendar.getInstance().get(Calendar.YEAR)
        val maxAge = currentYear - patientBirthYear
        
        android.util.Log.d("TimelineManager", "Current year: $currentYear")
        android.util.Log.d("TimelineManager", "Max age calculated: $maxAge")
        
        // Group events by age to determine which ages have events
        val eventsByAge = events.groupBy { it.age }
        
        android.util.Log.d("TimelineManager", "Events grouped by age:")
        eventsByAge.forEach { (age, ageEvents) ->
            android.util.Log.d("TimelineManager", "  Age $age: ${ageEvents.size} events")
        }
        
        val ageEvents = generateTimelineItems(0, maxAge, eventsByAge)
        
        android.util.Log.d("TimelineManager", "Generated ${ageEvents.size} timeline items")
        android.util.Log.d("TimelineManager", "=== generateTimelineWithPatientEvents Complete ===")
        
        return ageEvents
    }
    
    private fun generateTimelineItems(minAge: Int, maxAge: Int, eventsByAge: Map<Int, List<LifeEvent>>): List<TimelineItem> {
        android.util.Log.d("TimelineManager", "=== generateTimelineItems (private) Debug ===")
        android.util.Log.d("TimelineManager", "Age range: $minAge to $maxAge")
        android.util.Log.d("TimelineManager", "Events by age keys: ${eventsByAge.keys.sorted()}")
        
        val timelineItems = mutableListOf<TimelineItem>()
        val currentYear = Calendar.getInstance().get(Calendar.YEAR)
        
        for (age in minAge..maxAge) {
            val eventsAtAge = eventsByAge[age] ?: emptyList()
            android.util.Log.d("TimelineManager", "Processing age $age with ${eventsAtAge.size} events")
            
            val year = currentYear - maxAge + age // Calculate actual year for this age
            
            // If there are events at this age, create items for each event
            if (eventsAtAge.isNotEmpty()) {
                eventsAtAge.forEach { event ->
                    val yearItem = TimelineItem.YearItem(
                        age = age,
                        year = year,
                        event = event,
                        isExpanded = isExpanded(TimelineItem.YearItem(age, year, null)),
                        hasMonths = event.month != null
                    )
                    timelineItems.add(yearItem)
                    
                    // Add month item if event has month
                    if (event.month != null && yearItem.isExpanded) {
                        val monthItem = TimelineItem.MonthItem(
                            age = age,
                            year = year,
                            month = event.month,
                            event = event,
                            isExpanded = isExpanded(TimelineItem.MonthItem(age, year, event.month, null)),
                            hasDays = event.day != null
                        )
                        timelineItems.add(monthItem)
                        
                        // Add day item if event has day
                        if (event.day != null && monthItem.isExpanded) {
                            val dayItem = TimelineItem.DayItem(
                                age = age,
                                year = year,
                                month = event.month,
                                day = event.day,
                                event = event
                            )
                            timelineItems.add(dayItem)
                        }
                    }
                }
            } else {
                // Create a year item with no event for ages without events
                val yearItem = TimelineItem.YearItem(
                    age = age,
                    year = year,
                    event = null,
                    isExpanded = false,
                    hasMonths = false
                )
                timelineItems.add(yearItem)
            }
        }
        
        android.util.Log.d("TimelineManager", "Generated ${timelineItems.size} timeline items for ages $minAge-$maxAge")
        android.util.Log.d("TimelineManager", "=== generateTimelineItems (private) Complete ===")
        
        return timelineItems
    }
}
