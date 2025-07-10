package com.gtemedia.tinago

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Color
import android.os.Bundle
import android.util.Log
import android.view.View
import android.view.inputmethod.InputMethodManager
import android.widget.ArrayAdapter
import android.widget.AutoCompleteTextView
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.zxing.BarcodeFormat
import com.google.zxing.qrcode.QRCodeWriter
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class RegisterVehicleActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseFirestore

    private lateinit var editTextLicensePlate: EditText
    private lateinit var editTextVIN: EditText
    private lateinit var editTextMake: EditText
    private lateinit var editTextModel: EditText
    private lateinit var autoCompleteTextViewType: AutoCompleteTextView
    private lateinit var buttonRegisterVehicle: Button
    private lateinit var qrCodeImageView: ImageView
    private lateinit var qrCodeText: TextView

    private val TAG = "RegisterVehicleActivity"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_register_vehicle)

        // Initialize Firebase
        auth = FirebaseAuth.getInstance()
        db = FirebaseFirestore.getInstance()

        // Initialize UI elements
        editTextLicensePlate = findViewById(R.id.editTextLicensePlate)
        editTextVIN = findViewById(R.id.editTextVIN)
        editTextMake = findViewById(R.id.editTextMake)
        editTextModel = findViewById(R.id.editTextModel)
        autoCompleteTextViewType = findViewById(R.id.autoCompleteTextViewType)
        buttonRegisterVehicle = findViewById(R.id.buttonRegisterVehicle)
        qrCodeImageView = findViewById(R.id.qrCodeImageView)
        qrCodeText = findViewById(R.id.qrCodeText)

        // Setup vehicle type dropdown
        val vehicleTypes = arrayOf("Motorbike", "Tricycle", "Car", "Truck", "Bus", "Other")
        val adapter = ArrayAdapter(this, android.R.layout.simple_dropdown_item_1line, vehicleTypes)
        autoCompleteTextViewType.setAdapter(adapter)

        buttonRegisterVehicle.setOnClickListener {
            registerVehicle()
        }
    }

    /**
     * Handles the vehicle registration process.
     * Collects data from input fields, validates it, and saves it to Firestore.
     */
    private fun registerVehicle() {
        val licensePlate = editTextLicensePlate.text.toString().trim().uppercase(Locale.getDefault())
        val vin = editTextVIN.text.toString().trim().uppercase(Locale.getDefault())
        val make = editTextMake.text.toString().trim()
        val model = editTextModel.text.toString().trim()
        val type = autoCompleteTextViewType.text.toString().trim()

        // Basic validation
        if (licensePlate.isEmpty() || vin.isEmpty() || make.isEmpty() || model.isEmpty() || type.isEmpty()) {
            Toast.makeText(this, "Please fill in all vehicle details.", Toast.LENGTH_SHORT).show()
            return
        }

        // Get current user's UID
        val userId = auth.currentUser?.uid
        if (userId == null) {
            Toast.makeText(this, "User not logged in.", Toast.LENGTH_SHORT).show()
            return
        }

        // Hide keyboard
        hideKeyboard()

        // Create a new vehicle object
        val vehicle = hashMapOf(
            "ownerId" to userId,
            "licensePlate" to licensePlate,
            "vin" to vin,
            "make" to make,
            "model" to model,
            "type" to type,
            "registrationDate" to Date(), // Current timestamp
            "currentStatus" to "registered", // Initial status
            "imageUrl" to "", // Placeholder, implement image upload separately
            "qrCodeUrl" to "" // Will be updated after QR code generation
        )

        // Add vehicle to Firestore
        db.collection("vehicles")
            .add(vehicle)
            .addOnSuccessListener { documentReference ->
                Log.d(TAG, "Vehicle added with ID: ${documentReference.id}")
                Toast.makeText(this, "Vehicle registered successfully!", Toast.LENGTH_SHORT).show()

                // Generate QR code with the document ID and update the vehicle document
                val qrData = documentReference.id // Use the Firestore document ID as QR data
                val qrBitmap = generateQRCode(qrData)
                qrCodeImageView.setImageBitmap(qrBitmap)
                qrCodeImageView.visibility = View.VISIBLE
                qrCodeText.text = "Scan this QR code to view vehicle details:\n${documentReference.id}"
                qrCodeText.visibility = View.VISIBLE

                // In a real app, you would upload the QR code bitmap to Firebase Storage
                // and then update the 'qrCodeUrl' field in the Firestore document.
                // For this example, we'll just display it.

                // Optionally, clear fields after successful registration
                editTextLicensePlate.text.clear()
                editTextVIN.text.clear()
                editTextMake.text.clear()
                editTextModel.text.clear()
                autoCompleteTextViewType.text.clear()
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, "Error registering vehicle: ${e.localizedMessage}", Toast.LENGTH_LONG).show()
                Log.e(TAG, "Error adding vehicle document", e)
            }
    }

    /**
     * Generates a QR code bitmap from the given text data.
     * @param text The data to encode in the QR code.
     * @return A Bitmap representation of the QR code.
     */
    private fun generateQRCode(text: String): Bitmap? {
        val writer = QRCodeWriter()
        return try {
            val bitMatrix = writer.encode(text, BarcodeFormat.QR_CODE, 512, 512)
            val width = bitMatrix.width
            val height = bitMatrix.height
            val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.RGB_565)
            for (x in 0 until width) {
                for (y in 0 until height) {
                    bitmap.setPixel(x, y, if (bitMatrix.get(x, y)) Color.BLACK else Color.WHITE)
                }
            }
            bitmap
        } catch (e: Exception) {
            Log.e(TAG, "Error generating QR code: ${e.localizedMessage}", e)
            null
        }
    }

    /**
     * Hides the software keyboard.
     */
    private fun hideKeyboard() {
        val imm = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        var view = currentFocus
        if (view == null) {
            view = View(this)
        }
        imm.hideSoftInputFromWindow(view.windowToken, 0)
    }
}
