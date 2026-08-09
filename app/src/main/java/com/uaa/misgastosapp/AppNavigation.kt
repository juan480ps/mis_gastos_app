package com.uaa.misgastosapp

import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.uaa.misgastosapp.ui.*
import com.uaa.misgastosapp.ui.isOnboardingCompleted

/**
 * Rutas de navegación de la aplicación.
 */
object Routes {
    const val ONBOARDING = "onboarding"
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
    const val PREMIUM = "premium"
    const val EXPORT = "export"
    const val THEMES = "themes"
}

/**
 * Navegación principal de la aplicación.
 * Muestra onboarding la primera vez, luego va al Home.
 */
@RequiresApi(Build.VERSION_CODES.O)
@Composable
fun AppNavigation(navController: NavHostController) {
    val context = LocalContext.current
    
    // Verificar si es la primera vez que el usuario abre la app
    var startDestination by remember { mutableStateOf<String?>(null) }
    
    // Determinar destino inicial basado en onboarding
    LaunchedEffect(Unit) {
        startDestination = if (isOnboardingCompleted(context)) {
            Routes.HOME
        } else {
            Routes.ONBOARDING
        }
    }

    // Mostrar loading mientras se determina el destino
    if (startDestination == null) {
        return
    }

    NavHost(navController = navController, startDestination = startDestination!!) {
        // Onboarding (solo la primera vez)
        composable(Routes.ONBOARDING) {
            OnboardingScreen(
                onFinish = {
                    navController.navigate(Routes.HOME) {
                        popUpTo(Routes.ONBOARDING) { inclusive = true }
                    }
                }
            )
        }

        // Login y Registro (opcionales)
        composable(Routes.LOGIN) { LoginScreen(navController) }
        composable(Routes.REGISTER) { RegisterScreen(navController) }

        // Pantalla principal
        composable(Routes.HOME) { HomeScreen(navController) }
        composable(Routes.ADD_TRANSACTION) { AddTransactionScreen(navController) }
        composable(Routes.ADD_CATEGORY) { AddCategoryScreen(navController) }
        composable(Routes.CATEGORIES_LIST) { CategoriesListScreen(navController) }
        composable(Routes.MANAGE_BUDGETS) { ManageBudgetsScreen(navController) }
        composable(Routes.MANAGE_RECURRING_TRANSACTIONS) { ManageRecurringTransactionsScreen(navController) }

        // Agregar/Editar transacción recurrente
        composable(
            route = "${Routes.ADD_EDIT_RECURRING_TRANSACTION}/{${Routes.ARG_RECURRING_TRANSACTION_ID}}",
            arguments = listOf(navArgument(Routes.ARG_RECURRING_TRANSACTION_ID) {
                type = NavType.IntType
                defaultValue = -1
            })
        ) { backStackEntry ->
            val transactionId = backStackEntry.arguments?.getInt(Routes.ARG_RECURRING_TRANSACTION_ID)
            AddEditRecurringTransactionScreen(
                navController = navController,
                recurringTransactionId = if (transactionId == -1) null else transactionId
            )
        }

        // Nueva transacción recurrente
        composable(Routes.ADD_EDIT_RECURRING_TRANSACTION) {
            AddEditRecurringTransactionScreen(navController = navController, recurringTransactionId = null)
        }

        // Gráficos
        composable(Routes.CHARTS_SCREEN) { ChartsScreen(navController) }

        // Premium
        composable(Routes.PREMIUM) {
            PremiumScreen(
                onBack = { navController.popBackStack() }
            )
        }

        // Exportar datos (Premium)
        composable(Routes.EXPORT) {
            ExportScreen(
                onBack = { navController.popBackStack() }
            )
        }

        // Temas exclusivos
        composable(Routes.THEMES) {
            ThemesScreen(
                onBack = { navController.popBackStack() }
            )
        }
    }
}
