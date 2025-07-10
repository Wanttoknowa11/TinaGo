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

class LoginActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseFirestore
    private val tag = "LoginActivity"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)

        // Initialize Firebase Auth and Firestore
        auth = FirebaseAuth.getInstance()
        db = FirebaseFirestore.getInstance()

        // Get references to UI elements
        val editTextEmail = findViewById<EditText>(R.id.editTextEmail)
        val editTextPassword = findViewById<EditText>(R.id.editTextPassword)
        val buttonLogin = findViewById<Button>(R.id.buttonLogin)
        val textViewRegister = findViewById<TextView>(R.id.textViewRegister)
        val textViewLoginAsAuthority = findViewById<TextView>(R.id.textViewLoginAsAuthority)

        // Set up login button click listener
        buttonLogin.setOnClickListener {
            val email = editTextEmail.text.toString().trim()
            val password = editTextPassword.text.toString().trim()

            if (email.isEmpty() || password.isEmpty()) {
                Toast.makeText(this, "Please enter email and password.", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            // Sign in user with Firebase Authentication
            auth.signInWithEmailAndPassword(email, password)
                .addOnCompleteListener(this) { task ->
                    if (task.isSuccessful) {
                        // Login success, update UI with the signed-in user's information
                        Log.d(tag, "signInWithEmailAndPassword:success")
                        val user = auth.currentUser
                        user?.let { firebaseUser ->
                            // Fetch user's profile from Firestore to determine user type
                            db.collection("users").document(firebaseUser.uid).get()
                                .addOnSuccessListener { document ->
                                    if (document != null && document.exists()) {
                                        val userType = document.getString("userType")
                                        Toast.makeText(this, "Login Successful!", Toast.LENGTH_SHORT).show()

                                        // Save FCM token for push notifications
                                        FirebaseMessaging.getInstance().token.addOnCompleteListener { tokenTask ->
                                            if (tokenTask.isSuccessful) {
                                                val token = tokenTask.result
                                                if (token != null) {
                                                    db.collection("users").document(firebaseUser.uid)
                                                        .update("fcmToken", token)
                                                        .addOnSuccessListener {
                                                            Log.d(tag, "FCM token saved successfully.")
                                                        }
                                                        .addOnFailureListener { e ->
                                                            Log.w(tag, "Failed to save FCM token.", e)
                                                        }
                                                }
                                            } else {
                                                Log.w(tag, "Fetching FCM token failed.", tokenTask.exception)
                                            }
                                        }

                                        // Navigate based on user type
                                        if (userType == "authority") {
                                            val intent = Intent(this, AuthorityDashboardActivity::class.java)
                                            startActivity(intent)
                                        } else { // Default to citizen
                                            val intent = Intent(this, CitizenDashboardActivity::class.java)
                                            startActivity(intent)
                                        }
                                        finish() // Close LoginActivity
                                    } else {
                                        Log.w(tag, "User document not found in Firestore.")
                                        Toast.makeText(this, "User data not found. Please contact support.", Toast.LENGTH_LONG).show()
                                        auth.signOut() // Sign out if user data is missing
                                    }
                                }
                                .addOnFailureListener { e ->
                                    Log.e(tag, "Error fetching user document: ${e.localizedMessage}")
                                    Toast.makeText(this, "Error fetching user data. Please try again.", Toast.LENGTH_LONG).show()
                                    auth.signOut() // Sign out on error
                                }
                        }
                    } else {
                        // If sign in fails, display a message to the user.
                        Log.w(tag, "signInWithEmailAndPassword:failure", task.exception)
                        Toast.makeText(baseContext, "Authentication failed: ${task.exception?.message}",
                            Toast.LENGTH_SHORT).show()
                    }
                }
        }

        // Set up register link click listener
        textViewRegister.setOnClickListener {
            val intent = Intent(this, RegisterActivity::class.java)
            startActivity(intent)
        }

        // Set up login as authority link click listener
        textViewLoginAsAuthority.setOnClickListener {
            // This could directly navigate to AuthorityDashboard if a separate login is not needed,
            // or show a specific login screen for authorities.
            // For now, we'll keep the same login and differentiate by userType after successful login.
            Toast.makeText(this, "Please login with your authority credentials.", Toast.LENGTH_SHORT).show()
            // The logic to differentiate is already in the main login flow.
        }
    }

    override fun onStart() {
        super.onStart()
        // Check if user is already signed in (non-null) and update UI accordingly.
        val currentUser = auth.currentUser
        if (currentUser != null) {
            // User is already logged in, fetch their type and redirect
            db.collection("users").document(currentUser.uid).get()
                .addOnSuccessListener { document ->
                    if (document != null && document.exists()) {
                        val userType = document.getString("userType")
                        if (userType == "authority") {
                            val intent = Intent(this, AuthorityDashboardActivity::class.java)
                            startActivity(intent)
                        } else { // Default to citizen
                            val intent = Intent(this, CitizenDashboardActivity::class.java)
                            startActivity(intent)
                        }
                        finish() // Close LoginActivity
                    } else {
                        Log.w(tag, "User document not found on start. Signing out.")
                        auth.signOut()
                    }
                }
                .addOnFailureListener { e ->
                    Log.e(tag, "Error fetching user document on start: ${e.localizedMessage}")
                    auth.signOut() // Sign out on error
                }
        }
    }
}
