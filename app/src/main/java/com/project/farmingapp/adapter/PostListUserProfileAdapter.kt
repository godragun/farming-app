package com.project.farmingapp.adapter

import android.content.Context
import android.text.format.DateUtils
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.project.farmingapp.R
import com.project.farmingapp.supabase.RemoteDocument
import com.project.farmingapp.utilities.CellClickListener

class PostListUserProfileAdapter(
    val context: Context,
    var listData: ArrayList<RemoteDocument>,
    private val cellClickListener: CellClickListener
) : RecyclerView.Adapter<PostListUserProfileAdapter.PostListUserProfileViewHolder>() {

    class PostListUserProfileViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val title: TextView = itemView.findViewById(R.id.userPostTitleUserProfileFrag)
        val time: TextView = itemView.findViewById(R.id.userPostUploadTimeUserProfileFrag)
        val card: View = itemView.findViewById(R.id.userPostProfileCard)
        val image: ImageView = itemView.findViewById(R.id.userPostImageUserProfileFrag)
    }

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): PostListUserProfileViewHolder {
        val view = LayoutInflater.from(context).inflate(R.layout.user_profile_posts_single, parent, false)
        return PostListUserProfileAdapter.PostListUserProfileViewHolder(view)
    }

    override fun getItemCount(): Int = listData.size

    override fun onBindViewHolder(holder: PostListUserProfileViewHolder, position: Int) {
        val currentData = listData[position]

        holder.title.text = currentData.get("title").toString()
        val stamp = currentData.getLong("timeStamp") ?: currentData.getLong("time_stamp") ?: 0L
        holder.time.text = DateUtils.getRelativeTimeSpanString(stamp)
        holder.card.setOnClickListener {
            cellClickListener.onCellClickListener(currentData.id)
        }
        val imageUrl = currentData.getString("imageUrl")
        if (!imageUrl.isNullOrBlank()) {
            Glide.with(context).load(imageUrl).into(holder.image)
        }
    }
}
