// AccountViewModel

package com.uaa.misgastosapp.ui.viewmodel

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.uaa.misgastosapp.data.repository.AccountRepository
import com.uaa.misgastosapp.data.repository.AppRepositories
import com.uaa.misgastosapp.model.Account
import com.uaa.misgastosapp.utils.Result
import com.uaa.misgastosapp.utils.capitalizeFirst
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

// viewmodel para las cuentas/bancos opcionales. Separar transacciones por cuenta nunca es
// obligatorio: un usuario que nunca crea cuentas sigue viendo todo en la bolsa unica de siempre.
class AccountViewModel @JvmOverloads constructor(
    application: Application,
    private val repository: AccountRepository = AppRepositories.accountRepository(application)
) : AndroidViewModel(application) {

    private val _operationStatus = MutableStateFlow<Result<String>?>(null)
    val operationStatus: StateFlow<Result<String>?> = _operationStatus.asStateFlow()

    // true hasta la primera emision (o error), para distinguir "cargando" de "no hay cuentas".
    private val _isLoading = MutableStateFlow(true)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    val accounts: StateFlow<List<Account>> = repository.allAccounts
        .onEach { _isLoading.value = false }
        .catch { e ->
            Log.e("AccountVM", "Error en el flujo de cuentas", e)
            _isLoading.value = false
            emit(emptyList())
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), emptyList())

    // id de la ultima cuenta creada con exito, para que la pantalla que abrio "Añadir cuenta..."
    // desde un combo pueda auto-seleccionarla al volver.
    private val _lastCreatedAccountId = MutableStateFlow<Int?>(null)
    val lastCreatedAccountId: StateFlow<Int?> = _lastCreatedAccountId.asStateFlow()

    fun addAccount(name: String, bankName: String?, colorHex: String) {
        viewModelScope.launch {
            _operationStatus.value = Result.Loading
            try {
                if (name.isBlank()) {
                    throw IllegalArgumentException("El nombre de la cuenta no puede estar vacío.")
                }
                val capitalizedName = name.capitalizeFirst()
                val capitalizedBankName = bankName?.takeIf { it.isNotBlank() }?.capitalizeFirst()
                val newId = repository.insertAccount(capitalizedName, capitalizedBankName, colorHex)
                _lastCreatedAccountId.value = newId.toInt()
                _operationStatus.value = Result.Success("Cuenta '$capitalizedName' añadida")
            } catch (e: Exception) {
                Log.e("AccountVM", "Error al agregar cuenta", e)
                _operationStatus.value = Result.Error(e.message ?: "Error inesperado.")
            }
        }
    }

    fun updateAccount(id: Int, name: String, bankName: String?, colorHex: String) {
        viewModelScope.launch {
            _operationStatus.value = Result.Loading
            try {
                if (name.isBlank()) {
                    throw IllegalArgumentException("El nombre de la cuenta no puede estar vacío.")
                }
                val capitalizedName = name.capitalizeFirst()
                val capitalizedBankName = bankName?.takeIf { it.isNotBlank() }?.capitalizeFirst()
                repository.updateAccount(id, capitalizedName, capitalizedBankName, colorHex)
                _operationStatus.value = Result.Success("Cuenta '$capitalizedName' actualizada")
            } catch (e: Exception) {
                Log.e("AccountVM", "Error al actualizar cuenta", e)
                _operationStatus.value = Result.Error(e.message ?: "Error inesperado.")
            }
        }
    }

    fun deleteAccount(account: Account) {
        viewModelScope.launch {
            try {
                _operationStatus.value = Result.Loading
                repository.deleteAccount(account)
                _operationStatus.value = Result.Success("Cuenta '${account.name}' eliminada. Sus transacciones quedaron sin cuenta asignada.")
            } catch (e: Exception) {
                Log.e("AccountVM", "Error al eliminar cuenta", e)
                _operationStatus.value = Result.Error("No se pudo eliminar la cuenta.")
            }
        }
    }

    fun clearOperationStatus() {
        _operationStatus.value = null
    }
}
