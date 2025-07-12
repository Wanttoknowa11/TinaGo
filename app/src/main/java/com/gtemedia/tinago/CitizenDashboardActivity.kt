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

class CitizenDashboardActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseFirestore
    private lateinit var recyclerViewCitizenVehicles: RecyclerView
    private lateinit var citizenVehiclesAdapter: CitizenVehiclesAdapter
    private val vehiclesList = mutableListOf<Vehicle>()
    private var firestoreListener: ListenerRegistration? = null

    private val TAG = "CitizenDashboard"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_citizen_dashboard)

        // Initialize Firebase
        auth = FirebaseAuth.getInstance()
        db = FirebaseFirestore.getInstance()

        // Initialize UI elements
        val buttonRegisterVehicle = findViewById<Button>(R.id.buttonRegisterNewVehicle)
        val buttonScanQR = findViewById<Button>(R.id.buttonScanToReport)
        val buttonUserProfile = findViewById<Button>(R.id.buttonUserProfile)
        recyclerViewCitizenVehicles = findViewById(R.id.recyclerViewCitizenVehicles)

        // Set up RecyclerView
        recyclerViewCitizenVehicles.layoutManager = LinearLayoutManager(this)
        citizenVehiclesAdapter = CitizenVehiclesAdapter(vehiclesList)
        recyclerViewCitizenVehicles.adapter = citizenVehiclesAdapter

        // Set up button listeners
        buttonRegisterVehicle.setOnClickListener {
            val intent = Intent(this, RegisterVehicleActivity::class.java)
            startActivity(intent)
        }

        buttonScanQR.setOnClickListener {
            val intent = Intent(this, QrScannerActivity::class.java)
            startActivity(intent)
        }

        buttonUserProfile.setOnClickListener {
            val intent = Intent(this, UserProfileActivity::class.java)
            startActivity(intent)
        }
    }

    override fun onStart() {
        super.onStart()
        // Ensure user is logged in before fetching data
        val currentUser = auth.currentUser
        if (currentUser == null) {
            // Not logged in, redirect to main activity
            val intent = Intent(this, MainActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            startActivity(intent)
            finish()
        } else {
            // Start listening for real-time updates to the user's vehicles
            listenForCitizenVehicles(currentUser.uid)
        }
    }

    override fun onStop() {
        super.onStop()
        // Detach Firestore listener to prevent memory leaks
        firestoreListener?.remove()
    }

    /**
     * Sets up a real-time listener for the current user's vehicles in Firestore.
     * @param userId The UID of the current user.
     */
    private fun listenForCitizenVehicles(userId: String) {
        firestoreListener?.remove() // Remove previous listener if exists

        firestoreListener = db.collection("vehicles")
            .whereEqualTo("ownerId", userId)
            // .orderBy("registrationDate", com.google.firebase.firestore.Query.Direction.DESCENDING) // Ordering requires index
            .addSnapshotListener { snapshots, e ->
                if (e != null) {
                    Log.w(TAG, "Listen failed for citizen vehicles.", e)
                    Toast.makeText(this, "Error loading your vehicles: ${e.localizedMessage}", Toast.LENGTH_LONG).show()
                    return@addSnapshotListener
                }

                if (snapshots != null) {
                    val updatedVehicles = mutableListOf<Vehicle>()
                    for (doc in snapshots.documents) {
                        val vehicle = doc.toObject(Vehicle::class.java)
                        if (vehicle != null) {
                            // Manually set the ID as it's not automatically mapped by toObject
                            val vehicleWithId = vehicle.copy(id = doc.id)
                            updatedVehicles.add(vehicleWithId)
                        }
                    }
                    // Sort vehicles by registration date (latest first) in memory
                    updatedVehicles.sortByDescending { it.registrationDate }
                    citizenVehiclesAdapter.updateData(updatedVehicles)
                    Log.d(TAG, "Citizen vehicles updated: ${updatedVehicles.size}")
                } else {
                    Log.d(TAG, "Current data: null")
                }
            }
    }


}
