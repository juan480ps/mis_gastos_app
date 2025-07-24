// Network Module

package com.uaa.misgastosapp.network

import android.util.Log
import com.uaa.misgastosapp.utils.SecureSessionManager
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

// se crea un objeto 'singleton', lo que significa que solo habra una instancia de este modulo en toda la aplicacion.
// se encarga de toda la configuracion de la red.
object NetworkModule {
    // aca se define la direccion base del servidor. '10.0.2.2' es una direccion especial que usan los emuladores de android para conectarse al 'localhost' de la computadora.
    private const val BASE_URL = "http://10.0.2.2:5000/api/"
    // aca se guardara una referencia al gestor de sesiones seguras.
    private lateinit var sessionManager: SecureSessionManager

    // se declara la variable que contendra la instancia de retrofit.
    // 'volatile' se usa para que los cambios en esta variable sean visibles para todos los hilos de ejecucion inmediatamente.
    @Volatile
    private var retrofit: Retrofit? = null

    // esta es la propiedad publica que se usara para hacer las llamadas a la api.
    val apiService: GastosApiService
        get() {
            // si se intenta usar antes de inicializar el modulo, se lanza un error para avisar.
            return retrofit?.create(GastosApiService::class.java)
                ?: throw IllegalStateException("NetworkModule not initialized")
        }

    // esta es la funcion que se debe llamar al inicio de la app para configurar todo.
    fun initialize(sessionManager: SecureSessionManager) {
        // se usa 'synchronized' para asegurar que esta configuracion no sea interrumpida por otro hilo.
        synchronized(this) {
            // se guarda la referencia al gestor de sesiones.
            this.sessionManager = sessionManager
            // se llama a la funcion que construye el objeto retrofit.
            createRetrofit()
            Log.d("NetworkModule", "Initialized with base URL: $BASE_URL")
        }
    }

    // esta funcion privada se encarga de construir y configurar la instancia de retrofit.
    private fun createRetrofit() {
        // se crea un interceptor para registrar en la consola toda la informacion de las llamadas de red. es muy util para depurar.
        val loggingInterceptor = HttpLoggingInterceptor { message ->
            Log.d("OkHttp", message)
        }.apply {
            level = HttpLoggingInterceptor.Level.BODY // se configura para que muestre el cuerpo de la solicitud y la respuesta.
        }

        // se crea un interceptor para añadir el token de autenticacion a las cabeceras de las solicitudes.
        val authInterceptor = Interceptor { chain ->
            val original = chain.request()
            val requestBuilder = original.newBuilder()
                .header("Content-Type", "application/json") // se añade una cabecera estandar.

            // se obtiene la ruta de la solicitud para no añadir el token a las llamadas de 'login' o 'register'.
            val path = original.url.encodedPath
            if (!path.contains("login") && !path.contains("register")) {
                // se obtiene el token de acceso desde el gestor de sesiones.
                sessionManager.getAccessToken()?.let { token ->
                    // si el token no es el de "modo offline", se añade a la cabecera de autorizacion.
                    if (token != "offline_mode") {
                        Log.d("NetworkModule", "Adding token to request: Bearer $token")
                        requestBuilder.header("Authorization", "Bearer $token")
                    }
                } ?: Log.d("NetworkModule", "No token available for: $path")
            }

            val request = requestBuilder.build()
            Log.d("NetworkModule", "Request URL: ${request.url}")
            Log.d("NetworkModule", "Headers: ${request.headers}")

            try {
                // se envia la solicitud a la red.
                chain.proceed(request)
            } catch (e: Exception) {
                // si falla, se registra el error.
                Log.e("NetworkModule", "Request failed: ${e.message}")
                throw e
            }
        }

        // se construye el cliente http (okhttp) añadiendo los interceptores y configurando los tiempos de espera.
        val okHttpClient = OkHttpClient.Builder()
            .addInterceptor(authInterceptor)
            .addInterceptor(loggingInterceptor)
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .retryOnConnectionFailure(false)
            .cache(null) // se deshabilita la cache.
            .build()

        // se construye la instancia de retrofit con la url base, el cliente http y un conversor de json.
        retrofit = Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
    }

    // esta funcion se usa para forzar la recreacion del cliente de retrofit.
    fun updateApiClient() {
        synchronized(this) {
            Log.d("NetworkModule", "Updating ApiClient (re-creating Retrofit stack).")
            retrofit = null
            createRetrofit()
        }
    }

    // esta funcion tambien recrea el cliente, se usa especificamente despues de un cierre de sesion para limpiar el estado.
    fun clearAuthentication() {
        synchronized(this) {
            Log.d("NetworkModule", "Clearing authentication (re-creating Retrofit stack).")
            retrofit = null
            createRetrofit()
        }
    }
}