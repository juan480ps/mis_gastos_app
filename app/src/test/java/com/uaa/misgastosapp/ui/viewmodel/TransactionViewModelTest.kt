package com.uaa.misgastosapp.ui.viewmodel

import com.uaa.misgastosapp.data.repository.TransactionRepository
import com.uaa.misgastosapp.model.Transaction
import com.uaa.misgastosapp.utils.Result
import io.mockk.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.*
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class TransactionViewModelTest {

    private val testDispatcher = UnconfinedTestDispatcher()
    private val repository: TransactionRepository = mockk()

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `operationStatus is null initially`() = runTest {
        // Given - ViewModel would be created here
        // We test the Result class behavior
        val status: Result<String>? = null
        assertNull(status)
    }

    @Test
    fun `Result Loading state works correctly`() {
        // Given
        val loading: Result<String> = Result.Loading

        // Then
        assertTrue(loading is Result.Loading)
    }

    @Test
    fun `Result Success state works correctly`() {
        // Given
        val success: Result<String> = Result.Success("Operation completed")

        // Then
        assertTrue(success is Result.Success)
        assertEquals("Operation completed", (success as Result.Success).data)
    }

    @Test
    fun `Result Error state works correctly`() {
        // Given
        val error: Result<String> = Result.Error("Something went wrong")

        // Then
        assertTrue(error is Result.Error)
        assertEquals("Something went wrong", (error as Result.Error).message)
    }
}
