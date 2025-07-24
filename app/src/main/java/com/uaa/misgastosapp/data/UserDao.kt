// UserDao

package com.uaa.misgastosapp.data

import androidx.room.*

// con la anotacion @dao se le indica a room que esta es una interfaz para acceder a los datos.
@Dao
// aca se define la interfaz 'userdao', que contiene las operaciones para la tabla de usuarios.
interface UserDao {
    // esta anotacion se usa para insertar. si el usuario ya existe, la operacion se cancela.
    @Insert(onConflict = OnConflictStrategy.ABORT)
    // es una funcion suspendida que inserta un nuevo usuario. devuelve el id de la fila creada.
    suspend fun insert(user: UserEntity): Long

    // aca se define una consulta para verificar las credenciales de un usuario.
    @Query("SELECT * FROM users WHERE email = :email AND password = :password LIMIT 1")
    // es una funcion suspendida para el inicio de sesion. devuelve el usuario si el email y la contraseña son correctos, o nulo si no lo son.
    suspend fun login(email: String, password: String): UserEntity?

    // aca se define una consulta para buscar un usuario por su correo electronico.
    @Query("SELECT * FROM users WHERE email = :email LIMIT 1")
    // es una funcion suspendida que devuelve un usuario si se encuentra el email, o nulo si no existe.
    suspend fun getUserByEmail(email: String): UserEntity?

    // aca se define una consulta para buscar un usuario por su id.
    @Query("SELECT * FROM users WHERE id = :userId LIMIT 1")
    // es una funcion suspendida que devuelve el usuario correspondiente a un id.
    suspend fun getUserById(userId: Int): UserEntity?

    // esta anotacion se usa para actualizar una fila existente en la tabla.
    @Update
    // es una funcion suspendida que actualiza los datos de un usuario.
    suspend fun update(user: UserEntity)
}