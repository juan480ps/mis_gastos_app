// BudgetViewModel

package com.uaa.misgastosapp.ui.viewmodel

import android.app.Application
import android.os.Build
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.uaa.misgastosapp.data.repository.AppRepositories
import com.uaa.misgastosapp.data.repository.BudgetRepository
import com.uaa.misgastosapp.model.Budget
import com.uaa.misgastosapp.utils.Result
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.time.YearMonth
import java.time.format.DateTimeFormatter

// se asegura que este codigo solo se ejecute en versiones de android compatibles.
@RequiresApi(Build.VERSION_CODES.O)
// aca se define el viewmodel para los presupuestos. se encarga de la logica de negocio
// relacionada con la visualizacion y gestion de los presupuestos mensuales.
class BudgetViewModel(application: Application) : AndroidViewModel(application) {
    // se declara el repositorio de presupuestos, que sera la unica fuente de datos para este viewmodel.
    private val repository: BudgetRepository

    // el bloque 'init' se ejecuta cuando se crea una instancia de este viewmodel.
    init {
        repository = AppRepositories.budgetRepository(application)
    }

    // se crea un 'stateflow' para guardar el mes y año actual que se esta viendo en la pantalla.
    // es privado para que solo el viewmodel lo pueda modificar. por defecto, es el mes y año actual.
    private val _currentMonthYear = MutableStateFlow(YearMonth.now())
    // esta es la version publica y de solo lectura del estado del mes/año, para que la interfaz lo observe.
    val currentMonthYear: StateFlow<YearMonth> = _currentMonthYear.asStateFlow()

    // aca se crea un flujo que transforma el objeto 'yearmonth' a un texto con formato "yyyy-MM".
    private val currentMonthYearString: Flow<String> = _currentMonthYear
        .map { it.format(DateTimeFormatter.ofPattern("yyyy-MM")) }
        .distinctUntilChanged() // solo emite si el mes realmente cambio

    // se crea un 'stateflow' para comunicar el estado de setBudget, igual que en Transaction/CategoryViewModel.
    private val _operationStatus = MutableStateFlow<Result<String>?>(null)
    val operationStatus: StateFlow<Result<String>?> = _operationStatus.asStateFlow()

    // true hasta la primera emision (o error). asi la UI distingue "cargando" de "sin categorias
    // para presupuestar", que antes se veian igual (lista vacia en ambos casos).
    private val _isLoading = MutableStateFlow(true)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    // este es el 'stateflow' principal que la interfaz de usuario observara para mostrar la lista de presupuestos.
    val budgetsWithSpendingForCurrentMonth: StateFlow<List<Budget>> =
        repository.getBudgetsWithSpendingForMonth(currentMonthYearString)
            .distinctUntilChanged() // evita recomposiciones si los datos no cambiaron
            .onEach { _isLoading.value = false }
            .catch { e ->
                Log.e("BudgetVM", "Error en el flujo de presupuestos", e)
                _isLoading.value = false
                emit(emptyList())
            }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // cuantos presupuestos "todos los meses" hay en total, para el limite del plan Free
    // (independiente del mes que se este viendo).
    val recurringBudgetsCount: StateFlow<Int> = repository.recurringBudgetsCount
        .catch { e ->
            Log.e("BudgetVM", "Error en el flujo de presupuestos recurrentes", e)
            emit(0)
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    // esta funcion permite que la interfaz de usuario cambie el mes y año que se esta mostrando.
    fun setCurrentMonthYear(yearMonth: YearMonth) {
        _currentMonthYear.value = yearMonth
    }

    // esta funcion obtiene el presupuesto (modelo de dominio) para una categoria y mes especificos.
    fun getBudgetForCategory(categoryId: Int, monthYear: String): Flow<Budget?> {
        return repository.getBudgetForCategoryAndMonth(categoryId, monthYear)
            .catch { e ->
                // si ocurre un error, se registra y se emite un valor nulo.
                Log.e("BudgetVM", "Error al obtener presupuesto para categoría", e)
                emit(null)
            }
    }

    // esta funcion permite establecer o actualizar un presupuesto. isRecurring=true lo aplica a
    // todos los meses en vez de solo al mes indicado.
    fun setBudget(categoryId: Int, amount: Double, monthYear: String, isRecurring: Boolean = false) {
        // se inicia una corutina para no bloquear la interfaz.
        viewModelScope.launch {
            _operationStatus.value = Result.Loading
            try {
                // se llama al metodo del repositorio para guardar el presupuesto.
                repository.setBudget(categoryId, amount, monthYear, isRecurring)
                _operationStatus.value = Result.Success(
                    if (isRecurring) "Presupuesto guardado para todos los meses" else "Presupuesto guardado"
                )
            } catch (e: Exception) {
                // si hay un error, se registra y se actualiza el estado.
                Log.e("BudgetVM", "Error al establecer presupuesto", e)
                _operationStatus.value = Result.Error(e.message ?: "Error inesperado.")
            }
        }
    }

    // quita un presupuesto establecido (la categoria vuelve a "Sin presupuesto establecido").
    fun deleteBudget(categoryId: Int, monthYear: String, isRecurring: Boolean = false) {
        viewModelScope.launch {
            _operationStatus.value = Result.Loading
            try {
                repository.deleteBudget(categoryId, monthYear, isRecurring)
                _operationStatus.value = Result.Success("Presupuesto eliminado")
            } catch (e: Exception) {
                Log.e("BudgetVM", "Error al eliminar presupuesto", e)
                _operationStatus.value = Result.Error(e.message ?: "Error inesperado.")
            }
        }
    }

    fun clearOperationStatus() {
        _operationStatus.value = null
    }
}