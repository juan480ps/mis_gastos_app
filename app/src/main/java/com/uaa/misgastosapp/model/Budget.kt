// Budget

package com.uaa.misgastosapp.model

// aca se define una 'data class' que representa el modelo de un presupuesto tal como se usa en la interfaz de la aplicacion.
// esta clase combina datos de varias tablas para ser mostrada facilmente.
data class Budget(
    // se guarda el id del presupuesto, que viene de la base de datos.
    val id: Int,
    // se guarda el id de la categoria a la que pertenece este presupuesto.
    val categoryId: Int,
    // se guarda el nombre de la categoria, para mostrarlo en la pantalla.
    val categoryName: String,
    // se guarda el mes y año del presupuesto, por ejemplo, '2025-07'.
    val monthYear: String,
    // se guarda el monto total asignado para este presupuesto.
    val amount: Double,
    // true = este presupuesto aplica a todos los meses (no hace falta configurarlo cada mes);
    // false = solo aplica al mes especifico indicado en 'monthYear'.
    val isRecurring: Boolean = false,
    // se guarda el monto que se ha gastado hasta ahora en esta categoria y mes. su valor por defecto es 0.
    val spentAmount: Double = 0.0,
    // se calcula y guarda el monto que queda disponible. es una resta simple entre el monto total y lo gastado.
    val remainingAmount: Double = amount - spentAmount,
    // se calcula y guarda el progreso del gasto como un porcentaje. si el monto del presupuesto es cero, el progreso es cero para evitar una division por cero.
    val progress: Float = if (amount > 0) (spentAmount / amount).toFloat() else 0f
)