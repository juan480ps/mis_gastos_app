package com.uaa.misgastosapp.data.repository

import com.uaa.misgastosapp.data.TransactionDao
import com.uaa.misgastosapp.data.TransactionEntity
import com.uaa.misgastosapp.data.TransactionWithCategoryName
import io.mockk.*
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

class TransactionRepositoryTest {

    private lateinit var repository: TransactionRepository
    private val transactionDao: TransactionDao = mockk()

    @Before
    fun setup() {
        repository = TransactionRepository(transactionDao)
    }

    @Test
    fun `allTransactions returns mapped transactions with category names`() = runTest {
        // Given
        val entities = listOf(
            TransactionWithCategoryName(
                id = 1,
                title = "Almuerzo",
                amount = -5000.0,
                date = "2026-01-15",
                categoryId = 1,
                categoryName = "Comida"
            ),
            TransactionWithCategoryName(
                id = 2,
                title = "Salario",
                amount = 500000.0,
                date = "2026-01-01",
                categoryId = null,
                categoryName = null
            )
        )

        every { transactionDao.getAllWithCategoryName() } returns flowOf(entities)

        // When
        val result = repository.allTransactions.first()

        // Then
        assertEquals(2, result.size)
        assertEquals("Almuerzo", result[0].title)
        assertEquals(-5000.0, result[0].amount, 0.01)
        assertEquals("Comida", result[0].categoryName)
        assertEquals("Salario", result[1].title)
        assertEquals("Sin Categoría", result[1].categoryName)
    }

    @Test
    fun `insertTransaction creates entity and calls dao insert`() = runTest {
        // Given
        coEvery { transactionDao.insert(any()) } just Runs

        // When
        repository.insertTransaction(
            title = "Compra",
            amount = -10000.0,
            date = "2026-01-20",
            categoryId = 2
        )

        // Then
        coVerify(exactly = 1) { transactionDao.insert(any()) }
    }

    @Test
    fun `deleteTransaction calls dao delete with correct entity`() = runTest {
        // Given
        val transaction = TransactionEntity(
            id = 1,
            title = "Test",
            amount = -100.0,
            date = "2026-01-01",
            categoryId = 1
        )
        coEvery { transactionDao.getById(1) } returns transaction
        coEvery { transactionDao.delete(transaction) } just Runs

        // When
        repository.deleteTransaction(1)

        // Then
        coVerify(exactly = 1) { transactionDao.delete(transaction) }
    }

    @Test(expected = NoSuchElementException::class)
    fun `deleteTransaction throws exception when transaction not found`() = runTest {
        // Given
        coEvery { transactionDao.getById(999) } returns null

        // When
        repository.deleteTransaction(999)

        // Then - exception thrown
    }
}
