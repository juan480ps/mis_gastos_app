package com.uaa.misgastosapp.ui

import android.content.Context
import android.content.Intent
import android.os.Build
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.FileProvider
import com.uaa.misgastosapp.data.PremiumManager
import java.io.File
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter

/**
 * Pantalla de exportación de datos (solo Premium).
 * Permite exportar a PDF y CSV.
 */
@RequiresApi(Build.VERSION_CODES.O)
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExportScreen(
    onBack: () -> Unit,
    premiumManager: PremiumManager = PremiumManager.getInstance(LocalContext.current)
) {
    val isPremium by premiumManager.isPremium.collectAsState()
    val context = LocalContext.current
    var showPremiumDialog by remember { mutableStateOf(false) }
    var exportLoading by remember { mutableStateOf(false) }
    var exportSuccess by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Exportar Datos") },
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
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            if (!isPremium) {
                // Mensaje de Premium requerido
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.errorContainer
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            Icons.Default.Lock,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.error,
                            modifier = Modifier.size(48.dp)
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            "Función Premium",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            "Actualiza a Premium para exportar tus datos",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Button(
                            onClick = { showPremiumDialog = true },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.error
                            )
                        ) {
                            Icon(Icons.Default.Star, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Ver Premium")
                        }
                    }
                }
            } else {
                // Opciones de exportación
                Text(
                    text = "Elige el formato de exportación",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(24.dp))

                // Export PDF
                ExportOptionCard(
                    icon = Icons.Default.PictureAsPdf,
                    title = "Exportar PDF",
                    description = "Genera un reporte profesional con gráficos y tablas",
                    color = Color(0xFFE53935),
                    onClick = {
                        exportLoading = true
                        exportPdf(context) { success ->
                            exportLoading = false
                            exportSuccess = success
                        }
                    },
                    enabled = !exportLoading
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Export CSV
                ExportOptionCard(
                    icon = Icons.Default.TableChart,
                    title = "Exportar CSV/Excel",
                    description = "Descarga tus datos para abrir en Excel o Google Sheets",
                    color = Color(0xFF43A047),
                    onClick = {
                        exportLoading = true
                        exportCsv(context) { success ->
                            exportLoading = false
                            exportSuccess = success
                        }
                    },
                    enabled = !exportLoading
                )

                Spacer(modifier = Modifier.height(24.dp))

                if (exportLoading) {
                    CircularProgressIndicator()
                }

                if (exportSuccess) {
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.primaryContainer
                        )
                    ) {
                        Text(
                            "✅ Archivo exportado exitosamente",
                            modifier = Modifier.padding(16.dp),
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                }
            }
        }
    }

    // Dialogo Premium
    if (showPremiumDialog) {
        AlertDialog(
            onDismissRequest = { showPremiumDialog = false },
            title = { Text("Premium Requerido") },
            text = { Text("La exportación de datos es una función Premium. Obtén Premium por solo $0.99 USD y desbloquea todas las funciones.") },
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
private fun ExportOptionCard(
    icon: ImageVector,
    title: String,
    description: String,
    color: Color,
    onClick: () -> Unit,
    enabled: Boolean
) {
    Card(
        onClick = { if (enabled) onClick() },
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                icon,
                contentDescription = null,
                tint = color,
                modifier = Modifier.size(40.dp)
            )
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Icon(
                Icons.Default.ChevronRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@RequiresApi(Build.VERSION_CODES.O)
private fun exportPdf(context: Context, onResult: (Boolean) -> Unit) {
    try {
        // TODO: Implementar generación de PDF real con Android PDF generation
        // Por ahora creamos un archivo de texto simulado
        val fileName = "MisGastos_Reporte_${LocalDate.now()}.txt"
        val file = File(context.cacheDir, fileName)
        file.writeText("Reporte de Mis Gastos\nFecha: ${LocalDate.now()}\n\n[Próximamente: Reporte PDF completo con gráficos]")

        val uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            file
        )

        val intent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(uri, "text/plain")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(Intent.createChooser(intent, "Abrir reporte"))
        onResult(true)
    } catch (e: Exception) {
        Toast.makeText(context, "Error al exportar: ${e.message}", Toast.LENGTH_LONG).show()
        onResult(false)
    }
}

@RequiresApi(Build.VERSION_CODES.O)
private fun exportCsv(context: Context, onResult: (Boolean) -> Unit) {
    try {
        val fileName = "MisGastos_Datos_${LocalDate.now()}.csv"
        val file = File(context.cacheDir, fileName)
        file.writeText("Fecha,Título,Categoría,Monto\n[Próximamente: Datos exportados desde la base de datos]")

        val uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            file
        )

        val intent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(uri, "text/csv")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(Intent.createChooser(intent, "Abrir datos"))
        onResult(true)
    } catch (e: Exception) {
        Toast.makeText(context, "Error al exportar: ${e.message}", Toast.LENGTH_LONG).show()
        onResult(false)
    }
}
