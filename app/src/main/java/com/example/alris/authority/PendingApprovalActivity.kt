package com.example.alris.authority

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Security
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.alris.ui.theme.AlrisTheme
import com.google.firebase.auth.FirebaseAuth
import org.json.JSONObject

import com.example.alris.Constants
import com.example.alris.MainActivity
import com.example.alris.authority.request.ApiResponse
import com.example.alris.authority.request.ApprovalRequest
import com.example.alris.authority.request.RetrofitClient

class PendingApprovalActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            AlrisTheme {
                ApprovalRequestScreen()
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ApprovalRequestScreen() {
    val departments = listOf(
        "Municipal Waste Management",
        "Public Works Department",
        "Road Maintenance",
        "Traffic Police",
        "Town Planning",
        "Electricity Department",
        "Drainage Board",
        "Animal Control",
        "Disaster Management",
        "Fire Department",
        "Sanitation Department",
        "Sewerage Board",
        "Road Safety",
        "Urban Development"
    )
    var name by remember { mutableStateOf("") }
    val email = FirebaseAuth.getInstance().currentUser?.email ?: ""
    val uid = FirebaseAuth.getInstance().currentUser?.uid ?: ""
    var department by remember { mutableStateOf(departments.first()) }
    var expanded by remember { mutableStateOf(false) }
    val context = LocalContext.current

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(Color(0xFF667eea), Color(0xFF764ba2), Color(0xFF6B73FF))
                )
            ),
        contentAlignment = Alignment.Center
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp)
                .shadow(20.dp, RoundedCornerShape(24.dp)),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White)
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    "Authority Access Request",
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF2D3748)
                )

                Spacer(modifier = Modifier.height(24.dp))

                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Full Name") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                )

                Spacer(modifier = Modifier.height(16.dp))

                OutlinedTextField(
                    value = email,
                    onValueChange = {},
                    label = { Text("Email") },
                    singleLine = true,
                    enabled = false,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                )

                Spacer(modifier = Modifier.height(16.dp))

                ExposedDropdownMenuBox(
                    expanded = expanded,
                    onExpandedChange = { expanded = !expanded }
                ) {
                    OutlinedTextField(
                        value = department,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Department") },
                        trailingIcon = {
                            ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded)
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .menuAnchor(), // Important to anchor the dropdown
                        shape = RoundedCornerShape(12.dp)
                    )

                    ExposedDropdownMenu(
                        expanded = expanded,
                        onDismissRequest = { expanded = false }
                    ) {
                        departments.forEach { selectionOption ->
                            DropdownMenuItem(
                                text = { Text(selectionOption) },
                                onClick = {
                                    department = selectionOption
                                    expanded = false
                                }
                            )
                        }
                    }
                }



                Spacer(modifier = Modifier.height(24.dp))

                Button(
                    onClick = {
                        if (name.isBlank() ) {
                            Toast.makeText(context, "Please fill all required fields", Toast.LENGTH_SHORT).show()
                        } else {
                            sendApprovalRequest(uid, name, email, department, context)
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF6B73FF))
                ) {
                    Text("Submit Request", color = Color.White, fontSize = 16.sp)
                }
            }
        }
    }
}

fun sendApprovalRequest(
    uid: String,
    name: String,
    email: String,
    department: String,
    context: android.content.Context
) {
    val request = ApprovalRequest(uid, name, email, department)

    RetrofitClient.instance.requestApproval(request).enqueue(object : retrofit2.Callback<ApiResponse> {
        override fun onResponse(call: retrofit2.Call<ApiResponse>, response: retrofit2.Response<ApiResponse>) {
            if (response.isSuccessful) {
                Toast.makeText(context, "Request submitted successfully", Toast.LENGTH_SHORT).show()
                val intent = Intent(context, MainActivity::class.java)
                context.startActivity(intent)
            } else {
                Toast.makeText(context, "Failed to submit: ${response.message()}", Toast.LENGTH_LONG).show()
            }
        }

        override fun onFailure(call: retrofit2.Call<ApiResponse>, t: Throwable) {
            Toast.makeText(context, "Error: ${t.message}", Toast.LENGTH_LONG).show()
        }
    })
}
