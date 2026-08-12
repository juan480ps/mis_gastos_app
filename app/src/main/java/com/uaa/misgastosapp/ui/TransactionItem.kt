// TransactionItem

package com.uaa.misgastosapp.ui

import androidx.compose.animation.*
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
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
    onEdit: () -> Unit,
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
    // el fondo de la tarjeta es un tinte claro del color del tema activo (no verde/rosa fijos
    // que no combinaban con el tema elegido por el usuario en Temas).
    val cardBgColor = lerp(MaterialTheme.colorScheme.primary, Color.White, 0.85f)
    // este fondo siempre es claro (es un tinte hacia blanco), asi que el texto de encima debe
    // ser siempre oscuro; usar MaterialTheme.colorScheme.onSurface se vuelve casi blanco en modo
    // oscuro e ilegible sobre este fondo claro.
    val onCardColor = Color(0xFF1B1B1B)
    val onCardVariantColor = Color(0xFF5F6368)
    // se crea una animacion para la rotacion de la flecha de expandir/colapsar.
    val arrowRotation by animateFloatAsState(
        targetValue = if (expanded) 180f else 0f,
        animationSpec = tween(durationMillis = 300),
        label = "arrowRotation"
    )

    // se usa una card como contenedor principal del item. tocar en cualquier parte de la card
    // expande/colapsa el detalle (igual que tocar la fecha del encabezado del mes); los botones
    // de editar/borrar tienen su propio click y lo interceptan antes de que llegue a la card.
    Card(
        onClick = { expanded = !expanded },
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        elevation = CardDefaults.cardElevation(2.dp),
        // el fondo ya no depende de si es ingreso/gasto (eso lo indica el color del monto);
        // usa el tinte claro del color del tema para que combine con lo que el usuario elija.
        colors = CardDefaults.cardColors(
            containerColor = cardBgColor
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
                        color = onCardColor
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = formattedAmount,
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Bold,
                        color = amountColor
                    )
                }

                // se muestran los botones de accion (editar, borrar y expandir).
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = onEdit) {
                        Icon(Icons.Default.Edit, contentDescription = "Editar", tint = onCardVariantColor)
                    }
                    IconButton(onClick = onDelete) {
                        Icon(Icons.Default.Delete, contentDescription = "Eliminar", tint = onCardVariantColor)
                    }
                    // decorativo: la card entera ya expande/colapsa al tocarla, este icono solo
                    // muestra el estado (rotando) sin tener su propio area de click separada.
                    Icon(
                        imageVector = Icons.Default.ArrowDropDown,
                        contentDescription = if (expanded) "Colapsar" else "Expandir",
                        modifier = Modifier.rotate(arrowRotation).padding(12.dp),
                        tint = onCardVariantColor
                    )
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
                            color = onCardVariantColor
                        )
                        Text(
                            text = transaction.categoryName ?: "Sin Categoría",
                            style = MaterialTheme.typography.bodyMedium,
                            color = onCardVariantColor
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
                            color = onCardVariantColor
                        )
                        Text(
                            text = transaction.date,
                            style = MaterialTheme.typography.bodyMedium,
                            color = onCardVariantColor
                        )
                    }
                    // la cuenta es opcional: esta fila solo aparece si la transaccion tiene una asignada.
                    transaction.accountName?.let { accountName ->
                        Spacer(modifier = Modifier.height(4.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "Cuenta:",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = onCardVariantColor
                            )
                            Text(
                                text = accountName,
                                style = MaterialTheme.typography.bodyMedium,
                                color = onCardVariantColor
                            )
                        }
                    }

                }
            }
        }
    }
}