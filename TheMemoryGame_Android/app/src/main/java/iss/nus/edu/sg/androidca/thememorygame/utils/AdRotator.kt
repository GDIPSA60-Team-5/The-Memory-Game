package iss.nus.edu.sg.androidca.thememorygame.utils

import android.app.Activity
import android.content.Context
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.widget.ImageView
import com.bumptech.glide.Glide
import iss.nus.edu.sg.androidca.thememorygame.api.ApiConstants
import org.jsoup.Jsoup
import java.io.File
import kotlin.random.Random

class AdRotator(
    private val context: Context,
    private val imageView: ImageView,
    private val rotationInterval: Long = 30_000L
) {
    private val handler = Handler(Looper.getMainLooper())
    private var rotationRunnable: Runnable? = null
    private var localAdFiles: List<File> = emptyList()
    private var isActive = false

    /**
     * Starts downloading ads and begins rotation if not already started.
     */
    fun start() {
        if (isActive) return // Avoid multiple starts

        isActive = true
        imageView.visibility = ImageView.VISIBLE

        Thread {
            try {
                // Fetch ad image URLs from server
                val doc = Jsoup.connect(ApiConstants.HOST)
                    .userAgent("Mozilla")
                    .get()

                val imgElements = doc.select("img")
                val adImageUrls = TimeUtils.scrapeImages(imgElements, 11)

                // Prepare local filenames
                val adFilenames = (1..11).map { index ->
                    if (index < 10) "ad_0$index.png" else "ad_$index.png"
                }

                // Download images to local files
                localAdFiles = adFilenames.mapIndexed { index, filename ->
                    val file = TimeUtils.makeFile(context, filename)
                    TimeUtils.downloadToFile(adImageUrls[index], file)
                    file
                }

                Log.d("AdRotator", "Downloaded ads: $localAdFiles")

                // Start the ad rotation only if still active
                if (isActive) {
                    startRotation()
                }
            } catch (e: Exception) {
                Log.e("AdRotator", "Failed to load ads", e)
            }
        }.start()
    }

    /**
     * Starts the ad rotation Runnable that cycles ads at [rotationInterval].
     */
    private fun startRotation() {
        rotationRunnable = object : Runnable {
            override fun run() {
                if (!isActive || localAdFiles.isEmpty()) return

                // Check if context is valid before loading
                if (context is Activity && (context.isFinishing || context.isDestroyed)) {
                    Log.d("AdRotator", "Activity is finishing or destroyed, stopping rotation")
                    stop()
                    return
                }

                try {
                    val randomFile = localAdFiles.random()
                    if (randomFile.exists()) {
                        Glide.with(imageView.context)
                            .load(randomFile)
                            .skipMemoryCache(true)
                            .into(imageView)
                    }
                } catch (e: Exception) {
                    Log.e("AdRotator", "Error loading ad image", e)
                }

                if (isActive) {
                    handler.postDelayed(this, rotationInterval)
                }
            }
        }
        handler.post(rotationRunnable!!)
    }

    /**
     * Stops the ad rotation and clears resources safely.
     * @param isDestroying If true, avoids Glide usage to prevent loading into a destroyed context.
     */
    fun stop(isDestroying: Boolean = false) {
        isActive = false

        // Remove all scheduled tasks
        handler.removeCallbacksAndMessages(null)

        // Attempt to safely clear the ad image
        if (!isDestroying) {
            try {
                Glide.with(imageView.context).clear(imageView)
            } catch (e: Exception) {
                Log.e("AdRotator", "Failed to clear image with Glide", e)
            }
        }

        imageView.visibility = ImageView.GONE
    }
}
