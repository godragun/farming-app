package com.project.farmingapp.adapter

import android.content.Context
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.AnimationUtils
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.project.farmingapp.R
import com.project.farmingapp.model.data.WeatherList

class CurrentWeatherAdapter(val context: Context, val weatherrootdatas:List<WeatherList>):
    RecyclerView.Adapter<CurrentWeatherAdapter.CurrentWeatherViewHolder>() {
    class CurrentWeatherViewHolder(itemView: View): RecyclerView.ViewHolder(itemView) {
        var Ctemp=itemView.findViewById<TextView>(R.id.temp)
        var Cwedesc=itemView.findViewById<TextView>(R.id.desc)
        var Cwelogo=itemView.findViewById<ImageView>(R.id.icon)
        var CminTemp=itemView.findViewById<TextView>(R.id.minTemp)
        var CmaxTemp=itemView.findViewById<TextView>(R.id.maxTemp)
        var Chumidity=itemView.findViewById<TextView>(R.id.humidity)
        var CtodayTitle=itemView.findViewById<TextView>(R.id.todayTitle)
        var continer = itemView.findViewById<ConstraintLayout>(R.id.currentWeatherContainer)





    }

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): CurrentWeatherAdapter.CurrentWeatherViewHolder {
        val view= LayoutInflater.from(context).inflate(R.layout.single_currentweather,parent,false)
        return CurrentWeatherAdapter.CurrentWeatherViewHolder(view)
    }

    override fun getItemCount(): Int {
        return weatherrootdatas.size
    }

    override fun onBindViewHolder(holder: CurrentWeatherAdapter.CurrentWeatherViewHolder, position: Int) {
        val weathernew = weatherrootdatas[position]

        val tempC = (weathernew.main.temp - 273.15).toInt()
        holder.Ctemp.text = "$tempC\u00B0C"

        holder.Cwedesc.text = weathernew.weather[0].description.replaceFirstChar { c ->
            if (c.isLowerCase()) c.titlecase() else c.toString()
        }

        // Show only HH:MM from "yyyy-MM-dd HH:mm:ss"
        val timeStr = if (weathernew.dt_txt.length >= 16) weathernew.dt_txt.substring(11, 16) else "--:--"
        holder.CtodayTitle.text = timeStr

        holder.Chumidity.text = "${weathernew.main.humidity}%"

        // minTemp and maxTemp are hidden in new layout — set anyway to avoid crash
        holder.CminTemp.text = "${(weathernew.main.temp_min - 273.15f).toInt()}\u00B0C"
        holder.CmaxTemp.text = "${(weathernew.main.temp_max - 273.15f).toInt()}\u00B0C"

        holder.continer.animation = AnimationUtils.loadAnimation(context, R.anim.fade_scale)

        val iconUrl = "https://openweathermap.org/img/w/${weathernew.weather[0].icon}.png"
        Glide.with(holder.itemView.context)
            .load(iconUrl)
            .placeholder(R.drawable.ic_launcher_foreground)
            .error(R.drawable.ic_launcher_foreground)
            .into(holder.Cwelogo)
    }
}