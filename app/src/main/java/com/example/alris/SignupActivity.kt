package com.example.alris

import android.os.Bundle
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.IOException

class SignupActivity : AppCompatActivity() {

    private lateinit var usernameInput: EditText
    private lateinit var passwordInput: EditText
    private lateinit var phoneInput: EditText
    private lateinit var signupButton: Button

    private val client = OkHttpClient()
    private val baseUrl = "https://your-server-url.com"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_signup)

        usernameInput = findViewById(R.id.signupUsernameInput)
        passwordInput = findViewById(R.id.signupPasswordInput)
        phoneInput = findViewById(R.id.signupPhoneInput)
        signupButton = findViewById(R.id.signupButton)

        signupButton.setOnClickListener {
            val username = usernameInput.text.toString()
            val password = passwordInput.text.toString()
            val phone = phoneInput.text.toString()
            signupUser(username, password, phone)
        }
    }

    private fun signupUser(username: String, password: String, phone: String) {
        val json = """{"username":"$username", "password":"$password", "phone":"$phone"}"""
        val body = json.toRequestBody("application/json".toMediaTypeOrNull())

        val request = Request.Builder()
            .url("$baseUrl/signup")
            .post(body)
            .build()

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                runOnUiThread {
                    showToast("Signup failed: ${e.message}")
                }
            }

            override fun onResponse(call: Call, response: Response) {
                runOnUiThread {
                    showToast(response.body?.string() ?: "No response")
                }
            }
        })
    }

    private fun showToast(msg: String) {
        Toast.makeText(this, msg, Toast.LENGTH_SHORT).show()
    }
}
