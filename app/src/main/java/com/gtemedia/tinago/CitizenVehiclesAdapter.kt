package com.gtemedia.tinago

import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

// Define a simple data class to hold citizen vehicle information for the adapter
data class CitizenVehicleItem(
    val licensePlate: String,
    val make: String,
    val model: String,
    val status: String
)

class CitizenVehiclesAdapter(private val citizenVehicles: List<CitizenVehicleItem>) :
    RecyclerView.Adapter<CitizenVehiclesAdapter.CitizenVehicleViewHolder>() {

    // Define a listener for item clicks (optional, for future details view)
    var onItemClick: ((CitizenVehicleItem) -> Unit)? = null

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CitizenVehicleViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_citizen_vehicle, parent, false)
        return CitizenVehicleViewHolder(view)
    }

    override fun onBindViewHolder(holder: CitizenVehicleViewHolder, position: Int) {
        val vehicle = citizenVehicles[position]
        holder.bind(vehicle)
    }

    override fun getItemCount(): Int = citizenVehicles.size

    inner class CitizenVehicleViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val textViewCitizenPlate: TextView = itemView.findViewById(R.id.textViewCitizenPlate)
        private val textViewCitizenMakeModel: TextView = itemView.findViewById(R.id.textViewCitizenMakeModel)
        private val textViewCitizenStatus: TextView = itemView.findViewById(R.id.textViewCitizenStatus)

        init {
            // Set up click listener for the entire item view
            itemView.setOnClickListener {
                onItemClick?.invoke(citizenVehicles[adapterPosition])
            }
        }

        fun bind(vehicle: CitizenVehicleItem) {
            textViewCitizenPlate.text = "License Plate: ${vehicle.licensePlate}"
            textViewCitizenMakeModel.text = "Make: ${vehicle.make}, Model: ${vehicle.model}"
            textViewCitizenStatus.text = "Status: ${vehicle.status.replace("_", " ").capitalize()}" // Format status

            // Set status text color
            when (vehicle.status) {
                "reported_stolen" -> textViewCitizenStatus.setTextColor(Color.RED)
                "recovered" -> textViewCitizenStatus.setTextColor(Color.parseColor("#006400")) // Dark Green
                else -> textViewCitizenStatus.setTextColor(Color.BLACK) // Default for "registered"
            }
        }
    }
}