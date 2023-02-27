package io.fydeos.kangtester

import android.R
import android.content.ContentUris
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import android.provider.MediaStore.Audio.Media
import android.util.Log
import android.util.Size
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import io.fydeos.kangtester.databinding.PictureHolderBinding


class MediaStorePictureAdaptor constructor(val l: List<Uri>, var listener: ((String, String)->Unit)? = null) :
    RecyclerView.Adapter<MediaStorePictureAdaptor.ViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding =
            PictureHolderBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ViewHolder(binding.root)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val binding = PictureHolderBinding.bind(holder.itemView)
        val res = holder.itemView.context.contentResolver
        val uri = l[position]
        // binding.imageView.setImageURI(l[position])
        try {
            if (Build.VERSION.SDK_INT >= 29) {
                binding.imageView.setImageBitmap(
                    res.loadThumbnail(
                        uri,
                        Size(512, 384),
                        null
                    )
                )
            } else {
                binding.imageView.setImageBitmap(
                    MediaStore.Images.Thumbnails.getThumbnail(
                        res,
                        ContentUris.parseId(uri),
                        MediaStore.Images.Thumbnails.MINI_KIND,
                        null
                    )
                )
            }
        } catch (ex: java.lang.Exception) {
            Log.e("MediaStorePictureAdaptor", l[position].toString() + " " + ex.toString())
            listener?.invoke(l[position].toString(), ex.toString())
        }
        // binding.imageView.setImageResource(R.drawable.btn_star_big_on)
    }

    override fun getItemCount(): Int {
        return l.size
    }

    // Create a static inner class and provide references to all the Views for each data item.
    // This is particularly useful for caching the Views within the item layout for fast access.
    class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {

        // Create a constructor that accepts the entire row and search the View hierarchy to find each subview
        init {
        }
    }
}
