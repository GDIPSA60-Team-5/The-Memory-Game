package iss.nus.edu.sg.androidca.thememorygame.utils

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
    private val rotationInterval: Long = 30000L
) {
    private val adHandler = Handler(Looper.getMainLooper())
    private var adRunnable: Runnable? = null
    private var localFiles: List<File> = emptyList()

    fun start() {
        imageView.visibility = ImageView.VISIBLE
        Thread {
            try {
                val doc = Jsoup.connect(ApiConstants.HOST)
                    .userAgent("Mozilla")
                    .get()
                val imgElements = doc.select("img")
                val httpsImgSrcs = TimeUtils.scrapeImages(imgElements, 11)

                val ads = (1..11).map {
                    if (it < 10) "ad_0$it.png" else "ad_$it.png"
                }

                localFiles = ads.mapIndexed { index, filename ->
                    val file = TimeUtils.makeFile(context, filename)
                    TimeUtils.downloadToFile(httpsImgSrcs[index], file)
                    file
                }

                Log.d("AdRotator", "Downloaded ads: $localFiles")
                startRotation()

            } catch (e: Exception) {
                Log.e("AdRotator", "Failed to load ads", e)
            }
        }.start()
    }

    private fun startRotation() {
        adRunnable = object : Runnable {
            override fun run() {
                if (localFiles.isEmpty()) return

                val randomFile = localFiles[Random.nextInt(localFiles.size)]
                if (randomFile.exists()) {
                    Glide.with(context)
                        .load(randomFile)
                        .into(imageView)
                }
                adHandler.postDelayed(this, rotationInterval)
            }
        }
        adHandler.post(adRunnable!!)
    }

    fun stop() {
        adRunnable?.let { adHandler.removeCallbacks(it) }
        Glide.with(context).clear(imageView)
    }
}
