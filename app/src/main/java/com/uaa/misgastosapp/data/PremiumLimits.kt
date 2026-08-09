package com.uaa.misgastosapp.data

import android.content.Context

/**
 * Límites de la app según el plan (Free vs Premium).
 * Free: datos limitados, Premium: todo ilimitado.
 */
object PremiumLimits {

    // Límites FREE
    const val FREE_MAX_CATEGORIES = 10
    const val FREE_MAX_RECURRING_TRANSACTIONS = 5
    const val FREE_MAX_BUDGETS = 3
    const val FREE_HISTORY_MONTHS = 3

    // Límites PREMIUM (sin límite = -1)
    const val PREMIUM_MAX_CATEGORIES = -1
    const val PREMIUM_MAX_RECURRING_TRANSACTIONS = -1
    const val PREMIUM_MAX_BUDGETS = -1
    const val PREMIUM_HISTORY_MONTHS = -1

    /**
     * Verifica si el usuario puede agregar una categoría.
     * @return true si puede agregar, false si alcanzó el límite
     */
    fun canAddCategory(context: Context, currentCount: Int): Boolean {
        val isPremium = PremiumManager.getInstance(context).isPremium.value
        if (isPremium) return true
        return currentCount < FREE_MAX_CATEGORIES
    }

    /**
     * Verifica si el usuario puede agregar una transacción recurrente.
     */
    fun canAddRecurringTransaction(context: Context, currentCount: Int): Boolean {
        val isPremium = PremiumManager.getInstance(context).isPremium.value
        if (isPremium) return true
        return currentCount < FREE_MAX_RECURRING_TRANSACTIONS
    }

    /**
     * Verifica si el usuario puede agregar un presupuesto.
     */
    fun canAddBudget(context: Context, currentCount: Int): Boolean {
        val isPremium = PremiumManager.getInstance(context).isPremium.value
        if (isPremium) return true
        return currentCount < FREE_MAX_BUDGETS
    }

    /**
     * Obtiene el límite de meses de historial para el usuario.
     * @return -1 si tiene acceso ilimitado, o el número de meses
     */
    fun getHistoryMonthsLimit(context: Context): Int {
        val isPremium = PremiumManager.getInstance(context).isPremium.value
        return if (isPremium) PREMIUM_HISTORY_MONTHS else FREE_HISTORY_MONTHS
    }

    /**
     * Obtiene el límite de categorías para el usuario.
     * @return -1 si tiene acceso ilimitado, o el número máximo
     */
    fun getCategoriesLimit(context: Context): Int {
        val isPremium = PremiumManager.getInstance(context).isPremium.value
        return if (isPremium) PREMIUM_MAX_CATEGORIES else FREE_MAX_CATEGORIES
    }

    /**
     * Obtiene el límite de presupuestos para el usuario.
     */
    fun getBudgetsLimit(context: Context): Int {
        val isPremium = PremiumManager.getInstance(context).isPremium.value
        return if (isPremium) PREMIUM_MAX_BUDGETS else FREE_MAX_BUDGETS
    }

    /**
     * Obtiene el límite de transacciones recurrentes.
     */
    fun getRecurringLimit(context: Context): Int {
        val isPremium = PremiumManager.getInstance(context).isPremium.value
        return if (isPremium) PREMIUM_MAX_RECURRING_TRANSACTIONS else FREE_MAX_RECURRING_TRANSACTIONS
    }

    /**
     * Obtiene un mensaje descriptivo del límite actual.
     */
    fun getLimitMessage(context: Context, type: LimitType, currentCount: Int): String {
        val limit = when (type) {
            LimitType.CATEGORY -> getCategoriesLimit(context)
            LimitType.BUDGET -> getBudgetsLimit(context)
            LimitType.RECURRING -> getRecurringLimit(context)
        }
        return if (limit == -1) {
            "Ilimitado"
        } else {
            "$currentCount / $limit"
        }
    }
}

enum class LimitType {
    CATEGORY,
    BUDGET,
    RECURRING
}
