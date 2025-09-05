package com.vitalsign.patientmanager.model

import java.util.*

data class Patient(
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val dateOfBirth: Date? = null, // Made optional
    val createdAt: Date = Date(), // Add creation timestamp
    val events: MutableList<LifeEvent> = mutableListOf()
) {
    fun getCurrentAge(): Int? {
        if (dateOfBirth == null) return null
        
        val calendar = Calendar.getInstance()
        val birthCalendar = Calendar.getInstance()
        birthCalendar.time = dateOfBirth
        
        var age = calendar.get(Calendar.YEAR) - birthCalendar.get(Calendar.YEAR)
        
        if (calendar.get(Calendar.DAY_OF_YEAR) < birthCalendar.get(Calendar.DAY_OF_YEAR)) {
            age--
        }
        
        return age
    }
    
    fun getBuddhistYear(gregorianYear: Int): Int {
        return gregorianYear + 543
    }
    
    fun getFormattedCreatedAt(): String {
        val format = java.text.SimpleDateFormat("MMM dd, yyyy 'at' HH:mm", Locale.getDefault())
        return format.format(createdAt)
    }
}

data class LifeEvent(
    val id: String = UUID.randomUUID().toString(),
    val age: Int,
    val year: Int,
    val month: Int?,
    val day: Int?,
    val description: String
) {
    fun getBuddhistYear(): Int = year + 543
}
