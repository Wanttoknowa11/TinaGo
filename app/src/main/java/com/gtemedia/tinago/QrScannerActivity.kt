package com.gtemedia.tinago

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.google.firebase.firestore.FirebaseFirestore
import com.journeyapps.barcodescanner.BarcodeCallback
import com.journeyapps.barcodescanner.BarcodeResult
import com.journeyapps.barcodescanner.BarcodeView // Corrected import

class QrScannerActivity : AppCompatActivity() {

    private lateinit var barcodeView: BarcodeView
    private lateinit var firestore: FirebaseFirestore
    private var isTorchActive: Boolean = false // Track torch state locally

    private val CAMERA_PERMISSION_REQUEST_CODE = 100
    private val TAG = "QrScannerActivity"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_qr_scanner) // Use the simple QR scanner layout initially

        // Initialize Firestore
        firestore = FirebaseFirestore.getInstance()

        // Check for camera permission at runtime
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
            // Permission already granted, proceed with scanner setup
            setupScanner()
        } else {
            // Request camera permission
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.CAMERA), CAMERA_PERMISSION_REQUEST_CODE)
        }
    }

    /**
     * Sets up the barcode scanner after camera permission is granted.
     */
    private fun setupScanner() {
        // Switch to the custom barcode scanner layout
        setContentView(R.layout.custom_barcode_scanner_layout)

        barcodeView = findViewById<BarcodeView>(R.id.zxing_barcode_surface)
        val flashButton = findViewById<Button>(R.id.buttonFlash)

        // Set up barcode decoder callbacks
        barcodeView.decodeContinuous(object : BarcodeCallback {
            override fun barcodeResult(result: BarcodeResult) {
                // Handle the scanned QR code result
                result.text?.let { scannedCode ->
                    barcodeView.pause() // Pause scanning to prevent multiple scans
                    Log.d(TAG, "Scanned QR Code: $scannedCode")
                    Toast.makeText(this@QrScannerActivity, "Scanned: $scannedCode", Toast.LENGTH_LONG).show()

                    // Fetch vehicle details based on the scanned QR code (assuming it's a license plate)
                    fetchVehicleDetails(scannedCode)
                }
            }
            override fun possibleResultPoints(resultPoints: List<com.google.zxing.ResultPoint>?) {
                // Optional: Handle visual feedback for possible result points
            }
        })

        // Set up flash button listener
        flashButton.setOnClickListener {
            if (isTorchActive) { // Use the local state to check if torch is on
                barcodeView.setTorch(false) // Turn torch off
                isTorchActive = false // Update local state
                flashButton.text = "Flash On"
            } else {
                barcodeView.setTorch(true) // Turn torch on
                isTorchActive = true // Update local state
                flashButton.text = "Flash Off"
            }
        }
    }

    /**
     * Fetches vehicle details from Firestore based on the scanned license plate.
     * @param licensePlate The license plate extracted from the QR code.
     */
    private fun fetchVehicleDetails(licensePlate: String) {
        firestore.collection("vehicles")
            .whereEqualTo("licensePlate", licensePlate)
            .get()
            .addOnSuccessListener { documents ->
                if (!documents.isEmpty) {
                    // Assuming only one vehicle per license plate
                    val document = documents.documents[0]
                    val vehicleId = document.id
                    Log.d(TAG, "Vehicle found: $licensePlate, ID: $vehicleId")

                    // Navigate to VehicleDetailsActivity with the vehicle ID
                    val intent = Intent(this, VehicleDetailsActivity::class.java).apply {
                        putExtra("vehicleId", vehicleId)
                        putExtra("licensePlate", licensePlate) // Pass license plate for display
                    }
                    startActivity(intent)
                    finish() // Close scanner activity
                } else {
                    Log.d(TAG, "No vehicle found for license plate: $licensePlate")
                    Toast.makeText(this, "No vehicle found with this QR code.", Toast.LENGTH_LONG).show()
                    barcodeView.resume() // Resume scanning if no vehicle found
                }
            }
            .addOnFailureListener { e ->
                Log.e(TAG, "Error fetching vehicle details: ${e.localizedMessage}", e)
                Toast.makeText(this, "Error fetching vehicle details: ${e.localizedMessage}", Toast.LENGTH_LONG).show()
                barcodeView.resume() // Resume scanning on error
            }
    }

    override fun onResume() {
        super.onResume()
        barcodeView.resume() // Resume scanning when activity is resumed
        // Ensure torch state is reset when resuming, or re-apply if needed
        if (isTorchActive) {
            barcodeView.setTorch(true)
        }
    }

    override fun onPause() {
        super.onPause()
        barcodeView.pause() // Pause scanning when activity is paused
        // Turn off torch when pausing to save battery
        if (isTorchActive) {
            barcodeView.setTorch(false)
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == CAMERA_PERMISSION_REQUEST_CODE) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                setupScanner() // Permission granted, start scanner
            } else {
                Toast.makeText(this, "Camera permission is required to scan QR codes.", Toast.LENGTH_LONG).show()
                setResult(RESULT_CANCELED) // Indicate permission not granted
                finish() // Close activity
            }
        }
    }
}
