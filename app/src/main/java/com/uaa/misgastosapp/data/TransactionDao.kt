// TransactionDao

package com.uaa.misgastosapp.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface TransactionDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(transaction: TransactionEntity)

    @Delete
    suspend fun delete(transaction: TransactionEntity)

    @Query("SELECT * FROM transactions WHERE userId = :userId ORDER BY date DESC, id DESC")
    fun getAll(userId: Int): Flow<List<TransactionEntity>>

    @Query("SELECT * FROM transactions WHERE id = :id AND userId = :userId LIMIT 1")
    suspend fun getById(id: Int, userId: Int): TransactionEntity?

    @Query("SELECT SUM(amount) FROM transactions WHERE categoryId = :categoryId AND date LIKE :monthYearPattern AND amount < 0 AND userId = :userId")
    fun getSpentAmountForCategoryInMonth(categoryId: Int, monthYearPattern: String, userId: Int): Double?
}