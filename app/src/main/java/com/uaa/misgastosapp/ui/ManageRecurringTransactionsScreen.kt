// ManageRecurringTransactionScreen

package com.uaa.misgastosapp.ui

import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.uaa.misgastosapp.Routes
import androidx.compose.ui.platform.LocalContext
import com.uaa.misgastosapp.data.PremiumManager
import com.uaa.misgastosapp.model.RecurringTransaction
import com.uaa.misgastosapp.ui.components.AdBanner
import com.uaa.misgastosapp.ui.viewmodel.RecurringTransactionViewModel
import java.text.NumberFormat
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.*

// se asegura que el codigo use apis disponibles a partir de android oreo.
@RequiresApi(Build.VERSION_CODES.O)
// se usa esta anotacion para poder utilizar componentes de material 3 que aun son experimentales.
@OptIn(ExperimentalMaterial3Api::class)
// aca se define la pantalla (composable) para gestionar las transacciones recurrentes.
@Composable
fun ManageRecurringTransactionsScreen(
    navController: NavController,
    recurringViewModel: RecurringTransactionViewModel = viewModel()
) {
    // se obtiene la lista de transacciones recurrentes desde el viewmodel.
    val recurringTransactions by recurringViewModel.recurringTransactions.collectAsState()

    // se usa el componente scaffold para la estructura de la pantalla.
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Transacciones Recurrentes") },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = Color.White,
                    actionIconContentColor = Color.White,
                    navigationIconContentColor = Color.White
                ),
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Volver")
                    }
                }
            )
        },
        // se define un boton de accion flotante para añadir nuevas transacciones recurrentes.
        floatingActionButton = {
            FloatingActionButton(onClick = {
                // se navega a la pantalla de añadir/editar sin pasarle un id, lo que indica que es para añadir una nueva.
                navController.navigate(Routes.ADD_EDIT_RECURRING_TRANSACTION)
            },
                containerColor = MaterialTheme.colorScheme.primary,
                shape = CircleShape
            ) {
                Icon(Icons.Filled.Add, contentDescription = "Añadir Recurrente")
            }
        }
    ) { paddingValues ->
        Column(modifier = Modifier.padding(paddingValues).padding(8.dp)) {
            // si no hay transacciones, se muestra un mensaje.
            if (recurringTransactions.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("No hay transacciones recurrentes configuradas.")
                }
            } else {
                // si hay transacciones, se muestran en una lista.
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(recurringTransactions) { item ->
                        RecurringTransactionListItem(
                            item = item,
                            onEdit = {
                                // al editar, se navega a la pantalla de añadir/editar, pasandole el id del item.
                                navController.navigate("${Routes.ADD_EDIT_RECURRING_TRANSACTION}/${item.id}")
                            },
                            // al borrar, se llama directamente al viewmodel.
                            onDelete = { recurringViewModel.deleteRecurringTransaction(item) }
                        )
                    }
                }
            }
            
            // Banner AdMob (solo usuarios free)
            val context = LocalContext.current
            val isPremium by PremiumManager.getInstance(context).isPremium.collectAsState()
            if (!isPremium) {
                Spacer(modifier = Modifier.height(8.dp))
                AdBanner()
            }
        }
    }
}

// se asegura que el codigo use apis disponibles a partir de android oreo.
@RequiresApi(Build.VERSION_CODES.O)
// este es un composable reutilizable para cada elemento de la lista de transacciones recurrentes.
@Composable
fun RecurringTransactionListItem(
    item: RecurringTransaction,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    // se configuran los formatos de moneda y fecha.
    val currencyFormat = NumberFormat.getCurrencyInstance(Locale("es", "PY")).apply {
        maximumFractionDigits = 0
    }
    val dateFormat = DateTimeFormatter.ofPattern("dd MMM yyyy", Locale("es", "ES"))

    // se usa una card como contenedor del elemento.
    Card(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.padding(16.dp).fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                // se muestra la informacion de la transaccion.
                Text(item.title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Text(
                    // monto negativo = gasto, monto positivo = ingreso.
                    "${if (item.amount < 0) "Gasto" else "Ingreso"}: ${currencyFormat.format(item.amount.let { if(it < 0) it * -1 else it })}",
                    style = MaterialTheme.typography.bodyMedium
                )
                Text("Categoría: ${item.categoryName}", style = MaterialTheme.typography.bodySmall)
                Text(
                    "Próximo: ${LocalDate.parse(item.nextDueDate).format(dateFormat)}",
                    style = MaterialTheme.typography.bodySmall,
                    // el color del texto cambia si la transaccion esta inactiva.
                    color = if (item.isActive) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline
                )
                // si no esta activa, se muestra una etiqueta de "inactiva".
                if (!item.isActive) {
                    Text("INACTIVA", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.error)
                }
            }
            // se añaden los botones de accion para editar y eliminar.
            IconButton(onClick = onEdit) {
                Icon(Icons.Filled.Edit, contentDescription = "Editar")
            }
            IconButton(onClick = onDelete) {
                Icon(Icons.Filled.Delete, contentDescription = "Eliminar")
            }
        }
    }
}