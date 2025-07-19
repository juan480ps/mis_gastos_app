// CategoryRepository.kt

package com.uaa.misgastosapp.data.repository

import com.uaa.misgastosapp.data.CategoryDao
import com.uaa.misgastosapp.data.CategoryEntity
import com.uaa.misgastosapp.model.Category
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

class CategoryRepository(private val categoryDao: CategoryDao) {

    fun allCategories(userId: Int): Flow<List<Category>> = categoryDao.getAll(userId)
        .map { entities ->
            entities.map { Category(id = it.id, name = it.name) }
        }

    suspend fun insertCategory(name: String, userId: Int): Long {
        val existingCategories = allCategories(userId).first()
        if (existingCategories.any { it.name.equals(name, ignoreCase = true) }) {
            throw IllegalStateException("La categoría '$name' ya existe.")
        }
        return categoryDao.insert(CategoryEntity(name = name, userId = userId))
    }

    suspend fun deleteCategory(category: Category) {
        // userId is not needed here as Room deletes by primary key (id) from the entity object
        val entity = CategoryEntity(id = category.id, name = category.name, userId = 0)
        categoryDao.delete(entity)
    }
}