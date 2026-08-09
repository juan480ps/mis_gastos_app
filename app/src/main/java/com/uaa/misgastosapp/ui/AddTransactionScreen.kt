// AddTRansactionScreen.kt

package com.uaa.misgastosapp.ui

import android.os.Build
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.material3.MenuAnchorType
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
// aca se define la pantalla (composable) para añadir una nueva transaccion (gasto).
@Composable
fun AddTransactionScreen(
    navController: NavController,
    transactionViewModel: TransactionViewModel = viewModel(),
    categoryViewModel: CategoryViewModel = viewModel(),
    budgetViewModel: BudgetViewModel = viewModel()
) {
    // se declaran los estados para los campos del formulario. 'remembersaveable' se usa para que los datos sobrevivan a cambios de configuracion.
    var title by rememberSaveable { mutableStateOf("") }
    var amount by rememberSaveable { mutableStateOf("") } // monto con formato de miles.
    var rawAmount by rememberSaveable { mutableStateOf("") } // monto sin formato.
    var showError by rememberSaveable { mutableStateOf(false) }
    var errorMessage by rememberSaveable { mutableStateOf("") }
    val categories by categoryViewModel.categories.collectAsState()
    var selectedCategoryId by rememberSaveable { mutableStateOf<Int?>(null) }
    val selectedCategory = categories.find { it.id == selectedCategoryId }
    var categoryDropdownExpanded by rememberSaveable { mutableStateOf(false) }

    // se configuran formatos de numeros y se obtienen instancias utiles.
    val numberFormat = NumberFormat.getNumberInstance(Locale.US)
    val currencyFormat = NumberFormat.getCurrencyInstance(Locale("es", "PY")).apply { maximumFractionDigits = 0 }
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val scrollState = rememberScrollState()

    // se usa el componente scaffold para la estructura basica de la pantalla.
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Agregar Gasto") },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = Color.White,
                    actionIconContentColor = Color.White,
                    navigationIconContentColor = Color.White
                ),
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(imageVector = Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Volver")
                    }
                }
            )
        }
    ) { padding ->
        // se usa una columna con scroll para el formulario.
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .verticalScroll(scrollState)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // campo de texto para la descripcion.
            OutlinedTextField(
                value = title,
                onValueChange = { title = it },
                label = { Text("Descripción") },
                modifier = Modifier.fillMaxWidth(),
                isError = showError && title.isBlank(),
                singleLine = true
            )

            // campo de texto para el monto.
            OutlinedTextField(
                value = amount,
                // logica para limpiar y formatear el monto mientras el usuario escribe.
                onValueChange = { input ->
                    val cleanedInput = input.replace(",", "").filterIndexed { index, c ->
                        c.isDigit() || c == '.' || (c == '-' && index == 0)
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

            // menu desplegable para seleccionar la categoria.
            ExposedDropdownMenuBox(
                expanded = categoryDropdownExpanded,
                onExpandedChange = { categoryDropdownExpanded = !categoryDropdownExpanded },
                modifier = Modifier.fillMaxWidth()
            ) {
                OutlinedTextField(
                    value = selectedCategory?.name ?: "Seleccionar Categoría",
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Categoría") },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = categoryDropdownExpanded) },
                    modifier = Modifier
                        .menuAnchor(MenuAnchorType.PrimaryNotEditable, true)
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
                    // opcion para navegar a la pantalla de añadir nueva categoria.
                    DropdownMenuItem(
                        text = { Text("+ Añadir nueva categoría...", color = MaterialTheme.colorScheme.primary) },
                        onClick = {
                            categoryDropdownExpanded = false
                            navController.navigate(Routes.ADD_CATEGORY)
                        }
                    )
                }
            }

            // se muestra un mensaje de error si es necesario.
            if (showError) {
                Text(
                    text = errorMessage,
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall
                )
            }

            // boton para guardar la transaccion.
            Button(
                modifier = Modifier.fillMaxWidth(),
                onClick = {
                    val parsedAmount = rawAmount.toDoubleOrNull() ?: 0.0

                    // se realizan las validaciones.
                    when {
                        title.isBlank() -> {
                            errorMessage = "La descripción no puede estar vacía."
                            showError = true
                        }
                        parsedAmount == 0.0 -> {
                            errorMessage = "El monto no puede ser cero."
                            showError = true
                        }
                        else -> {
                            showError = false

                            // se inicia una corutina para realizar las operaciones.
                            coroutineScope.launch {
                                // se añade la transaccion a traves del viewmodel.
                                transactionViewModel.addTransaction(
                                    title = title,
                                    amount = parsedAmount,
                                    date = LocalDate.now().toString(),
                                    categoryId = selectedCategoryId
                                )

                                // logica para revisar y mostrar alertas sobre el presupuesto.
                                val currentExpenseAmount = if (parsedAmount < 0) parsedAmount * -1 else parsedAmount
                                if (parsedAmount < 0 && selectedCategory != null) {
                                    val monthYearStr = YearMonth.now().format(DateTimeFormatter.ofPattern("yyyy-MM"))
                                    // se obtiene el presupuesto para la categoria y mes actuales.
                                    val budgetEntity = budgetViewModel.getBudgetForCategory(selectedCategory.id, monthYearStr).firstOrNull()

                                    if (budgetEntity != null && budgetEntity.amount > 0) {
                                        // se calcula el gasto total en la categoria despues de esta nueva transaccion.
                                        val previousTransactions = transactionViewModel.transactions.firstOrNull() ?: emptyList()
                                        val spentBeforeThisTransaction = previousTransactions
                                            .filter {
                                                it.categoryId == selectedCategory.id &&
                                                        it.date.startsWith(monthYearStr) &&
                                                        it.amount < 0
                                            }
                                            .sumOf { it.amount * -1 }

                                        val totalSpentAfterThisTransaction = spentBeforeThisTransaction + currentExpenseAmount

                                        // se comprueba si se ha excedido el presupuesto.
                                        if (totalSpentAfterThisTransaction > budgetEntity.amount) {
                                            val exceededBy = totalSpentAfterThisTransaction - budgetEntity.amount
                                            Toast.makeText(
                                                context,
                                                "¡Alerta! Has excedido el presupuesto de ${selectedCategory.name} en ${currencyFormat.format(exceededBy)}",
                                                Toast.LENGTH_LONG
                                            ).show()
                                            // se comprueba si se ha gastado mas del 85% del presupuesto.
                                        } else if (totalSpentAfterThisTransaction > budgetEntity.amount * 0.85) {
                                            Toast.makeText(
                                                context,
                                                "¡Cuidado! Te estás acercando al límite del presupuesto de ${selectedCategory.name}.",
                                                Toast.LENGTH_LONG
                                            ).show()
                                        }
                                    }
                                }

                                // se vuelve a la pantalla anterior.
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