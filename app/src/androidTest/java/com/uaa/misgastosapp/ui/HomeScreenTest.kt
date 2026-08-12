package com.uaa.misgastosapp.ui

import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.material3.Text
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.uaa.misgastosapp.Routes
import com.uaa.misgastosapp.ui.theme.GastosTheme
import org.junit.Rule
import org.junit.Test

/**
 * Test E2E de HomeScreen.
 *
 * Esta app no tiene Hilt ni una capa de DI de test (ver GastosApp.kt: DI manual), asi que
 * HomeScreen se renderiza con sus dependencias reales (Room/SQLCipher del dispositivo de test) en
 * vez de dobles de prueba. Por eso el test solo verifica elementos estructurales que no dependen
 * del contenido financiero -- el saludo y que el boton de agregar gasto navega a la pantalla
 * correspondiente -- en vez de aserciones sobre datos concretos, que dependerian del estado de la
 * base de datos del dispositivo/emulador donde corra.
 */
@RequiresApi(Build.VERSION_CODES.O)
class HomeScreenTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun homeScreen_showsGreetingAndAddButtonNavigatesToAddTransaction() {
        composeTestRule.setContent {
            val navController = rememberNavController()
            GastosTheme {
                NavHost(navController = navController, startDestination = Routes.HOME) {
                    composable(Routes.HOME) { HomeScreen(navController = navController) }
                    // stub liviano: alcanza con confirmar que la navegacion llega a esta ruta,
                    // no hace falta renderizar la pantalla real de Agregar Transaccion aca.
                    composable(
                        route = "${Routes.ADD_TRANSACTION}?${Routes.ARG_TRANSACTION_ID}={${Routes.ARG_TRANSACTION_ID}}" +
                            "&${Routes.ARG_PRESELECTED_ACCOUNT_ID}={${Routes.ARG_PRESELECTED_ACCOUNT_ID}}",
                        arguments = listOf(
                            navArgument(Routes.ARG_TRANSACTION_ID) { type = NavType.IntType; defaultValue = -1 },
                            navArgument(Routes.ARG_PRESELECTED_ACCOUNT_ID) { type = NavType.IntType; defaultValue = -1 }
                        )
                    ) {
                        Text("add_transaction_screen_stub")
                    }
                }
            }
        }

        // El saludo muestra "Hola, <nombre>" o "Hola, Invitado" segun haya sesion o no.
        composeTestRule.onNode(hasText("Hola,", substring = true)).assertIsDisplayed()

        // El boton flotante de agregar gasto debe navegar a Agregar Transaccion.
        composeTestRule.onNodeWithContentDescription("Agregar Gasto").performClick()
        composeTestRule.onNodeWithText("add_transaction_screen_stub").assertIsDisplayed()
    }
}
