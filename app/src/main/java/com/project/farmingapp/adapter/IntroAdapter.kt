package com.project.farmingapp.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.bumptech.glide.load.resource.bitmap.DownsampleStrategy
import com.project.farmingapp.R
import com.project.farmingapp.model.data.IntroData
import com.project.farmingapp.utilities.AgriAssetPaths

class IntroAdapter(private val introSlides: List<IntroData>) :
    RecyclerView.Adapter<IntroAdapter.IntroViewHolder>() {

    inner class IntroViewHolder(view: View) : RecyclerView.ViewHolder(view) {

        private val textTitle = view.findViewById<TextView>(R.id.sliderTitle)
        private val textDescription = view.findViewById<TextView>(R.id.sliderDescription)
        private val imageIcon = view.findViewById<ImageView>(R.id.imageSlider)

        fun bind(introSlider: IntroData) {
            textTitle.text = introSlider.title
            textDescription.text = introSlider.description
            
            // Load image from assets using proper file URI
            try {
                val imageUri = com.project.farmingapp.utilities.AgriAssetPaths.fileUri(introSlider.imageAssetPath)
                android.util.Log.d("IntroAdapter", "Loading image: $imageUri")
                
                Glide.with(imageIcon.context)
                    .load(imageUri)
                    .diskCacheStrategy(DiskCacheStrategy.NONE)
                    .skipMemoryCache(false)
                    .override(1080, 1920)
                    .centerCrop()
                    .listener(object : com.bumptech.glide.request.RequestListener<android.graphics.drawable.Drawable> {
                        override fun onLoadFailed(
                            e: com.bumptech.glide.load.engine.GlideException?,
                            model: Any?,
                            target: com.bumptech.glide.request.target.Target<android.graphics.drawable.Drawable>,
                            isFirstResource: Boolean
                        ): Boolean {
                            android.util.Log.e("IntroAdapter", "Image load failed: ${imageUri}, Error: ${e?.message}")
                            return false
                        }

                        override fun onResourceReady(
                            resource: android.graphics.drawable.Drawable,
                            model: Any,
                            target: com.bumptech.glide.request.target.Target<android.graphics.drawable.Drawable>,
                            dataSource: com.bumptech.glide.load.DataSource,
                            isFirstResource: Boolean
                        ): Boolean {
                            android.util.Log.d("IntroAdapter", "Image loaded successfully: ${imageUri}")
                            return false
                        }
                    })
                    .error(R.drawable.ic_launcher_foreground)
                    .into(imageIcon)
            } catch (e: Exception) {
                // Fallback to placeholder if asset doesn't exist
                imageIcon.setImageDrawable(
                    androidx.core.content.ContextCompat.getDrawable(
                        imageIcon.context,
                        R.drawable.ic_launcher_foreground
                    )
                )
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): IntroViewHolder {
        return IntroViewHolder(
            LayoutInflater.from(parent.context).inflate(R.layout.single_slider_screen, parent, false)
        )
    }

    override fun getItemCount(): Int = introSlides.size

    override fun onBindViewHolder(holder: IntroViewHolder, position: Int) {
        holder.bind(introSlides[position])
    }
}
