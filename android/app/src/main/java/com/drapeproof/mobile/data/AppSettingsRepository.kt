package com.drapeproof.mobile.data

import android.content.Context
import android.content.SharedPreferences

enum class AppThemeMode {
    SYSTEM,
    LIGHT,
    DARK,
}

object AppSettingsRepository {
    private const val PREFS_NAME = "drapeit_app_settings"
    private const val KEY_THEME_MODE = "pref_theme_mode"
    private const val KEY_SOUND_ENABLED = "pref_sound_enabled"
    private const val KEY_HAPTIC_ENABLED = "pref_haptic_enabled"
    private const val KEY_INTERACTIVE_TOUR_DONE = "pref_interactive_tour_done"

    private fun getPrefs(context: Context): SharedPreferences {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }

    fun getThemeMode(context: Context): AppThemeMode {
        val name = getPrefs(context).getString(KEY_THEME_MODE, AppThemeMode.SYSTEM.name)
        return runCatching { AppThemeMode.valueOf(name ?: AppThemeMode.SYSTEM.name) }.getOrDefault(AppThemeMode.SYSTEM)
    }

    fun setThemeMode(context: Context, mode: AppThemeMode) {
        getPrefs(context).edit().putString(KEY_THEME_MODE, mode.name).apply()
    }

    fun isSoundEnabled(context: Context): Boolean {
        return getPrefs(context).getBoolean(KEY_SOUND_ENABLED, true)
    }

    fun setSoundEnabled(context: Context, enabled: Boolean) {
        getPrefs(context).edit().putBoolean(KEY_SOUND_ENABLED, enabled).apply()
    }

    fun isHapticEnabled(context: Context): Boolean {
        return getPrefs(context).getBoolean(KEY_HAPTIC_ENABLED, true)
    }

    fun setHapticEnabled(context: Context, enabled: Boolean) {
        getPrefs(context).edit().putBoolean(KEY_HAPTIC_ENABLED, enabled).apply()
    }

    fun isInteractiveTourDone(context: Context): Boolean {
        return getPrefs(context).getBoolean(KEY_INTERACTIVE_TOUR_DONE, false)
    }

    fun setInteractiveTourDone(context: Context, done: Boolean) {
        getPrefs(context).edit().putBoolean(KEY_INTERACTIVE_TOUR_DONE, done).apply()
    }
}
