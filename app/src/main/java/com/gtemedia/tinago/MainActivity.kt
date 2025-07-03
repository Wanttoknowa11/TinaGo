package com.gtemedia.tinago // Make sure this matches your package name!

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.EditText
import android.widget.RadioButton
import android.widget.RadioGroup
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.Timestamp // Import for server timestamp

class MainActivity : AppCompatActivity() {

    // Firebase instances
    private lateinit var auth: FirebaseAuth
    private lateinit var firestore: FirebaseFirestore

    // UI elements declarations
    private lateinit var editTextFullName: EditText
    private lateinit var editTextEmail: EditText
    private lateinit var editTextPassword: EditText
    private lateinit var radioGroupUserType: RadioGroup
    private lateinit var radioCitizen: RadioButton
    private lateinit var radioAuthority: RadioButton
    private lateinit var buttonRegister: Button
    private lateinit var buttonLogin: Button
    private lateinit var textViewStatus: TextView // For displaying messages to the user

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main) // Link to your layout file

        // Initialize Firebase instances
        auth = FirebaseAuth.getInstance()
        firestore = FirebaseFirestore.getInstance()

        // Find UI elements by their IDs from activity_main.xml
        editTextFullName = findViewById(R.id.editTextFullName)
        editTextEmail = findViewById(R.id.editTextEmail)
        editTextPassword = findViewById(R.id.editTextPassword)
        radioGroupUserType = findViewById(R.id.radioGroupUserType)
        radioCitizen = findViewById(R.id.radioCitizen)
        radioAuthority = findViewById(R.id.radioAuthority)
        buttonRegister = findViewById(R.id.buttonRegister)
        buttonLogin = findViewById(R.id.buttonLogin)
        textViewStatus = findViewById(R.id.textViewStatus)

        // Set up click listeners for the buttons
        buttonRegister.setOnClickListener {
            registerUser() // Call the registration function
        }

        buttonLogin.setOnClickListener {
            loginUser() // Call the login function
        }

        // Check if a user is already logged in when the app starts
        val currentUser = auth.currentUser
        if (currentUser != null) {
            // If a user is logged in, try to navigate them directly to their dashboard
            // This prevents them from seeing the login/register screen every time
            navigateToDashboard(currentUser.uid)
        }
    }

    // --- User Registration Logic ---
    private fun registerUser() {
        // Get input values from UI fields
        val fullName = editTextFullName.text.toString().trim()
        val email = editTextEmail.text.toString().trim()
        val password = editTextPassword.text.toString().trim()
        // Determine user type based on selected radio button
        val userType = if (radioCitizen.isChecked) "citizen" else "authority"

        // Basic input validation
        if (fullName.isEmpty() || email.isEmpty() || password.isEmpty()) {
            textViewStatus.text = "Please fill in all fields."
            return // Stop execution if validation fails
        }
        if (password.length < 6) {
            textViewStatus.text = "Password must be at least 6 characters long."
            return
        }

        textViewStatus.text = "Registering user..." // Update status for user feedback

        // 1. Create user in Firebase Authentication
        auth.createUserWithEmailAndPassword(email, password)
            .addOnCompleteListener(this) { task ->
                if (task.isSuccessful) {
                    // User created successfully in Firebase Auth
                    val user = auth.currentUser
                    user?.let {
                        // 2. Save additional user profile data to Firestore
                        saveUserToFirestore(it.uid, fullName, email, userType)
                    }
                } else {
                    // Registration failed in Firebase Auth
                    Log.w("VigiCarAuth", "createUserWithEmailAndPassword:failure", task.exception)
                    val errorMessage = task.exception?.localizedMessage ?: "Unknown error"
                    textViewStatus.text = "Registration failed: $errorMessage"
                    Toast.makeText(baseContext, "Registration failed.", Toast.LENGTH_SHORT).show()
                }
            }
    }

    // --- Save User Profile to Firestore ---
    private fun saveUserToFirestore(uid: String, fullName: String, email: String, userType: String) {
        // Create a map of data to save to Firestore
        val userMap = hashMapOf(
            "uid" to uid,
            "fullName" to fullName,
            "email" to email,
            "userType" to userType,
            "createdAt" to Timestamp.now() // Use Firestore's server timestamp
        )

        // Add the user document to the 'users' collection with the UID as the document ID
        firestore.collection("users").document(uid).set(userMap)
            .addOnSuccessListener {
                Log.d("VigiCarAuth", "User profile saved to Firestore for $uid as $userType")
                textViewStatus.text = "Registration successful! Welcome, $fullName."
                Toast.makeText(baseContext, "Registration successful!", Toast.LENGTH_SHORT).show()
                // 3. Navigate to the appropriate dashboard after successful registration and profile save
                navigateToDashboard(uid)
            }
            .addOnFailureListener { e ->
                Log.e("VigiCarAuth", "Error saving user profile to Firestore", e)
                textViewStatus.text = "Registration successful in Auth, but failed to save profile: ${e.localizedMessage}"
                // IMPORTANT: If Firestore save fails, you might want to delete the user from Firebase Auth
                // to prevent orphaned accounts or inconsistencies.
                auth.currentUser?.delete()
                Toast.makeText(baseContext, "Error saving profile. Please try again.", Toast.LENGTH_LONG).show()
            }
    }

    // --- User Login Logic ---
    private fun loginUser() {
        // Get input values
        val email = editTextEmail.text.toString().trim()
        val password = editTextPassword.text.toString().trim()

        // Basic input validation
        if (email.isEmpty() || password.isEmpty()) {
            textViewStatus.text = "Please enter email and password."
            return
        }

        textViewStatus.text = "Logging in..."

        // Authenticate user with Email and Password
        auth.signInWithEmailAndPassword(email, password)
            .addOnCompleteListener(this) { task ->
                if (task.isSuccessful) {
                    // Login successful
                    val user = auth.currentUser
                    user?.let {
                        Log.d("VigiCarAuth", "signInWithEmailAndPassword:success for ${it.uid}")
                        textViewStatus.text = "Logged in successfully!"
                        Toast.makeText(baseContext, "Logged in!", Toast.LENGTH_SHORT).show()
                        // Navigate to the appropriate dashboard
                        navigateToDashboard(it.uid)
                    }
                } else {
                    // Login failed
                    Log.w("VigiCarAuth", "signInWithEmailAndPassword:failure", task.exception)
                    val errorMessage = task.exception?.localizedMessage ?: "Unknown error"
                    textViewStatus.text = "Login failed: $errorMessage"
                    Toast.makeText(baseContext, "Login failed.", Toast.LENGTH_SHORT).show()
                }
            }
    }

    // --- Navigation to Dashboards ---
    private fun navigateToDashboard(uid: String) {
        textViewStatus.text = "Fetching user profile..."
        // First, fetch the user's profile from Firestore to determine their userType
        firestore.collection("users").document(uid).get()
            .addOnSuccessListener { document ->
                if (document != null && document.exists()) {
                    val userType = document.getString("userType") // Get the userType
                    val fullName = document.getString("fullName") // Get the full name for welcome message
                    Log.d("VigiCarAuth", "Navigating user type: $userType")

                    // Create an Intent to start the correct dashboard activity
                    val intent = when (userType) {
                        "citizen" -> Intent(this, CitizenDashboardActivity::class.java)
                        "authority" -> Intent(this, AuthorityDashboardActivity::class.java)
                        else -> {
                            // Handle unknown user types gracefully
                            Toast.makeText(this, "Unknown user type. Please contact support.", Toast.LENGTH_LONG).show()
                            auth.signOut() // Sign out user if type is unknown
                            return@addOnSuccessListener // Exit this success listener
                        }
                    }
                    // Pass important user data to the dashboard activities
                    intent.putExtra("UID", uid)
                    intent.putExtra("FULL_NAME", fullName)
                    startActivity(intent)
                    finish() // Finish MainActivity so the user cannot go back to login/register with back button
                } else {
                    // User document not found in Firestore (should not happen if registration was successful)
                    Log.e("VigiCarAuth", "User document not found for UID: $uid")
                    textViewStatus.text = "User profile not found. Please re-register or contact support."
                    auth.signOut() // Sign out the user if their profile is missing
                }
            }
            .addOnFailureListener { e ->
                // Error fetching user document
                Log.e("VigiCarAuth", "Error fetching user document: ${e.localizedMessage}", e)
                textViewStatus.text = "Error fetching user profile. Please try again."
                auth.signOut() // Sign out on error
            }
    }
}