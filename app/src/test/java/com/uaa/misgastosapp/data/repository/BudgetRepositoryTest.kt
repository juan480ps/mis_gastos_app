package com.uaa.misgastosapp.data.repository

import com.uaa.misgastosapp.data.*
import io.mockk.*
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class BudgetRepositoryTest {

    private lateinit var repository: BudgetRepository
    private val budgetDao: BudgetDao = mockk()
    private val categoryDao: CategoryDao = mockk()
    private val transactionDao: TransactionDao = mockk()

    @Before
    fun setup() {
        // BudgetRepository llama a budgetDao.getRecurringBudgets() apenas se construye (para
        // recurringBudgetsCount) y tambien dentro de getBudgetForCategoryAndMonth(); sin este
        // stub, MockK no tiene ninguna respuesta configurada y lanza MockKException al
        // construir el repository, antes de llegar a ningun test.
        every { budgetDao.getRecurringBudgets() } returns flowOf(emptyList())
        repository = BudgetRepository(budgetDao, categoryDao, transactionDao)
    }

    @Test
    fun `setBudget throws exception when amount is negative`() = runTest {
        // When & Then
        try {
            repository.setBudget(categoryId = 1, amount = -100.0, monthYear = "2026-01")
            fail("Should throw IllegalArgumentException")
        } catch (e: IllegalArgumentException) {
            assertEquals("El presupuesto no puede ser negativo.", e.message)
        }
    }

    @Test
    fun `setBudget calls dao insertOrUpdate when amount is valid`() = runTest {
        // Given
        coEvery { budgetDao.insertOrUpdate(any()) } just Runs

        // When
        repository.setBudget(categoryId = 1, amount = 50000.0, monthYear = "2026-01")

        // Then
        coVerify(exactly = 1) { budgetDao.insertOrUpdate(any()) }
    }

    @Test
    fun `getBudgetForCategoryAndMonth returns dao result`() = runTest {
        // Given
        val budget = BudgetEntity(
            id = 1,
            categoryId = 1,
            monthYear = "2026-01",
            amount = 100000.0
        )
        every { budgetDao.getBudgetForCategoryAndMonth(1, "2026-01") } returns flowOf(budget)
        // getBudgetForCategoryAndMonth resuelve el nombre de categoria con este dao aparte.
        coEvery { categoryDao.getCategoryNameById(1) } returns "Comida"

        // When
        val result = repository.getBudgetForCategoryAndMonth(1, "2026-01").first()

        // Then
        assertNotNull(result)
        assertEquals(100000.0, result!!.amount, 0.01)
    }
}
