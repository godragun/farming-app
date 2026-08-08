package com.project.farmingapp.view.socialmedia

import com.project.farmingapp.R
import com.project.farmingapp.adapter.OfflinePostItem

object LocalPostDraftStore {
    private val drafts = mutableListOf<OfflinePostItem>()

    fun addDraft(title: String, description: String) {
        drafts.add(
            0,
            OfflinePostItem(
                name = "You",
                title = title,
                description = description.ifBlank { "No description" },
                uploadedAt = System.currentTimeMillis(),
                imageRes = R.drawable.disease,
                imageAssetName = com.project.farmingapp.utilities.AgriAssetPaths.PLANTS_I_GREW_POST
            )
        )
    }

    fun allDrafts(): List<OfflinePostItem> = drafts.toList()
}
