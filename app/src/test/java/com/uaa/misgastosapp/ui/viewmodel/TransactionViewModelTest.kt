package com.uaa.misgastosapp.ui.viewmodel

import android.app.Application
import app.cash.turbine.test
import com.uaa.misgastosapp.data.repository.TransactionRepository
import com.uaa.misgastosapp.model.Transaction
import com.uaa.misgastosapp.utils.Result
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class TransactionViewModelTest {

    private val testDispatcher = UnconfinedTestDispatcher()
    private val application: Application = mockk(relaxed = true)
    private val repository: TransactionRepository = mockk()

    private val sampleTransaction = Transaction(
        id = 1,
        title = "Supermercado",
        amount = -50000.0,
        date = "2026-08-09",
        categoryId = 1,
        categoryName = "Comida"
    )

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun createViewModel(): TransactionViewModel {
        every { repository.allTransactions } returns flowOf(listOf(sampleTransaction))
        return TransactionViewModel(application, repository)
    }

    @Test
    fun `transactions expone la lista del repositorio`() = runTest {
        val viewModel = createViewModel()

        viewModel.transactions.test {
            assertEquals(listOf(sampleTransaction), awaitItem())
        }
    }

    @Test
    fun `operationStatus es null al crear el ViewModel`() {
        val viewModel = createViewModel()
        assertNull(viewModel.operationStatus.value)
    }

    @Test
    fun `addTransaction exitosa termina en Success`() = runTest {
        val viewModel = createViewModel()
        coEvery { repository.insertTransaction(any(), any(), any(), any(), any()) } returns Unit

        viewModel.addTransaction("Supermercado", -50000.0, "2026-08-09", categoryId = 1)

        coVerify { repository.insertTransaction("Supermercado", -50000.0, "2026-08-09", 1, null) }
        val status = viewModel.operationStatus.value
        assertTrue(status is Result.Success)
    }

    @Test
    fun `addTransaction fallida termina en Error`() = runTest {
        val viewModel = createViewModel()
        coEvery { repository.insertTransaction(any(), any(), any(), any(), any()) } throws RuntimeException("fallo de red")

        viewModel.addTransaction("Supermercado", -50000.0, "2026-08-09", categoryId = 1)

        val status = viewModel.operationStatus.value
        assertTrue(status is Result.Error)
        assertTrue((status as Result.Error).message.contains("fallo de red"))
    }

    @Test
    fun `deleteTransaction exitosa termina en Success`() = runTest {
        val viewModel = createViewModel()
        coEvery { repository.deleteTransaction(any()) } returns Unit

        viewModel.deleteTransaction(1)

        coVerify { repository.deleteTransaction(1) }
        assertTrue(viewModel.operationStatus.value is Result.Success)
    }

    @Test
    fun `deleteTransaction fallida termina en Error`() = runTest {
        val viewModel = createViewModel()
        coEvery { repository.deleteTransaction(any()) } throws NoSuchElementException("no existe")

        viewModel.deleteTransaction(999)

        assertTrue(viewModel.operationStatus.value is Result.Error)
    }

    @Test
    fun `clearOperationStatus vuelve a null`() = runTest {
        val viewModel = createViewModel()
        coEvery { repository.deleteTransaction(any()) } returns Unit
        viewModel.deleteTransaction(1)
        assertTrue(viewModel.operationStatus.value is Result.Success)

        viewModel.clearOperationStatus()

        assertNull(viewModel.operationStatus.value)
    }
}
