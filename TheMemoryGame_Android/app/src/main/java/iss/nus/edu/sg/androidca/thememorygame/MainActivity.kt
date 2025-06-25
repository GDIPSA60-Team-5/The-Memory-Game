package iss.nus.edu.sg.androidca.thememorygame

import android.os.Bundle
import android.os.Environment
import android.widget.Button
import android.widget.EditText
import android.widget.GridView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import org.jsoup.Jsoup
import java.io.File
import java.net.URL
import android.util.Log
import java.net.HttpURLConnection

class MainActivity : AppCompatActivity() {
    val filenames = arrayOf("1.jpg", "2.jpg", "3.jpg", "4.jpg", "5.jpg", "6.jpg", "7.jpg", "8.jpg", "9.jpg", "10.jpg", "11.jpg", "12.jpg", "13.jpg",
        "14.jpg", "15.jpg", "16.jpg", "17.jpg", "18.jpg", "19.jpg", "20.jpg")
    lateinit var adapter: MyCustomAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
        val fetch = findViewById<Button>(R.id.btn)
        val gridView = findViewById<GridView>(R.id.imageGridView)
        val dir = getExternalFilesDir(Environment.DIRECTORY_PICTURES)
        val url = findViewById<EditText>(R.id.url)

        adapter = MyCustomAdapter(this, filenames)
        gridView.adapter = adapter

        dir?.listFiles()?.forEach { file ->
            if (file.isFile) {
                file.delete()
            }
        }

        fetch.setOnClickListener {
            Thread {
                dir?.listFiles()?.forEach { file ->
                    if (file.isFile) {
                        file.delete()
                    }
                }
                try {
                    val doc = Jsoup.connect(url.text.toString())
                        .userAgent("Mozilla")
                        .get()
                    val imgElements = doc.select("img")
                    val httpsImgSrcs = mutableListOf<String>()

                    for (element in imgElements) {
                        val imgSrc = element.absUrl("src")
                        if (imgSrc.startsWith("https") && !imgSrc.endsWith(".svg")  && !httpsImgSrcs.contains(imgSrc)) {
                            httpsImgSrcs.add(imgSrc)
                            if (httpsImgSrcs.size > 20) break
                        }
                    }
                    for (i in 0 until httpsImgSrcs.size) {
                        val fileName = "${i+1}.jpg"
                        val file = makeFile(fileName)

                        downloadToFile(httpsImgSrcs[i], file)
                        Log.d("PostDownload", "Download function is called.")

                        runOnUiThread {
                            adapter.notifyDataSetChanged()
                            Log.d("adapter notifyDataSetChanged", "this function is called properly!")
                        }
                    }
                } catch(e: Exception) {
                    e.printStackTrace()
                }
            }.start()
        }
    }

    private fun makeFile(fname: String) : File {
        val dir = getExternalFilesDir(Environment.DIRECTORY_PICTURES)
        Log.d("makeFile fun", "makeFile works!")
        return File(dir, fname)
    }

    private fun downloadToFile(url: String, file: File) {
        val connection = URL(url).openConnection() as HttpURLConnection
        connection.setRequestProperty("User-Agent", "Mozilla")

        connection.inputStream.use { input ->
            file.outputStream().use { output ->
                input.copyTo(output)
            }
        }
    }
}