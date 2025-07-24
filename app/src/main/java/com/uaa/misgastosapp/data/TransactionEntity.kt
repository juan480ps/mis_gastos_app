// TransactionEntity

package com.uaa.misgastosapp.data

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.PrimaryKey

// con la anotacion @entity se le indica a room que esta clase representa una tabla en la base de datos.
@Entity(
    // aca se le da el nombre "transactions" a la tabla.
    tableName = "transactions",
    // se definen las llaves foraneas, que son relaciones con otras tablas.
    foreignKeys = [
        ForeignKey(
            // se especifica que esta tabla se relaciona con la tabla de categorias (categoryentity).
            entity = CategoryEntity::class,
            // la columna "id" de la tabla de categorias es la columna padre de la relacion.
            parentColumns = ["id"],
            // la columna "categoryid" de esta tabla es la columna hija.
            childColumns = ["categoryId"],
            // si se borra una categoria, el campo 'categoryid' en las transacciones asociadas se pondra como nulo. la transaccion no se borrara.
            onDelete = ForeignKey.SET_NULL
        )
    ]
)
// se define una 'data class' para representar la estructura de una transaccion.
data class TransactionEntity(
    // se indica que 'id' es la llave primaria. 'autogenerate' hace que el id se cree automaticamente.
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    // esta columna guarda el titulo o descripcion de la transaccion.
    val title: String,
    // esta columna guarda el monto de la transaccion. puede ser positivo (ingreso) o negativo (gasto).
    val amount: Double,
    // esta columna guarda la fecha de la transaccion como texto.
    val date: String,
    // esta columna guarda el id de la categoria asociada. el signo de interrogacion indica que puede no tener una categoria (ser nulo).
    val categoryId: Int? = null
)