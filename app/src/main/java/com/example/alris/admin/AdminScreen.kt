package com.example.alris.admin
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ExitToApp
import androidx.compose.material3.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.example.alris.Constants.logoutAndGoToLogin
import com.example.alris.ui.theme.AlrisTheme
import com.google.firebase.ktx.Firebase
import com.google.firebase.firestore.ktx.firestore

class AdminActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            AlrisTheme {
                AdminScreen()
            }
        }
    }
}
@Composable
fun AdminScreen() {
    val context = LocalContext.current
    var selectedTab by remember { mutableStateOf(AdminTab.Requests) }

    Scaffold(
        bottomBar = {
            BottomNavigationBar(
                selectedTab = selectedTab,
                onTabSelected = { selectedTab = it }
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { logoutAndGoToLogin(context) },
                containerColor = MaterialTheme.colorScheme.error,
                contentColor = MaterialTheme.colorScheme.onError
            ) {
                Icon(Icons.Default.ExitToApp, contentDescription = "Logout")
            }
        },
        floatingActionButtonPosition = FabPosition.End
    ) { innerPadding ->
        Box(modifier = Modifier
            .fillMaxSize()
            .padding(innerPadding)) {
            when (selectedTab) {
                AdminTab.Requests -> RequestsScreen()
                AdminTab.Login -> LoginScreen()
                AdminTab.Analytics -> AnalyticsScreen()
            }
        }
    }
}

enum class AdminTab(val title: String) {
    Requests("Requests"),
    Login("Login"),
    Analytics("Analytics")
}
@Composable
fun BottomNavigationBar(
    selectedTab: AdminTab,
    onTabSelected: (AdminTab) -> Unit
) {
    NavigationBar {
        AdminTab.values().forEach { tab ->
            NavigationBarItem(
                selected = selectedTab == tab,
                onClick = { onTabSelected(tab) },
                label = { Text(tab.title) },
                icon = {} // Optional: Add icons if needed
            )
        }
    }
}
@Composable
fun RequestsScreen() {
    val db = Firebase.firestore
    val pendingRequests = remember { mutableStateListOf<ApprovalRequest>() }

    LaunchedEffect(Unit) {
        db.collection("approval_requests")
            .whereEqualTo("status", "pending")
            .get()
            .addOnSuccessListener { result ->
                for (document in result) {
                    val data = document.toObject(ApprovalRequest::class.java).copy(
                        requestId = document.id
                    )
                    pendingRequests.add(data)
                }
            }
    }

    LazyColumn {
        items(pendingRequests) { request ->
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                Text("Email: ${request.email}")
                Text("Role: ${request.role}")
                Spacer(modifier = Modifier.height(8.dp))

                Button(onClick = {
                    approveUser(request.uid, request.requestId)
                    pendingRequests.remove(request)
                }) {
                    Text("Approve")
                }

                Divider(modifier = Modifier.padding(vertical = 8.dp))
            }
        }
    }
}
@Composable
fun LoginScreen() {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Text("Login Screen Placeholder")
    }
}

@Composable
fun AnalyticsScreen() {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Text("Analytics Report Placeholder")
    }
}


data class ApprovalRequest(
    val uid: String = "",
    val email: String = "",
    val role: String = "",
    val status: String = "",
    val requestId: String = "" // <-- needed to update
)

fun approveUser(uid: String, requestId: String) {
    val db = Firebase.firestore
    db.collection("users").document(uid).update("status", "approved")
    db.collection("approval_requests").document(requestId).update("status", "approved")
}
