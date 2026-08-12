// CategoryRepository.kt

package com.uaa.misgastosapp.data.repository

import com.uaa.misgastosapp.data.CategoryDao
import com.uaa.misgastosapp.data.CategoryEntity
import com.uaa.misgastosapp.model.Category
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

// esta clase es la encargada de manejar la logica de las categorias.
// sirve como un intermediario entre la base de datos (dao) y la interfaz de usuario.
class CategoryRepository(private val categoryDao: CategoryDao) {

    // aca se define una variable que contiene una lista de todas las categorias.
    // esta lista se actualiza automaticamente gracias al uso de 'flow'.
    val allCategories: Flow<List<Category>> = categoryDao.getAll()
        .map { entities -> // el '.map' se usa para transformar los datos.
            // se convierte cada 'categoryentity' (formato de la base de datos) a un objeto 'category' (formato usado en la app).
            entities.map { Category(id = it.id, name = it.name) }
        }

    // esta es una funcion suspendida para insertar una nueva categoria.
    suspend fun insertCategory(name: String): Long {
        // se obtiene la lista actual de categorias para poder revisarla. 'first()' obtiene el primer valor emitido por el flow.
        val existingCategories = allCategories.first()
        // se comprueba si ya existe una categoria con el mismo nombre, sin importar si esta en mayusculas o minusculas.
        if (existingCategories.any { it.name.equals(name, ignoreCase = true) }) {
            // si la categoria ya existe, se lanza un error para evitar duplicados.
            throw IllegalStateException("La categoría '$name' ya existe.")
        }
        // si no existe, se crea la entidad de la categoria y se inserta en la base de datos.
        return categoryDao.insert(CategoryEntity(name = name))
    }

    // esta es una funcion suspendida para renombrar una categoria existente.
    suspend fun updateCategory(id: Int, newName: String) {
        val existingCategories = allCategories.first()
        // se permite que coincida con su propio nombre actual (no es un duplicado real).
        if (existingCategories.any { it.id != id && it.name.equals(newName, ignoreCase = true) }) {
            throw IllegalStateException("La categoría '$newName' ya existe.")
        }
        categoryDao.update(id, newName)
    }

    // esta es una funcion suspendida para borrar una categoria.
    suspend fun deleteCategory(category: Category) {
        // se convierte el objeto 'category' (del modelo de la app) a un 'categoryentity' (de la base de datos).
        val entity = CategoryEntity(id = category.id, name = category.name)
        // se llama a la funcion de borrar en el dao, pasandole la entidad correspondiente.
        categoryDao.delete(entity)
    }
}