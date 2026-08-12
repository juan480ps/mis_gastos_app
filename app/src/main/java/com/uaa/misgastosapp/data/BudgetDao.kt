// BudgetDao

package com.uaa.misgastosapp.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

// con la anotacion @dao se le indica a room que esta es una interfaz para acceder a los datos.
@Dao
// aca se define la interfaz 'BudgetDao', que contiene las operaciones para la tabla de presupuestos.
interface BudgetDao {
    // esta anotacion se usa para insertar. si el presupuesto ya existe, se reemplaza por el nuevo.
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    // es una funcion suspendida (para no bloquear la pantalla) que inserta o actualiza un presupuesto.
    suspend fun insertOrUpdate(budget: BudgetEntity)

    // aca se define una consulta para obtener un presupuesto especifico.
    @Query("SELECT * FROM budgets WHERE categoryId = :categoryId AND monthYear = :monthYear LIMIT 1")
    // esta funcion busca el presupuesto para una categoria y un mes/año especificos.
    // devuelve un 'flow', lo que significa que el resultado se actualizara automaticamente si cambia.
    fun getBudgetForCategoryAndMonth(categoryId: Int, monthYear: String): Flow<BudgetEntity?>

    // aca se define una consulta para obtener todos los presupuestos de un mes determinado.
    // se excluyen los recurrentes (isRecurring=1): esos se piden por separado con getRecurringBudgets().
    @Query("SELECT * FROM budgets WHERE monthYear = :monthYear AND isRecurring = 0")
    // esta funcion devuelve una lista de todos los presupuestos para un mes/año. tambien usa 'flow'.
    fun getBudgetsForMonth(monthYear: String): Flow<List<BudgetEntity>>

    // presupuestos marcados como "todos los meses": aplican sin importar el mes que se este viendo.
    @Query("SELECT * FROM budgets WHERE isRecurring = 1")
    fun getRecurringBudgets(): Flow<List<BudgetEntity>>

    // aca se define una consulta para borrar un presupuesto usando su id.
    @Query("DELETE FROM budgets WHERE id = :budgetId")
    // es una funcion suspendida que borra un presupuesto especifico de la tabla.
    suspend fun deleteBudgetById(budgetId: Int)

    // se borra por categoria+mes (la llave unica real desde el punto de vista del usuario) en vez
    // de por id: el id interno cambia cada vez que insertOrUpdate hace un "insert or replace"
    // (borra la fila vieja e inserta una nueva con id distinto), asi que confiar en un id
    // capturado por la UI un momento antes podia apuntar a una fila que ya no existía.
    @Query("DELETE FROM budgets WHERE categoryId = :categoryId AND monthYear = :monthYear")
    suspend fun deleteBudgetForCategoryAndMonth(categoryId: Int, monthYear: String)

    // aca se define una consulta para borrar todos los presupuestos asociados a una categoria.
    @Query("DELETE FROM budgets WHERE categoryId = :categoryId")
    // es una funcion suspendida que se usa para borrar los presupuestos de una categoria.
    suspend fun deleteBudgetsForCategory(categoryId: Int)
}