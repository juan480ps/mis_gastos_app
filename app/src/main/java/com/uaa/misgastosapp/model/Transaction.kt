// Transaction

package com.uaa.misgastosapp.model

// aca se define una 'data class' que representa el modelo de una transaccion, tal como se usa en la logica de la aplicacion.
// esta clase combina datos de la entidad de transaccion con el nombre de la categoria para su uso en la interfaz.
data class Transaction(
    // se guarda el id unico de la transaccion.
    val id: Int,
    // se guarda el titulo o descripcion de la transaccion.
    val title: String,
    // se guarda el monto de la transaccion.
    val amount: Double,
    // se guarda la fecha en que se realizo la transaccion.
    val date: String,
    // se guarda el id de la categoria asociada. puede ser nulo.
    val categoryId: Int?,
    // se guarda el nombre de la categoria asociada. puede ser nulo.
    val categoryName: String?
)