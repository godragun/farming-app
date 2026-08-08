package com.project.farmingapp.adapter

import android.content.Context
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
import com.project.farmingapp.supabase.RemoteDocument
import com.project.farmingapp.utilities.CellClickListener
import com.project.farmingapp.utilities.formatLkr
import com.project.farmingapp.utilities.parseAmount
import com.project.farmingapp.utilities.pulseTapFeedback

class EcommerceAdapter(
    val context: Context,
    val ecommtListData: List<RemoteDocument>,
    private val cellClickListener: CellClickListener
) : RecyclerView.Adapter<EcommerceAdapter.EcommercceViewModel>() {

    private var lastAnimatedPosition = -1
    private val itemAnimation by lazy { AnimationUtils.loadAnimation(context, R.anim.fade_scale) }

    class EcommercceViewModel(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val title: TextView = itemView.findViewById(R.id.ecommtitle)
        val price: TextView = itemView.findViewById(R.id.ecommPrice)
        val retailer: TextView = itemView.findViewById(R.id.ecommretailer)
        val availability: TextView = itemView.findViewById(R.id.ecommItemAvailability)
        val image: ImageView = itemView.findViewById(R.id.ecommImage)
        val rating: RatingBar = itemView.findViewById(R.id.ecommRating)
    }

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): EcommerceAdapter.EcommercceViewModel {
        val view = LayoutInflater.from(context).inflate(R.layout.single_ecomm_item, parent, false)
        return EcommerceAdapter.EcommercceViewModel(view)
    }

    override fun getItemCount(): Int = ecommtListData.size

    override fun onBindViewHolder(holder: EcommerceAdapter.EcommercceViewModel, position: Int) {
        val currentList = ecommtListData[position]
        holder.title.text = currentList.get("title").toString()
        holder.price.text = formatLkr(parseAmount(currentList.get("price").toString()))
        holder.retailer.text = currentList.get("retailer").toString()
        holder.availability.text = currentList.get("availability").toString()
        @Suppress("UNCHECKED_CAST")
        val allImages = currentList.get("imageUrl") as? List<String>
        if (!allImages.isNullOrEmpty()) {
            Glide.with(context).load(allImages[0]).into(holder.image)
        } else {
            holder.image.setImageResource(R.drawable.ic_launcher_foreground)
        }
        holder.rating.rating = currentList.get("rating")?.toString()?.toFloatOrNull() ?: 0f
        if (position > lastAnimatedPosition) {
            holder.itemView.startAnimation(itemAnimation)
            lastAnimatedPosition = position
        }

        holder.itemView.setOnClickListener {
            holder.itemView.pulseTapFeedback()
            cellClickListener.onCellClickListener(currentList.id)
        }
    }
}
