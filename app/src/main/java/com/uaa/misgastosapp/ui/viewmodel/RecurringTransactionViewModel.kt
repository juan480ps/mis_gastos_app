// RecurringTransactionViewModel

package com.uaa.misgastosapp.ui.viewmodel

import android.app.Application
import android.os.Build
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.uaa.misgastosapp.data.*
import com.uaa.misgastosapp.data.repository.AppRepositories
import com.uaa.misgastosapp.data.repository.RecurringTransactionRepository
import com.uaa.misgastosapp.model.RecurringTransaction
import com.uaa.misgastosapp.utils.Result
import com.uaa.misgastosapp.utils.capitalizeFirst
import com.uaa.misgastosapp.worker.RecurringNotificationHelper
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter

// se asegura que este codigo solo se ejecute en versiones de android compatibles.
@RequiresApi(Build.VERSION_CODES.O)
// aca se define el viewmodel para las transacciones recurrentes.
class RecurringTransactionViewModel(application: Application) : AndroidViewModel(application) {
    // se declara el repositorio, que sera la unica fuente de datos.
    private val repository: RecurringTransactionRepository

    // el bloque 'init' se ejecuta cuando se crea una instancia del viewmodel.
    init {
        repository = AppRepositories.recurringTransactionRepository(application)
    }

    // se crea un 'stateflow' para comunicar el estado de guardar/eliminar, igual que en los demas ViewModels.
    private val _operationStatus = MutableStateFlow<Result<String>?>(null)
    val operationStatus: StateFlow<Result<String>?> = _operationStatus.asStateFlow()

    // true hasta la primera emision (o error), para que la UI distinga "cargando" de "sin
    // recurrentes creadas" (antes se veian igual: lista vacia en ambos casos).
    private val _isLoading = MutableStateFlow(true)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    // este 'stateflow' expone la lista de todas las transacciones recurrentes desde el repositorio.
    val recurringTransactions: StateFlow<List<RecurringTransaction>> = repository.allRecurringTransactions
        .onEach { _isLoading.value = false }
        // se añade un bloque 'catch' para atrapar y registrar cualquier error que ocurra en el flujo.
        .catch { e ->
            Log.e("RecurringVM", "Error en el flujo de transacciones recurrentes", e)
            _isLoading.value = false
            emit(emptyList()) // si hay un error, se emite una lista vacia.
        }
        // se convierte el flujo en un 'stateflow' que se mantiene activo mientras haya observadores.
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), emptyList())

    // esta funcion obtiene una transaccion recurrente especifica por su id.
    // usa un 'callback' para devolver el resultado de forma asincrona.
    fun getRecurringTransactionById(id: Int, callback: (RecurringTransaction?) -> Unit) {
        viewModelScope.launch {
            try {
                // se llama al repositorio y el resultado se pasa al callback.
                callback(repository.getById(id))
            } catch (e: Exception) {
                // si hay un error, se registra y se devuelve nulo a traves del callback.
                Log.e("RecurringVM", "Error al obtener transacción recurrente por ID", e)
                callback(null)
            }
        }
    }

    // esta funcion se encarga de añadir o actualizar una transaccion recurrente.
    fun addOrUpdateRecurringTransaction(
        id: Int? = null,
        title: String,
        amount: Double,
        categoryId: Int?,
        recurrenceType: RecurrenceType,
        dayOfMonth: Int,
        startDate: LocalDate,
        endDate: LocalDate?,
        isActive: Boolean
    ) {
        // se inicia una corutina.
        viewModelScope.launch {
            _operationStatus.value = Result.Loading
            try {
                // se realizan validaciones sobre los datos de entrada.
                if (title.isBlank()) throw IllegalArgumentException("El título no puede estar vacío.")
                if (amount <= 0) throw IllegalArgumentException("El monto debe ser mayor a cero.")
                if (dayOfMonth !in 1..31) throw IllegalArgumentException("Día del mes inválido.")

                // se crea el objeto 'entity' que se guardara en la base de datos.
                val formatter = DateTimeFormatter.ISO_LOCAL_DATE
                val entity = RecurringTransactionEntity(
                    id = id ?: 0, // si el id es nulo, se le asigna 0 para que room lo autogenere.
                    title = title.capitalizeFirst(),
                    amount = amount,
                    categoryId = categoryId,
                    recurrenceType = recurrenceType,
                    dayOfMonth = dayOfMonth,
                    startDate = startDate.format(formatter),
                    endDate = endDate?.format(formatter),
                    // se calcula la primera fecha de vencimiento.
                    nextDueDate = calculateNextDueDate(startDate, dayOfMonth).format(formatter),
                    isActive = isActive
                )

                // se comprueba si es una nueva transaccion o una actualizacion.
                if (id == null) {
                    repository.insert(entity)
                } else {
                    repository.update(entity)
                }
                _operationStatus.value = Result.Success("Transacción recurrente guardada")
            } catch (e: Exception) {
                // si hay un error, se registra y se actualiza el estado.
                Log.e("RecurringVM", "Error al guardar transacción recurrente", e)
                _operationStatus.value = Result.Error(e.message ?: "Error inesperado.")
            }
        }
    }

    // esta funcion elimina una transaccion recurrente.
    fun deleteRecurringTransaction(recurringTransaction: RecurringTransaction) {
        viewModelScope.launch {
            _operationStatus.value = Result.Loading
            try {
                repository.delete(recurringTransaction)
                _operationStatus.value = Result.Success("Transacción recurrente eliminada")
            } catch (e: Exception) {
                Log.e("RecurringVM", "Error al eliminar transacción recurrente", e)
                _operationStatus.value = Result.Error(e.message ?: "No se pudo eliminar la transacción recurrente.")
            }
        }
    }

    fun clearOperationStatus() {
        _operationStatus.value = null
    }

    // esta funcion inicia el proceso de verificar y crear las transacciones vencidas.
    // si se proceso alguna, se notifica: antes el balance cambiaba en silencio y la fecha de
    // "proximo vencimiento" no tenia ninguna consecuencia visible para el usuario.
    fun processDueRecurringTransactions() {
        viewModelScope.launch {
            try {
                val processed = repository.processDueRecurringTransactions()
                if (processed.isNotEmpty()) {
                    RecurringNotificationHelper.notifyProcessed(getApplication(), processed)
                }
            } catch (e: Exception) {
                Log.e("RecurringVM", "Error al procesar transacciones recurrentes debidas", e)
            }
        }
    }

    // esta funcion privada calcula la siguiente fecha de vencimiento a partir de una fecha dada.
    private fun calculateNextDueDate(fromDate: LocalDate, dayOfMonth: Int): LocalDate {
        var yearMonth = YearMonth.from(fromDate)
        // se comprueba si el dia de vencimiento de este mes ya paso.
        if (fromDate.dayOfMonth >= dayOfMonth) {
            // si ya paso, se avanza al siguiente mes.
            yearMonth = yearMonth.plusMonths(1)
        }
        // se ajusta el dia por si el proximo mes tiene menos dias.
        val targetDay = dayOfMonth.coerceAtMost(yearMonth.lengthOfMonth())
        return yearMonth.atDay(targetDay)
    }
}