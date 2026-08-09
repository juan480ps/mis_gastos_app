package com.uaa.misgastosapp.ui

import android.app.Activity
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.uaa.misgastosapp.data.PremiumManager

/**
 * Pantalla para mostrar los beneficios de Premium y permite la compra.
 * Precio: $0.99 USD (pago único)
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PremiumScreen(
    onBack: () -> Unit,
    premiumManager: PremiumManager = PremiumManager.getInstance(LocalContext.current)
) {
    val isPremium by premiumManager.isPremium.collectAsState()
    val context = LocalContext.current
    var showPurchaseDialog by remember { mutableStateOf(false) }
    var purchaseLoading by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Mis Gastos Premium") },
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
                .verticalScroll(rememberScrollState())
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Header con gradiente
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        Brush.linearGradient(
                            colors = listOf(
                                Color(0xFF6C63FF),
                                Color(0xFF3F51B5)
                            )
                        ),
                        shape = RoundedCornerShape(20.dp)
                    )
                    .padding(32.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        imageVector = Icons.Default.Star,
                        contentDescription = null,
                        modifier = Modifier.size(64.dp),
                        tint = Color(0xFFFFD700)
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "Premium",
                        fontSize = 28.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Desbloquea todo el potencial",
                        fontSize = 14.sp,
                        color = Color.White.copy(alpha = 0.8f)
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // ========== LÍMITES EXPANDIDOS ==========
            SectionHeader("📊 Datos Ilimitados")

            PremiumBenefitItem(
                icon = Icons.Default.Category,
                title = "Categorías Ilimitadas",
                description = "Crea todas las categorías que necesites (Free: 10)"
            )
            PremiumBenefitItem(
                icon = Icons.Default.Autorenew,
                title = "Transacciones Recurrentes",
                description = "Sin límite de gastos fijos (Free: 5)"
            )
            PremiumBenefitItem(
                icon = Icons.Default.History,
                title = "Historial Completo",
                description = "Accede a todo tu historial (Free: últimos 3 meses)"
            )
            PremiumBenefitItem(
                icon = Icons.Default.Assessment,
                title = "Presupuestos Ilimitados",
                description = "Controla todos tus presupuestos (Free: 3)"

            )

            Spacer(modifier = Modifier.height(16.dp))

            // ========== EXPORTACIÓN ==========
            SectionHeader("📤 Exportación de Datos")

            PremiumBenefitItem(
                icon = Icons.Default.PictureAsPdf,
                title = "Exportar PDF",
                description = "Genera reportes mensuales y anuales"
            )
            PremiumBenefitItem(
                icon = Icons.Default.TableChart,
                title = "Exportar Excel/CSV",
                description = "Descarga tus datos para análisis externo"
            )
            PremiumBenefitItem(
                icon = Icons.Default.CloudUpload,
                title = "Backup a Google Drive",
                description = "Respaldo automático en la nube"
            )

            Spacer(modifier = Modifier.height(16.dp))

            // ========== GRÁFICOS AVANZADOS ==========
            SectionHeader("📈 Análisis Avanzado")

            PremiumBenefitItem(
                icon = Icons.Default.ShowChart,
                title = "Gráficos Interactivos",
                description = "Zoom, filtros y detalles por categoría"
            )
            PremiumBenefitItem(
                icon = Icons.Default.CompareArrows,
                title = "Comparativa Mes a Mes",
                description = "Compara tus gastos entre períodos"
            )
            PremiumBenefitItem(
                icon = Icons.Default.TrendingUp,
                title = "Predicción de Gastos",
                description = "Estimación basada en tu historial"
            )
            PremiumBenefitItem(
                icon = Icons.Default.Analytics,
                title = "Estadísticas Detalladas",
                description = "Patrones de consumo y tendencias"
            )

            Spacer(modifier = Modifier.height(16.dp))

            // ========== PERSONALIZACIÓN ==========
            SectionHeader("🎨 Personalización")

            PremiumBenefitItem(
                icon = Icons.Default.Palette,
                title = "5+ Temas Exclusivos",
                description = "Oscuro premium, colores personalizados"
            )
            PremiumBenefitItem(
                icon = Icons.Default.Widgets,
                title = "Widgets para Home",
                description = "Resumen rápido en tu pantalla"
            )

            Spacer(modifier = Modifier.height(16.dp))

            // ========== FUNCIONALIDADES EXCLUSIVAS ==========
            SectionHeader("⭐ Exclusivo Premium")

            PremiumBenefitItem(
                icon = Icons.Default.AttachMoney,
                title = "Multi-Divisa",
                description = "USD, EUR, BRL, PYG y más"
            )
            PremiumBenefitItem(
                icon = Icons.Default.Search,
                title = "Búsqueda Avanzada",
                description = "Filtra por fecha, monto y categoría"
            )
            PremiumBenefitItem(
                icon = Icons.Default.Label,
                title = "Etiquetas",
                description = "Organiza transacciones con tags"
            )
            PremiumBenefitItem(
                icon = Icons.Default.Notifications,
                title = "Alertas Inteligentes",
                description = "Avisos de gasto inusual o metas alcanzadas"
            )
            PremiumBenefitItem(
                icon = Icons.Default.Flight,
                title = "Modo Viaje",
                description = "Separa gastos por destino"
            )

            Spacer(modifier = Modifier.height(16.dp))

            // ========== SOPORTE ==========
            SectionHeader("🤝 Soporte")

            PremiumBenefitItem(
                icon = Icons.Default.Support,
                title = "Soporte Prioritario",
                description = "Respuesta en menos de 24 horas"
            )
            PremiumBenefitItem(
                icon = Icons.Default.Update,
                title = "Acceso Anticipado",
                description = "Prueba nuevas funciones primero"
            )

            Spacer(modifier = Modifier.height(24.dp))

            // Precio y botón
            if (isPremium) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer
                    )
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Default.CheckCircle,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                "¡Ya eres Premium!",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                "Disfruta de todos los beneficios",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            } else {
                // Precio
                Text(
                    text = "$0.99 USD",
                    fontSize = 36.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.fillMaxWidth(),
                    textAlign = TextAlign.Center
                )
                Text(
                    text = "Pago único • Para siempre",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.fillMaxWidth(),
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(16.dp))

                Button(
                    onClick = {
                        val activity = context as? Activity
                        if (activity != null) {
                            purchaseLoading = true
                            premiumManager.launchPurchaseFlow(activity) { success ->
                                purchaseLoading = false
                                if (!success) {
                                    showPurchaseDialog = true
                                }
                            }
                        } else {
                            showPurchaseDialog = true
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp),
                    shape = RoundedCornerShape(16.dp),
                    enabled = !purchaseLoading
                ) {
                    if (purchaseLoading) {
                        CircularProgressIndicator(
                            color = Color.White,
                            modifier = Modifier.size(24.dp)
                        )
                    } else {
                        Icon(Icons.Default.ShoppingCart, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Obtener Premium", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = "Al comprar, aceptas los Términos de Servicio",
                    fontSize = 10.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.fillMaxWidth(),
                    textAlign = TextAlign.Center
                )
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }

    // Dialogo de error
    if (showPurchaseDialog) {
        AlertDialog(
            onDismissRequest = { showPurchaseDialog = false },
            title = { Text("No disponible") },
            text = { Text("La compra no está disponible en este momento. Asegúrate de tener conexión a internet y que tu cuenta de Google Play esté configurada.") },
            confirmButton = {
                TextButton(onClick = { showPurchaseDialog = false }) {
                    Text("Entendido")
                }
            }
        )
    }
}

@Composable
private fun SectionHeader(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
        modifier = Modifier.fillMaxWidth()
    )
    Spacer(modifier = Modifier.height(8.dp))
}

@Composable
private fun PremiumBenefitItem(
    icon: ImageVector,
    title: String,
    description: String
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(20.dp)
        )
        Spacer(modifier = Modifier.width(12.dp))
        Column {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold)
            )
            Text(
                text = description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
