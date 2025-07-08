package com.example.alris.authority.request

data class ApprovalRequest(
    val uid: String,
    val name: String,
    val email: String,
    val department: String,
)

// ApiResponse.kt
data class ApiResponse(
    val message: String
)
