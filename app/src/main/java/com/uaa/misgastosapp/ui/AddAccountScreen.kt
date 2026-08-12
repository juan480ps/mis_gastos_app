// AddAccountScreen

package com.uaa.misgastosapp.ui

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.uaa.misgastosapp.data.PremiumLimits
import com.uaa.misgastosapp.ui.viewmodel.AccountViewModel
import com.uaa.misgastosapp.utils.Result

// paleta acotada para identificar cuentas visualmente; no depende del tema elegido en Temas
// porque cada cuenta necesita su propio color, independiente del color general de la app.
// cada color lleva un nombre en español para que TalkBack pueda anunciarlo (ver contentDescription
// mas abajo); antes los swatches no tenian ninguna etiqueta accesible.
val ACCOUNT_COLOR_PALETTE = listOf(
    "#2563EB" to "Azul",
    "#16A34A" to "Verde",
    "#DC2626" to "Rojo",
    "#9333EA" to "Violeta",
    "#0D9488" to "Verde azulado",
    "#EA580C" to "Naranja",
    "#DB2777" to "Rosa",
    "#525252" to "Gris"
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddAccountScreen(
    navController: NavController,
    accountId: Int? = null,
    accountViewModel: AccountViewModel = viewModel()
) {
    var accountName by remember { mutableStateOf("") }
    var bankName by remember { mutableStateOf("") }
    var selectedColor by remember { mutableStateOf(ACCOUNT_COLOR_PALETTE.first().first) }
    // evita que la carga inicial de datos (modo edicion) pise lo que el usuario ya empezo a escribir.
    var loadedExistingAccount by remember { mutableStateOf(false) }
    val context = LocalContext.current
    val operationStatus by accountViewModel.operationStatus.collectAsState()
    val accounts by accountViewModel.accounts.collectAsState()
    val isEditing = accountId != null

    // en modo edicion, precarga los datos de la cuenta existente apenas estan disponibles.
    LaunchedEffect(accountId, accounts) {
        if (accountId != null && !loadedExistingAccount) {
            accounts.find { it.id == accountId }?.let { existing ->
                accountName = existing.name
                bankName = existing.bankName ?: ""
                selectedColor = existing.colorHex
                loadedExistingAccount = true
            }
        }
    }

    LaunchedEffect(operationStatus) {
        when (val status = operationStatus) {
            is Result.Success -> {
                Toast.makeText(context, status.data, Toast.LENGTH_SHORT).show()
                accountViewModel.clearOperationStatus()
                // se le pasa el id de la cuenta recien creada a la pantalla anterior, para que
                // pueda dejarla seleccionada en su combo en vez de quedar en "Sin cuenta".
                // en modo edicion no hay id nuevo (lastCreatedAccountId sigue en null), asi que
                // esto no pisa la seleccion existente en la pantalla anterior.
                if (!isEditing) {
                    navController.previousBackStackEntry?.savedStateHandle?.set(
                        "newAccountId", accountViewModel.lastCreatedAccountId.value
                    )
                }
                navController.popBackStack()
            }
            is Result.Error -> {
                Toast.makeText(context, status.message, Toast.LENGTH_LONG).show()
                accountViewModel.clearOperationStatus()
            }
            is Result.Loading -> {}
            null -> {}
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (isEditing) "Editar Cuenta" else "Nueva Cuenta") },
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
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .padding(paddingValues)
                .padding(16.dp)
                .fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                "Separar tus gastos por cuenta es opcional. Podés seguir agregando transacciones " +
                    "sin elegir ninguna cuenta y se ven todas juntas como siempre.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.outline
            )

            OutlinedTextField(
                value = accountName,
                onValueChange = { accountName = it },
                label = { Text("Nombre de la cuenta") },
                placeholder = { Text("Ej: Ahorros Itaú") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences)
            )

            OutlinedTextField(
                value = bankName,
                onValueChange = { bankName = it },
                label = { Text("Banco (opcional)") },
                placeholder = { Text("Ej: Itaú, efectivo, billetera...") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences)
            )

            Text("Color", style = MaterialTheme.typography.labelLarge)
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                ACCOUNT_COLOR_PALETTE.forEach { (hex, colorName) ->
                    val color = Color(android.graphics.Color.parseColor(hex))
                    val isSelected = selectedColor == hex
                    Box(
                        modifier = Modifier
                            // minimo recomendado de 48dp para un elemento tactil (antes 36dp).
                            .size(48.dp)
                            .clip(CircleShape)
                            .background(color)
                            .border(
                                width = if (isSelected) 3.dp else 0.dp,
                                color = MaterialTheme.colorScheme.onSurface,
                                shape = CircleShape
                            )
                            .clickable { selectedColor = hex }
                            .semantics {
                                // el swatch no tenia ninguna etiqueta accesible antes: TalkBack lo
                                // anunciaba como un elemento sin nombre.
                                contentDescription = if (isSelected) "$colorName, seleccionado" else colorName
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        if (isSelected) {
                            Icon(Icons.Filled.Check, contentDescription = null, tint = Color.White)
                        }
                    }
                }
            }

            Button(
                onClick = {
                    // el limite de cuentas del plan Free solo aplica al crear una nueva, no al editar.
                    if (accountName.isBlank()) {
                        Toast.makeText(context, "El nombre no puede estar vacío", Toast.LENGTH_SHORT).show()
                    } else if (!isEditing && !PremiumLimits.canAddAccount(context, accounts.size)) {
                        Toast.makeText(
                            context,
                            "Alcanzaste el límite de ${PremiumLimits.FREE_MAX_ACCOUNTS} cuentas del plan Free. Pasate a Premium para cuentas ilimitadas.",
                            Toast.LENGTH_LONG
                        ).show()
                    } else if (isEditing) {
                        accountViewModel.updateAccount(accountId!!, accountName, bankName, selectedColor)
                    } else {
                        accountViewModel.addAccount(accountName, bankName, selectedColor)
                    }
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(if (isEditing) "Guardar Cambios" else "Guardar Cuenta")
            }
        }
    }
}
