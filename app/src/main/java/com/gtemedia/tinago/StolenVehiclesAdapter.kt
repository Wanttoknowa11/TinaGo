package com.gtemedia.tinago

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.recyclerview.widget.RecyclerView
import com.denzcoskun.imageslider.models.SlideModel
import com.denzcoskun.imageslider.ImageSlider
import java.text.SimpleDateFormat
import java.util.*

data class StolenVehicleItem(
    val licensePlate: String,
    val vin: String,
    val make: String,
    val model: String,
    val description: String,
    val location: String,
    val reportDate: Date?,
    val imageUrls: List<String> = emptyList()
)

class StolenVehiclesAdapter(
    private val stolenVehicles: List<StolenVehicleItem>
) : RecyclerView.Adapter<StolenVehiclesAdapter.StolenVehicleViewHolder>() {

    var onItemClick: ((StolenVehicleItem) -> Unit)? = null
    var onFoundClick: ((StolenVehicleItem) -> Unit)? = null
    var onSpottedClick: ((StolenVehicleItem) -> Unit)? = null

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): StolenVehicleViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_stolen_vehicle, parent, false)
        return StolenVehicleViewHolder(view)
    }

    override fun onBindViewHolder(holder: StolenVehicleViewHolder, position: Int) {
        holder.bind(stolenVehicles[position])
    }

    override fun getItemCount(): Int = stolenVehicles.size

    inner class StolenVehicleViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {

        private val imageSlider: ImageSlider = itemView.findViewById(R.id.image_slider)
        private val textViewPlate: TextView = itemView.findViewById(R.id.textViewStolenPlate)
        private val textViewVIN: TextView = itemView.findViewById(R.id.textViewStolenVIN)
        private val textViewMakeModel: TextView = itemView.findViewById(R.id.textViewStolenMakeModel)
        private val textViewDetails: TextView = itemView.findViewById(R.id.textViewTheftDetails)
        private val textViewLocation: TextView = itemView.findViewById(R.id.textViewTheftLocation)
        private val textViewDate: TextView = itemView.findViewById(R.id.textViewStolenReportDate)
        private val buttonFound: Button = itemView.findViewById(R.id.Fbutton)
        private val buttonSpotted: Button = itemView.findViewById(R.id.sbutton)

        init {
            itemView.setOnClickListener {
                onItemClick?.invoke(stolenVehicles[adapterPosition])
            }
            buttonFound.setOnClickListener {
                onFoundClick?.invoke(stolenVehicles[adapterPosition])
            }
            buttonSpotted.setOnClickListener {
                onSpottedClick?.invoke(stolenVehicles[adapterPosition])
            }
        }

        fun bind(vehicle: StolenVehicleItem) {
            val imageList = ArrayList<SlideModel>()

            imageList.add(SlideModel(R.drawable.img, "Image One."))
            imageList.add(SlideModel(R.drawable.img, "Image Two."))
            imageList.add(SlideModel(R.drawable.img, "Image Three."))
            imageList.add(SlideModel(R.drawable.img, "Image Four."))
            imageSlider.setImageList(imageList)

            textViewPlate.text = "License Plate: ${vehicle.licensePlate}"
            textViewVIN.text = "VIN: ${vehicle.vin}"
            textViewMakeModel.text = "Make: ${vehicle.make}, Model: ${vehicle.model}"
            textViewDetails.text = "Details: ${vehicle.description}"
            textViewLocation.text = "Location: ${vehicle.location}"

            val formattedDate = vehicle.reportDate?.let {
                SimpleDateFormat("MMM dd, yyyy", Locale.getDefault()).format(it)
            } ?: "N/A"
            textViewDate.text = "Reported On: $formattedDate"
        }

    }
}
