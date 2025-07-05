package com.example.alris.user

import android.util.Size
import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.util.Log
import android.view.ViewGroup
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Error
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleOwner
import com.example.alris.Constants
import com.google.android.gms.location.LocationServices
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.RequestBody.Companion.asRequestBody
import java.io.File
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CameraScreen() {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val coroutineScope = rememberCoroutineScope()

    val previewView = remember {
        PreviewView(context).apply {
            layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )
        }
    }

    var imageCapture by remember { mutableStateOf<ImageCapture?>(null) }
    var locationText by remember { mutableStateOf("") }
    var lastPhotoPath by remember { mutableStateOf<String?>(null) }
    var isCapturing by remember { mutableStateOf(false) }
    var uploadStatus by remember { mutableStateOf<UploadStatus>(UploadStatus.Idle) }
    var hasLocation by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        if (hasPermissions(context)) {
            startCamera(previewView, lifecycleOwner) {
                imageCapture = it
            }
        }
    }

    fun fetchLocation(onResult: (Location?) -> Unit) {
        val fusedLocationClient = LocationServices.getFusedLocationProviderClient(context)
        if (ActivityCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            onResult(null)
            return
        }
        fusedLocationClient.lastLocation.addOnSuccessListener { location: Location? ->
            onResult(location)
            hasLocation = location != null
            if (location != null) {
                locationText = "${location.latitude.format(4)}, ${location.longitude.format(4)}"
            }
        }
    }

    fun takePhoto() {
        if (isCapturing) return
        isCapturing = true
        val capture = imageCapture ?: return
        val photoFile = File(
            context.getExternalFilesDir(null),
            SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date()) + ".jpg"
        )

        val outputOptions = ImageCapture.OutputFileOptions.Builder(photoFile).build()
        capture.takePicture(
            outputOptions,
            ContextCompat.getMainExecutor(context),
            object : ImageCapture.OnImageSavedCallback {
                override fun onError(exc: ImageCaptureException) {
                    Log.e("CameraX", "Capture failed: ${exc.message}", exc)
                    isCapturing = false
                    uploadStatus = UploadStatus.Error("Capture failed")
                }

                override fun onImageSaved(output: ImageCapture.OutputFileResults) {
                    Log.d("CameraX", "Photo saved: ${photoFile.absolutePath}")
                    lastPhotoPath = photoFile.absolutePath

                    coroutineScope.launch {
                        delay(500) // Show capture animation
                        isCapturing = false
                        uploadStatus = UploadStatus.Uploading
                    }

                    fetchLocation { location ->
                        if (location != null) {
                            coroutineScope.launch(Dispatchers.IO) {
                                uploadToServer(photoFile, location.latitude, location.longitude) { success ->
                                    uploadStatus = if (success) {
                                        UploadStatus.Success
                                    } else {
                                        UploadStatus.Error("Upload failed")
                                    }

                                    coroutineScope.launch {
                                        delay(2000)
                                        uploadStatus = UploadStatus.Idle
                                    }
                                }
                            }
                        } else {
                            uploadStatus = UploadStatus.Error("Location not available")
                        }
                    }
                }
            }
        )
    }

    // Fetch location periodically
    LaunchedEffect(Unit) {
        while (true) {
            fetchLocation { }
            delay(5000) // Update every 5 seconds
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        // Camera Preview
        AndroidView(
            factory = { previewView },
            modifier = Modifier.fillMaxSize()
        )

        // Top gradient overlay
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(120.dp)
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color.Black.copy(alpha = 0.7f),
                            Color.Transparent
                        )
                    )
                )
        )

        // Bottom gradient overlay
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(200.dp)
                .align(Alignment.BottomCenter)
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color.Transparent,
                            Color.Black.copy(alpha = 0.8f)
                        )
                    )
                )
        )

        // Status Bar
        StatusBar(
            hasLocation = hasLocation,
            locationText = locationText,
            uploadStatus = uploadStatus,
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.TopCenter)
                .padding(top = 24.dp)
        )

        // Capture Button and Controls
        CaptureControls(
            isCapturing = isCapturing,
            onCaptureClick = { takePhoto() },
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 32.dp)
        )

        // Flash Effect
        AnimatedVisibility(
            visible = isCapturing,
            enter = fadeIn(animationSpec = tween(100)),
            exit = fadeOut(animationSpec = tween(100))
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.White.copy(alpha = 0.8f))
            )
        }
    }
}

@Composable
fun StatusBar(
    hasLocation: Boolean,
    locationText: String,
    uploadStatus: UploadStatus,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .padding(horizontal = 16.dp)
            .shadow(8.dp, RoundedCornerShape(20.dp)),
        colors = CardDefaults.cardColors(
            containerColor = Color.Black.copy(alpha = 0.6f)
        ),
        shape = RoundedCornerShape(20.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Location Status
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    Icons.Default.LocationOn,
                    contentDescription = "Location",
                    tint = if (hasLocation) Color.Green else Color.Gray,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = if (hasLocation) locationText else "No location",
                    color = Color.White,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium
                )
            }

            // Upload Status
            UploadStatusIndicator(uploadStatus)
        }
    }
}

@Composable
fun UploadStatusIndicator(status: UploadStatus) {
    when (status) {
        is UploadStatus.Idle -> {
            // Nothing to show
        }
        is UploadStatus.Uploading -> {
            Row(verticalAlignment = Alignment.CenterVertically) {
                CircularProgressIndicator(
                    modifier = Modifier.size(16.dp),
                    strokeWidth = 2.dp,
                    color = Color.White
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Uploading...",
                    color = Color.White,
                    fontSize = 12.sp
                )
            }
        }
        is UploadStatus.Success -> {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    Icons.Default.CheckCircle,
                    contentDescription = "Success",
                    tint = Color.Green,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Uploaded!",
                    color = Color.Green,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium
                )
            }
        }
        is UploadStatus.Error -> {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    Icons.Default.Error,
                    contentDescription = "Error",
                    tint = Color.Red,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = status.message,
                    color = Color.Red,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium
                )
            }
        }
    }
}

@Composable
fun CaptureControls(
    isCapturing: Boolean,
    onCaptureClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val scale by animateFloatAsState(
        targetValue = if (isCapturing) 0.9f else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        )
    )

    val rotation by animateFloatAsState(
        targetValue = if (isCapturing) 360f else 0f,
        animationSpec = tween(500)
    )

    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Capture Button
        Box(
            modifier = Modifier
                .size(80.dp)
                .scale(scale)
                .graphicsLayer(rotationZ = rotation)
                .shadow(16.dp, CircleShape)
        ) {
            // Outer Ring
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .border(4.dp, Color.White, CircleShape)
                    .padding(8.dp)
            ) {
                // Inner Button
                FloatingActionButton(
                    onClick = onCaptureClick,
                    modifier = Modifier.fillMaxSize(),
                    containerColor = Color.White,
                    contentColor = Color.Black,
                    shape = CircleShape
                ) {
                    if (isCapturing) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(24.dp),
                            strokeWidth = 3.dp,
                            color = Color.Black
                        )
                    } else {
                        Icon(
                            Icons.Default.CameraAlt,
                            contentDescription = "Capture",
                            modifier = Modifier.size(32.dp)
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Capture Text
        Text(
            text = if (isCapturing) "Processing..." else "Capture & Upload",
            color = Color.White,
            fontSize = 16.sp,
            fontWeight = FontWeight.Medium,
            textAlign = TextAlign.Center
        )
    }
}

// Helper function for formatting coordinates
fun Double.format(digits: Int) = "%.${digits}f".format(this)

sealed class UploadStatus {
    object Idle : UploadStatus()
    object Uploading : UploadStatus()
    object Success : UploadStatus()
    data class Error(val message: String) : UploadStatus()
}

fun startCamera(
    previewView: PreviewView,
    lifecycleOwner: LifecycleOwner,
    onCaptureReady: (ImageCapture) -> Unit
) {
    val context = previewView.context
    val cameraProviderFuture = ProcessCameraProvider.getInstance(context)
    cameraProviderFuture.addListener({
        val cameraProvider = cameraProviderFuture.get()

        val preview = Preview.Builder().build().also {
            it.setSurfaceProvider(previewView.surfaceProvider)
        }

        val capture = ImageCapture.Builder()
            .setTargetResolution(Size(1280, 720)) // or 640x480
            .build()
        onCaptureReady(capture)

        val selector = CameraSelector.DEFAULT_BACK_CAMERA
        cameraProvider.unbindAll()
        cameraProvider.bindToLifecycle(lifecycleOwner, selector, preview, capture)
    }, ContextCompat.getMainExecutor(context))
}

fun uploadToServer(
    file: File,
    latitude: Double,
    longitude: Double,
    onComplete: (Boolean) -> Unit
) {
    val client = OkHttpClient()

    val requestBody = MultipartBody.Builder()
        .setType(MultipartBody.FORM)
        .addFormDataPart("photo", file.name, file.asRequestBody("image/jpeg".toMediaTypeOrNull()))
        .addFormDataPart("latitude", latitude.toString())
        .addFormDataPart("longitude", longitude.toString())
        .build()

    val request = Request.Builder()
        .url("${Constants.BASE_URL}/upload")
        .post(requestBody)
        .build()

    client.newCall(request).enqueue(object : Callback {
        override fun onFailure(call: Call, e: IOException) {
            Log.e("UPLOAD", "Upload failed: ${e.message}")
            onComplete(false)
        }

        override fun onResponse(call: Call, response: Response) {
            Log.d("UPLOAD", "Success: ${response.body?.string()}")
            onComplete(response.isSuccessful)
        }
    })
}

fun hasPermissions(context: Context): Boolean {
    return listOf(
        Manifest.permission.CAMERA,
        Manifest.permission.ACCESS_FINE_LOCATION
    ).all {
        ContextCompat.checkSelfPermission(context, it) == PackageManager.PERMISSION_GRANTED
    }
}