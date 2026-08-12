package com.uaa.misgastosapp.ui

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
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.uaa.misgastosapp.data.PremiumManager
import com.uaa.misgastosapp.ui.theme.AppTheme
import com.uaa.misgastosapp.ui.theme.AppThemes
import com.uaa.misgastosapp.ui.theme.ThemePrefs
import com.uaa.misgastosapp.ui.theme.onColorFor

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
    // el estado viene de ThemePrefs (compartido con GastosTheme), asi que elegir un tema aca
    // cambia de inmediato el color de toda la app, no solo el resaltado de esta pantalla.
    val selectedThemeState = ThemePrefs.currentThemeId(context)
    val selectedTheme = selectedThemeState.value ?: "default"
    var showPremiumDialog by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Temas") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Volver")
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
                        onClick = { ThemePrefs.selectTheme(context, theme.id) }
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
                                ThemePrefs.selectTheme(context, theme.id)
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
        // texto/icono legible segun el color propio de ESTE swatch, no del tema activo: temas
        // claros como Dorado o Rosa necesitan texto oscuro, no blanco fijo (antes era ilegible
        // sobre esos dos swatches especificos).
        val onSwatchColor = onColorFor(theme.primaryColor)
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
                    tint = onSwatchColor,
                    modifier = Modifier.size(32.dp)
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    theme.name,
                    fontSize = 10.sp,
                    color = onSwatchColor,
                    textAlign = TextAlign.Center
                )
                if (isLocked) {
                    Icon(
                        Icons.Default.Lock,
                        contentDescription = "Premium",
                        tint = onSwatchColor.copy(alpha = 0.7f),
                        modifier = Modifier.size(12.dp)
                    )
                }
            }
        }
    }
}
