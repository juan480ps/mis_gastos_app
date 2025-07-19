// RecurringTransactionViewModel

package com.uaa.misgastosapp.ui.viewmodel

import android.app.Application
import android.os.Build
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.uaa.misgastosapp.data.*
import com.uaa.misgastosapp.data.repository.RecurringTransactionRepository
import com.uaa.misgastosapp.model.RecurringTransaction
import com.uaa.misgastosapp.utils.SecureSessionManager
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter

@RequiresApi(Build.VERSION_CODES.O)
class RecurringTransactionViewModel(application: Application) : AndroidViewModel(application) {
    private val repository: RecurringTransactionRepository
    private val sessionManager = SecureSessionManager(application)

    init {
        val db = AppDatabase.getInstance(application)
        repository = RecurringTransactionRepository(
            db.recurringTransactionDao(),
            db.transactionDao(),
            db.categoryDao()
        )
    }

    val recurringTransactions: StateFlow<List<RecurringTransaction>> = sessionManager.userIdFlow
        .flatMapLatest { userId ->
            repository.allRecurringTransactions(userId)
        }
        .catch { e ->
            Log.e("RecurringVM", "Error en el flujo de transacciones recurrentes", e)
            emit(emptyList())
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), emptyList())

    fun getRecurringTransactionById(id: Int, callback: (RecurringTransactionEntity?) -> Unit) {
        viewModelScope.launch {
            try {
                val userId = sessionManager.getUserId()
                if (userId == 0) {
                    callback(null)
                    return@launch
                }
                callback(repository.getById(id, userId))
            } catch (e: Exception) {
                Log.e("RecurringVM", "Error al obtener transacción recurrente por ID", e)
                callback(null)
            }
        }
    }

    fun addOrUpdateRecurringTransaction(
        id: Int? = null,
        title: String,
        amount: Double,
        categoryId: Int?,
        recurrenceType: RecurrenceType,
        dayOfMonth: Int,
        startDate: LocalDate,
        endDate: LocalDate?,
        isActive: Boolean,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        viewModelScope.launch {
            try {

                if (title.isBlank()) throw IllegalArgumentException("El título no puede estar vacío.")
                if (amount <= 0) throw IllegalArgumentException("El monto debe ser mayor a cero.")
                if (dayOfMonth !in 1..31) throw IllegalArgumentException("Día del mes inválido.")
                val userId = sessionManager.getUserId()
                if (userId == 0) throw IllegalStateException("Usuario no autenticado.")

                val formatter = DateTimeFormatter.ISO_LOCAL_DATE
                val entity = RecurringTransactionEntity(
                    id = id ?: 0,
                    title = title,
                    amount = amount,
                    categoryId = categoryId,
                    recurrenceType = recurrenceType,
                    dayOfMonth = dayOfMonth,
                    startDate = startDate.format(formatter),
                    endDate = endDate?.format(formatter),

                    nextDueDate = calculateNextDueDate(startDate, dayOfMonth).format(formatter),
                    isActive = isActive,
                    userId = userId
                )

                if (id == null) {
                    repository.insert(entity)
                } else {
                    repository.update(entity)
                }
                onSuccess()
            } catch (e: Exception) {
                Log.e("RecurringVM", "Error al guardar transacción recurrente", e)
                onError(e.message ?: "Error inesperado.")
            }
        }
    }

    fun deleteRecurringTransaction(recurringTransaction: RecurringTransaction) {
        viewModelScope.launch {
            try {
                repository.delete(recurringTransaction)
            } catch (e: Exception) {
                Log.e("RecurringVM", "Error al eliminar transacción recurrente", e)
            }
        }
    }

    fun processDueRecurringTransactions() {
        viewModelScope.launch {
            try {
                repository.processDueRecurringTransactions()
            } catch (e: Exception) {
                Log.e("RecurringVM", "Error al procesar transacciones recurrentes debidas", e)
            }
        }
    }

    private fun calculateNextDueDate(fromDate: LocalDate, dayOfMonth: Int): LocalDate {
        var yearMonth = YearMonth.from(fromDate)
        if (fromDate.dayOfMonth >= dayOfMonth) {
            yearMonth = yearMonth.plusMonths(1)
        }
        val targetDay = dayOfMonth.coerceAtMost(yearMonth.lengthOfMonth())
        return yearMonth.atDay(targetDay)
    }
}