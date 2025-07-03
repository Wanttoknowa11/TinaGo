package com.gtemedia.tinago

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class LoginActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth
    private lateinit var firestore: FirebaseFirestore
    private lateinit var emailEditText: EditText
    private lateinit var passwordEditText: EditText
    private lateinit var loginButton: Button
    private lateinit var registerLink: TextView
    private lateinit var loginAsCitizenButton: Button
    private lateinit var loginAsAuthorityButton: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)

        auth = FirebaseAuth.getInstance()
        firestore = FirebaseFirestore.getInstance()

        emailEditText = findViewById(R.id.emailEditText)
        passwordEditText = findViewById(R.id.passwordEditText)
        loginButton = findViewById(R.id.loginButton)
        registerLink = findViewById(R.id.registerLink)
        loginAsCitizenButton = findViewById(R.id.loginAsCitizenButton)
        loginAsAuthorityButton = findViewById(R.id.loginAsAuthorityButton)

        // Check if user is already logged in
        if (auth.currentUser != null) {
            checkUserRoleAndRedirect(auth.currentUser!!.uid)
        }

        loginButton.setOnClickListener {
            performLogin()
        }

        registerLink.setOnClickListener {
            startActivity(Intent(this, RegisterActivity::class.java))
        }

        loginAsCitizenButton.setOnClickListener {
            // Placeholder for quick citizen login if needed for testing (remove in production)
            // For actual use, just performLogin() based on entered credentials.
            emailEditText.setText("citizen@example.com")
            passwordEditText.setText("password")
            performLogin()
        }

        loginAsAuthorityButton.setOnClickListener {
            // Placeholder for quick authority login if needed for testing (remove in production)
            // For actual use, just performLogin() based on entered credentials.
            emailEditText.setText("authority@example.com")
            passwordEditText.setText("password")
            performLogin()
        }
    }

    private fun performLogin() {
        val email = emailEditText.text.toString().trim()
        val password = passwordEditText.text.toString().trim()

        if (email.isEmpty() || password.isEmpty()) {
            Toast.makeText(this, "Please enter email and password.", Toast.LENGTH_SHORT).show()
            return
        }

        auth.signInWithEmailAndPassword(email, password)
            .addOnCompleteListener(this) { task ->
                if (task.isSuccessful) {
                    val user = auth.currentUser
                    user?.let {
                        checkUserRoleAndRedirect(it.uid)
                    }
                } else {
                    Toast.makeText(this, "Authentication failed: ${task.exception?.message}", Toast.LENGTH_LONG).show()
                }
            }
    }

    private fun checkUserRoleAndRedirect(uid: String) {
        firestore.collection("users").document(uid).get()
            .addOnSuccessListener { document ->
                if (document.exists()) {
                    val role = document.getString("role")
                    val fullName = document.getString("fullName") ?: "User"

                    if (role == "citizen") {
                        val intent = Intent(this, CitizenDashboardActivity::class.java)
                        intent.putExtra("FULL_NAME", fullName)
                        startActivity(intent)
                        finish()
                    } else if (role == "authority") {
                        val intent = Intent(this, AuthorityDashboardActivity::class.java)
                        intent.putExtra("FULL_NAME", fullName)
                        startActivity(intent)
                        finish()
                    } else {
                        Toast.makeText(this, "Unknown user role.", Toast.LENGTH_SHORT).show()
                        auth.signOut() // Sign out if role is unknown
                    }
                } else {
                    Toast.makeText(this, "User data not found.", Toast.LENGTH_SHORT).show()
                    auth.signOut() // Sign out if user data is missing
                }
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, "Error checking user role: ${e.localizedMessage}", Toast.LENGTH_LONG).show()
                auth.signOut() // Sign out on error
            }
    }
}