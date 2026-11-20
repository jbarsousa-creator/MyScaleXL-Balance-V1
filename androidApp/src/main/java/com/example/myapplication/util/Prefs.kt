package com.example.myapplication.util

import android.content.Context
import android.content.SharedPreferences
import android.content.res.Configuration
import android.content.res.Resources
import java.util.Locale

class Prefs(context: Context) {
    private val sp: SharedPreferences =
        context.getSharedPreferences("myscale_prefs", Context.MODE_PRIVATE)

    /** "system", "pt", "fr" ou "en" */
    var language: String
        get() = sp.getString("language", "system") ?: "system"
        set(value) { sp.edit().putString("language", value).apply() }

    var apiKey: String
        get() = sp.getString("api_key", "") ?: ""
        set(value) { sp.edit().putString("api_key", value.trim()).apply() }

    /** 0 = automático */
    var dailyGoal: Int
        get() = sp.getInt("daily_goal", 0)
        set(value) { sp.edit().putInt("daily_goal", value).apply() }
}

object LocaleHelper {
    fun wrap(base: Context): Context {
        val lang = Prefs(base).language
        if (lang == "system") {
            Locale.setDefault(Resources.getSystem().configuration.locales[0])
            return base
        }
        val locale = Locale(lang)
        Locale.setDefault(locale)
        val config = Configuration(base.resources.configuration)
        config.setLocale(locale)
        return base.createConfigurationContext(config)
    }
}
