// BudgetEntity

package com.uaa.misgastosapp.data

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

// con la anotacion @Entity se le indica a room que esta clase representa una tabla en la base de datos.
@Entity(
    // aca se le da el nombre "budgets" a la tabla.
    tableName = "budgets",
    // se definen las llaves foraneas.
    foreignKeys = [
        ForeignKey(
            // se especifica que esta tabla se relaciona con la tabla de categorias (categoryentity).
            entity = CategoryEntity::class,
            // la columna "id" de la tabla de categorias es la columna padre.
            parentColumns = ["id"],
            // la columna "categoryid" de esta tabla (budgets) es la columna hija.
            childColumns = ["categoryId"],
            // si se borra una categoria, todos los presupuestos asociados a ella tambien se borraran.
            onDelete = ForeignKey.CASCADE
        )
    ],
    // aca se definen los indices, que ayudan a que las busquedas sean mas rapidas y a evitar duplicados.
    indices = [Index(value = ["categoryId", "monthYear"], unique = true)] // se asegura que no pueda haber mas de un presupuesto para la misma categoria en el mismo mes/año.
)
// se define una 'data class' para representar la estructura de un presupuesto.
data class BudgetEntity(
    // se indica que 'id' es la llave primaria. 'autogenerate' hace que el id se cree automaticamente.
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    // esta columna guarda el id de la categoria a la que pertenece el presupuesto.
    val categoryId: Int,
    // esta columna guarda el mes y el año del presupuesto en formato de texto.
    val monthYear: String,
    // esta columna guarda el monto total del presupuesto.
    val amount: Double,
    // true = aplica a todos los meses (no hace falta volver a configurarlo cada mes); false =
    // solo aplica al mes especifico guardado en 'monthYear'. cuando es true, 'monthYear' se
    // guarda con el sentinel RECURRING_MONTH_YEAR (ver BudgetRepository) en vez de un mes real.
    val isRecurring: Boolean = false
)