package iss.nus.edu.sg.androidca.thememorygame.activities

import android.content.Intent
import android.graphics.Typeface
import android.media.MediaPlayer
import android.os.Bundle
import android.util.Log
import android.view.Gravity
import android.view.LayoutInflater
import android.widget.*
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import iss.nus.edu.sg.androidca.thememorygame.R
import iss.nus.edu.sg.androidca.thememorygame.api.ApiConstants
import iss.nus.edu.sg.androidca.thememorygame.api.HttpClientProvider
import iss.nus.edu.sg.androidca.thememorygame.data.RecordDto
import iss.nus.edu.sg.androidca.thememorygame.utils.TimeUtils
import okhttp3.Request
import java.io.IOException
import androidx.core.graphics.toColorInt

class LeaderBoardActivity : AppCompatActivity() {
    private val client = HttpClientProvider.client
    private var victorySoundPlayer: MediaPlayer? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        supportActionBar?.hide()
        setContentView(R.layout.activity_leader_board)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.leader_board)) { view, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            view.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        victorySoundPlayer = MediaPlayer.create(this, R.raw.transition).apply { start() }

        val completionTime = intent.getLongExtra("completion_time", 0L)
        findViewById<TextView>(R.id.current_completion_time).text = TimeUtils.formatElapsedTime(completionTime)

        findViewById<Button>(R.id.close_button).setOnClickListener {
            startActivity(Intent(this, FetchActivity::class.java))
            finish()
        }

        val tableLayout = findViewById<TableLayout>(R.id.leaderboard_table)
        Thread {
            try {
                val top5 = fetchTop5()
                val rank = fetchRank(completionTime)
                val username = fetchUsername()

                Log.d("Leaderboard", "Top5: $top5")
                Log.d("Leaderboard", "Rank: $rank")
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
        val request = Request.Builder().url(ApiConstants.TOP_FIVE).build()
        client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) throw IOException("Unexpected code $response")
            val body = response.body?.string() ?: throw IOException("Empty body")
            val type = object : TypeToken<List<RecordDto>>() {}.type
            return Gson().fromJson(body, type)
        }
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
        val request = Request.Builder().url(ApiConstants.USERNAME).build()

        client.newCall(request).execute().use { response ->
            if (response.code == 401) throw Exception("Not logged in")
            if (!response.isSuccessful) throw IOException("Unexpected code $response")
            return response.body?.string()?.replace("\"", "") ?: throw IOException("Empty body")
        }
    }

    private fun displayLeaderBoard(
        tableLayout: TableLayout,
        records: List<RecordDto>,
        currentTime: Long,
        rank: Int,
        username: String
    ) {
        if (tableLayout.childCount > 1) {
            tableLayout.removeViews(1, tableLayout.childCount - 1)
        }

        val currentUserInTop5 = records.any { it.completionTime == currentTime && it.name == username }

        records.forEachIndexed { index, record ->
            val highlight = (record.completionTime == currentTime) && (record.name == username)
            val row = createTableRowFromLayout(index + 1, record.name, record.completionTime, highlight, tableLayout)
            tableLayout.addView(row)
        }

        if (!currentUserInTop5) {
            if (rank > 6) addDotsRow(tableLayout)
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
        val span = TableRow.LayoutParams().apply { span = 3 }
        dots.layoutParams = span

        dotsRow.addView(dots)
        tableLayout.addView(dotsRow)
    }

    private fun addCurrentUserRow(tableLayout: TableLayout, currentTime: Long, rank: Int, username: String) {
        val row = createTableRowFromLayout(rank, username, currentTime, highlight = true, parent = tableLayout)
        tableLayout.addView(row)
    }

    private fun createTableRowFromLayout(
        rank: Int,
        name: String,
        time: Long,
        highlight: Boolean = false,
        parent: TableLayout
    ): TableRow {
        val inflater = LayoutInflater.from(this)
        val row = inflater.inflate(R.layout.leaderboard_row, parent, false) as TableRow

        val rankView = row.findViewById<TextView>(R.id.rankView)
        val nameView = row.findViewById<TextView>(R.id.nameView)
        val timeView = row.findViewById<TextView>(R.id.timeView)

        rankView.text = rank.toString()
        nameView.text = name
        timeView.text = TimeUtils.formatElapsedTime(time)

        if (highlight) {
            row.setBackgroundColor("#3355BBDD".toColorInt())
            rankView.setTypeface(null, Typeface.BOLD)
            nameView.setTypeface(null, Typeface.BOLD)
            timeView.setTypeface(null, Typeface.BOLD)
        }

        return row
    }

    override fun onDestroy() {
        super.onDestroy()
        victorySoundPlayer?.release()
        victorySoundPlayer = null
    }
}
