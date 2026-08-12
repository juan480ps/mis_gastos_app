package com.uaa.misgastosapp.ui.viewmodel

import android.app.Application
import android.os.Build
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.compose.ui.graphics.Color
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.uaa.misgastosapp.data.PremiumManager
import com.uaa.misgastosapp.data.TransactionWithCategoryName
import com.uaa.misgastosapp.data.repository.AppRepositories
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
    private val transactionRepository = AppRepositories.transactionRepository(application)

    private val _currentMonthYear = MutableStateFlow(YearMonth.now())
    val currentMonthYear: StateFlow<YearMonth> = _currentMonthYear.asStateFlow()

    private val currentMonthYearString: Flow<String> = _currentMonthYear
        .map { it.format(DateTimeFormatter.ofPattern("yyyy-MM")) }
        .distinctUntilChanged()

    // filtrar los graficos por cuenta es una funcion Premium; en el plan Free se ignora este
    // filtro y siempre se muestran los datos combinados de todas las cuentas.
    private val _accountFilterId = MutableStateFlow<Int?>(null)
    val accountFilterId: StateFlow<Int?> = _accountFilterId.asStateFlow()

    fun setCurrentMonthYear(yearMonth: YearMonth) {
        _currentMonthYear.value = yearMonth
    }

    fun setAccountFilter(accountId: Int?) {
        _accountFilterId.value = accountId
    }

    // se vuelve a chequear isPremium aca (no solo en la UI) para que el filtro por cuenta jamas
    // se aplique si el usuario deja de ser Premium mientras esta pantalla sigue abierta.
    private val isPremium = PremiumManager.getInstance(application).isPremium

    @OptIn(ExperimentalCoroutinesApi::class)
    val processedExpensePieData: StateFlow<List<PieChartData>> = combine(
        currentMonthYearString, _accountFilterId, isPremium
    ) { monthStr, accountId, premium -> Triple(monthStr, accountId, premium) }
        .distinctUntilChanged()
        .flatMapLatest { (monthStr, accountId, premium) ->
            transactionRepository.getExpensesWithCategoryName("$monthStr%")
                .map { allExpenses: List<TransactionWithCategoryName> ->
                    val expenses = if (premium && accountId != null) {
                        allExpenses.filter { it.accountId == accountId }
                    } else {
                        allExpenses
                    }
                    if (expenses.isEmpty()) return@map emptyList<PieChartData>()

                    val expensesByCategory: Map<Int?, Double> = expenses
                        .groupBy { it.categoryId }
                        .mapValues { entry -> entry.value.sumOf { it.amount * -1 } }

                    val predefinedColors = listOf(
                        Color(0xFFF44336), Color(0xFFE91E63), Color(0xFF9C27B0), Color(0xFF673AB7),
                        Color(0xFF3F51B5), Color(0xFF2196F3), Color(0xFF03A9F4), Color(0xFF00BCD4),
                        Color(0xFF009688), Color(0xFF4CAF50), Color(0xFF8BC34A), Color(0xFFCDDC39),
                        Color(0xFFFFEB3B), Color(0xFFFFC107), Color(0xFFFF9800), Color(0xFF795548)
                    )

                    expensesByCategory.entries
                        .sortedByDescending { it.value }
                        .mapIndexed { index: Int, entry: Map.Entry<Int?, Double> ->
                            val categoryName: String = expenses
                                .firstOrNull { it.categoryId == entry.key }
                                ?.categoryName ?: "Sin Categoría"
                            PieChartData(
                                label = categoryName,
                                value = entry.value.toFloat(),
                                color = predefinedColors[index % predefinedColors.size]
                            )
                        }
                }
        }
        .distinctUntilChanged()
        .catch { e ->
            Log.e("ChartsVM", "Flow error: ${e.message}")
            emit(emptyList())
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), emptyList())
}
