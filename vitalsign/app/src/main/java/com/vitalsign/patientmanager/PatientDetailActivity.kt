package com.vitalsign.patientmanager

import android.app.DatePickerDialog
import android.content.Intent
import android.os.Bundle
import android.text.TextWatcher
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.vitalsign.patientmanager.adapter.HierarchicalTimelineAdapter
import com.vitalsign.patientmanager.databinding.ActivityPatientDetailBinding
import com.vitalsign.patientmanager.model.LifeEvent
import com.vitalsign.patientmanager.model.Patient
import com.vitalsign.patientmanager.model.TimelineItem
import com.vitalsign.patientmanager.data.PatientManager
import com.vitalsign.patientmanager.utils.TimelineManager
import java.text.SimpleDateFormat
import java.util.*

class PatientDetailActivity : AppCompatActivity() {
    
    private lateinit var binding: ActivityPatientDetailBinding
    private var patient: Patient? = null
    private var expandedYears: MutableSet<Int> = mutableSetOf()
    private var expandedMonths: MutableSet<String> = mutableSetOf() // "year-month" format
    private var showMonthColumn = false
    private var showDayColumn = false
    private val timelineManager = TimelineManager()
    private lateinit var hierarchicalTimelineAdapter: HierarchicalTimelineAdapter
    private val dateFormat = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())
    private val dayFormat = SimpleDateFormat("EEEE", Locale.getDefault())
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityPatientDetailBinding.inflate(layoutInflater)
        setContentView(binding.root)
        
        // Initialize PatientManager with context
        PatientManager.initialize(this)
        
        // Get patient ID from intent
        val patientId = intent.getStringExtra("patient_id")
        if (patientId != null) {
            patient = PatientManager.getPatientById(patientId)
        }
        
        if (patient == null) {
            // Fallback: create sample patient if no ID provided
            createSamplePatient()
        }
        
        setupViews()
        setupRecyclerView()
        setupClickListeners()
        updateCurrentDateInfo()
        generateLifeTimeline()
    }
    
    private fun createSamplePatient() {
        val calendar = Calendar.getInstance()
        calendar.set(2004, Calendar.JANUARY, 15)
        
        patient = Patient(
            name = "John Doe",
            dateOfBirth = calendar.time
        )
        
        // Add some sample events
        patient!!.events.add(LifeEvent(age = 0, year = 2004, month = 1, day = 15, description = "natal: jaundice"))
        patient!!.events.add(LifeEvent(age = 1, year = 2005, month = 6, day = 10, description = "accident"))
    }
    
    private fun setupViews() {
        patient?.let { p ->
            binding.textPatientName.text = p.name
            
            if (p.dateOfBirth != null) {
                binding.editDateOfBirth.setText(dateFormat.format(p.dateOfBirth))
                val age = p.getCurrentAge()
                binding.textAge.text = if (age != null) "Age: $age" else "Age: Not calculated"
            } else {
                binding.editDateOfBirth.setText("Not specified")
                binding.textAge.text = "Age: Not specified (no birth date)"
            }
            
            // Show when patient was created
            binding.textPatientCreated.text = "Patient created: ${p.getFormattedCreatedAt()}"
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
            
            binding.recyclerViewEvents.apply {
                layoutManager = LinearLayoutManager(this@PatientDetailActivity)
                adapter = hierarchicalTimelineAdapter
            }
            
            refreshTimeline()
        }
    }
    
    private fun refreshTimeline() {
        patient?.let { p ->
            val birthYear = p.dateOfBirth?.let { 
                val calendar = Calendar.getInstance()
                calendar.time = it
                calendar.get(Calendar.YEAR)
            }
            
            // Debug logging
            android.util.Log.d("PatientDetailActivity", "=== RefreshTimeline Debug ===")
            android.util.Log.d("PatientDetailActivity", "Patient: ${p.name}")
            android.util.Log.d("PatientDetailActivity", "Birth year: $birthYear")
            android.util.Log.d("PatientDetailActivity", "Total events: ${p.events.size}")
            
            p.events.forEachIndexed { index, event ->
                android.util.Log.d("PatientDetailActivity", "Event $index: age=${event.age}, year=${event.year}, desc='${event.description}'")
            }
            
            val timelineItems = timelineManager.generateTimelineWithPatientEvents(p.events, birthYear)
            android.util.Log.d("PatientDetailActivity", "Generated ${timelineItems.size} timeline items")
            
            hierarchicalTimelineAdapter.updateTimelineItems(timelineItems)
            updateHeaderVisibility(timelineItems)
            
            android.util.Log.d("PatientDetailActivity", "=== RefreshTimeline Complete ===")
        }
    }
    
    private fun updateHeaderVisibility(timelineItems: List<TimelineItem>) {
        val showMonthColumn = timelineItems.any { it is TimelineItem.MonthItem || it is TimelineItem.DayItem }
        val showDayColumn = timelineItems.any { it is TimelineItem.DayItem }
        
        binding.headerMonth.visibility = if (showMonthColumn) android.view.View.VISIBLE else android.view.View.GONE
        binding.headerDay.visibility = if (showDayColumn) android.view.View.VISIBLE else android.view.View.GONE
    }
    
    private fun setupClickListeners() {
        binding.editDateOfBirth.setOnClickListener {
            showDatePicker()
        }
        
        binding.fabAddEvent.setOnClickListener {
            showAddEventDialog()
        }
        
        binding.buttonShowMonthlyView.setOnClickListener {
            showMonthlyView()
        }
        
        binding.buttonFullscreenTimeline.setOnClickListener {
            android.util.Log.d("PatientDetailActivity", "Full-screen timeline button clicked")
            showFullscreenTimeline()
        }
    }
    
    private fun updateCurrentDateInfo() {
        val currentDate = Date()
        binding.textCurrentDate.text = "Today: ${dateFormat.format(currentDate)}"
        binding.textCurrentDay.text = "Day: ${dayFormat.format(currentDate)}"
    }
    
    private fun showDatePicker() {
        patient?.let { p ->
            val calendar = Calendar.getInstance()
            if (p.dateOfBirth != null) {
                calendar.time = p.dateOfBirth
            }
            
            val datePickerDialog = DatePickerDialog(
                this,
                { _, year, month, dayOfMonth ->
                    calendar.set(year, month, dayOfMonth)
                    val newDateOfBirth = calendar.time
                    
                    // Update patient with new date of birth
                    val updatedPatient = p.copy(dateOfBirth = newDateOfBirth)
                    PatientManager.updatePatient(updatedPatient)
                    patient = updatedPatient
                    
                    binding.editDateOfBirth.setText(dateFormat.format(newDateOfBirth))
                    val age = updatedPatient.getCurrentAge()
                    binding.textAge.text = if (age != null) "Age: $age" else "Age: Not calculated"
                    generateLifeTimeline()
                },
                calendar.get(Calendar.YEAR),
                calendar.get(Calendar.MONTH),
                calendar.get(Calendar.DAY_OF_MONTH)
            )
            
            datePickerDialog.show()
        }
    }
    
    private fun generateLifeTimeline() {
        patient?.let { p ->
            p.events.clear()
            
            val age = p.getCurrentAge()
            if (age != null && p.dateOfBirth != null) {
                val birthCalendar = Calendar.getInstance()
                birthCalendar.time = p.dateOfBirth
                val birthYear = birthCalendar.get(Calendar.YEAR)
                
                for (ageYear in 0..age) {
                    val year = birthYear + ageYear
                    
                    // Check if there's an existing event for this age
                    val existingEvent = p.events.find { it.age == ageYear }
                    if (existingEvent == null) {
                        p.events.add(
                            LifeEvent(
                                age = ageYear,
                                year = year,
                                month = null,
                                day = null,
                                description = ""
                            )
                        )
                    }
                }
            } else {
                // If no birth date, show message
                Toast.makeText(this, "Please set date of birth to generate timeline", Toast.LENGTH_SHORT).show()
            }
            
            refreshTimeline()
        }
    }
    
    private fun showAddEventDialog() {
        patient?.let { p ->
            val age = p.getCurrentAge()
            if (age == null) {
                Toast.makeText(this, "Please set date of birth first", Toast.LENGTH_SHORT).show()
                return
            }
            
            val ages = (0..age).map { it.toString() }.toTypedArray()
            
            AlertDialog.Builder(this)
                .setTitle("Select Age")
                .setItems(ages) { _, which ->
                    val selectedAge = which
                    showEventDetailsDialog(selectedAge)
                }
                .show()
        }
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
        
        // Pre-fill month and day if provided
        month?.let { inputMonth.setText(it.toString()) }
        day?.let { inputDay.setText(it.toString()) }
        
        // Set hints and constraints
        inputDescription.hint = "Enter event description"
        inputMonth.hint = "Month (1-12)"
        inputDay.hint = "Day (1-31)"
        
        // Update date display when month/day changes
        val updateDateDisplay = {
            val month = inputMonth.text.toString().toIntOrNull()
            val day = inputDay.text.toString().toIntOrNull()
            
            if (month != null && day != null && month in 1..12 && day in 1..31) {
                val calendar = Calendar.getInstance()
                val currentAge = patient?.getCurrentAge() ?: 0
                val year = Calendar.getInstance().get(Calendar.YEAR) - currentAge + age
                
                try {
                    calendar.set(year, month - 1, day) // month is 0-based in Calendar
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
                        val month = if (monthStr.isNotEmpty()) monthStr.toIntOrNull() else null
                        val day = if (dayStr.isNotEmpty()) dayStr.toIntOrNull() else null
                        
                        // Validate month and day
                        val validMonth = month?.let { if (it in 1..12) it else null }
                        val validDay = day?.let { if (it in 1..31) it else null }
                        
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
                        refreshTimeline()
                        
                        // Update patient in manager
                        PatientManager.updatePatient(p)
                        
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
    
    private fun showMonthlyView() {
        Toast.makeText(this, "Monthly view feature coming soon!", Toast.LENGTH_SHORT).show()
    }
    
    private fun showFullscreenTimeline() {
        patient?.let { p ->
            Toast.makeText(this, "Opening full-screen timeline...", Toast.LENGTH_SHORT).show()
            val intent = Intent(this, FullscreenTimelineActivity::class.java)
            intent.putExtra("patient_id", p.id)
            startActivity(intent)
        } ?: run {
            Toast.makeText(this, "No patient data available", Toast.LENGTH_SHORT).show()
        }
    }
}
