package com.uaa.misgastosapp.ui.theme

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.Eco
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Park
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Water
import androidx.compose.material.icons.filled.Waves
import androidx.compose.material.icons.filled.WbSunny
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector

/**
 * Tema de color de la app.
 */
data class AppTheme(
    val id: String,
    val name: String,
    val icon: ImageVector,
    val primaryColor: Color,
    val isPremium: Boolean
)

/**
 * Temas disponibles en la app. GastosTheme (Theme.kt) aplica el color de este tema como
 * primary del ColorScheme cuando el usuario elige uno explícitamente en ThemesScreen.
 */
object AppThemes {
    val themes = listOf(
        // Temas gratuitos
        AppTheme("default", "Morado Clásico", Icons.Default.Palette, Color(0xFF6C63FF), isPremium = false),
        AppTheme("blue", "Azul Profesional", Icons.Default.Water, Color(0xFF1976D2), isPremium = false),
        AppTheme("green", "Verde Fresco", Icons.Default.Eco, Color(0xFF388E3C), isPremium = false),

        // Temas Premium
        AppTheme("gold", "Dorado Premium", Icons.Default.Star, Color(0xFFFFB300), isPremium = true),
        AppTheme("rose", "Rosa Elegante", Icons.Default.Favorite, Color(0xFFE91E63), isPremium = true),
        AppTheme("midnight", "Medianoche", Icons.Default.DarkMode, Color(0xFF37474F), isPremium = true),
        AppTheme("ocean", "Océano Profundo", Icons.Default.Waves, Color(0xFF0277BD), isPremium = true),
        AppTheme("forest", "Bosque Mágico", Icons.Default.Park, Color(0xFF2E7D32), isPremium = true),
        AppTheme("sunset", "Atardecer", Icons.Default.WbSunny, Color(0xFFFF6F00), isPremium = true)
    )
}
