// CategoryEntity

package com.uaa.misgastosapp.data

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

// con la anotacion @entity se le indica a room que esta clase representa una tabla en la base de datos.
@Entity(
    // aca se le da el nombre "categories" a la tabla.
    tableName = "categories",
    // aca se definen los indices, que ayudan a que las busquedas sean mas rapidas y a evitar duplicados.
    indices = [Index(value = ["name"], unique = true)] // se crea un indice en la columna "name" y se marca como unico para que no haya dos categorias con el mismo nombre.
)
// se define una 'data class' para representar la estructura de una categoria.
data class CategoryEntity(
    // se indica que 'id' es la llave primaria. 'autogenerate' hace que el id se cree automaticamente.
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    // esta columna guarda el nombre de la categoria.
    val name: String
)