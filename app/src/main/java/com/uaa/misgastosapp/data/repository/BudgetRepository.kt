// BudgetRepository.kt

package com.uaa.misgastosapp.data.repository

import com.uaa.misgastosapp.data.BudgetDao
import com.uaa.misgastosapp.data.BudgetEntity
import com.uaa.misgastosapp.data.CategoryDao
import com.uaa.misgastosapp.data.TransactionDao
import com.uaa.misgastosapp.model.Budget
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map

// esta clase es la encargada de manejar la logica de los presupuestos.
// se comunica con las tablas de presupuestos, categorias y transacciones para obtener datos completos.
@OptIn(ExperimentalCoroutinesApi::class)
class BudgetRepository(
    private val budgetDao: BudgetDao,
    private val categoryDao: CategoryDao,
    private val transactionDao: TransactionDao
) {
    companion object {
        // sentinel guardado en la columna 'monthYear' para los presupuestos que aplican a todos
        // los meses (isRecurring=true); nunca coincide con un mes real (formato "yyyy-MM").
        const val RECURRING_MONTH_YEAR = "RECURRING"
    }

    // cuantos presupuestos "todos los meses" existen en total, para el limite del plan Free
    // (independiente del mes que se este viendo, a diferencia de los presupuestos por mes).
    val recurringBudgetsCount: Flow<Int> = budgetDao.getRecurringBudgets().map { it.size }

    // esta funcion se encarga de obtener los presupuestos de un mes junto con el total gastado en cada uno.
    fun getBudgetsWithSpendingForMonth(monthYearFlow: Flow<String>): Flow<List<Budget>> {
        // 'flatmaplatest' se usa para que cada vez que el mes/año cambie, la logica de adentro se vuelva a ejecutar con el nuevo valor.
        return monthYearFlow.flatMapLatest { monthStr ->
            // 'combine' se usa para juntar los resultados de varias fuentes de datos en tiempo real.
            // se ejecuta cada vez que hay un cambio en las categorias, los presupuestos (del mes o
            // recurrentes), o las transacciones.
            combine(
                categoryDao.getAll(),
                budgetDao.getBudgetsForMonth(monthStr),
                budgetDao.getRecurringBudgets(),
                transactionDao.getExpensesForMonth("$monthStr%")
            ) { categoriesEntities, monthBudgetEntities, recurringBudgetEntities, transactionsForMonth ->

                // despues, se recorre la lista de todas las categorias.
                categoriesEntities.map { categoryEntity ->
                    // por cada categoria, un presupuesto especifico de este mes tiene prioridad
                    // sobre uno recurrente (permite "este mes excepcionalmente gasto distinto").
                    val specificBudget = monthBudgetEntities.find { it.categoryId == categoryEntity.id }
                    val recurringBudget = recurringBudgetEntities.find { it.categoryId == categoryEntity.id }
                    val resolvedBudget = specificBudget ?: recurringBudget
                    // se calcula cuanto se ha gastado en esa categoria, sumando las transacciones correspondientes.
                    val spentAmount = transactionsForMonth
                        .filter { it.categoryId == categoryEntity.id }
                        .sumOf { it.amount * -1 }
                    // se obtiene el monto del presupuesto. si no existe, es 0.
                    val budgetAmount = resolvedBudget?.amount ?: 0.0
                    // se crea un objeto 'budget' con toda la informacion combinada.
                    Budget(
                        id = resolvedBudget?.id ?: 0,
                        categoryId = categoryEntity.id,
                        categoryName = categoryEntity.name,
                        monthYear = monthStr,
                        amount = budgetAmount,
                        isRecurring = resolvedBudget?.isRecurring ?: false,
                        spentAmount = spentAmount
                    )
                }.sortedBy { it.categoryName } // finalmente, se ordena la lista de presupuestos por el nombre de la categoria.
            }
        }
    }

    // esta funcion se usa para establecer o actualizar un presupuesto. si isRecurring es true,
    // aplica a todos los meses (se ignora el 'monthYear' recibido y se usa el sentinel).
    suspend fun setBudget(categoryId: Int, amount: Double, monthYear: String, isRecurring: Boolean = false) {
        // se comprueba que el monto del presupuesto no sea negativo.
        if (amount < 0) {
            throw IllegalArgumentException("El presupuesto no puede ser negativo.")
        }
        // se crea una entidad de presupuesto con los datos recibidos.
        val budgetEntity = BudgetEntity(
            categoryId = categoryId,
            monthYear = if (isRecurring) RECURRING_MONTH_YEAR else monthYear,
            amount = amount,
            isRecurring = isRecurring
        )
        // se inserta o actualiza en la base de datos a traves del dao.
        budgetDao.insertOrUpdate(budgetEntity)
    }

    // quita un presupuesto establecido (vuelve a "Sin presupuesto establecido" para esa
    // categoria/mes, o deja de aplicar a todos los meses si era recurrente); no afecta las
    // transacciones ya registradas. se borra por categoria+mes (no por id): insertOrUpdate hace
    // un "insert or replace" que borra la fila vieja e inserta una nueva con id distinto, asi que
    // un id capturado por la UI un momento antes podia ya no corresponder a ninguna fila real.
    suspend fun deleteBudget(categoryId: Int, monthYear: String, isRecurring: Boolean = false) {
        val key = if (isRecurring) RECURRING_MONTH_YEAR else monthYear
        budgetDao.deleteBudgetForCategoryAndMonth(categoryId, key)
    }

    // esta funcion obtiene un presupuesto especifico para una categoria y un mes, como modelo de
    // dominio (no la entidad de Room), para no filtrar el detalle de persistencia hacia la UI.
    // si no hay uno especifico para ese mes, cae al recurrente (mismo criterio de prioridad que
    // getBudgetsWithSpendingForMonth). no incluye 'spentAmount' (no se calcula en esta consulta puntual).
    fun getBudgetForCategoryAndMonth(categoryId: Int, monthYear: String): Flow<Budget?> {
        return combine(
            budgetDao.getBudgetForCategoryAndMonth(categoryId, monthYear),
            budgetDao.getRecurringBudgets()
        ) { specific, recurringList ->
            val resolved = specific ?: recurringList.find { it.categoryId == categoryId }
            resolved?.let {
                Budget(
                    id = it.id,
                    categoryId = it.categoryId,
                    categoryName = categoryDao.getCategoryNameById(it.categoryId) ?: "Sin Categoría",
                    monthYear = monthYear,
                    amount = it.amount,
                    isRecurring = it.isRecurring,
                    spentAmount = 0.0
                )
            }
        }
    }
}
