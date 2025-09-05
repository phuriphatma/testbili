package com.vitalsign.patientmanager.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.vitalsign.patientmanager.databinding.ItemPatientBinding
import com.vitalsign.patientmanager.model.Patient
import java.text.SimpleDateFormat
import java.util.*

class PatientListAdapter(
    private val patients: List<Patient>,
    private val onPatientClick: (Patient) -> Unit
) : RecyclerView.Adapter<PatientListAdapter.PatientViewHolder>() {
    
    private val dateFormat = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())
    
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PatientViewHolder {
        val binding = ItemPatientBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return PatientViewHolder(binding)
    }
    
    override fun onBindViewHolder(holder: PatientViewHolder, position: Int) {
        holder.bind(patients[position])
    }
    
    override fun getItemCount(): Int = patients.size
    
    inner class PatientViewHolder(
        private val binding: ItemPatientBinding
    ) : RecyclerView.ViewHolder(binding.root) {
        
        fun bind(patient: Patient) {
            binding.textPatientName.text = patient.name
            
            // Handle optional age
            val age = patient.getCurrentAge()
            if (age != null) {
                binding.textPatientAge.text = "Age: $age"
            } else {
                binding.textPatientAge.text = "Age: Not specified"
            }
            
            // Handle optional date of birth
            if (patient.dateOfBirth != null) {
                binding.textDateOfBirth.text = "DOB: ${dateFormat.format(patient.dateOfBirth)}"
            } else {
                binding.textDateOfBirth.text = "DOB: Not specified"
            }
            
            // Show creation date
            binding.textCreatedAt.text = "Created: ${patient.getFormattedCreatedAt()}"
            
            binding.root.setOnClickListener {
                onPatientClick(patient)
            }
        }
    }
}
