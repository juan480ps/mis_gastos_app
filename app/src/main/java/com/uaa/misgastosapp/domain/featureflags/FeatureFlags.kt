package com.uaa.misgastosapp.domain.featureflags

import android.content.Context

/**
 * Sistema de Feature Flags para controlar funcionalidades de la app.
 * Permite habilitar/deshabilitar features sin actualizar la app.
 */
class FeatureFlags(private val context: Context) {

    private val prefs = context.getSharedPreferences("feature_flags", Context.MODE_PRIVATE)

    companion object {
        // Core Features
        const val KEY_DARK_MODE = "feature_dark_mode"
        const val KEY_NOTIFICATIONS = "feature_notifications"
        const val KEY_BIOMETRIC_AUTH = "feature_biometric"
        
        // Premium Features
        const val KEY_CLOUD_SYNC = "feature_cloud_sync"
        const val KEY_MULTI_CURRENCY = "feature_multi_currency"
        const val KEY_EXPORT_PDF = "feature_export_pdf"
        
        // Experimental Features
        const val KEY_AI_CATEGORIZATION = "feature_ai_categorization"
        const val KEY_VOICE_INPUT = "feature_voice_input"
        const val KEY_WIDGETS = "feature_widgets"

        private val DEFAULT_FLAGS = mapOf(
            KEY_DARK_MODE to true,
            KEY_NOTIFICATIONS to true,
            KEY_BIOMETRIC_AUTH to true,
            KEY_CLOUD_SYNC to true,
            KEY_MULTI_CURRENCY to false,
            KEY_EXPORT_PDF to false,
            KEY_AI_CATEGORIZATION to false,
            KEY_VOICE_INPUT to false,
            KEY_WIDGETS to true
        )
    }

    fun isEnabled(flagKey: String): Boolean {
        return if (prefs.contains(flagKey)) {
            prefs.getBoolean(flagKey, false)
        } else {
            DEFAULT_FLAGS[flagKey] ?: false
        }
    }

    fun setEnabled(flagKey: String, enabled: Boolean) {
        prefs.edit().putBoolean(flagKey, enabled).apply()
    }

    fun getAllFlags(): Map<String, Boolean> {
        return DEFAULT_FLAGS.map { (key, defaultValue) ->
            key to (if (prefs.contains(key)) prefs.getBoolean(key, defaultValue) else defaultValue)
        }.toMap()
    }

    fun resetAll() {
        prefs.edit().clear().apply()
    }

    fun reset(flagKey: String) {
        prefs.edit().remove(flagKey).apply()
    }
}
