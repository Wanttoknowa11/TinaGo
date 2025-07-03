package com.gtemedia.tinago

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import com.bumptech.glide.Glide
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.StorageReference

class RegisterActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth
    private lateinit var firestore: FirebaseFirestore
    private lateinit var storageRef: StorageReference

    private lateinit var fullNameEditText: EditText
    private lateinit var emailEditText: EditText
    private lateinit var passwordEditText: EditText
    private lateinit var confirmPasswordEditText: EditText
    private lateinit var registerButton: Button
    private lateinit var backToLoginLink: TextView
    private lateinit var profileImageView: ImageView
    private lateinit var selectImageButton: Button

    private var imageUri: Uri? = null

    companion object {
        private const val PICK_IMAGE_REQUEST = 1
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_register)

        auth = FirebaseAuth.getInstance()
        firestore = FirebaseFirestore.getInstance()
        storageRef = FirebaseStorage.getInstance().reference.child("profile_images")

        // Views
        fullNameEditText = findViewById(R.id.fullNameEditText)
        emailEditText = findViewById(R.id.emailEditText)
        passwordEditText = findViewById(R.id.passwordEditText)
        confirmPasswordEditText = findViewById(R.id.confirmPasswordEditText)
        registerButton = findViewById(R.id.registerButton)
        backToLoginLink = findViewById(R.id.backToLoginLink)
        profileImageView = findViewById(R.id.profileImageView)
        selectImageButton = findViewById(R.id.selectImageButton)

        // Image selection
        selectImageButton.setOnClickListener {
            openImagePicker()
        }

        // Register logic
        registerButton.setOnClickListener {
            performRegistration()
        }

        backToLoginLink.setOnClickListener {
            finish()
        }
    }

    @SuppressLint("IntentReset")
    private fun openImagePicker() {
        val intent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
        intent.type = "image/*"
        startActivityForResult(intent, PICK_IMAGE_REQUEST)
    }

    @Deprecated("This method has been deprecated in favor of using the Activity Result API\n      which brings increased type safety via an {@link ActivityResultContract} and the prebuilt\n      contracts for common intents available in\n      {@link androidx.activity.result.contract.ActivityResultContracts}, provides hooks for\n      testing, and allow receiving results in separate, testable classes independent from your\n      activity. Use\n      {@link #registerForActivityResult(ActivityResultContract, ActivityResultCallback)}\n      with the appropriate {@link ActivityResultContract} and handling the result in the\n      {@link ActivityResultCallback#onActivityResult(Object) callback}.")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == PICK_IMAGE_REQUEST && resultCode == Activity.RESULT_OK && data != null && data.data != null) {
            imageUri = data.data
            Glide.with(this).load(imageUri).into(profileImageView)
        }
    }

    private fun performRegistration() {
        val fullName = fullNameEditText.text.toString().trim()
        val email = emailEditText.text.toString().trim()
        val password = passwordEditText.text.toString().trim()
        val confirmPassword = confirmPasswordEditText.text.toString().trim()

        if (fullName.isEmpty() || email.isEmpty() || password.isEmpty() || confirmPassword.isEmpty()) {
            Toast.makeText(this, "All fields are required.", Toast.LENGTH_SHORT).show()
            return
        }

        if (password != confirmPassword) {
            Toast.makeText(this, "Passwords do not match.", Toast.LENGTH_SHORT).show()
            return
        }

        if (password.length < 6) {
            Toast.makeText(this, "Password must be at least 6 characters.", Toast.LENGTH_SHORT).show()
            return
        }

        auth.createUserWithEmailAndPassword(email, password)
            .addOnCompleteListener(this) { task ->
                if (task.isSuccessful) {
                    val user = auth.currentUser
                    user?.let {
                        if (imageUri != null) {
                            uploadProfileImage(it.uid, fullName, email)
                        } else {
                            saveUserToFirestore(it.uid, fullName, email, null)
                        }
                    }
                } else {
                    Toast.makeText(this, "Registration failed: ${task.exception?.message}", Toast.LENGTH_LONG).show()
                }
            }
    }

    private fun uploadProfileImage(uid: String, fullName: String, email: String) {
        val fileRef = storageRef.child("$uid.jpg")
        imageUri?.let {
            fileRef.putFile(it)
                .addOnSuccessListener {
                    fileRef.downloadUrl.addOnSuccessListener { uri ->
                        saveUserToFirestore(uid, fullName, email, uri.toString())
                    }
                }
                .addOnFailureListener { e ->
                    Toast.makeText(this, "Image upload failed: ${e.message}", Toast.LENGTH_SHORT).show()
                    saveUserToFirestore(uid, fullName, email, null) // Still save user
                }
        }
    }

    private fun saveUserToFirestore(uid: String, fullName: String, email: String, imageUrl: String?) {
        val userMap = hashMapOf(
            "fullName" to fullName,
            "email" to email,
            "role" to "citizen",
            "profileImageUrl" to imageUrl
        )

        firestore.collection("users").document(uid).set(userMap)
            .addOnSuccessListener {
                Toast.makeText(this, "Registration successful!", Toast.LENGTH_SHORT).show()
                val intent = Intent(this, CitizenDashboardActivity::class.java)
                intent.putExtra("FULL_NAME", fullName)
                startActivity(intent)
                finish()
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, "Failed to save user data: ${e.message}", Toast.LENGTH_LONG).show()
                auth.currentUser?.delete()
            }
    }
}
