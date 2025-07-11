package iss.nus.edu.sg.androidca.thememorygame

import android.content.Context
import android.os.Environment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.ImageView
import com.bumptech.glide.Glide
import iss.nus.edu.sg.androidca.thememorygame.activities.PlayActivity
import java.io.File

class MyCustomAdapter(
    private val context: Context,
    private val filenames: Array<String>
) : ArrayAdapter<String>(context, R.layout.fetch_grid, filenames) {

    // === Constants ===
    companion object {
        private const val MAX_FLIPPED_CARDS = 2
        private const val PLACEHOLDER_TAG = "placeholder"
        private const val REAL_IMAGE_TAG = "real_image"
    }

    // === State ===
    private val revealedPositions = mutableSetOf<Int>()
    private val currentlyFlipped = mutableListOf<Int>()
    private val isGameMode = context is PlayActivity

    // === View Binding ===
    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
        val view = convertView ?: inflateView(parent)
        val cardFront = view.findViewById<ImageView>(R.id.cardFront)
        val cardBack = view.findViewById<ImageView>(R.id.cardBack)
        val tickView = view.findViewById<ImageView>(R.id.tickView)

        bindCardState(position, cardFront, cardBack, tickView)
        return view
    }

    private fun inflateView(parent: ViewGroup): View {
        return LayoutInflater.from(context).inflate(R.layout.fetch_grid, parent, false)
    }

    private fun bindCardState(position: Int, front: ImageView, back: ImageView, tick: ImageView) {
        val isRevealed = revealedPositions.contains(position)
        val isFlipped = currentlyFlipped.contains(position)
        val showFront = !isGameMode || isRevealed || isFlipped

        if (showFront) {
            front.visibility = View.VISIBLE
            back.visibility = View.GONE
            loadImageForPosition(position, front)
        } else {
            front.visibility = View.GONE
            back.visibility = View.VISIBLE
            back.setImageResource(R.drawable.card_back)
            back.tag = PLACEHOLDER_TAG
        }

        tick.visibility = if (isRevealed) View.VISIBLE else View.GONE
    }

    private fun loadImageForPosition(position: Int, imageView: ImageView) {
        val filename = filenames[position]
        val file = File(context.getExternalFilesDir(Environment.DIRECTORY_PICTURES), filename)

        if (file.exists()) {
            try {
                Glide.with(imageView.context)
                    .load(file)
                    .placeholder(R.drawable.card_back)
                    .into(imageView)
                imageView.tag = REAL_IMAGE_TAG
            } catch (e: Exception) {
                imageView.setImageResource(R.drawable.card_back)
                imageView.tag = PLACEHOLDER_TAG
            }
        } else {
            imageView.setImageResource(R.drawable.card_back)
            imageView.tag = PLACEHOLDER_TAG
        }
    }

    // === Game Logic ===
    fun revealPosition(position: Int): Boolean {
        if (canFlipCard(position)) {
            currentlyFlipped.add(position)
            notifyDataSetChanged()
            return true
        }
        return false
    }

    private fun canFlipCard(position: Int): Boolean {
        return currentlyFlipped.size < MAX_FLIPPED_CARDS &&
                position !in currentlyFlipped &&
                position !in revealedPositions
    }

    fun checkForMatch(): Boolean {
        return currentlyFlipped.size == MAX_FLIPPED_CARDS &&
                filenames[currentlyFlipped[0]] == filenames[currentlyFlipped[1]]
    }

    fun finalizeMatch() {
        revealedPositions.addAll(currentlyFlipped)
        clearFlippedCards()
    }

    fun resetFlipped() {
        clearFlippedCards()
    }

    private fun clearFlippedCards() {
        currentlyFlipped.clear()
        notifyDataSetChanged()
    }

    // === Game Status ===
    fun getCurrentlyFlippedCount(): Int = currentlyFlipped.size
    fun getRevealedCount(): Int = revealedPositions.size
    fun isGameComplete(): Boolean = revealedPositions.size == filenames.size
}
