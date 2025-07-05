package com.example.alris.authority

import android.content.Context
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.example.alris.Constants.logoutAndGoToLogin

data class Report(val id: String, val title: String, val description: String, var isDone: Boolean)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReportsScreen() {
    val context = LocalContext.current

    var reports by remember {
        mutableStateOf(
            listOf(
                Report("1", "Pothole on Main Street", "Large pothole near traffic signal.", false),
                Report("2", "Streetlight Out", "No light near park road.", false)
            )
        )
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
        LazyColumn(modifier = Modifier
            .padding(innerPadding)
            .padding(16.dp)) {
            items(reports.size) { index ->
                val report = reports[index]
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp),
                    elevation = CardDefaults.cardElevation()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(report.title, style = MaterialTheme.typography.titleMedium)
                        Text(report.description, style = MaterialTheme.typography.bodyMedium)
                        Spacer(modifier = Modifier.height(8.dp))
                        Button(
                            onClick = {
                                reports = reports.toMutableList().apply {
                                    this[index] = this[index].copy(isDone = true)
                                }
                            },
                            enabled = !report.isDone
                        ) {
                            Text(if (report.isDone) "Marked Done" else "Mark as Done")
                        }
                    }
                }
            }
        }
    }
}
