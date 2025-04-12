package com.example.campusparking.model

data class GuestPass(
    val id: Int = 0,
    val vehicleNumber: String,
    val vehicleType: String,
    val slotId: Int,
    val issueTime: Long = System.currentTimeMillis(),
    val expiryTime: Long = System.currentTimeMillis() + 24 * 60 * 60 * 1000, // 24 hours validity
    val contactPhone: String = "",
    val contactEmail: String = "",
    val passCode: String, // Unique code for the pass
    val isActive: Boolean = true
)

