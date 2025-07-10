package iss.nus.edu.sg.androidca.thememorygame

import android.content.Context
import android.graphics.BitmapFactory
import android.os.Environment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.ImageView
import java.io.File

class MyCustomAdapter(private val context: Context, private val filenames: Array<String>) : ArrayAdapter<Any?>(
    context,
    R.layout.fetch_grid,
    filenames
) {
    val revealedPositions = mutableSetOf<Int>()
    val currentlyFlipped = mutableListOf<Int>()

    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
        val view = convertView ?: LayoutInflater.from(context).inflate(R.layout.fetch_grid, parent, false)
        val imgView = view.findViewById<ImageView>(R.id.imageView)

        // Use this code block if request comes from play activity
        // Show placeholder if the images are not currently flipped or already revealed
        if (context is PlayActivity) {
            if (revealedPositions.contains(position) || currentlyFlipped.contains(position)) {
                val file = File(context.getExternalFilesDir(Environment.DIRECTORY_PICTURES), filenames[position])
                showImage(file, imgView)
            } else {
                imgView.setImageResource(R.drawable.placeholder)
                imgView.tag = "placeholder"
            }
        }
        else {  // Use this one if request comes from fetch activity
            val file = File(context.getExternalFilesDir(Environment.DIRECTORY_PICTURES), filenames[position])
            showImage(file, imgView)
        }
        return view
    }

    // Show images one by one on the grid view
    // Use tags "real_image" and "placeholder" to distinguish the type of image being shown
    private fun showImage(file: File, imgView: ImageView) {
        if (file.exists()) {
            val bitmap = BitmapFactory.decodeFile(file.absolutePath)
            imgView.setImageBitmap(bitmap)
            imgView.tag = "real_image"
        } else {
            imgView.setImageResource(R.drawable.placeholder)
            imgView.tag = "placeholder"
        }
    }

    // Return the position of the flipped image
    fun revealPosition(position: Int) {
        if (currentlyFlipped.size < 2 && !currentlyFlipped.contains(position)) {
            currentlyFlipped.add(position)
            notifyDataSetChanged()
        }
    }

    // Check if the two flipped images match or not
    fun checkForMatch(): Boolean {
        if (currentlyFlipped.size == 2) {
            val first = currentlyFlipped[0]
            val second = currentlyFlipped[1]

            return filenames[first] == filenames[second]
        }
        return false
    }

    // If matched, add currently flipped image positions to the revealed/matched images list
    fun finalizeMatch() {
        revealedPositions.addAll(currentlyFlipped)
        currentlyFlipped.clear()
        notifyDataSetChanged()
    }

    // If not matched, clear the currently flipped list
    fun resetFlipped() {
        currentlyFlipped.clear()
        notifyDataSetChanged()
    }
}