package com.vitalsign.patientmanager.data

import android.content.Context
import com.vitalsign.patientmanager.model.Patient

object PatientManager {
    private val patients = mutableListOf<Patient>()
    private var storage: PatientStorage? = null
    private var isInitialized = false
    
    fun initialize(context: Context) {
        if (!isInitialized) {
            storage = PatientStorage(context)
            loadPatientsFromStorage()
            isInitialized = true
        }
    }
    
    private fun loadPatientsFromStorage() {
        storage?.let { storage ->
            patients.clear()
            patients.addAll(storage.loadPatients())
        }
    }
    
    private fun savePatientsToStorage() {
        storage?.savePatients(patients)
    }
    
    fun addPatient(patient: Patient) {
        patients.add(patient)
        savePatientsToStorage()
    }
    
    fun getAllPatients(): List<Patient> {
        return patients.toList()
    }
    
    fun getPatientById(id: String): Patient? {
        return patients.find { it.id == id }
    }
    
    fun updatePatient(updatedPatient: Patient) {
        val index = patients.indexOfFirst { it.id == updatedPatient.id }
        if (index != -1) {
            patients[index] = updatedPatient
            savePatientsToStorage()
        }
    }
    
    fun removePatient(id: String) {
        patients.removeAll { it.id == id }
        savePatientsToStorage()
    }
    
    // Initialize with sample data only if no patients exist
    fun initializeSampleData() {
        if (patients.isEmpty()) {
            val samplePatient = Patient(
                name = "John Doe",
                dateOfBirth = java.util.Calendar.getInstance().apply {
                    set(2004, java.util.Calendar.JANUARY, 15)
                }.time
            )
            addPatient(samplePatient)
        }
    }
    
    fun clearAllData() {
        patients.clear()
        storage?.clearAllData()
    }
}
