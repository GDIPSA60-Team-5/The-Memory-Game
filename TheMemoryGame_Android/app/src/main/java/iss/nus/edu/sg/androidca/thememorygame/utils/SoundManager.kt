package iss.nus.edu.sg.androidca.thememorygame.utils

import android.content.Context
import android.media.MediaPlayer
import android.media.SoundPool
import android.os.Handler
import android.os.Looper
import android.util.Log
import iss.nus.edu.sg.androidca.thememorygame.R

class SoundManager(context: Context) {

    private val bgPlayer: MediaPlayer = MediaPlayer.create(context, R.raw.bg_music).apply {
        isLooping = true
        setOnErrorListener { mp, what, extra ->
            Log.e("SoundManager", "MediaPlayer error: $what, $extra")
            false
        }
    }

    private val soundPool: SoundPool = SoundPool.Builder()
        .setMaxStreams(4)
        .build()

    private var flipId: Int = -1
    private var matchId: Int = -1
    private var soundsLoaded = false

    private var lastFlipTime = 0L
    private var lastMatchTime = 0L
    private val soundCooldown = 100L

    private val handler = Handler(Looper.getMainLooper())

    init {
        soundPool.setOnLoadCompleteListener { _, sampleId, status ->
            if (status == 0) {
                Log.d("SoundManager", "Sound loaded successfully: $sampleId")
                checkAllSoundsLoaded()
            } else {
                Log.e("SoundManager", "Failed to load sound: $sampleId, status: $status")
            }
        }

        flipId = soundPool.load(context, R.raw.flip, 1)
        matchId = soundPool.load(context, R.raw.match, 1)
    }

    private fun checkAllSoundsLoaded() {
        soundsLoaded = true
    }

    fun playBackground() {
        try {
            if (!bgPlayer.isPlaying) {
                bgPlayer.start()
            }
        } catch (e: Exception) {
            Log.e("SoundManager", "Error playing background music", e)
        }
    }

    fun pauseBackground() {
        try {
            if (bgPlayer.isPlaying) {
                bgPlayer.pause()
            }
        } catch (e: Exception) {
            Log.e("SoundManager", "Error pausing background music", e)
        }
    }

    fun playFlip() {
        val currentTime = System.currentTimeMillis()
        if (currentTime - lastFlipTime < soundCooldown) {
            return
        }

        if (soundsLoaded && flipId != -1) {
            try {
                soundPool.play(flipId, 1f, 1f, 1, 0, 1f)
                lastFlipTime = currentTime
            } catch (e: Exception) {
                Log.e("SoundManager", "Error playing flip sound", e)
            }
        }
    }

    fun playMatch() {
        val currentTime = System.currentTimeMillis()
        if (currentTime - lastMatchTime < soundCooldown) {
            return
        }

        if (soundsLoaded && matchId != -1) {
            try {
                soundPool.play(matchId, 1f, 1f, 1, 0, 1f)
                lastMatchTime = currentTime
            } catch (e: Exception) {
                Log.e("SoundManager", "Error playing match sound", e)
            }
        }
    }

    fun cleanup() {
        try {
            soundPool.release()
            bgPlayer.release()
        } catch (e: Exception) {
            Log.e("SoundManager", "Error during cleanup", e)
        }
    }
}