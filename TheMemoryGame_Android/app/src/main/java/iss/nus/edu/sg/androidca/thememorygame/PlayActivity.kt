package iss.nus.edu.sg.androidca.thememorygame

import android.animation.Animator
import android.animation.AnimatorInflater
import android.animation.AnimatorListenerAdapter
import android.content.Intent
import android.graphics.BitmapFactory
import android.media.MediaPlayer
import android.media.SoundPool
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import android.util.Log
import android.view.View
import android.widget.AdapterView
import android.widget.GridView
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import org.jsoup.Jsoup
import java.net.URL
import kotlin.random.Random


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

    private lateinit var adImageView: ImageView
    private val adHandler = Handler(Looper.getMainLooper())
    private lateinit var adRunnable: Runnable

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_play)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.play)) { v, insets ->
            val sys = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(sys.left, sys.top, sys.right, sys.bottom)
            insets
        }

        // Initialize timer
        timerHandler = Handler(Looper.getMainLooper())
        timerRunnable = Runnable {
            elapsedMillis = SystemClock.elapsedRealtime() - startTime
            findViewById<TextView>(R.id.timer).text =
                TimeUtils.formatElapsedTime(elapsedMillis)
            timerHandler.postDelayed(timerRunnable, 10)
        }

        // Set up game board
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

        // Set up ad image
        adImageView = findViewById(R.id.ads)

        // Determine user type (replace with real check)
//        isPaidUser = false // TODO: Replace with real logic
//        if (!isPaidUser) {
//            adImageView.visibility = View.VISIBLE
//            startAdRotation()
//        } else {
//            adImageView.visibility = View.GONE
//        }
        adImageView.visibility = View.VISIBLE
        startAdRotation()
    }

    private fun startAdRotation() {
        Thread {
            try {
                // Step 1: Scrape image URLs (background)
                val doc = Jsoup.connect("http://10.0.2.2:5187/")
                    .userAgent("Mozilla")
                    .get()
                val imgElements = doc.select("img")
                val httpsImgSrcs = TimeUtils.scrapeImages(imgElements, 11)

                // Step 2: Create image file names
                val ads = mutableListOf<String>()
                for (i in 1..11) {
                    val fileName = if (i < 10) "ad_0$i.png" else "ad_$i.png"
                    ads.add(fileName)
                }

                // Step 3: Download images to local storage
                val localFiles = mutableListOf<File>()
                for (i in ads.indices) {
                    val file = TimeUtils.makeFile(applicationContext, ads[i])
                    TimeUtils.downloadToFile(httpsImgSrcs[i], file)
                    localFiles.add(file)
                }
                Log.d("Downloaded Ads:", localFiles.toString())

                // Step 4: Switch back to main thread for UI updates
                runOnUiThread {
                    adRunnable = object : Runnable {
                        override fun run() {
                            val randomIndex = Random.nextInt(localFiles.size)
                            val file = localFiles[randomIndex]

                            if (file.exists()) {
                                // Decode bitmap in a separate thread (optional)
                                Thread {
                                    val bitmap = BitmapFactory.decodeFile(file.absolutePath)
                                    runOnUiThread {
                                        adImageView.setImageBitmap(bitmap)
                                    }
                                }.start()
                            }

                            adHandler.postDelayed(this, 30000)
                        }
                    }
                    adHandler.post(adRunnable)
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }.start()
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


            adHandler.removeCallbacks(adRunnable)

            // Set game won to true so that clicks are not registered anymore
            gameWon = true
            Thread {
                val url =
                    "http://10.0.2.2:5187/Home/SaveCompletionTime?completionTime=$elapsedMillis"
                try {
                    val saveResult: String = URL(url).openStream().bufferedReader().use { it.readText() }
                    runOnUiThread {
                        if (saveResult == "saved") {
                            Toast.makeText(applicationContext, "Completion time saved!", Toast.LENGTH_SHORT).show()
                        } else {
                            Toast.makeText(applicationContext, "Error saving completion time.", Toast.LENGTH_SHORT).show()
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