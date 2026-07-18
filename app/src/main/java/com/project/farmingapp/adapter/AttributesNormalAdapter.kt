package com.project.farmingapp.adapter

import android.content.Context
import android.text.Layout
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.project.farmingapp.R

class AttributesNormalAdapter(val context: Context, val allData : List<Map<String, Any>>): RecyclerView.Adapter<AttributesNormalAdapter.AttributesNormalViewHolder>() {
    class AttributesNormalViewHolder(itemView: View): RecyclerView.ViewHolder(itemView) {
        val title: TextView = itemView.findViewById(R.id.normalAttributeTitle)
        val value: TextView = itemView.findViewById(R.id.normalAttributeValue)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): AttributesNormalViewHolder {
        val view = LayoutInflater.from(context).inflate(R.layout.single_normal_attributes_ecomm, parent, false)
        return AttributesNormalAdapter.AttributesNormalViewHolder(view)
    }

    override fun getItemCount(): Int {
        return allData.size
    }

    override fun onBindViewHolder(holder: AttributesNormalViewHolder, position: Int) {
        val currentData = allData[position]
        for((key, value) in currentData){
            holder.title.text = key.toString() + " - "
            holder.value.text = value.toString()
        }
    }
}