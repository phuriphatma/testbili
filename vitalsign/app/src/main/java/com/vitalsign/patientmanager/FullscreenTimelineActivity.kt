package com.vitalsign.patientmanager

import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.vitalsign.patientmanager.adapter.HierarchicalTimelineAdapter
import com.vitalsign.patientmanager.data.PatientManager
import com.vitalsign.patientmanager.databinding.ActivityFullscreenTimelineBinding
import com.vitalsign.patientmanager.model.LifeEvent
import com.vitalsign.patientmanager.model.Patient
import com.vitalsign.patientmanager.model.TimelineItem
import com.vitalsign.patientmanager.utils.TimelineManager
import com.vitalsign.patientmanager.view.FreeScrollContainer
import java.text.SimpleDateFormat
import java.util.*

class FullscreenTimelineActivity : AppCompatActivity() {
    
    private lateinit var binding: ActivityFullscreenTimelineBinding
    private var patient: Patient? = null
    private val timelineManager = TimelineManager()
    private lateinit var hierarchicalTimelineAdapter: HierarchicalTimelineAdapter
    private val dateFormat = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityFullscreenTimelineBinding.inflate(layoutInflater)
        setContentView(binding.root)
        
        // Get patient ID from intent
        val patientId = intent.getStringExtra("patient_id")
        if (patientId != null) {
            patient = PatientManager.getPatientById(patientId)
        }
        
        if (patient == null) {
            Toast.makeText(this, "Patient not found", Toast.LENGTH_SHORT).show()
            finish()
            return
        }
        
        setupViews()
        setupRecyclerView()
        setupClickListeners()
        refreshTimeline()
    }
    
    private fun setupViews() {
        patient?.let { p ->
            binding.textPatientName.text = "Timeline for ${p.name}"
            
            if (p.dateOfBirth != null) {
                val age = p.getCurrentAge()
                binding.textPatientInfo.text = if (age != null) "Age: $age" else "Age: Not calculated"
            } else {
                binding.textPatientInfo.text = "Age: Not specified (no birth date)"
            }
        }
    }
    
    private fun setupRecyclerView() {
        patient?.let { p ->
            hierarchicalTimelineAdapter = HierarchicalTimelineAdapter(
                onAddEventClick = { age, month, day ->
                    showEventDetailsDialog(age, month, day)
                },
                onExpandClick = { timelineItem ->
                    timelineManager.toggleExpansion(timelineItem)
                    refreshTimeline()
                }
            )
            
            binding.recyclerViewTimeline.apply {
                layoutManager = LinearLayoutManager(this@FullscreenTimelineActivity)
                adapter = hierarchicalTimelineAdapter
            }
            
            refreshTimeline()
        }
    }
    
    private fun setupClickListeners() {
        binding.buttonBack.setOnClickListener {
            finish()
        }
        
        binding.fabAddEvent.setOnClickListener {
            showAddEventDialog()
        }
    }
    
    private fun refreshTimeline() {
        patient?.let { p ->
            val birthYear = p.dateOfBirth?.let { 
                val calendar = Calendar.getInstance()
                calendar.time = it
                calendar.get(Calendar.YEAR)
            }
            
            val timelineItems = timelineManager.generateTimelineWithPatientEvents(p.events, birthYear)
            hierarchicalTimelineAdapter.updateTimelineItems(timelineItems)
            updateHeaderVisibility(timelineItems)
        }
    }
    
    private fun updateHeaderVisibility(timelineItems: List<TimelineItem>) {
        val showMonthColumn = timelineItems.any { it is TimelineItem.MonthItem || it is TimelineItem.DayItem }
        val showDayColumn = timelineItems.any { it is TimelineItem.DayItem }
        
        binding.headerMonth.visibility = if (showMonthColumn) android.view.View.VISIBLE else android.view.View.GONE
        binding.headerDay.visibility = if (showDayColumn) android.view.View.VISIBLE else android.view.View.GONE
    }
    
    private fun showAddEventDialog() {
        val builder = AlertDialog.Builder(this)
        builder.setTitle("Add Event")
        builder.setMessage("Please tap on a specific age in the timeline to add an event.")
        builder.setPositiveButton("OK", null)
        builder.show()
    }
    
    private fun showEventDetailsDialog(age: Int, month: Int? = null, day: Int? = null) {
        val builder = AlertDialog.Builder(this)
        builder.setTitle("Add Event for Age $age")
        
        // Create custom layout for the dialog
        val dialogView = layoutInflater.inflate(R.layout.dialog_add_event, null)
        builder.setView(dialogView)
        
        val inputDescription = dialogView.findViewById<android.widget.EditText>(R.id.editEventDescription)
        val inputMonth = dialogView.findViewById<android.widget.EditText>(R.id.editEventMonth)
        val inputDay = dialogView.findViewById<android.widget.EditText>(R.id.editEventDay)
        val textEventDate = dialogView.findViewById<android.widget.TextView>(R.id.textEventDate)
        
        // Set hints and constraints
        inputDescription.hint = "Enter event description"
        inputMonth.hint = "Month (1-12)"
        inputDay.hint = "Day (1-31)"
        
        // Pre-fill if month/day were provided
        month?.let { inputMonth.setText(it.toString()) }
        day?.let { inputDay.setText(it.toString()) }
        
        // Update date display when month/day changes
        val updateDateDisplay = {
            val monthVal = inputMonth.text.toString().toIntOrNull()
            val dayVal = inputDay.text.toString().toIntOrNull()
            
            if (monthVal != null && dayVal != null && monthVal in 1..12 && dayVal in 1..31) {
                val calendar = Calendar.getInstance()
                val currentAge = patient?.getCurrentAge() ?: 0
                val year = Calendar.getInstance().get(Calendar.YEAR) - currentAge + age
                
                try {
                    calendar.set(year, monthVal - 1, dayVal) // month is 0-based in Calendar
                    val dateFormat = SimpleDateFormat("MMM dd, yyyy (EEEE)", Locale.getDefault())
                    textEventDate.text = "Date: ${dateFormat.format(calendar.time)}"
                    textEventDate.visibility = android.view.View.VISIBLE
                } catch (e: Exception) {
                    textEventDate.text = "Invalid date"
                    textEventDate.visibility = android.view.View.VISIBLE
                }
            } else {
                textEventDate.visibility = android.view.View.GONE
            }
        }
        
        // Add text watchers for real-time date validation
        inputMonth.addTextChangedListener(object : android.text.TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: android.text.Editable?) { updateDateDisplay() }
        })
        
        inputDay.addTextChangedListener(object : android.text.TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: android.text.Editable?) { updateDateDisplay() }
        })
        
        builder.setPositiveButton("Add") { _, _ ->
            val description = inputDescription.text.toString().trim()
            val monthStr = inputMonth.text.toString().trim()
            val dayStr = inputDay.text.toString().trim()
            
            if (description.isNotEmpty()) {
                patient?.let { p ->
                    val currentAge = p.getCurrentAge()
                    if (currentAge != null && p.dateOfBirth != null) {
                        val year = Calendar.getInstance().get(Calendar.YEAR) - currentAge + age
                        
                        // Parse month and day
                        val monthVal = if (monthStr.isNotEmpty()) monthStr.toIntOrNull() else null
                        val dayVal = if (dayStr.isNotEmpty()) dayStr.toIntOrNull() else null
                        
                        // Validate month and day
                        val validMonth = monthVal?.let { if (it in 1..12) it else null }
                        val validDay = dayVal?.let { if (it in 1..31) it else null }
                        
                        // Remove existing empty event and add new one
                        p.events.removeAll { it.age == age && it.description.isEmpty() }
                        p.events.add(
                            LifeEvent(
                                age = age,
                                year = year,
                                month = validMonth,
                                day = validDay,
                                description = description
                            )
                        )
                        
                        p.events.sortBy { it.age }
                        
                        // Update patient in manager
                        PatientManager.updatePatient(p)
                        
                        // Refresh timeline
                        refreshTimeline()
                        
                        val dateInfo = if (validMonth != null && validDay != null) {
                            " on $validMonth/$validDay"
                        } else {
                            ""
                        }
                        Toast.makeText(this, "Event added for age $age$dateInfo", Toast.LENGTH_SHORT).show()
                    }
                }
            } else {
                Toast.makeText(this, "Please enter event description", Toast.LENGTH_SHORT).show()
            }
        }
        
        builder.setNegativeButton("Cancel", null)
        builder.show()
    }
}
