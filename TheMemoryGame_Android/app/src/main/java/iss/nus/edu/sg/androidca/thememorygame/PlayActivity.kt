package iss.nus.edu.sg.androidca.thememorygame

import java.io.File
import android.annotation.SuppressLint
import android.content.Intent
import android.graphics.BitmapFactory
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
    private var elapsedMillis: Long = 0L

    private lateinit var adImageView: ImageView
    private val adHandler = Handler(Looper.getMainLooper())
    private lateinit var adRunnable: Runnable

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_play)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.play)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        // Initialize timer
        timerHandler = Handler(Looper.getMainLooper())
        timerRunnable = Runnable {
            elapsedMillis = SystemClock.elapsedRealtime() - startTime
            val timerText = findViewById<TextView>(R.id.timer)
            timerText.text = TimeUtils.formatElapsedTime(elapsedMillis)
            timerHandler.postDelayed(timerRunnable, 10)
        }

        // Set up game board
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

    @SuppressLint("SetTextI18n")
    override fun onItemClick(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
        if (!::adapter.isInitialized) return

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
                matches.text = "${adapter.revealedPositions.size / 2}/6 matches"
            } else {
                gridView.postDelayed({
                    adapter.resetFlipped()
                    gridView.isEnabled = true
                }, 800)
            }
        }

        if (adapter.revealedPositions.size == adapter.count) {
            timerHandler.removeCallbacks(timerRunnable)
            adHandler.removeCallbacks(adRunnable)

            val url = "http://10.0.2.2:5187/Home/SaveCompletionTime?completionTime=$elapsedMillis"
            Log.d("Backend Call Link: ", url)
            Thread {
                try {
                    val saveResult = URL(url).openStream().bufferedReader().use { it.readText() }
                    runOnUiThread {
                        if (saveResult == "saved") {
                            Toast.makeText(this, "Completion time saved!", Toast.LENGTH_SHORT).show()
                        } else {
                            Toast.makeText(this, "Error saving completion time.", Toast.LENGTH_SHORT).show()
                        }
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }.start()

            timerHandler.postDelayed({
                val intent = Intent(this, LeaderBoardActivity::class.java)
                intent.putExtra("completion_time", elapsedMillis)
                startActivity(intent)
                finish()
            }, 800)
        }
    }
}