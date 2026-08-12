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
import androidx.compose.material.icons.filled.Delete
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
import com.uaa.misgastosapp.data.PremiumLimits
import com.uaa.misgastosapp.data.PremiumManager
import com.uaa.misgastosapp.Routes
import com.uaa.misgastosapp.ui.components.AdBanner
import com.uaa.misgastosapp.ui.components.AppBottomNavBar
import com.uaa.misgastosapp.ui.components.ThousandsSeparatorVisualTransformation
import com.uaa.misgastosapp.utils.Result

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
    val recurringBudgetsCount by budgetViewModel.recurringBudgetsCount.collectAsState()
    val isLoadingBudgets by budgetViewModel.isLoading.collectAsState()
    val currentYearMonth by budgetViewModel.currentMonthYear.collectAsState()
    // se definen estados para manejar el dialogo de edicion de presupuesto.
    var showSetBudgetDialog by remember { mutableStateOf(false) }
    var selectedCategoryForBudget by remember { mutableStateOf<Budget?>(null) }
    // se usan estados para manejar el dialogo de confirmacion de borrado, igual que en
    // Categorías/Cuentas/Recurrentes (antes, quitar un presupuesto solo se podia hacer
    // escondido dentro del dialogo de editar).
    var budgetToDelete by remember { mutableStateOf<Budget?>(null) }
    val context = LocalContext.current
    val isPremium by PremiumManager.getInstance(context).isPremium.collectAsState()
    val monthDisplayFormatter = DateTimeFormatter.ofPattern("MMMM yyyy", Locale("es", "ES"))
    val operationStatus by budgetViewModel.operationStatus.collectAsState()

    // se observa el resultado de setBudget: exito cierra el dialogo y avisa, error solo avisa.
    LaunchedEffect(operationStatus) {
        when (val status = operationStatus) {
            is Result.Success -> {
                Toast.makeText(context, status.data, Toast.LENGTH_SHORT).show()
                budgetViewModel.clearOperationStatus()
                showSetBudgetDialog = false
            }
            is Result.Error -> {
                Toast.makeText(context, status.message, Toast.LENGTH_LONG).show()
                budgetViewModel.clearOperationStatus()
            }
            is Result.Loading -> {}
            null -> {}
        }
    }

    // se usa el componente scaffold para la estructura de la pantalla.
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Gestionar Presupuestos") },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = MaterialTheme.colorScheme.onPrimary,
                    actionIconContentColor = MaterialTheme.colorScheme.onPrimary,
                    navigationIconContentColor = MaterialTheme.colorScheme.onPrimary
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
                        onPreviousMonth = {
                            // el plan Free solo puede ver los ultimos FREE_HISTORY_MONTHS meses.
                            val earliestAllowed = YearMonth.now().minusMonths((PremiumLimits.FREE_HISTORY_MONTHS - 1).toLong())
                            if (isPremium || currentYearMonth.isAfter(earliestAllowed)) {
                                budgetViewModel.setCurrentMonthYear(currentYearMonth.minusMonths(1))
                            } else {
                                Toast.makeText(
                                    context,
                                    "El historial de más de ${PremiumLimits.FREE_HISTORY_MONTHS} meses es una función Premium.",
                                    Toast.LENGTH_SHORT
                                ).show()
                            }
                        },
                        onNextMonth = { budgetViewModel.setCurrentMonthYear(currentYearMonth.plusMonths(1)) }
                    )
                }
            )
        },
        // barra inferior compartida con Inicio/Categorías/Recurrentes/Gráficos, para que no
        // desaparezca al entrar a esta sección.
        bottomBar = { AppBottomNavBar(navController, Routes.MANAGE_BUDGETS) }
    ) { paddingValues ->
        Column(modifier = Modifier.padding(paddingValues).padding(8.dp)) {
            Text(
                text = "Presupuestos para: ${currentYearMonth.format(monthDisplayFormatter).replaceFirstChar { it.uppercase() }}",
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 8.dp)
            )
            // se distingue "cargando" de "realmente no hay categorias para presupuestar".
            if (isLoadingBudgets) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            } else if (budgetsWithSpending.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("No hay categorías para presupuestar o no hay categorías creadas.")
                }
            } else {
                // si hay presupuestos, se muestran en una lista.
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(budgetsWithSpending) { budgetItem ->
                        BudgetListItem(
                            budget = budgetItem,
                            onEditClick = {
                                // se avisa el limite del plan Free antes de abrir el dialogo (solo
                                // aplica si esta categoria todavia no tiene presupuesto, ya que
                                // editar uno existente no suma al limite).
                                val activeBudgetsThisMonth = budgetsWithSpending.count { it.amount > 0 }
                                if (budgetItem.amount <= 0 && !PremiumLimits.canAddBudget(context, activeBudgetsThisMonth)) {
                                    Toast.makeText(
                                        context,
                                        "Alcanzaste el límite de ${PremiumLimits.FREE_MAX_BUDGETS} presupuestos del plan Free. Pasate a Premium para presupuestos ilimitados.",
                                        Toast.LENGTH_LONG
                                    ).show()
                                } else {
                                    // al hacer clic en editar, se guarda la categoria y se abre el dialogo.
                                    selectedCategoryForBudget = budgetItem
                                    showSetBudgetDialog = true
                                }
                            },
                            onDeleteClick = { budgetToDelete = budgetItem }
                        )
                    }
                }
            }
            
            // Banner AdMob (solo usuarios free)
            val isPremium by PremiumManager.getInstance(context).isPremium.collectAsState()
            if (!isPremium) {
                Spacer(modifier = Modifier.height(8.dp))
                AdBanner()
            }
        }
        // se muestra el dialogo para establecer el presupuesto si 'showsetbudgetdialog' es verdadero.
        if (showSetBudgetDialog && selectedCategoryForBudget != null) {
            SetBudgetDialog(
                budgetInfo = selectedCategoryForBudget!!,
                currentMonthYear = currentYearMonth,
                onDismiss = { showSetBudgetDialog = false },
                recurringBudgetsCount = recurringBudgetsCount,
                onSetBudget = { categoryId, amount, monthYearStr, isRecurring ->
                    // ya tenia un presupuesto de ESE mismo tipo antes de abrir el dialogo? si es
                    // asi, guardarlo es una edicion y no suma al limite correspondiente.
                    val wasAlreadySameType = selectedCategoryForBudget?.let {
                        it.amount > 0 && it.isRecurring == isRecurring
                    } == true

                    if (isRecurring) {
                        // limite de presupuestos "todos los meses", independiente del de por mes.
                        if (!wasAlreadySameType && !PremiumLimits.canAddRecurringBudget(context, recurringBudgetsCount)) {
                            Toast.makeText(
                                context,
                                "Alcanzaste el límite de ${PremiumLimits.FREE_MAX_RECURRING_BUDGETS} presupuestos \"todos los meses\" del plan Free. Pasate a Premium para presupuestos recurrentes ilimitados.",
                                Toast.LENGTH_LONG
                            ).show()
                            return@SetBudgetDialog
                        }
                    } else {
                        // el limite de presupuestos por mes del plan Free cuenta cuantos ya
                        // tienen un monto establecido este mes; editar uno existente no suma.
                        val activeBudgetsThisMonth = budgetsWithSpending.count { it.amount > 0 && !it.isRecurring }
                        if (!wasAlreadySameType && !PremiumLimits.canAddBudget(context, activeBudgetsThisMonth)) {
                            Toast.makeText(
                                context,
                                "Alcanzaste el límite de ${PremiumLimits.FREE_MAX_BUDGETS} presupuestos del plan Free. Pasate a Premium para presupuestos ilimitados.",
                                Toast.LENGTH_LONG
                            ).show()
                            return@SetBudgetDialog
                        }
                    }
                    // si el usuario cambio de tipo (ej. de "este mes" a "todos los meses"), se
                    // quita la version vieja para no dejar dos presupuestos activos a la vez.
                    if (selectedCategoryForBudget?.amount ?: 0.0 > 0.0 &&
                        selectedCategoryForBudget?.isRecurring != isRecurring
                    ) {
                        budgetViewModel.deleteBudget(categoryId, monthYearStr, selectedCategoryForBudget?.isRecurring ?: false)
                    }
                    // el resultado se maneja en el LaunchedEffect que observa operationStatus.
                    budgetViewModel.setBudget(categoryId, amount, monthYearStr, isRecurring)
                },
                onDeleteBudget = { categoryId, monthYearStrToDelete, isRecurring ->
                    // el resultado se maneja en el LaunchedEffect que observa operationStatus.
                    budgetViewModel.deleteBudget(categoryId, monthYearStrToDelete, isRecurring)
                }
            )
        }

        // se muestra el dialogo de confirmacion si hay un presupuesto seleccionado para borrar.
        if (budgetToDelete != null) {
            AlertDialog(
                onDismissRequest = { budgetToDelete = null },
                title = { Text("Quitar Presupuesto") },
                text = {
                    Text(
                        "¿Quitar el presupuesto de '${budgetToDelete?.categoryName}'? Las transacciones ya registradas no se borran."
                    )
                },
                confirmButton = {
                    TextButton(onClick = {
                        budgetToDelete?.let { budgetViewModel.deleteBudget(it.categoryId, it.monthYear, it.isRecurring) }
                        budgetToDelete = null
                    }) {
                        Text("Quitar", color = MaterialTheme.colorScheme.error)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { budgetToDelete = null }) {
                        Text("Cancelar")
                    }
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
fun BudgetListItem(budget: Budget, onEditClick: () -> Unit, onDeleteClick: () -> Unit) {
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
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(budget.categoryName, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    if (budget.amount > 0 && budget.isRecurring) {
                        Spacer(modifier = Modifier.width(6.dp))
                        Surface(
                            shape = MaterialTheme.shapes.small,
                            color = MaterialTheme.colorScheme.secondaryContainer
                        ) {
                            Text(
                                "Todos los meses",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSecondaryContainer,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }
                }
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
            // quitar solo tiene sentido si ya hay un presupuesto establecido para esta categoria.
            if (budget.amount > 0) {
                IconButton(onClick = onDeleteClick) {
                    Icon(Icons.Filled.Delete, contentDescription = "Quitar Presupuesto")
                }
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
    recurringBudgetsCount: Int,
    onDismiss: () -> Unit,
    onSetBudget: (categoryId: Int, amount: Double, monthYearStr: String, isRecurring: Boolean) -> Unit,
    onDeleteBudget: (categoryId: Int, monthYearStr: String, isRecurring: Boolean) -> Unit
) {
    // estado para el valor numerico real (solo digitos); el separador de miles se agrega solo
    // para mostrarlo via ThousandsSeparatorVisualTransformation, nunca se reformatea el texto
    // real (eso hacia saltar el cursor al final cada vez que se escribia un digito).
    var rawAmount by remember {
        mutableStateOf(
            if (budgetInfo.amount > 0) budgetInfo.amount.toLong().toString() else ""
        )
    }
    // "este mes" o "todos los meses"; arranca con el valor actual del presupuesto (o "este mes"
    // si todavia no hay ninguno establecido para esta categoria).
    var isRecurring by remember { mutableStateOf(budgetInfo.isRecurring) }

    val context = LocalContext.current
    val isPremium by PremiumManager.getInstance(context).isPremium.collectAsState()
    val monthYearStr = currentMonthYear.format(DateTimeFormatter.ofPattern("yyyy-MM"))
    val monthDisplayFormatter = DateTimeFormatter.ofPattern("MMMM yyyy", Locale("es", "ES"))

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
                    value = rawAmount,
                    // se limita a un maximo razonable (999,999,999,999) y solo digitos.
                    onValueChange = { input -> rawAmount = input.filter { it.isDigit() }.take(12) },
                    label = { Text("Monto del Presupuesto (PYG)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    prefix = { Text("₲ ") },
                    placeholder = { Text("0") },
                    visualTransformation = ThousandsSeparatorVisualTransformation()
                )

                // permite elegir si el presupuesto aplica solo a este mes o a todos los meses
                // (no hace falta volver a configurarlo cada mes). los recurrentes tienen su
                // propio limite en el plan Free, separado del de presupuestos por mes.
                Column(modifier = Modifier.fillMaxWidth()) {
                    SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                        SegmentedButton(
                            selected = !isRecurring,
                            onClick = { isRecurring = false },
                            shape = SegmentedButtonDefaults.itemShape(index = 0, count = 2)
                        ) {
                            Text("Este mes")
                        }
                        SegmentedButton(
                            selected = isRecurring,
                            onClick = { isRecurring = true },
                            shape = SegmentedButtonDefaults.itemShape(index = 1, count = 2)
                        ) {
                            Text("Todos los meses")
                        }
                    }
                    if (isRecurring && !isPremium) {
                        Text(
                            "Plan Free: $recurringBudgetsCount / ${PremiumLimits.FREE_MAX_RECURRING_BUDGETS} presupuestos \"todos los meses\"",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.outline,
                            modifier = Modifier.padding(top = 4.dp)
                        )
                    }
                }

                // quitar el presupuesto solo tiene sentido si ya hay uno establecido; no borra
                // las transacciones, solo el limite. se usa el tipo ORIGINAL (budgetInfo.isRecurring),
                // no el del toggle, ya que "quitar" debe apuntar a lo que realmente existe en la bd.
                if (budgetInfo.amount > 0) {
                    TextButton(
                        onClick = { onDeleteBudget(budgetInfo.categoryId, monthYearStr, budgetInfo.isRecurring) },
                        modifier = Modifier.align(Alignment.Start)
                    ) {
                        Text("Quitar presupuesto", color = MaterialTheme.colorScheme.error)
                    }
                }

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
                                onSetBudget(budgetInfo.categoryId, amount, monthYearStr, isRecurring)
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