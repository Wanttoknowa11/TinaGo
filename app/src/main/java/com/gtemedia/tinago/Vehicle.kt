package com.gtemedia.tinago

import java.util.Date

/**
 * Data class representing a Vehicle in the TinaGo application.
 * This class is used for mapping vehicle documents from Firestore.
 */
data class Vehicle(
    val id: String = "", // Document ID from Firestore
    val ownerId: String = "",
    val licensePlate: String = "",
    val vin: String = "",
    val make: String = "",
    val model: String = "",
    val type: String = "",
    val registrationDate: Date? = null,
    var currentStatus: String = "registered", // e.g., "registered", "stolen", "recovered"
    val imageUrl: String? = null, // URL to vehicle image in Firebase Storage
    val qrCodeUrl: String? = null, // URL to generated QR code image in Firebase Storage
    // Fields for theft report, might be null if the vehicle is not stolen or recovered
    val theftDate: Date? = null,
    val theftLocation: String? = null,
    val description: String? = null,
    val reportDate: Date? = null // Date when the theft report was filed
) {
    // No-argument constructor required for Firestore deserialization
    constructor() : this("", "", "", "", "", "", "", null, "registered", null, null, null, null, null, null)
}
