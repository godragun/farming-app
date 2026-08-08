package com.project.farmingapp.utilities

/**
 * Central paths under [android_asset] for bundled images (see app/src/main/assets/images/).
 */
object AgriAssetPaths {
    const val IMAGES = "images"

    const val PLANTS_I_GREW_POST = "$IMAGES/the_plants_i_grew_post.jpeg"
    const val SEED_SPREADING = "$IMAGES/seed_spreading.jpeg"
    const val SPRINKLING_SYSTEM_POST = "$IMAGES/new_sprinkling_system_post.jpeg"
    const val PADDY_WATER_MANAGEMENT = "$IMAGES/paddy_field_water_management.jpeg"
    const val NEW_PLANTATION_WAY_POST = "$IMAGES/new_plantation_way_post.jpeg"

    const val FRONT_PAGE_1 = "$IMAGES/front_page_1.jpeg"
    const val FRONT_PAGE_2 = "$IMAGES/front_page_2.jpeg"
    const val FRONT_PAGE_3 = "$IMAGES/front_page_3.jpeg"
    const val FRONT_PAGE_4 = "$IMAGES/front_page_4.jpeg"
    const val FRONT_PAGE_5 = "$IMAGES/front_page_5.jpeg"
    const val FRONT_PAGE_6 = "$IMAGES/front_page_6.jpeg"
    /** No separate `front_page_7` file in bundle; used for the 7th onboarding slide. */
    const val INTRO_ONBOARDING = "$IMAGES/onboarding.png"
    const val FRONT_PAGE_8 = "$IMAGES/front_page_8.jpeg"

    const val LOGIN_SIGN_HERO = "$IMAGES/login_sign_page.jpeg"

    const val LOGO_WITH_LABEL = "$IMAGES/agri_logo_3.png"
    const val LOGO_TRANSPARENT = "$IMAGES/agri_logo_final_3.png"

    const val PINEAPPLE = "$IMAGES/pineapple.jpeg"
    const val TEA_LEAVES = "$IMAGES/tea_leaves.jpeg"
    const val POTATO = "$IMAGES/potato.jpeg"
    const val LEAF_DISEASE = "$IMAGES/leaf_disease.png"
    const val ORGANIC_FERTILIZER = "$IMAGES/uria.jpeg"
    const val TOMATO_CROP = "$IMAGES/tomato.jpg"
    const val CHILLI_CROP = "$IMAGES/chilli.jpg"

    fun fileUri(pathUnderAssets: String) = "file:///android_asset/$pathUnderAssets"
}
