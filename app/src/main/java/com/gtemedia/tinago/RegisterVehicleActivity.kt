package com.gtemedia.tinago

import android.content.Context
import android.graphics.Bitmap
import android.os.Bundle
import android.util.Log
import android.view.View
import android.view.inputmethod.InputMethodManager
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.zxing.BarcodeFormat
import com.journeyapps.barcodescanner.BarcodeEncoder

class RegisterVehicleActivity : AppCompatActivity() {

    private lateinit var firestore: FirebaseFirestore
    private lateinit var auth: FirebaseAuth
    private lateinit var licensePlateEditText: EditText
    private lateinit var makeEditText: EditText
    private lateinit var modelEditText: EditText
    private lateinit var registerVehicleButton: Button
    private lateinit var qrCodeImageView: ImageView
    private lateinit var qrCodeText: TextView
    private lateinit var progressBar: ProgressBar

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_register_vehicle)

        firestore = FirebaseFirestore.getInstance()
        auth = FirebaseAuth.getInstance()

        licensePlateEditText = findViewById(R.id.licensePlateEditText)
        makeEditText = findViewById(R.id.makeEditText)
        modelEditText = findViewById(R.id.modelEditText)
        registerVehicleButton = findViewById(R.id.registerVehicleButton)
        qrCodeImageView = findViewById(R.id.qrCodeImageView)
        qrCodeText = findViewById(R.id.qrCodeText)
        progressBar = findViewById(R.id.progressBar)

        registerVehicleButton.setOnClickListener {
            registerVehicle()
        }
    }

    private fun registerVehicle() {
        val licensePlate = licensePlateEditText.text.toString().trim().uppercase() // Ensure uppercase
        val make = makeEditText.text.toString().trim()
        val model = modelEditText.text.toString().trim()
        val ownerUid = auth.currentUser?.uid

        if (licensePlate.isEmpty() || make.isEmpty() || model.isEmpty()) {
            Toast.makeText(this, "Please fill all fields.", Toast.LENGTH_SHORT).show()
            return
        }

        if (ownerUid == null) {
            Toast.makeText(this, "User not logged in.", Toast.LENGTH_SHORT).show()
            return
        }

        progressBar.visibility = View.VISIBLE
        hideKeyboard()

        // Check if license plate already exists
        firestore.collection("vehicles").document(licensePlate).get()
            .addOnSuccessListener { documentSnapshot ->
                if (documentSnapshot.exists()) {
                    Toast.makeText(this, "Vehicle with this license plate already registered.", Toast.LENGTH_LONG).show()
                    progressBar.visibility = View.GONE
                } else {
                    // Proceed with registration
                    val vehicleData = hashMapOf(
                        "make" to make,
                        "model" to model,
                        "ownerUid" to ownerUid,
                        "status" to "registered" // Initial status
                    )

                    firestore.collection("vehicles").document(licensePlate).set(vehicleData)
                        .addOnSuccessListener {
                            Toast.makeText(this, "Vehicle registered successfully!", Toast.LENGTH_SHORT).show()
                            generateAndDisplayQrCode(licensePlate)
                            progressBar.visibility = View.GONE
                            clearInputFields()
                        }
                        .addOnFailureListener { e ->
                            Toast.makeText(this, "Error registering vehicle: ${e.message}", Toast.LENGTH_LONG).show()
                            Log.e("RegisterVehicle", "Error registering vehicle: ${e.message}", e)
                            progressBar.visibility = View.GONE
                        }
                }
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, "Error checking license plate: ${e.message}", Toast.LENGTH_LONG).show()
                Log.e("RegisterVehicle", "Error checking license plate existence: ${e.message}", e)
                progressBar.visibility = View.GONE
            }
    }

    private fun generateAndDisplayQrCode(data: String) {
        try {
            val barcodeEncoder = BarcodeEncoder()
            val bitmap: Bitmap = barcodeEncoder.encodeBitmap(data, BarcodeFormat.QR_CODE, 400, 400)
            qrCodeImageView.setImageBitmap(bitmap)
            qrCodeImageView.visibility = View.VISIBLE
            qrCodeText.text = "Scan this QR code for $data"
            qrCodeText.visibility = View.VISIBLE
        } catch (e: Exception) {
            Toast.makeText(this, "Error generating QR code: ${e.message}", Toast.LENGTH_LONG).show()
            Log.e("RegisterVehicle", "Error generating QR code: ${e.message}", e)
        }
    }

    private fun clearInputFields() {
        licensePlateEditText.text.clear()
        makeEditText.text.clear()
        modelEditText.text.clear()
    }

    private fun hideKeyboard() {
        val imm = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        var view = currentFocus
        if (view == null) {
            view = View(this)
        }
        imm.hideSoftInputFromWindow(view.windowToken, 0)
    }
}