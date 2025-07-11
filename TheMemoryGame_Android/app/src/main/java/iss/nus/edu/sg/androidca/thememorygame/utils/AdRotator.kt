package iss.nus.edu.sg.androidca.thememorygame.utils

import android.app.Activity
import android.content.Context
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.widget.ImageView
import com.bumptech.glide.Glide
import iss.nus.edu.sg.androidca.thememorygame.api.ApiConstants
import iss.nus.edu.sg.androidca.thememorygame.api.HttpClientProvider
import okhttp3.Request
import org.jsoup.Jsoup
import java.io.File
import java.net.HttpURLConnection
import java.net.URL

class AdRotator(
    private val context: Context,
    private val imageView: ImageView,
    private val rotationInterval: Long = 30_000L
) {
    private val handler = Handler(Looper.getMainLooper())
    private var rotationRunnable: Runnable? = null
    private var localAdFiles: List<File> = emptyList()
    private var isActive = false

    fun start() {
        if (isActive) return
        isActive = true

        Thread {
            val canSeeAds = checkCanSeeAds()
            handler.post {
                if (canSeeAds) {
                    imageView.visibility = ImageView.VISIBLE
                    loadAdsAndStart()
                } else {
                    imageView.visibility = ImageView.GONE
                }
            }
        }.start()
    }

    private fun checkCanSeeAds(): Boolean {
        return try {
            val request = Request.Builder()
                .url(ApiConstants.CAN_SEE_ADS)
                .get()
                .build()

            val response = HttpClientProvider.client.newCall(request).execute()
            val body = response.body?.string()?.trim()

            Log.d("AdRotator", "Ads visibility check response: $body")

            body == "true" || body == "\"true\""
        } catch (e: Exception) {
            Log.e("AdRotator", "Failed to check ads visibility", e)
            false
        }
    }

    private fun loadAdsAndStart() {
        Thread {
            try {
                val doc = Jsoup.connect(ApiConstants.HOST).userAgent("Mozilla").get()
                val adImageUrls = TimeUtils.scrapeImages(doc.select("img"), 11)
                val adFilenames = (1..11).map { "ad_${if (it < 10) "0$it" else it}.png" }

                localAdFiles = adFilenames.mapIndexed { index, filename ->
                    val file = TimeUtils.makeFile(context, filename)
                    TimeUtils.downloadToFile(adImageUrls[index], file)
                    file
                }

                if (isActive) startRotation()
            } catch (e: Exception) {
                Log.e("AdRotator", "Failed to load ads", e)
            }
        }.start()
    }

    private fun startRotation() {
        rotationRunnable = object : Runnable {
            override fun run() {
                if (!isActive || localAdFiles.isEmpty()) return

                if (context is Activity && (context.isFinishing || context.isDestroyed)) {
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

                if (isActive) handler.postDelayed(this, rotationInterval)
            }
        }
        handler.post(rotationRunnable!!)
    }

    fun stop(isDestroying: Boolean = false) {
        isActive = false
        handler.removeCallbacksAndMessages(null)

        if (!isDestroying) {
            try {
                Glide.with(imageView.context).clear(imageView)
            } catch (e: Exception) {
                Log.e("AdRotator", "Failed to clear image", e)
            }
        }

        imageView.visibility = ImageView.GONE
    }
}