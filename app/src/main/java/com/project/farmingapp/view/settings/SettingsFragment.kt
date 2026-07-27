package com.project.farmingapp.view.settings

import android.content.Context
import android.content.res.Configuration
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.LocaleListCompat
import androidx.fragment.app.Fragment
import com.google.android.material.switchmaterial.SwitchMaterial
import com.project.farmingapp.R
import java.util.Locale

class SettingsFragment : Fragment() {

    private lateinit var switchDarkMode: SwitchMaterial
    private lateinit var switchLanguage: SwitchMaterial

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_settings, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        switchDarkMode = view.findViewById(R.id.switchDarkMode)
        switchLanguage = view.findViewById(R.id.switchLanguage)

        val prefs = requireActivity().getSharedPreferences("MyPrefs", Context.MODE_PRIVATE)
        val isDark = prefs.getBoolean("darkModeEnabled", false)
        val currentLang = prefs.getString("language", "en")

        // Temporarily remove listener to avoid ANR loop
        switchDarkMode.setOnCheckedChangeListener(null)
        switchDarkMode.isChecked = isDark

        switchLanguage.setOnCheckedChangeListener(null)
        switchLanguage.isChecked = currentLang == "si"

        switchDarkMode.setOnCheckedChangeListener { _, isChecked ->
            prefs.edit().putBoolean("darkModeEnabled", isChecked).apply()
            AppCompatDelegate.setDefaultNightMode(
                if (isChecked) AppCompatDelegate.MODE_NIGHT_YES else AppCompatDelegate.MODE_NIGHT_NO
            )
        }

        switchLanguage.setOnCheckedChangeListener { _, isChecked ->
            val lang = if (isChecked) "si" else "en"
            prefs.edit().putString("language", lang).apply()
            
            // Apply language using the modern AndroidX API
            val localeList = LocaleListCompat.create(Locale(lang))
            AppCompatDelegate.setApplicationLocales(localeList)
        }
    }
}
