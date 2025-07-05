package com.example.alris.admin
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.material3.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
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
    val db = Firebase.firestore
    val pendingRequests = remember { mutableStateListOf<ApprovalRequest>() }

    // Fetch pending requests only once
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
            Column(modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)) {

                Text("Email: ${request.email}")
                Text("Role: ${request.role}")
                Spacer(modifier = Modifier.height(8.dp))

                Button(onClick = {
                    approveUser(request.uid, request.requestId)
                    pendingRequests.remove(request) // Optimistic UI update
                }) {
                    Text("Approve")
                }

                Divider(modifier = Modifier.padding(vertical = 8.dp))
            }
        }
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
    db.collection("users").document(uid).update("approved", true)
    db.collection("approval_requests").document(requestId).update("status", "approved")
}
