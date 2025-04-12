package com.example.campusparking.model

data class Violation(
    val id: Int = 0,
    val userId: Int,
    val vehicleNumber: String,
    val violationType: String, // "overspeeding", "wrong_parking", etc.
    val penaltyAmount: Double = 500.0, // Default penalty amount
    val violationDate: Long,
    val description: String = "",
    val status: String = "pending" // "pending", "paid", "disputed"
)

