package com.example.alris

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.util.Log
import android.view.ViewGroup
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleOwner
import com.google.android.gms.location.LocationServices
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.RequestBody.Companion.asRequestBody
import java.io.File
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.*

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
    var locationText by remember { mutableStateOf("Location: Loading...") }
    var lastPhotoPath by remember { mutableStateOf<String?>(null) }

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
        }
    }

    fun takePhoto() {
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
                }

                override fun onImageSaved(output: ImageCapture.OutputFileResults) {
                    Log.d("CameraX", "Photo saved: ${photoFile.absolutePath}")
                    lastPhotoPath = photoFile.absolutePath

                    fetchLocation { location ->
                        if (location != null) {
                            locationText = "Lat: ${location.latitude}, Lng: ${location.longitude}"
                            coroutineScope.launch(Dispatchers.IO) {
                                uploadToServer(photoFile, location.latitude, location.longitude)
                            }
                        } else {
                            locationText = "Location not available"
                        }
                    }
                }
            }
        )
    }

    Column(modifier = Modifier.fillMaxSize()) {
        AndroidView(
            factory = { previewView },
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
        )

        Text(
            text = locationText,
            modifier = Modifier.padding(8.dp),
            style = MaterialTheme.typography.bodyMedium
        )

        Button(
            onClick = { takePhoto() },
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Text("Capture & Upload")
        }
    }
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

        val capture = ImageCapture.Builder().build()
        onCaptureReady(capture)

        val selector = CameraSelector.DEFAULT_BACK_CAMERA
        cameraProvider.unbindAll()
        cameraProvider.bindToLifecycle(lifecycleOwner, selector, preview, capture)
    }, ContextCompat.getMainExecutor(context))
}
fun uploadToServer(file: File, latitude: Double, longitude: Double) {
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
        }

        override fun onResponse(call: Call, response: Response) {
            Log.d("UPLOAD", "Success: ${response.body?.string()}")
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
