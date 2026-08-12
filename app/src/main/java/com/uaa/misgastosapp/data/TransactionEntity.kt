// TransactionEntity

package com.uaa.misgastosapp.data

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

// con la anotacion @entity se le indica a room que esta clase representa una tabla en la base de datos.
@Entity(
    // aca se le da el nombre "transactions" a la tabla.
    tableName = "transactions",
    // 'date' tiene indice porque las consultas de gastos del mes (Home, Charts) filtran con
    // "WHERE date LIKE 'yyyy-MM%'"; sin indice, cada una de esas consultas escanea toda la tabla.
    indices = [Index(value = ["categoryId"]), Index(value = ["accountId"]), Index(value = ["date"])],
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
        ),
        ForeignKey(
            // asociar una cuenta/banco a la transaccion es opcional: sirve para separar los
            // gastos por banco en vez de tener todo en una sola bolsa, sin ser obligatorio.
            entity = AccountEntity::class,
            parentColumns = ["id"],
            childColumns = ["accountId"],
            // si se borra la cuenta, la transaccion no se borra: solo queda sin cuenta asignada.
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
    val categoryId: Int? = null,
    // cuenta/banco asociado, opcional: null significa que la transaccion queda en la "bolsa general".
    val accountId: Int? = null
)

// data class para el resultado del JOIN entre transacciones, categorias y cuentas.
// permite obtener el nombre de la categoria/cuenta en una sola query, evitando N+1 queries.
data class TransactionWithCategoryName(
    val id: Int,
    val title: String,
    val amount: Double,
    val date: String,
    val categoryId: Int?,
    val categoryName: String?,
    val accountId: Int? = null,
    val accountName: String? = null
)