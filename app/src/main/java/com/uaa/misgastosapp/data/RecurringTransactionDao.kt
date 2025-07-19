// RecurringTransactionDao

package com.uaa.misgastosapp.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface RecurringTransactionDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(recurringTransaction: RecurringTransactionEntity): Long

    @Update
    suspend fun update(recurringTransaction: RecurringTransactionEntity)

    @Delete
    suspend fun delete(recurringTransaction: RecurringTransactionEntity)

    @Query("SELECT * FROM recurring_transactions WHERE id = :id AND userId = :userId")
    suspend fun getById(id: Int, userId: Int): RecurringTransactionEntity?

    @Query("SELECT * FROM recurring_transactions WHERE userId = :userId ORDER BY nextDueDate ASC")
    fun getAll(userId: Int): Flow<List<RecurringTransactionEntity>>

    @Query("SELECT * FROM recurring_transactions WHERE isActive = 1 AND nextDueDate <= :currentDate")
    suspend fun getDueRecurringTransactions(currentDate: String): List<RecurringTransactionEntity>
}