package com.gtemedia.tinago

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class UserProfileActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseFirestore

    private lateinit var editTextFullName: EditText
    private lateinit var editTextEmail: EditText
    private lateinit var editTextPhoneNumber: EditText
    private lateinit var editTextAddress: EditText
    private lateinit var saveProfileButton: Button
    private lateinit var logoutButton: Button

    private val TAG = "UserProfileActivity"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_user_profile)

        // Initialize Firebase
        auth = FirebaseAuth.getInstance()
        db = FirebaseFirestore.getInstance()

        // Initialize UI elements
        editTextFullName = findViewById(R.id.editTextFullName)
        editTextEmail = findViewById(R.id.editTextEmail)
        editTextPhoneNumber = findViewById(R.id.editTextPhoneNumber)
        editTextAddress = findViewById(R.id.editTextAddress)
        saveProfileButton = findViewById(R.id.saveProfileButton)
        logoutButton = findViewById(R.id.logoutButton)

        // Load user profile data
        loadUserProfile()

        // Set up save profile button click listener
        saveProfileButton.setOnClickListener {
            saveUserProfile()
        }

        // Set up logout button click listener
        logoutButton.setOnClickListener {
            auth.signOut()
            Toast.makeText(this, "Logged out successfully.", Toast.LENGTH_SHORT).show()
            val intent = Intent(this, MainActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            startActivity(intent)
            finish()
        }
    }

    /**
     * Loads the current user's profile data from Firestore and populates the UI fields.
     */
    private fun loadUserProfile() {
        val userId = auth.currentUser?.uid
        if (userId != null) {
            db.collection("users").document(userId).get()
                .addOnSuccessListener { document ->
                    if (document != null && document.exists()) {
                        editTextFullName.setText(document.getString("fullName"))
                        editTextEmail.setText(document.getString("email"))
                        editTextPhoneNumber.setText(document.getString("phoneNumber"))
                        editTextAddress.setText(document.getString("address"))
                        Log.d(TAG, "User profile loaded successfully.")
                    } else {
                        Toast.makeText(this, "User profile not found.", Toast.LENGTH_SHORT).show()
                        Log.w(TAG, "No such document for user ID: $userId")
                    }
                }
                .addOnFailureListener { e ->
                    Toast.makeText(this, "Error loading profile: ${e.localizedMessage}", Toast.LENGTH_LONG).show()
                    Log.e(TAG, "Error loading user document", e)
                }
        } else {
            Toast.makeText(this, "User not logged in.", Toast.LENGTH_SHORT).show()
            finish() // Close activity if user is not logged in
        }
    }

    /**
     * Saves the updated user profile data to Firestore.
     */
    private fun saveUserProfile() {
        val userId = auth.currentUser?.uid
        if (userId != null) {
            val fullName = editTextFullName.text.toString().trim()
            val phoneNumber = editTextPhoneNumber.text.toString().trim()
            val address = editTextAddress.text.toString().trim()

            if (fullName.isEmpty()) {
                Toast.makeText(this, "Full Name cannot be empty.", Toast.LENGTH_SHORT).show()
                return
            }

            val userUpdates = hashMapOf(
                "fullName" to fullName,
                "phoneNumber" to phoneNumber,
                "address" to address
            )

            db.collection("users").document(userId)
                .update(userUpdates as Map<String, Any>) // Cast to Map<String, Any>
                .addOnSuccessListener {
                    Toast.makeText(this, "Profile updated successfully!", Toast.LENGTH_SHORT).show()
                    Log.d(TAG, "User profile updated for ID: $userId")
                }
                .addOnFailureListener { e ->
                    Toast.makeText(this, "Failed to update profile: ${e.message}", Toast.LENGTH_LONG).show()
                    Log.e(TAG, "Error updating user document", e)
                }
        }
    }
}
