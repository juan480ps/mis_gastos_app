// TransactionViewModel

package com.uaa.misgastosapp.ui.viewmodel

import android.app.Application
import android.os.Build
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.uaa.misgastosapp.data.AppDatabase
import com.uaa.misgastosapp.data.repository.TransactionRepository
import com.uaa.misgastosapp.model.Transaction
import com.uaa.misgastosapp.utils.Result
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDate

// se asegura que este codigo solo se ejecute en versiones de android compatibles.
@RequiresApi(Build.VERSION_CODES.O)
// aca se define el viewmodel para las transacciones (ingresos y gastos).
class TransactionViewModel(application: Application) : AndroidViewModel(application) {
    // se declara el repositorio de transacciones, que sera la unica fuente de datos.
    private val repository: TransactionRepository

    // el bloque 'init' se ejecuta cuando se crea una instancia del viewmodel.
    init {
        // se obtiene la instancia de la base de datos.
        val db = AppDatabase.getInstance(application)
        // se inicializa el repositorio, pasandole los daos necesarios.
        repository = TransactionRepository(db.transactionDao(), db.categoryDao())
    }

    // se crea un 'stateflow' para comunicar el estado de una operacion (como agregar o borrar).
    // es privado para que solo el viewmodel lo pueda modificar.
    private val _operationStatus = MutableStateFlow<Result<String>?>(null)
    // esta es la version publica y de solo lectura para que la interfaz observe el estado.
    val operationStatus: StateFlow<Result<String>?> = _operationStatus.asStateFlow()

    // este 'stateflow' expone la lista de todas las transacciones desde el repositorio.
    // la interfaz lo observara para mostrar el historial de transacciones.
    val transactions: StateFlow<List<Transaction>> = repository.allTransactions
        // se convierte el flujo en un 'stateflow' que se mantiene activo mientras haya observadores.
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), emptyList())

    // esta funcion se encarga de añadir una nueva transaccion.
    fun addTransaction(title: String, amount: Double, date: String, categoryId: Int?) {
        // se inicia una corutina para no bloquear la interfaz.
        viewModelScope.launch {
            try {
                // se actualiza el estado a 'cargando'.
                _operationStatus.value = Result.Loading
                // se llama al repositorio para que inserte la transaccion.
                repository.insertTransaction(title, amount, date, categoryId)
                // si es exitoso, se actualiza el estado a 'exito' con un mensaje.
                _operationStatus.value = Result.Success("Transacción agregada exitosamente")
            } catch (e: Exception) {
                // si hay un error, se registra y se actualiza el estado a 'error'.
                Log.e("TransactionVM", "Error al agregar transacción", e)
                _operationStatus.value = Result.Error("Error al agregar transacción: ${e.message}")
            }
        }
    }

    // esta funcion se encarga de eliminar una transaccion usando su id.
    fun deleteTransaction(id: Int) {
        viewModelScope.launch {
            try {
                // se actualiza el estado a 'cargando'.
                _operationStatus.value = Result.Loading
                // se llama al repositorio para que elimine la transaccion.
                repository.deleteTransaction(id)
                // si es exitoso, se actualiza el estado a 'exito'.
                _operationStatus.value = Result.Success("Transacción eliminada")
            } catch (e: Exception) {
                // si hay un error, se registra y se actualiza el estado a 'error'.
                Log.e("TransactionVM", "Error al eliminar transacción", e)
                _operationStatus.value = Result.Error("Error al eliminar transacción: ${e.message}")
            }
        }
    }

    // esta funcion se usa para limpiar el estado de la operacion, despues de que el usuario ha visto el mensaje de estado.
    fun clearOperationStatus() {
        _operationStatus.value = null
    }
}