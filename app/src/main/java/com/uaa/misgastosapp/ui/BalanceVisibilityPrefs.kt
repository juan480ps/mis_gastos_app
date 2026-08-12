package com.uaa.misgastosapp.ui

import android.content.Context
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf

/**
 * Recuerda si el usuario prefiere ocultar el balance en SummaryCard, para que la preferencia siga
 * aplicada al reabrir la app. rememberSaveable por si solo no alcanza: sobrevive a la navegacion
 * entre pantallas pero se resetea si el proceso de la app termina (ej. cerrarla del todo).
 */
object BalanceVisibilityPrefs {
    private const val PREFS = "balance_visibility_prefs"
    private const val KEY_VISIBLE = "is_balance_visible"

    @Volatile
    private var current: MutableState<Boolean>? = null

    fun isVisible(context: Context): MutableState<Boolean> {
        return current ?: synchronized(this) {
            current ?: mutableStateOf(load(context)).also { current = it }
        }
    }

    fun setVisible(context: Context, visible: Boolean) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit()
            .putBoolean(KEY_VISIBLE, visible)
            .apply()
        isVisible(context).value = visible
    }

    private fun load(context: Context): Boolean =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getBoolean(KEY_VISIBLE, true)
}
