package com.drapeproof.mobile.data

import android.content.Context
import android.content.SharedPreferences

object TutorialRepository {
    private const val PREFS_NAME = "drape_tutorial_prefs"
    private const val KEY_TUTORIAL_COMPLETED = "is_tutorial_completed"

    private fun getPrefs(context: Context): SharedPreferences {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }

    fun isTutorialCompleted(context: Context): Boolean {
        return getPrefs(context).getBoolean(KEY_TUTORIAL_COMPLETED, false)
    }

    fun setTutorialCompleted(context: Context, completed: Boolean = true) {
        getPrefs(context).edit().putBoolean(KEY_TUTORIAL_COMPLETED, completed).apply()
    }
}
