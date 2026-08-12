// AddEditRecurringTransactionScreen
package com.uaa.misgastosapp.ui

import android.app.DatePickerDialog
import android.os.Build
import android.widget.DatePicker
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material3.*
import androidx.compose.material3.MenuAnchorType
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.uaa.misgastosapp.Routes
import com.uaa.misgastosapp.data.PremiumLimits
import com.uaa.misgastosapp.data.RecurrenceType
import com.uaa.misgastosapp.model.Category
import com.uaa.misgastosapp.ui.components.ThousandsSeparatorVisualTransformation
import com.uaa.misgastosapp.ui.viewmodel.CategoryViewModel
import com.uaa.misgastosapp.ui.viewmodel.RecurringTransactionViewModel
import com.uaa.misgastosapp.utils.Result
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.util.*

// se asegura que el codigo use apis disponibles a partir de android oreo.
@RequiresApi(Build.VERSION_CODES.O)
// se usa esta anotacion para poder utilizar componentes de material 3 que aun son experimentales.
@OptIn(ExperimentalMaterial3Api::class)
// aca se define la pantalla (composable) para añadir o editar una transaccion recurrente.
@Composable
fun AddEditRecurringTransactionScreen(
    navController: NavController,
    recurringTransactionId: Int? = null,
    recurringViewModel: RecurringTransactionViewModel = viewModel(),
    categoryViewModel: CategoryViewModel = viewModel()
) {
    // se obtienen instancias y se declaran los estados para los campos del formulario.
    val context = LocalContext.current
    var title by remember { mutableStateOf("") }
    // solo digitos; el separador de miles se agrega al mostrarlo via ThousandsSeparatorVisualTransformation.
    var rawAmount by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf<Category?>(null) }
    var categoryDropdownExpanded by remember { mutableStateOf(false) }
    val categories by categoryViewModel.categories.collectAsState()
    val recurringTransactions by recurringViewModel.recurringTransactions.collectAsState()
    var recurrenceType by remember { mutableStateOf(RecurrenceType.MONTHLY) }
    var dayOfMonth by remember { mutableStateOf(LocalDate.now().dayOfMonth.toString()) }
    var startDate by remember { mutableStateOf(LocalDate.now()) }
    var endDate by remember { mutableStateOf<LocalDate?>(null) }
    var isActive by remember { mutableStateOf(true) }
    var screenTitle by remember { mutableStateOf("Añadir Recurrente") }
    // se configuran los formatos para fechas y numeros.
    val dateFormat = DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM).withLocale(Locale("es", "ES"))
    val operationStatus by recurringViewModel.operationStatus.collectAsState()

    // al volver de "+ Añadir nueva categoría...", la que se acaba de crear queda seleccionada
    // automaticamente, igual que en Agregar Transacción.
    val savedStateHandle = navController.currentBackStackEntry?.savedStateHandle
    val newCategoryId = savedStateHandle?.getStateFlow<Int?>("newCategoryId", null)?.collectAsState()
    LaunchedEffect(newCategoryId?.value, categories) {
        newCategoryId?.value?.let { id ->
            categories.find { it.id == id }?.let { found ->
                selectedCategory = found
                savedStateHandle?.remove<Int?>("newCategoryId")
            }
        }
    }

    // se observa el resultado de guardar: exito muestra el toast y vuelve atras, error solo avisa.
    LaunchedEffect(operationStatus) {
        when (val status = operationStatus) {
            is Result.Success -> {
                Toast.makeText(context, status.data, Toast.LENGTH_SHORT).show()
                recurringViewModel.clearOperationStatus()
                navController.popBackStack()
            }
            is Result.Error -> {
                Toast.makeText(context, status.message, Toast.LENGTH_LONG).show()
                recurringViewModel.clearOperationStatus()
            }
            is Result.Loading -> {}
            null -> {}
        }
    }

    // este efecto se ejecuta solo cuando 'recurringtransactionid' cambia.
    // se usa para cargar los datos de una transaccion existente cuando se entra en modo de edicion.
    LaunchedEffect(key1 = recurringTransactionId) {
        if (recurringTransactionId != null) {
            screenTitle = "Editar Recurrente"
            recurringViewModel.getRecurringTransactionById(recurringTransactionId) { entity ->
                entity?.let {
                    // se llenan los estados del formulario con los datos de la entidad cargada.
                    title = it.title
                    rawAmount = it.amount.toString().replace(".0", "")
                    it.categoryId?.let { catId -> selectedCategory = categories.find { c -> c.id == catId } }
                    recurrenceType = it.recurrenceType
                    dayOfMonth = it.dayOfMonth.toString()
                    startDate = LocalDate.parse(it.startDate)
                    endDate = it.endDate?.let { LocalDate.parse(it) }
                    isActive = it.isActive
                }
            }
        }
    }

    // se usa el componente scaffold para la estructura de la pantalla.
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(screenTitle) },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = Color.White,
                    actionIconContentColor = Color.White,
                    navigationIconContentColor = Color.White
                ),
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Volver")
                    }
                }
            )
        }
    ) { paddingValues ->
        // se usa una columna con scroll para el contenido del formulario.
        Column(
            modifier = Modifier
                .padding(paddingValues)
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // campo de texto para el titulo.
            OutlinedTextField(
                value = title,
                onValueChange = { title = it },
                label = { Text("Título") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences)
            )
            // campo de texto para el monto: el texto real son solo digitos, el separador de
            // miles es puramente visual para que el cursor no salte mientras se escribe.
            OutlinedTextField(
                value = rawAmount,
                onValueChange = { input -> rawAmount = input.filter { it.isDigit() } },
                label = { Text("Monto (PYG)") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                visualTransformation = ThousandsSeparatorVisualTransformation()
            )
            // menu desplegable para seleccionar la categoria.
            ExposedDropdownMenuBox(
                expanded = categoryDropdownExpanded,
                onExpandedChange = { categoryDropdownExpanded = !categoryDropdownExpanded }
            ) {
                OutlinedTextField(
                    value = selectedCategory?.name ?: "Seleccionar Categoría",
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Categoría") },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = categoryDropdownExpanded) },
                    modifier = Modifier.menuAnchor(MenuAnchorType.PrimaryNotEditable, true).fillMaxWidth()
                )
                ExposedDropdownMenu(
                    expanded = categoryDropdownExpanded,
                    onDismissRequest = { categoryDropdownExpanded = false }
                ) {
                    DropdownMenuItem(text = { Text("Sin Categoría") }, onClick = {
                        selectedCategory = null
                        categoryDropdownExpanded = false
                    })
                    categories.forEach { category ->
                        DropdownMenuItem(text = { Text(category.name) }, onClick = {
                            selectedCategory = category
                            categoryDropdownExpanded = false
                        })
                    }
                    // opcion para navegar a la pantalla de añadir nueva categoria, igual que en
                    // Agregar Transacción. se avisa el limite del plan Free antes de abrir el
                    // formulario, no despues de llenarlo.
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
            // texto informativo sobre el tipo de recurrencia.
            Text("Tipo de Recurrencia: Mensual", style = MaterialTheme.typography.bodyLarge)
            // campo de texto para el dia del mes.
            OutlinedTextField(
                value = dayOfMonth,
                onValueChange = { dayOfMonth = it.filter { c -> c.isDigit() }.take(2) },
                label = { Text("Día del Mes (1-31)") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )
            // campo para seleccionar la fecha de inicio.
            DatePickerField(
                label = "Fecha de Inicio",
                selectedDate = startDate,
                onDateSelected = { startDate = it },
                dateFormat = dateFormat
            )
            // campo para seleccionar la fecha de fin (opcional).
            DatePickerField(
                label = "Fecha de Fin (Opcional)",
                selectedDate = endDate,
                onDateSelected = { endDate = it },
                dateFormat = dateFormat,
                isOptional = true,
                onClearDate = { endDate = null }
            )
            // checkbox para marcar si la transaccion esta activa.
            Row(verticalAlignment = Alignment.CenterVertically) {
                Checkbox(checked = isActive, onCheckedChange = { isActive = it })
                Text("Activa")
            }
            // boton para guardar los cambios.
            Button(
                onClick = {
                    val finalAmount = rawAmount.toDoubleOrNull()
                    val finalDayOfMonth = dayOfMonth.toIntOrNull()
                    if (finalAmount == null || finalDayOfMonth == null) {
                        Toast.makeText(context, "Monto o día del mes inválido.", Toast.LENGTH_SHORT).show()
                        return@Button
                    }
                    // el limite de recurrentes del plan Free solo aplica al crear una nueva, no al editar.
                    if (recurringTransactionId == null &&
                        !PremiumLimits.canAddRecurringTransaction(context, recurringTransactions.size)
                    ) {
                        Toast.makeText(
                            context,
                            "Alcanzaste el límite de ${PremiumLimits.FREE_MAX_RECURRING_TRANSACTIONS} recurrentes del plan Free. Pasate a Premium para recurrentes ilimitadas.",
                            Toast.LENGTH_LONG
                        ).show()
                        return@Button
                    }
                    // se llama al viewmodel para guardar o actualizar la transaccion; el resultado
                    // se maneja en el LaunchedEffect que observa operationStatus.
                    recurringViewModel.addOrUpdateRecurringTransaction(
                        id = recurringTransactionId,
                        title = title,
                        amount = finalAmount,
                        categoryId = selectedCategory?.id,
                        recurrenceType = recurrenceType,
                        dayOfMonth = finalDayOfMonth,
                        startDate = startDate,
                        endDate = endDate,
                        isActive = isActive
                    )
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(if (recurringTransactionId == null) "Añadir Recurrente" else "Actualizar Recurrente")
            }
        }
    }
}

// se asegura que el codigo use apis disponibles a partir de android oreo.
@RequiresApi(Build.VERSION_CODES.O)
// este es un composable reutilizable para campos de seleccion de fecha.
@Composable
fun DatePickerField(
    label: String,
    selectedDate: LocalDate?,
    onDateSelected: (LocalDate) -> Unit,
    dateFormat: DateTimeFormatter,
    isOptional: Boolean = false,
    onClearDate: (() -> Unit)? = null
) {
    val context = LocalContext.current
    val calendar = Calendar.getInstance()
    // se configura el calendario con la fecha seleccionada, si existe.
    selectedDate?.let {
        calendar.set(it.year, it.monthValue - 1, it.dayOfMonth)
    }
    // se crea el dialogo de seleccion de fecha.
    val datePickerDialog = DatePickerDialog(
        context,
        { _: DatePicker, year: Int, month: Int, day: Int ->
            onDateSelected(LocalDate.of(year, month + 1, day))
        },
        calendar.get(Calendar.YEAR),
        calendar.get(Calendar.MONTH),
        calendar.get(Calendar.DAY_OF_MONTH)
    )
    // se muestra el campo de texto que al hacer clic, abre el dialogo.
    OutlinedTextField(
        value = selectedDate?.format(dateFormat) ?: if (isOptional) "Sin fecha" else "",
        onValueChange = {},
        label = { Text(label) },
        readOnly = true,
        modifier = Modifier.fillMaxWidth().clickable { datePickerDialog.show() },
        trailingIcon = {
            Row {
                // si es opcional y hay una fecha, se muestra un boton para limpiar.
                if (isOptional && selectedDate != null && onClearDate != null) {
                    IconButton(onClick = {
                        onClearDate()
                        datePickerDialog.dismiss()
                    }) {
                        Icon(Icons.Filled.Clear, "Limpiar fecha")
                    }
                }
                // el icono principal para abrir el selector de fecha.
                IconButton(onClick = { datePickerDialog.show() }) {
                    Icon(Icons.Filled.DateRange, "Seleccionar fecha")
                }
            }
        }
    )
}