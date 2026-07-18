package com.project.farmingapp.adapter

import android.content.Context
import android.text.Layout
import android.text.format.DateUtils
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.google.firebase.firestore.DocumentSnapshot
import com.project.farmingapp.R
import com.project.farmingapp.utilities.CellClickListener

class PostListUserProfileAdapter(val context: Context, var listData: ArrayList<DocumentSnapshot>, private val cellClickListener: CellClickListener) : RecyclerView.Adapter<PostListUserProfileAdapter.PostListUserProfileViewHolder>() {
    class PostListUserProfileViewHolder(itemView: View): RecyclerView.ViewHolder(itemView){
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

    override fun getItemCount(): Int {
        return listData.size
    }

    override fun onBindViewHolder(holder: PostListUserProfileViewHolder, position: Int) {
        val currentData = listData[position]

        holder.title.text = currentData.get("title").toString()
        holder.time.text = DateUtils.getRelativeTimeSpanString(currentData.get("timeStamp") as Long)
        holder.card.setOnClickListener {
            cellClickListener.onCellClickListener(currentData.id)
        }
        if (!currentData.get("imageUrl").toString().isNullOrEmpty()){
            Glide.with(context).load(currentData.getString("imageUrl")).into(holder.image)
        }
    }
}