// TransactionViewModel

package com.uaa.misgastosapp.ui.viewmodel

import android.app.Application
import android.os.Build
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.uaa.misgastosapp.data.repository.AppRepositories
import com.uaa.misgastosapp.data.repository.TransactionRepository
import com.uaa.misgastosapp.model.Transaction
import com.uaa.misgastosapp.utils.Result
import com.uaa.misgastosapp.utils.capitalizeFirst
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDate

// se asegura que este codigo solo se ejecute en versiones de android compatibles.
@RequiresApi(Build.VERSION_CODES.O)
// aca se define el viewmodel para las transacciones (ingresos y gastos).
// el repositorio se puede inyectar (para tests); @JvmOverloads genera el constructor de un solo
// parametro que necesita el ViewModelProvider por defecto para instanciarlo en produccion.
class TransactionViewModel @JvmOverloads constructor(
    application: Application,
    private val repository: TransactionRepository = AppRepositories.transactionRepository(application)
) : AndroidViewModel(application) {

    // se crea un 'stateflow' para comunicar el estado de una operacion (como agregar o borrar).
    // es privado para que solo el viewmodel lo pueda modificar.
    private val _operationStatus = MutableStateFlow<Result<String>?>(null)
    // esta es la version publica y de solo lectura para que la interfaz observe el estado.
    val operationStatus: StateFlow<Result<String>?> = _operationStatus.asStateFlow()

    // este 'stateflow' expone la lista de todas las transacciones desde el repositorio.
    val transactions: StateFlow<List<Transaction>> = repository.allTransactions
        .distinctUntilChanged() // evita recomposiciones si los datos no cambiaron
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), emptyList())

    // esta funcion se encarga de añadir una nueva transaccion.
    // accountId es opcional: asociar una cuenta/banco nunca es obligatorio.
    fun addTransaction(title: String, amount: Double, date: String, categoryId: Int?, accountId: Int? = null) {
        // se inicia una corutina para no bloquear la interfaz.
        viewModelScope.launch {
            try {
                // se actualiza el estado a 'cargando'.
                _operationStatus.value = Result.Loading
                // se llama al repositorio para que inserte la transaccion.
                repository.insertTransaction(title.capitalizeFirst(), amount, date, categoryId, accountId)
                // si es exitoso, se actualiza el estado a 'exito' con un mensaje.
                _operationStatus.value = Result.Success("Transacción agregada exitosamente")
            } catch (e: Exception) {
                // si hay un error, se registra y se actualiza el estado a 'error'.
                Log.e("TransactionVM", "Error al agregar transacción", e)
                _operationStatus.value = Result.Error("Error al agregar transacción: ${e.message}")
            }
        }
    }

    // esta funcion se encarga de actualizar una transaccion existente.
    fun updateTransaction(id: Int, title: String, amount: Double, date: String, categoryId: Int?, accountId: Int?) {
        viewModelScope.launch {
            try {
                _operationStatus.value = Result.Loading
                repository.updateTransaction(id, title.capitalizeFirst(), amount, date, categoryId, accountId)
                _operationStatus.value = Result.Success("Transacción actualizada")
            } catch (e: Exception) {
                Log.e("TransactionVM", "Error al actualizar transacción", e)
                _operationStatus.value = Result.Error("Error al actualizar transacción: ${e.message}")
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