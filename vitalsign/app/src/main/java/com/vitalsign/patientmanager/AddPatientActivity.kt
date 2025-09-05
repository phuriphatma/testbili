package com.vitalsign.patientmanager

import android.app.DatePickerDialog
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.vitalsign.patientmanager.databinding.ActivityAddPatientBinding
import com.vitalsign.patientmanager.model.Patient
import com.vitalsign.patientmanager.data.PatientManager
import java.text.SimpleDateFormat
import java.util.*

class AddPatientActivity : AppCompatActivity() {
    
    private lateinit var binding: ActivityAddPatientBinding
    private var selectedDateOfBirth: Date? = null
    private val dateFormat = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAddPatientBinding.inflate(layoutInflater)
        setContentView(binding.root)
        
        // Initialize PatientManager with context
        PatientManager.initialize(this)
        
        setupViews()
        setupClickListeners()
    }
    
    private fun setupViews() {
        // Show current date and time when creating patient
        val currentDateTime = SimpleDateFormat("MMM dd, yyyy 'at' HH:mm", Locale.getDefault()).format(Date())
        binding.textCurrentDateTime.text = "Creating patient on: $currentDateTime"
        
        // Make date of birth optional by showing placeholder
        binding.editDateOfBirth.hint = "Date of Birth (Optional)"
    }
    
    private fun setupClickListeners() {
        binding.editDateOfBirth.setOnClickListener {
            showDatePicker()
        }
        
        binding.buttonSave.setOnClickListener {
            savePatient()
        }
        
        binding.buttonCancel.setOnClickListener {
            finish()
        }
        
        binding.buttonClearDOB.setOnClickListener {
            clearDateOfBirth()
        }
    }
    
    private fun showDatePicker() {
        val calendar = Calendar.getInstance()
        
        val datePickerDialog = DatePickerDialog(
            this,
            { _, year, month, dayOfMonth ->
                calendar.set(year, month, dayOfMonth)
                selectedDateOfBirth = calendar.time
                binding.editDateOfBirth.setText(dateFormat.format(selectedDateOfBirth!!))
            },
            calendar.get(Calendar.YEAR) - 20, // Default to 20 years ago
            calendar.get(Calendar.MONTH),
            calendar.get(Calendar.DAY_OF_MONTH)
        )
        
        datePickerDialog.show()
    }
    
    private fun clearDateOfBirth() {
        selectedDateOfBirth = null
        binding.editDateOfBirth.setText("")
    }
    
    private fun savePatient() {
        val name = binding.editPatientName.text.toString().trim()
        
        if (name.isEmpty()) {
            binding.editPatientName.error = "Please enter patient name"
            return
        }
        
        // Create new patient with optional date of birth
        val newPatient = Patient(
            name = name,
            dateOfBirth = selectedDateOfBirth,
            createdAt = Date() // Current timestamp
        )
        
        // Save patient using PatientManager
        PatientManager.addPatient(newPatient)
        
        Toast.makeText(this, "Patient '$name' added successfully!", Toast.LENGTH_SHORT).show()
        
        setResult(RESULT_OK)
        finish()
    }
}
