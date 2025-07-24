// BudgetViewModel

package com.uaa.misgastosapp.ui.viewmodel

import android.app.Application
import android.os.Build
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.uaa.misgastosapp.data.AppDatabase
import com.uaa.misgastosapp.data.BudgetEntity
import com.uaa.misgastosapp.data.repository.BudgetRepository
import com.uaa.misgastosapp.model.Budget
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
        // se obtiene la instancia de la base de datos.
        val db = AppDatabase.getInstance(application)
        // se inicializa el repositorio, pasandole los daos necesarios.
        repository = BudgetRepository(db.budgetDao(), db.categoryDao(), db.transactionDao())
    }

    // se crea un 'stateflow' para guardar el mes y año actual que se esta viendo en la pantalla.
    // es privado para que solo el viewmodel lo pueda modificar. por defecto, es el mes y año actual.
    private val _currentMonthYear = MutableStateFlow(YearMonth.now())
    // esta es la version publica y de solo lectura del estado del mes/año, para que la interfaz lo observe.
    val currentMonthYear: StateFlow<YearMonth> = _currentMonthYear.asStateFlow()

    // aca se crea un flujo que transforma el objeto 'yearmonth' a un texto con formato "yyyy-mm".
    // este formato es el que se usa en la base de datos para las consultas.
    private val currentMonthYearString: Flow<String> = _currentMonthYear.map {
        it.format(DateTimeFormatter.ofPattern("yyyy-MM"))
    }

    // este es el 'stateflow' principal que la interfaz de usuario observara para mostrar la lista de presupuestos.
    val budgetsWithSpendingForCurrentMonth: StateFlow<List<Budget>> =
        // se llama al repositorio para obtener el flujo de presupuestos y gastos para el mes actual.
        repository.getBudgetsWithSpendingForMonth(currentMonthYearString)
            // se añade un bloque 'catch' para atrapar y registrar cualquier error que ocurra en el flujo.
            .catch { e ->
                Log.e("BudgetVM", "Error en el flujo de presupuestos", e)
                // si hay un error, se emite una lista vacia para no bloquear la app.
                emit(emptyList())
            }
            // se convierte el flujo "frio" en un flujo "caliente" (stateflow).
            // se mantiene activo durante 5 segundos despues de que el ultimo observador se va.
            // el valor inicial es una lista vacia.
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // esta funcion permite que la interfaz de usuario cambie el mes y año que se esta mostrando.
    fun setCurrentMonthYear(yearMonth: YearMonth) {
        _currentMonthYear.value = yearMonth
    }

    // esta funcion obtiene el presupuesto para una categoria y mes especificos.
    fun getBudgetForCategory(categoryId: Int, monthYear: String): Flow<BudgetEntity?> {
        return repository.getBudgetForCategoryAndMonth(categoryId, monthYear)
            .catch { e ->
                // si ocurre un error, se registra y se emite un valor nulo.
                Log.e("BudgetVM", "Error al obtener presupuesto para categoría", e)
                emit(null)
            }
    }

    // esta funcion permite establecer o actualizar un presupuesto.
    fun setBudget(categoryId: Int, amount: Double, monthYear: String, onSuccess: () -> Unit, onError: (String) -> Unit) {
        // se inicia una corutina para no bloquear la interfaz.
        viewModelScope.launch {
            try {
                // se llama al metodo del repositorio para guardar el presupuesto.
                repository.setBudget(categoryId, amount, monthYear)
                // si todo sale bien, se llama a la funcion de exito.
                onSuccess()
            } catch (e: Exception) {
                // si hay un error, se registra y se llama a la funcion de error.
                Log.e("BudgetVM", "Error al establecer presupuesto", e)
                onError(e.message ?: "Error inesperado.")
            }
        }
    }
}