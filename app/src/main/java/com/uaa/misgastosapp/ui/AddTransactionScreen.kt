// AddTransactionScreen.kt

package com.uaa.misgastosapp.ui

import android.os.Build
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.uaa.misgastosapp.Routes
import com.uaa.misgastosapp.model.Category
import com.uaa.misgastosapp.ui.viewmodel.BudgetViewModel
import com.uaa.misgastosapp.ui.viewmodel.CategoryViewModel
import com.uaa.misgastosapp.ui.viewmodel.TransactionViewModel
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch
import java.text.NumberFormat
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.util.*

// se asegura que el codigo use apis disponibles a partir de android oreo.
@RequiresApi(Build.VERSION_CODES.O)
// se usa esta anotacion para poder utilizar componentes de material 3 que aun son experimentales.
@OptIn(ExperimentalMaterial3Api::class)
// aca se define la pantalla (composable) para añadir una nueva transaccion.
@Composable
fun AddTransactionScreen(
    navController: NavController,
    transactionViewModel: TransactionViewModel = viewModel(),
    categoryViewModel: CategoryViewModel = viewModel(),
    budgetViewModel: BudgetViewModel = viewModel()
) {
    // --- INICIO DE MODIFICACIONES ---

    // 1. Se añade un estado para el tipo de transacción. Por defecto es "EGRESO".
    var transactionType by rememberSaveable { mutableStateOf("EGRESO") }

    // --- FIN DE MODIFICACIONES ---

    var title by rememberSaveable { mutableStateOf("") }
    var amount by rememberSaveable { mutableStateOf("") } // monto con formato de miles.
    var rawAmount by rememberSaveable { mutableStateOf("") } // monto sin formato.
    var showError by rememberSaveable { mutableStateOf(false) }
    var errorMessage by rememberSaveable { mutableStateOf("") }
    val categories by categoryViewModel.categories.collectAsState()
    var selectedCategoryId by rememberSaveable { mutableStateOf<Int?>(null) }
    val selectedCategory = categories.find { it.id == selectedCategoryId }
    var categoryDropdownExpanded by rememberSaveable { mutableStateOf(false) }

    val numberFormat = NumberFormat.getNumberInstance(Locale.US)
    val currencyFormat = NumberFormat.getCurrencyInstance(Locale("es", "PY")).apply { maximumFractionDigits = 0 }
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val scrollState = rememberScrollState()

    Scaffold(
        topBar = {
            TopAppBar(
                // 2. Título actualizado para ser más genérico.
                title = { Text("Agregar Transacción") },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = Color.White,
                    actionIconContentColor = Color.White,
                    navigationIconContentColor = Color.White
                ),
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(imageVector = Icons.Default.ArrowBack, contentDescription = "Volver")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .verticalScroll(scrollState)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // --- INICIO DE MODIFICACIONES ---

            // 3. Selector de tipo de transacción (Ingreso/Egreso).
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Botón de Egreso
                Button(
                    onClick = { transactionType = "EGRESO" },
                    modifier = Modifier.weight(1f),
                    colors = if (transactionType == "EGRESO") ButtonDefaults.buttonColors() else ButtonDefaults.outlinedButtonColors(),
                    border = if (transactionType == "EGRESO") null else ButtonDefaults.outlinedButtonBorder
                ) {
                    Text("Egreso")
                }
                // Botón de Ingreso
                Button(
                    onClick = { transactionType = "INGRESO" },
                    modifier = Modifier.weight(1f),
                    colors = if (transactionType == "INGRESO") ButtonDefaults.buttonColors() else ButtonDefaults.outlinedButtonColors(),
                    border = if (transactionType == "INGRESO") null else ButtonDefaults.outlinedButtonBorder
                ) {
                    Text("Ingreso")
                }
            }

            // --- FIN DE MODIFICACIONES ---

            OutlinedTextField(
                value = title,
                onValueChange = { title = it },
                label = { Text("Descripción") },
                modifier = Modifier.fillMaxWidth(),
                isError = showError && title.isBlank(),
                singleLine = true
            )

            OutlinedTextField(
                value = amount,
                onValueChange = { input ->
                    // 4. Se elimina la lógica para aceptar el signo negativo. El usuario solo introduce números positivos.
                    val cleanedInput = input.replace(",", "").filter { c ->
                        c.isDigit() || c == '.'
                    }
                    rawAmount = cleanedInput

                    val formatted = try {
                        if (cleanedInput.isNotBlank()) {
                            val parsed = cleanedInput.toDouble()
                            numberFormat.format(parsed)
                        } else ""
                    } catch (e: Exception) {
                        cleanedInput
                    }
                    amount = formatted
                },
                label = { Text("Monto") },
                modifier = Modifier.fillMaxWidth(),
                isError = showError && (rawAmount.toDoubleOrNull() ?: 0.0) == 0.0,
                singleLine = true
            )

            // El menú de categorías se muestra solo para egresos
            AnimatedVisibility(visible = transactionType == "EGRESO") {
                ExposedDropdownMenuBox(
                    expanded = categoryDropdownExpanded,
                    onExpandedChange = { categoryDropdownExpanded = !categoryDropdownExpanded },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    OutlinedTextField(
                        value = selectedCategory?.name ?: "Seleccionar Categoría (Opcional)",
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Categoría") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = categoryDropdownExpanded) },
                        modifier = Modifier
                            .menuAnchor()
                            .fillMaxWidth()
                    )
                    ExposedDropdownMenu(
                        expanded = categoryDropdownExpanded,
                        onDismissRequest = { categoryDropdownExpanded = false },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        DropdownMenuItem(
                            text = { Text("Sin Categoría") },
                            onClick = {
                                selectedCategoryId = null
                                categoryDropdownExpanded = false
                            }
                        )
                        categories.forEach { category ->
                            DropdownMenuItem(
                                text = { Text(category.name) },
                                onClick = {
                                    selectedCategoryId = category.id
                                    categoryDropdownExpanded = false
                                }
                            )
                        }
                        DropdownMenuItem(
                            text = { Text("+ Añadir nueva categoría...", color = MaterialTheme.colorScheme.primary) },
                            onClick = {
                                categoryDropdownExpanded = false
                                navController.navigate(Routes.ADD_CATEGORY)
                            }
                        )
                    }
                }
            }

            if (showError) {
                Text(
                    text = errorMessage,
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall
                )
            }

            Button(
                modifier = Modifier.fillMaxWidth(),
                onClick = {
                    val parsedAmount = rawAmount.toDoubleOrNull() ?: 0.0

                    when {
                        title.isBlank() -> {
                            errorMessage = "La descripción no puede estar vacía."
                            showError = true
                        }
                        parsedAmount == 0.0 -> {
                            errorMessage = "El monto не puede ser cero."
                            showError = true
                        }
                        else -> {
                            showError = false

                            // --- INICIO DE MODIFICACIONES ---

                            // 5. Se determina el monto final a guardar.
                            // Si es "EGRESO", se multiplica por -1 para hacerlo negativo.
                            // Si es "INGRESO", se mantiene positivo.
                            val finalAmount = if (transactionType == "EGRESO") -parsedAmount else parsedAmount

                            // Si es un ingreso, la categoría no es relevante, se pasa null.
                            val finalCategoryId = if (transactionType == "INGRESO") null else selectedCategoryId

                            // --- FIN DE MODIFICACIONES ---

                            coroutineScope.launch {
                                // 6. Se envía el monto y la categoría final al ViewModel.
                                transactionViewModel.addTransaction(
                                    title = title,
                                    amount = finalAmount,
                                    date = LocalDate.now().toString(),
                                    categoryId = finalCategoryId
                                )

                                // La lógica de alerta de presupuesto solo se aplica a los egresos.
                                if (transactionType == "EGRESO" && selectedCategory != null) {
                                    val monthYearStr = YearMonth.now().format(DateTimeFormatter.ofPattern("yyyy-MM"))
                                    val budgetEntity = budgetViewModel.getBudgetForCategory(selectedCategory.id, monthYearStr).firstOrNull()

                                    if (budgetEntity != null && budgetEntity.amount > 0) {
                                        val previousTransactions = transactionViewModel.transactions.firstOrNull() ?: emptyList()
                                        val spentBeforeThisTransaction = previousTransactions
                                            .filter {
                                                it.categoryId == selectedCategory.id &&
                                                        it.date.startsWith(monthYearStr) &&
                                                        it.amount < 0
                                            }
                                            .sumOf { it.amount * -1 }

                                        // Se usa el monto positivo para el cálculo
                                        val totalSpentAfterThisTransaction = spentBeforeThisTransaction + parsedAmount

                                        if (totalSpentAfterThisTransaction > budgetEntity.amount) {
                                            val exceededBy = totalSpentAfterThisTransaction - budgetEntity.amount
                                            Toast.makeText(
                                                context,
                                                "¡Alerta! Has excedido el presupuesto de ${selectedCategory.name} en ${currencyFormat.format(exceededBy)}",
                                                Toast.LENGTH_LONG
                                            ).show()
                                        } else if (totalSpentAfterThisTransaction > budgetEntity.amount * 0.85) {
                                            Toast.makeText(
                                                context,
                                                "¡Cuidado! Te estás acercando al límite del presupuesto de ${selectedCategory.name}.",
                                                Toast.LENGTH_LONG
                                            ).show()
                                        }
                                    }
                                }

                                navController.popBackStack()
                            }

                        }
                    }
                }) {
                Text("Guardar")
            }
        }
    }
}