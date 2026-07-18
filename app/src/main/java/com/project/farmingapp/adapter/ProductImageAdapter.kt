package com.project.farmingapp.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.project.farmingapp.R

class ProductImageAdapter(private val imageUrls: List<String>) :
    RecyclerView.Adapter<ProductImageAdapter.ImageViewHolder>() {

    class ImageViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val imageView: ImageView = view.findViewById(R.id.productSliderImage)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ImageViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_product_image, parent, false)
        return ImageViewHolder(view)
    }

    override fun getItemCount(): Int = imageUrls.size.coerceAtLeast(1)

    override fun onBindViewHolder(holder: ImageViewHolder, position: Int) {
        val url = imageUrls.getOrNull(position)
        if (url.isNullOrBlank()) {
            holder.imageView.setImageResource(R.drawable.ic_launcher_foreground)
        } else {
            Glide.with(holder.imageView.context)
                .load(url)
                .placeholder(R.drawable.ic_launcher_foreground)
                .error(R.drawable.ic_launcher_foreground)
                .centerCrop()
                .into(holder.imageView)
        }
    }
}
