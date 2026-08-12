// CategoryViewModel

package com.uaa.misgastosapp.ui.viewmodel

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.uaa.misgastosapp.data.repository.AppRepositories
import com.uaa.misgastosapp.data.repository.CategoryRepository
import com.uaa.misgastosapp.model.Category
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

// aca se define el viewmodel para las categorias. se encarga de la logica de negocio para crear, ver y eliminar categorias.
class CategoryViewModel(application: Application) : AndroidViewModel(application) {
    // se declara el repositorio de categorias, que sera la unica fuente de datos.
    private val repository: CategoryRepository

    // el bloque 'init' se ejecuta cuando se crea una instancia del viewmodel.
    init {
        repository = AppRepositories.categoryRepository(application)
    }

    // se crea un 'stateflow' para comunicar el estado de una operacion (como borrar).
    // es privado para que solo el viewmodel lo pueda modificar.
    private val _operationStatus = MutableStateFlow<Result<String>?>(null)
    // esta es la version publica y de solo lectura para que la interfaz observe el estado de la operacion.
    val operationStatus: StateFlow<Result<String>?> = _operationStatus.asStateFlow()

    // true hasta que llega la primera emision (o un error). asi la UI puede distinguir "cargando"
    // de "no hay categorias creadas", que antes se veian exactamente igual (lista vacia en ambos casos).
    private val _isLoading = MutableStateFlow(true)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    // este 'stateflow' expone la lista de todas las categorias desde el repositorio.
    // la interfaz lo observara para mostrar las categorias.
    val categories: StateFlow<List<Category>> = repository.allCategories
        .onEach { _isLoading.value = false }
        .catch { e ->
            Log.e("CategoryVM", "Error en el flujo de categorías", e)
            _isLoading.value = false
            emit(emptyList())
        }
        // se convierte el flujo en un 'stateflow' que se mantiene activo mientras haya observadores y 5 segundos mas.
        // su valor inicial es una lista vacia.
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), emptyList())

    // id de la ultima categoria creada con exito, para que la pantalla que abrio "Añadir
    // categoría..." desde un combo pueda auto-seleccionarla al volver.
    private val _lastCreatedCategoryId = MutableStateFlow<Int?>(null)
    val lastCreatedCategoryId: StateFlow<Int?> = _lastCreatedCategoryId.asStateFlow()

    // esta funcion se encarga de añadir una nueva categoria. usa el mismo operationStatus que
    // deleteCategory (antes usaba callbacks onSuccess/onError, inconsistente con el resto del VM).
    fun addCategory(name: String) {
        viewModelScope.launch {
            _operationStatus.value = Result.Loading
            try {
                // se valida que el nombre de la categoria no este vacio.
                if (name.isBlank()) {
                    throw IllegalArgumentException("El nombre de la categoría no puede estar vacío.")
                }
                val capitalizedName = name.capitalizeFirst()
                // se llama al repositorio para que inserte la categoria.
                val newId = repository.insertCategory(capitalizedName)
                _lastCreatedCategoryId.value = newId.toInt()
                _operationStatus.value = Result.Success("Categoría '$capitalizedName' añadida")
            } catch (e: Exception) {
                // si ocurre un error, se registra y se actualiza el estado.
                Log.e("CategoryVM", "Error al agregar categoría", e)
                _operationStatus.value = Result.Error(e.message ?: "Error inesperado.")
            }
        }
    }

    // esta funcion se encarga de renombrar una categoria existente.
    fun updateCategory(category: Category, newName: String) {
        viewModelScope.launch {
            _operationStatus.value = Result.Loading
            try {
                if (newName.isBlank()) {
                    throw IllegalArgumentException("El nombre de la categoría no puede estar vacío.")
                }
                val capitalizedName = newName.capitalizeFirst()
                repository.updateCategory(category.id, capitalizedName)
                _operationStatus.value = Result.Success("Categoría renombrada a '$capitalizedName'")
            } catch (e: Exception) {
                Log.e("CategoryVM", "Error al renombrar categoría", e)
                _operationStatus.value = Result.Error(e.message ?: "Error inesperado.")
            }
        }
    }

    // esta funcion se encarga de eliminar una categoria.
    fun deleteCategory(category: Category) {
        // se inicia una corutina.
        viewModelScope.launch {
            try {
                // se actualiza el estado a 'cargando' para que la interfaz pueda mostrar un indicador.
                _operationStatus.value = Result.Loading
                // se llama al repositorio para que elimine la categoria.
                repository.deleteCategory(category)
                // si la eliminacion es exitosa, se actualiza el estado a 'exito' con un mensaje.
                _operationStatus.value = Result.Success("Categoría '${category.name}' eliminada.")
            } catch (e: Exception) {
                // si ocurre un error (por ejemplo, porque la categoria esta en uso), se actualiza el estado a 'error'.
                Log.e("CategoryVM", "Error al eliminar categoría", e)
                _operationStatus.value = Result.Error("No se pudo eliminar la categoría. Asegúrate de que no esté en uso por transacciones, presupuestos o gastos recurrentes.")
            }
        }
    }

    // esta funcion se usa para limpiar el estado de la operacion, por ejemplo, despues de que el usuario ha visto el mensaje de exito o error.
    fun clearOperationStatus() {
        _operationStatus.value = null
    }
}