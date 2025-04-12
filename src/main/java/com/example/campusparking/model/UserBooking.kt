package com.example.campusparking.model

data class UserBooking(
    val id: Int = 0,
    val userId: Int,
    val slotId: Int,
    val bookingDate: Long,
    val entryTime: Long = 0,
    val exitTime: Long = 0,
    val expectedEntryTime: Long = 0, // Added expected entry time
    val expectedExitTime: Long = 0, // Added expected exit time
    val status: String,
    val vehicleType: String = "2-wheeler", // "2-wheeler" or "4-wheeler"
    val duration: Long = 0 // Duration in minutes
)

