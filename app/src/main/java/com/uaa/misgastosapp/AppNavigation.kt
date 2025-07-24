// AppNavigation

package com.uaa.misgastosapp

import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.uaa.misgastosapp.ui.*
import com.uaa.misgastosapp.ui.viewmodel.AuthViewModel

// se crea un objeto para guardar las "direcciones" de todas las pantallas.
// se hace de esta forma para evitar errores de escritura en el codigo.
object Routes {
    const val LOGIN = "login"
    const val REGISTER = "register"
    const val HOME = "home"
    const val ADD_TRANSACTION = "add_transaction"
    const val ADD_CATEGORY = "add_category"
    const val CATEGORIES_LIST = "categories_list"
    const val MANAGE_BUDGETS = "manage_budgets"
    const val MANAGE_RECURRING_TRANSACTIONS = "manage_recurring_transactions"
    const val ADD_EDIT_RECURRING_TRANSACTION = "add_edit_recurring_transaction"
    const val ARG_RECURRING_TRANSACTION_ID = "recurringTransactionId"
    const val CHARTS_SCREEN = "charts_screen"
}

// se asegura que este codigo solo se ejecute en versiones de android compatibles.
@RequiresApi(Build.VERSION_CODES.O)
// esta es la funcion principal que se encarga de organizar la navegacion en la aplicacion.
@Composable
fun AppNavigation(navController: NavHostController) {
    // se obtiene el viewmodel de autenticacion, que sabe si el usuario inicio sesion.
    val authViewModel: AuthViewModel = viewModel()
    // se crea una variable que observa si el usuario esta logueado o no. se actualiza sola.
    val isLoggedIn by authViewModel.isLoggedIn.collectAsState()

    // se decide cual sera la primera pantalla. si el usuario ya inicio sesion, es 'home', si no, 'login'.
    val startDestination = if (isLoggedIn) Routes.HOME else Routes.LOGIN

    // este bloque de codigo se ejecuta solo cuando el valor de 'isloggedin' cambia.
    LaunchedEffect(isLoggedIn) {
        // se comprueba si el usuario cerro sesion y no esta en una pantalla publica (login o registro).
        if (!isLoggedIn && navController.currentDestination?.route != Routes.LOGIN &&
            navController.currentDestination?.route != Routes.REGISTER) {
            // si la condicion se cumple, se navega a la pantalla de login.
            navController.navigate(Routes.LOGIN) {
                // se borra el historial de pantallas para que no se pueda volver atras.
                popUpTo(0) { inclusive = true }
            }
        }
    }

    // aca se configura el mapa de navegacion. se indica el controlador y la pantalla de inicio.
    NavHost(navController = navController, startDestination = startDestination) {
        // se define que cuando se navegue a la ruta 'login', se muestre la pantalla 'loginscreen'.
        composable(Routes.LOGIN) { LoginScreen(navController) }
        composable(Routes.REGISTER) { RegisterScreen(navController) }

        // aca se definen las rutas para las pantallas que necesitan que el usuario este logueado.
        composable(Routes.HOME) { HomeScreen(navController) }
        composable(Routes.ADD_TRANSACTION) { AddTransactionScreen(navController) }
        composable(Routes.ADD_CATEGORY) { AddCategoryScreen(navController) }
        composable(Routes.CATEGORIES_LIST) { CategoriesListScreen(navController) }
        composable(Routes.MANAGE_BUDGETS) { ManageBudgetsScreen(navController) }
        composable(Routes.MANAGE_RECURRING_TRANSACTIONS) { ManageRecurringTransactionsScreen(navController) }

        // esta es una ruta especial para agregar o editar transacciones recurrentes.
        // puede recibir un numero (id de la transaccion) al final de la direccion.
        composable(
            route = "${Routes.ADD_EDIT_RECURRING_TRANSACTION}/{${Routes.ARG_RECURRING_TRANSACTION_ID}}",
            // aca se define que el id es un numero y que si no se envia, su valor por defecto es -1.
            arguments = listOf(navArgument(Routes.ARG_RECURRING_TRANSACTION_ID) {
                type = NavType.IntType
                defaultValue = -1
            })
        ) { backStackEntry ->
            // se obtiene el id que se paso en la ruta.
            val transactionId = backStackEntry.arguments?.getInt(Routes.ARG_RECURRING_TRANSACTION_ID)
            // se muestra la pantalla de agregar/editar, pasandole el id si es que existe.
            AddEditRecurringTransactionScreen(
                navController = navController,
                recurringTransactionId = if (transactionId == -1) null else transactionId
            )
        }

        // esta es la misma pantalla pero para cuando se quiere agregar una nueva transaccion (sin pasarle un id).
        composable(Routes.ADD_EDIT_RECURRING_TRANSACTION) {
            AddEditRecurringTransactionScreen(navController = navController, recurringTransactionId = null)
        }
        // aca se define la ruta para la pantalla de graficos.
        composable(Routes.CHARTS_SCREEN) { ChartsScreen(navController) }
    }
}