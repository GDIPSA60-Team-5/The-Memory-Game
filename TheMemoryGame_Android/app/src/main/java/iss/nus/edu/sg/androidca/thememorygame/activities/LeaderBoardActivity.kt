package iss.nus.edu.sg.androidca.thememorygame.activities

import android.content.Intent
import android.graphics.Typeface
import android.media.MediaPlayer
import android.os.Bundle
import android.util.Log
import android.view.Gravity
import android.widget.Button
import android.widget.TableLayout
import android.widget.TableRow
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import iss.nus.edu.sg.androidca.thememorygame.data.RecordDto
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken
import iss.nus.edu.sg.androidca.thememorygame.R
import iss.nus.edu.sg.androidca.thememorygame.api.ApiConstants
import iss.nus.edu.sg.androidca.thememorygame.api.HttpClientProvider
import iss.nus.edu.sg.androidca.thememorygame.utils.TimeUtils
import okhttp3.Request
import java.io.IOException


class LeaderBoardActivity : AppCompatActivity() {
    private val client = HttpClientProvider.client
    private var victorySoundPlayer: MediaPlayer? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        supportActionBar?.hide()
        setContentView(R.layout.activity_leader_board)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.leader_board)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        victorySoundPlayer = MediaPlayer.create(this, R.raw.transition)
        victorySoundPlayer?.start()

        // Show the current completion time on screen
        val completionTime = intent.getLongExtra("completion_time", 0L)
        val currentCompletionTimeView = findViewById<TextView>(R.id.current_completion_time)

        val timerText = TimeUtils.formatElapsedTime(completionTime)
        currentCompletionTimeView.text = timerText


        // Close button to restart the game again
        val closeButton = findViewById<Button>(R.id.close_button)
        closeButton.setOnClickListener {
            val intent = Intent(this, FetchActivity::class.java)
            startActivity(intent)
            finish()
        }

        val tableLayout = findViewById<TableLayout>(R.id.leaderboard_table)
        Thread {
            try {
                val top5 = fetchTop5()
                Log.d("Leaderboard", "Top5: $top5")
                val rank = fetchRank(completionTime)
                Log.d("Leaderboard", "Rank: $rank")
                val username = fetchUsername()
                Log.d("Leaderboard", "Username: $username")


                runOnUiThread {
                    displayLeaderBoard(tableLayout, top5, completionTime, rank, username)
                }
            } catch (e: Exception) {
                e.printStackTrace()
                runOnUiThread {
                    Toast.makeText(this, "Failed to load leaderboard", Toast.LENGTH_SHORT).show()
                }
            }
        }.start()
    }

    private fun fetchTop5(): List<RecordDto> {
        val request = Request.Builder()
            .url(ApiConstants.TOP_FIVE)
            .build()

        client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) throw IOException("Unexpected code $response")

            val body = response.body?.string() ?: throw IOException("Empty body")
            val gson = Gson()
            val type = object : TypeToken<List<RecordDto>>() {}.type
            return gson.fromJson(body, type)
        }
    }

    private fun displayLeaderBoard(tableLayout: TableLayout, records: List<RecordDto>, currentTime: Long, rank: Int, username:String) {
        if (tableLayout.childCount > 1) {
            tableLayout.removeViews(1, tableLayout.childCount - 1)
        }
        var currentUserinTop5 = false

        for ((index, record) in records.withIndex()) {
            val row = TableRow(this)

            val rankView = TextView(this).apply {
                text = (index + 1).toString()
                setPadding(8, 8, 8, 8)
                gravity = Gravity.CENTER
            }

            val nameView = TextView(this).apply {
                text = record.name
                setPadding(8, 8, 8, 8)
                gravity = Gravity.CENTER
            }

            val timeView = TextView(this).apply {
                val formatted = TimeUtils.formatElapsedTime(record.completionTime)
                text = formatted
                setPadding(8, 8, 8, 8)
                gravity = Gravity.CENTER
            }

            if (record.completionTime == currentTime) {
                currentUserinTop5 = true
                row.setBackgroundColor(getColor(R.color.highlight))
                nameView.setTypeface(null, Typeface.BOLD)
                timeView.setTypeface(null, Typeface.BOLD)
                rankView.setTypeface(null, Typeface.BOLD)
            }
            row.addView(rankView)
            row.addView(nameView)
            row.addView(timeView)
            tableLayout.addView(row)
        }
        if (!currentUserinTop5) {
            if (rank > 6) {
                addDotsRow(tableLayout)
            }
            addCurrentUserRow(tableLayout, currentTime, rank, username)
        }


    }

    private fun addDotsRow(tableLayout: TableLayout) {
        val dotsRow = TableRow(this)
        val dots = TextView(this).apply {
            text = "⋮"
            gravity = Gravity.CENTER
            textSize = 22f
            setPadding(8, 8, 8, 8)
        }
        val span = TableRow.LayoutParams()
        span.span = 3
        dots.layoutParams = span

        dotsRow.addView(dots)
        tableLayout.addView(dotsRow)
    }

    private fun addCurrentUserRow(tableLayout: TableLayout, currentTime: Long, rank: Int, username: String) {
        val row = TableRow(this)
        row.setBackgroundColor(getColor(R.color.highlight))

        val rankView = TextView(this).apply {
            text = rank.toString()
            gravity = Gravity.CENTER
            setPadding(8, 8, 8, 8)
            setTypeface(null, Typeface.BOLD)
        }

        val nameView = TextView(this).apply {
            text = username
            gravity = Gravity.CENTER
            setPadding(8, 8, 8, 8)
            setTypeface(null, Typeface.BOLD)
        }

        val timeView = TextView(this).apply {
            text = TimeUtils.formatElapsedTime(currentTime)
            gravity = Gravity.CENTER
            setPadding(8, 8, 8, 8)
            setTypeface(null, Typeface.BOLD)
        }

        row.addView(rankView)
        row.addView(nameView)
        row.addView(timeView)
        tableLayout.addView(row)
    }


    private fun fetchRank(completionTime: Long): Int {
        val request = Request.Builder()
            .url("${ApiConstants.FIND_RANK}?time=$completionTime")
            .build()

        client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) throw IOException("Unexpected code $response")
            val body = response.body?.string() ?: throw IOException("Empty body")
            return body.toInt()
        }
    }


    private fun fetchUsername(): String {
        val request = Request.Builder()
            .url(ApiConstants.USERNAME)
            .build()

        client.newCall(request).execute().use { response ->
            if (response.code == 401) throw Exception("Not logged in")
            if (!response.isSuccessful) throw IOException("Unexpected code $response")

            return response.body?.string()?.replace("\"", "") ?: throw IOException("Empty body")
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        victorySoundPlayer?.release()
        victorySoundPlayer = null
    }

}