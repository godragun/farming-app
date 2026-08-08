package com.project.farmingapp.adapter

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.cardview.widget.CardView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.project.farmingapp.R
import com.project.farmingapp.supabase.RemoteDocument
import com.project.farmingapp.utilities.CellClickListener

class ArticleListAdapter(
    val context: Context,
    val articleListData: List<RemoteDocument>,
    private val cellClickListener: CellClickListener
) : RecyclerView.Adapter<ArticleListAdapter.ArticleListViewholder>() {

    class ArticleListViewholder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        var articleName = itemView.findViewById<TextView>(R.id.descTextxArticleListFrag)
        var articleImage = itemView.findViewById<ImageView>(R.id.imageArticleListFrag)
        var articleCard = itemView.findViewById<CardView>(R.id.articleListCardArtListFrag)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ArticleListViewholder {
        val view = LayoutInflater.from(context).inflate(R.layout.article_list_single, parent, false)
        return ArticleListAdapter.ArticleListViewholder(view)
    }

    override fun getItemCount(): Int = articleListData.size

    override fun onBindViewHolder(holder: ArticleListViewholder, position: Int) {
        val singleArticle = articleListData[position]
        val title = singleArticle.data["title"]?.toString().orEmpty()

        holder.articleName.text = title
        holder.articleCard.setOnClickListener {
            cellClickListener.onCellClickListener(title)
        }
        val list = (singleArticle.data["images"] as? List<*>)?.mapNotNull { it as? String }.orEmpty()

        Glide.with(holder.itemView.context)
            .load(list.firstOrNull())
            .placeholder(R.drawable.fruits)
            .error(R.drawable.fruits)
            .into(holder.articleImage)
    }
}
