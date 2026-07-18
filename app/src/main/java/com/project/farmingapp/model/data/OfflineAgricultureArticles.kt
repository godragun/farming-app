package com.project.farmingapp.model.data

import android.os.Bundle
import com.project.farmingapp.utilities.AgriAssetPaths

data class OfflineAgricultureArticle(
    val title: String,
    val imageAsset: String,
    val description: String,
    val process: String,
    val soil: String,
    val state: String,
    val temperature: String,
    val time: String,
    val diseases: String,
    val weight: String,
    val vitamins: String,
    val treeHeight: String,
    val growthTime: String,
    val howGrown: String
)

object OfflineAgricultureArticles {

    val all: List<OfflineAgricultureArticle> = listOf(
        OfflineAgricultureArticle(
            title = "Pineapple",
            imageAsset = AgriAssetPaths.PINEAPPLE,
            description = "Pineapple suits warm lowlands and well-drained soils, with steady moisture during fruit development.",
            process = "Use suckers or crowns, plant on raised beds or ridges, mulch to reduce weeds, and protect inflorescence during fruiting.",
            soil = "Sandy loam, slightly acidic (pH 4.5-6.0)",
            state = "Gampaha, Kurunegala, Puttalam",
            temperature = "24C - 32C",
            time = "Year-round planting; 16-20 months to harvest",
            diseases = "Heart rot, base rot",
            weight = "1-3 kg per fruit typical",
            vitamins = "Vitamin C, manganese",
            treeHeight = "Rosette to 1-1.5 m",
            growthTime = "16-20 months",
            howGrown = "Pineapple is propagated from suckers or crowns, spaced for sunlight, and fed with potassium-heavy nutrition as the fruit swells."
        ),
        OfflineAgricultureArticle(
            title = "Tea",
            imageAsset = AgriAssetPaths.TEA_LEAVES,
            description = "Highland tea needs reliable rain, cool nights, and acidic soils for tender flush and quality leaf.",
            process = "Contour planting, mulch young lines, shade management where needed, and disciplined pruning cycles.",
            soil = "Acidic, well-drained loam",
            state = "Nuwara Eliya, Kandy, Hatton",
            temperature = "18C - 25C",
            time = "Year-round with seasonal flushes",
            diseases = "Blister blight, stem canker",
            weight = "Leaf harvest every 7-10 days",
            vitamins = "Antioxidants, mild caffeine",
            treeHeight = "Bush table ~60-90 cm",
            growthTime = "3-4 years to full plucking",
            howGrown = "Tea is maintained as a plucking table on slopes with regular pruning, soil conservation, and factory-ready flush timing."
        ),
        OfflineAgricultureArticle(
            title = "Potato",
            imageAsset = AgriAssetPaths.POTATO,
            description = "Potato prefers cool nights and friable soil; avoid waterlogging to reduce tuber rot.",
            process = "Chit seed, ridge plant, hill soil as stems grow, and stop irrigation before senescence for skin set.",
            soil = "Loamy sand to sandy loam, good drainage",
            state = "Nuwara Eliya, Badulla (upcountry)",
            temperature = "15C - 22C",
            time = "Cool season cycles",
            diseases = "Late blight, bacterial wilt",
            weight = "Market tubers 80-200 g",
            vitamins = "Vitamin C, potassium",
            treeHeight = "40-80 cm canopy",
            growthTime = "90-120 days",
            howGrown = "Potato is grown from certified seed tubers on ridges with hilling and balanced N-P-K, stopping irrigation before harvest."
        ),
        OfflineAgricultureArticle(
            title = "Modern plantation",
            imageAsset = AgriAssetPaths.NEW_PLANTATION_WAY_POST,
            description = "Combines contour layout, water control, and mechanised soil prep to establish uniform stands with less runoff.",
            process = "Laser or contour levelling, basins or drip laterals, quality planting material, and staged nutrition.",
            soil = "Matched to crop; focus on stable structure and organic matter",
            state = "Island-wide by agro-ecological zone",
            temperature = "Follow target crop range",
            time = "Align with Maha/Yala rains or irrigation windows",
            diseases = "Integrated pest scouting from week one",
            weight = "Yield targets per crop advisory",
            vitamins = "N/A (system guide)",
            treeHeight = "N/A",
            growthTime = "Full rotation per enterprise plan",
            howGrown = "Use raised beds or contour strips, stabilise topsoil with mulch, and schedule irrigation to match crop stage for strong early vigour."
        ),
        OfflineAgricultureArticle(
            title = "Chilli",
            imageAsset = "images/chilli.jpg",
            description = "Chilli performs best in warm weather with good sunlight and moderate irrigation.",
            process = "Raise seedlings, transplant on raised beds, and stake early for airflow.",
            soil = "Well-drained sandy loam (pH 6.0-7.0)",
            state = "Kandy, Matale, Badulla",
            temperature = "20C - 30C",
            time = "Dry season with irrigation support",
            diseases = "Leaf curl, anthracnose",
            weight = "80-120 g per fruit",
            vitamins = "Vitamin C and antioxidants",
            treeHeight = "60-120 cm",
            growthTime = "80-95 days",
            howGrown = "Chilli is grown from nursery seedlings transplanted to raised rows with organic compost and regular pruning."
        ),
        OfflineAgricultureArticle(
            title = "Rice",
            imageAsset = "images/rice_plant.png",
            description = "Rice is a staple crop in Sri Lanka, ideal for irrigated lowlands.",
            process = "Prepare nursery, transplant seedlings, maintain water levels, and top-dress nutrients.",
            soil = "Clay loam / alluvial soils",
            state = "Anuradhapura, Polonnaruwa",
            temperature = "25C - 35C",
            time = "Yala and Maha seasons",
            diseases = "Leaf blast, sheath blight",
            weight = "High grain yield under irrigation",
            vitamins = "Carbohydrate-rich staple",
            treeHeight = "90-140 cm depending on variety",
            growthTime = "100-140 days",
            howGrown = "Rice is grown in puddled fields where seedlings are transplanted from nursery beds and managed through stage-wise irrigation."
        ),
        OfflineAgricultureArticle(
            title = "Coconut",
            imageAsset = "images/coconut.jpeg",
            description = "Coconut prefers warm coastal and intermediate climates with good sun exposure.",
            process = "Plant with spacing, apply organic matter, irrigate in dry months, and maintain basins.",
            soil = "Sandy loam / lateritic soils",
            state = "Kurunegala, Puttalam, Gampaha",
            temperature = "24C - 32C",
            time = "Planting during rainy onset",
            diseases = "Bud rot, stem bleeding",
            weight = "Regular nut production after maturity",
            vitamins = "Healthy fats and minerals",
            treeHeight = "10-30 m",
            growthTime = "5-7 years to bearing",
            howGrown = "Coconut is grown in deep pits with wide spacing and basin mulching, then maintained with seasonal manuring and moisture control."
        )
    )

    fun bundleForTitle(title: String): Bundle? {
        val article = all.firstOrNull { it.title == title } ?: return null
        return Bundle().apply {
            putBoolean("offline_article", true)
            putString("offline_title", article.title)
            putString("offline_desc", article.description)
            putString("offline_process", article.process)
            putString("offline_soil", article.soil)
            putString("offline_state", article.state)
            putString("offline_temp", article.temperature)
            putString("offline_time", article.time)
            putString("offline_diseases", article.diseases)
            putString("offline_weight", article.weight)
            putString("offline_vitamins", article.vitamins)
            putString("offline_tree_height", article.treeHeight)
            putString("offline_growth_time", article.growthTime)
            putString("offline_how_grown", article.howGrown)
            putString("offline_image_asset", article.imageAsset)
        }
    }
}
