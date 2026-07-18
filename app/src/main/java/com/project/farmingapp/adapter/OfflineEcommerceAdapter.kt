package com.project.farmingapp.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.AnimationUtils
import android.widget.ImageView
import android.widget.RatingBar
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.project.farmingapp.R
import com.project.farmingapp.utilities.formatLkr
import com.project.farmingapp.utilities.pulseTapFeedback

data class OfflineEcommerceItem(
    val id: String,
    val title: String,
    val price: Int,
    val retailer: String,
    val availability: String,
    val rating: Float,
    val type: String,
    val imageRes: Int,
    val imageAssetName: String?,
    val shortDesc: String,
    val longDesc: String,
    val howToUse: String,
    val deliveryCharge: Int
)

class OfflineEcommerceAdapter(
    private val items: List<OfflineEcommerceItem>,
    private val onItemClicked: (OfflineEcommerceItem) -> Unit
) : RecyclerView.Adapter<OfflineEcommerceAdapter.OfflineEcommerceViewHolder>() {
    private var lastAnimatedPosition = -1
    private var cachedAnimation: android.view.animation.Animation? = null

    class OfflineEcommerceViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val title: TextView = itemView.findViewById(R.id.ecommtitle)
        val price: TextView = itemView.findViewById(R.id.ecommPrice)
        val retailer: TextView = itemView.findViewById(R.id.ecommretailer)
        val availability: TextView = itemView.findViewById(R.id.ecommItemAvailability)
        val image: ImageView = itemView.findViewById(R.id.ecommImage)
        val rating: RatingBar = itemView.findViewById(R.id.ecommRating)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): OfflineEcommerceViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.single_ecomm_item, parent, false)
        return OfflineEcommerceViewHolder(view)
    }

    override fun getItemCount(): Int = items.size

    override fun onBindViewHolder(holder: OfflineEcommerceViewHolder, position: Int) {
        val item = items[position]
        holder.title.text = item.title
        holder.price.text = formatLkr(item.price)
        holder.retailer.text = item.retailer
        holder.availability.text = item.availability
        holder.rating.rating = item.rating
        if (!item.imageAssetName.isNullOrBlank()) {
            Glide.with(holder.itemView)
                .load("file:///android_asset/${item.imageAssetName}")
                .into(holder.image)
        } else {
            holder.image.setImageResource(item.imageRes)
        }
        if (position > lastAnimatedPosition) {
            val anim = cachedAnimation ?: AnimationUtils.loadAnimation(holder.itemView.context, R.anim.fade_scale).also { cachedAnimation = it }
            holder.itemView.startAnimation(anim)
            lastAnimatedPosition = position
        }
        holder.itemView.setOnClickListener {
            holder.itemView.pulseTapFeedback()
            onItemClicked(item)
        }
    }
}
