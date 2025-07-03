package com.gtemedia.tinago

import android.app.DatePickerDialog
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FieldValue
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

class ReportTheftActivity : AppCompatActivity() {

    private lateinit var firestore: FirebaseFirestore
    private lateinit var auth: FirebaseAuth
    private lateinit var textViewLicensePlate: TextView
    private lateinit var editTextTheftDate: EditText
    private lateinit var editTextTheftLocation: EditText
    private lateinit var editTextDescription: EditText
    private lateinit var buttonFileReport: Button
    private lateinit var buttonCancelReport: Button

    private var selectedTheftDate: Calendar? = null
    private var scannedLicensePlate: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_report_theft)

        firestore = FirebaseFirestore.getInstance()
        auth = FirebaseAuth.getInstance()

        textViewLicensePlate = findViewById(R.id.textViewReportingLicensePlate)
        editTextTheftDate = findViewById(R.id.editTextTheftDate)
        editTextTheftLocation = findViewById(R.id.editTextTheftLocation)
        editTextDescription = findViewById(R.id.editTextDescription)
        buttonFileReport = findViewById(R.id.buttonFileTheftReport)
        buttonCancelReport = findViewById(R.id.buttonCancelReport)

        scannedLicensePlate = intent.getStringExtra("SCANNED_LICENSE_PLATE")

        if (scannedLicensePlate != null && scannedLicensePlate!!.isNotEmpty()) {
            textViewLicensePlate.text = getString(R.string.reporting_theft_for_license_plate, scannedLicensePlate)
        } else {
            Toast.makeText(this, "Error: No license plate provided for theft report.", Toast.LENGTH_LONG).show()
            finish() // Close activity if no license plate
            return
        }

        editTextTheftDate.setOnClickListener {
            showDatePickerDialog()
        }

        buttonFileReport.setOnClickListener {
            fileTheftReport()
        }

        buttonCancelReport.setOnClickListener {
            finish() // Go back to CitizenDashboard
        }
    }

    private fun showDatePickerDialog() {
        val calendar = Calendar.getInstance()
        val year = calendar.get(Calendar.YEAR)
        val month = calendar.get(Calendar.MONTH)
        val day = calendar.get(Calendar.DAY_OF_MONTH)

        val datePickerDialog = DatePickerDialog(this,
            { _, selectedYear, selectedMonth, selectedDayOfMonth ->
                selectedTheftDate = Calendar.getInstance().apply {
                    set(selectedYear, selectedMonth, selectedDayOfMonth)
                }
                val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
                editTextTheftDate.setText(dateFormat.format(selectedTheftDate!!.time))
            }, year, month, day)

        // Optional: Set max date to today
        datePickerDialog.datePicker.maxDate = System.currentTimeMillis()
        datePickerDialog.show()
    }

    private fun fileTheftReport() {
        val licensePlate = scannedLicensePlate
        val theftDate = editTextTheftDate.text.toString().trim()
        val theftLocation = editTextTheftLocation.text.toString().trim()
        val description = editTextDescription.text.toString().trim()
        val reporterUid = auth.currentUser?.uid

        if (licensePlate == null || theftDate.isEmpty() || theftLocation.isEmpty() || description.isEmpty() || reporterUid == null) {
            Toast.makeText(this, "Please fill all fields and ensure you are logged in.", Toast.LENGTH_LONG).show()
            return
        }

        // 1. Create a new theft report document
        val theftReportData = hashMapOf(
            "licensePlate" to licensePlate,
            "reporterUid" to reporterUid,
            "reportDate" to FieldValue.serverTimestamp(), // Use server timestamp
            "theftDate" to theftDate, // As a string
            "theftLocation" to theftLocation,
            "description" to description,
            "status" to "reported" // Initial status for a theft report
        )

        firestore.collection("theft_reports")
            .add(theftReportData)
            .addOnSuccessListener {
                Log.d("ReportTheft", "Theft report added successfully.")
                // 2. Update the vehicle's status to "reported_stolen"
                firestore.collection("vehicles").document(licensePlate)
                    .update("status", "reported_stolen")
                    .addOnSuccessListener {
                        Toast.makeText(this, "Vehicle reported stolen successfully!", Toast.LENGTH_LONG).show()
                        Log.d("ReportTheft", "Vehicle status updated to reported_stolen.")
                        finish() // Go back to CitizenDashboard
                    }
                    .addOnFailureListener { e ->
                        Toast.makeText(this, "Error updating vehicle status: ${e.localizedMessage}", Toast.LENGTH_LONG).show()
                        Log.e("ReportTheft", "Error updating vehicle status: ${e.localizedMessage}", e)
                        // Consider deleting the theft report if vehicle status update fails
                    }
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, "Error filing theft report: ${e.localizedMessage}", Toast.LENGTH_LONG).show()
                Log.e("ReportTheft", "Error adding theft report: ${e.localizedMessage}", e)
            }
    }
}