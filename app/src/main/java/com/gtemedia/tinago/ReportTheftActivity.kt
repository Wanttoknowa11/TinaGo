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
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

class ReportTheftActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseFirestore

    private lateinit var textViewVehicleInfo: TextView
    private lateinit var editTextTheftLocation: EditText
    private lateinit var editTextTheftDate: EditText
    private lateinit var editTextDescription: EditText
    private lateinit var buttonReportTheft: Button
    private lateinit var buttonCancelReport: Button

    private var vehicleId: String? = null
    private var licensePlate: String? = null

    private val TAG = "ReportTheftActivity"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_report_theft)

        // Initialize Firebase
        auth = FirebaseAuth.getInstance()
        db = FirebaseFirestore.getInstance()

        // Get vehicle ID and license plate from intent
        vehicleId = intent.getStringExtra("vehicleId")
        licensePlate = intent.getStringExtra("licensePlate")

        // Initialize UI elements
        textViewVehicleInfo = findViewById(R.id.textViewVehicleInfo)
        editTextTheftLocation = findViewById(R.id.editTextTheftLocation)
        editTextTheftDate = findViewById(R.id.editTextTheftDate)
        editTextDescription = findViewById(R.id.editTextDescription)
        buttonReportTheft = findViewById(R.id.buttonReportTheft)
        buttonCancelReport = findViewById(R.id.buttonCancelReport)

        // Display vehicle info
        textViewVehicleInfo.text = "Reporting theft for: ${licensePlate ?: "N/A"}"

        // Set up DatePickerDialog for theft date
        editTextTheftDate.setOnClickListener {
            showDatePickerDialog()
        }
        editTextTheftDate.keyListener = null // Make EditText not editable by keyboard

        buttonReportTheft.setOnClickListener {
            fileTheftReport()
        }

        buttonCancelReport.setOnClickListener {
            finish() // Close activity without reporting
        }
    }

    /**
     * Displays a DatePickerDialog for selecting the theft date.
     */
    private fun showDatePickerDialog() {
        val calendar = Calendar.getInstance()
        val year = calendar.get(Calendar.YEAR)
        val month = calendar.get(Calendar.MONTH)
        val day = calendar.get(Calendar.DAY_OF_MONTH)

        val datePickerDialog = DatePickerDialog(this, { _, selectedYear, selectedMonth, selectedDay ->
            val selectedDate = Calendar.getInstance()
            selectedDate.set(selectedYear, selectedMonth, selectedDay)
            val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
            editTextTheftDate.setText(dateFormat.format(selectedDate.time))
        }, year, month, day)

        datePickerDialog.show()
    }

    /**
     * Files the theft report by updating vehicle status and adding a new theft report document.
     */
    private fun fileTheftReport() {
        val theftLocation = editTextTheftLocation.text.toString().trim()
        val theftDateString = editTextTheftDate.text.toString().trim()
        val description = editTextDescription.text.toString().trim()

        if (theftLocation.isEmpty() || theftDateString.isEmpty()) {
            Toast.makeText(this, "Please enter theft location and date.", Toast.LENGTH_SHORT).show()
            return
        }

        val theftDate: Date? = try {
            SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).parse(theftDateString)
        } catch (e: Exception) {
            Toast.makeText(this, "Invalid date format. Please use YYYY-MM-DD.", Toast.LENGTH_SHORT).show()
            return
        }

        if (vehicleId == null || auth.currentUser == null) {
            Toast.makeText(this, "Error: Vehicle or user information missing.", Toast.LENGTH_SHORT).show()
            return
        }

        val userId = auth.currentUser!!.uid

        // 1. Update vehicle status to "stolen"
        val vehicleDocRef = db.collection("vehicles").document(vehicleId!!)
        vehicleDocRef.update("currentStatus", "stolen")
            .addOnSuccessListener {
                Log.d(TAG, "Vehicle status updated to stolen for ID: $vehicleId")

                // 2. Add a new theft report document
                val theftReport = hashMapOf(
                    "vehicleId" to vehicleId,
                    "reporterId" to userId,
                    "theftDate" to theftDate,
                    "theftLocation" to theftLocation,
                    "description" to description,
                    "reportDate" to Date(), // Timestamp of when the report was filed
                    "isResolved" to false
                )

                db.collection("theft_reports")
                    .add(theftReport)
                    .addOnSuccessListener { documentReference ->
                        Log.d(TAG, "Theft report added with ID: ${documentReference.id}")
                        Toast.makeText(this, "Theft report filed successfully!", Toast.LENGTH_SHORT).show()
                        finish() // Close activity after successful report
                    }
                    .addOnFailureListener { e ->
                        Toast.makeText(this, "Error filing theft report: ${e.localizedMessage}", Toast.LENGTH_LONG).show()
                        Log.e(TAG, "Error adding theft report: ${e.localizedMessage}", e)
                        // Optionally, revert vehicle status if report saving fails
                        vehicleDocRef.update("currentStatus", "registered")
                    }
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, "Error updating vehicle status: ${e.localizedMessage}", Toast.LENGTH_LONG).show()
                Log.e(TAG, "Error updating vehicle status to 'stolen': ${e.localizedMessage}", e)
            }
    }
}
