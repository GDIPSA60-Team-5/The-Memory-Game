package iss.nus.edu.sg.androidca.thememorygame

import android.annotation.SuppressLint

class TimeUtils {
    companion object {
        // Function to format milliseconds into minutes:seconds:centi-seconds format
        @SuppressLint("DefaultLocale")
        fun formatElapsedTime(elapsedMillis: Long): String {
            val centiSeconds = (elapsedMillis / 10).toInt()
            val seconds = centiSeconds / 100
            val minutes = seconds / 60
            val remainingSeconds = seconds % 60
            val remainingCentiSeconds = centiSeconds % 100

            return String.format("%02d:%02d:%02d", minutes, remainingSeconds, remainingCentiSeconds)
        }
    }
}
