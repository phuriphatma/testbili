package com.vitalsign.patientmanager.data

import android.content.Context
import android.content.SharedPreferences
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.vitalsign.patientmanager.model.Patient

class PatientStorage(context: Context) {
    
    private val sharedPreferences: SharedPreferences = 
        context.getSharedPreferences("patient_data", Context.MODE_PRIVATE)
    private val gson = Gson()
    
    fun savePatients(patients: List<Patient>) {
        val json = gson.toJson(patients)
        sharedPreferences.edit()
            .putString(KEY_PATIENTS, json)
            .apply()
    }
    
    fun loadPatients(): List<Patient> {
        val json = sharedPreferences.getString(KEY_PATIENTS, null)
        return if (json != null) {
            val type = object : TypeToken<List<Patient>>() {}.type
            gson.fromJson(json, type) ?: emptyList()
        } else {
            emptyList()
        }
    }
    
    fun clearAllData() {
        sharedPreferences.edit().clear().apply()
    }
    
    companion object {
        private const val KEY_PATIENTS = "patients"
    }
}
