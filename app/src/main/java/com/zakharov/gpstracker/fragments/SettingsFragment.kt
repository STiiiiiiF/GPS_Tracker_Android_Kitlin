package com.zakharov.gpstracker.fragments

import android.graphics.Color
import android.os.Bundle
import android.widget.Toast
import androidx.preference.Preference
import androidx.preference.Preference.OnPreferenceChangeListener
import androidx.preference.PreferenceFragmentCompat
import com.zakharov.gpstracker.R

class SettingsFragment : PreferenceFragmentCompat() {

    private lateinit var timePref: Preference
    private lateinit var colorPref: Preference

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        setPreferencesFromResource(R.xml.main_preference, rootKey)
        init()
    }

    private fun init() {
        timePref = findPreference("update_time_key")!!
        colorPref = findPreference("color_key")!!
        val changeListener = onChangeListener()
        timePref.onPreferenceChangeListener = changeListener
        colorPref.onPreferenceChangeListener = changeListener
        initPrefs()
    }

    private fun onChangeListener(): OnPreferenceChangeListener {
        return OnPreferenceChangeListener { pref, value ->
            when (pref.key) {
                "update_time_key" -> onTimeChange(value.toString())
                "color_key" -> onColorChange(value.toString())
            }
            true
        }
    }

    private fun onTimeChange(value: String) {
        val nameArray = resources.getStringArray(R.array.loc_time_update_name)
        val valueArray = resources.getStringArray(R.array.loc_time_update_value)
        val title = timePref.title.toString().substringBefore(":")
        timePref.title = "$title: ${nameArray[valueArray.indexOf(value)]}"
    }

    private fun onColorChange(value: String) {
        colorPref.icon?.setTint(Color.parseColor(value))
        val nameArray = resources.getStringArray(R.array.color_name)
        val valueArray = resources.getStringArray(R.array.color_value)
        val summary = "Your choose".substringBefore(":")
        colorPref.summary = "$summary: ${nameArray[valueArray.indexOf(value)]}"


    }


    private fun initPrefs() {
        val pref = timePref.preferenceManager.sharedPreferences!!
        val nameArray = resources.getStringArray(R.array.loc_time_update_name)
        val valueArray = resources.getStringArray(R.array.loc_time_update_value)
        val title = timePref.title
        timePref.title =
            "$title: ${nameArray[valueArray.indexOf(pref.getString("update_time_key", "3000"))]}"

        val trackColor = pref.getString("color_key", "#FF000000")
        colorPref.icon?.setTint(Color.parseColor(trackColor))

    }
}