// GastosApiService

package com.uaa.misgastosapp.network

import com.uaa.misgastosapp.model.LoginRequest
import com.uaa.misgastosapp.model.LoginResponse
import com.uaa.misgastosapp.model.LogoutResponse
import com.uaa.misgastosapp.model.ProfileResponse
import com.uaa.misgastosapp.model.RegisterRequest
import com.uaa.misgastosapp.model.RegisterResponse
import retrofit2.Response
import retrofit2.http.*

// aca se define la interfaz que contiene todas las llamadas a la api del servidor.
// retrofit usara esta interfaz para generar el codigo necesario para comunicarse con la red.
interface GastosApiService {
    // se define una peticion de tipo 'post' a la ruta 'register' del servidor.
    @POST("register")
    // es una funcion suspendida para registrar un nuevo usuario. se envia un objeto 'registerrequest' en el cuerpo de la peticion y se espera una respuesta de tipo 'registerresponse'.
    suspend fun register(@Body request: RegisterRequest): Response<RegisterResponse>

    // se define una peticion de tipo 'post' a la ruta 'login'.
    @POST("login")
    // es una funcion suspendida para iniciar sesion. se envia un 'loginrequest' y se espera un 'loginresponse'.
    suspend fun login(@Body request: LoginRequest): Response<LoginResponse>

    // se define una peticion de tipo 'get' a la ruta 'profile'.
    @GET("profile")
    // es una funcion suspendida para obtener los datos del perfil del usuario. se espera un 'profileresponse'.
    suspend fun getProfile(): Response<ProfileResponse>

    // se define una peticion de tipo 'post' a la ruta 'logout'.
    @POST("logout")
    // es una funcion suspendida para cerrar la sesion en el servidor. se espera un 'logoutresponse'.
    suspend fun logout(): Response<LogoutResponse>
}