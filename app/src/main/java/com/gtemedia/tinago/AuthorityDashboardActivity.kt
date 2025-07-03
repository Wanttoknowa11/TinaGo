package com.gtemedia.tinago

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query

class AuthorityDashboardActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth
    private lateinit var firestore: FirebaseFirestore
    private lateinit var buttonScanToQuery: Button
    private lateinit var buttonLogoutAuthority: Button
    private lateinit var recyclerViewStolenVehicles: RecyclerView
    private lateinit var stolenVehiclesAdapter: StolenVehiclesAdapter
    private val stolenVehicleList = mutableListOf<StolenVehicleItem>()

    // Launcher for handling QR scan results for Authority
    private val authorityQrScannerLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == RESULT_OK) {
            val scannedLicensePlate = result.data?.getStringExtra("SCANNED_DATA")
            if (scannedLicensePlate != null && scannedLicensePlate.isNotEmpty()) {
                // Launch VehicleDetailsActivity
                val detailsIntent = Intent(this, VehicleDetailsActivity::class.java)
                detailsIntent.putExtra("SCANNED_LICENSE_PLATE", scannedLicensePlate) // Pass the license plate
                startActivity(detailsIntent)
            } else {
                Toast.makeText(this, "No QR data found for query.", Toast.LENGTH_SHORT).show()
            }
        } else {
            Toast.makeText(this, "QR Scan was cancelled by Authority.", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_authority_dashboard)

        auth = FirebaseAuth.getInstance()
        firestore = FirebaseFirestore.getInstance()

        val welcomeTextView: TextView = findViewById(R.id.welcomeTextView)
        buttonLogoutAuthority = findViewById(R.id.buttonLogoutAuthority)
        buttonScanToQuery = findViewById(R.id.buttonScanToQuery)

        // Initialize RecyclerView
        recyclerViewStolenVehicles = findViewById(R.id.recyclerViewStolenVehicles)
        recyclerViewStolenVehicles.layoutManager = LinearLayoutManager(this)
        stolenVehiclesAdapter = StolenVehiclesAdapter(stolenVehicleList)
        recyclerViewStolenVehicles.adapter = stolenVehiclesAdapter

        // Optional: Set up item click listener for the adapter (can launch VehicleDetailsActivity)
        stolenVehiclesAdapter.onItemClick = { vehicleItem ->
            val detailsIntent = Intent(this, VehicleDetailsActivity::class.java)
            detailsIntent.putExtra("SCANNED_LICENSE_PLATE", vehicleItem.licensePlate)
            startActivity(detailsIntent)
        }

        val fullName = intent.getStringExtra("FULL_NAME") ?: "Authority"
        welcomeTextView.text = "Welcome, $fullName (Authority)"

        buttonLogoutAuthority.setOnClickListener {
            auth.signOut()
            finish()
        }

        buttonScanToQuery.setOnClickListener {
            val intent = Intent(this, QrScannerActivity::class.java)
            authorityQrScannerLauncher.launch(intent)
        }
    }

    override fun onResume() {
        super.onResume()
        // Refresh the list of stolen vehicles every time the activity comes to the foreground
        fetchStolenVehicles()
    }

    private fun fetchStolenVehicles() {
        // First, get all vehicles marked as "reported_stolen"
        firestore.collection("vehicles")
            .whereEqualTo("status", "reported_stolen")
            .get()
            .addOnSuccessListener { vehicleQuerySnapshot ->
                stolenVehicleList.clear() // Clear existing list
                val stolenPlates = mutableSetOf<String>()

                // Collect license plates of all stolen vehicles
                for (vehicleDoc in vehicleQuerySnapshot.documents) {
                    val licensePlate = vehicleDoc.id
                    stolenPlates.add(licensePlate)
                }

                // Now, fetch the active theft reports for these plates
                if (stolenPlates.isNotEmpty()) {
                    firestore.collection("theft_reports")
                        .whereIn("licensePlate", stolenPlates.toList())
                        .whereEqualTo("status", "reported") // Only active reports
                        .orderBy("reportDate", Query.Direction.DESCENDING) // Order by latest report
                        .get()
                        .addOnSuccessListener { reportQuerySnapshot ->
                            for (reportDoc in reportQuerySnapshot.documents) {
                                val licensePlate = reportDoc.getString("licensePlate") ?: "N/A"
                                val reportDate = reportDoc.getTimestamp("reportDate")?.toDate()

                                // Get corresponding vehicle details from vehicleQuerySnapshot
                                val vehicleDoc = vehicleQuerySnapshot.documents.firstOrNull { it.id == licensePlate }

                                if (vehicleDoc != null) {
                                    val make = vehicleDoc.getString("make") ?: "N/A"
                                    val model = vehicleDoc.getString("model") ?: "N/A"
                                    stolenVehicleList.add(StolenVehicleItem(
                                        licensePlate, make, model, reportDate.toString(),
                                        description = TODO(),
                                        location = TODO(),
                                        reportDate = TODO(),
                                        imageUrls = TODO()
                                    ))
                                } else {
                                    Log.w("AuthorityDashboard", "No vehicle document found for stolen report with license plate: $licensePlate")
                                }
                            }
                            stolenVehiclesAdapter.notifyDataSetChanged() // Notify adapter data changed
                            if (stolenVehicleList.isEmpty()) {
                                // No toast here, as it's normal to have no stolen vehicles
                                Log.d("AuthorityDashboard", "No active stolen vehicles found.")
                            }
                        }
                        .addOnFailureListener { e ->
                            Log.e("AuthorityDashboard", "Error fetching theft reports: ${e.localizedMessage}", e)
                            Toast.makeText(this, "Error loading stolen reports: ${e.localizedMessage}", Toast.LENGTH_LONG).show()
                        }
                } else {
                    stolenVehiclesAdapter.notifyDataSetChanged() // Clear list if no stolen vehicles
                    // No toast here, as it's normal to have no stolen vehicles
                    Log.d("AuthorityDashboard", "No vehicles currently reported as stolen.")
                }
            }
            .addOnFailureListener { e ->
                Log.e("AuthorityDashboard", "Error fetching stolen vehicles (initial query): ${e.localizedMessage}", e)
                Toast.makeText(this, "Error loading stolen vehicle data: ${e.localizedMessage}", Toast.LENGTH_LONG).show()
            }
    }
}