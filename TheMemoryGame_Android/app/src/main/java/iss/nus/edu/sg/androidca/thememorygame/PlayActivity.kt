package iss.nus.edu.sg.androidca.thememorygame

import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.AdapterView
import android.widget.GridView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat

class PlayActivity : AppCompatActivity(), AdapterView.OnItemClickListener {

    private lateinit var adapter: MyCustomAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_play)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.play)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        val gridView = findViewById<GridView>(R.id.playGridView)
        val filenames = intent.getStringArrayExtra("filenames")
        Log.d("PlayActivity filenames", filenames.toString())

        if (filenames != null && filenames.size == 12) {
            adapter = MyCustomAdapter(this, filenames)
            gridView.adapter = adapter
            gridView.onItemClickListener = this
        } else {
            Toast.makeText(this, "Invalid image data", Toast.LENGTH_SHORT).show()
            finish()
            return
        }
    }

    override fun onItemClick(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
        if (!::adapter.isInitialized) return

        adapter.revealPosition(position)

        if (adapter.currentlyFlipped.size == 2) {
            val gridView = findViewById<GridView>(R.id.playGridView)
            gridView.postDelayed({
                if (adapter.checkForMatch()) {
                    adapter.finalizeMatch()
                } else {
                    adapter.resetFlipped()
                }
            }, 1000)
        }

        if (adapter.revealedPositions.size == adapter.count) {
            Toast.makeText(this, "You won!", Toast.LENGTH_LONG).show()
        }
    }
}