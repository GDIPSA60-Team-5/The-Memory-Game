package iss.nus.edu.sg.androidca.thememorygame

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat

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
        val currentCompletionTimeTableView = findViewById<TextView>(R.id.current_completion_time_in_table)
        val timerText = TimeUtils.formatElapsedTime(completionTime)
        currentCompletionTimeView.text = timerText
        currentCompletionTimeTableView.text = timerText

        // Close button to restart the game again
        val closeButton = findViewById<Button>(R.id.close_button)
        closeButton.setOnClickListener {
            val intent = Intent(this, FetchActivity::class.java)
            startActivity(intent)
            finish()
        }
    }
}