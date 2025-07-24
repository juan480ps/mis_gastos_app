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
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import java.time.YearMonth
import java.time.format.DateTimeFormatter

// aca se define una 'data class' para contener los datos de cada porcion del grafico de torta.
data class PieChartData(
    // la etiqueta de texto que se mostrara.
    val label: String,
    // el valor numerico que determina el tamaño de la porcion.
    val value: Float,
    // el color de la porcion.
    val color: Color
)

// se asegura que este codigo solo se ejecute en versiones de android compatibles.
@RequiresApi(Build.VERSION_CODES.O)
// aca se define el viewmodel para la pantalla de graficos.
class ChartsViewModel(application: Application) : AndroidViewModel(application) {
    // se obtienen los daos de transaccion y categoria directamente desde la instancia de la base de datos.
    private val transactionDao = AppDatabase.getInstance(application).transactionDao()
    private val categoryDao = AppDatabase.getInstance(application).categoryDao()
    // se crea un 'stateflow' para guardar el mes y año que se esta mostrando en el grafico.
    private val _currentMonthYear = MutableStateFlow(YearMonth.now())
    // esta es la version publica y de solo lectura para que la interfaz la observe.
    val currentMonthYear: StateFlow<YearMonth> = _currentMonthYear.asStateFlow()

    // se crea un flujo que convierte el objeto 'yearmonth' a un texto con formato "yyyy-mm".
    private val currentMonthYearString: Flow<String> = _currentMonthYear.map {
        try {
            it.format(DateTimeFormatter.ofPattern("yyyy-MM"))
        } catch (e: Exception) {
            // si hay un error al formatear, se registra y se usa la fecha actual como respaldo.
            Log.e("ChartsVM", "Error formatting date: ${e.message}")
            YearMonth.now().format(DateTimeFormatter.ofPattern("yyyy-MM"))
        }
    }

    // esta funcion permite que la interfaz cambie el mes y año que se esta visualizando.
    fun setCurrentMonthYear(yearMonth: YearMonth) {
        try {
            _currentMonthYear.value = yearMonth
        } catch (e: Exception) {
            Log.e("ChartsVM", "Error setting month year: ${e.message}")
        }
    }

    // esta anotacion indica que se esta usando una caracteristica experimental de las corutinas.
    @OptIn(ExperimentalCoroutinesApi::class)
    // este es el 'stateflow' que la interfaz observara para obtener los datos del grafico de torta.
    val processedExpensePieData: StateFlow<List<PieChartData>> = currentMonthYearString.flatMapLatest { monthStr ->
        // se usa 'flatmaplatest' para que toda la logica se re-ejecute cuando cambie el mes.
        // se usa 'combine' para juntar los datos de las transacciones y las categorias en tiempo real.
        combine(
            transactionDao.getAll(),
            categoryDao.getAll()
        ) { transactions, categories ->
            try {
                // se filtran las transacciones para obtener solo los gastos del mes seleccionado.
                val expensesInMonth = transactions
                    .filter { it.date.startsWith(monthStr) && it.amount < 0 }

                // si no hay gastos, se devuelve una lista vacia inmediatamente.
                if (expensesInMonth.isEmpty()) {
                    return@combine emptyList<PieChartData>()
                }

                // se agrupan los gastos por el id de su categoria y se suma el total gastado en cada una.
                val expensesByCategory = expensesInMonth
                    .groupBy { it.categoryId }
                    .mapValues { entry -> entry.value.sumOf { it.amount * -1 } }

                // se crea una lista para guardar los datos del grafico.
                val chartDataList = mutableListOf<PieChartData>()
                var colorIndex = 0

                // se define una lista de colores predefinidos para el grafico.
                val predefinedColors = listOf(
                    Color(0xFFF44336), Color(0xFFE91E63), Color(0xFF9C27B0), Color(0xFF673AB7),
                    Color(0xFF3F51B5), Color(0xFF2196F3), Color(0xFF03A9F4), Color(0xFF00BCD4),
                    Color(0xFF009688), Color(0xFF4CAF50), Color(0xFF8BC34A), Color(0xFFCDDC39),
                    Color(0xFFFFEB3B), Color(0xFFFFC107), Color(0xFFFF9800), Color(0xFF795548)
                )

                // se recorren los gastos agrupados por categoria, ordenados del mayor al menor.
                expensesByCategory.entries
                    .sortedByDescending { it.value }
                    .forEach { (categoryId, totalAmount) ->
                        // se busca el nombre de la categoria. si no tiene, se usa "sin categoria".
                        val categoryName = categoryId?.let { id ->
                            categories.find { it.id == id }?.name
                        } ?: "Sin Categoría"
                        // se añade una nueva porcion de datos a la lista del grafico.
                        chartDataList.add(
                            PieChartData(
                                label = categoryName,
                                value = totalAmount.toFloat(),
                                // se asigna un color de la lista de forma ciclica.
                                color = predefinedColors[colorIndex % predefinedColors.size]
                            )
                        )
                        // se avanza al siguiente color.
                        colorIndex++
                    }
                chartDataList
            } catch (e: Exception) {
                // si hay un error procesando los datos, se registra y se devuelve una lista vacia.
                Log.e("ChartsVM", "Error processing pie data: ${e.message}")
                emptyList()
            }
        }.catch { e ->
            // si hay un error en el flujo mismo, se registra y se emite una lista vacia.
            Log.e("ChartsVM", "Flow error: ${e.message}")
            emit(emptyList())
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), emptyList())
}