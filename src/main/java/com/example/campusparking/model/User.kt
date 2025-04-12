package com.example.campusparking.model

data class User(
    val id: Int = 0,
    val username: String,
    val password: String,
    val role: String,
    val name: String,
    val email: String,
    val salary: Double = 0.0 // Added for salary deductions
)

