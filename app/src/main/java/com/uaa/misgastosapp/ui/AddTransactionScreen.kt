// AddTRansactionScreen.kt

package com.uaa.misgastosapp.ui

import android.os.Build
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.material3.MenuAnchorType
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.uaa.misgastosapp.Routes
import com.uaa.misgastosapp.data.PremiumLimits
import com.uaa.misgastosapp.data.PremiumManager
import com.uaa.misgastosapp.model.Category
import com.uaa.misgastosapp.ui.components.AdBanner
import com.uaa.misgastosapp.ui.components.ThousandsSeparatorVisualTransformation
import com.uaa.misgastosapp.ui.viewmodel.AccountViewModel
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
    transactionId: Int? = null,
    // cuenta que estaba filtrada en Inicio al presionar "+"; solo aplica al crear una nueva
    // transaccion (en modo edicion se precargan los datos reales de la transaccion existente).
    preselectedAccountId: Int? = null,
    transactionViewModel: TransactionViewModel = viewModel(),
    categoryViewModel: CategoryViewModel = viewModel(),
    budgetViewModel: BudgetViewModel = viewModel(),
    accountViewModel: AccountViewModel = viewModel()
) {
    val isEditing = transactionId != null
    // se declaran los estados para los campos del formulario. 'remembersaveable' se usa para que los datos sobrevivan a cambios de configuracion.
    var title by rememberSaveable { mutableStateOf("") }
    // true = gasto (resta), false = ingreso (suma). El usuario ya no tiene que escribir un
    // numero negativo para que se considere un gasto: el signo lo decide este selector.
    var isExpense by rememberSaveable { mutableStateOf(true) }
    // solo digitos; el separador de miles se agrega al mostrarlo via ThousandsSeparatorVisualTransformation,
    // nunca se reformatea el texto real (eso es lo que hacia saltar el cursor antes).
    var rawAmount by rememberSaveable { mutableStateOf("") }
    // fecha original de la transaccion al editar; una nueva siempre usa la fecha de hoy.
    var transactionDate by rememberSaveable { mutableStateOf(LocalDate.now().toString()) }
    var showError by rememberSaveable { mutableStateOf(false) }
    var errorMessage by rememberSaveable { mutableStateOf("") }
    val categories by categoryViewModel.categories.collectAsState()
    var selectedCategoryId by rememberSaveable { mutableStateOf<Int?>(null) }
    val selectedCategory = categories.find { it.id == selectedCategoryId }
    var categoryDropdownExpanded by rememberSaveable { mutableStateOf(false) }
    // asociar una cuenta/banco es opcional: por defecto queda "Sin cuenta" (la bolsa general de
    // siempre), salvo que se venga de Inicio con una cuenta especifica filtrada.
    val accounts by accountViewModel.accounts.collectAsState()
    var selectedAccountId by rememberSaveable { mutableStateOf(preselectedAccountId) }
    val selectedAccount = accounts.find { it.id == selectedAccountId }
    var accountDropdownExpanded by rememberSaveable { mutableStateOf(false) }

    // en modo edicion, precarga los datos de la transaccion existente apenas esta disponible.
    val allTransactions by transactionViewModel.transactions.collectAsState()
    var loadedExistingTransaction by remember { mutableStateOf(false) }
    LaunchedEffect(transactionId, allTransactions) {
        if (transactionId != null && !loadedExistingTransaction) {
            allTransactions.find { it.id == transactionId }?.let { existing ->
                title = existing.title
                isExpense = existing.amount < 0
                rawAmount = kotlin.math.abs(existing.amount).toLong().toString()
                transactionDate = existing.date
                selectedCategoryId = existing.categoryId
                selectedAccountId = existing.accountId
                loadedExistingTransaction = true
            }
        }
    }

    // al volver de "+ Añadir nueva categoría/cuenta...", la que se acaba de crear queda
    // seleccionada automaticamente en vez de quedar en "Sin categoría/Sin cuenta".
    val savedStateHandle = navController.currentBackStackEntry?.savedStateHandle
    val newCategoryId = savedStateHandle?.getStateFlow<Int?>("newCategoryId", null)?.collectAsState()
    val newAccountId = savedStateHandle?.getStateFlow<Int?>("newAccountId", null)?.collectAsState()
    LaunchedEffect(newCategoryId?.value) {
        newCategoryId?.value?.let { id ->
            selectedCategoryId = id
            savedStateHandle?.remove<Int?>("newCategoryId")
        }
    }
    LaunchedEffect(newAccountId?.value) {
        newAccountId?.value?.let { id ->
            selectedAccountId = id
            savedStateHandle?.remove<Int?>("newAccountId")
        }
    }

    // se configuran formatos de numeros y se obtienen instancias utiles.
    val currencyFormat = NumberFormat.getCurrencyInstance(Locale("es", "PY")).apply { maximumFractionDigits = 0 }
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val scrollState = rememberScrollState()

    // se usa el componente scaffold para la estructura basica de la pantalla.
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        when {
                            isEditing && isExpense -> "Editar Gasto"
                            isEditing -> "Editar Ingreso"
                            isExpense -> "Agregar Gasto"
                            else -> "Agregar Ingreso"
                        }
                    )
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = MaterialTheme.colorScheme.onPrimary,
                    actionIconContentColor = MaterialTheme.colorScheme.onPrimary,
                    navigationIconContentColor = MaterialTheme.colorScheme.onPrimary
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
                singleLine = true,
                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences)
            )

            // selector Gasto/Ingreso: reemplaza tener que escribir un monto negativo para que
            // se considere un gasto, que era confuso y facil de olvidar.
            SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                SegmentedButton(
                    selected = isExpense,
                    onClick = { isExpense = true },
                    shape = SegmentedButtonDefaults.itemShape(index = 0, count = 2)
                ) {
                    Text("Gasto")
                }
                SegmentedButton(
                    selected = !isExpense,
                    onClick = { isExpense = false },
                    shape = SegmentedButtonDefaults.itemShape(index = 1, count = 2)
                ) {
                    Text("Ingreso")
                }
            }

            // campo de texto para el monto: siempre se escribe positivo, el signo lo aplica el selector de arriba.
            // el texto real son solo digitos; el separador de miles es puramente visual (VisualTransformation),
            // asi el cursor nunca salta al final mientras se escribe.
            OutlinedTextField(
                value = rawAmount,
                onValueChange = { input -> rawAmount = input.filter { it.isDigit() } },
                label = { Text("Monto") },
                modifier = Modifier.fillMaxWidth(),
                isError = showError && (rawAmount.toDoubleOrNull() ?: 0.0) == 0.0,
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                visualTransformation = ThousandsSeparatorVisualTransformation()
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
                    // opcion para navegar a la pantalla de añadir nueva categoria. se avisa el
                    // limite del plan Free antes de abrir el formulario, no despues de llenarlo.
                    DropdownMenuItem(
                        text = { Text("+ Añadir nueva categoría...", color = MaterialTheme.colorScheme.primary) },
                        onClick = {
                            categoryDropdownExpanded = false
                            if (!PremiumLimits.canAddCategory(context, categories.size)) {
                                Toast.makeText(
                                    context,
                                    "Alcanzaste el límite de ${PremiumLimits.FREE_MAX_CATEGORIES} categorías del plan Free. Pasate a Premium para categorías ilimitadas.",
                                    Toast.LENGTH_LONG
                                ).show()
                            } else {
                                navController.navigate(Routes.ADD_CATEGORY)
                            }
                        }
                    )
                }
            }

            // menu desplegable para asociar una cuenta/banco (opcional).
            ExposedDropdownMenuBox(
                expanded = accountDropdownExpanded,
                onExpandedChange = { accountDropdownExpanded = !accountDropdownExpanded },
                modifier = Modifier.fillMaxWidth()
            ) {
                OutlinedTextField(
                    value = selectedAccount?.name ?: "Sin cuenta (opcional)",
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Cuenta") },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = accountDropdownExpanded) },
                    modifier = Modifier
                        .menuAnchor(MenuAnchorType.PrimaryNotEditable, true)
                        .fillMaxWidth()
                )
                ExposedDropdownMenu(
                    expanded = accountDropdownExpanded,
                    onDismissRequest = { accountDropdownExpanded = false },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    DropdownMenuItem(
                        text = { Text("Sin cuenta") },
                        onClick = {
                            selectedAccountId = null
                            accountDropdownExpanded = false
                        }
                    )
                    accounts.forEach { account ->
                        DropdownMenuItem(
                            text = { Text(account.name) },
                            onClick = {
                                selectedAccountId = account.id
                                accountDropdownExpanded = false
                            }
                        )
                    }
                    // se avisa el limite del plan Free antes de abrir el formulario, no despues de llenarlo.
                    DropdownMenuItem(
                        text = { Text("+ Añadir nueva cuenta...", color = MaterialTheme.colorScheme.primary) },
                        onClick = {
                            accountDropdownExpanded = false
                            if (!PremiumLimits.canAddAccount(context, accounts.size)) {
                                Toast.makeText(
                                    context,
                                    "Alcanzaste el límite de ${PremiumLimits.FREE_MAX_ACCOUNTS} cuentas del plan Free. Pasate a Premium para cuentas ilimitadas.",
                                    Toast.LENGTH_LONG
                                ).show()
                            } else {
                                navController.navigate(Routes.ADD_ACCOUNT)
                            }
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
                    // el usuario siempre escribe un monto positivo; el signo lo decide el selector Gasto/Ingreso.
                    val enteredAmount = rawAmount.toDoubleOrNull() ?: 0.0
                    val signedAmount = if (isExpense) -enteredAmount else enteredAmount

                    // se realizan las validaciones.
                    when {
                        title.isBlank() -> {
                            errorMessage = "La descripción no puede estar vacía."
                            showError = true
                        }
                        enteredAmount == 0.0 -> {
                            errorMessage = "El monto no puede ser cero."
                            showError = true
                        }
                        else -> {
                            showError = false

                            // se inicia una corutina para realizar las operaciones.
                            coroutineScope.launch {
                                // se agrega o actualiza la transaccion a traves del viewmodel.
                                if (isEditing) {
                                    transactionViewModel.updateTransaction(
                                        id = transactionId!!,
                                        title = title,
                                        amount = signedAmount,
                                        date = transactionDate,
                                        categoryId = selectedCategoryId,
                                        accountId = selectedAccountId
                                    )
                                } else {
                                    transactionViewModel.addTransaction(
                                        title = title,
                                        amount = signedAmount,
                                        date = transactionDate,
                                        categoryId = selectedCategoryId,
                                        accountId = selectedAccountId
                                    )
                                }

                                // logica para revisar y mostrar alertas sobre el presupuesto.
                                val currentExpenseAmount = enteredAmount
                                if (isExpense && selectedCategory != null) {
                                    val monthYearStr = YearMonth.now().format(DateTimeFormatter.ofPattern("yyyy-MM"))
                                    // se obtiene el presupuesto para la categoria y mes actuales.
                                    val budgetEntity = budgetViewModel.getBudgetForCategory(selectedCategory.id, monthYearStr).firstOrNull()

                                    if (budgetEntity != null && budgetEntity.amount > 0) {
                                        // se calcula el gasto total en la categoria despues de esta transaccion.
                                        // si se esta editando, se excluye la version anterior de esta misma
                                        // transaccion para no contarla dos veces.
                                        val previousTransactions = transactionViewModel.transactions.firstOrNull() ?: emptyList()
                                        val spentBeforeThisTransaction = previousTransactions
                                            .filter {
                                                it.categoryId == selectedCategory.id &&
                                                        it.date.startsWith(monthYearStr) &&
                                                        it.amount < 0 &&
                                                        it.id != transactionId
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
                Text(if (isEditing) "Guardar Cambios" else "Guardar")
            }
            
            // Banner AdMob (solo usuarios free)
            val isPremium by PremiumManager.getInstance(context).isPremium.collectAsState()
            if (!isPremium) {
                Spacer(modifier = Modifier.height(16.dp))
                AdBanner()
            }
        }
    }
}