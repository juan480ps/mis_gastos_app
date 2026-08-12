// AppBottomNavBar

package com.uaa.misgastosapp.ui.components

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Autorenew
import androidx.compose.material.icons.filled.Assessment
import androidx.compose.material.icons.filled.Category
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.PieChart
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Text
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.uaa.misgastosapp.Routes

data class BottomNavItem(
    val label: String,
    val icon: ImageVector,
    val route: String
)

val bottomNavItems = listOf(
    BottomNavItem("Inicio", Icons.Default.Home, Routes.HOME),
    BottomNavItem("Categorías", Icons.Default.Category, Routes.CATEGORIES_LIST),
    BottomNavItem("Presupuestos", Icons.Default.Assessment, Routes.MANAGE_BUDGETS),
    BottomNavItem("Recurrentes", Icons.Default.Autorenew, Routes.MANAGE_RECURRING_TRANSACTIONS),
    BottomNavItem("Gráficos", Icons.Default.PieChart, Routes.CHARTS_SCREEN),
)

/**
 * Barra de navegacion inferior compartida por las 5 pantallas principales (Inicio, Categorías,
 * Presupuestos, Recurrentes, Gráficos). Antes solo existia dentro de HomeScreen, asi que
 * desaparecia al entrar a cualquier otra seccion; ahora cada una de esas pantallas la incluye
 * igual, resaltando la pestaña que corresponde segun 'currentRoute'.
 */
@Composable
fun AppBottomNavBar(navController: NavController, currentRoute: String) {
    Box(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
        NavigationBar(
            modifier = Modifier.clip(RoundedCornerShape(24.dp)),
            containerColor = MaterialTheme.colorScheme.primary
        ) {
            bottomNavItems.forEach { item ->
                val isSelected = item.route == currentRoute
                NavigationBarItem(
                    selected = isSelected,
                    onClick = {
                        if (!isSelected) {
                            // popUpTo+launchSingleTop+restoreState es el patron estandar para
                            // tabs: evita apilar pantallas al saltar entre secciones y restaura
                            // el estado (scroll, meses colapsados, etc.) de la pestaña al volver.
                            navController.navigate(item.route) {
                                popUpTo(Routes.HOME) { saveState = true }
                                launchSingleTop = true
                                restoreState = true
                            }
                        }
                    },
                    icon = { Icon(item.icon, contentDescription = item.label) },
                    label = {
                        // en pantallas angostas "Presupuestos"/"Recurrentes" no entraban en una
                        // linea y el texto se cortaba en dos, rompiendo la forma de la barra; con
                        // una linea fija y elipsis nunca se desborda.
                        Text(
                            item.label,
                            style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            softWrap = false
                        )
                    },
                    alwaysShowLabel = true,
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = Color.White,
                        selectedTextColor = Color.White,
                        unselectedIconColor = Color.White.copy(alpha = 0.7f),
                        unselectedTextColor = Color.White.copy(alpha = 0.7f),
                        indicatorColor = Color.White.copy(alpha = 0.2f)
                    )
                )
            }
        }
    }
}
