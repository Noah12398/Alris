package com.example.alris

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.IOException

class MainActivity : ComponentActivity() {

    private lateinit var googleSignInClient: GoogleSignInClient
    private val RC_SIGN_IN = 1001
    private lateinit var auth: FirebaseAuth
    private val client = OkHttpClient()
     val baseUrl =Constants.BASE_URL

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        auth = FirebaseAuth.getInstance()

        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(getString(R.string.default_web_client_id))
            .requestEmail()
            .build()
        googleSignInClient = GoogleSignIn.getClient(this, gso)

        setContent {
            AlrisTheme {
                GoogleSignInScreen(onSignInClicked = {
                    val signInIntent = googleSignInClient.signInIntent
                    startActivityForResult(signInIntent, RC_SIGN_IN)
                })
            }
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == RC_SIGN_IN) {
            val task = GoogleSignIn.getSignedInAccountFromIntent(data)
            try {
                val account = task.getResult(ApiException::class.java)!!
                firebaseAuthWithGoogle(account.idToken!!)
            } catch (e: ApiException) {
                Log.e("GoogleSignIn", "Google sign-in failed. Code: ${e.statusCode}, Message: ${e.localizedMessage}", e)
                showToast("Google sign-in failed. Code: ${e.statusCode}")
            }
        }
    }


    private fun firebaseAuthWithGoogle(idToken: String) {
        val credential = GoogleAuthProvider.getCredential(idToken, null)
        auth.signInWithCredential(credential)
            .addOnCompleteListener(this) { task ->
                if (task.isSuccessful) {
                    val user = auth.currentUser
                    user?.getIdToken(true)?.addOnCompleteListener { tokenTask ->
                        if (tokenTask.isSuccessful) {
                            val finalToken = tokenTask.result?.token
                            sendIdTokenToServer(finalToken)
                        } else {
                            showToast("Failed to get ID token.")
                        }
                    }
                } else {
                    showToast("Authentication failed.")
                }
            }
    }

    private fun sendIdTokenToServer(idToken: String?) {
        if (idToken == null) return

        val json = """{"idToken":"$idToken"}"""
        val body = json.toRequestBody("application/json".toMediaTypeOrNull())
        val request = Request.Builder()
            .url("$baseUrl/verifyToken")
            .post(body)
            .build()

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                Log.e("AlrisNetwork", "Failed to connect to server", e) // Logs full stack trace
                runOnUiThread { showToast("Server error: ${e.message}") }
            }

            override fun onResponse(call: Call, response: Response) {
                runOnUiThread {
                    if (response.isSuccessful) {
                        Log.i("AlrisNetwork", "Server verification successful.")
                        showToast("Google login verified with server.")

                        // Navigate to DashboardActivity
                        val intent = Intent(this@MainActivity, DashboardActivity::class.java)
                        startActivity(intent)
                        finish() // optional: closes login screen
                    } else {
                        Log.w("AlrisNetwork", "Server rejected token. Code: ${response.code}")
                        showToast("Server rejected token.")
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
fun AlrisTheme(content: @Composable () -> Unit) {
    val colorScheme = darkColorScheme(
        primary = Color(0xFF6C63FF),
        secondary = Color(0xFF03DAC6),
        background = Color(0xFF0A0A0A),
        surface = Color(0xFF1A1A1A),
        onPrimary = Color.White,
        onSecondary = Color.Black,
        onBackground = Color.White,
        onSurface = Color.White
    )

    MaterialTheme(
        colorScheme = colorScheme,
        content = content
    )
}

@Composable
fun GoogleSignInScreen(onSignInClicked: () -> Unit) {
    var isPressed by remember { mutableStateOf(false) }

    // Animated background gradient
    val infiniteTransition = rememberInfiniteTransition(label = "background")
    val gradientOffset by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(8000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "gradient"
    )

    // Floating animation for elements
    val floatingAnimation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 10f,
        animationSpec = infiniteRepeatable(
            animation = tween(3000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "floating"
    )

    // Entrance animations
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.95f else 1f,
        animationSpec = tween(150),
        label = "scale"
    )

    LaunchedEffect(Unit) {
        isPressed = false
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFF1A1A2E).copy(alpha = 0.8f + gradientOffset * 0.2f),
                        Color(0xFF16213E).copy(alpha = 0.9f),
                        Color(0xFF0F0F23).copy(alpha = 1f)
                    )
                )
            )
    ) {
        // Animated background circles
        AnimatedBackgroundCircles()

        Column(
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .fillMaxSize()
                .padding(32.dp)
                .offset(y = floatingAnimation.dp)
        ) {


            // Welcome text with gradient
            Text(
                text = "Welcome to",
                style = MaterialTheme.typography.headlineSmall.copy(
                    fontWeight = FontWeight.Light,
                    color = Color.White.copy(alpha = 0.7f)
                ),
                textAlign = TextAlign.Center
            )

            Text(
                text = "ALRIS",
                style = MaterialTheme.typography.displayMedium.copy(
                    fontWeight = FontWeight.Bold,
                    fontSize = 48.sp,
                    color = Color.White
                ),
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(vertical = 8.dp)
            )

            Text(
                text = "Your AI-powered reporting platform",
                style = MaterialTheme.typography.bodyLarge.copy(
                    color = Color(0xFF03DAC6),
                    fontWeight = FontWeight.Medium
                ),
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(bottom = 64.dp)
            )

            // Google Sign-In Button
            GoogleSignInButton(
                onSignInClicked = onSignInClicked,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
            )
        }
    }
}

@Composable
fun AnimatedBackgroundCircles() {
    val infiniteTransition = rememberInfiniteTransition(label = "circles")

    val circle1Alpha by infiniteTransition.animateFloat(
        initialValue = 0.1f,
        targetValue = 0.3f,
        animationSpec = infiniteRepeatable(
            animation = tween(4000),
            repeatMode = RepeatMode.Reverse
        ),
        label = "circle1"
    )

    val circle2Alpha by infiniteTransition.animateFloat(
        initialValue = 0.2f,
        targetValue = 0.4f,
        animationSpec = infiniteRepeatable(
            animation = tween(6000),
            repeatMode = RepeatMode.Reverse
        ),
        label = "circle2"
    )

    Box(modifier = Modifier.fillMaxSize()) {
        // Circle 1
        Box(
            modifier = Modifier
                .size(200.dp)
                .offset((-50).dp, (-80).dp)
                .background(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            Color(0xFF6C63FF).copy(alpha = circle1Alpha),
                            Color.Transparent
                        )
                    ),
                    shape = RoundedCornerShape(50)
                )
        )

        // Circle 2
        Box(
            modifier = Modifier
                .size(150.dp)
                .align(Alignment.TopEnd)
                .offset(50.dp, (-30).dp)
                .background(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            Color(0xFF03DAC6).copy(alpha = circle2Alpha),
                            Color.Transparent
                        )
                    ),
                    shape = RoundedCornerShape(50)
                )
        )

        // Circle 3
        Box(
            modifier = Modifier
                .size(120.dp)
                .align(Alignment.BottomStart)
                .offset((-30).dp, 40.dp)
                .background(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            Color(0xFF6C63FF).copy(alpha = circle1Alpha * 0.5f),
                            Color.Transparent
                        )
                    ),
                    shape = RoundedCornerShape(50)
                )
        )
    }
}

@Composable
fun GoogleSignInButton(
    onSignInClicked: () -> Unit,
    modifier: Modifier = Modifier
) {
    var isPressed by remember { mutableStateOf(false) }

    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.95f else 1f,
        animationSpec = tween(100),
        label = "button_scale"
    )

    Button(
        onClick = {
            isPressed = true
            onSignInClicked()
        },
        modifier = modifier.scale(scale),
        colors = ButtonDefaults.buttonColors(
            containerColor = Color(0xFF6C63FF)
        ),
        shape = RoundedCornerShape(16.dp),
        elevation = ButtonDefaults.buttonElevation(
            defaultElevation = 8.dp,
            pressedElevation = 4.dp
        )
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            // Google icon placeholder
            Card(
                modifier = Modifier.size(24.dp),
                shape = RoundedCornerShape(4.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White)
            ) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "G",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF4285F4)
                        )
                    )
                }
            }

            Spacer(modifier = Modifier.width(12.dp))

            Text(
                text = "Continue with Google",
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.Medium,
                    color = Color.White
                )
            )
        }
    }

    LaunchedEffect(isPressed) {
        if (isPressed) {
            kotlinx.coroutines.delay(150)
            isPressed = false
        }
    }
}

@Composable
fun FeatureItem(
    icon: ImageVector,
    title: String,
    description: String
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.width(100.dp)
    ) {
        Card(
            modifier = Modifier.size(48.dp),
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(
                containerColor = Color(0xFF6C63FF).copy(alpha = 0.2f)
            )
        ) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = title,
                    modifier = Modifier.size(24.dp),
                    tint = Color(0xFF6C63FF)
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = title,
            style = MaterialTheme.typography.labelMedium.copy(
                fontWeight = FontWeight.SemiBold,
                color = Color.White
            ),
            textAlign = TextAlign.Center
        )

        Text(
            text = description,
            style = MaterialTheme.typography.labelSmall.copy(
                color = Color.White.copy(alpha = 0.7f)
            ),
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(top = 2.dp)
        )
    }
}