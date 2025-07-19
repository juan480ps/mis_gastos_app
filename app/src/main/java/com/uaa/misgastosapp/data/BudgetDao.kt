// BudgetDao

package com.uaa.misgastosapp.data
import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface BudgetDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdate(budget: BudgetEntity)
    @Query("SELECT * FROM budgets WHERE categoryId = :categoryId AND monthYear = :monthYear AND userId = :userId LIMIT 1")
    fun getBudgetForCategoryAndMonth(categoryId: Int, monthYear: String, userId: Int): Flow<BudgetEntity?>

    @Query("SELECT * FROM budgets WHERE monthYear = :monthYear AND userId = :userId")
    fun getBudgetsForMonth(monthYear: String, userId: Int): Flow<List<BudgetEntity>>

    @Query("DELETE FROM budgets WHERE id = :budgetId")
    suspend fun deleteBudgetById(budgetId: Int)

    @Query("DELETE FROM budgets WHERE categoryId = :categoryId AND userId = :userId")
    suspend fun deleteBudgetsForCategory(categoryId: Int, userId: Int)
}