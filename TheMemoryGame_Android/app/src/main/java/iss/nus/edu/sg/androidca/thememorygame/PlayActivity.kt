package iss.nus.edu.sg.androidca.thememorygame

import android.annotation.SuppressLint
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import android.util.Log
import android.view.View
import android.widget.AdapterView
import android.widget.GridView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat

class PlayActivity : AppCompatActivity(), AdapterView.OnItemClickListener {

    private lateinit var adapter: MyCustomAdapter
    private var startTime = 0L
    private var running = false
    private lateinit var timerHandler: Handler
    private lateinit var timerRunnable: Runnable

    @SuppressLint("DefaultLocale")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_play)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.play)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        // Initialize a timer object
        timerHandler = Handler(Looper.getMainLooper())

        timerRunnable = Runnable {
            @SuppressLint("DefaultLocale")
            val elapsedMillis = SystemClock.elapsedRealtime() - startTime
            val centiSeconds = (elapsedMillis / 10).toInt()
            val seconds = centiSeconds / 100
            val minutes = seconds / 60
            val remainingSeconds = seconds % 60
            val remainingCentiSeconds = centiSeconds % 100

            val timerText = findViewById<TextView>(R.id.timer)
            timerText.text = String.format("%02d:%02d:%02d", minutes, remainingSeconds, remainingCentiSeconds)

            // Schedule the runnable to run again after 1 second
            timerHandler.postDelayed(timerRunnable, 10)
        }

        val gridView = findViewById<GridView>(R.id.playGridView)
        val filenames = intent.getStringArrayExtra("filenames")
        Log.d("PlayActivity filenames", filenames.toString())

        if (filenames != null && filenames.size == 12) {
            adapter = MyCustomAdapter(this, filenames)
            gridView.adapter = adapter
            gridView.onItemClickListener = this
        } else {
            Toast.makeText(this, "Invalid image data", Toast.LENGTH_SHORT).show()
            finish()
            return
        }
    }

    @SuppressLint("SetTextI18n")
    override fun onItemClick(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
        if (!::adapter.isInitialized) return

        // Run the timer at the first click on item
        if (!running) {
            startTime = SystemClock.elapsedRealtime()
            timerHandler.post(timerRunnable)
            running = true
        }

        adapter.revealPosition(position)

        if (adapter.currentlyFlipped.size == 2) {
            val gridView = findViewById<GridView>(R.id.playGridView)
            val matches = findViewById<TextView>(R.id.matches)
            gridView.isEnabled = false
            if (adapter.checkForMatch()) {
                adapter.finalizeMatch()
                gridView.isEnabled = true
                // Show the matches
                matches.text = "${adapter.revealedPositions.size / 2}/6 matches"
            }else {
                gridView.postDelayed({
                    adapter.resetFlipped()
                    gridView.isEnabled = true
                }, 800)
            }
        }

        if (adapter.revealedPositions.size == adapter.count) {
            timerHandler.removeCallbacks(timerRunnable)
            Toast.makeText(this, "You won!", Toast.LENGTH_LONG).show()
        }
    }
}