package com.uaa.misgastosapp

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.annotation.RequiresApi
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import androidx.navigation.compose.rememberNavController
import com.uaa.misgastosapp.network.NetworkModule
import com.uaa.misgastosapp.utils.SecureSessionManager
import com.uaa.misgastosapp.ui.theme.GastosTheme
import com.uaa.misgastosapp.ui.viewmodel.RecurringTransactionViewModel
import kotlinx.coroutines.launch
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen

/**
 * Actividad principal de la aplicación.
 * Usa DI manual en lugar de Hilt para compatibilidad con AGP 9.0.
 */
class MainActivity : ComponentActivity() {

    private val recurringTransactionViewModel: RecurringTransactionViewModel by viewModels()

    // en Android 13+ el permiso de notificaciones se pide en tiempo de ejecucion; sin esto,
    // las notificaciones de transacciones recurrentes procesadas nunca se muestran.
    private val requestNotificationPermission = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { /* si el usuario lo niega, seguimos funcionando igual, solo sin notificaciones. */ }

    @RequiresApi(Build.VERSION_CODES.O)
    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) {
            requestNotificationPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
        }

        val sessionManager = SecureSessionManager(this)
        NetworkModule.initialize(sessionManager)

        // Procesar transacciones recurrentes al abrir la app
        lifecycleScope.launch {
            try {
                recurringTransactionViewModel.processDueRecurringTransactions()
            } catch (e: Exception) {
                Log.e("MainActivity", "Error processing recurring transactions: ${e.message}")
            }
        }

        setContent {
            GastosTheme {
                val navController = rememberNavController()
                AppNavigation(navController = navController)
            }
        }
    }
}
