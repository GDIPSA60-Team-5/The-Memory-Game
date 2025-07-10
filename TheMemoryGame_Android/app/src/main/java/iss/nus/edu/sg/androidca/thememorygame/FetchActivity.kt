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
    private val filenames = (1..20).map { "$it.jpg" }
    private lateinit var adapter: MyCustomAdapter
    private var bgThread: Thread? = null
    private var selectedPositions = mutableSetOf<Int>()

    private lateinit var gridView: GridView
    private lateinit var urlInput: EditText
    private lateinit var progressBar: ProgressBar
    private lateinit var progressText: TextView

    private fun initializeUI() {
        gridView = findViewById(R.id.imageGridView)
        urlInput = findViewById(R.id.url)
        progressBar = findViewById(R.id.progressBar)
        progressText = findViewById(R.id.progressTextView)

        adapter = MyCustomAdapter(this, filenames.toTypedArray())
        gridView.adapter = adapter
    }

    private fun getImageDir(): File? {
        return getExternalFilesDir(Environment.DIRECTORY_PICTURES)
    }

    @SuppressLint("SetTextI18n")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        supportActionBar?.hide()
        setContentView(R.layout.activity_fetch)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.fetch)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
        initializeUI()

        // Delete previously downloaded images on launch
        getImageDir()?.let { deleteExistingImages(it) }

        findViewById<Button>(R.id.btn).setOnClickListener {
            fetchImages()
        }

        gridView.onItemClickListener = this
    }

    private fun fetchImages() {
        resetBackgroundThread()
        bgThread = Thread {
            val imageDir = getImageDir() ?: return@Thread

            try {
                prepareForNewFetch(imageDir)

                val imageUrls = scrapeImageUrlsFromInput()
                showInitialDownloadUI(imageUrls.size)

                downloadAllImages(imageUrls, imageDir)

                onDownloadCompleted()

            } catch (e: Exception) {
                showDownloadError(e)
            }
        }
        bgThread?.start()
    }
    private fun showInitialDownloadUI(total: Int) {
        runOnUiThread {
            progressBar.apply {
                visibility = View.VISIBLE
                max = total
                progress = 0
            }
            progressText.text = "Starting download..."
            progressText.visibility = View.VISIBLE
        }
    }
    private fun prepareForNewFetch(directory: File) {
        deleteExistingImages(directory)
        runOnUiThread {
            adapter.notifyDataSetChanged()
            clearSelectionHighlights()
        }
    }
    @Throws(Exception::class)
    private fun scrapeImageUrlsFromInput(imageCount: Int = 20): List<String> {
        val pageUrl = urlInput.text.toString()
        val doc = Jsoup.connect(pageUrl).userAgent("Mozilla").get()
        return doc.select("img")
            .mapNotNull { it.absUrl("src") }
            .filter { it.startsWith("https") && !it.endsWith(".svg") }
            .distinct()
            .take(imageCount)
    }

    private fun downloadAllImages(imageUrls: List<String>, directory: File) {
        imageUrls.forEachIndexed { index, url ->
            val filename = "${index + 1}.jpg"
            val file = File(directory, filename)

            downloadToFile(url, file)

            runOnUiThread {
                progressBar.progress = index + 1
                progressText.text = "Downloading ${index + 1} of ${imageUrls.size} images..."
                adapter.notifyDataSetChanged()
            }
        }
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

    private fun onDownloadCompleted() {
        runOnUiThread {
            progressText.text = "Download completed!\nSelect 6 images to start the game."
            progressBar.visibility = View.GONE
        }
    }

    private fun showDownloadError(e: Exception) {
        e.printStackTrace()
        runOnUiThread {
            Toast.makeText(this, e.message ?: "Error occurred", Toast.LENGTH_LONG).show()
        }
    }

    private fun resetBackgroundThread() {
        bgThread?.interrupt()
        bgThread = null
    }

    private fun deleteExistingImages(directory: File) {
        directory.listFiles()?.forEach { if (it.isFile) it.delete() }
    }

    private fun clearSelectionHighlights() {
        selectedPositions.forEach { pos ->
            val child = gridView.getChildAt(pos)
            val imageView = child?.findViewById<ImageView>(R.id.imageView)
            val tickView = child?.findViewById<ImageView>(R.id.tickView)
            updateImageSelectionUI(imageView, tickView, false)
        }
        selectedPositions.clear()
    }

    override fun onItemClick(parent: AdapterView<*>?, view: View?, pos: Int, id: Long) {
        val imageView = view?.findViewById<ImageView>(R.id.imageView)
        val tickView = view?.findViewById<ImageView>(R.id.tickView)

        if (imageView?.tag == "placeholder") {
            Toast.makeText(this, "Please select only real images", Toast.LENGTH_SHORT).show()
            return
        }

        if (selectedPositions.contains(pos)) {
            selectedPositions.remove(pos)
            updateImageSelectionUI(imageView, tickView, false)
        } else {
            selectedPositions.add(pos)
            updateImageSelectionUI(imageView, tickView, true)

            if (selectedPositions.size == 6) {
                startGameWithSelectedImages()
            }
        }
    }

    private fun updateImageSelectionUI(imageView: ImageView?, tickView: ImageView?, isSelected: Boolean) {
        if (isSelected) {
            imageView?.alpha = 0.5f
            imageView?.scaleX = 1.1f
            imageView?.scaleY = 1.1f
            tickView?.visibility = View.VISIBLE
        } else {
            imageView?.alpha = 1.0f
            imageView?.scaleX = 1.0f
            imageView?.scaleY = 1.0f
            tickView?.visibility = View.GONE
        }
    }

    private fun startGameWithSelectedImages() {
        Toast.makeText(this, "Game is starting...", Toast.LENGTH_SHORT).show()
        val selectedFiles = selectedPositions.map { filenames[it] }.shuffled()
        val pairedFiles = (selectedFiles + selectedFiles).shuffled()
        val intent = Intent(this, PlayActivity::class.java).apply {
            putExtra("filenames", pairedFiles.toTypedArray())
        }
        startActivity(intent)
        finish()
    }
}