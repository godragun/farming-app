package com.project.farmingapp.utilities

import android.app.Activity
import android.content.Context
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import com.project.farmingapp.R

fun Context.toast(message: String) {
    showAgriToastOrFallback(this, message, Toast.LENGTH_LONG)
}

fun Activity.showAgriMessage(message: String) {
    showAgriToastOrFallback(this, message, Toast.LENGTH_LONG)
}

private fun showAgriToastOrFallback(context: Context, message: String, duration: Int) {
    try {
        val inflater = LayoutInflater.from(context)
        val view = inflater.inflate(R.layout.toast_agri_brand, null)
        view.findViewById<TextView>(R.id.toast_text).text = message
        view.findViewById<ImageView>(R.id.toast_icon).setImageResource(R.drawable.ic_agri_launcher_foreground)
        @Suppress("DEPRECATION")
        val t = Toast(context)
        t.duration = duration
        t.setGravity(Gravity.BOTTOM or Gravity.CENTER_HORIZONTAL, 0, dp(context, 88))
        t.view = view
        t.show()
    } catch (_: Exception) {
        Toast.makeText(context, message, duration).show()
    }
}

private fun dp(context: Context, v: Int): Int =
    (v * context.resources.displayMetrics.density).toInt()

fun ProgressBar.show() {
    visibility = View.VISIBLE
}

fun ProgressBar.hide() {
    visibility = View.GONE
}

const val CURRENCY_PREFIX = "Rs. "

fun formatLkr(amount: Int): String = CURRENCY_PREFIX + amount.toString()

fun parseAmount(raw: String): Int {
    val digits = raw.filter { it.isDigit() }
    return digits.toIntOrNull() ?: 0
}
