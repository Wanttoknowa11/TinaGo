package com.gtemedia.tinago

import android.content.Intent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import java.text.SimpleDateFormat
import java.util.Locale

class StolenVehiclesAdapter(private val vehicles: MutableList<Vehicle>) :
    RecyclerView.Adapter<StolenVehiclesAdapter.StolenVehicleViewHolder>() {

    class StolenVehicleViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val textViewLicensePlate: TextView = itemView.findViewById(R.id.textViewLicensePlate)
        val textViewMakeModel: TextView = itemView.findViewById(R.id.textViewMakeModel)
        val textViewTheftLocation: TextView = itemView.findViewById(R.id.textViewTheftLocation)
        val textViewDate: TextView = itemView.findViewById(R.id.textViewDate)
        val buttonViewDetails: Button = itemView.findViewById(R.id.buttonViewDetails)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): StolenVehicleViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_stolen_vehicle, parent, false)
        return StolenVehicleViewHolder(view)
    }

    override fun onBindViewHolder(holder: StolenVehicleViewHolder, position: Int) {
        val vehicle = vehicles[position]
        holder.textViewLicensePlate.text = "License Plate: ${vehicle.licensePlate}"
        holder.textViewMakeModel.text = "Make: ${vehicle.make}, Model: ${vehicle.model}"
        holder.textViewTheftLocation.text = "Location: ${vehicle.theftLocation ?: "N/A"}"

        val formattedDate = vehicle.reportDate?.let {
            SimpleDateFormat("MMM dd, yyyy", Locale.getDefault()).format(it)
        } ?: "N/A"
        holder.textViewDate.text = "Reported On: $formattedDate"

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
     * @param newVehicles The new list of stolen vehicles.
     */
    fun updateData(newVehicles: List<Vehicle>) {
        vehicles.clear()
        vehicles.addAll(newVehicles)
        notifyDataSetChanged()
    }
}
