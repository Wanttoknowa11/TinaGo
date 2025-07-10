package com.gtemedia.tinago

import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import java.text.SimpleDateFormat
import java.util.Locale

class VehicleDetailsActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseFirestore

    private lateinit var imageViewVehicle: ImageView
    private lateinit var textViewLicensePlate: TextView
    private lateinit var textViewVIN: TextView
    private lateinit var textViewMakeModel: TextView
    private lateinit var textViewType: TextView
    private lateinit var textViewRegistrationDate: TextView
    private lateinit var textViewCurrentStatus: TextView
    private lateinit var textViewReportDetails: TextView
    private lateinit var textViewTheftDate: TextView
    private lateinit var textViewTheftLocation: TextView
    private lateinit var textViewDescription: TextView
    private lateinit var buttonReportTheft: Button
    private lateinit var buttonMarkAsRecovered: Button
    private lateinit var buttonBack: Button

    private var vehicleId: String? = null
    private var licensePlate: String? = null
    private var currentVehicleStatus: String? = null

    private val TAG = "VehicleDetailsActivity"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_vehicle_details)

        // Initialize Firebase
        auth = FirebaseAuth.getInstance()
        db = FirebaseFirestore.getInstance()

        // Get vehicle ID and license plate from intent
        vehicleId = intent.getStringExtra("vehicleId")
        licensePlate = intent.getStringExtra("licensePlate")

        // Initialize UI elements
        imageViewVehicle = findViewById(R.id.imageViewVehicle)
        textViewLicensePlate = findViewById(R.id.textViewLicensePlate)
        textViewVIN = findViewById(R.id.textViewVIN)
        textViewMakeModel = findViewById(R.id.textViewMakeModel)
        textViewType = findViewById(R.id.textViewType)
        textViewRegistrationDate = findViewById(R.id.textViewRegistrationDate)
        textViewCurrentStatus = findViewById(R.id.textViewCurrentStatus)
        textViewReportDetails = findViewById(R.id.textViewReportDetails)
        textViewTheftDate = findViewById(R.id.textViewTheftDate)
        textViewTheftLocation = findViewById(R.id.textViewTheftLocation)
        textViewDescription = findViewById(R.id.textViewDescription)
        buttonReportTheft = findViewById(R.id.buttonReportTheft)
        buttonMarkAsRecovered = findViewById(R.id.buttonMarkAsRecovered)
        buttonBack = findViewById(R.id.buttonBack)

        // Load vehicle details
        if (vehicleId != null) {
            loadVehicleDetails(vehicleId!!)
        } else {
            Toast.makeText(this, "Vehicle ID not provided.", Toast.LENGTH_SHORT).show()
            finish()
        }

        buttonReportTheft.setOnClickListener {
            val intent = Intent(this, ReportTheftActivity::class.java).apply {
                putExtra("vehicleId", vehicleId)
                putExtra("licensePlate", licensePlate)
            }
            startActivity(intent)
        }

        buttonMarkAsRecovered.setOnClickListener {
            markVehicleAsRecovered()
        }

        buttonBack.setOnClickListener {
            finish() // Go back to the previous activity (dashboard)
        }
    }

    /**
     * Loads vehicle details from Firestore and populates the UI.
     * @param id The document ID of the vehicle in Firestore.
     */
    private fun loadVehicleDetails(id: String) {
        db.collection("vehicles").document(id).get()
            .addOnSuccessListener { document ->
                if (document != null && document.exists()) {
                    val vehicle = document.toObject(Vehicle::class.java)
                    if (vehicle != null) {
                        // Update the global licensePlate and currentVehicleStatus
                        licensePlate = vehicle.licensePlate
                        currentVehicleStatus = vehicle.currentStatus

                        textViewLicensePlate.text = "License Plate: ${vehicle.licensePlate}"
                        textViewVIN.text = "VIN: ${vehicle.vin}"
                        textViewMakeModel.text = "Make: ${vehicle.make}, Model: ${vehicle.model}"
                        textViewType.text = "Type: ${vehicle.type}"

                        val formattedDate = vehicle.registrationDate?.let {
                            SimpleDateFormat("MMM dd, yyyy", Locale.getDefault()).format(it)
                        } ?: "N/A"
                        textViewRegistrationDate.text = "Registration Date: $formattedDate"

                        textViewCurrentStatus.text = "Status: ${vehicle.currentStatus?.replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString() }}"

                        // Set status text color
                        when (vehicle.currentStatus) {
                            "stolen" -> textViewCurrentStatus.setTextColor(Color.RED)
                            "recovered" -> textViewCurrentStatus.setTextColor(Color.parseColor("#006400")) // Dark Green
                            else -> textViewCurrentStatus.setTextColor(Color.BLACK) // Default for "registered"
                        }

                        // Handle image loading (using a library like Glide or Picasso in a real app)
                        // For now, a placeholder or default image is used in XML.
                        // if (vehicle.imageUrl?.isNotEmpty() == true) {
                        //     Glide.with(this).load(vehicle.imageUrl).into(imageViewVehicle)
                        // }

                        // Show/hide buttons based on vehicle status and user type
                        updateButtonVisibility(vehicle.currentStatus)

                        // Load theft report details if the vehicle is stolen or recovered
                        if (vehicle.currentStatus == "stolen" || vehicle.currentStatus == "recovered") {
                            loadTheftReportDetails(id)
                        }
                    }
                } else {
                    Toast.makeText(this, "Vehicle not found.", Toast.LENGTH_SHORT).show()
                    finish()
                }
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, "Error loading vehicle details: ${e.localizedMessage}", Toast.LENGTH_LONG).show()
                Log.e(TAG, "Error fetching vehicle document", e)
                finish()
            }
    }

    /**
     * Updates the visibility of the "Report Theft" and "Mark as Recovered" buttons
     * based on the vehicle's current status and the user's role.
     * @param status The current status of the vehicle.
     */
    private fun updateButtonVisibility(status: String?) {
        val currentUser = auth.currentUser
        if (currentUser != null) {
            db.collection("users").document(currentUser.uid).get()
                .addOnSuccessListener { userDoc ->
                    val userType = userDoc.getString("userType")
                    val isOwner = userDoc.id == auth.currentUser?.uid // Simple check, more robust needed for shared vehicles

                    when (status) {
                        "registered" -> {
                            buttonReportTheft.visibility = if (isOwner) View.VISIBLE else View.GONE
                            buttonMarkAsRecovered.visibility = View.GONE
                        }
                        "stolen" -> {
                            buttonReportTheft.visibility = View.GONE
                            buttonMarkAsRecovered.visibility = if (userType == "authority") View.VISIBLE else View.GONE
                            textViewReportDetails.visibility = View.VISIBLE
                            textViewTheftDate.visibility = View.VISIBLE
                            textViewTheftLocation.visibility = View.VISIBLE
                            textViewDescription.visibility = View.VISIBLE
                        }
                        "recovered" -> {
                            buttonReportTheft.visibility = View.GONE
                            buttonMarkAsRecovered.visibility = View.GONE // Already recovered
                            textViewReportDetails.visibility = View.VISIBLE
                            textViewTheftDate.visibility = View.VISIBLE
                            textViewTheftLocation.visibility = View.VISIBLE
                            textViewDescription.visibility = View.VISIBLE
                        }
                        else -> {
                            buttonReportTheft.visibility = View.GONE
                            buttonMarkAsRecovered.visibility = View.GONE
                        }
                    }
                }
                .addOnFailureListener { e ->
                    Log.e(TAG, "Error fetching user type for button visibility: ${e.localizedMessage}", e)
                    // Default to hiding buttons on error
                    buttonReportTheft.visibility = View.GONE
                    buttonMarkAsRecovered.visibility = View.GONE
                }
        } else {
            // User not logged in, hide all action buttons
            buttonReportTheft.visibility = View.GONE
            buttonMarkAsRecovered.visibility = View.GONE
        }
    }

    /**
     * Loads the theft report details for a given vehicle ID.
     * @param vehicleId The ID of the vehicle.
     */
    private fun loadTheftReportDetails(vehicleId: String) {
        db.collection("theft_reports")
            .whereEqualTo("vehicleId", vehicleId)
            .orderBy("reportDate", com.google.firebase.firestore.Query.Direction.DESCENDING) // Get the latest report
            .limit(1)
            .get()
            .addOnSuccessListener { documents ->
                if (!documents.isEmpty) {
                    val report = documents.documents[0].data
                    if (report != null) {
                        textViewReportDetails.visibility = View.VISIBLE
                        textViewTheftDate.visibility = View.VISIBLE
                        textViewTheftLocation.visibility = View.VISIBLE
                        textViewDescription.visibility = View.VISIBLE

                        val theftDate = report["theftDate"] as? com.google.firebase.Timestamp
                        val formattedTheftDate = theftDate?.toDate()?.let {
                            SimpleDateFormat("MMM dd, yyyy", Locale.getDefault()).format(it)
                        } ?: "N/A"

                        textViewTheftDate.text = "Theft Date: $formattedTheftDate"
                        textViewTheftLocation.text = "Theft Location: ${report["theftLocation"] as? String ?: "N/A"}"
                        textViewDescription.text = "Description: ${report["description"] as? String ?: "N/A"}"
                    }
                } else {
                    Log.d(TAG, "No theft report found for vehicle ID: $vehicleId")
                    textViewReportDetails.visibility = View.GONE
                    textViewTheftDate.visibility = View.GONE
                    textViewTheftLocation.visibility = View.GONE
                    textViewDescription.visibility = View.GONE
                }
            }
            .addOnFailureListener { e ->
                Log.e(TAG, "Error loading theft report details: ${e.localizedMessage}", e)
                Toast.makeText(this, "Error loading theft report details.", Toast.LENGTH_SHORT).show()
            }
    }

    /**
     * Marks the current vehicle as "recovered" in Firestore.
     * This function should only be callable by authority users.
     */
    private fun markVehicleAsRecovered() {
        if (vehicleId == null) {
            Toast.makeText(this, "Vehicle ID is missing.", Toast.LENGTH_SHORT).show()
            return
        }

        // Confirm with the user before marking as recovered
        // In a real app, you might use a custom dialog instead of a simple Toast.
        Toast.makeText(this, "Marking as recovered...", Toast.LENGTH_SHORT).show()

        db.collection("vehicles").document(vehicleId!!)
            .update("currentStatus", "recovered")
            .addOnSuccessListener {
                Toast.makeText(this, "Vehicle marked as recovered!", Toast.LENGTH_SHORT).show()
                Log.d(TAG, "Vehicle $licensePlate marked as recovered.")
                // Refresh UI or navigate back
                loadVehicleDetails(vehicleId!!) // Reload details to reflect status change
                // In a real app, a Cloud Function would trigger FCM to notify the owner.
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, "Error marking vehicle as recovered: ${e.localizedMessage}", Toast.LENGTH_LONG).show()
                Log.e(TAG, "Error updating vehicle status to 'recovered' for '$licensePlate'", e)
            }
    }

    // Data class to represent a Vehicle (for Firestore mapping)
    // This should ideally be in a separate file or a common data model package
    data class Vehicle(
        val id: String = "", // Document ID from Firestore
        val ownerId: String = "",
        val licensePlate: String = "",
        val vin: String = "",
        val make: String = "",
        val model: String = "",
        val type: String = "",
        val registrationDate: Date? = null,
        var currentStatus: String = "registered",
        val imageUrl: String? = null,
        val qrCodeUrl: String? = null,
        // Fields for theft report, might be null if not stolen
        val theftDate: Date? = null,
        val theftLocation: String? = null,
        val description: String? = null,
        val reportDate: Date? = null
    ) {
        // No-argument constructor required for Firestore deserialization
        constructor() : this("", "", "", "", "", "", "", null, "registered", null, null, null, null, null, null)
    }
}
