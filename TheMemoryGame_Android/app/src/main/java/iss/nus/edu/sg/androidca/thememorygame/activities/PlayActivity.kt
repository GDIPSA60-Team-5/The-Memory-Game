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

    // UI Components
    private lateinit var gridView: GridView
    private lateinit var matchesView: TextView

    // Game Components
    private lateinit var adapter: MyCustomAdapter
    private lateinit var timer: GameTimer
    private lateinit var soundManager: SoundManager
    private lateinit var adRotator: AdRotator

    // Animation Resources
    private lateinit var flipOutAnimator: Animator
    private lateinit var flipInAnimator: Animator

    // Game State
    private var gameStarted = false
    private var gameWon = false
    private var startTime = 0L
    private var elapsedMillis = 0L
    private var isDestroying = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        supportActionBar?.hide()
        setContentView(R.layout.activity_play)

        initializeViews()
        applyWindowInsets()
        initializeAnimators()
        initializeGameComponents()

        val filenames = getValidatedImageFilenames()
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

    private fun applyWindowInsets() {
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.play)) { view, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            view.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
    }

    private fun initializeAnimators() {
        flipOutAnimator = AnimatorInflater.loadAnimator(this, R.animator.flip_out)
        flipInAnimator = AnimatorInflater.loadAnimator(this, R.animator.flip_in)
    }

    private fun initializeGameComponents() {
        timer = GameTimer(findViewById(R.id.timer))
        soundManager = SoundManager(this)
        adRotator = AdRotator(this, findViewById(R.id.ads))
        adRotator.start()
    }

    private fun getValidatedImageFilenames(): Array<String>? {
        val filenames = intent.getStringArrayExtra("filenames")
        return if (filenames?.size == REQUIRED_IMAGE_COUNT) filenames else null
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
        pauseGlideRequestsSafely()
    }

    override fun onDestroy() {
        isDestroying = true
        cleanupComponents()
        super.onDestroy()
    }

    private fun pauseGlideRequestsSafely() {
        try {
            if (!isDestroying) {
                Glide.with(this).pauseRequests()
            }
        } catch (e: Exception) {
            // Ignore if already destroyed
        }
    }

    private fun cleanupComponents() {
        try {
            adRotator.stop(isDestroying)
            timer.cleanup()
            soundManager.cleanup()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    override fun onItemClick(parent: AdapterView<*>, view: View, position: Int, id: Long) {
        if (shouldIgnoreClick(view)) return

        startGameIfNeeded()
        handleCardClick(view, position)
    }

    private fun shouldIgnoreClick(view: View): Boolean {
        return isDestroying ||
                gameWon ||
                isViewAnimating(view) ||
                adapter.getCurrentlyFlippedCount() >= MAX_FLIPPED_CARDS
    }

    private fun isViewAnimating(view: View): Boolean {
        return view.getTag(R.id.is_animating) == true
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
            markViewAsAnimating(view)
            soundManager.playFlip()
            animateCardFlip(view)
        }
    }

    private fun markViewAsAnimating(view: View) {
        view.setTag(R.id.is_animating, true)
    }

    private fun animateCardFlip(view: View) {
        if (isDestroying) return

        val flipOut = createAnimatorFor(flipOutAnimator, view)
        val flipIn = createAnimatorFor(flipInAnimator, view)

        view.rotationY = 0f

        flipOut.addListener(object : AnimatorListenerAdapter() {
            override fun onAnimationEnd(animation: Animator) {
                flipOut.removeListener(this)
                if (!isDestroying) {
                    flipIn.start()
                }
            }
        })

        flipIn.addListener(object : AnimatorListenerAdapter() {
            override fun onAnimationEnd(animation: Animator) {
                flipIn.removeListener(this)
                if (!isDestroying) {
                    view.setTag(R.id.is_animating, false)

                    if (adapter.getCurrentlyFlippedCount() == MAX_FLIPPED_CARDS) {
                        processCardMatch()
                    }
                }
            }
        })

        flipOut.start()
    }

    private fun createAnimatorFor(baseAnimator: Animator, targetView: View): Animator {
        val clone = baseAnimator.clone()
        clone.setTarget(targetView)
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
        if (canCompleteGame()) {
            completeGame()
        }
    }

    private fun canCompleteGame(): Boolean {
        return adapter.isGameComplete() && !gameWon && !isDestroying
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
                val request = createSaveTimeRequest()
                executeApiRequest(request)
            } catch (e: Exception) {
                handleSaveError("Error saving completion time")
            }
        }.start()
    }

    private fun createSaveTimeRequest(): Request {
        val jsonBody = "$elapsedMillis"
        val requestBody = jsonBody.toRequestBody("application/json".toMediaType())

        return Request.Builder()
            .url(ApiConstants.SAVE_TIME)
            .post(requestBody)
            .addHeader("Content-Type", "application/json")
            .build()
    }

    private fun executeApiRequest(request: Request) {
        val client = HttpClientProvider.client

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                handleSaveError("Error: ${e.message}")
            }

            override fun onResponse(call: Call, response: Response) {
                if (!isDestroying) {
                    val responseBody = response.body?.string().orEmpty()
                    val isSuccessful = response.isSuccessful
                    val responseCode = response.code

                    runOnUiThread {
                        handleSaveResponse(responseBody, isSuccessful, responseCode)
                    }
                }
            }
        })
    }

    private fun handleSaveError(message: String) {
        if (!isDestroying) {
            runOnUiThread {
                showToast(message)
            }
        }
    }

    private fun handleSaveResponse(responseBody: String, isSuccessful: Boolean, responseCode: Int) {
        if (isDestroying) return

        val message = if (isSuccessful && responseBody == "saved") {
            "Completion time saved!"
        } else {
            "Error saving time: $responseCode"
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