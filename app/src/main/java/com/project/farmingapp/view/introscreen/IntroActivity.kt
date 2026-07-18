package com.project.farmingapp.view.introscreen

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.viewpager2.widget.ViewPager2
import com.project.farmingapp.R
import com.project.farmingapp.adapter.IntroAdapter
import com.project.farmingapp.databinding.ActivityIntroBinding
import com.project.farmingapp.model.data.IntroData
import com.project.farmingapp.utilities.AgriAssetPaths
import com.project.farmingapp.view.auth.LoginActivity

class IntroActivity : AppCompatActivity() {

    companion object {
        /**
         * Bump when onboarding assets or flow change so existing installs see the new slider once.
         */
        const val INTRO_SCHEMA_VERSION = 4
        const val PREF_INTRO_SCHEMA_VERSION = "intro_schema_version"
    }

    private lateinit var binding: ActivityIntroBinding

    private val introSliderAdapter = IntroAdapter(
        listOf(
            IntroData(
                "Agri Lanka",
                "Meet your smart farming companion: practical tools, trusted guidance, and everything you need in one place.",
                AgriAssetPaths.FRONT_PAGE_1
            ),
            IntroData(
                "Your Agri dashboard",
                "See weather at a glance, jump to programs, articles, field survey, and shopping without losing context.",
                AgriAssetPaths.FRONT_PAGE_2
            ),
            IntroData(
                "Agri Chatbot",
                "Ask common farming questions any time—varieties, irrigation timing, and crop care pointers in plain language.",
                AgriAssetPaths.FRONT_PAGE_3
            ),
            IntroData(
                "Weather forecasting",
                "Plan irrigation and field work with local temperature, humidity, wind, and multi-day outlooks.",
                AgriAssetPaths.FRONT_PAGE_4
            ),
            IntroData(
                "Markets & satellite maps",
                "Explore pricing insights, satellite maps, and government programs in a few taps.",
                AgriAssetPaths.FRONT_PAGE_5
            ),
            IntroData(
                "E‑commerce & more",
                "Browse inputs and produce listings, then continue your work with a consistent Agri Lanka experience.",
                AgriAssetPaths.FRONT_PAGE_6
            ),
            IntroData(
                "Field data & programs",
                "Collect field survey data offline, sync when you are online, and follow schemes tailored for growers.",
                AgriAssetPaths.INTRO_ONBOARDING
            ),
            IntroData(
                "Ready when you are",
                "Tap Get Started to sign in or create an account—then grow together with Agri Lanka.",
                AgriAssetPaths.FRONT_PAGE_8
            )
        )
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityIntroBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Ensure Skip always appears above the ViewPager2 swipe layer
        binding.skipIntro.bringToFront()

        binding.sliderViewPager.adapter = introSliderAdapter
        setupIndicators()
        setCurrentIndicator(0)

        binding.sliderViewPager.registerOnPageChangeCallback(object :
            ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {
                super.onPageSelected(position)
                setCurrentIndicator(position)
                // Update button text based on position
                binding.nextBtn.text = if (position + 1 == introSliderAdapter.itemCount) {
                    getString(R.string.get_started_intro)
                } else {
                    getString(R.string.next_intro)
                }
            }
        })

        if (binding.sliderViewPager.currentItem + 1 == introSliderAdapter.itemCount) {
            binding.nextBtn.text = getString(R.string.get_started_intro)
        } else {
            binding.nextBtn.text = getString(R.string.next_intro)
        }

        binding.nextBtn.setOnClickListener {
            if (binding.sliderViewPager.currentItem + 1 < introSliderAdapter.itemCount) {
                binding.sliderViewPager.currentItem += 1
                binding.nextBtn.text = getString(R.string.next_intro)
                if (binding.sliderViewPager.currentItem + 1 == introSliderAdapter.itemCount) {
                    binding.nextBtn.text = getString(R.string.get_started_intro)
                }
            } else {
                completeIntro()
            }
        }
        binding.skipIntro.setOnClickListener {
            completeIntro()
        }
    }

    private fun completeIntro() {
        val sharedPreferences = getSharedPreferences("MyPrefs", Context.MODE_PRIVATE)
        sharedPreferences.edit()
            .putBoolean("firstTime", false)
            .putInt(PREF_INTRO_SCHEMA_VERSION, INTRO_SCHEMA_VERSION)
            .apply()
        startActivity(Intent(this, LoginActivity::class.java))
        finish()
    }

    private fun setupIndicators() {
        val indicators = arrayOfNulls<ImageView>(introSliderAdapter.itemCount)
        val layoutParams: LinearLayout.LayoutParams =
            LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            )
        layoutParams.setMargins(12, 0, 12, 0)

        for (i in indicators.indices) {
            indicators[i] = ImageView(applicationContext)
            indicators[i].apply {
                this?.setImageDrawable(
                    ContextCompat.getDrawable(
                        applicationContext,
                        R.drawable.indicator_inactive
                    )
                )
                this?.layoutParams = layoutParams
            }

            binding.sliderballsContainer.addView(indicators[i])
        }
    }

    private fun setCurrentIndicator(index: Int) {
        val childCount = binding.sliderballsContainer.childCount
        for (i in 0 until childCount) {
            val imageView = binding.sliderballsContainer.getChildAt(i) as ImageView
            if (i == index) {
                imageView.setImageDrawable(
                    ContextCompat.getDrawable(
                        applicationContext,
                        R.drawable.indicator_active
                    )
                )
            } else {
                imageView.setImageDrawable(
                    ContextCompat.getDrawable(
                        applicationContext,
                        R.drawable.indicator_inactive
                    )
                )
            }
        }

        if (index == introSliderAdapter.itemCount - 1) {
            binding.nextBtn.text = getString(R.string.get_started_intro)
        } else {
            binding.nextBtn.text = getString(R.string.next_intro)
        }
    }
}
