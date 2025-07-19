// RecurringTransactionRepository.kt

package com.uaa.misgastosapp.data.repository

import android.os.Build
import androidx.annotation.RequiresApi
import com.uaa.misgastosapp.data.*
import com.uaa.misgastosapp.model.RecurringTransaction
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter

@RequiresApi(Build.VERSION_CODES.O)
class RecurringTransactionRepository(
    private val recurringDao: RecurringTransactionDao,
    private val transactionDao: TransactionDao,
    private val categoryDao: CategoryDao
) {

    fun allRecurringTransactions(userId: Int): Flow<List<RecurringTransaction>> = recurringDao.getAll(userId)
        .map { entities ->
            entities.map { entity ->
                val categoryName = entity.categoryId?.let { categoryDao.getCategoryNameById(it, userId) }
                RecurringTransaction(
                    id = entity.id,
                    title = entity.title,
                    amount = entity.amount,
                    categoryId = entity.categoryId,
                    categoryName = categoryName ?: "Sin Categoría",
                    recurrenceType = entity.recurrenceType,
                    dayOfMonth = entity.dayOfMonth,
                    startDate = entity.startDate,
                    endDate = entity.endDate,
                    nextDueDate = entity.nextDueDate,
                    isActive = entity.isActive,
                    userId = entity.userId
                )
            }
        }

    suspend fun getById(id: Int, userId: Int): RecurringTransactionEntity? = recurringDao.getById(id, userId)

    suspend fun insert(entity: RecurringTransactionEntity) = recurringDao.insert(entity)

    suspend fun update(entity: RecurringTransactionEntity) = recurringDao.update(entity)

    suspend fun delete(item: RecurringTransaction) {
        val entity = toEntity(item)
        recurringDao.delete(entity)
    }

    suspend fun processDueRecurringTransactions() {
        val today = LocalDate.now()
        val formatter = DateTimeFormatter.ISO_LOCAL_DATE
        val dueItems = recurringDao.getDueRecurringTransactions(today.format(formatter))

        for (item in dueItems) {
            val nextDueDate = LocalDate.parse(item.nextDueDate, formatter)
            val endDate = item.endDate?.let { LocalDate.parse(it, formatter) }

            if (!item.isActive || (endDate != null && nextDueDate.isAfter(endDate))) {
                continue
            }

            transactionDao.insert(
                TransactionEntity(
                    title = item.title,
                    amount = -item.amount,
                    date = item.nextDueDate,
                    categoryId = item.categoryId,
                    userId = item.userId
                )
            )

            val newNextDueDate = calculateNextDueDate(
                nextDueDate.plusDays(1),
                item.dayOfMonth,
                item.recurrenceType
            )

            val updatedItem = if (endDate != null && newNextDueDate.isAfter(endDate)) {
                item.copy(isActive = false, nextDueDate = newNextDueDate.format(formatter))
            } else {
                item.copy(nextDueDate = newNextDueDate.format(formatter))
            }
            recurringDao.update(updatedItem)
        }
    }

    private fun calculateNextDueDate(fromDate: LocalDate, dayOfMonth: Int, type: RecurrenceType): LocalDate {
        return when (type) {
            RecurrenceType.MONTHLY -> getNextMonthlyDueDate(fromDate, dayOfMonth)
        }
    }

    private fun getNextMonthlyDueDate(currentDate: LocalDate, day: Int): LocalDate {
        var yearMonth = YearMonth.from(currentDate)
        if (currentDate.dayOfMonth >= day) {
            yearMonth = yearMonth.plusMonths(1)
        }
        val targetDay = day.coerceAtMost(yearMonth.lengthOfMonth())
        return yearMonth.atDay(targetDay)
    }

    private fun toEntity(item: RecurringTransaction): RecurringTransactionEntity {
        return RecurringTransactionEntity(
            id = item.id,
            title = item.title,
            amount = item.amount,
            categoryId = item.categoryId,
            recurrenceType = item.recurrenceType,
            dayOfMonth = item.dayOfMonth,
            startDate = item.startDate,
            endDate = item.endDate,
            nextDueDate = item.nextDueDate,
            isActive = item.isActive,
            userId = item.userId
        )
    }
}