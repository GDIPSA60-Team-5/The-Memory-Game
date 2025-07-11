package iss.nus.edu.sg.androidca.thememorygame.activities

import android.content.Intent
import android.os.Bundle
import android.os.Environment
import android.view.View
import android.widget.*
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import iss.nus.edu.sg.androidca.thememorygame.MyCustomAdapter
import iss.nus.edu.sg.androidca.thememorygame.R
import org.jsoup.Jsoup
import java.io.File
import java.net.HttpURLConnection
import java.net.URL
import androidx.core.graphics.toColorInt

class FetchActivity : AppCompatActivity(), AdapterView.OnItemClickListener {

    companion object {
        private const val TOTAL_IMAGES = 20
        private const val REQUIRED_SELECTIONS = 6
        private const val ANIMATION_DURATION = 300L
    }

    // Data
    private val filenames = (1..TOTAL_IMAGES).map { "$it.jpg" }
    private val selectedPositions = mutableSetOf<Int>()

    // UI Components
    private lateinit var gridView: GridView
    private lateinit var urlInput: EditText
    private lateinit var progressBar: ProgressBar
    private lateinit var progressText: TextView
    private lateinit var progressOverlay: View
    private lateinit var adapter: MyCustomAdapter
    private lateinit var fetchButton: Button

    // Thread
    private var downloadThread: Thread? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        supportActionBar?.hide()
        setContentView(R.layout.activity_fetch)

        setupWindowInsets()
        initializeUIComponents()
        setupAdapter()
        setupEventListeners()
        cleanupExistingImages(getImageDirectory())
    }

    private fun setupWindowInsets() {
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.mainContainer)) { view, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            view.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
    }

    private fun initializeUIComponents() {
        gridView = findViewById(R.id.imageGridView)
        urlInput = findViewById(R.id.url)
        progressBar = findViewById(R.id.progressBar)
        progressText = findViewById(R.id.progressTextView)
        progressOverlay = findViewById(R.id.progressOverlay)
        fetchButton = findViewById(R.id.btn)
    }

    private fun setupAdapter() {
        adapter = MyCustomAdapter(this, filenames.toTypedArray())
        gridView.adapter = adapter
    }

    private fun setupEventListeners() {
        fetchButton.setOnClickListener { startImageDownload() }
        gridView.onItemClickListener = this
    }

    private fun getImageDirectory(): File? = getExternalFilesDir(Environment.DIRECTORY_PICTURES)

    private fun cleanupExistingImages(directory: File?) {
        directory?.listFiles()?.forEach { if (it.isFile) it.delete() }
    }

    private fun startImageDownload() {
        downloadThread?.interrupt()
        downloadThread = Thread { performImageDownload() }.also { it.start() }
    }

    private fun performImageDownload() {
        val imageDirectory = getImageDirectory() ?: return

        try {
            prepareForDownload(imageDirectory)
            val imageUrls = extractImageUrls()
            if (imageUrls.isEmpty()) {
                showError("No suitable images found on the webpage")
                return
            }
            initializeProgressUI(imageUrls.size)
            downloadImages(imageUrls, imageDirectory)
            handleDownloadSuccess()
        } catch (e: Exception) {
            e.printStackTrace()
            runOnUiThread { showError(e.message ?: "An error occurred during download") }
        }
    }

    private fun prepareForDownload(directory: File) {
        cleanupExistingImages(directory)
        runOnUiThread {
            adapter.notifyDataSetChanged()
            clearAllSelections()
        }
    }

    @Throws(Exception::class)
    private fun extractImageUrls(): List<String> {
        val pageUrl = urlInput.text.toString().trim()
        if (pageUrl.isEmpty()) throw IllegalArgumentException("Please enter a valid URL")

        return Jsoup.connect(pageUrl)
            .userAgent("Mozilla")
            .get()
            .select("img")
            .mapNotNull { it.absUrl("src") }
            .filter { it.startsWith("https") && !it.endsWith(".svg") }
            .distinct()
            .take(TOTAL_IMAGES)
    }

    private fun initializeProgressUI(totalImages: Int) {
        runOnUiThread {
            progressOverlay.apply {
                alpha = 0f
                visibility = View.VISIBLE
                animate().alpha(1f).setDuration(ANIMATION_DURATION).start()
            }
            progressBar.apply {
                max = totalImages
                progress = 0
                visibility = View.VISIBLE
            }
            updateProgressText("Starting download...")
        }
    }

    private fun updateProgressText(text: String) {
        progressText.apply {
            this.text = text
            visibility = View.VISIBLE
        }
    }

    private fun downloadImages(imageUrls: List<String>, directory: File) {
        imageUrls.forEachIndexed { index, url ->
            if (Thread.currentThread().isInterrupted) return

            val file = File(directory, "${index + 1}.jpg")
            val connection = URL(url).openConnection() as HttpURLConnection
            connection.setRequestProperty("User-Agent", "Mozilla")

            connection.inputStream.use { input ->
                file.outputStream().use { output ->
                    input.copyTo(output)
                }
            }

            runOnUiThread {
                progressBar.progress = index + 1
                progressText.text = "Downloading ${index + 1} of ${imageUrls.size} images..."
                adapter.notifyDataSetChanged()
            }
        }
    }

    private fun handleDownloadSuccess() {
        runOnUiThread {
            progressBar.visibility = View.GONE
            progressText.apply {
                text = "✅ Download complete!\nSelect $REQUIRED_SELECTIONS images to start the game."
                setTextColor("#80FFEA".toColorInt())
            }
            progressOverlay.setBackgroundColor("#AA000000".toColorInt())
        }
    }

    private fun showError(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_LONG).show()
    }

    private fun clearAllSelections() {
        selectedPositions.forEach { position ->
            val childView = gridView.getChildAt(position)
            val imageView = childView?.findViewById<ImageView>(R.id.cardFront)
            val tickView = childView?.findViewById<ImageView>(R.id.tickView)
            applySelectionStyle(imageView, tickView, false)
        }
        selectedPositions.clear()
    }

    override fun onItemClick(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
        val imageView = view?.findViewById<ImageView>(R.id.cardFront)
        val tickView = view?.findViewById<ImageView>(R.id.tickView)

        if (imageView?.tag == "placeholder") {
            showError("Please select only real images")
            return
        }

        val selected = selectedPositions.contains(position)
        if (selected) {
            selectedPositions.remove(position)
        } else {
            selectedPositions.add(position)
            if (selectedPositions.size == REQUIRED_SELECTIONS) {
                Toast.makeText(this, "Game is starting...", Toast.LENGTH_SHORT).show()
                initiateGameStart()
            }
        }
        applySelectionStyle(imageView, tickView, !selected)
    }

    private fun applySelectionStyle(imageView: ImageView?, tickView: ImageView?, isSelected: Boolean) {
        imageView?.apply {
            alpha = if (isSelected) 0.5f else 1.0f
            scaleX = if (isSelected) 1.1f else 1.0f
            scaleY = if (isSelected) 1.1f else 1.0f
        }
        tickView?.visibility = if (isSelected) View.VISIBLE else View.GONE
    }

    private fun initiateGameStart() {
        Toast.makeText(this, "Game is starting...", Toast.LENGTH_SHORT).show()

        val selectedImageFiles = selectedPositions.map { filenames[it] }.shuffled()
        val gameImageFiles = createGameImageList(selectedImageFiles)

        launchGameActivity(gameImageFiles)
    }

    private fun createGameImageList(selectedFiles: List<String>): List<String> {
        return (selectedFiles + selectedFiles).shuffled()
    }

    private fun launchGameActivity(imageFiles: List<String>) {
        val intent = Intent(this, PlayActivity::class.java).apply {
            putExtra("filenames", imageFiles.toTypedArray())
        }

        startActivity(intent)
        finish()
    }
}
