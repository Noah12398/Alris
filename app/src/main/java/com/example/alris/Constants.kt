package com.example.alris

import android.content.Context
import android.content.Intent
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.firebase.auth.FirebaseAuth

object Constants {
    const val BASE_URL = "http://192.168.1.44:5000"

    // Make sure you have this somewhere globally accessible (e.g., Constants.kt)
    val googleSignInOptions = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
        .requestIdToken("YOUR_WEB_CLIENT_ID") // Replace with your real web client ID
        .requestEmail()
        .build()

    fun logoutAndGoToLogin(context: Context) {
        FirebaseAuth.getInstance().signOut()
        val googleSignInClient = GoogleSignIn.getClient(context, googleSignInOptions)
        googleSignInClient.signOut().addOnCompleteListener {
            val intent = Intent(context, MainActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            context.startActivity(intent)
        }
    }

}
