package com.gtemedia.tinago

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import java.util.Date
import java.util.Locale

class AuthorityDashboardActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseFirestore
    private lateinit var recyclerViewStolenVehicles: RecyclerView
    private lateinit var stolenVehiclesAdapter: StolenVehiclesAdapter
    private val stolenVehiclesList = mutableListOf<Vehicle>()
    private var firestoreListener: ListenerRegistration? = null

    private val TAG = "AuthorityDashboard"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_authority_dashboard)

        // Initialize Firebase
        auth = FirebaseAuth.getInstance()
        db = FirebaseFirestore.getInstance()

        // Initialize UI elements
        val buttonScanQR = findViewById<Button>(R.id.buttonScanQR)
        val buttonLogoutAuthority = findViewById<Button>(R.id.buttonLogoutAuthority)
        recyclerViewStolenVehicles = findViewById(R.id.recyclerViewStolenVehicles)

        // Set up RecyclerView
        recyclerViewStolenVehicles.layoutManager = LinearLayoutManager(this)
        stolenVehiclesAdapter = StolenVehiclesAdapter(stolenVehiclesList)
        recyclerViewStolenVehicles.adapter = stolenVehiclesAdapter

        // Set up button listeners
        buttonScanQR.setOnClickListener {
            val intent = Intent(this, QrScannerActivity::class.java)
            startActivity(intent)
        }

        buttonLogoutAuthority.setOnClickListener {
            auth.signOut()
            Toast.makeText(this, "Logged out successfully.", Toast.LENGTH_SHORT).show()
            val intent = Intent(this, MainActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            startActivity(intent)
            finish()
        }
    }

    override fun onStart() {
        super.onStart()
        // Ensure user is logged in and is an authority before fetching data
        val currentUser = auth.currentUser
        if (currentUser == null) {
            // Not logged in, redirect to main activity
            val intent = Intent(this, MainActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            startActivity(intent)
            finish()
        } else {
            // Verify user type (optional, but good for security)
            db.collection("users").document(currentUser.uid).get()
                .addOnSuccessListener { document ->
                    val userType = document.getString("userType")
                    if (userType == "authority") {
                        listenForStolenVehicles()
                    } else {
                        Toast.makeText(this, "Access denied. You are not an authority.", Toast.LENGTH_LONG).show()
                        auth.signOut()
                        val intent = Intent(this, MainActivity::class.java)
                        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                        startActivity(intent)
                        finish()
                    }
                }
                .addOnFailureListener { e ->
                    Log.e(TAG, "Error fetching user type: ${e.localizedMessage}", e)
                    Toast.makeText(this, "Error verifying user role. Please re-login.", Toast.LENGTH_LONG).show()
                    auth.signOut()
                    val intent = Intent(this, MainActivity::class.java)
                    intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                    startActivity(intent)
                    finish()
                }
        }
    }

    override fun onStop() {
        super.onStop()
        // Detach Firestore listener to prevent memory leaks
        firestoreListener?.remove()
    }

    /**
     * Sets up a real-time listener for stolen vehicles in Firestore.
     */
    private fun listenForStolenVehicles() {
        firestoreListener?.remove() // Remove previous listener if exists

        firestoreListener = db.collection("vehicles")
            .whereEqualTo("currentStatus", "stolen")
            // .orderBy("reportDate", com.google.firebase.firestore.Query.Direction.DESCENDING) // Requires index
            .addSnapshotListener { snapshots, e ->
                if (e != null) {
                    Log.w(TAG, "Listen failed for stolen vehicles.", e)
                    Toast.makeText(this, "Error loading stolen vehicle data: ${e.localizedMessage}",
                        Toast.LENGTH_LONG).show()
                    return@addSnapshotListener
                }

                if (snapshots != null) {
                    val updatedStolenVehicles = mutableListOf<Vehicle>()
                    for (doc in snapshots.documents) {
                        val vehicle = doc.toObject(Vehicle::class.java)
                        if (vehicle != null) {
                            // Manually set the ID as it's not automatically mapped by toObject
                            val vehicleWithId = vehicle.copy(id = doc.id)

                            // Fetch the latest theft report details for this vehicle
                            db.collection("theft_reports")
                                .whereEqualTo("vehicleId", doc.id)
                                .orderBy("reportDate", com.google.firebase.firestore.Query.Direction.DESCENDING)
                                .limit(1)
                                .get()
                                .addOnSuccessListener { theftReports ->
                                    if (!theftReports.isEmpty) {
                                        val latestReport = theftReports.documents[0].data
                                        val theftDate = latestReport?.get("theftDate") as? com.google.firebase.Timestamp
                                        val theftLocation = latestReport?.get("theftLocation") as? String
                                        val reportDate = latestReport?.get("reportDate") as? com.google.firebase.Timestamp

                                        // Create a new Vehicle object with theft details
                                        val fullVehicle = vehicleWithId.copy(
                                            theftDate = theftDate?.toDate(),
                                            theftLocation = theftLocation,
                                            reportDate = reportDate?.toDate()
                                        )
                                        updatedStolenVehicles.add(fullVehicle)
                                    } else {
                                        updatedStolenVehicles.add(vehicleWithId) // Add without theft details if none found
                                    }
                                    // Update adapter after all details are fetched for this batch
                                    // This might cause multiple updates if many vehicles are fetched,
                                    // consider a single update after all async calls complete.
                                    updatedStolenVehicles.sortByDescending { it.reportDate ?: Date(0) } // Sort by report date
                                    stolenVehiclesAdapter.updateData(updatedStolenVehicles)
                                }
                                .addOnFailureListener { e ->
                                    Log.e(TAG, "Error fetching theft report for vehicle ${doc.id}: ${e.localizedMessage}", e)
                                    updatedStolenVehicles.add(vehicleWithId) // Add vehicle even if report fetch fails
                                    updatedStolenVehicles.sortByDescending { it.reportDate ?: Date(0) }
                                    stolenVehiclesAdapter.updateData(updatedStolenVehicles)
                                }
                        }
                    }
                    Log.d(TAG, "Stolen vehicles updated: ${updatedStolenVehicles.size}")
                } else {
                    Log.d(TAG, "Current data: null")
                }
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
