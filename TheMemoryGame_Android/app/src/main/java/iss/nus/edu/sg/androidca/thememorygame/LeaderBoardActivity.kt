package iss.nus.edu.sg.androidca.thememorygame

import android.content.Intent
import android.graphics.Typeface
import android.os.Bundle
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
import androidx.core.view.setPadding
import iss.nus.edu.sg.androidca.thememorygame.constants.Constants
import iss.nus.edu.sg.androidca.thememorygame.data.RecordDto
import java.net.HttpURLConnection
import java.net.URL
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken


class LeaderBoardActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_leader_board)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.leader_board)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

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
                val rank = fetchRank(completionTime)
                val username = fetchUsername()

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
        val url = URL("${Constants.BASE_URL}/api/home/top5")
        val connection = url.openConnection() as HttpURLConnection
        connection.requestMethod = "GET"

        return connection.inputStream.bufferedReader().use { reader ->
            val response = reader.readText()
            val gson = Gson()
            val type = object : TypeToken<List<RecordDto>>() {}.type
            gson.fromJson(response, type)
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
            addDotsRow(tableLayout)
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

        val rank = TextView(this).apply {
            text = rank.toString()
            gravity = Gravity.CENTER
            setPadding(8, 8, 8, 8)
            setTypeface(null, Typeface.BOLD)
        }

        val name = TextView(this).apply {
            text = username
            gravity = Gravity.CENTER
            setPadding(8, 8, 8, 8)
            setTypeface(null, Typeface.BOLD)
        }

        val time = TextView(this).apply {
            text = TimeUtils.formatElapsedTime(currentTime)
            gravity = Gravity.CENTER
            setPadding(8, 8, 8, 8)
            setTypeface(null, Typeface.BOLD)
        }

        row.addView(rank)
        row.addView(name)
        row.addView(time)
        tableLayout.addView(row)
    }

    private fun fetchRank(completionTime: Long): Int {
        val url = URL("${Constants.BASE_URL}/api/home/rank?time=$completionTime")
        val connection = url.openConnection() as HttpURLConnection
        connection.requestMethod = "GET"
        return connection.inputStream.bufferedReader().use {
            it.readText().toInt()
        }
        }

    private fun fetchUsername(): String {
        val url = URL("${Constants.BASE_URL}/api/home/me")
        val connection = url.openConnection() as HttpURLConnection
        connection.requestMethod = "GET"

        if (connection.responseCode != 200){
            throw Exception("Not logged in")
        }
        return connection.inputStream.bufferedReader().use{
            it.readText().replace("\"", "")

        }
    }


}