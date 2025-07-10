package iss.nus.edu.sg.androidca.thememorygame

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
import iss.nus.edu.sg.androidca.thememorygame.api.ApiConstants
import iss.nus.edu.sg.androidca.thememorygame.api.HttpClientProvider
import iss.nus.edu.sg.androidca.thememorygame.utils.AdRotator
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
        soundManager.playBackground()
    }

    override fun onPause() {
        super.onPause()
        soundManager.pauseBackground()
    }

    override fun onDestroy() {
        super.onDestroy()
        cleanup()
    }

    private fun cleanup() {
        timer.cleanup()
        soundManager.cleanup()
        adRotator.stop()
    }

    override fun onItemClick(parent: AdapterView<*>, view: View, position: Int, id: Long) {
        if (!isValidClick(view, position)) return

        startGameIfNeeded()
        handleCardClick(view, position)
    }

    private fun isValidClick(view: View, position: Int): Boolean {
        return !gameWon &&
                view.getTag(R.id.is_animating) != true &&
                adapter.getCurrentlyFlippedCount() < MAX_FLIPPED_CARDS
    }

    private fun startGameIfNeeded() {
        if (!gameStarted) {
            gameStarted = true
            startTime = SystemClock.elapsedRealtime()
            timer.start(startTime)
        }
    }

    private fun handleCardClick(view: View, position: Int) {
        val wasRevealed = adapter.revealPosition(position)
        if (wasRevealed) {
            view.setTag(R.id.is_animating, true)
            soundManager.playFlip()
            animateCardFlip(view)
        }
    }

    private fun animateCardFlip(view: View) {
        val flipOutAnimator = inflateAnimator(flipOutRes, view)
        val flipInAnimator = inflateAnimator(flipInRes, view)

        view.setTag(R.id.is_animating, true)
        view.rotationY = 0f

        flipOutAnimator.addListener(object : AnimatorListenerAdapter() {
            override fun onAnimationEnd(animation: Animator) {
                flipInAnimator.start()
                flipOutAnimator.removeListener(this)
            }
        })

        flipInAnimator.addListener(object : AnimatorListenerAdapter() {
            override fun onAnimationEnd(animation: Animator) {
                view.setTag(R.id.is_animating, false)

                if (adapter.getCurrentlyFlippedCount() == MAX_FLIPPED_CARDS) {
                    processCardMatch()
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
        gridView.isEnabled = false

        if (adapter.checkForMatch()) {
            handleSuccessfulMatch()
        } else {
            handleFailedMatch()
        }

        checkForGameCompletion()
    }

    private fun handleSuccessfulMatch() {
        soundManager.playMatch()
        adapter.finalizeMatch()
        updateMatchesDisplay()
        gridView.isEnabled = true
    }

    private fun handleFailedMatch() {
        Handler(Looper.getMainLooper()).postDelayed({
            adapter.resetFlipped()
            gridView.isEnabled = true
        }, MATCH_REVEAL_DELAY)
    }

    private fun updateMatchesDisplay() {
        val matchedPairs = adapter.getRevealedCount() / 2
        matchesView.text = getString(R.string.matches_format, matchedPairs)
    }

    private fun checkForGameCompletion() {
        if (adapter.isGameComplete() && !gameWon) {
            completeGame()
        }
    }

    private fun completeGame() {
        gameWon = true
        timer.stop()
        adRotator.stop()
        elapsedMillis = SystemClock.elapsedRealtime() - startTime
        saveCompletionTime()
        navigateToLeaderboard()
    }

    private fun navigateToLeaderboard() {
        val intent = Intent(this, LeaderBoardActivity::class.java).apply {
            putExtra("completion_time", elapsedMillis)
        }
        startActivity(intent)
        finish()
    }

    private fun saveCompletionTime() {
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
                showToast("Error: ${e.message}")
            }

            override fun onResponse(call: Call, response: Response) {
                handleSaveResponse(response)
            }
        })
    }

    private fun handleSaveResponse(response: Response) {
        val result = response.body?.string().orEmpty()
        val message = when {
            response.isSuccessful && result == "saved" -> "Completion time saved!"
            else -> "Error saving time: ${response.code}"
        }
        showToast(message)
    }

    private fun showToast(message: String) {
        runOnUiThread {
            Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
        }
    }
}
