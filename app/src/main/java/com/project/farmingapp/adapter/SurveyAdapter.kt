package com.project.farmingapp.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.project.farmingapp.databinding.SingleSurveyItemBinding
import com.project.farmingapp.model.data.FieldSurvey
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class SurveyAdapter(
    private val onView: (FieldSurvey) -> Unit,
    private val onSync: (FieldSurvey) -> Unit,
    private val onDelete: (FieldSurvey) -> Unit
) : ListAdapter<FieldSurvey, SurveyAdapter.ViewHolder>(DIFF) {

    inner class ViewHolder(private val b: SingleSurveyItemBinding) :
        RecyclerView.ViewHolder(b.root) {

        fun bind(survey: FieldSurvey, position: Int) {
            b.surveyItemNumber.text = "Survey #%03d".format(position + 1)
            b.surveyItemCrop.text = "🌾 ${survey.cropType.ifEmpty { "—" }}"
            b.surveyItemLocation.text = "📍 ${survey.locationText.ifEmpty { "—" }}"
            b.surveyItemFarmer.text = "👤 ${survey.farmerName.ifEmpty { "—" }}"
            b.surveyItemTime.text = "🕐 ${formatTime(survey.savedAt)}"

            if (survey.isSynced) {
                b.surveyItemSyncBadge.text = "✔ Synced"
                b.surveyItemSyncBadge.setTextColor(b.root.context.getColor(com.project.farmingapp.R.color.colorPrimary))
            } else {
                b.surveyItemSyncBadge.text = "⏳ Pending"
                b.surveyItemSyncBadge.setTextColor(b.root.context.getColor(com.project.farmingapp.R.color.colorThird))
            }

            b.surveyItemViewBtn.setOnClickListener { onView(survey) }
            b.surveyItemSyncBtn.setOnClickListener { onSync(survey) }
            b.surveyItemDeleteBtn.setOnClickListener { onDelete(survey) }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder =
        ViewHolder(SingleSurveyItemBinding.inflate(LayoutInflater.from(parent.context), parent, false))

    override fun onBindViewHolder(holder: ViewHolder, position: Int) =
        holder.bind(getItem(position), position)

    private fun formatTime(millis: Long): String {
        val now = System.currentTimeMillis()
        val diff = now - millis
        return when {
            diff < 60_000 -> "Just now"
            diff < 3_600_000 -> "${diff / 60_000}m ago"
            diff < 86_400_000 -> SimpleDateFormat("h:mm a", Locale.getDefault()).format(Date(millis))
            diff < 172_800_000 -> "Yesterday"
            else -> SimpleDateFormat("MMM d", Locale.getDefault()).format(Date(millis))
        }
    }

    companion object {
        private val DIFF = object : DiffUtil.ItemCallback<FieldSurvey>() {
            override fun areItemsTheSame(a: FieldSurvey, b: FieldSurvey) = a.id == b.id
            override fun areContentsTheSame(a: FieldSurvey, b: FieldSurvey) = a == b
        }
    }
}
