package com.uaa.misgastosapp.worker

import android.content.Context
import android.os.Build
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.work.*
import com.uaa.misgastosapp.data.AppDatabase
import com.uaa.misgastosapp.data.repository.RecurringTransactionRepository
import java.time.Duration
import java.util.concurrent.TimeUnit

/**
 * Worker que procesa transacciones recurrentes vencidas en background.
 * Se ejecuta periodicamente sin necesidad de abrir la app.
 */
class RecurringTransactionWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    companion object {
        private const val TAG = "RecurringWorker"
        private const val WORK_NAME = "recurring_transactions_periodic"

        /**
         * Programa el trabajo periodico para procesar transacciones recurrentes.
         * Se ejecuta cada 6 horas con restriccion de red.
         */
        @RequiresApi(Build.VERSION_CODES.O)
        fun schedulePeriodicWork(context: Context) {
            val constraints = Constraints.Builder()
                .setRequiredNetworkType(NetworkType.NOT_REQUIRED) // Funciona offline
                .setRequiresBatteryNotLow(true) // No drena batería baja
                .build()

            val workRequest = PeriodicWorkRequestBuilder<RecurringTransactionWorker>(
                repeatInterval = 6,
                repeatIntervalTimeUnit = TimeUnit.HOURS
            )
                .setConstraints(constraints)
                .setBackoffCriteria(
                    BackoffPolicy.EXPONENTIAL,
                    Duration.ofMinutes(5)
                )
                .addTag("recurring_transactions")
                .build()

            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                WORK_NAME,
                ExistingPeriodicWorkPolicy.KEEP, // No duplicar si ya existe
                workRequest
            )

            Log.d(TAG, "WorkManager programado: cada 6 horas")
        }

        /**
         * Cancela el trabajo periodico.
         */
        fun cancelPeriodicWork(context: Context) {
            WorkManager.getInstance(context).cancelUniqueWork(WORK_NAME)
            Log.d(TAG, "WorkManager cancelado")
        }
    }

    @RequiresApi(Build.VERSION_CODES.O)
    override suspend fun doWork(): Result {
        return try {
            Log.d(TAG, "Iniciando procesamiento de transacciones recurrentes...")

            val database = AppDatabase.getInstance(applicationContext)
            val repository = RecurringTransactionRepository(
                recurringDao = database.recurringTransactionDao(),
                transactionDao = database.transactionDao(),
                categoryDao = database.categoryDao()
            )

            val processed = repository.processDueRecurringTransactions()
            if (processed.isNotEmpty()) {
                // asi el usuario se entera de que paso algo, en vez de que el balance cambie en
                // silencio mientras la app estaba cerrada.
                RecurringNotificationHelper.notifyProcessed(applicationContext, processed)
            }

            Log.d(TAG, "Transacciones recurrentes procesadas exitosamente (${processed.size})")
            Result.success()
        } catch (e: Exception) {
            Log.e(TAG, "Error procesando transacciones recurrentes", e)
            // Reintentar con backoff exponencial
            Result.retry()
        }
    }
}
