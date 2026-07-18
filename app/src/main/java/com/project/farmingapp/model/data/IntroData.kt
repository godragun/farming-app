package com.project.farmingapp.model.data

/**
 * @param imageAssetPath Path under `assets/` (e.g. `images/front_page_1.jpeg`), loaded via Glide.
 */
data class IntroData(
    val title: String,
    val description: String,
    val imageAssetPath: String
)
