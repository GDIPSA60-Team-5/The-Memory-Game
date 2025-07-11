package iss.nus.edu.sg.androidca.thememorygame.activities

import android.animation.Animator
import android.animation.AnimatorInflater
import android.animation.AnimatorListenerAdapter
import android.content.Intent
import android.os.*
import android.view.View
import android.widget.*
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.bumptech.glide.Glide
import iss.nus.edu.sg.androidca.thememorygame.MyCustomAdapter
import iss.nus.edu.sg.androidca.thememorygame.R
import iss.nus.edu.sg.androidca.thememorygame.api.ApiConstants
import iss.nus.edu.sg.androidca.thememorygame.api.HttpClientProvider
import iss.nus.edu.sg.androidca.thememorygame.utils.AdRotator
import iss.nus.edu.sg.androidca.thememorygame.utils.GameTimer
import iss.nus.edu.sg.androidca.thememorygame.utils.SoundManager
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.IOException

class PlayActivity : AppCompatActivity(), AdapterView.OnItemClickListener {

    companion object {
        private const val REQUIRED_IMAGE_COUNT = 12
        private const val MATCH_REVEAL_DELAY = 800L
        private const val MAX_FLIPPED_CARDS = 2
    }

    private lateinit var adapter: MyCustomAdapter
    private lateinit var timer: GameTimer
    private lateinit var soundManager: SoundManager
    private lateinit var adRotator: AdRotator
    private lateinit var gridView: GridView
    private lateinit var matchesView: TextView

    private var gameStarted = false
    private var gameWon = false
    private var startTime = 0L
    private var elapsedMillis = 0L
    private var isDestroying = false

    private lateinit var flipOutRes: Animator
    private lateinit var flipInRes: Animator

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        supportActionBar?.hide()
        setContentView(R.layout.activity_play)

        initializeViews()
        applyInsets()
        initializeAnimators()
        initializeComponents()

        val filenames = getImageFilenames()
        if (filenames == null) {
            handleInvalidImageData()
            return
        }

        setupGame(filenames)
    }

    private fun initializeViews() {
        gridView = findViewById(R.id.playGridView)
        matchesView = findViewById(R.id.matches)
    }

    private fun applyInsets() {
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.play)) { view, insets ->
            val sys = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            view.setPadding(sys.left, sys.top, sys.right, sys.bottom)
            insets
        }
    }

    private fun initializeAnimators() {
        flipOutRes = AnimatorInflater.loadAnimator(this, R.animator.flip_out)
        flipInRes = AnimatorInflater.loadAnimator(this, R.animator.flip_in)
    }

    private fun initializeComponents() {
        timer = GameTimer(findViewById(R.id.timer))
        soundManager = SoundManager(this)
        adRotator = AdRotator(this, findViewById(R.id.ads))
        adRotator.start()
    }

    private fun getImageFilenames(): Array<String>? {
        val filenames = intent.getStringArrayExtra("filenames")
        return if (filenames != null && filenames.size == REQUIRED_IMAGE_COUNT) {
            filenames
        } else {
            null
        }
    }

    private fun handleInvalidImageData() {
        showToast("Invalid image data")
        finish()
    }

    private fun setupGame(filenames: Array<String>) {
        adapter = MyCustomAdapter(this, filenames)
        gridView.apply {
            adapter = this@PlayActivity.adapter
            onItemClickListener = this@PlayActivity
        }
    }

    override fun onResume() {
        super.onResume()
        if (!isDestroying) {
            soundManager.playBackground()
        }
    }

    override fun onPause() {
        super.onPause()
        soundManager.pauseBackground()
    }

    override fun onStop() {
        super.onStop()
        // Clear Glide safely when activity stops
        try {
            if (!isDestroying) {
                Glide.with(this).pauseRequests()
            }
        } catch (e: Exception) {
            // Ignore if already destroyed
        }
    }

    override fun onDestroy() {
        isDestroying = true

        // Stop components before calling super.onDestroy()
        try {
            // Pass the isDestroying flag to AdRotator to prevent Glide calls
            adRotator.stop(isDestroying)
            timer.cleanup()
            soundManager.cleanup()
        } catch (e: Exception) {
            // Log but don't crash
            e.printStackTrace()
        }

        super.onDestroy()
    }

    override fun onItemClick(parent: AdapterView<*>, view: View, position: Int, id: Long) {
        if (isDestroying || !isValidClick(view, position)) return

        startGameIfNeeded()
        handleCardClick(view, position)
    }

    private fun isValidClick(view: View, position: Int): Boolean {
        return !gameWon &&
                !isDestroying &&
                view.getTag(R.id.is_animating) != true &&
                adapter.getCurrentlyFlippedCount() < MAX_FLIPPED_CARDS
    }

    private fun startGameIfNeeded() {
        if (!gameStarted && !isDestroying) {
            gameStarted = true
            startTime = SystemClock.elapsedRealtime()
            timer.start(startTime)
        }
    }

    private fun handleCardClick(view: View, position: Int) {
        if (isDestroying) return

        val wasRevealed = adapter.revealPosition(position)
        if (wasRevealed) {
            view.setTag(R.id.is_animating, true)
            soundManager.playFlip()
            animateCardFlip(view)
        }
    }

    private fun animateCardFlip(view: View) {
        if (isDestroying) return

        val flipOutAnimator = inflateAnimator(flipOutRes, view)
        val flipInAnimator = inflateAnimator(flipInRes, view)

        view.setTag(R.id.is_animating, true)
        view.rotationY = 0f

        flipOutAnimator.addListener(object : AnimatorListenerAdapter() {
            override fun onAnimationEnd(animation: Animator) {
                if (!isDestroying) {
                    flipInAnimator.start()
                }
                flipOutAnimator.removeListener(this)
            }
        })

        flipInAnimator.addListener(object : AnimatorListenerAdapter() {
            override fun onAnimationEnd(animation: Animator) {
                if (!isDestroying) {
                    view.setTag(R.id.is_animating, false)

                    if (adapter.getCurrentlyFlippedCount() == MAX_FLIPPED_CARDS) {
                        processCardMatch()
                    }
                }
                flipInAnimator.removeListener(this)
            }
        })

        flipOutAnimator.start()
    }

    private fun inflateAnimator(base: Animator, target: View): Animator {
        val clone = base.clone()
        clone.setTarget(target)
        return clone
    }

    private fun processCardMatch() {
        if (isDestroying) return

        gridView.isEnabled = false

        if (adapter.checkForMatch()) {
            handleSuccessfulMatch()
        } else {
            handleFailedMatch()
        }

        checkForGameCompletion()
    }

    private fun handleSuccessfulMatch() {
        if (isDestroying) return

        soundManager.playMatch()
        adapter.finalizeMatch()
        updateMatchesDisplay()
        gridView.isEnabled = true
    }

    private fun handleFailedMatch() {
        if (isDestroying) return

        Handler(Looper.getMainLooper()).postDelayed({
            if (!isDestroying) {
                adapter.resetFlipped()
                gridView.isEnabled = true
            }
        }, MATCH_REVEAL_DELAY)
    }

    private fun updateMatchesDisplay() {
        if (isDestroying) return

        val matchedPairs = adapter.getRevealedCount() / 2
        matchesView.text = getString(R.string.matches_format, matchedPairs)
    }

    private fun checkForGameCompletion() {
        if (adapter.isGameComplete() && !gameWon && !isDestroying) {
            completeGame()
        }
    }

    private fun completeGame() {
        if (isDestroying) return

        gameWon = true
        timer.stop()
        adRotator.stop()
        elapsedMillis = SystemClock.elapsedRealtime() - startTime
        saveCompletionTime()
        navigateToLeaderboard()
    }

    private fun navigateToLeaderboard() {
        if (isDestroying) return

        val intent = Intent(this, LeaderBoardActivity::class.java).apply {
            putExtra("completion_time", elapsedMillis)
        }
        startActivity(intent)
        finish()
    }

    private fun saveCompletionTime() {
        Thread {
            try {
                val client = HttpClientProvider.client
                val jsonBody = "$elapsedMillis"
                val requestBody = jsonBody.toRequestBody("application/json".toMediaType())

                val request = Request.Builder()
                    .url(ApiConstants.SAVE_TIME)
                    .post(requestBody)
                    .addHeader("Content-Type", "application/json")
                    .build()

                client.newCall(request).enqueue(object : Callback {
                    override fun onFailure(call: Call, e: IOException) {
                        if (!isDestroying) {
                            runOnUiThread {
                                showToast("Error: ${e.message}")
                            }
                        }
                    }

                    override fun onResponse(call: Call, response: Response) {
                        if (!isDestroying) {
                            // Read the response body in the background thread
                            val result = response.body?.string().orEmpty()
                            val isSuccessful = response.isSuccessful
                            val responseCode = response.code

                            // Now switch to UI thread with the data already read
                            runOnUiThread {
                                handleSaveResponse(result, isSuccessful, responseCode)
                            }
                        }
                    }
                })
            } catch (e: Exception) {
                if (!isDestroying) {
                    runOnUiThread {
                        showToast("Error saving completion time")
                    }
                }
            }
        }.start()
    }

    private fun handleSaveResponse(result: String, isSuccessful: Boolean, responseCode: Int) {
        if (isDestroying) return

        val message = when {
            isSuccessful && result == "saved" -> "Completion time saved!"
            else -> "Error saving time: $responseCode"
        }
        showToast(message)
    }

    private fun showToast(message: String) {
        if (!isDestroying) {
            runOnUiThread {
                if (!isDestroying) {
                    Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
                }
            }
        }
    }
}