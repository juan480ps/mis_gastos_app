package com.uaa.misgastosapp.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface TransactionDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(transaction: TransactionEntity)

    @Delete
    suspend fun delete(transaction: TransactionEntity)

    @Query("SELECT * FROM transactions ORDER BY id DESC")
    fun getAll(): Flow<List<TransactionEntity>>

    @Query("""
        SELECT t.id, t.title, t.amount, t.date, t.categoryId, c.name as categoryName
        FROM transactions t
        LEFT JOIN categories c ON t.categoryId = c.id
        ORDER BY t.id DESC
    """)
    fun getAllWithCategoryName(): Flow<List<TransactionWithCategoryName>>

    @Query("SELECT * FROM transactions WHERE date LIKE :monthYearPattern AND amount < 0")
    fun getExpensesForMonth(monthYearPattern: String): Flow<List<TransactionEntity>>

    @Query("""
        SELECT t.id, t.title, t.amount, t.date, t.categoryId, c.name as categoryName
        FROM transactions t
        LEFT JOIN categories c ON t.categoryId = c.id
        WHERE t.date LIKE :monthYearPattern AND t.amount < 0
        ORDER BY t.amount ASC
    """)
    fun getExpensesWithCategoryName(monthYearPattern: String): Flow<List<TransactionWithCategoryName>>

    @Query("SELECT * FROM transactions WHERE id = :id LIMIT 1")
    suspend fun getById(id: Int): TransactionEntity?
}
