package iss.nus.edu.sg.androidca.thememorygame

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.EditText
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.AppCompatButton
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import iss.nus.edu.sg.androidca.thememorygame.api.ApiConstants
import iss.nus.edu.sg.androidca.thememorygame.api.HttpClientProvider
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.io.IOException

class LoginActivity : AppCompatActivity() {
    private val client = HttpClientProvider.client
    private val apiUrl = ApiConstants.LOGIN

    private lateinit var loginBtn: AppCompatButton
    private lateinit var loginSpinner: View
    private lateinit var enterUserName: EditText
    private lateinit var enterPassword: EditText

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        supportActionBar?.hide()
        setContentView(R.layout.activity_login)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.login)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, 0)
            insets
        }


        loginBtn = findViewById(R.id.loginBtn)
        loginSpinner = findViewById(R.id.loginSpinner)
        enterUserName = findViewById(R.id.userName)
        enterPassword = findViewById(R.id.password)

        loginBtn.setOnClickListener {
            val username = enterUserName.text.toString()
            val password = enterPassword.text.toString()

            if (username.isBlank() || password.isBlank()) {
                Toast.makeText(this, "Username and password cannot be empty", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            toggleLoading(true)
            sendDataToBackend(username, password)
        }
    }

    private fun toggleLoading(isLoading: Boolean) {
        runOnUiThread {
            loginBtn.isEnabled = !isLoading
            loginSpinner.visibility = if (isLoading) View.VISIBLE else View.GONE
            loginBtn.text = if (isLoading) "" else "LOGIN TO PLAY"
        }
    }

    private fun sendDataToBackend(username: String, password: String) {
        val json = JSONObject()
            .put("username", username)
            .put("password", password)
            .toString()

        Log.d("DEBUG", "username=[$username], password=[$password]")

        val requestBody = json.toRequestBody("application/json".toMediaType())

        Log.d("DEBUG", json)

        val request = Request.Builder()
            .url(apiUrl)
            .post(requestBody)
            .addHeader("Content-Type", "application/json")
            .build()


        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                Log.e("HTTP", "request failure", e)
                runOnUiThread {
                    toggleLoading(false)
                    Toast.makeText(this@LoginActivity, "Internet error: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }

            override fun onResponse(call: Call, response: Response) {
                response.use {  // ✅ auto-close
                    val statusCode = it.code
                    runOnUiThread {
                        toggleLoading(false)
                        if (it.isSuccessful) {
                            Toast.makeText(this@LoginActivity, "Login successful", Toast.LENGTH_SHORT).show()
                            val intent = Intent(this@LoginActivity, FetchActivity::class.java)
                            startActivity(intent)
                            finish()
                        } else {
                            val errorBody = it.body?.string()
                            Toast.makeText(
                                this@LoginActivity,
                                "Login failure: HTTP $statusCode\n$errorBody",
                                Toast.LENGTH_LONG
                            ).show()
                        }
                    }
                }
            }

        })
    }
}
