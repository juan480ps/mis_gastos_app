// SummaryScreen

package com.uaa.misgastosapp.ui

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.text.font.FontWeight
import java.text.NumberFormat
import java.util.*

// aca se define un composable reutilizable que muestra una tarjeta con el balance total.
@Composable
fun SummaryCard(balance: Double) {
    // se configura el formato de moneda para guaranies.
    val numberFormat = NumberFormat.getCurrencyInstance(Locale("es", "PY")).apply {
        maximumFractionDigits = 0
    }
    // se formatea el balance recibido.
    val formattedBalance = numberFormat.format(balance)
    // ojito de privacidad: oculta el monto (ej. para mirar la app en publico). se guarda en
    // BalanceVisibilityPrefs (no rememberSaveable) para que la preferencia siga aplicada aunque
    // se cierre y reabra la app, no solo al navegar entre pantallas.
    val context = LocalContext.current
    val isBalanceVisible by BalanceVisibilityPrefs.isVisible(context)

    // se usa el componente 'card' para mostrar la informacion.
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(120.dp),
        elevation = CardDefaults.cardElevation(4.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primary)
    ) {
        // se organiza el contenido en una columna vertical.
        Column(
            Modifier
                .padding(16.dp)
                .fillMaxSize(),
            verticalArrangement = Arrangement.Center
        ) {
            // se muestra el titulo "balance total" junto con el ojito para ocultar/mostrar.
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    "Balance Total",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onPrimary
                )
                IconButton(
                    onClick = { BalanceVisibilityPrefs.setVisible(context, !isBalanceVisible) },
                    modifier = Modifier.size(24.dp)
                ) {
                    Icon(
                        if (isBalanceVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                        contentDescription = if (isBalanceVisible) "Ocultar saldo" else "Mostrar saldo",
                        tint = MaterialTheme.colorScheme.onPrimary
                    )
                }
            }
            // se muestra el monto del balance formateado, o un texto enmascarado si esta oculto.
            Text(
                if (isBalanceVisible) formattedBalance else "₲ • • • • • •",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onPrimary
            )
        }
    }
}