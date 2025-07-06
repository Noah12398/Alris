package com.example.alris.authority

import com.example.alris.model.Report
import retrofit2.Response
import retrofit2.http.GET

interface ReportService {
    @GET("reports")
    suspend fun getReports(): Response<List<Report>>
}
