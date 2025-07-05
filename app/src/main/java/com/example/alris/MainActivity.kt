package com.example.alris

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.AdminPanelSettings
import androidx.compose.material.icons.filled.Security
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.alris.admin.AdminActivity
import com.example.alris.authority.PendingApprovalActivity
import com.example.alris.ui.theme.AlrisTheme
import com.example.alris.user.DashboardActivity
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.io.IOException

class MainActivity : ComponentActivity() {

    private lateinit var googleSignInClient: GoogleSignInClient
    private lateinit var auth: FirebaseAuth
    private val RC_SIGN_IN = 1001
    private val client = OkHttpClient()
    private val baseUrl = Constants.BASE_URL
    private var loginRole = "user"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Firebase Auth
        auth = FirebaseAuth.getInstance()

        // Google Sign-In config
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(getString(R.string.default_web_client_id)) // From google-services.json
            .requestEmail()
            .build()
        googleSignInClient = GoogleSignIn.getClient(this, gso)

        setContent {
            AlrisTheme {
                GoogleSignInTabbedScreen(
                    onSignInClicked = {
                        val signInIntent = googleSignInClient.signInIntent
                        startActivityForResult(signInIntent, RC_SIGN_IN)
                    },
                    onRoleSelected = { role -> loginRole = role }
                )
            }
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == RC_SIGN_IN) {
            val task = GoogleSignIn.getSignedInAccountFromIntent(data)
            try {
                val account = task.getResult(ApiException::class.java)
                firebaseAuthWithGoogle(account.idToken!!)
            } catch (e: ApiException) {
                showToast("Google sign-in failed: ${e.statusCode}")
            }
        }
    }

    private fun firebaseAuthWithGoogle(idToken: String) {
        val credential = GoogleAuthProvider.getCredential(idToken, null)
        auth.signInWithCredential(credential)
            .addOnCompleteListener(this) { task ->
                if (task.isSuccessful) {
                    val user = auth.currentUser
                    user?.getIdToken(true)?.addOnSuccessListener { result ->
                        sendIdTokenToServer(result.token)
                    }
                } else {
                    showToast("Firebase authentication failed.")
                }
            }
    }

    private fun sendIdTokenToServer(idToken: String?) {
        if (idToken == null) return
        val json = """{"idToken":"$idToken", "role":"$loginRole"}"""
        val requestBody = json.toRequestBody("application/json".toMediaTypeOrNull())

        val request = Request.Builder()
            .url("$baseUrl/verifyToken")
            .post(requestBody)
            .build()

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                runOnUiThread { showToast("Server error: ${e.message}") }
            }

            override fun onResponse(call: Call, response: Response) {
                runOnUiThread {
                    if (response.isSuccessful) {
                        val body = response.body?.string()
                        Log.d("SERVER_RESPONSE", "Raw response body: $body")
                        if (body == null) {
                            showToast("Empty response from server.")
                            return@runOnUiThread
                        }

                        try {
                            val json = JSONObject(body)
                            val status = json.optString("status", "pending")

                            when {
                                loginRole == "user" -> {
                                    startActivity(Intent(this@MainActivity, DashboardActivity()::class.java))
                                }
                                loginRole == "admin" && status == "approved" -> {
                                    startActivity(Intent(this@MainActivity, AdminActivity::class.java))
                                }
                                loginRole == "admin" && status == "denied" -> {
                                    showToast("Access denied. You are not an authorized admin.")
                                }
                                status == "pending" && loginRole == "authority" -> {
                                    startActivity(Intent(this@MainActivity, PendingApprovalActivity::class.java))
                                }
                                status == "approved" -> {
                                    startActivity(Intent(this@MainActivity, DashboardActivity::class.java))
                                }
                                else -> {
                                    showToast("Access not granted. Please wait for approval.")
                                }
                            }
                            finish()
                        } catch (e: Exception) {
                            showToast("Error parsing server response.")
                            Log.e("LoginError", "JSON parse error: $body", e)
                        }
                    } else {
                        showToast("Server error: ${response.code}")
                    }
                }
            }
        })
    }

    private fun showToast(msg: String) {
        Toast.makeText(this, msg, Toast.LENGTH_SHORT).show()
    }
}

@Composable
fun GoogleSignInTabbedScreen(onSignInClicked: () -> Unit, onRoleSelected: (String) -> Unit) {
    val tabs = listOf(
        TabData("User", Icons.Default.AccountCircle, "Access your dashboard"),
        TabData("Authority", Icons.Default.Security, "Authority portal access"),
        TabData("Admin", Icons.Default.AdminPanelSettings, "Administrative controls")
    )
    var selectedTabIndex by remember { mutableStateOf(0) }

    onRoleSelected(tabs[selectedTabIndex].title.lowercase())

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFF667eea),
                        Color(0xFF764ba2),
                        Color(0xFF6B73FF)
                    )
                )
            ),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp)
        ) {
            // App Title
            Text(
                text = "Welcome to Alris",
                fontSize = 32.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                textAlign = TextAlign.Center
            )

            Text(
                text = "Choose your role to continue",
                fontSize = 16.sp,
                color = Color.White.copy(alpha = 0.8f),
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(top = 8.dp)
            )

            Spacer(modifier = Modifier.height(48.dp))

            // Custom Tab Container
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .shadow(
                        elevation = 20.dp,
                        shape = RoundedCornerShape(24.dp)
                    ),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(
                    containerColor = Color.White.copy(alpha = 0.95f)
                )
            ) {
                Column(
                    modifier = Modifier.padding(24.dp)
                ) {
                    // Custom Tab Row
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(16.dp))
                            .background(Color.Gray.copy(alpha = 0.1f))
                            .padding(4.dp),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        tabs.forEachIndexed { index, tab ->
                            TabItem(
                                tab = tab,
                                isSelected = selectedTabIndex == index,
                                onClick = {
                                    selectedTabIndex = index
                                    onRoleSelected(tabs[index].title.lowercase())
                                },
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(32.dp))

                    // Role Description
                    AnimatedContent(
                        targetState = selectedTabIndex,
                        Modifier.fillMaxWidth(),
                                transitionSpec = {
                            slideInHorizontally { it } + fadeIn() togetherWith
                                    slideOutHorizontally { -it } + fadeOut()
                        }
                    ) { index ->
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(
                                imageVector = tabs[index].icon,
                                contentDescription = null,
                                tint = Color(0xFF6B73FF),
                                modifier = Modifier.size(48.dp)
                            )

                            Spacer(modifier = Modifier.height(16.dp))

                            Text(
                                text = tabs[index].title,
                                fontSize = 24.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF2D3748)
                            )

                            Text(
                                text = tabs[index].description,
                                fontSize = 14.sp,
                                color = Color(0xFF718096),
                                textAlign = TextAlign.Center,
                                modifier = Modifier.padding(top = 4.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(40.dp))

                    // Sign In Button
                    Button(
                        onClick = onSignInClicked,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(56.dp)
                            .shadow(
                                elevation = 8.dp,
                                shape = RoundedCornerShape(16.dp)
                            ),
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF6B73FF)
                        )
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            // Google Logo placeholder - you can replace with actual Google logo
                            Box(
                                modifier = Modifier
                                    .size(24.dp)
                                    .background(
                                        Color.White,
                                        RoundedCornerShape(12.dp)
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "G",
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF6B73FF)
                                )
                            }

                            Spacer(modifier = Modifier.width(12.dp))

                            Text(
                                text = "Continue with Google",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = Color.White
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Footer
            Text(
                text = "Secure authentication powered by Google",
                fontSize = 12.sp,
                color = Color.White.copy(alpha = 0.6f),
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
fun TabItem(
    tab: TabData,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val backgroundColor = if (isSelected) Color(0xFF6B73FF) else Color.Transparent
    val textColor = if (isSelected) Color.White else Color(0xFF718096)

    Card(
        onClick = onClick,
        modifier = modifier
            .fillMaxWidth() // Ensure Card expands horizontally
            .clip(RoundedCornerShape(12.dp)),
        colors = CardDefaults.cardColors(
            containerColor = backgroundColor
        ),
        elevation = CardDefaults.cardElevation(
            defaultElevation = if (isSelected) 4.dp else 0.dp
        )
    ) {
        // Center both vertically & horizontally
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = tab.icon,
                contentDescription = null,
                tint = textColor,
                modifier = Modifier.size(20.dp)
            )

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = tab.title,
                fontSize = 12.sp,
                fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                color = textColor,
                textAlign = TextAlign.Center
            )
        }
    }
}
data class TabData(
    val title: String,
    val icon: ImageVector,
    val description: String
)