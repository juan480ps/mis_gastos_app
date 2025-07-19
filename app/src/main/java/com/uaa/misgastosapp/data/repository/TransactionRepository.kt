// TransactionRepository.kt

package com.uaa.misgastosapp.data.repository

import android.os.Build
import androidx.annotation.RequiresApi
import com.uaa.misgastosapp.data.CategoryDao
import com.uaa.misgastosapp.data.TransactionDao
import com.uaa.misgastosapp.data.TransactionEntity
import com.uaa.misgastosapp.model.Transaction
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.time.LocalDate

@RequiresApi(Build.VERSION_CODES.O)
class TransactionRepository(
    private val transactionDao: TransactionDao,
    private val categoryDao: CategoryDao
) {

    fun allTransactions(userId: Int): Flow<List<Transaction>> = transactionDao.getAll(userId)
        .map { entityList ->
            entityList.map { entity ->
                val categoryName = entity.categoryId?.let { categoryDao.getCategoryNameById(it, userId) } ?: "Sin Categoría"
                Transaction(
                    id = entity.id,
                    title = entity.title,
                    amount = entity.amount,
                    date = entity.date,
                    categoryId = entity.categoryId,
                    categoryName = categoryName
                )
            }
        }

    suspend fun insertTransaction(title: String, amount: Double, date: String, categoryId: Int?, userId: Int) {
        val transaction = TransactionEntity(
            title = title,
            amount = amount,
            date = date,
            categoryId = categoryId,
            userId = userId
        )
        transactionDao.insert(transaction)
    }

    suspend fun deleteTransaction(id: Int, userId: Int) {
        val transactionToDelete = transactionDao.getById(id, userId)
            ?: throw NoSuchElementException("Transacción con ID $id no encontrada.")
        transactionDao.delete(transactionToDelete)
    }
}