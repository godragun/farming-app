package com.project.farmingapp.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.project.farmingapp.R
import com.project.farmingapp.utilities.formatLkr

class OfflineDashboardEcomAdapter(
    private val items: List<OfflineEcommerceItem>,
    private val onItemClicked: (OfflineEcommerceItem) -> Unit
) : RecyclerView.Adapter<OfflineDashboardEcomAdapter.OfflineDashboardViewHolder>() {

    class OfflineDashboardViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val title: TextView = itemView.findViewById(R.id.itemTitle)
        val price: TextView = itemView.findViewById(R.id.itemPrice)
        val image: ImageView = itemView.findViewById(R.id.itemImage)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): OfflineDashboardViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.single_dashboard_ecomm_item, parent, false)
        return OfflineDashboardViewHolder(view)
    }

    override fun getItemCount(): Int = items.size

    override fun onBindViewHolder(holder: OfflineDashboardViewHolder, position: Int) {
        val item = items[position]
        holder.title.text = item.title
        holder.price.text = formatLkr(item.price)
        if (!item.imageAssetName.isNullOrBlank()) {
            Glide.with(holder.itemView)
                .load("file:///android_asset/${item.imageAssetName}")
                .into(holder.image)
        } else {
            holder.image.setImageResource(item.imageRes)
        }
        holder.itemView.setOnClickListener { onItemClicked(item) }
    }
}
