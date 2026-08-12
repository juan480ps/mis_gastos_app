// HomeScreen

package com.uaa.misgastosapp.ui
import android.annotation.SuppressLint
import android.os.Build
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.*
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.uaa.misgastosapp.BuildConfig
import com.uaa.misgastosapp.Routes
import com.uaa.misgastosapp.ui.components.AdBanner
import com.uaa.misgastosapp.ui.components.AppBottomNavBar
import com.uaa.misgastosapp.data.PremiumManager
import com.uaa.misgastosapp.model.Budget
import com.uaa.misgastosapp.model.Transaction
import com.uaa.misgastosapp.ui.viewmodel.AccountViewModel
import com.uaa.misgastosapp.ui.viewmodel.AuthViewModel
import com.uaa.misgastosapp.ui.viewmodel.BudgetViewModel
import com.uaa.misgastosapp.ui.viewmodel.RecurringTransactionViewModel
import com.uaa.misgastosapp.ui.viewmodel.TransactionViewModel
import com.uaa.misgastosapp.utils.Result
import java.text.NumberFormat
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.util.*
import java.util.Locale

// se suprime una advertencia sobre la indentacion que puede ser un falso positivo.
@SuppressLint("SuspiciousIndentation")
// se asegura que el codigo use apis disponibles a partir de android oreo.
@RequiresApi(Build.VERSION_CODES.O)
// se usa esta anotacion para poder utilizar componentes de material 3 que aun son experimentales.
@OptIn(ExperimentalMaterial3Api::class)
// aca se define la pantalla principal (composable) de la aplicacion.
@Composable
fun HomeScreen(
    navController: NavController,
    transactionViewModel: TransactionViewModel = viewModel(),
    budgetViewModel: BudgetViewModel = viewModel(),
    recurringTransactionViewModel: RecurringTransactionViewModel = viewModel(),
    authViewModel: AuthViewModel = viewModel(),
    accountViewModel: AccountViewModel = viewModel()
) {
    // se obtienen los estados desde los diferentes viewmodels.
    val allTransactions by transactionViewModel.transactions.collectAsState()
    val operationStatus by transactionViewModel.operationStatus.collectAsState()
    // filtro opcional por cuenta: por defecto (null = "Todas las cuentas") el balance y la lista
    // se comportan exactamente como antes, sumando todo en una sola bolsa.
    // se guarda solo el id (rememberSaveable) en vez del objeto Account: como Home es el destino
    // inicial, sigue en el backstack mientras se navega a otras pantallas, asi que este estado
    // sobrevive el viaje de ida y vuelta (antes, con 'remember' comun, se perdia al volver).
    val accounts by accountViewModel.accounts.collectAsState()
    var accountFilterId by rememberSaveable { mutableStateOf<Int?>(null) }
    val accountFilter = accounts.find { it.id == accountFilterId }
    var accountFilterExpanded by remember { mutableStateOf(false) }
    val transactions = if (accountFilter == null) allTransactions else allTransactions.filter { it.accountId == accountFilter?.id }
    val budgetsWithSpending by budgetViewModel.budgetsWithSpendingForCurrentMonth.collectAsState(initial = emptyList())
    val currentYearMonth by budgetViewModel.currentMonthYear.collectAsState()
    // se configuran formatos de moneda y fecha.
    val currencyFormat = NumberFormat.getCurrencyInstance(Locale("es", "PY")).apply {
        maximumFractionDigits = 0
    }
    val monthDisplayFormatter = DateTimeFormatter.ofPattern("MMMM yyyy", Locale("es", "ES"))
    val monthHeaderFormatter = DateTimeFormatter.ofPattern("MMMM 'de' yyyy", Locale("es", "ES"))
    // se definen estados para manejar dialogos y la visibilidad de las transacciones.
    var showLogoutDialog by rememberSaveable { mutableStateOf(false) }
    // por defecto, los meses anteriores al actual arrancan colapsados y el mes actual expandido;
    // si el usuario toca el encabezado de un mes para cambiarlo, esa eleccion se recuerda y
    // tiene prioridad sobre el default (para ambos sentidos: expandir un mes viejo o colapsar
    // el mes actual), incluso si mas tarde aparecen transacciones nuevas en otros meses.
    var userExpandedMonths by rememberSaveable { mutableStateOf(emptySet<YearMonth>()) }
    var userCollapsedMonths by rememberSaveable { mutableStateOf(emptySet<YearMonth>()) }
    // el resumen de presupuestos tambien se puede colapsar, y se recuerda la eleccion.
    var budgetSummaryCollapsed by rememberSaveable { mutableStateOf(false) }
    fun isMonthCollapsed(yearMonth: YearMonth): Boolean = when {
        yearMonth in userExpandedMonths -> false
        yearMonth in userCollapsedMonths -> true
        else -> yearMonth != YearMonth.now()
    }

    var transactionToDelete by remember { mutableStateOf<Transaction?>(null) }
    var showDeleteTransactionDialog by rememberSaveable { mutableStateOf(false) }

    val isLoggedIn by authViewModel.isLoggedIn.collectAsState()
    val userName = if (isLoggedIn) authViewModel.getCurrentUserName() ?: "Usuario" else "Invitado"
    val context = LocalContext.current

    // se observa el estado de las operaciones para mostrar mensajes.
    LaunchedEffect(operationStatus) {
        val status = operationStatus
        when (status) {
            is Result.Success -> {
                Toast.makeText(context, status.data, Toast.LENGTH_SHORT).show()
                transactionViewModel.clearOperationStatus()
            }
            is Result.Error -> {
                Toast.makeText(context, status.message, Toast.LENGTH_LONG).show()
                transactionViewModel.clearOperationStatus()
            }
            else -> {}
        }
    }

    // se usa el componente scaffold para la estructura de la pantalla.
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Hola, $userName") },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = Color.White,
                    actionIconContentColor = Color.White
                ),
                actions = {
                    // Botón Exportar (solo Premium)
                    val isPremium by PremiumManager.getInstance(context).isPremium.collectAsState()
                    if (isPremium) {
                        IconButton(onClick = { navController.navigate(Routes.EXPORT) }) {
                            Icon(
                                Icons.Default.FileDownload,
                                contentDescription = "Exportar",
                                tint = Color.White
                            )
                        }
                    }
                    // Botón Temas: visible siempre, ThemesScreen ya bloquea los temas premium si no corresponde.
                    IconButton(onClick = { navController.navigate(Routes.THEMES) }) {
                        Icon(
                            Icons.Default.Palette,
                            contentDescription = "Temas",
                            tint = Color.White
                        )
                    }
                    // Botón Cuentas: separar gastos por banco es opcional, se accede desde acá.
                    IconButton(onClick = { navController.navigate(Routes.MANAGE_ACCOUNTS) }) {
                        Icon(
                            Icons.Default.AccountBalance,
                            contentDescription = "Cuentas",
                            tint = Color.White
                        )
                    }
                    // Botón Ayuda: vuelve a mostrar la pantalla de bienvenida que explica la app.
                    IconButton(onClick = { navController.navigate(Routes.ONBOARDING) }) {
                        Icon(
                            Icons.AutoMirrored.Filled.HelpOutline,
                            contentDescription = "Ayuda",
                            tint = Color.White
                        )
                    }
                    // Botón Premium: se oculta al ser premium (ya no hace falta el CTA de compra),
                    // excepto en debug, donde siempre queda visible para poder volver a apagar el
                    // toggle de simulacion en PremiumScreen (si no, se pierde el unico acceso a esa
                    // pantalla en cuanto se activa el toggle).
                    if (!isPremium || BuildConfig.DEBUG) {
                        IconButton(onClick = { navController.navigate(Routes.PREMIUM) }) {
                            Icon(
                                Icons.Default.Star,
                                contentDescription = "Premium",
                                tint = Color(0xFFFFD700)
                            )
                        }
                    }
                    IconButton(onClick = { showLogoutDialog = true }) {
                        if (isLoggedIn) {
                            Icon(Icons.AutoMirrored.Filled.ExitToApp, contentDescription = "Cerrar Sesión")
                        } else {
                            Icon(Icons.Default.Person, contentDescription = "Iniciar Sesión")
                        }
                    }
                }
            )
        },
        bottomBar = { AppBottomNavBar(navController, Routes.HOME) },
        floatingActionButton = {
            // boton flotante para añadir nuevas transacciones.
            FloatingActionButton(
                onClick = {
                    // si hay una cuenta especifica filtrada (no "Todas las cuentas"), la nueva
                    // transaccion la trae preseleccionada en vez de arrancar en "Sin cuenta".
                    val route = if (accountFilterId != null) {
                        "${Routes.ADD_TRANSACTION}?${Routes.ARG_PRESELECTED_ACCOUNT_ID}=$accountFilterId"
                    } else {
                        Routes.ADD_TRANSACTION
                    }
                    navController.navigate(route)
                },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                shape = CircleShape
            ) {
                Icon(Icons.Default.Add, contentDescription = "Agregar Gasto")
            }
        }
    ) { padding ->
        // Banner de AdMob para usuarios free
        val isPremium by PremiumManager.getInstance(context).isPremium.collectAsState()
        
        Column(modifier = Modifier.padding(padding)) {
            // Banner AdMob (solo para usuarios free)
            if (!isPremium) {
                AdBanner(
                    modifier = Modifier.fillMaxWidth()
                )
            }

        // se usa una 'lazycolumn' para mostrar el contenido principal de forma eficiente.
        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .padding(horizontal = 16.dp, vertical = 8.dp)
        ) {
            // selector opcional de cuenta: solo aparece si el usuario creo alguna cuenta.
            // "Todas las cuentas" (por defecto) mantiene el balance/lista de siempre, sin filtrar.
            if (accounts.isNotEmpty()) {
                item {
                    Box(modifier = Modifier.padding(bottom = 4.dp)) {
                        TextButton(onClick = { accountFilterExpanded = true }) {
                            Icon(Icons.Default.AccountBalance, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(accountFilter?.name ?: "Todas las cuentas")
                            Icon(Icons.Filled.KeyboardArrowDown, contentDescription = null)
                        }
                        DropdownMenu(
                            expanded = accountFilterExpanded,
                            onDismissRequest = { accountFilterExpanded = false }
                        ) {
                            DropdownMenuItem(
                                text = { Text("Todas las cuentas") },
                                onClick = {
                                    accountFilterId = null
                                    accountFilterExpanded = false
                                }
                            )
                            accounts.forEach { account ->
                                DropdownMenuItem(
                                    text = { Text(account.name) },
                                    onClick = {
                                        accountFilterId = account.id
                                        accountFilterExpanded = false
                                    }
                                )
                            }
                        }
                    }
                }
            }

            // se muestra una tarjeta con el resumen del saldo.
            item {
                SummaryCard(balance = transactions.sumOf { it.amount })
            }

            // se muestra un titulo para la seccion de presupuestos, clickeable para colapsarla
            // (igual que los encabezados de mes de las transacciones).
            item {
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { budgetSummaryCollapsed = !budgetSummaryCollapsed }
                        .padding(vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        "Resumen de Presupuestos (${currentYearMonth.format(monthDisplayFormatter).replaceFirstChar { it.uppercase() }})",
                        style = MaterialTheme.typography.titleMedium,
                        modifier = Modifier.weight(1f)
                    )
                    Icon(
                        imageVector = if (budgetSummaryCollapsed) Icons.Filled.KeyboardArrowDown else Icons.Filled.KeyboardArrowUp,
                        contentDescription = if (budgetSummaryCollapsed) "Expandir presupuestos" else "Minimizar presupuestos"
                    )
                }
            }

            // se muestran los presupuestos del mes, salvo que el usuario haya colapsado la seccion.
            if (!budgetSummaryCollapsed) {
                if (budgetsWithSpending.any { it.amount > 0 }) {
                    items(budgetsWithSpending.filter { it.amount > 0 }) { budgetItem ->
                        BudgetStatusItem(budgetItem, currencyFormat)
                    }
                } else {
                    item {
                        Text(
                            "No hay presupuestos configurados para este mes. Ve a 'Gestionar Presupuestos'.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.outline
                        )
                    }
                }
            }

            // se muestra un titulo para las transacciones recientes.
            item {
                Spacer(modifier = Modifier.height(16.dp))
                Text("Transacciones Recientes", style = MaterialTheme.typography.titleMedium)
            }

            // se muestra un indicador de carga si una operacion esta en curso.
            if (operationStatus is Result.Loading) {
                item {
                    Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator()
                    }
                }
            }

            // se muestran las transacciones o un mensaje si no hay ninguna.
            if (transactions.isEmpty()) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 20.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("No hay transacciones registradas.")
                    }
                }
            } else {
                val dateParser = DateTimeFormatter.ofPattern("yyyy-MM-dd")
                // se agrupan las transacciones por mes y año.
                val groupedTransactions = transactions
                    .groupBy {
                        try {
                            YearMonth.from(LocalDate.parse(it.date, dateParser))
                        } catch (e: Exception) {
                            null
                        }
                    }
                    .filterKeys { it != null }
                    .toSortedMap(compareByDescending { it!! })

                // se itera sobre los grupos para mostrar cada mes.
                groupedTransactions.forEach { (yearMonth, monthTransactions) ->
                    // se muestra una cabecera para cada mes, que es clickeable para expandir/colapsar.
                    item {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 16.dp, bottom = 8.dp)
                                .clickable {
                                    // se guarda la eleccion del usuario en el set que corresponda,
                                    // y se la quita del otro para que no quede una preferencia vieja
                                    // contradictoria si el usuario cambia de opinion mas de una vez.
                                    if (isMonthCollapsed(yearMonth!!)) {
                                        userExpandedMonths = userExpandedMonths + yearMonth
                                        userCollapsedMonths = userCollapsedMonths - yearMonth
                                    } else {
                                        userCollapsedMonths = userCollapsedMonths + yearMonth
                                        userExpandedMonths = userExpandedMonths - yearMonth
                                    }
                                },
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = yearMonth!!.format(monthHeaderFormatter)
                                    .replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale("es", "ES")) else it.toString() },
                                style = MaterialTheme.typography.bodyMedium.copy(
                                    fontSize = (MaterialTheme.typography.bodyMedium.fontSize.value - 1).sp,
                                    fontWeight = FontWeight.SemiBold
                                ),
                                color = MaterialTheme.colorScheme.outline,
                                modifier = Modifier.weight(1f)
                            )
                            HorizontalDivider(
                                color = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f),
                                modifier = Modifier.weight(1f)
                            )
                            val isCollapsed = isMonthCollapsed(yearMonth!!)
                            Icon(
                                imageVector = if (isCollapsed) Icons.Filled.KeyboardArrowUp else Icons.Filled.KeyboardArrowDown,
                                contentDescription = if (isCollapsed) "Expandir mes" else "Minimizar mes",
                                tint = MaterialTheme.colorScheme.outline,
                                modifier = Modifier.padding(start = 8.dp)
                            )
                        }
                    }

                    // si el mes no esta colapsado, se muestran sus transacciones.
                    if (!isMonthCollapsed(yearMonth!!)) {
                        items(monthTransactions, key = { it.id }) { tx ->
                            TransactionItem(
                                transaction = tx,
                                onEdit = { navController.navigate("${Routes.ADD_TRANSACTION}?${Routes.ARG_TRANSACTION_ID}=${tx.id}") },
                                onDelete = {
                                    transactionToDelete = tx
                                    showDeleteTransactionDialog = true
                                }
                            )
                        }
                    }
                }
            }
        }
    } // Fin LazyColumn
    } // Fin Column

    // se muestra el dialogo segun el estado de sesion.
    if (showLogoutDialog) {
        if (isLoggedIn) {
            // dialogo para cerrar sesion.
            AlertDialog(
                onDismissRequest = { showLogoutDialog = false },
                title = { Text("Cerrar Sesión") },
                text = { Text("¿Estás seguro de que deseas cerrar sesión?") },
                confirmButton = {
                    TextButton(
                        onClick = {
                            showLogoutDialog = false
                            authViewModel.logout()
                        }
                    ) {
                        Text("Sí")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showLogoutDialog = false }) {
                        Text("Cancelar")
                    }
                }
            )
        } else {
            // dialogo para iniciar sesion (opcional).
            AlertDialog(
                onDismissRequest = { showLogoutDialog = false },
                title = { Text("Iniciar Sesión") },
                text = { Text("¿Deseas iniciar sesión para sincronizar tus datos con la nube?") },
                confirmButton = {
                    TextButton(
                        onClick = {
                            showLogoutDialog = false
                            navController.navigate(Routes.LOGIN)
                        }
                    ) {
                        Text("Iniciar Sesión")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showLogoutDialog = false }) {
                        Text("Usar sin sesión")
                    }
                }
            )
        }
    }

    // se muestra el dialogo de confirmacion para eliminar una transaccion.
    if (showDeleteTransactionDialog && transactionToDelete != null) {
        AlertDialog(
            onDismissRequest = {
                showDeleteTransactionDialog = false
                transactionToDelete = null
            },
            title = { Text("Confirmar Eliminación") },
            text = { Text("¿Estás seguro de que quieres eliminar la transacción '${transactionToDelete?.title}'? Esta acción no se puede deshacer.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        transactionToDelete?.let { transactionViewModel.deleteTransaction(it.id) }
                        showDeleteTransactionDialog = false
                        transactionToDelete = null
                    }
                ) {
                    Text("Eliminar", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        showDeleteTransactionDialog = false
                        transactionToDelete = null
                    }
                ) {
                    Text("Cancelar")
                }
            }
        )
    }
}

// este es un composable reutilizable para mostrar el estado de un presupuesto.
@Composable
fun BudgetStatusItem(budget: Budget, currencyFormat: NumberFormat) {
    // el color de la barra de progreso cambia segun el porcentaje gastado.
    val progressColor = when {
        budget.progress > 1f -> MaterialTheme.colorScheme.error
        budget.progress == 1f -> Color(0xFFFFA000)
        budget.progress > 0.85f -> Color(0xFFFFA000)
        else -> MaterialTheme.colorScheme.primary
    }
    Column(modifier = Modifier.padding(vertical = 4.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(budget.categoryName, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.SemiBold)
            Text(
                "${currencyFormat.format(budget.spentAmount)} / ${currencyFormat.format(budget.amount)}",
                style = MaterialTheme.typography.bodyMedium,
                color = if (budget.spentAmount > budget.amount) progressColor else LocalContentColor.current
            )
        }
        Spacer(modifier = Modifier.height(4.dp))
        // se muestra la barra de progreso lineal.
        LinearProgressIndicator(
            progress = { budget.progress.coerceIn(0f, 1f) },
            modifier = Modifier.fillMaxWidth(),
            color = progressColor,
            trackColor = progressColor.copy(alpha = 0.3f)
        )
        // se muestran mensajes de alerta segun el estado del presupuesto.
        if (budget.progress > 1f) {
            Text(
                "Excedido en ${currencyFormat.format(budget.spentAmount - budget.amount)}",
                style = MaterialTheme.typography.bodySmall,
                color = progressColor,
                modifier = Modifier.align(Alignment.End)
            )
        } else if (budget.progress > 0.85f && budget.progress < 1f) {
            Text(
                "¡Cuidado! Cercano al límite.",
                style = MaterialTheme.typography.bodySmall,
                color = progressColor,
                modifier = Modifier.align(Alignment.End)
            )
        } else if (budget.progress == 1f) {
            Text(
                "¡Cuidado! Límite alcanzado.",
                style = MaterialTheme.typography.bodySmall,
                color = progressColor,
                modifier = Modifier.align(Alignment.End)
            )
        }
    }
}