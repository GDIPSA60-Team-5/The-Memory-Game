package iss.nus.edu.sg.androidca.thememorygame.activities

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
import iss.nus.edu.sg.androidca.thememorygame.R
import iss.nus.edu.sg.androidca.thememorygame.api.ApiConstants
import iss.nus.edu.sg.androidca.thememorygame.api.HttpClientProvider
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.io.IOException

class LoginActivity : AppCompatActivity() {

    private lateinit var loginBtn: AppCompatButton
    private lateinit var loginSpinner: View
    private lateinit var usernameInput: EditText
    private lateinit var passwordInput: EditText

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        enableEdgeToEdge()
        supportActionBar?.hide()
        setContentView(R.layout.activity_login)
        setupInsets()
        initViews()
        setupListeners()
    }

    private fun setupInsets() {
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.login)) { view, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            view.setPadding(systemBars.left, systemBars.top, systemBars.right, 0)
            insets
        }
    }

    private fun initViews() {
        loginBtn = findViewById(R.id.loginBtn)
        loginSpinner = findViewById(R.id.loginSpinner)
        usernameInput = findViewById(R.id.userName)
        passwordInput = findViewById(R.id.password)
    }

    private fun setupListeners() {
        loginBtn.setOnClickListener {
            val username = usernameInput.text.toString().trim()
            val password = passwordInput.text.toString().trim()

            if (validateInput(username, password)) {
                performLogin(username, password)
            }
        }
    }

    private fun validateInput(username: String, password: String): Boolean {
        return if (username.isEmpty() || password.isEmpty()) {
            showToast("Username and password cannot be empty")
            false
        } else true
    }

    private fun performLogin(username: String, password: String) {
        showLoading(true)
        val requestBody = createLoginRequestBody(username, password)
        val request = buildLoginRequest(requestBody)

        HttpClientProvider.client.newCall(request).enqueue(LoginCallback())
    }

    private fun createLoginRequestBody(username: String, password: String): RequestBody {
        val json = JSONObject().apply {
            put("username", username)
            put("password", password)
        }.toString()

        Log.d("LoginActivity", "Login request: username=[$username]")
        return json.toRequestBody("application/json".toMediaType())
    }

    private fun buildLoginRequest(body: RequestBody): Request {
        return Request.Builder()
            .url(ApiConstants.LOGIN)
            .post(body)
            .addHeader("Content-Type", "application/json")
            .build()
    }

    private fun showLoading(isLoading: Boolean) {
        runOnUiThread {
            loginBtn.isEnabled = !isLoading
            loginSpinner.visibility = if (isLoading) View.VISIBLE else View.GONE
            loginBtn.text = if (isLoading) "" else getString(R.string.login_to_play)
        }
    }

    private fun showToast(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }

    private fun navigateToFetchActivity() {
        startActivity(Intent(this, FetchActivity::class.java))
        finish()
    }

    private inner class LoginCallback : Callback {
        override fun onFailure(call: Call, e: IOException) {
            Log.e("LoginActivity", "Login request failed", e)
            runOnUiThread {
                showLoading(false)
                showToast("Connection error: ${e.message}")
            }
        }

        override fun onResponse(call: Call, response: Response) {
            response.use {
                runOnUiThread {
                    showLoading(false)
                    handleLoginResponse(it)
                }
            }
        }
    }

    private fun handleLoginResponse(response: Response) {
        if (response.isSuccessful) {
            showToast("Login successful")
            navigateToFetchActivity()
        } else {
            val errorMsg = response.body?.string().orEmpty()
            showToast("Login failed: HTTP ${response.code}\n$errorMsg")
        }
    }
}
