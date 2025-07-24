// TransactionDao

package com.uaa.misgastosapp.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

// con la anotacion @dao se le indica a room que esta es una interfaz para acceder a los datos.
@Dao
// aca se define la interfaz 'transactiondao', que contiene las operaciones para la tabla de transacciones.
interface TransactionDao {
    // esta anotacion se usa para insertar. si la transaccion ya existe, se reemplaza por la nueva.
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    // es una funcion suspendida (para no bloquear la pantalla) que inserta una transaccion.
    suspend fun insert(transaction: TransactionEntity)

    // esta anotacion se usa para borrar una fila de la tabla.
    @Delete
    // es una funcion suspendida que borra una transaccion especifica.
    suspend fun delete(transaction: TransactionEntity)

    // aca se define una consulta para obtener todas las transacciones, ordenadas de la mas nueva a la mas vieja.
    @Query("SELECT * FROM transactions ORDER BY id DESC")
    // esta funcion devuelve una lista de todas las transacciones como un 'flow'.
    // esto permite que la lista se actualice sola si hay cambios en los datos.
    fun getAll(): Flow<List<TransactionEntity>>

    // aca se define una consulta para obtener una sola transaccion usando su id.
    @Query("SELECT * FROM transactions WHERE id = :id LIMIT 1")
    // es una funcion suspendida que busca una transaccion. puede devolver nulo si no la encuentra.
    suspend fun getById(id: Int): TransactionEntity?

    // aca se define una consulta que suma los montos de una categoria en un mes especifico.
    @Query("SELECT SUM(amount) FROM transactions WHERE categoryId = :categoryId AND date LIKE :monthYearPattern AND amount < 0")
    // esta funcion devuelve el total gastado para una categoria en un mes y año.
    // solo suma los montos negativos, que representan los gastos.
    fun getSpentAmountForCategoryInMonth(categoryId: Int, monthYearPattern: String): Double?
}