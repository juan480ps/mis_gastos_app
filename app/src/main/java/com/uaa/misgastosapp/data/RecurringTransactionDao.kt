// RecurringTransactionDao

package com.uaa.misgastosapp.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

// con la anotacion @dao se le indica a room que esta es una interfaz para acceder a los datos.
@Dao
// aca se define la interfaz 'recurringtransactiondao', que contiene las operaciones para la tabla de transacciones recurrentes.
interface RecurringTransactionDao {
    // esta anotacion se usa para insertar. si la transaccion ya existe, se reemplaza por la nueva.
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    // es una funcion suspendida que inserta una transaccion recurrente.
    suspend fun insert(recurringTransaction: RecurringTransactionEntity): Long

    // esta anotacion se usa para actualizar una fila existente en la tabla.
    @Update
    // es una funcion suspendida que actualiza una transaccion recurrente.
    suspend fun update(recurringTransaction: RecurringTransactionEntity)

    // esta anotacion se usa para borrar una fila de la tabla.
    @Delete
    // es una funcion suspendida que borra una transaccion recurrente.
    suspend fun delete(recurringTransaction: RecurringTransactionEntity)

    // aca se define una consulta para obtener una sola transaccion recurrente usando su id.
    @Query("SELECT * FROM recurring_transactions WHERE id = :id")
    // es una funcion suspendida que busca una transaccion especifica. puede devolver nulo si no la encuentra.
    suspend fun getById(id: Int): RecurringTransactionEntity?

    // aca se define una consulta para obtener todas las transacciones recurrentes, ordenadas por la proxima fecha de vencimiento.
    @Query("SELECT * FROM recurring_transactions ORDER BY nextDueDate ASC")
    // esta funcion devuelve una lista de todas las transacciones recurrentes como un 'flow',
    // lo que permite que la lista se actualice sola si hay cambios.
    fun getAll(): Flow<List<RecurringTransactionEntity>>

    // aca se define una consulta para obtener las transacciones que ya vencieron.
    @Query("SELECT * FROM recurring_transactions WHERE isActive = 1 AND nextDueDate <= :currentDate")
    // es una funcion suspendida que devuelve una lista de transacciones recurrentes activas cuya fecha de vencimiento ya paso.
    suspend fun getDueRecurringTransactions(currentDate: String): List<RecurringTransactionEntity>
}