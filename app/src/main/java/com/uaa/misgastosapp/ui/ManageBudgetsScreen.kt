// ManageBudgetsScreen

package com.uaa.misgastosapp.ui

import android.os.Build
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.uaa.misgastosapp.model.Budget
import com.uaa.misgastosapp.ui.viewmodel.BudgetViewModel
import java.text.NumberFormat
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.*
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue

// se asegura que el codigo use apis disponibles a partir de android oreo.
@RequiresApi(Build.VERSION_CODES.O)
// se usa esta anotacion para poder utilizar componentes de material 3 que aun son experimentales.
@OptIn(ExperimentalMaterial3Api::class)
// aca se define la pantalla (composable) para gestionar los presupuestos.
@Composable
fun ManageBudgetsScreen(
    navController: NavController,
    budgetViewModel: BudgetViewModel = viewModel()
) {
    // se obtienen los estados desde el viewmodel.
    val budgetsWithSpending by budgetViewModel.budgetsWithSpendingForCurrentMonth.collectAsState(initial = emptyList())
    val currentYearMonth by budgetViewModel.currentMonthYear.collectAsState()
    // se definen estados para manejar el dialogo de edicion de presupuesto.
    var showSetBudgetDialog by remember { mutableStateOf(false) }
    var selectedCategoryForBudget by remember { mutableStateOf<Budget?>(null) }
    val context = LocalContext.current
    val monthDisplayFormatter = DateTimeFormatter.ofPattern("MMMM yyyy", Locale("es", "ES"))

    // se usa el componente scaffold para la estructura de la pantalla.
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Gestionar Presupuestos") },
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
                },
                // se añade un navegador de meses en la barra de acciones.
                actions = {
                    MonthNavigator(
                        currentYearMonth = currentYearMonth,
                        onPreviousMonth = { budgetViewModel.setCurrentMonthYear(currentYearMonth.minusMonths(1)) },
                        onNextMonth = { budgetViewModel.setCurrentMonthYear(currentYearMonth.plusMonths(1)) }
                    )
                }
            )
        }
    ) { paddingValues ->
        Column(modifier = Modifier.padding(paddingValues).padding(8.dp)) {
            Text(
                text = "Presupuestos para: ${currentYearMonth.format(monthDisplayFormatter).replaceFirstChar { it.uppercase() }}",
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 8.dp)
            )
            // si no hay presupuestos, se muestra un mensaje.
            if (budgetsWithSpending.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("No hay categorías para presupuestar o no hay categorías creadas.")
                }
            } else {
                // si hay presupuestos, se muestran en una lista.
                LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(budgetsWithSpending) { budgetItem ->
                        BudgetListItem(
                            budget = budgetItem,
                            onEditClick = {
                                // al hacer clic en editar, se guarda la categoria y se abre el dialogo.
                                selectedCategoryForBudget = budgetItem
                                showSetBudgetDialog = true
                            }
                        )
                    }
                }
            }
        }
        // se muestra el dialogo para establecer el presupuesto si 'showsetbudgetdialog' es verdadero.
        if (showSetBudgetDialog && selectedCategoryForBudget != null) {
            SetBudgetDialog(
                budgetInfo = selectedCategoryForBudget!!,
                currentMonthYear = currentYearMonth,
                onDismiss = { showSetBudgetDialog = false },
                onSetBudget = { categoryId, amount, monthYearStr ->
                    budgetViewModel.setBudget(
                        categoryId,
                        amount,
                        monthYearStr,
                        onSuccess = {
                            Toast.makeText(context, "Presupuesto guardado", Toast.LENGTH_SHORT).show()
                            showSetBudgetDialog = false
                        },
                        onError = { errorMsg ->
                            Toast.makeText(context, errorMsg, Toast.LENGTH_LONG).show()
                        }
                    )
                }
            )
        }
    }
}

// se asegura que el codigo use apis disponibles a partir de android oreo.
@RequiresApi(Build.VERSION_CODES.O)
// este es un composable reutilizable para navegar entre meses.
@Composable
fun MonthNavigator(
    currentYearMonth: YearMonth,
    onPreviousMonth: () -> Unit,
    onNextMonth: () -> Unit
) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        IconButton(onClick = onPreviousMonth) {
            Text("<", style = MaterialTheme.typography.titleMedium)
        }
        Text(
            currentYearMonth.month.getDisplayName(TextStyle.SHORT, Locale("es", "ES")).uppercase(),
            style = MaterialTheme.typography.titleSmall
        )
        IconButton(onClick = onNextMonth) {
            Text(">", style = MaterialTheme.typography.titleMedium)
        }
    }
}

// se asegura que el codigo use apis disponibles a partir de android oreo.
@RequiresApi(Build.VERSION_CODES.O)
// este es un composable reutilizable para cada elemento de la lista de presupuestos.
@Composable
fun BudgetListItem(budget: Budget, onEditClick: () -> Unit) {
    val currencyFormat = NumberFormat.getCurrencyInstance(Locale("es", "PY")).apply {
        maximumFractionDigits = 0
    }
    // se determina el color de la barra de progreso.
    val progressColor = when {
        budget.progress > 1f -> MaterialTheme.colorScheme.error
        budget.progress > 0.85f -> Color(0xFFFFA000)
        else -> MaterialTheme.colorScheme.primary
    }
    Card(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(budget.categoryName, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    "Presupuesto: ${currencyFormat.format(budget.amount)}",
                    style = MaterialTheme.typography.bodyMedium
                )
                Text(
                    "Gastado: ${currencyFormat.format(budget.spentAmount)}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = if (budget.spentAmount > budget.amount && budget.amount > 0) MaterialTheme.colorScheme.error else LocalContentColor.current
                )
                // si hay un presupuesto establecido, se muestra la barra de progreso.
                if (budget.amount > 0) {
                    LinearProgressIndicator(
                        progress = { budget.progress.coerceIn(0f, 1f) },
                        modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
                        color = progressColor,
                        trackColor = progressColor.copy(alpha = 0.3f)
                    )
                    val percentage = (budget.progress * 100).toInt()
                    // se muestra un texto con el estado del presupuesto.
                    Text(
                        text = if (budget.progress > 1f) "Excedido en ${currencyFormat.format(budget.spentAmount - budget.amount)} (${percentage}%)"
                        else if (budget.progress > 0.85f) "Cercano al límite (${percentage}%)"
                        else "${percentage}% gastado",
                        style = MaterialTheme.typography.bodySmall,
                        color = progressColor
                    )
                } else {
                    Text("Sin presupuesto establecido", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.outline)
                }
            }
            IconButton(onClick = onEditClick) {
                Icon(Icons.Filled.Edit, contentDescription = "Editar Presupuesto")
            }
        }
    }
}

// se asegura que el codigo use apis disponibles a partir de android oreo.
@RequiresApi(Build.VERSION_CODES.O)
// se usa esta anotacion para poder utilizar componentes de material 3 que aun son experimentales.
@OptIn(ExperimentalMaterial3Api::class)
// este es el composable para el dialogo que permite establecer o editar un presupuesto.
@Composable
fun SetBudgetDialog(
    budgetInfo: Budget,
    currentMonthYear: YearMonth,
    onDismiss: () -> Unit,
    onSetBudget: (categoryId: Int, amount: Double, monthYearStr: String) -> Unit
) {
    // estado para el valor numerico real (sin formato).
    var rawAmount by remember {
        mutableStateOf(
            if (budgetInfo.amount > 0) budgetInfo.amount.toLong().toString() else ""
        )
    }

    // estado para el valor formateado que se muestra.
    var formattedAmount by remember {
        mutableStateOf(
            if (budgetInfo.amount > 0) {
                NumberFormat.getNumberInstance(Locale.US).format(budgetInfo.amount.toLong())
            } else ""
        )
    }

    val context = LocalContext.current
    val monthYearStr = currentMonthYear.format(DateTimeFormatter.ofPattern("yyyy-MM"))
    val monthDisplayFormatter = DateTimeFormatter.ofPattern("MMMM yyyy", Locale("es", "ES"))
    val numberFormatter = NumberFormat.getNumberInstance(Locale.US)

    // se muestra un dialogo.
    Dialog(onDismissRequest = onDismiss) {
        Card {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    "Establecer Presupuesto para",
                    style = MaterialTheme.typography.headlineSmall
                )
                Text(
                    budgetInfo.categoryName,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    "(${currentMonthYear.format(monthDisplayFormatter).replaceFirstChar { it.uppercase() }})",
                    style = MaterialTheme.typography.titleMedium
                )

                // campo de texto para ingresar el monto del presupuesto.
                OutlinedTextField(
                    value = formattedAmount,
                    // logica para formatear el numero mientras se escribe.
                    onValueChange = { input ->
                        // remover todo lo que no sea digito.
                        val digitsOnly = input.replace(",", "").filter { it.isDigit() }

                        if (digitsOnly.isEmpty()) {
                            rawAmount = ""
                            formattedAmount = ""
                        } else {
                            // limitar a un maximo razonable (999,999,999,999).
                            val numericValue = digitsOnly.take(12).toLongOrNull() ?: 0L
                            rawAmount = numericValue.toString()

                            // formatear con separadores de miles.
                            formattedAmount = numberFormatter.format(numericValue)
                        }
                    },
                    label = { Text("Monto del Presupuesto (PYG)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    prefix = { Text("₲ ") },
                    placeholder = { Text("0") }
                )

                // botones de accion del dialogo.
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onDismiss) {
                        Text("Cancelar")
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(onClick = {
                        val amount = rawAmount.toDoubleOrNull() ?: 0.0
                        // se realizan las validaciones del monto.
                        when {
                            rawAmount.isEmpty() -> {
                                Toast.makeText(context, "Por favor, ingrese un monto.", Toast.LENGTH_SHORT).show()
                            }
                            amount < 0 -> {
                                Toast.makeText(context, "El monto no puede ser negativo.", Toast.LENGTH_SHORT).show()
                            }
                            amount > 999999999999 -> {
                                Toast.makeText(context, "El monto es demasiado grande.", Toast.LENGTH_SHORT).show()
                            }
                            else -> {
                                // si todo es correcto, se llama a la funcion para guardar.
                                onSetBudget(budgetInfo.categoryId, amount, monthYearStr)
                            }
                        }
                    }) {
                        Text("Guardar")
                    }
                }
            }
        }
    }
}