package com.gtemedia.tinago

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.messaging.FirebaseMessaging
import java.util.UUID

class RegisterActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseFirestore
    private val tag = "RegisterActivity"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_register)

        // Initialize Firebase Auth and Firestore
        auth = FirebaseAuth.getInstance()
        db = FirebaseFirestore.getInstance()

        // Get references to UI elements
        val editTextFullName = findViewById<EditText>(R.id.editTextFullName)
        val editTextEmail = findViewById<EditText>(R.id.editTextEmail)
        val editTextPassword = findViewById<EditText>(R.id.editTextPassword)
        val editTextConfirmPassword = findViewById<EditText>(R.id.editTextConfirmPassword)
        val buttonRegister = findViewById<Button>(R.id.buttonRegister)
        val backToLoginLink = findViewById<TextView>(R.id.backToLoginLink)

        // Set up register button click listener
        buttonRegister.setOnClickListener {
            val fullName = editTextFullName.text.toString().trim()
            val email = editTextEmail.text.toString().trim()
            val password = editTextPassword.text.toString().trim()
            val confirmPassword = editTextConfirmPassword.text.toString().trim()

            // Basic input validation
            if (fullName.isEmpty() || email.isEmpty() || password.isEmpty() || confirmPassword.isEmpty()) {
                Toast.makeText(this, "Please fill in all fields.", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (password.length < 6) {
                Toast.makeText(this, "Password must be at least 6 characters long.", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (password != confirmPassword) {
                Toast.makeText(this, "Passwords do not match.", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            // Create user with Firebase Authentication
            auth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener(this) { task ->
                    if (task.isSuccessful) {
                        // User registration success
                        Log.d(tag, "createUserWithEmailAndPassword:success")
                        val user = auth.currentUser
                        user?.let { firebaseUser ->
                            // Save user data to Firestore
                            saveUserDataToFirestore(firebaseUser.uid, fullName, email)
                        }
                    } else {
                        // If sign up fails, display a message to the user.
                        Log.w(tag, "createUserWithEmailAndPassword:failure", task.exception)
                        Toast.makeText(baseContext, "Registration failed: ${task.exception?.message}",
                            Toast.LENGTH_SHORT).show()
                    }
                }
        }

        // Set up back to login link click listener
        backToLoginLink.setOnClickListener {
            finish() // Close RegisterActivity and return to previous (LoginActivity)
        }
    }

    /**
     * Saves user data to Firestore after successful Firebase Authentication registration.
     * @param uid The Firebase User ID.
     * @param fullName The full name of the user.
     * @param email The email of the user.
     */
    private fun saveUserDataToFirestore(uid: String, fullName: String, email: String) {
        // Default user type to "citizen" for new registrations
        val user = hashMapOf(
            "fullName" to fullName,
            "email" to email,
            "userType" to "citizen", // Default to citizen
            "phoneNumber" to "", // Can be updated later in profile
            "address" to "" // Can be updated later in profile
        )

        db.collection("users").document(uid)
            .set(user)
            .addOnSuccessListener {
                Log.d(tag, "User data successfully written to Firestore!")
                Toast.makeText(this, "Registration successful!", Toast.LENGTH_SHORT).show()

                // Save FCM token for push notifications
                FirebaseMessaging.getInstance().token.addOnCompleteListener { task ->
                    if (task.isSuccessful) {
                        val token = task.result
                        if (token != null) {
                            db.collection("users").document(uid)
                                .update("fcmToken", token)
                                .addOnSuccessListener {
                                    Log.d(tag, "FCM token saved successfully during registration.")
                                }
                                .addOnFailureListener { e ->
                                    Log.w(tag, "Failed to save FCM token during registration.", e)
                                }
                        }
                    } else {
                        Log.w(tag, "Fetching FCM token failed during registration.", task.exception)
                    }
                }

                // Redirect to Citizen Dashboard after successful registration and data saving
                val intent = Intent(this, CitizenDashboardActivity::class.java)
                startActivity(intent)
                finish() // Close RegisterActivity
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, "Failed to save user data: ${e.message}", Toast.LENGTH_LONG).show()
                Log.w(tag, "Error writing user document", e)
                // Optionally, delete the Firebase Auth user if Firestore save fails
                auth.currentUser?.delete()
            }
    }
}
