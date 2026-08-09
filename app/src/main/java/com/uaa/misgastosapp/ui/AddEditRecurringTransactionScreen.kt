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
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material3.*
import androidx.compose.material3.MenuAnchorType
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.uaa.misgastosapp.data.RecurrenceType
import com.uaa.misgastosapp.model.Category
import com.uaa.misgastosapp.ui.viewmodel.CategoryViewModel
import com.uaa.misgastosapp.ui.viewmodel.RecurringTransactionViewModel
import java.text.DecimalFormat
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
    var rawAmount by remember { mutableStateOf("") } // monto sin formato.
    var formattedAmount by remember { mutableStateOf("") } // monto con formato de miles.
    var selectedCategory by remember { mutableStateOf<Category?>(null) }
    var categoryDropdownExpanded by remember { mutableStateOf(false) }
    val categories by categoryViewModel.categories.collectAsState()
    var recurrenceType by remember { mutableStateOf(RecurrenceType.MONTHLY) }
    var dayOfMonth by remember { mutableStateOf(LocalDate.now().dayOfMonth.toString()) }
    var startDate by remember { mutableStateOf(LocalDate.now()) }
    var endDate by remember { mutableStateOf<LocalDate?>(null) }
    var isActive by remember { mutableStateOf(true) }
    var screenTitle by remember { mutableStateOf("Añadir Recurrente") }
    // se configuran los formatos para fechas y numeros.
    val dateFormat = DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM).withLocale(Locale("es", "ES"))
    val decimalFormat = DecimalFormat("#,###")

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
                    formattedAmount = decimalFormat.format(rawAmount.toDouble())
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

    // esta funcion se encarga de actualizar el monto formateado cada vez que el usuario escribe.
    fun updateFormattedAmount(newValue: String) {
        rawAmount = newValue.filter { it.isDigit() }
        formattedAmount = if (rawAmount.isNotEmpty()) {
            try {
                decimalFormat.format(rawAmount.toDouble())
            } catch (e: NumberFormatException) {
                ""
            }
        } else {
            ""
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
                singleLine = true
            )
            // campo de texto para el monto.
            OutlinedTextField(
                value = formattedAmount,
                onValueChange = { updateFormattedAmount(it) },
                label = { Text("Monto (PYG)") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
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
                    // se llama al viewmodel para guardar o actualizar la transaccion.
                    recurringViewModel.addOrUpdateRecurringTransaction(
                        id = recurringTransactionId,
                        title = title,
                        amount = finalAmount,
                        categoryId = selectedCategory?.id,
                        recurrenceType = recurrenceType,
                        dayOfMonth = finalDayOfMonth,
                        startDate = startDate,
                        endDate = endDate,
                        isActive = isActive,
                        onSuccess = {
                            Toast.makeText(context, "Guardado correctamente.", Toast.LENGTH_SHORT).show()
                            navController.popBackStack()
                        },
                        onError = { errorMsg ->
                            Toast.makeText(context, errorMsg, Toast.LENGTH_LONG).show()
                        }
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
                        // aca hay un pequeño error en el icono, deberia ser uno de 'limpiar'.
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Limpiar fecha")
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