// TransactionItem

package com.uaa.misgastosapp.ui

import androidx.compose.animation.*
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.uaa.misgastosapp.model.Transaction
import java.text.NumberFormat
import java.util.*

// se usa esta anotacion para poder utilizar apis de animacion que aun son experimentales.
@OptIn(ExperimentalAnimationApi::class)
// aca se define un composable reutilizable para mostrar cada transaccion en la lista del home.
@Composable
fun TransactionItem(
    transaction: Transaction,
    onDelete: () -> Unit
) {
    // se configuran el formato de moneda y se formatea el monto.
    val numberFormat = NumberFormat.getCurrencyInstance(Locale("es", "PY")).apply {
        maximumFractionDigits = 0
    }
    val formattedAmount = numberFormat.format(transaction.amount)
    // se define un estado para controlar si los detalles de la transaccion estan expandidos.
    var expanded by remember { mutableStateOf(false) }
    // se determina si la transaccion es un ingreso o un gasto.
    val isIncome = transaction.amount >= 0
    // se definen colores para los ingresos y gastos.
    val incomeColorText = Color(0xFF1B5E20)
    val expenseColorText = Color(0xFFB71C1C)
    val amountColor = if (isIncome) incomeColorText else expenseColorText
    val incomeBgColor = Color(0xFFE8F5E9)
    val expenseBgColor = Color(0xFFFFEBEE)
    // se crea una animacion para la rotacion de la flecha de expandir/colapsar.
    val arrowRotation by animateFloatAsState(
        targetValue = if (expanded) 180f else 0f,
        animationSpec = tween(durationMillis = 300),
        label = "arrowRotation"
    )

    // se usa una card como contenedor principal del item.
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        elevation = CardDefaults.cardElevation(2.dp),
        // el color de fondo de la tarjeta depende si es ingreso o gasto.
        colors = CardDefaults.cardColors(
            containerColor = if (isIncome) incomeBgColor else expenseBgColor
        )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // se muestra la informacion principal en una fila.
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = transaction.title,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = formattedAmount,
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Bold,
                        color = amountColor
                    )
                }

                // se muestran los botones de accion (borrar y expandir).
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = onDelete) {
                        Icon(Icons.Default.Delete, contentDescription = "Eliminar", tint = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    IconButton(onClick = { expanded = !expanded }) {
                        Icon(
                            imageVector = Icons.Default.ArrowDropDown,
                            contentDescription = "Expandir",
                            modifier = Modifier.rotate(arrowRotation),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            // se usa 'animatedvisibility' para mostrar u ocultar los detalles con una animacion.
            AnimatedVisibility(
                visible = expanded,
                enter = expandVertically(animationSpec = tween(300)) + fadeIn(),
                exit = shrinkVertically(animationSpec = tween(300)) + fadeOut()
            ) {
                // aca se define el contenido de los detalles (categoria y fecha).
                Column(modifier = Modifier.padding(top = 12.dp)) {
                    HorizontalDivider(modifier = Modifier.padding(bottom = 8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "Categoría:",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = transaction.categoryName ?: "Sin Categoría",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "Fecha:",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = transaction.date,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                }
            }
        }
    }
}