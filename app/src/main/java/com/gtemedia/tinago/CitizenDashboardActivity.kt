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

class CitizenDashboardActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth
    private lateinit var firestore: FirebaseFirestore
    private lateinit var buttonScanToReport: Button
    private lateinit var buttonRegisterNewVehicle: Button
    private lateinit var buttonLogoutCitizen: Button

    private lateinit var recyclerViewCitizenVehicles: RecyclerView
    private lateinit var citizenVehiclesAdapter: CitizenVehiclesAdapter
    private val citizenVehicleList = mutableListOf<CitizenVehicleItem>()

    // Launcher for handling QR scan results for Citizen
    private val citizenQrScannerLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == RESULT_OK) {
            val scannedLicensePlate = result.data?.getStringExtra("SCANNED_DATA")
            if (scannedLicensePlate != null && scannedLicensePlate.isNotEmpty()) {
                // Launch ReportTheftActivity
                val reportIntent = Intent(this, ReportTheftActivity::class.java)
                reportIntent.putExtra("SCANNED_LICENSE_PLATE", scannedLicensePlate)
                startActivity(reportIntent)
            } else {
                Toast.makeText(this, "No QR data found for reporting.", Toast.LENGTH_SHORT).show()
            }
        } else {
            Toast.makeText(this, "QR Scan was cancelled by Citizen.", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_citizen_dashboard)

        auth = FirebaseAuth.getInstance()
        firestore = FirebaseFirestore.getInstance()

        val welcomeTextView: TextView = findViewById(R.id.welcomeTextView)
        buttonScanToReport = findViewById(R.id.buttonScanToReport)
        buttonRegisterNewVehicle = findViewById(R.id.buttonRegisterNewVehicle)
        buttonLogoutCitizen = findViewById(R.id.buttonLogoutCitizen)

        // Initialize RecyclerView
        recyclerViewCitizenVehicles = findViewById(R.id.recyclerViewCitizenVehicles)
        recyclerViewCitizenVehicles.layoutManager = LinearLayoutManager(this)
        citizenVehiclesAdapter = CitizenVehiclesAdapter(citizenVehicleList)
        recyclerViewCitizenVehicles.adapter = citizenVehiclesAdapter

        // Optional: Set up item click listener for the adapter (if you want to open details)
        citizenVehiclesAdapter.onItemClick = { vehicleItem ->
            // For now, let's just show a toast, but you could launch a VehicleDetailsActivity
            // We can adapt VehicleDetailsActivity to show general details, not just stolen ones.
            val detailsIntent = Intent(this, VehicleDetailsActivity::class.java)
            detailsIntent.putExtra("SCANNED_LICENSE_PLATE", vehicleItem.licensePlate)
            startActivity(detailsIntent)
            // Toast.makeText(this, "Clicked on: ${vehicleItem.licensePlate} (Status: ${vehicleItem.status})", Toast.LENGTH_SHORT).show()
        }

        val fullName = intent.getStringExtra("FULL_NAME") ?: "Citizen"
        welcomeTextView.text = getString(R.string.welcome_citizen, fullName) // Use string resource if applicable

        buttonLogoutCitizen.setOnClickListener {
            auth.signOut()
            finish() // Finish dashboard, go back to LoginActivity
        }

        buttonScanToReport.setOnClickListener {
            val intent = Intent(this, QrScannerActivity::class.java)
            citizenQrScannerLauncher.launch(intent)
        }

        buttonRegisterNewVehicle.setOnClickListener {
            val intent = Intent(this, RegisterVehicleActivity::class.java)
            startActivity(intent)
        }
    }

    override fun onResume() {
        super.onResume()
        // Refresh the list of vehicles every time the activity comes to the foreground
        fetchCitizenVehicles()
    }

    private fun fetchCitizenVehicles() {
        val currentUser = auth.currentUser
        if (currentUser == null) {
            Toast.makeText(this, "User not logged in.", Toast.LENGTH_SHORT).show()
            Log.e("CitizenDashboard", "Attempted to fetch vehicles without a logged-in user.")
            return
        }

        val ownerUid = currentUser.uid

        firestore.collection("vehicles")
            .whereEqualTo("ownerUid", ownerUid)
            .get()
            .addOnSuccessListener { querySnapshot ->
                citizenVehicleList.clear() // Clear previous list
                for (document in querySnapshot.documents) {
                    val licensePlate = document.id // Document ID is the license plate
                    val make = document.getString("make") ?: "N/A"
                    val model = document.getString("model") ?: "N/A"
                    val status = document.getString("status") ?: "N/A"
                    citizenVehicleList.add(CitizenVehicleItem(licensePlate, make, model, status))
                }
                citizenVehiclesAdapter.notifyDataSetChanged() // Notify adapter that data has changed

                if (citizenVehicleList.isEmpty()) {
                    // No toast here as it's normal for new users
                    Log.d("CitizenDashboard", "User has no registered vehicles.")
                }
            }
            .addOnFailureListener { e ->
                Log.e("CitizenDashboard", "Error fetching citizen's vehicles: ${e.localizedMessage}", e)
                Toast.makeText(this, "Error loading your vehicles: ${e.localizedMessage}", Toast.LENGTH_LONG).show()
            }
    }
}