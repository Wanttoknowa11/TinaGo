package com.gtemedia.tinago

import android.content.Intent
import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import java.util.Date
import java.util.Locale
import com.gtemedia.tinago.Vehicle


class CitizenVehiclesAdapter(private val vehicles: MutableList<Vehicle>) :
    RecyclerView.Adapter<CitizenVehiclesAdapter.CitizenVehicleViewHolder>() {

    class CitizenVehicleViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val textViewCitizenLicensePlate: TextView = itemView.findViewById(R.id.textViewCitizenLicensePlate)
        val textViewCitizenMakeModel: TextView = itemView.findViewById(R.id.textViewCitizenMakeModel)
        val textViewCitizenStatus: TextView = itemView.findViewById(R.id.textViewCitizenStatus)
        val buttonViewDetails: Button = itemView.findViewById(R.id.buttonViewDetails)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CitizenVehicleViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_citizen_vehicle, parent, false)
        return CitizenVehicleViewHolder(view)
    }

    override fun onBindViewHolder(holder: CitizenVehicleViewHolder, position: Int) {
        val vehicle = vehicles[position]
        holder.textViewCitizenLicensePlate.text = "License Plate: ${vehicle.licensePlate}"
        holder.textViewCitizenMakeModel.text = "Make: ${vehicle.make}, Model: ${vehicle.model}"
        holder.textViewCitizenStatus.text = "Status: ${vehicle.currentStatus.replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString() }}"

        // Set text color based on status
        when (vehicle.currentStatus) {
            "stolen" -> holder.textViewCitizenStatus.setTextColor(Color.RED)
            "recovered" -> holder.textViewCitizenStatus.setTextColor(Color.parseColor("#006400")) // Dark Green
            else -> holder.textViewCitizenStatus.setTextColor(Color.BLACK) // Default for "registered"
        }

        holder.buttonViewDetails.setOnClickListener {
            val context = holder.itemView.context
            val intent = Intent(context, VehicleDetailsActivity::class.java).apply {
                putExtra("vehicleId", vehicle.id)
                putExtra("licensePlate", vehicle.licensePlate) // Pass license plate for display
            }
            context.startActivity(intent)
        }
    }

    override fun getItemCount(): Int = vehicles.size

    /**
     * Updates the list of vehicles in the adapter and notifies the RecyclerView of the change.
     * @param newVehicles The new list of vehicles.
     */
    fun updateData(newVehicles: MutableList<Vehicle>) {
        vehicles.clear()
        vehicles.addAll(newVehicles)
        notifyDataSetChanged()
    }

}
