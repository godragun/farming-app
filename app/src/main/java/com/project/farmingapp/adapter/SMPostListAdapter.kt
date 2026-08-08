package com.project.farmingapp.adapter

import android.text.format.DateUtils
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.AnimationUtils
import android.webkit.WebView
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.project.farmingapp.R
import com.project.farmingapp.supabase.RemoteDocument
import com.project.farmingapp.supabase.SupabaseDb
import java.util.concurrent.Executors

class SMPostListAdapter(
    val context: android.content.Context,
    val postListData: List<RemoteDocument>
) : RecyclerView.Adapter<SMPostListAdapter.SMPostListViewModel>() {

    private val profileImageCache = mutableMapOf<String, String>()
    private val executor = Executors.newSingleThreadExecutor()
    private var lastAnimatedPosition = -1
    private val itemAnimation by lazy { AnimationUtils.loadAnimation(context, R.anim.fade_transition) }

    class SMPostListViewModel(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val userName: TextView = itemView.findViewById(R.id.userNamePostSM)
        val title: TextView = itemView.findViewById(R.id.userPostTitleValue)
        val description: TextView = itemView.findViewById(R.id.userPostDescValue)
        val uploadTime: TextView = itemView.findViewById(R.id.userPostUploadTime)
        val postImage: ImageView = itemView.findViewById(R.id.postImageSM)
        val postVideo: WebView = itemView.findViewById(R.id.postVideoSM)
        val profileImageCard: View = itemView.findViewById(R.id.userProfileImageCard)
        val container: View = itemView.findViewById(R.id.post_container)
        val profileImage: ImageView = itemView.findViewById(R.id.userProfileImagePost)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SMPostListViewModel {
        val view = LayoutInflater.from(context).inflate(R.layout.post_with_image_sm, parent, false)
        val holder = SMPostListAdapter.SMPostListViewModel(view)
        holder.postVideo.settings.javaScriptEnabled = true
        holder.postVideo.settings.loadWithOverviewMode = true
        holder.postVideo.settings.useWideViewPort = true
        return holder
    }

    override fun getItemCount(): Int = postListData.size

    override fun onBindViewHolder(holder: SMPostListViewModel, position: Int) {
        val currentPost = postListData[position]

        holder.userName.text = currentPost.get("name").toString()
        holder.title.text = currentPost.get("title").toString()
        holder.description.text = currentPost.get("description").toString()
        val stamp = currentPost.getLong("timeStamp") ?: currentPost.getLong("time_stamp") ?: 0L
        holder.uploadTime.text = DateUtils.getRelativeTimeSpanString(stamp)

        val imageUrl = currentPost.get("imageUrl")
        Log.d("Post without Image1", imageUrl.toString())

        val uploadType = currentPost.get("uploadType")?.toString().orEmpty()
        if (uploadType == "video") {
            holder.postImage.visibility = View.GONE
            holder.postVideo.visibility = View.VISIBLE
            holder.postVideo.loadUrl(currentPost.get("imageUrl").toString())
        } else if (uploadType == "image") {
            Glide.with(context).load(currentPost.get("imageUrl")).into(holder.postImage)
            holder.postVideo.visibility = View.GONE
            holder.postImage.visibility = View.VISIBLE
            Log.d("Upload Type 2 ", uploadType)
        } else if (uploadType.isEmpty()) {
            Log.d("Post without Image2", imageUrl.toString())
            holder.postImage.visibility = View.GONE
            holder.postVideo.visibility = View.GONE
            Log.d("Upload Type 3 ", uploadType)
        }

        if (position > lastAnimatedPosition) {
            holder.profileImageCard.startAnimation(itemAnimation)
            holder.container.startAnimation(itemAnimation)
            lastAnimatedPosition = position
        }
        holder.description.setOnClickListener {
            holder.description.maxLines = Int.MAX_VALUE
        }

        val userId = currentPost.get("userID")?.toString()
            ?: currentPost.get("user_id")?.toString()
            ?: return
        val cachedImage = profileImageCache[userId]
        if (!cachedImage.isNullOrBlank()) {
            Glide.with(context).load(cachedImage).into(holder.profileImage)
        } else {
            executor.execute {
                try {
                    val user = SupabaseDb.selectOne("users", SupabaseDb.eq("email", userId), idKey = "email")
                        ?: SupabaseDb.selectOne("users", SupabaseDb.eq("id", userId))
                    val profileImage = user?.getString("profileImage").orEmpty()
                    if (profileImage.isNotBlank()) {
                        profileImageCache[userId] = profileImage
                        (context as? android.app.Activity)?.runOnUiThread {
                            Glide.with(context).load(profileImage).into(holder.profileImage)
                        } ?: android.os.Handler(android.os.Looper.getMainLooper()).post {
                            Glide.with(context).load(profileImage).into(holder.profileImage)
                        }
                    }
                } catch (_: Exception) {
                }
            }
        }
    }
}
