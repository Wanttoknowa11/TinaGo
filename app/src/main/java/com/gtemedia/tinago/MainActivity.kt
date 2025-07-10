package com.gtemedia.tinago // Make sure this matches your package name!

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.FirebaseApp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.messaging.FirebaseMessaging

class MainActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseFirestore
    private lateinit var textViewStatus: TextView
    private val tag = "MainActivity" // Tag for logging

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // Initialize Firebase (if not already initialized)
        FirebaseApp.initializeApp(this)

        // Initialize Firebase Auth and Firestore instances
        auth = FirebaseAuth.getInstance()
        db = FirebaseFirestore.getInstance()

        // Get references to UI elements
        val buttonLogin = findViewById<Button>(R.id.buttonLogin)
        val buttonRegister = findViewById<Button>(R.id.buttonRegister)
        textViewStatus = findViewById(R.id.textViewStatus)

        // Set up click listener for the Login button
        buttonLogin.setOnClickListener {
            val intent = Intent(this, LoginActivity::class.java)
            startActivity(intent)
        }

        // Set up click listener for the Register button
        buttonRegister.setOnClickListener {
            val intent = Intent(this, RegisterActivity::class.java)
            startActivity(intent)
        }
    }

    override fun onStart() {
        super.onStart()
        // Check if user is signed in (non-null) and update UI accordingly.
        val currentUser = auth.currentUser
        if (currentUser != null) {
            // User is logged in, fetch their user type from Firestore
            fetchUserTypeAndRedirect(currentUser.uid)
        } else {
            // No user is signed in, display a message or keep login/register buttons active
            textViewStatus.text = "Please login or register to continue."
        }
    }

    /**
     * Fetches the user's type from Firestore and redirects them to the appropriate dashboard.
     * @param userId The UID of the currently logged-in Firebase user.
     */
    private fun fetchUserTypeAndRedirect(userId: String) {
        db.collection("users").document(userId).get()
            .addOnSuccessListener { document ->
                if (document != null && document.exists()) {
                    val userType = document.getString("userType")
                    Log.d(tag, "User type fetched: $userType")

                    // Save FCM token for push notifications
                    FirebaseMessaging.getInstance().token.addOnCompleteListener { task ->
                        if (task.isSuccessful) {
                            val token = task.result
                            if (token != null) {
                                db.collection("users").document(userId)
                                    .update("fcmToken", token)
                                    .addOnSuccessListener {
                                        Log.d(tag, "FCM token saved successfully.")
                                    }
                                    .addOnFailureListener { e ->
                                        Log.w(tag, "Failed to save FCM token.", e)
                                    }
                            }
                        } else {
                            Log.w(tag, "Fetching FCM token failed.", task.exception)
                        }
                    }

                    // Redirect based on user type
                    if (userType == "authority") {
                        val intent = Intent(this, AuthorityDashboardActivity::class.java)
                        startActivity(intent)
                    } else { // Default to citizen
                        val intent = Intent(this, CitizenDashboardActivity::class.java)
                        startActivity(intent)
                    }
                    finish() // Close MainActivity after redirection
                } else {
                    // User document not found, might be a new user or data issue
                    Log.w(tag, "User document not found for UID: $userId. Signing out.")
                    textViewStatus.text = "User profile not found. Please register again."
                    auth.signOut() // Sign out the user if their data is missing
                }
            }
            .addOnFailureListener { e ->
                // Error fetching user document
                Log.e(tag, "Error fetching user document: ${e.localizedMessage}", e)
                textViewStatus.text = "Error fetching user profile. Please try again."
                auth.signOut() // Sign out on error
            }
    }
}
