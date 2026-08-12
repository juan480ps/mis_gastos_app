package com.uaa.misgastosapp.ui.theme

import android.content.Context
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf

/**
 * Guarda y expone el tema elegido en ThemesScreen. GastosTheme (Theme.kt) observa
 * currentThemeId() para aplicar ese color en toda la app; antes, la seleccion se guardaba en
 * SharedPreferences pero nada la volvia a leer, asi que "elegir un tema" no cambiaba nada.
 *
 * null significa "el usuario nunca elige un tema": en ese caso GastosTheme sigue usando el
 * color dinamico de Android 12+ / los colores por defecto, como antes de que existiera esta pantalla.
 */
object ThemePrefs {
    private const val PREFS = "theme_prefs"
    private const val KEY_THEME = "selected_theme"

    @Volatile
    private var current: MutableState<String?>? = null

    fun currentThemeId(context: Context): MutableState<String?> {
        return current ?: synchronized(this) {
            current ?: mutableStateOf(loadThemeId(context)).also { current = it }
        }
    }

    fun selectTheme(context: Context, themeId: String) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit()
            .putString(KEY_THEME, themeId)
            .apply()
        currentThemeId(context).value = themeId
    }

    private fun loadThemeId(context: Context): String? =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getString(KEY_THEME, null)
}
