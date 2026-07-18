package com.project.farmingapp.adapter

import android.content.Context
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
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import com.project.farmingapp.R


class SMPostListAdapter(val context: Context, val postListData : List<DocumentSnapshot>): RecyclerView.Adapter<SMPostListAdapter.SMPostListViewModel>() {

    lateinit var firebaseAuth: FirebaseAuth
    lateinit var firebaseFirestore: FirebaseFirestore
    private val profileImageCache = mutableMapOf<String, String>()
    private var lastAnimatedPosition = -1
    private val itemAnimation by lazy { AnimationUtils.loadAnimation(context, R.anim.fade_transition) }
    class SMPostListViewModel(itemView: View): RecyclerView.ViewHolder(itemView){
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

    override fun getItemCount(): Int {
        return postListData.size
    }

    override fun onBindViewHolder(holder: SMPostListViewModel, position: Int) {
        val currentPost = postListData[position]

        holder.userName.text = currentPost.get("name").toString()
        holder.title.text = currentPost.get("title").toString()
        holder.description.text = currentPost.get("description").toString()
        holder.uploadTime.text = DateUtils.getRelativeTimeSpanString(currentPost.get("timeStamp") as Long)

        val imageUrl = currentPost.get("imageUrl")
        Log.d("Post without Image1", imageUrl.toString())


        val uploadType = currentPost.get("uploadType").toString()
        if (uploadType == "video"){
            holder.postImage.visibility = View.GONE
            holder.postVideo.visibility = View.VISIBLE
            holder.postVideo.loadUrl(currentPost.get("imageUrl").toString())


        } else if (uploadType == "image"){
            Glide.with(context).load(currentPost.get("imageUrl")).into(holder.postImage)
            holder.postVideo.visibility = View.GONE
            holder.postImage.visibility = View.VISIBLE
            Log.d("Upload Type 2 ", uploadType)
        }else if (uploadType.isEmpty() ){
            Log.d("Post without Image2", imageUrl.toString())
            holder.postImage.visibility = View.GONE
            holder.postVideo.visibility = View.GONE
            Log.d("Upload Type 3 ", uploadType)
        }

        firebaseAuth = FirebaseAuth.getInstance()

        if (position > lastAnimatedPosition) {
            holder.profileImageCard.startAnimation(itemAnimation)
            holder.container.startAnimation(itemAnimation)
            lastAnimatedPosition = position
        }
//        Glide.with(context).load(firebaseAuth.currentUser!!.photoUrl.toString()).into(holder.itemView.userProfileImagePost)
        holder.description.setOnClickListener {
            holder.description.maxLines = Int.MAX_VALUE
        }



        firebaseFirestore = FirebaseFirestore.getInstance()
        val userId = currentPost.get("userID").toString()
        val cachedImage = profileImageCache[userId]
        if (!cachedImage.isNullOrBlank()) {
            Glide.with(context).load(cachedImage).into(holder.profileImage)
        } else {
            firebaseFirestore.collection("users").document(userId).get()
                .addOnSuccessListener {
                    val profileImage = it.get("profileImage").toString()
                    if (!profileImage.isNullOrEmpty()) {
                        profileImageCache[userId] = profileImage
                        Glide.with(context).load(profileImage).into(holder.profileImage)
                    }
                }
        }
    }
}