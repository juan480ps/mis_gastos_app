package com.uaa.misgastosapp.ui

import android.app.Activity
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
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
import com.uaa.misgastosapp.BuildConfig
import com.uaa.misgastosapp.data.PremiumLimits
import com.uaa.misgastosapp.data.PremiumManager

/**
 * Pantalla para mostrar los beneficios de Premium y permite la compra.
 * Precio: $0.99 USD (pago único)
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PremiumScreen(
    onBack: () -> Unit,
    onNavigateToLegal: () -> Unit = {},
    premiumManager: PremiumManager = PremiumManager.getInstance(LocalContext.current)
) {
    val isPremium by premiumManager.isPremium.collectAsState()
    val context = LocalContext.current
    var showPurchaseDialog by remember { mutableStateOf(false) }
    var purchaseLoading by remember { mutableStateOf(false) }
    var restoreLoading by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Mis Gastos Premium") },
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
                .verticalScroll(rememberScrollState())
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Header con gradiente. Usa los colores del tema activo (antes eran un morado/indigo
            // fijo) para que se vea coherente con cualquiera de los 9 temas de la app, en vez de
            // desentonar en Dorado/Rosa/Medianoche/Oceano/Bosque/Atardecer.
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        Brush.linearGradient(
                            colors = listOf(
                                MaterialTheme.colorScheme.primary,
                                MaterialTheme.colorScheme.tertiary
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
                        // el dorado del icono de estrella es un acento de marca intencional
                        // (mismo criterio que el icono "Premium" de HomeScreen), no un olvido:
                        // se mantiene constante entre temas a proposito.
                        tint = Color(0xFFFFD700)
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "Premium",
                        fontSize = 28.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Desbloquea todo el potencial",
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.8f)
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Toggle de debug: solo existe en builds de debug (ver PremiumManager.setPremiumForTesting).
            // En release, BuildConfig.DEBUG es una constante fija en `false` y R8 elimina esta
            // seccion entera como codigo muerto, asi que no llega al APK que se publica.
            if (BuildConfig.DEBUG) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                "🛠️ Simular Premium (debug)",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                "Solo visible en builds de debug. No usa Billing real.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onErrorContainer
                            )
                        }
                        Switch(
                            checked = isPremium,
                            onCheckedChange = { premiumManager.setPremiumForTesting(it) }
                        )
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))
            }

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
            PremiumBenefitItem(
                icon = Icons.Default.AccountBalance,
                title = "Cuentas Bancarias Ilimitadas",
                description = "Separa tus gastos por banco sin límite (Free: ${PremiumLimits.FREE_MAX_ACCOUNTS})"
            )

            Spacer(modifier = Modifier.height(16.dp))

            // ========== EXPORTACIÓN ==========
            SectionHeader("📤 Exportación de Datos")

            PremiumBenefitItem(
                icon = Icons.Default.PictureAsPdf,
                title = "Exportar PDF",
                description = "Genera un reporte con todas tus transacciones"
            )
            PremiumBenefitItem(
                icon = Icons.Default.TableChart,
                title = "Exportar Excel/CSV",
                description = "Descarga tus datos para análisis externo"
            )

            Spacer(modifier = Modifier.height(16.dp))

            // ========== PERSONALIZACIÓN Y EXPERIENCIA ==========
            SectionHeader("🎨 Personalización")

            PremiumBenefitItem(
                icon = Icons.Default.Palette,
                title = "5+ Temas Exclusivos",
                description = "Elegí el color de la app tocando el ícono de paleta en Inicio"
            )
            PremiumBenefitItem(
                icon = Icons.Default.Block,
                title = "Sin Publicidad",
                description = "Navegá sin banners en toda la app"
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
                            color = MaterialTheme.colorScheme.onPrimary,
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
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable(onClick = onNavigateToLegal),
                    textAlign = TextAlign.Center,
                    textDecoration = androidx.compose.ui.text.style.TextDecoration.Underline
                )

                Spacer(modifier = Modifier.height(8.dp))

                // permite recuperar la compra en una reinstalacion o un dispositivo nuevo,
                // en vez de depender solo de la restauracion silenciosa al conectar el BillingClient.
                TextButton(
                    onClick = {
                        restoreLoading = true
                        premiumManager.restorePurchases { found ->
                            restoreLoading = false
                            Toast.makeText(
                                context,
                                if (found) "¡Compra restaurada! Ya tenés Premium." else "No se encontró ninguna compra de Premium para restaurar.",
                                Toast.LENGTH_LONG
                            ).show()
                        }
                    },
                    enabled = !restoreLoading,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    if (restoreLoading) {
                        CircularProgressIndicator(modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                    }
                    Text("Restaurar compras")
                }
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
