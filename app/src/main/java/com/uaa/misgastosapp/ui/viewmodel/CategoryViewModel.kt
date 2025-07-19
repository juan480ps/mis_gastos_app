// CategoryViewModel

package com.uaa.misgastosapp.ui.viewmodel

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.uaa.misgastosapp.data.AppDatabase
import com.uaa.misgastosapp.data.repository.CategoryRepository
import com.uaa.misgastosapp.model.Category
import com.uaa.misgastosapp.utils.Result
import com.uaa.misgastosapp.utils.SecureSessionManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class CategoryViewModel(application: Application) : AndroidViewModel(application) {
    private val repository: CategoryRepository
    private val sessionManager = SecureSessionManager(application)

    init {
        val db = AppDatabase.getInstance(application)
        repository = CategoryRepository(db.categoryDao())
    }

    private val _operationStatus = MutableStateFlow<Result<String>?>(null)
    val operationStatus: StateFlow<Result<String>?> = _operationStatus.asStateFlow()

    val categories: StateFlow<List<Category>> = sessionManager.userIdFlow
        .flatMapLatest { userId ->
            repository.allCategories(userId)
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), emptyList())

    fun addCategory(name: String, onSuccess: () -> Unit, onError: (String) -> Unit) {
        viewModelScope.launch {
            try {
                if (name.isBlank()) {
                    throw IllegalArgumentException("El nombre de la categoría no puede estar vacío.")
                }
                val userId = sessionManager.getUserId()
                if (userId == 0) throw IllegalStateException("Usuario no autenticado.")
                repository.insertCategory(name, userId)
                onSuccess()
            } catch (e: Exception) {
                Log.e("CategoryVM", "Error al agregar categoría", e)
                onError(e.message ?: "Error inesperado.")
            }
        }
    }

    fun deleteCategory(category: Category) {
        viewModelScope.launch {
            try {
                _operationStatus.value = Result.Loading
                repository.deleteCategory(category)
                _operationStatus.value = Result.Success("Categoría '${category.name}' eliminada.")
            } catch (e: Exception) {
                Log.e("CategoryVM", "Error al eliminar categoría", e)
                _operationStatus.value = Result.Error("No se pudo eliminar la categoría. Asegúrate de que no esté en uso por transacciones, presupuestos o gastos recurrentes.")
            }
        }
    }

    fun clearOperationStatus() {
        _operationStatus.value = null
    }
}