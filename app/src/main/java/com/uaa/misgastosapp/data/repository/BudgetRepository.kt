// BudgetRepository.kt

package com.uaa.misgastosapp.data.repository

import com.uaa.misgastosapp.data.BudgetDao
import com.uaa.misgastosapp.data.BudgetEntity
import com.uaa.misgastosapp.data.CategoryDao
import com.uaa.misgastosapp.data.TransactionDao
import com.uaa.misgastosapp.model.Budget
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest

// esta clase es la encargada de manejar la logica de los presupuestos.
// se comunica con las tablas de presupuestos, categorias y transacciones para obtener datos completos.
class BudgetRepository(
    private val budgetDao: BudgetDao,
    private val categoryDao: CategoryDao,
    private val transactionDao: TransactionDao
) {
    // esta funcion se encarga de obtener los presupuestos de un mes junto con el total gastado en cada uno.
    fun getBudgetsWithSpendingForMonth(monthYearFlow: Flow<String>): Flow<List<Budget>> {
        // 'flatmaplatest' se usa para que cada vez que el mes/año cambie, la logica de adentro se vuelva a ejecutar con el nuevo valor.
        return monthYearFlow.flatMapLatest { monthStr ->
            // 'combine' se usa para juntar los resultados de varias fuentes de datos en tiempo real.
            // se ejecuta cada vez que hay un cambio en las categorias, los presupuestos del mes, o las transacciones.
            combine(
                categoryDao.getAll(),
                budgetDao.getBudgetsForMonth(monthStr),
                transactionDao.getAll()
            ) { categoriesEntities, budgetEntities, transactionEntities ->
                // primero, se filtran las transacciones para quedarse solo con los gastos del mes actual.
                val transactionsForMonth = transactionEntities.filter {
                    it.date.startsWith(monthStr) && it.amount < 0
                }

                // despues, se recorre la lista de todas las categorias.
                categoriesEntities.map { categoryEntity ->
                    // por cada categoria, se busca si tiene un presupuesto definido para ese mes.
                    val budgetEntity = budgetEntities.find { it.categoryId == categoryEntity.id }
                    // se calcula cuanto se ha gastado en esa categoria, sumando las transacciones correspondientes.
                    val spentAmount = transactionsForMonth
                        .filter { it.categoryId == categoryEntity.id }
                        .sumOf { it.amount * -1 }
                    // se obtiene el monto del presupuesto. si no existe, es 0.
                    val budgetAmount = budgetEntity?.amount ?: 0.0
                    // se crea un objeto 'budget' con toda la informacion combinada.
                    Budget(
                        id = budgetEntity?.id ?: 0,
                        categoryId = categoryEntity.id,
                        categoryName = categoryEntity.name,
                        monthYear = monthStr,
                        amount = budgetAmount,
                        spentAmount = spentAmount
                    )
                }.sortedBy { it.categoryName } // finalmente, se ordena la lista de presupuestos por el nombre de la categoria.
            }
        }
    }

    // esta funcion se usa para establecer o actualizar un presupuesto.
    suspend fun setBudget(categoryId: Int, amount: Double, monthYear: String) {
        // se comprueba que el monto del presupuesto no sea negativo.
        if (amount < 0) {
            throw IllegalArgumentException("El presupuesto no puede ser negativo.")
        }
        // se crea una entidad de presupuesto con los datos recibidos.
        val budgetEntity = BudgetEntity(
            categoryId = categoryId,
            monthYear = monthYear,
            amount = amount
        )
        // se inserta o actualiza en la base de datos a traves del dao.
        budgetDao.insertOrUpdate(budgetEntity)
    }

    // esta funcion obtiene un presupuesto especifico para una categoria y un mes.
    fun getBudgetForCategoryAndMonth(categoryId: Int, monthYear: String): Flow<BudgetEntity?> {
        // simplemente llama a la funcion correspondiente en el dao.
        return budgetDao.getBudgetForCategoryAndMonth(categoryId, monthYear)
    }
}