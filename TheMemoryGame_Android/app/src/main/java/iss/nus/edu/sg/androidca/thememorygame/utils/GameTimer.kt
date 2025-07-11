package iss.nus.edu.sg.androidca.thememorygame.utils

import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import android.widget.TextView

class GameTimer(private val timerView: TextView) {
    private val handler = Handler(Looper.getMainLooper())
    private var runnable: Runnable? = null

    fun start(startTime: Long) {
        runnable = object : Runnable {
            override fun run() {
                val elapsed = SystemClock.elapsedRealtime() - startTime
                timerView.text = TimeUtils.formatElapsedTime(elapsed)
                handler.postDelayed(this, 10)
            }
        }
        handler.post(runnable!!)
    }

    fun stop() {
        handler.removeCallbacks(runnable!!)
    }

    fun cleanup() = stop()
}
