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

// se asegura que este codigo solo se ejecute en versiones de android compatibles.
@RequiresApi(Build.VERSION_CODES.O)
// esta clase es la encargada de manejar la logica de las transacciones (ingresos y gastos).
class TransactionRepository(
    private val transactionDao: TransactionDao,
    private val categoryDao: CategoryDao
) {

    // aca se define una variable que contiene una lista de todas las transacciones.
    // esta lista se actualiza automaticamente gracias al uso de 'flow'.
    val allTransactions: Flow<List<Transaction>> = transactionDao.getAll()
        .map { entityList -> // el '.map' se usa para transformar la lista de entidades.
            // se recorre cada 'transactionentity' (de la base de datos) para convertirla a 'transaction' (del modelo de la app).
            entityList.map { entity ->
                // se busca el nombre de la categoria usando su id. si no tiene o no se encuentra, se le asigna "sin categoria".
                val categoryName = entity.categoryId?.let { categoryDao.getCategoryNameById(it) } ?: "Sin Categoría"
                // se crea el objeto 'transaction' con todos los datos combinados.
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

    // esta es una funcion suspendida para insertar una nueva transaccion.
    suspend fun insertTransaction(title: String, amount: Double, date: String, categoryId: Int?) {
        // se crea una entidad de transaccion con los datos recibidos.
        val transaction = TransactionEntity(
            title = title,
            amount = amount,
            date = date,
            categoryId = categoryId
        )
        // se llama a la funcion de insertar en el dao para guardarla en la base de datos.
        transactionDao.insert(transaction)
    }

    // esta es una funcion suspendida para borrar una transaccion usando su id.
    suspend fun deleteTransaction(id: Int) {
        // primero, se busca la transaccion en la base de datos para asegurarse de que existe.
        val transactionToDelete = transactionDao.getById(id)
        // si no se encuentra la transaccion, se lanza un error para avisar que no existe.
            ?: throw NoSuchElementException("Transacción con ID $id no encontrada.")
        // si se encuentra, se llama a la funcion de borrar en el dao.
        transactionDao.delete(transactionToDelete)
    }
}