package com.project.farmingapp.view.ecommerce

import android.content.Context
import android.os.Bundle
import com.project.farmingapp.R
import com.project.farmingapp.adapter.OfflineEcommerceItem

object OfflineEcommerceData {

    fun getItems(context: Context): List<OfflineEcommerceItem> {
        return listOf(
            OfflineEcommerceItem(
                id = "offline_crop_chilli",
                title = "Pro Farm Tractor Rental",
                price = 12500,
                retailer = "Agri Lanka Machinery",
                availability = "In Stock",
                rating = 4.6f,
                type = "machine",
                imageRes = R.drawable.fruits,
                imageAssetName = "images/tractor.jpg",
                shortDesc = "Heavy-duty tractor rental for field prep and tilling.",
                longDesc = "Ideal for ploughing, leveling, and post-harvest field clearing with fuel-efficient operation for medium and large farms.",
                howToUse = "Book by day(s). Use trained operator guidance for soil depth and safe turning radius.",
                deliveryCharge = 1200
            ),
            OfflineEcommerceItem(
                id = "offline_crop_coconut",
                title = "Premium Seed Pack",
                price = 2450,
                retailer = "GreenLife Inputs",
                availability = "In Stock",
                rating = 4.5f,
                type = "fertilizer",
                imageRes = R.drawable.fruits,
                imageAssetName = "images/seeds.jpg",
                shortDesc = "High-germination seed collection for seasonal crops.",
                longDesc = "Seed mix selected for Sri Lankan climate zones with disease tolerance and stable early-stage growth.",
                howToUse = "Sow in prepared nursery trays or raised beds, keep moisture stable, and transplant healthy seedlings.",
                deliveryCharge = 250
            ),
            OfflineEcommerceItem(
                id = "offline_crop_eggplant",
                title = "Fresh Tomatoes",
                price = 780,
                retailer = "Daily Harvest Market",
                availability = "Limited Stock",
                rating = 4.2f,
                type = "pestiside",
                imageRes = R.drawable.fruits,
                imageAssetName = "images/tomato.jpg",
                shortDesc = "Farm-fresh tomatoes with same-day packing.",
                longDesc = "Naturally ripened tomatoes with firm skin and high pulp quality for salads, sauces, and home kitchens.",
                howToUse = "Store in cool dry area and consume within 3-4 days for best taste.",
                deliveryCharge = 150
            ),
            OfflineEcommerceItem(
                id = "offline_crop_rice",
                title = "Garden Rake",
                price = 1800,
                retailer = "Field Tools Depot",
                availability = "In Stock",
                rating = 4.1f,
                type = "machine",
                imageRes = R.drawable.fruits,
                imageAssetName = "images/rake.jpg",
                shortDesc = "Sturdy rake for clearing leaves and loosening topsoil.",
                longDesc = "Lightweight but durable rake suitable for home gardens and larger nurseries to improve soil aeration.",
                howToUse = "Use after watering to break crusted topsoil and remove dry debris around plants.",
                deliveryCharge = 200
            ),
            OfflineEcommerceItem(
                id = "offline_crop_tea",
                title = "Steel Shovel",
                price = 2350,
                retailer = "Agro Hardware Center",
                availability = "In Stock",
                rating = 4.4f,
                type = "machine",
                imageRes = R.drawable.fruits,
                imageAssetName = "images/shovel.jpg",
                shortDesc = "Rust-resistant shovel for digging and transplanting.",
                longDesc = "Balanced grip and reinforced blade make it suitable for trenching, compost turning, and pit preparation.",
                howToUse = "Use with proper posture; clean after each use to extend blade life.",
                deliveryCharge = 220
            ),
            OfflineEcommerceItem(
                id = "offline_produce_cucumber",
                title = "Fresh Cucumbers",
                price = 280,
                retailer = "Daily Harvest Market",
                availability = "In Stock",
                rating = 4.3f,
                type = "crop",
                imageRes = R.drawable.fruits,
                imageAssetName = "images/cucumber.jpeg",
                shortDesc = "Crunchy, field-fresh cucumbers picked this morning.",
                longDesc = "Locally grown cucumbers with crisp texture and high water content, ideal for salads, raitas, and daily home cooking.",
                howToUse = "Refrigerate unwashed in a breathable bag; wash just before use for best freshness.",
                deliveryCharge = 150
            ),
            OfflineEcommerceItem(
                id = "offline_produce_dhal",
                title = "Red Dhal (Masoor)",
                price = 680,
                retailer = "Agri Lanka Grains",
                availability = "In Stock",
                rating = 4.6f,
                type = "crop",
                imageRes = R.drawable.fruits,
                imageAssetName = "images/dhal.jpeg",
                shortDesc = "Cleaned and sorted red dhal, 1 kg pack.",
                longDesc = "Protein-rich red lentils, quick to cook and suitable for everyday curries, soups, and tempered dhal.",
                howToUse = "Rinse, soak 10 minutes, then simmer with turmeric and salt until soft; temper with spices.",
                deliveryCharge = 180
            ),
            OfflineEcommerceItem(
                id = "offline_produce_muatta",
                title = "Mung Beans (Muatta)",
                price = 820,
                retailer = "GreenLife Inputs",
                availability = "In Stock",
                rating = 4.5f,
                type = "crop",
                imageRes = R.drawable.fruits,
                imageAssetName = "images/muatta.jpeg",
                shortDesc = "Whole green mung beans, farmer-packed 1 kg.",
                longDesc = "Traditional mung beans (muatta) sorted and cleaned for cooking kiribath sides, sprouting, and healthy snacks.",
                howToUse = "Soak overnight, boil until tender, or sprout for salads and stir-fries.",
                deliveryCharge = 180
            ),
            OfflineEcommerceItem(
                id = "offline_produce_peanuts",
                title = "Raw Peanuts (Rathu Kadala)",
                price = 890,
                retailer = "Daily Harvest Market",
                availability = "In Stock",
                rating = 4.4f,
                type = "crop",
                imageRes = R.drawable.fruits,
                imageAssetName = "images/penuts.jpeg",
                shortDesc = "Fresh shelled peanuts, 1 kg.",
                longDesc = "Hand-sorted peanuts suitable for roasting, curries, and making fresh peanut butter at home.",
                howToUse = "Dry-roast on low flame until skins loosen; store in an airtight jar after cooling.",
                deliveryCharge = 180
            ),
            OfflineEcommerceItem(
                id = "offline_input_pesticide",
                title = "Crop Protection Pesticide",
                price = 2850,
                retailer = "Agro Hardware Center",
                availability = "In Stock",
                rating = 4.2f,
                type = "pestiside",
                imageRes = R.drawable.fruits,
                imageAssetName = "images/pesttersitiods.jpeg",
                shortDesc = "Broad-spectrum pesticide for common field pests.",
                longDesc = "Formulated for paddy, vegetables, and fruit crops to control sucking and chewing insects while protecting yield.",
                howToUse = "Follow label dosage; mix with clean water and spray in early morning or late evening using PPE.",
                deliveryCharge = 300
            ),
            OfflineEcommerceItem(
                id = "offline_seed_samba",
                title = "Samba Rice Seed (Wee Ata)",
                price = 520,
                retailer = "GreenLife Inputs",
                availability = "In Stock",
                rating = 4.7f,
                type = "fertilizer",
                imageRes = R.drawable.fruits,
                imageAssetName = "images/samba wee ata.jpeg",
                shortDesc = "Certified Samba paddy seed, 5 kg pack.",
                longDesc = "High-germination Samba rice seed suitable for Yala and Maha seasons with good tillering and grain quality.",
                howToUse = "Pre-soak 12 hours, drain, and broadcast or transplant in well-puddled fields with stable water level.",
                deliveryCharge = 250
            ),
            OfflineEcommerceItem(
                id = "offline_input_urea",
                title = "Urea Fertilizer (50 kg)",
                price = 4650,
                retailer = "Agri Lanka Inputs",
                availability = "In Stock",
                rating = 4.5f,
                type = "fertilizer",
                imageRes = R.drawable.fruits,
                imageAssetName = "images/uria.jpeg",
                shortDesc = "Urea 46% N, 50 kg nitrogen fertilizer.",
                longDesc = "High-analysis nitrogen source for paddy, vegetables, and fruit crops to boost vegetative growth and yield.",
                howToUse = "Apply as per soil test and crop stage; split application reduces losses and improves uptake.",
                deliveryCharge = 450
            )
        )
    }

    fun toBundle(item: OfflineEcommerceItem): Bundle = Bundle().apply {
        putBoolean("offline_item", true)
        putString("offline_item_id", item.id)
        putString("offline_title", item.title)
        putString("offline_short_desc", item.shortDesc)
        putString("offline_long_desc", item.longDesc)
        putString("offline_how_to_use", item.howToUse)
        putString("offline_retailer", item.retailer)
        putString("offline_availability", item.availability)
        putString("offline_type", item.type)
        putInt("offline_price", item.price)
        putInt("offline_delivery_charge", item.deliveryCharge)
        putFloat("offline_rating", item.rating)
        putInt("offline_image_res", item.imageRes)
        putString("offline_image_asset", item.imageAssetName)
    }

}
