// RecurringTransactionEntity

package com.uaa.misgastosapp.data

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.PrimaryKey

// aca se define un tipo de dato especial para los tipos de recurrencia. por ahora, solo contiene 'monthly' (mensual).
enum class RecurrenceType {
    MONTHLY
}

// con la anotacion @entity se le indica a room que esta clase representa una tabla en la base de datos.
@Entity(
    // aca se le da el nombre "recurring_transactions" a la tabla.
    tableName = "recurring_transactions",
    // se definen las llaves foraneas, que son relaciones con otras tablas.
    foreignKeys = [
        ForeignKey(
            // se especifica que esta tabla se relaciona con la tabla de categorias (categoryentity).
            entity = CategoryEntity::class,
            // la columna "id" de la tabla de categorias es la columna padre.
            parentColumns = ["id"],
            // la columna "categoryid" de esta tabla es la columna hija.
            childColumns = ["categoryId"],
            // si se borra una categoria, el campo 'categoryid' en las transacciones recurrentes asociadas se pondra como nulo, pero la transaccion no se borrara.
            onDelete = ForeignKey.SET_NULL
        )
    ]
)
// se define una 'data class' para representar la estructura de una transaccion recurrente.
data class RecurringTransactionEntity(
    // se indica que 'id' es la llave primaria. 'autogenerate' hace que el id se cree automaticamente.
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    // esta columna guarda el titulo o descripcion de la transaccion.
    val title: String,
    // esta columna guarda el monto de la transaccion.
    val amount: Double,
    // esta columna guarda el id de la categoria asociada. el signo de interrogacion indica que puede ser nulo.
    val categoryId: Int?,
    // aca se guarda el tipo de recurrencia (por ejemplo, mensual).
    val recurrenceType: RecurrenceType,
    // aca se guarda el dia del mes en que se debe procesar la transaccion.
    val dayOfMonth: Int,
    // esta columna guarda la fecha de inicio de la recurrencia.
    val startDate: String,
    // esta columna guarda la fecha de fin. puede ser nula si la recurrencia no tiene fin.
    val endDate: String?,
    // aca se guarda la proxima fecha en que se debe procesar la transaccion. es 'var' porque se actualiza.
    var nextDueDate: String,
    // esta columna indica si la transaccion recurrente esta activa o no.
    val isActive: Boolean = true
)