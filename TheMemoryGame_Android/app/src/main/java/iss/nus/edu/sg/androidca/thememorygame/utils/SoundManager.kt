package iss.nus.edu.sg.androidca.thememorygame.utils

import android.content.Context
import android.media.MediaPlayer
import android.media.SoundPool
import iss.nus.edu.sg.androidca.thememorygame.R

class SoundManager(context: Context) {

    private val bgPlayer: MediaPlayer = MediaPlayer.create(context, R.raw.bg_music).apply {
        isLooping = true
    }

    private val soundPool: SoundPool = SoundPool.Builder()
        .setMaxStreams(4)
        .build()

    private val flipId: Int = soundPool.load(context, R.raw.flip, 1)
    private val matchId: Int = soundPool.load(context, R.raw.match, 1)

    fun playBackground() {
        if (!bgPlayer.isPlaying) {
            bgPlayer.start()
        }
    }

    fun pauseBackground() {
        bgPlayer.pause()
    }

    fun playFlip() {
        soundPool.play(flipId, 1f, 1f, 1, 0, 1f)
    }

    fun playMatch() {
        soundPool.play(matchId, 1f, 1f, 1, 0, 1f)
    }

    fun cleanup() {
        soundPool.release()
        bgPlayer.release()
    }
}
