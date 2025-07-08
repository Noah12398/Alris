package com.example.alris.authority.request

// ApiService.kt
import retrofit2.Call
import retrofit2.http.Body
import retrofit2.http.POST

interface ApiService {
    @POST("/request-approval")
    fun requestApproval(@Body request: ApprovalRequest): Call<ApiResponse>
}
