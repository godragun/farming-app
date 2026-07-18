package com.project.farmingapp.adapter

import android.text.format.DateUtils
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.AnimationUtils
import android.webkit.WebView
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.resource.bitmap.DownsampleStrategy
import com.project.farmingapp.R

data class OfflinePostItem(
    val name: String,
    val title: String,
    val description: String,
    val uploadedAt: Long,
    val imageRes: Int,
    val imageAssetName: String? = null
)

class OfflineSMPostAdapter(
    private val posts: List<OfflinePostItem>
) : RecyclerView.Adapter<OfflineSMPostAdapter.OfflinePostViewHolder>() {
    private var lastAnimatedPosition = -1
    private var cachedAnimation: android.view.animation.Animation? = null

    class OfflinePostViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val userName: TextView = itemView.findViewById(R.id.userNamePostSM)
        val title: TextView = itemView.findViewById(R.id.userPostTitleValue)
        val description: TextView = itemView.findViewById(R.id.userPostDescValue)
        val uploadTime: TextView = itemView.findViewById(R.id.userPostUploadTime)
        val postImage: ImageView = itemView.findViewById(R.id.postImageSM)
        val postVideo: WebView = itemView.findViewById(R.id.postVideoSM)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): OfflinePostViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.post_with_image_sm, parent, false)
        return OfflinePostViewHolder(view)
    }

    override fun getItemCount(): Int = posts.size

    override fun onBindViewHolder(holder: OfflinePostViewHolder, position: Int) {
        val post = posts[position]
        holder.userName.text = post.name
        holder.title.text = post.title
        holder.description.text = post.description
        holder.uploadTime.text = DateUtils.getRelativeTimeSpanString(post.uploadedAt)
        holder.postVideo.visibility = View.GONE
        holder.postImage.visibility = View.VISIBLE
        if (!post.imageAssetName.isNullOrBlank()) {
            Glide.with(holder.itemView)
                .load("file:///android_asset/${post.imageAssetName}")
                .downsample(DownsampleStrategy.CENTER_INSIDE)
                .override(900, 1200)
                .into(holder.postImage)
        } else {
            holder.postImage.setImageResource(post.imageRes)
        }
        if (position > lastAnimatedPosition) {
            val anim = cachedAnimation ?: AnimationUtils.loadAnimation(holder.itemView.context, R.anim.fade_transition).also { cachedAnimation = it }
            holder.itemView.startAnimation(anim)
            lastAnimatedPosition = position
        }
    }
}
