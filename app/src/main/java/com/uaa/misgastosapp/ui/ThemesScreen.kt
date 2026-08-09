package com.uaa.misgastosapp.ui

import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.uaa.misgastosapp.data.PremiumManager

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
 * Temas disponibles en la app.
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

/**
 * Pantalla de selección de temas.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ThemesScreen(
    onBack: () -> Unit,
    premiumManager: PremiumManager = PremiumManager.getInstance(LocalContext.current)
) {
    val isPremium by premiumManager.isPremium.collectAsState()
    val context = LocalContext.current
    var selectedTheme by remember { mutableStateOf(getSavedTheme(context)) }
    var showPremiumDialog by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Temas") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Volver")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp)
        ) {
            // Temas gratuitos
            Text(
                "Temas Gratuitos",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(8.dp))

            LazyVerticalGrid(
                columns = GridCells.Fixed(3),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(AppThemes.themes.filter { !it.isPremium }) { theme ->
                    ThemeCard(
                        theme = theme,
                        isSelected = selectedTheme == theme.id,
                        onClick = {
                            selectedTheme = theme.id
                            saveTheme(context, theme.id)
                        }
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Temas Premium
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "Temas Premium",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.weight(1f)
                )
                if (!isPremium) {
                    Icon(
                        Icons.Default.Lock,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
            Spacer(modifier = Modifier.height(8.dp))

            LazyVerticalGrid(
                columns = GridCells.Fixed(3),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(AppThemes.themes.filter { it.isPremium }) { theme ->
                    ThemeCard(
                        theme = theme,
                        isSelected = selectedTheme == theme.id,
                        isLocked = !isPremium,
                        onClick = {
                            if (isPremium) {
                                selectedTheme = theme.id
                                saveTheme(context, theme.id)
                            } else {
                                showPremiumDialog = true
                            }
                        }
                    )
                }
            }
        }
    }

    // Dialogo Premium
    if (showPremiumDialog) {
        AlertDialog(
            onDismissRequest = { showPremiumDialog = false },
            title = { Text("Premium Requerido") },
            text = { Text("Los temas exclusivos son para usuarios Premium. Obtén Premium por solo $0.99 USD.") },
            confirmButton = {
                TextButton(onClick = { showPremiumDialog = false }) {
                    Text("Ver Premium")
                }
            },
            dismissButton = {
                TextButton(onClick = { showPremiumDialog = false }) {
                    Text("Cancelar")
                }
            }
        )
    }
}

@Composable
private fun ThemeCard(
    theme: AppTheme,
    isSelected: Boolean,
    isLocked: Boolean = false,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(1f)
            .then(
                if (isSelected) {
                    Modifier.border(3.dp, MaterialTheme.colorScheme.primary, RoundedCornerShape(12.dp))
                } else {
                    Modifier
                }
            ),
        colors = CardDefaults.cardColors(
            containerColor = theme.primaryColor.copy(alpha = if (isLocked) 0.3f else 0.8f)
        ),
        onClick = onClick
    ) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Icon(
                    theme.icon,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(32.dp)
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    theme.name,
                    fontSize = 10.sp,
                    color = Color.White,
                    textAlign = TextAlign.Center
                )
                if (isLocked) {
                    Icon(
                        Icons.Default.Lock,
                        contentDescription = "Premium",
                        tint = Color.White.copy(alpha = 0.7f),
                        modifier = Modifier.size(12.dp)
                    )
                }
            }
        }
    }
}

private const val THEME_PREFS = "theme_prefs"
private const val KEY_THEME = "selected_theme"

private fun getSavedTheme(context: Context): String {
    return context.getSharedPreferences(THEME_PREFS, Context.MODE_PRIVATE)
        .getString(KEY_THEME, "default") ?: "default"
}

private fun saveTheme(context: Context, themeId: String) {
    context.getSharedPreferences(THEME_PREFS, Context.MODE_PRIVATE)
        .edit()
        .putString(KEY_THEME, themeId)
        .apply()
}
