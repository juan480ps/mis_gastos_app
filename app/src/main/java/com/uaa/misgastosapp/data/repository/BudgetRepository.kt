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

class BudgetRepository(
    private val budgetDao: BudgetDao,
    private val categoryDao: CategoryDao,
    private val transactionDao: TransactionDao
) {
    fun getBudgetsWithSpendingForMonth(monthYearFlow: Flow<String>, userId: Int): Flow<List<Budget>> {
        return monthYearFlow.flatMapLatest { monthStr ->
            combine(
                categoryDao.getAll(userId),
                budgetDao.getBudgetsForMonth(monthStr, userId),
                transactionDao.getAll(userId)
            ) { categoriesEntities, budgetEntities, transactionEntities ->
                val transactionsForMonth = transactionEntities.filter {
                    it.date.startsWith(monthStr) && it.amount < 0
                }

                categoriesEntities.map { categoryEntity ->
                    val budgetEntity = budgetEntities.find { it.categoryId == categoryEntity.id }
                    val spentAmount = transactionsForMonth
                        .filter { it.categoryId == categoryEntity.id }
                        .sumOf { it.amount * -1 }
                    val budgetAmount = budgetEntity?.amount ?: 0.0
                    Budget(
                        id = budgetEntity?.id ?: 0,
                        categoryId = categoryEntity.id,
                        categoryName = categoryEntity.name,
                        monthYear = monthStr,
                        amount = budgetAmount,
                        spentAmount = spentAmount
                    )
                }.sortedBy { it.categoryName }
            }
        }
    }

    suspend fun setBudget(categoryId: Int, amount: Double, monthYear: String, userId: Int) {
        if (amount < 0) {
            throw IllegalArgumentException("El presupuesto no puede ser negativo.")
        }
        val budgetEntity = BudgetEntity(
            categoryId = categoryId,
            monthYear = monthYear,
            amount = amount,
            userId = userId
        )
        budgetDao.insertOrUpdate(budgetEntity)
    }

    fun getBudgetForCategoryAndMonth(categoryId: Int, monthYear: String, userId: Int): Flow<BudgetEntity?> {
        return budgetDao.getBudgetForCategoryAndMonth(categoryId, monthYear, userId)
    }
}