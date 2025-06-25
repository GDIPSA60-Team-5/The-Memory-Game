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
import android.util.Log

class MyCustomAdapter(private val context: Context, private val filenames: Array<String>) : ArrayAdapter<Any?>(
    context,
    R.layout.grid,
    filenames
) {
    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
        val view = convertView ?: LayoutInflater.from(context).inflate(R.layout.grid, parent, false)
        val imgView = view.findViewById<ImageView>(R.id.imageView)
        val file = File(context.getExternalFilesDir(Environment.DIRECTORY_PICTURES), filenames[position])

        Log.d("Adapter getView Function", "This function is called properly!")

        if (file.exists()) {
            val bitmap = BitmapFactory.decodeFile(file.absolutePath)
            imgView.setImageBitmap(bitmap)
        } else {
            imgView.setImageResource(R.drawable.placeholder)
        }

        return view
    }
}