// ManageAccountsScreen

package com.uaa.misgastosapp.ui

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.uaa.misgastosapp.Routes
import com.uaa.misgastosapp.data.PremiumLimits
import com.uaa.misgastosapp.model.Account
import com.uaa.misgastosapp.ui.viewmodel.AccountViewModel
import com.uaa.misgastosapp.ui.viewmodel.TransactionViewModel
import com.uaa.misgastosapp.utils.Result
import java.text.NumberFormat
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ManageAccountsScreen(
    navController: NavController,
    accountViewModel: AccountViewModel = viewModel(),
    transactionViewModel: TransactionViewModel = viewModel()
) {
    val accounts by accountViewModel.accounts.collectAsState()
    val isLoading by accountViewModel.isLoading.collectAsState()
    val transactions by transactionViewModel.transactions.collectAsState()
    val operationStatus by accountViewModel.operationStatus.collectAsState()
    val context = LocalContext.current

    var accountToDelete by remember { mutableStateOf<Account?>(null) }
    var showDeleteDialog by rememberSaveable { mutableStateOf(false) }

    LaunchedEffect(operationStatus) {
        when (val status = operationStatus) {
            is Result.Success -> {
                Toast.makeText(context, status.data, Toast.LENGTH_SHORT).show()
                accountViewModel.clearOperationStatus()
            }
            is Result.Error -> {
                Toast.makeText(context, status.message, Toast.LENGTH_LONG).show()
                accountViewModel.clearOperationStatus()
            }
            is Result.Loading -> {}
            null -> {}
        }
    }

    // cuanto suman las transacciones sin ninguna cuenta asignada: sigue siendo la bolsa general.
    val unassignedBalance = transactions.filter { it.accountId == null }.sumOf { it.amount }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Mis Cuentas") },
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
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = {
                    // se avisa el limite del plan Free antes de abrir el formulario, no despues
                    // de llenarlo.
                    if (!PremiumLimits.canAddAccount(context, accounts.size)) {
                        Toast.makeText(
                            context,
                            "Alcanzaste el límite de ${PremiumLimits.FREE_MAX_ACCOUNTS} cuentas del plan Free. Pasate a Premium para cuentas ilimitadas.",
                            Toast.LENGTH_LONG
                        ).show()
                    } else {
                        navController.navigate(Routes.ADD_ACCOUNT)
                    }
                },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                shape = CircleShape
            ) {
                Icon(Icons.Filled.Add, contentDescription = "Añadir Cuenta")
            }
        }
    ) { paddingValues ->
        Column(modifier = Modifier.padding(paddingValues).padding(16.dp)) {
            Text(
                "Separar tus transacciones por cuenta es opcional. Lo que no asignes a " +
                    "ninguna cuenta sigue contando para el balance general de Inicio.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.outline
            )
            Spacer(modifier = Modifier.height(16.dp))

            if (isLoading) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            } else if (accounts.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("No creaste ninguna cuenta todavía. Usá el botón + si querés separar tus gastos por banco.")
                }
            } else {
                LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    item {
                        UnassignedBalanceItem(balance = unassignedBalance)
                    }
                    items(accounts, key = { it.id }) { account ->
                        val accountBalance = transactions.filter { it.accountId == account.id }.sumOf { it.amount }
                        AccountListItem(
                            account = account,
                            balance = accountBalance,
                            onEdit = { navController.navigate("${Routes.ADD_ACCOUNT}/${account.id}") },
                            onDelete = {
                                accountToDelete = account
                                showDeleteDialog = true
                            }
                        )
                    }
                }
            }
        }
    }

    if (showDeleteDialog && accountToDelete != null) {
        AlertDialog(
            onDismissRequest = {
                showDeleteDialog = false
                accountToDelete = null
            },
            title = { Text("Eliminar Cuenta") },
            text = {
                Text(
                    "¿Eliminar la cuenta '${accountToDelete?.name}'? Sus transacciones no se " +
                        "borran, solo quedan sin cuenta asignada."
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    accountToDelete?.let { accountViewModel.deleteAccount(it) }
                    showDeleteDialog = false
                    accountToDelete = null
                }) {
                    Text("Eliminar", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = {
                    showDeleteDialog = false
                    accountToDelete = null
                }) {
                    Text("Cancelar")
                }
            }
        )
    }
}

@Composable
private fun UnassignedBalanceItem(balance: Double) {
    val currencyFormat = NumberFormat.getCurrencyInstance(Locale("es", "PY")).apply { maximumFractionDigits = 0 }
    Card(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                Text("Sin cuenta asignada", style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.SemiBold)
                Text(
                    "Transacciones que no elegiste asociar a ninguna cuenta",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.outline
                )
            }
            Text(currencyFormat.format(balance), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun AccountListItem(account: Account, balance: Double, onEdit: () -> Unit, onDelete: () -> Unit) {
    val currencyFormat = NumberFormat.getCurrencyInstance(Locale("es", "PY")).apply { maximumFractionDigits = 0 }
    val accountColor = Color(android.graphics.Color.parseColor(account.colorHex))
    Card(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                Box(
                    modifier = Modifier
                        .size(14.dp)
                        .clip(CircleShape)
                        .background(accountColor)
                )
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(account.name, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.SemiBold)
                    if (!account.bankName.isNullOrBlank()) {
                        Text(account.bankName, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.outline)
                    }
                }
            }
            Text(currencyFormat.format(balance), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            IconButton(onClick = onEdit) {
                Icon(Icons.Filled.Edit, contentDescription = "Editar Cuenta")
            }
            IconButton(onClick = onDelete) {
                Icon(Icons.Filled.Delete, contentDescription = "Eliminar Cuenta")
            }
        }
    }
}
