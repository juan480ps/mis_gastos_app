// mainactivity

package com.uaa.misgastosapp

import android.os.Build
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.annotation.RequiresApi
import androidx.lifecycle.lifecycleScope
import androidx.navigation.compose.rememberNavController
import com.uaa.misgastosapp.network.NetworkModule
import com.uaa.misgastosapp.utils.SecureSessionManager
import com.uaa.misgastosapp.ui.theme.GastosTheme
import com.uaa.misgastosapp.ui.viewmodel.RecurringTransactionViewModel
import kotlinx.coroutines.launch
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen


// esta es la clase de la actividad principal, la primera pantalla que se abre.
class MainActivity : ComponentActivity() {
    // aca se crea una variable para el manejo de los gastos que se repiten.
    // 'by viewmodels()' se usa para que los datos no se pierdan si se gira el telefono.
    private val recurringTransactionViewModel: RecurringTransactionViewModel by viewModels()

    // con esta linea se asegura que el codigo solo se ejecute en versiones de android
    // que lo soporten (en este caso, android oreo o superior).
    @RequiresApi(Build.VERSION_CODES.O)
    // esta funcion se ejecuta cuando la actividad se crea por primera vez.
    override fun onCreate(savedInstanceState: Bundle?) {

        // se instala una pantalla de carga (splash screen) que se muestra
        // mientras la aplicacion se inicia.
        installSplashScreen()

        // se llama al metodo oncreate de la clase padre para que se realicen sus tareas iniciales.
        super.onCreate(savedInstanceState)

        // se crea una instancia para el manejo seguro de la sesion del usuario.
        val sessionManager = SecureSessionManager(this)
        // se inicializa el modulo de red, que se encarga de las conexiones a internet.
        NetworkModule.initialize(sessionManager)

        // se inicia una tarea en segundo plano para no bloquear la pantalla.
        lifecycleScope.launch {
            try {
                // se revisan y procesan los gastos recurrentes que esten vencidos.
                recurringTransactionViewModel.processDueRecurringTransactions()
            } catch (e: Exception) {
                // si ocurre un error, este se muestra en la consola de depuracion.
                Log.e("MainActivity", "Error processing recurring transactions: ${e.message}")
            }
        }

        // aca se define la interfaz de usuario de la actividad.
        setContent {
            // se aplica el tema visual de la aplicacion (colores, fuentes, etc.).
            GastosTheme {
                // se crea un controlador de navegacion para el movimiento entre pantallas.
                val navController = rememberNavController()
                // se configura la navegacion de la aplicacion.
                AppNavigation(navController = navController)
            }
        }
    }
}