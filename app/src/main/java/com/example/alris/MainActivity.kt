package com.example.alris

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.BlendMode.Companion.Color
import androidx.compose.ui.unit.dp
import com.example.alris.ui.theme.AlrisTheme
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
    val tabs = listOf("User", "Authority", "Admin")
    var selectedTabIndex by remember { mutableStateOf(0) }

    onRoleSelected(tabs[selectedTabIndex].lowercase())

    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxSize()) {
        TabRow(selectedTabIndex = selectedTabIndex) {
            tabs.forEachIndexed { index, title ->
                Tab(
                    selected = selectedTabIndex == index,
                    onClick = {
                        selectedTabIndex = index
                        onRoleSelected(tabs[index].lowercase())
                    },
                    text = { Text(title) }
                )
            }
        }

        Spacer(modifier = Modifier.height(40.dp))

        Button(
            onClick = onSignInClicked,
            modifier = Modifier
                .padding(20.dp)
                .height(56.dp)
                .fillMaxWidth(0.7f),
            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
        ) {
            Text("Sign in with Google")
        }
    }
}



