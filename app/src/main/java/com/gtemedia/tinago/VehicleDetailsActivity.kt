package com.gtemedia.tinago

import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FieldValue // Import FieldValue for server timestamps
import java.text.SimpleDateFormat
import java.util.Locale

class VehicleDetailsActivity : AppCompatActivity() {

    private lateinit var firestore: FirebaseFirestore
    private lateinit var textViewDetailLicensePlate: TextView
    private lateinit var textViewDetailStatus: TextView
    private lateinit var textViewDetailMake: TextView
    private lateinit var textViewDetailModel: TextView
    private lateinit var textViewDetailOwnerUid: TextView // Hidden TextView for owner UID
    private lateinit var layoutReportDetails: LinearLayout // Layout for theft report details
    private lateinit var textViewReportDate: TextView
    private lateinit var textViewTheftDate: TextView
    private lateinit var textViewTheftLocation: TextView
    private lateinit var textViewReportDescription: TextView
    private lateinit var buttonMarkAsRecovered: Button
    private lateinit var textViewMessage: TextView

    // Variable to hold the license plate passed to this activity
    private var currentLicensePlate: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_vehicle_details)

        firestore = FirebaseFirestore.getInstance()

        // Initialize UI elements
        textViewDetailLicensePlate = findViewById(R.id.textViewDetailLicensePlate)
        textViewDetailStatus = findViewById(R.id.textViewDetailStatus)
        textViewDetailMake = findViewById(R.id.textViewDetailMake)
        textViewDetailModel = findViewById(R.id.textViewDetailModel)
        textViewDetailOwnerUid = findViewById(R.id.textViewDetailOwnerUid)
        layoutReportDetails = findViewById(R.id.layoutReportDetails)
        textViewReportDate = findViewById(R.id.textViewReportDate)
        textViewTheftDate = findViewById(R.id.textViewTheftDate)
        textViewTheftLocation = findViewById(R.id.textViewTheftLocation)
        textViewReportDescription = findViewById(R.id.textViewReportDescription)
        buttonMarkAsRecovered = findViewById(R.id.buttonMarkAsRecovered)
        textViewMessage = findViewById(R.id.textViewMessage)

        // Get the license plate from the Intent that started this activity
        currentLicensePlate = intent.getStringExtra("SCANNED_LICENSE_PLATE")

        if (currentLicensePlate != null) {
            textViewDetailLicensePlate.text = getString(R.string.license_plate_display, currentLicensePlate) // Use string resource for dynamic text
            fetchVehicleDetails(currentLicensePlate!!)
        } else {
            textViewMessage.text = "Error: No license plate provided."
            Toast.makeText(this, "Error: No license plate to query.", Toast.LENGTH_LONG).show()
            Log.e("VehicleDetails", "No license plate received in Intent.")
        }

        // Set up listener for the "Mark As Recovered" button
        buttonMarkAsRecovered.setOnClickListener {
            currentLicensePlate?.let {
                markVehicleAsRecovered(it)
            } ?: run {
                Toast.makeText(this, "No license plate to mark as recovered.", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun fetchVehicleDetails(licensePlate: String) {
        firestore.collection("vehicles").document(licensePlate)
            .get()
            .addOnSuccessListener { documentSnapshot ->
                if (documentSnapshot.exists()) {
                    val status = documentSnapshot.getString("status") ?: "N/A"
                    val make = documentSnapshot.getString("make") ?: "N/A"
                    val model = documentSnapshot.getString("model") ?: "N/A"
                    val ownerUid = documentSnapshot.getString("ownerUid") ?: "N/A"

                    textViewDetailStatus.text = getString(R.string.status_display, status.replace("_", " ").capitalize()) // Use string resource, format status
                    textViewDetailMake.text = getString(R.string.make_display, make) // Use string resource
                    textViewDetailModel.text = getString(R.string.model_display, model) // Use string resource
                    textViewDetailOwnerUid.text = getString(R.string.owner_uid_display, ownerUid) // Use string resource

                    // Adjust status text color
                    if (status == "reported_stolen") {
                        textViewDetailStatus.setTextColor(getColor(android.R.color.holo_red_dark))
                        fetchTheftReport(licensePlate) // Fetch report details if stolen
                        buttonMarkAsRecovered.visibility = View.VISIBLE // Show recover button
                    } else {
                        textViewDetailStatus.setTextColor(getColor(android.R.color.holo_green_dark))
                        layoutReportDetails.visibility = View.GONE // Hide report section if not stolen
                        buttonMarkAsRecovered.visibility = View.GONE // Hide recover button
                    }
                } else {
                    textViewMessage.text = "Vehicle '$licensePlate' not found."
                    Toast.makeText(this, "Vehicle '$licensePlate' not found.", Toast.LENGTH_LONG).show()
                    Log.w("VehicleDetails", "Vehicle document for '$licensePlate' does not exist.")
                    // If vehicle not found, hide report details and button
                    layoutReportDetails.visibility = View.GONE
                    buttonMarkAsRecovered.visibility = View.GONE
                }
            }
            .addOnFailureListener { e ->
                textViewMessage.text = "Error loading vehicle details."
                Toast.makeText(this, "Error loading vehicle details: ${e.localizedMessage}", Toast.LENGTH_LONG).show()
                Log.e("VehicleDetails", "Error fetching vehicle details for '$licensePlate'", e)
            }
    }

    private fun fetchTheftReport(licensePlate: String) {
        // Query the 'theft_reports' collection
        firestore.collection("theft_reports")
            .whereEqualTo("licensePlate", licensePlate)
            .whereEqualTo("status", "reported") // Only get active 'reported' thefts
            .get()
            .addOnSuccessListener { querySnapshot ->
                if (!querySnapshot.isEmpty) {
                    // Assuming only one active report per vehicle at a time
                    val reportDocument = querySnapshot.documents[0]
                    val reportDate = reportDocument.getTimestamp("reportDate")?.toDate()
                    val theftDate = reportDocument.getString("theftDate") ?: "N/A"
                    val theftLocation = reportDocument.getString("theftLocation") ?: "N/A"
                    val description = reportDocument.getString("description") ?: "N/A"

                    val dateFormat = SimpleDateFormat("MMM dd, yyyy HH:mm", Locale.getDefault())
                    val formattedReportDate = reportDate?.let { dateFormat.format(it) } ?: "N/A"


                    textViewReportDate.text = getString(R.string.report_date_display, formattedReportDate) // Use string resource
                    textViewTheftDate.text = getString(R.string.theft_date_display, theftDate) // Use string resource
                    textViewTheftLocation.text = getString(R.string.theft_location_display, theftLocation) // Use string resource
                    textViewReportDescription.text = getString(R.string.description_display, description) // Use string resource
                    layoutReportDetails.visibility = View.VISIBLE // Show the report details section
                } else {
                    // This case should ideally not happen if vehicle status is 'reported_stolen'
                    Log.w("VehicleDetails", "Vehicle '$licensePlate' is stolen but no active report found.")
                    textViewMessage.text = "Vehicle is stolen but no active report found."
                    layoutReportDetails.visibility = View.GONE // Hide if no report
                    buttonMarkAsRecovered.visibility = View.GONE // Hide if no report
                }
            }
            .addOnFailureListener { e ->
                Log.e("VehicleDetails", "Error fetching theft report for '$licensePlate'", e)
                Toast.makeText(this, "Error fetching theft report: ${e.localizedMessage}", Toast.LENGTH_LONG).show()
                layoutReportDetails.visibility = View.GONE // Hide on error
                buttonMarkAsRecovered.visibility = View.GONE // Hide on error
            }
    }

    private fun markVehicleAsRecovered(licensePlate: String) {
        // 1. Update vehicle status to 'recovered'
        firestore.collection("vehicles").document(licensePlate)
            .update("status", "recovered")
            .addOnSuccessListener {
                Log.d("VehicleDetails", "Vehicle '$licensePlate' status updated to 'recovered'.")

                // 2. Update the corresponding theft report status to 'resolved'
                firestore.collection("theft_reports")
                    .whereEqualTo("licensePlate", licensePlate)
                    .whereEqualTo("status", "reported") // Find the active report
                    .get()
                    .addOnSuccessListener { querySnapshot ->
                        if (!querySnapshot.isEmpty) {
                            val batch = firestore.batch()
                            querySnapshot.documents.forEach { document ->
                                val reportRef = firestore.collection("theft_reports").document(document.id)
                                batch.update(reportRef, mapOf(
                                    "status" to "resolved",
                                    "resolvedDate" to FieldValue.serverTimestamp() // Add resolution timestamp
                                ))
                            }
                            batch.commit()
                                .addOnSuccessListener {
                                    Toast.makeText(this, "Vehicle recovered and report resolved!", Toast.LENGTH_LONG).show()
                                    Log.d("VehicleDetails", "Theft report for '$licensePlate' resolved.")
                                    // Refresh UI after update
                                    fetchVehicleDetails(licensePlate)
                                }
                                .addOnFailureListener { e ->
                                    Toast.makeText(this, "Error resolving report: ${e.localizedMessage}", Toast.LENGTH_LONG).show()
                                    Log.e("VehicleDetails", "Error resolving theft report for '$licensePlate'", e)
                                }
                        } else {
                            Log.w("VehicleDetails", "No active theft report found to resolve for '$licensePlate'.")
                            Toast.makeText(this, "Vehicle recovered, but no active report to resolve.", Toast.LENGTH_LONG).show()
                            // Still refresh UI
                            fetchVehicleDetails(licensePlate)
                        }
                    }
                    .addOnFailureListener { e ->
                        Toast.makeText(this, "Error finding report to resolve: ${e.localizedMessage}", Toast.LENGTH_LONG).show()
                        Log.e("VehicleDetails", "Error finding theft report for '$licensePlate' to resolve", e)
                    }
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, "Error marking vehicle as recovered: ${e.localizedMessage}", Toast.LENGTH_LONG).show()
                Log.e("VehicleDetails", "Error updating vehicle status to 'recovered' for '$licensePlate'", e)
            }
    }
}