package iss.nus.edu.sg.androidca.thememorygame

import android.animation.Animator
import android.animation.AnimatorInflater
import android.animation.AnimatorListenerAdapter
import android.content.Intent
import android.media.MediaPlayer
import android.media.SoundPool
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import android.view.View
import android.widget.AdapterView
import android.widget.GridView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import java.net.URL

class PlayActivity : AppCompatActivity(), AdapterView.OnItemClickListener {

    private lateinit var adapter: MyCustomAdapter

    private var startTime = 0L
    private var running = false
    private lateinit var timerHandler: Handler
    private lateinit var timerRunnable: Runnable
    private var elapsedMillis = 0L
    private var gameWon = false
    private var bgPlayer: MediaPlayer? = null
    private var transitionPlayer: MediaPlayer? = null
    private lateinit var soundPool: SoundPool
    private var soundFlipId = 0
    private var soundMatchId = 0

    private lateinit var flipOut: Animator
    private lateinit var flipIn: Animator

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_play)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.play)) { v, insets ->
            val sys = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(sys.left, sys.top, sys.right, sys.bottom)
            insets
        }

        timerHandler = Handler(Looper.getMainLooper())
        timerRunnable = Runnable {
            elapsedMillis = SystemClock.elapsedRealtime() - startTime
            findViewById<TextView>(R.id.timer).text =
                TimeUtils.formatElapsedTime(elapsedMillis)
            timerHandler.postDelayed(timerRunnable, 10)
        }

        bgPlayer = MediaPlayer.create(this, R.raw.bg_music).apply {
            isLooping = true
        }
        transitionPlayer = MediaPlayer.create(this, R.raw.transition)

        soundPool = SoundPool.Builder().setMaxStreams(4).build()
        soundFlipId  = soundPool.load(this, R.raw.flip, 1)
        soundMatchId = soundPool.load(this, R.raw.match, 1)

        flipOut = AnimatorInflater.loadAnimator(this, R.animator.flip_out)
        flipIn  = AnimatorInflater.loadAnimator(this, R.animator.flip_in)

        val gridView = findViewById<GridView>(R.id.playGridView)
        val filenames = intent.getStringArrayExtra("filenames")
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

    override fun onResume() {
        super.onResume()
        bgPlayer?.start()
    }

    override fun onPause() {
        super.onPause()
        bgPlayer?.pause()
    }

    override fun onItemClick(parent: AdapterView<*>, view: View, position: Int, id: Long) {
        if (gameWon || view.getTag(R.id.is_animating) == true) return

        if (!running) {
            startTime = SystemClock.elapsedRealtime()
            timerHandler.post(timerRunnable)
            running = true
        }


        soundPool.play(soundFlipId, 1f, 1f, 1, 0, 1f)
        view.setTag(R.id.is_animating, true)

        flipOut.setTarget(view)
        flipIn.setTarget(view)
        flipOut.addListener(object : AnimatorListenerAdapter() {
            override fun onAnimationEnd(animation: Animator) {
                flipOut.removeListener(this)
                adapter.revealPosition(position)
                view.setTag(R.id.is_animating, false)
                flipIn.start()
                handleMatchAndWin(parent as GridView)
            }
        })
        flipOut.start()
    }

    private fun handleMatchAndWin(gridView: GridView) {
        val matches = findViewById<TextView>(R.id.matches)

        if (adapter.currentlyFlipped.size == 2) {
            gridView.isEnabled = false
            if (adapter.checkForMatch()) {
                // 成功配对音效
                soundPool.play(soundMatchId, 1f, 1f, 1, 0, 1f)

                adapter.finalizeMatch()
                gridView.isEnabled = true
                val count = adapter.revealedPositions.size / 2
                matches.text = getString(R.string.matches_format, count)
            } else {
                gridView.postDelayed({
                    adapter.resetFlipped()
                    gridView.isEnabled = true
                }, 800)
            }
        }

        if (adapter.revealedPositions.size == adapter.count && !gameWon) {
            gameWon = true
            timerHandler.removeCallbacks(timerRunnable)


            Thread {
                val url =
                    "http://10.0.2.2:5187/Home/SaveCompletionTime?completionTime=$elapsedMillis"
                try {
                    URL(url).openStream().bufferedReader().use { reader ->
                        val result = reader.readText()
                        runOnUiThread {
                            Toast.makeText(
                                this,
                                if (result == "saved") "Completion time saved!"
                                else "Error saving completion time.",
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                    }
                } catch (e: Exception) {
                    runOnUiThread {
                        Toast.makeText(this, e.message, Toast.LENGTH_SHORT).show()
                    }
                }
            }.start()

            transitionPlayer?.setOnCompletionListener {
                startActivity(
                    Intent(this, LeaderBoardActivity::class.java).apply {
                        putExtra("completion_time", elapsedMillis)
                    }
                )
                finish()
            }
            transitionPlayer?.start()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        soundPool.release()
        bgPlayer?.release()
        transitionPlayer?.release()
        timerHandler.removeCallbacks(timerRunnable)
    }
}
