package com.uaa.misgastosapp.domain.monitoring

import android.os.SystemClock
import android.util.Log
import android.content.Context

/**
 * Monitor de performance para rastrear métricas de la app.
 */
class PerformanceMonitor(private val context: Context) {

    companion object {
        private const val TAG = "PerformanceMonitor"
        private const val PREFS_NAME = "performance_metrics"
    }

    private val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    private val timers = mutableMapOf<String, Long>()

    fun startTimer(operationName: String) {
        timers[operationName] = SystemClock.elapsedRealtime()
    }

    fun stopTimer(operationName: String): Long {
        val startTime = timers.remove(operationName) ?: return 0
        val duration = SystemClock.elapsedRealtime() - startTime
        saveMetric(operationName, duration)
        Log.d(TAG, "$operationName completado en ${duration}ms")
        return duration
    }

    fun trackMetric(name: String, value: Long, unit: String = "ms") {
        saveMetric(name, value)
        Log.d(TAG, "$name: $value$unit")
    }

    fun trackEvent(eventName: String, properties: Map<String, Any> = emptyMap()) {
        val propertiesStr = properties.entries.joinToString(", ") { "${it.key}=${it.value}" }
        Log.d(TAG, "Event: $eventName [$propertiesStr]")
        val count = prefs.getInt("event_$eventName", 0)
        prefs.edit().putInt("event_$eventName", count + 1).apply()
    }

    fun getAverageTime(operationName: String): Long {
        val total = prefs.getLong("${operationName}_total", 0)
        val count = prefs.getInt("${operationName}_count", 0)
        return if (count > 0) total / count else 0
    }

    fun getMetricsSummary(): Map<String, Any> {
        val allKeys = prefs.all.keys
        val metrics = mutableMapOf<String, Any>()
        
        allKeys.forEach { key ->
            when {
                key.endsWith("_total") -> {
                    val operationName = key.removeSuffix("_total")
                    metrics[operationName] = mapOf(
                        "total_ms" to prefs.getLong(key, 0),
                        "count" to prefs.getInt("${operationName}_count", 0),
                        "avg_ms" to getAverageTime(operationName)
                    )
                }
                key.startsWith("event_") -> {
                    val eventName = key.removePrefix("event_")
                    metrics["event_$eventName"] = prefs.getInt(key, 0)
                }
            }
        }
        return metrics
    }

    fun clearMetrics() {
        prefs.edit().clear().apply()
    }

    private fun saveMetric(name: String, value: Long) {
        val totalKey = "${name}_total"
        val countKey = "${name}_count"
        val total = prefs.getLong(totalKey, 0)
        val count = prefs.getInt(countKey, 0)
        prefs.edit()
            .putLong(totalKey, total + value)
            .putInt(countKey, count + 1)
            .apply()
    }
}
