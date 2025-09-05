package com.vitalsign.patientmanager

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.vitalsign.patientmanager.adapter.PatientListAdapter
import com.vitalsign.patientmanager.databinding.ActivityMainBinding
import com.vitalsign.patientmanager.model.Patient
import com.vitalsign.patientmanager.data.PatientManager
import java.util.*

class MainActivity : AppCompatActivity() {
    
    private lateinit var binding: ActivityMainBinding
    private lateinit var patientAdapter: PatientListAdapter
    private val patients = mutableListOf<Patient>()
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        
        // Initialize PatientManager with context for persistent storage
        PatientManager.initialize(this)
        
        setupRecyclerView()
        setupFab()
        
        // Initialize sample data if needed (only if no patients exist)
        PatientManager.initializeSampleData()
        
        // Load all patients
        loadPatients()
    }
    
    override fun onResume() {
        super.onResume()
        // Refresh patient list when returning to this activity
        loadPatients()
    }
    
    private fun loadPatients() {
        patients.clear()
        patients.addAll(PatientManager.getAllPatients())
        patientAdapter.notifyDataSetChanged()
    }
    
    private fun setupRecyclerView() {
        patientAdapter = PatientListAdapter(patients) { patient ->
            val intent = Intent(this, PatientDetailActivity::class.java)
            intent.putExtra("patient_id", patient.id)
            startActivity(intent)
        }
        
        binding.recyclerViewPatients.apply {
            layoutManager = LinearLayoutManager(this@MainActivity)
            adapter = patientAdapter
        }
    }
    
    private fun setupFab() {
        binding.fabAddPatient.setOnClickListener {
            val intent = Intent(this, AddPatientActivity::class.java)
            startActivityForResult(intent, ADD_PATIENT_REQUEST)
        }
    }
    
    private fun loadSampleData() {
        val calendar = Calendar.getInstance()
        calendar.set(2004, Calendar.JANUARY, 15)
        
        val samplePatient = Patient(
            name = "John Doe",
            dateOfBirth = calendar.time
        )
        
        patients.add(samplePatient)
        patientAdapter.notifyDataSetChanged()
    }
    
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == ADD_PATIENT_REQUEST && resultCode == RESULT_OK) {
            // Refresh the patient list
            patientAdapter.notifyDataSetChanged()
        }
    }
    
    companion object {
        const val ADD_PATIENT_REQUEST = 1
    }
}
