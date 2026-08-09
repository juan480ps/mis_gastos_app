// TransactionRepository.kt

package com.uaa.misgastosapp.data.repository

import android.os.Build
import androidx.annotation.RequiresApi
import com.uaa.misgastosapp.data.TransactionDao
import com.uaa.misgastosapp.data.TransactionEntity
import com.uaa.misgastosapp.model.Transaction
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.time.LocalDate

// se asegura que este codigo solo se ejecute en versiones de android compatibles.
@RequiresApi(Build.VERSION_CODES.O)
// esta clase es la encargada de manejar la logica de las transacciones (ingresos y gastos).
class TransactionRepository(
    private val transactionDao: TransactionDao
) {

    // aca se define una variable que contiene una lista de todas las transacciones.
    // usa un JOIN con categorias para obtener el nombre en una sola query (sin N+1).
    val allTransactions: Flow<List<Transaction>> = transactionDao.getAllWithCategoryName()
        .map { entityList ->
            entityList.map { entity ->
                Transaction(
                    id = entity.id,
                    title = entity.title,
                    amount = entity.amount,
                    date = entity.date,
                    categoryId = entity.categoryId,
                    categoryName = entity.categoryName ?: "Sin Categoría"
                )
            }
        }

    // esta es una funcion suspendida para insertar una nueva transaccion.
    suspend fun insertTransaction(title: String, amount: Double, date: String, categoryId: Int?) {
        val transaction = TransactionEntity(
            title = title,
            amount = amount,
            date = date,
            categoryId = categoryId
        )
        transactionDao.insert(transaction)
    }

    // esta es una funcion suspendida para borrar una transaccion usando su id.
    suspend fun deleteTransaction(id: Int) {
        val transactionToDelete = transactionDao.getById(id)
            ?: throw NoSuchElementException("Transacción con ID $id no encontrada.")
        transactionDao.delete(transactionToDelete)
    }
}