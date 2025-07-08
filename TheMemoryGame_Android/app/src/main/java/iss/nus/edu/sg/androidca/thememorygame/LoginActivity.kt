package iss.nus.edu.sg.androidca.thememorygame

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.io.IOException


class LoginActivity : AppCompatActivity() {
    private val client = OkHttpClient()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_login)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.login)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        val login = findViewById<Button>(R.id.loginBtn)
        val enterUserName = findViewById<EditText>(R.id.userName)
        val enterPassword = findViewById<EditText>(R.id.password)

        login.setOnClickListener {
            val username = enterUserName.text.toString()
            val password = enterPassword.text.toString()

            if (username.isBlank() || password.isBlank()) {
                Toast.makeText(this, "Username and password can not be empty", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            sendDataToBackend(username, password)
        }
    }

    private fun sendDataToBackend(username: String, password: String) {
        val json = JSONObject()
            .put("username", username)
            .put("password", password)
            .toString()

        val requestBody = json.toRequestBody("application/json".toMediaType())

        //TODO: change your own url
        val request = Request.Builder()
            .url("http://10.0.2.2:5187/api/login/login")
            .post(requestBody)
            .addHeader("Content-Type","application/json")
            .build()

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                Log.e("HTTP", "request failure", e)
                runOnUiThread {
                    Toast.makeText(this@LoginActivity, "Internet error: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }

            override fun onResponse(call: Call, response: Response) {
                val statusCode = response.code
                runOnUiThread {
                    if (response.isSuccessful) {
                        // 200-299 means success
                        Toast.makeText(this@LoginActivity, "login successfully", Toast.LENGTH_SHORT).show()
                        // go to Fetch page
                        val intent = Intent(this@LoginActivity, FetchActivity::class.java)
                        startActivity(intent)
                        finish() // close login page
                    } else {
                        // other code means failure
                        Toast.makeText(
                            this@LoginActivity,
                            "login failure: HTTP $statusCode",
                            Toast.LENGTH_LONG
                        ).show()
                    }
                }
            }
        })
    }
}
