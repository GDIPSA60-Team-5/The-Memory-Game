package iss.nus.edu.sg.androidca.thememorygame

import android.animation.Animator
import android.animation.AnimatorInflater
import android.animation.AnimatorListenerAdapter
import android.content.Intent
import android.media.MediaPlayer
import android.media.SoundPool
import android.os.*
import android.util.Log
import android.view.View
import android.widget.*
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import iss.nus.edu.sg.androidca.thememorygame.api.ApiConstants
import iss.nus.edu.sg.androidca.thememorygame.api.HttpClientProvider
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Call
import okhttp3.Callback
import okhttp3.Response
import java.io.IOException


class PlayActivity : AppCompatActivity(), AdapterView.OnItemClickListener {

    private lateinit var adapter: MyCustomAdapter
    private var gameWon = false

    private lateinit var timerHandler: Handler
    private lateinit var timerRunnable: Runnable
    private var startTime = 0L
    private var elapsedMillis = 0L
    private var running = false

    private var bgPlayer: MediaPlayer? = null
    private var transitionPlayer: MediaPlayer? = null
    private lateinit var soundPool: SoundPool
    private var soundFlipId = 0
    private var soundMatchId = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_play)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.play)) { v, insets ->
            val sys = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(sys.left, sys.top, sys.right, sys.bottom)
            insets
        }

        setupTimer()
        setupSounds()

        val filenames = intent.getStringArrayExtra("filenames")
        if (filenames == null || filenames.size != 12) {
            Toast.makeText(this, "Invalid image data", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        adapter = MyCustomAdapter(this, filenames)
        findViewById<GridView>(R.id.playGridView).apply {
            adapter = this@PlayActivity.adapter
            onItemClickListener = this@PlayActivity
        }

    }

    private fun setupTimer() {
        timerHandler = Handler(Looper.getMainLooper())
        timerRunnable = object : Runnable {
            override fun run() {
                elapsedMillis = SystemClock.elapsedRealtime() - startTime
                findViewById<TextView>(R.id.timer).text = TimeUtils.formatElapsedTime(elapsedMillis)
                timerHandler.postDelayed(this, 10)
            }
        }
    }

    private fun setupSounds() {
        bgPlayer = MediaPlayer.create(this, R.raw.bg_music).apply { isLooping = true }
        transitionPlayer = MediaPlayer.create(this, R.raw.transition)
        soundPool = SoundPool.Builder().setMaxStreams(4).build()
        soundFlipId = soundPool.load(this, R.raw.flip, 1)
        soundMatchId = soundPool.load(this, R.raw.match, 1)
    }

    override fun onResume() {
        super.onResume()
        bgPlayer?.start()
    }

    override fun onPause() {
        super.onPause()
        bgPlayer?.pause()
    }

    override fun onItemClick(parent: AdapterView<*>, view: View, position: Int, id: Long) {
        // Do not allow flipping after game ends or if already revealed/flipping
        if (gameWon ||
            adapter.revealedPositions.contains(position) ||
            adapter.currentlyFlipped.contains(position) ||
            view.getTag(R.id.is_animating) == true ||
            adapter.currentlyFlipped.size >= 2
        ) return

        // Start timer on first flip
        if (!running) {
            startTime = SystemClock.elapsedRealtime()
            timerHandler.post(timerRunnable)
            running = true
        }

        // Flip animation
        view.setTag(R.id.is_animating, true)
        soundPool.play(soundFlipId, 1f, 1f, 1, 0, 1f)

        val flipOut = AnimatorInflater.loadAnimator(this, R.animator.flip_out)
        val flipIn = AnimatorInflater.loadAnimator(this, R.animator.flip_in)

        flipOut.setTarget(view)
        flipIn.setTarget(view)

        flipOut.addListener(object : AnimatorListenerAdapter() {
            override fun onAnimationEnd(animation: Animator) {
                adapter.revealPosition(position)
                view.setTag(R.id.is_animating, false)
                flipIn.start()

                if (adapter.currentlyFlipped.size == 2) {
                    handleMatchAndWin(parent as GridView)
                }
            }
        })

        flipOut.start()
    }


    private fun handleMatchAndWin(gridView: GridView) {
        if (gameWon) return

        val matchesView = findViewById<TextView>(R.id.matches)

        if (adapter.currentlyFlipped.size == 2) {
            gridView.isEnabled = false

            if (adapter.checkForMatch()) {
                soundPool.play(soundMatchId, 1f, 1f, 1, 0, 1f)
                adapter.finalizeMatch()
                val matchedPairs = adapter.revealedPositions.size / 2
                matchesView.text = getString(R.string.matches_format, matchedPairs)
                gridView.isEnabled = true
            } else {
                Handler(Looper.getMainLooper()).postDelayed({
                    adapter.resetFlipped()
                    gridView.isEnabled = true
                }, 800)
            }
        }

        if (adapter.revealedPositions.size == adapter.count && !gameWon) {
            gameWon = true
            timerHandler.removeCallbacks(timerRunnable)
            saveCompletionTime()

            transitionPlayer?.setOnCompletionListener {
                startActivity(Intent(this, LeaderBoardActivity::class.java).apply {
                    putExtra("completion_time", elapsedMillis)
                })
                finish()
            }
            transitionPlayer?.start()
        }
    }

    private fun saveCompletionTime() {
        val client = HttpClientProvider.client
        val url = "${ApiConstants.BASE_URL}${ApiConstants.SAVE_TIME_ENDPOINT}"

        val json = "{\"completionTime\": $elapsedMillis}"
        val requestBody = json.toRequestBody("application/json".toMediaType())

        val request = Request.Builder()
            .url(url)
            .post(requestBody)
            .addHeader("Content-Type", "application/json")
            .build()

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                Log.e("SaveTime", "Failed to send completion time", e)
                runOnUiThread {
                    Toast.makeText(this@PlayActivity, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }

            override fun onResponse(call: Call, response: Response) {
                val result = response.body?.string()
                Log.d("SaveTime", "Response: $result")
                runOnUiThread {
                    val message = if (result == "saved") "Completion time saved!" else "Error saving completion time."
                    Toast.makeText(this@PlayActivity, message, Toast.LENGTH_SHORT).show()
                }
            }
        })
    }


    override fun onDestroy() {
        super.onDestroy()
        soundPool.release()
        bgPlayer?.release()
        transitionPlayer?.release()
        timerHandler.removeCallbacks(timerRunnable)
    }
}
