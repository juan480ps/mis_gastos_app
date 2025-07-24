// SummaryScreen

package com.uaa.misgastosapp.ui

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.text.font.FontWeight
import java.text.NumberFormat
import java.util.*
import androidx.compose.ui.graphics.Color

// aca se define un composable reutilizable que muestra una tarjeta con el balance total.
@Composable
fun SummaryCard(balance: Double) {
    // se configura el formato de moneda para guaranies.
    val numberFormat = NumberFormat.getCurrencyInstance(Locale("es", "PY")).apply {
        maximumFractionDigits = 0
    }
    // se formatea el balance recibido.
    val formattedBalance = numberFormat.format(balance)

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
            // se muestra el titulo "balance total".
            Text(
                "Balance Total",
                style = MaterialTheme.typography.titleMedium,
                color = Color.White
            )
            // se muestra el monto del balance formateado.
            Text(
                formattedBalance,
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
        }
    }
}