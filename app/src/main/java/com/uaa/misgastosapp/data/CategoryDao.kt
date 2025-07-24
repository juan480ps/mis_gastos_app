// CategoryDao

package com.uaa.misgastosapp.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

// con la anotacion @dao se le indica a room que esta es una interfaz para acceder a los datos.
@Dao
// aca se define la interfaz 'CategoryDao', que contiene las operaciones para la tabla de categorias.
interface CategoryDao {
    // esta anotacion se usa para insertar. si la categoria ya existe, se ignora la operacion.
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    // es una funcion suspendida que inserta una categoria y devuelve el id de la nueva fila.
    suspend fun insert(category: CategoryEntity): Long

    // aca se define una consulta para obtener todas las categorias, ordenadas por nombre.
    @Query("SELECT * FROM categories ORDER BY name ASC")
    // esta funcion devuelve una lista de todas las categorias como un 'flow'.
    // esto permite que la lista se actualice sola si hay cambios en la base de datos.
    fun getAll(): Flow<List<CategoryEntity>>

    // aca se define una consulta para obtener una sola categoria usando su id.
    @Query("SELECT * FROM categories WHERE id = :id LIMIT 1")
    // es una funcion suspendida que busca una categoria especifica. puede devolver nulo si no la encuentra.
    suspend fun getById(id: Int): CategoryEntity?

    // aca se define una consulta para obtener solo el nombre de una categoria a partir de su id.
    @Query("SELECT name FROM categories WHERE id = :id LIMIT 1")
    // es una funcion suspendida que devuelve el nombre de la categoria como texto.
    suspend fun getCategoryNameById(id: Int): String?

    // esta anotacion se usa para borrar una fila de la tabla.
    @Delete
    // es una funcion suspendida que borra una categoria. room sabe como encontrarla usando la llave primaria del objeto.
    suspend fun delete(category: CategoryEntity)

    // aca se define una consulta para actualizar el nombre de una categoria.
    @Query("UPDATE categories SET name = :newName WHERE id = :id")
    // es una funcion suspendida que actualiza una categoria especifica usando su id.
    suspend fun update(id: Int, newName: String)
}