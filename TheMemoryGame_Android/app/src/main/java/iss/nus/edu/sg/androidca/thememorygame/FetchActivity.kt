package iss.nus.edu.sg.androidca.thememorygame

import android.annotation.SuppressLint
import android.content.Intent
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
import android.view.View
import android.widget.AdapterView
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import java.net.HttpURLConnection

class FetchActivity : AppCompatActivity(), AdapterView.OnItemClickListener {
    private val filenames = arrayOf("1.jpg", "2.jpg", "3.jpg", "4.jpg", "5.jpg", "6.jpg", "7.jpg", "8.jpg", "9.jpg", "10.jpg", "11.jpg", "12.jpg", "13.jpg",
        "14.jpg", "15.jpg", "16.jpg", "17.jpg", "18.jpg", "19.jpg", "20.jpg")
    private lateinit var adapter: MyCustomAdapter
    private var bgThread: Thread? = null
    private var selectedPositions = mutableSetOf<Int>()

    @SuppressLint("SetTextI18n")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_fetch)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.fetch)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
        val fetch = findViewById<Button>(R.id.btn)
        val gridView = findViewById<GridView>(R.id.imageGridView)
        val dir = getExternalFilesDir(Environment.DIRECTORY_PICTURES)
        val url = findViewById<EditText>(R.id.url)

        // Create an adapter object first
        adapter = MyCustomAdapter(this, filenames)
        gridView.adapter = adapter

        // Delete previously downloaded objects when this app is first launched
        if (dir != null) {
            deleteExistingImages(dir)
        }

        fetch.setOnClickListener {
            resetBackgroundThread()
            bgThread = Thread {
                // Delete images when images are fetched from new url
                if (dir != null) {
                    deleteExistingImages(dir)
                }

                // Download images from url
                try {
                    val doc = Jsoup.connect(url.text.toString())
                        .userAgent("Mozilla")
                        .get()
                    val imgElements = doc.select("img")
                    val httpsImgSrcs = mutableListOf<String>()
                    val progressBar = findViewById<ProgressBar>(R.id.progressBar)
                    val progressText = findViewById<TextView>(R.id.progressTextView)

                    // Scrape 20 image links from the html tag that starts with "src"
                    for (element in imgElements) {
                        val imgSrc = element.absUrl("src")
                        if (imgSrc.startsWith("https") && !imgSrc.endsWith(".svg") && !httpsImgSrcs.contains(imgSrc)) {
                            httpsImgSrcs.add(imgSrc)
                            if (httpsImgSrcs.size >= 20) break
                        }
                    }

                    val totalImages = httpsImgSrcs.size
                    // Show progress bar when downloading
                    runOnUiThread {
                        progressText.visibility = View.VISIBLE
                        progressBar.visibility = View.VISIBLE
                        progressBar.max = totalImages
                        progressBar.progress = 0
                    }

                    // Make file with specified names (eg. 1.jpg, 2.jpg) and download images to the created files
                    for (i in 0 until totalImages) {
                        val fileName = "${i+1}.jpg"
                        val file = makeFile(fileName)

                        downloadToFile(httpsImgSrcs[i], file)

                        // Show the images and update progress bar while downloading
                        runOnUiThread {
                            adapter.notifyDataSetChanged()
                            progressText.text = "Downloading ${i+1} of $totalImages images..."
                            progressBar.progress = i + 1

                            for (j: Int in selectedPositions) {
                                val child = gridView.getChildAt(j)
                                val imageView = child.findViewById<ImageView>(R.id.imageView)
                                val tickView = child.findViewById<ImageView>(R.id.tickView)

                                imageView.alpha = 1.0f
                                imageView.scaleX = 1.0f
                                imageView.scaleY = 1.0f
                                tickView?.visibility = View.GONE
                            }
                            selectedPositions.clear()
                        }
                    }
                    // Change the text view after download is completed
                    runOnUiThread {
                        progressText.text = "Download completed! \nSelect 6 images to start the game."
                        progressBar.visibility = View.GONE
                    }
                } catch(e: Exception) {
                    e.printStackTrace()
                    runOnUiThread{
                        Toast.makeText(this, e.message ?: "An error occurred", Toast.LENGTH_LONG).show()
                    }
                }
            }
            bgThread?.start()
        }
        // Listen to user selection on 6 images to play
        gridView.onItemClickListener = this
    }

    // Create file to store image download
    private fun makeFile(filename: String) : File {
        val dir = getExternalFilesDir(Environment.DIRECTORY_PICTURES)
        return File(dir, filename)
    }

    // Download images
    private fun downloadToFile(url: String, file: File) {
        val connection = URL(url).openConnection() as HttpURLConnection
        connection.setRequestProperty("User-Agent", "Mozilla")

        connection.inputStream.use { input ->
            file.outputStream().use { output ->
                input.copyTo(output)
            }
        }
    }

    // Delete existing images
    private fun deleteExistingImages(directory: File) {
        directory.listFiles()?.forEach { file ->
            if (file.isFile) {
                file.delete()
            }
        }
    }

    // If interrupted during fetching images, reset the fetch process
    private fun resetBackgroundThread() {
        bgThread?.interrupt()
        bgThread = null
    }

    // Tick and opaque the images if being clicked
    override fun onItemClick(parent: AdapterView<*>?, view: View?, pos: Int, id: Long) {
        val imageView = view?.findViewById<ImageView>(R.id.imageView)
        val tickView = view?.findViewById<ImageView>(R.id.tickView)

        // If image is placeholder image, then do not allow clicking on it
        if (imageView?.tag == "placeholder") {
            Toast.makeText(this, "Please select only real images", Toast.LENGTH_SHORT).show()
            return
        }

        // If image is already selected, clicking again will deselect the image
        if (selectedPositions.contains(pos)) {
            selectedPositions.remove(pos)
            imageView?.alpha = 1.0f
            imageView?.scaleX = 1.0f
            imageView?.scaleY = 1.0f
            tickView?.visibility = View.GONE
        } else {
            // Select the image
            selectedPositions.add(pos)
            imageView?.alpha = 0.5f
            imageView?.scaleX = 1.1f
            imageView?.scaleY = 1.1f
            tickView?.visibility = View.VISIBLE

            // If images clicked are 6, then start the play activity
            if (selectedPositions.size == 6) {
                Toast.makeText(this, "Game is starting...", Toast.LENGTH_SHORT).show()
                // get file names of the selected positions
                val selectedFileNames = selectedPositions.map { filenames[it] }.toList().shuffled()
                val pairedFileNames = (selectedFileNames + selectedFileNames).shuffled()
                val intent = Intent(this, PlayActivity::class.java)
                intent.putExtra("filenames", pairedFileNames.toTypedArray())
                // play game
                startActivity(intent)
                finish()
                return
            }
        }
    }
}