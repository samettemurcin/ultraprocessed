package com.b2.ultraprocessed.storage.preferences

import android.content.Context

class AppPreferences(context: Context) {
    private val prefs = context.applicationContext.getSharedPreferences(
        FILE_NAME,
        Context.MODE_PRIVATE,
    )

    var soundEffectsEnabled: Boolean
        get() = prefs.getBoolean(KEY_SOUND_EFFECTS_ENABLED, true)
        set(value) {
            prefs.edit().putBoolean(KEY_SOUND_EFFECTS_ENABLED, value).apply()
        }

    companion object {
        private const val FILE_NAME = "zest_app_preferences"
        private const val KEY_SOUND_EFFECTS_ENABLED = "sound_effects_enabled"
    }
}
