package com.example.alris.authority

import android.content.Context
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.example.alris.Constants.logoutAndGoToLogin
import com.example.alris.model.Report

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReportsScreen() {
    val context = LocalContext.current

    var reports by remember { mutableStateOf<List<Report>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    // Load reports when screen launches
    LaunchedEffect(Unit) {
        try {
            val response = ApiClient.reportService.getReports()
            if (response.isSuccessful) {
                reports = response.body() ?: emptyList()
            } else {
                errorMessage = "Error loading reports: ${response.code()}"
            }
        } catch (e: Exception) {
            errorMessage = "Error: ${e.localizedMessage}"
        } finally {
            isLoading = false
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Authority Dashboard") },
                actions = {
                    TextButton(onClick = { logoutAndGoToLogin(context) }) {
                        Text("Logout")
                    }
                }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .padding(16.dp)
        ) {
            when {
                isLoading -> {
                    CircularProgressIndicator()
                }

                errorMessage != null -> {
                    Text("❌ $errorMessage", color = MaterialTheme.colorScheme.error)
                }

                else -> {
                    LazyColumn {
                        items(reports) { report ->
                            ReportCard(report)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ReportCard(report: Report) {
    var isDone by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        elevation = CardDefaults.cardElevation()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(report.classification.uppercase(), style = MaterialTheme.typography.titleMedium)
            Spacer(modifier = Modifier.height(4.dp))
            Text(report.description, style = MaterialTheme.typography.bodyMedium)
            Spacer(modifier = Modifier.height(8.dp))

            if (report.public_url.isNotBlank()) {
                AsyncImage(
                    model = report.public_url,
                    contentDescription = "Uploaded image",
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp)
                )
                Spacer(modifier = Modifier.height(8.dp))
            }

            Text("Lat: ${report.latitude}, Long: ${report.longitude}", style = MaterialTheme.typography.bodySmall)
            Spacer(modifier = Modifier.height(4.dp))
            Text("Spam: ${report.is_spam}, Fake: ${report.is_fake}", style = MaterialTheme.typography.bodySmall)

            Spacer(modifier = Modifier.height(8.dp))
            Button(
                onClick = { isDone = true },
                enabled = !isDone
            ) {
                Text(if (isDone) "Marked Done" else "Mark as Done")
            }
        }
    }
}
