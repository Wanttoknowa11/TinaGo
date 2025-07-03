package com.gtemedia.tinago

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.google.zxing.client.android.Intents
import com.google.zxing.integration.android.IntentIntegrator

class QrScannerActivity : AppCompatActivity() {

    private val CAMERA_PERMISSION_REQUEST_CODE = 101

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // No layout needed for this activity as it's just a wrapper for the scanner

        // Check for camera permission first
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
            startQrScanner()
        } else {
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.CAMERA), CAMERA_PERMISSION_REQUEST_CODE)
        }
    }

    private fun startQrScanner() {
        val integrator = IntentIntegrator(this)
        integrator.setDesiredBarcodeFormats(IntentIntegrator.QR_CODE)
        integrator.setPrompt("Scan a vehicle QR code")
        integrator.setCameraId(0)  // Use a specific camera ID.
        integrator.setBeepEnabled(true)
        integrator.setBarcodeImageEnabled(true) // Set to true to get scanned image data
        integrator.initiateScan()
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        val result = IntentIntegrator.parseActivityResult(requestCode, resultCode, data)
        if (result != null) {
            if (result.contents == null) {
                // If scan was cancelled or failed
                val originalIntent = Intent()
                setResult(RESULT_CANCELED, originalIntent)
                Toast.makeText(this, "Scan Cancelled", Toast.LENGTH_LONG).show()
            } else {
                // Scan successful
                val scannedData = result.contents
                val originalIntent = Intent()
                originalIntent.putExtra("SCANNED_DATA", scannedData)
                setResult(RESULT_OK, originalIntent)
                Toast.makeText(this, "Scanned: " + scannedData, Toast.LENGTH_LONG).show()
            }
        } else {
            super.onActivityResult(requestCode, resultCode, data)
        }
        finish() // Close the scanner activity
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == CAMERA_PERMISSION_REQUEST_CODE) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                startQrScanner() // Permission granted, start scanner
            } else {
                Toast.makeText(this, "Camera permission is required to scan QR codes.", Toast.LENGTH_LONG).show()
                setResult(RESULT_CANCELED) // Indicate permission not granted
                finish() // Close activity
            }
        }
    }
}