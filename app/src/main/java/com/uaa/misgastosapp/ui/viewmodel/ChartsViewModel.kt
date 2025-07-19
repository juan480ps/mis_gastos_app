// ChatsViewModel

package com.uaa.misgastosapp.ui.viewmodel

import android.app.Application
import android.os.Build
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.compose.ui.graphics.Color
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.uaa.misgastosapp.data.AppDatabase
import com.uaa.misgastosapp.utils.SecureSessionManager
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import java.time.YearMonth
import java.time.format.DateTimeFormatter

data class PieChartData(
    val label: String,
    val value: Float,
    val color: Color
)

@RequiresApi(Build.VERSION_CODES.O)
class ChartsViewModel(application: Application) : AndroidViewModel(application) {
    private val transactionDao = AppDatabase.getInstance(application).transactionDao()
    private val categoryDao = AppDatabase.getInstance(application).categoryDao()
    private val sessionManager = SecureSessionManager(application)

    private val _currentMonthYear = MutableStateFlow(YearMonth.now())
    val currentMonthYear: StateFlow<YearMonth> = _currentMonthYear.asStateFlow()

    private val currentMonthYearString: Flow<String> = _currentMonthYear.map {
        try {
            it.format(DateTimeFormatter.ofPattern("yyyy-MM"))
        } catch (e: Exception) {
            Log.e("ChartsVM", "Error formatting date: ${e.message}")
            YearMonth.now().format(DateTimeFormatter.ofPattern("yyyy-MM"))
        }
    }

    fun setCurrentMonthYear(yearMonth: YearMonth) {
        try {
            _currentMonthYear.value = yearMonth
        } catch (e: Exception) {
            Log.e("ChartsVM", "Error setting month year: ${e.message}")
        }
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    val processedExpensePieData: StateFlow<List<PieChartData>> = combine(
        sessionManager.userIdFlow,
        currentMonthYearString
    ) { userId, monthStr -> Pair(userId, monthStr) }
        .flatMapLatest { (userId, monthStr) ->
            if (userId == 0) {
                return@flatMapLatest flowOf(emptyList<PieChartData>())
            }
            combine(
                transactionDao.getAll(userId),
                categoryDao.getAll(userId)
            ) { transactions, categories ->
                try {
                    val expensesInMonth = transactions
                        .filter { it.date.startsWith(monthStr) && it.amount < 0 }

                    if (expensesInMonth.isEmpty()) {
                        return@combine emptyList<PieChartData>()
                    }

                    val expensesByCategory = expensesInMonth
                        .groupBy { it.categoryId }
                        .mapValues { entry -> entry.value.sumOf { it.amount * -1 } }

                    val chartDataList = mutableListOf<PieChartData>()
                    var colorIndex = 0

                    val predefinedColors = listOf(
                        Color(0xFFF44336), Color(0xFFE91E63), Color(0xFF9C27B0), Color(0xFF673AB7),
                        Color(0xFF3F51B5), Color(0xFF2196F3), Color(0xFF03A9F4), Color(0xFF00BCD4),
                        Color(0xFF009688), Color(0xFF4CAF50), Color(0xFF8BC34A), Color(0xFFCDDC39),
                        Color(0xFFFFEB3B), Color(0xFFFFC107), Color(0xFFFF9800), Color(0xFF795548)
                    )

                    expensesByCategory.entries
                        .sortedByDescending { it.value }
                        .forEach { (categoryId, totalAmount) ->
                            val categoryName = categoryId?.let { id ->
                                categories.find { it.id == id }?.name
                            } ?: "Sin Categoría"
                            chartDataList.add(
                                PieChartData(
                                    label = categoryName,
                                    value = totalAmount.toFloat(),
                                    color = predefinedColors[colorIndex % predefinedColors.size]
                                )
                            )
                            colorIndex++
                        }
                    chartDataList
                } catch (e: Exception) {
                    Log.e("ChartsVM", "Error processing pie data: ${e.message}")
                    emptyList()
                }
            }.catch { e ->
                Log.e("ChartsVM", "Flow error: ${e.message}")
                emit(emptyList())
            }
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), emptyList())
}