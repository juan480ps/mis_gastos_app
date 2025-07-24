// AuthModels

package com.uaa.misgastosapp.model

import com.google.gson.annotations.SerializedName

// aca se define el modelo de datos para la solicitud de registro.
// se usa para estructurar la informacion que se envia al servidor al crear una cuenta.
data class RegisterRequest(
    // se usa '@serializedname' para indicar que en el json del servidor, este campo se llama "full_name".
    @SerializedName("full_name") val fullName: String,
    // se guarda el correo electronico del nuevo usuario.
    val email: String,
    // se guarda el nombre de usuario para el registro.
    val username: String,
    // se guarda la contraseña para la nueva cuenta.
    val password: String
)

// aca se define el modelo para la solicitud de inicio de sesion.
data class LoginRequest(
    // se guarda el identificador del usuario, que puede ser su email o nombre de usuario.
    val identifier: String,
    // se guarda la contraseña para el inicio de sesion.
    val password: String
)

// aca se define el modelo para la respuesta del servidor al registrarse.
data class RegisterResponse(
    // se guarda el mensaje de confirmacion recibido del servidor.
    val msg: String
)

// aca se define el modelo para la respuesta del servidor al iniciar sesion.
data class LoginResponse(
    // se usa '@serializedname' porque el servidor envia este campo como "access_token".
    @SerializedName("access_token") val accessToken: String
)

// aca se define el modelo para la respuesta del servidor al solicitar el perfil del usuario.
data class ProfileResponse(
    // se guarda el id del usuario.
    val id: Int,
    // se mapea el campo "full_name" del json a esta variable.
    @SerializedName("full_name") val fullName: String,
    // se guarda el correo electronico del usuario.
    val email: String,
    // se guarda el nombre de usuario.
    val username: String,
    // se mapea el campo "created_at" del json.
    @SerializedName("created_at") val createdAt: String,
    // se mapea el campo "is_confirmed" para saber si el usuario ha confirmado su cuenta.
    @SerializedName("is_confirmed") val isConfirmed: Boolean
)

// aca se define el modelo para la respuesta del servidor al cerrar sesion.
data class LogoutResponse(
    // se guarda el mensaje de confirmacion del cierre de sesion.
    val msg: String
)

// aca se define un modelo generico para las respuestas de error del servidor.
data class ErrorResponse(
    // se guarda el mensaje de error principal, puede ser nulo.
    val msg: String?,
    // se guarda un mensaje de error mas tecnico, puede ser nulo.
    val error: String?
)