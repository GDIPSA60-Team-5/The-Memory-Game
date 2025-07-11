package iss.nus.edu.sg.androidca.thememorygame.activities

import android.content.Intent
import android.graphics.Color
import android.graphics.Typeface
import android.media.MediaPlayer
import android.os.Bundle
import android.util.Log
import android.view.Gravity
import android.view.LayoutInflater
import android.widget.*
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.graphics.toColorInt
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

class LeaderBoardActivity : AppCompatActivity() {

    private val client = HttpClientProvider.client
    private var victorySoundPlayer: MediaPlayer? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        supportActionBar?.hide()
        setContentView(R.layout.activity_leader_board)

        applyWindowInsets()
        playVictorySound()

        val completionTime = intent.getLongExtra("completion_time", 0L)
        displayCurrentCompletionTime(completionTime)
        setupCloseButton()

        fetchAndDisplayLeaderboard(completionTime)
    }

    private fun applyWindowInsets() {
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.leader_board)) { view, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            view.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
    }

    private fun playVictorySound() {
        victorySoundPlayer = MediaPlayer.create(this, R.raw.transition).apply { start() }
    }

    private fun displayCurrentCompletionTime(completionTime: Long) {
        findViewById<TextView>(R.id.current_completion_time).text =
            TimeUtils.formatElapsedTime(completionTime)
    }

    private fun setupCloseButton() {
        findViewById<Button>(R.id.close_button).setOnClickListener {
            startActivity(Intent(this, FetchActivity::class.java))
            finish()
        }
    }

    private fun fetchAndDisplayLeaderboard(completionTime: Long) {
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

    // Networking methods
    @Throws(IOException::class)
    private fun fetchTop5(): List<RecordDto> = client.newCall(
        Request.Builder().url(ApiConstants.TOP_FIVE).build()
    ).execute().use { response ->
        checkResponse(response)
        val body = response.body?.string() ?: throw IOException("Empty body")
        val type = object : TypeToken<List<RecordDto>>() {}.type
        Gson().fromJson(body, type)
    }

    @Throws(IOException::class)
    private fun fetchRank(completionTime: Long): Int = client.newCall(
        Request.Builder().url("${ApiConstants.FIND_RANK}?time=$completionTime").build()
    ).execute().use { response ->
        checkResponse(response)
        val body = response.body?.string() ?: throw IOException("Empty body")
        body.toInt()
    }

    @Throws(Exception::class)
    private fun fetchUsername(): String = client.newCall(
        Request.Builder().url(ApiConstants.USERNAME).build()
    ).execute().use { response ->
        when {
            response.code == 401 -> throw Exception("Not logged in")
            !response.isSuccessful -> throw IOException("Unexpected code $response")
            else -> response.body?.string()?.replace("\"", "") ?: throw IOException("Empty body")
        }
    }

    private fun checkResponse(response: okhttp3.Response) {
        if (!response.isSuccessful) throw IOException("Unexpected code $response")
    }

    // UI display logic
    private fun displayLeaderBoard(
        tableLayout: TableLayout,
        records: List<RecordDto>,
        currentTime: Long,
        rank: Int,
        username: String
    ) {
        clearTableExceptHeader(tableLayout)

        val currentUserInTop5 = records.any { it.completionTime == currentTime && it.name == username }

        records.forEachIndexed { index, record ->
            val highlight = record.completionTime == currentTime && record.name == username
            tableLayout.addView(createTableRow(index + 1, record.name, record.completionTime, highlight, tableLayout))
        }

        if (!currentUserInTop5) {
            if (rank > 6) addEllipsisRow(tableLayout)
            addCurrentUserRow(tableLayout, rank, username, currentTime)
        }
    }

    private fun clearTableExceptHeader(tableLayout: TableLayout) {
        if (tableLayout.childCount > 1) {
            tableLayout.removeViews(1, tableLayout.childCount - 1)
        }
    }

    private fun addEllipsisRow(tableLayout: TableLayout) {
        val dotsRow = TableRow(this)
        val dots = TextView(this).apply {
            text = "⋮"
            gravity = Gravity.CENTER
            textSize = 22f
            setPadding(8, 8, 8, 8)
            setTextColor(Color.WHITE)
            layoutParams = TableRow.LayoutParams().apply { span = 3 }
        }
        dotsRow.addView(dots)
        tableLayout.addView(dotsRow)
    }

    private fun addCurrentUserRow(tableLayout: TableLayout, rank: Int, username: String, currentTime: Long) {
        val row = createTableRow(rank, username, currentTime, highlight = true, parent = tableLayout)
        tableLayout.addView(row)
    }

    private fun createTableRow(
        rank: Int,
        name: String,
        time: Long,
        highlight: Boolean = false,
        parent: TableLayout
    ): TableRow {
        val inflater = LayoutInflater.from(this)
        val row = inflater.inflate(R.layout.leaderboard_row, parent, false) as TableRow

        row.findViewById<TextView>(R.id.rankView).apply {
            text = rank.toString()
            setTypeface(null, if (highlight) Typeface.BOLD else Typeface.NORMAL)
        }
        row.findViewById<TextView>(R.id.nameView).apply {
            text = name
            setTypeface(null, if (highlight) Typeface.BOLD else Typeface.NORMAL)
        }
        row.findViewById<TextView>(R.id.timeView).apply {
            text = TimeUtils.formatElapsedTime(time)
            setTypeface(null, if (highlight) Typeface.BOLD else Typeface.NORMAL)
        }

        if (highlight) {
            row.setBackgroundColor("#3355BBDD".toColorInt())
        }

        return row
    }

    override fun onDestroy() {
        super.onDestroy()
        victorySoundPlayer?.release()
        victorySoundPlayer = null
    }
}
