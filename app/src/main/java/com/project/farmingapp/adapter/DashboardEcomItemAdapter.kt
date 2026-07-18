package com.project.farmingapp.adapter

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.AnimationUtils
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.google.firebase.firestore.DocumentSnapshot
import com.project.farmingapp.R
import com.project.farmingapp.utilities.CellClickListener
import com.project.farmingapp.utilities.pulseTapFeedback


class DashboardEcomItemAdapter(var context: Context,val allData: List<DocumentSnapshot>, val itemsToShow: List<Int>, val cellClickListener: CellClickListener): RecyclerView.Adapter<DashboardEcomItemAdapter.DashboardEcomItemViewHolder>() {
    private var lastAnimatedPosition = -1
    private val itemAnimation by lazy { AnimationUtils.loadAnimation(context, R.anim.fade_scale) }
    class DashboardEcomItemViewHolder(itemView: View): RecyclerView.ViewHolder(itemView){
        val title: TextView = itemView.findViewById(R.id.itemTitle)
        val price: TextView = itemView.findViewById(R.id.itemPrice)
        val image: ImageView = itemView.findViewById(R.id.itemImage)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): DashboardEcomItemViewHolder {
        val view = LayoutInflater.from(context).inflate(R.layout.single_dashboard_ecomm_item, parent, false)
        return DashboardEcomItemViewHolder(view)
    }

    override fun getItemCount(): Int {
        return itemsToShow.size
    }

    override fun onBindViewHolder(holder: DashboardEcomItemViewHolder, position: Int) {
        val currentData = allData[itemsToShow[position]]

        holder.title.text = currentData.get("title").toString()
        holder.price.text = "\u20B9"  + currentData.get("price").toString()
        val allImages = currentData.get("imageUrl") as ArrayList<String>
        Glide.with(context).load(allImages[0]).into(holder.image)
        if (position > lastAnimatedPosition) {
            holder.itemView.startAnimation(itemAnimation)
            lastAnimatedPosition = position
        }
        holder.itemView.setOnClickListener {
            holder.itemView.pulseTapFeedback()
            cellClickListener.onCellClickListener(currentData.id)
        }

    }

}