// RecurringTransaction

package com.uaa.misgastosapp.model

import com.uaa.misgastosapp.data.RecurrenceType

// aca se define una 'data class' que representa el modelo de una transaccion recurrente, tal como se usa en la logica de la aplicacion.
// esta clase combina datos de la entidad de transaccion recurrente con el nombre de la categoria.
data class RecurringTransaction(
    // se guarda el id unico de la transaccion recurrente.
    val id: Int,
    // se guarda el titulo o descripcion de la transaccion.
    val title: String,
    // se guarda el monto de la transaccion.
    val amount: Double,
    // se guarda el id de la categoria asociada. puede ser nulo.
    val categoryId: Int?,
    // se guarda el nombre de la categoria asociada. puede ser nulo.
    val categoryName: String?,
    // se guarda el tipo de recurrencia (por ejemplo, mensual).
    val recurrenceType: RecurrenceType,
    // se guarda el dia del mes en que se debe procesar la transaccion.
    val dayOfMonth: Int,
    // se guarda la fecha de inicio de la recurrencia.
    val startDate: String,
    // se guarda la fecha de fin de la recurrencia. puede ser nula.
    val endDate: String?,
    // se guarda la proxima fecha en que se debe procesar la transaccion.
    val nextDueDate: String,
    // se guarda si la transaccion recurrente esta activa o no.
    val isActive: Boolean
)