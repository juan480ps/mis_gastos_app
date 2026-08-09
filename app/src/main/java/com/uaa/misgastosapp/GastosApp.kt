package com.uaa.misgastosapp

import android.app.Application
import android.os.Build
import android.util.Log
import androidx.annotation.RequiresApi
import com.google.android.gms.ads.MobileAds
import com.uaa.misgastosapp.data.PremiumManager
import com.uaa.misgastosapp.worker.RecurringTransactionWorker

/**
 * Application class para inicializar componentes globales.
 * Usa DI manual en lugar de Hilt para compatibilidad con AGP 9.0.
 */
class GastosApp : Application() {

    companion object {
        private const val TAG = "GastosApp"
        
        // Singleton para acceso global a la base de datos
        lateinit var instance: GastosApp
            private set
    }

    @RequiresApi(Build.VERSION_CODES.O)
    override fun onCreate() {
        super.onCreate()
        instance = this

        // Inicializar AdMob
        MobileAds.initialize(this) { initializationStatus ->
            Log.d(TAG, "AdMob inicializado: ${initializationStatus.adapterStatusMap.size} adaptadores")
        }

        // Inicializar PremiumManager (BillingClient)
        PremiumManager.getInstance(this).startConnection()

        // Programar WorkManager para transacciones recurrentes
        try {
            RecurringTransactionWorker.schedulePeriodicWork(this)
            Log.d(TAG, "WorkManager inicializado correctamente")
        } catch (e: Exception) {
            Log.e(TAG, "Error inicializando WorkManager", e)
        }
    }
}
