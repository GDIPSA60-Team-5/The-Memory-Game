package iss.nus.edu.sg.androidca.thememorygame.utils

import android.annotation.SuppressLint
import android.content.Context
import android.os.Environment
import org.jsoup.select.Elements
import java.io.File
import java.net.HttpURLConnection
import java.net.URL


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

        fun downloadToFile(url: String, file: File) {
            val connection = URL(url).openConnection() as HttpURLConnection
            connection.setRequestProperty("User-Agent", "Mozilla")

            connection.inputStream.use { input ->
                file.outputStream().use { output ->
                    input.copyTo(output)
                }
            }
        }

        fun makeFile(context: Context, filename: String) : File {
            val dir = context.getExternalFilesDir(Environment.DIRECTORY_PICTURES)
            return File(dir, filename)
        }

        fun scrapeImages(imgElements: Elements, amount: Int): MutableList<String> {
            val imgSrcs = mutableListOf<String>()
            for (element in imgElements) {
                val imgSrc = element.absUrl("src")
                if ((imgSrc.startsWith("http") || imgSrc.startsWith("https"))
                    && !imgSrc.endsWith(".svg")
                    && !imgSrcs.contains(imgSrc)
                ) {
                    imgSrcs.add(imgSrc)
                    if (imgSrcs.size >= amount) break
                }
            }
            return imgSrcs
        }

    }
}
