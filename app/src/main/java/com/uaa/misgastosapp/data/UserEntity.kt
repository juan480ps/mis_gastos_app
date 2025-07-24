// UserEntity

package com.uaa.misgastosapp.data

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

// con la anotacion @entity se le indica a room que esta clase representa una tabla en la base de datos.
@Entity(
    // aca se le da el nombre "users" a la tabla.
    tableName = "users",
    // aca se definen los indices, que ayudan a que las busquedas sean mas rapidas y a evitar duplicados.
    indices = [Index(value = ["email"], unique = true)] // se crea un indice en la columna "email" y se marca como unico para que no haya dos usuarios con el mismo correo.
)
// se define una 'data class' para representar la estructura de un usuario.
data class UserEntity(
    // se indica que 'id' es la llave primaria. 'autogenerate' hace que el id se cree automaticamente.
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    // esta columna guarda el correo electronico del usuario.
    val email: String,
    // esta columna guarda la contraseña del usuario.
    val password: String,
    // esta columna guarda el nombre del usuario.
    val name: String,
    // esta columna guarda la fecha de creacion del usuario como texto.
    val createdAt: String
)