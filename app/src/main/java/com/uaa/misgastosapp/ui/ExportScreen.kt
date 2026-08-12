package com.uaa.misgastosapp.ui

import android.app.Application
import android.content.Context
import android.content.Intent
import android.graphics.Paint
import android.graphics.pdf.PdfDocument
import android.os.Build
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
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
import com.uaa.misgastosapp.data.repository.AppRepositories
import com.uaa.misgastosapp.model.Transaction
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
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
    val coroutineScope = rememberCoroutineScope()
    var showPremiumDialog by remember { mutableStateOf(false) }
    var exportLoading by remember { mutableStateOf(false) }
    var exportSuccess by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Exportar Datos") },
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
                    description = "Genera un reporte con el listado de tus transacciones",
                    color = Color(0xFFE53935),
                    onClick = {
                        exportLoading = true
                        exportSuccess = false
                        coroutineScope.launch {
                            exportSuccess = exportPdf(context)
                            exportLoading = false
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
                        exportSuccess = false
                        coroutineScope.launch {
                            exportSuccess = exportCsv(context)
                            exportLoading = false
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

// obtiene las transacciones reales desde el repositorio (no simuladas), para los dos formatos de exportacion.
@RequiresApi(Build.VERSION_CODES.O)
private suspend fun loadTransactionsForExport(context: Context): List<Transaction> {
    val application = context.applicationContext as Application
    return AppRepositories.transactionRepository(application).allTransactions.first()
}

private fun shareExportedFile(context: Context, file: File, mimeType: String, chooserTitle: String) {
    val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
    val intent = Intent(Intent.ACTION_VIEW).apply {
        setDataAndType(uri, mimeType)
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    }
    context.startActivity(Intent.createChooser(intent, chooserTitle))
}

@RequiresApi(Build.VERSION_CODES.O)
private suspend fun exportPdf(context: Context): Boolean {
    return try {
        val transactions = loadTransactionsForExport(context)
        val file = withContext(Dispatchers.IO) {
            val pageWidth = 595
            val pageHeight = 842
            val margin = 40f
            val lineHeight = 18f

            val titlePaint = Paint().apply { textSize = 16f; isFakeBoldText = true }
            val textPaint = Paint().apply { textSize = 11f }

            val pdfDocument = PdfDocument()
            var pageNumber = 1
            var page = pdfDocument.startPage(PdfDocument.PageInfo.Builder(pageWidth, pageHeight, pageNumber).create())
            var canvas = page.canvas
            var y = margin

            canvas.drawText("Reporte de Mis Gastos", margin, y, titlePaint)
            y += lineHeight * 1.5f
            canvas.drawText("Generado: ${LocalDate.now()}", margin, y, textPaint)
            y += lineHeight
            val total = transactions.sumOf { it.amount }
            canvas.drawText("Balance total: ${"%,.0f".format(total)}", margin, y, textPaint)
            y += lineHeight * 2

            for (t in transactions) {
                if (y > pageHeight - margin) {
                    pdfDocument.finishPage(page)
                    pageNumber++
                    page = pdfDocument.startPage(PdfDocument.PageInfo.Builder(pageWidth, pageHeight, pageNumber).create())
                    canvas = page.canvas
                    y = margin
                }
                canvas.drawText(
                    "${t.date}  ${t.categoryName ?: "Sin Categoría"}  ${t.title}  ${"%,.0f".format(t.amount)}",
                    margin, y, textPaint
                )
                y += lineHeight
            }
            pdfDocument.finishPage(page)

            val fileName = "MisGastos_Reporte_${LocalDate.now()}.pdf"
            val outFile = File(context.cacheDir, fileName)
            outFile.outputStream().use { pdfDocument.writeTo(it) }
            pdfDocument.close()
            outFile
        }
        shareExportedFile(context, file, "application/pdf", "Abrir reporte")
        true
    } catch (e: Exception) {
        Toast.makeText(context, "Error al exportar: ${e.message}", Toast.LENGTH_LONG).show()
        false
    }
}

private fun csvEscape(value: String): String {
    return if (value.contains(",") || value.contains("\"") || value.contains("\n")) {
        "\"" + value.replace("\"", "\"\"") + "\""
    } else {
        value
    }
}

@RequiresApi(Build.VERSION_CODES.O)
private suspend fun exportCsv(context: Context): Boolean {
    return try {
        val transactions = loadTransactionsForExport(context)
        val file = withContext(Dispatchers.IO) {
            val fileName = "MisGastos_Datos_${LocalDate.now()}.csv"
            val outFile = File(context.cacheDir, fileName)
            outFile.bufferedWriter().use { writer ->
                writer.appendLine("Fecha,Título,Categoría,Monto")
                for (t in transactions) {
                    writer.appendLine(
                        "${t.date},${csvEscape(t.title)},${csvEscape(t.categoryName ?: "Sin Categoría")},${t.amount}"
                    )
                }
            }
            outFile
        }
        shareExportedFile(context, file, "text/csv", "Abrir datos")
        true
    } catch (e: Exception) {
        Toast.makeText(context, "Error al exportar: ${e.message}", Toast.LENGTH_LONG).show()
        false
    }
}
