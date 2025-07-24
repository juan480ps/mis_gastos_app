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

interface GastosApiService {
    @POST("register")
    suspend fun register(@Body request: RegisterRequest): Response<RegisterResponse>

    @POST("login")
    suspend fun login(@Body request: LoginRequest): Response<LoginResponse>

    @GET("profile")
    suspend fun getProfile(): Response<ProfileResponse>

    @POST("logout")
    suspend fun logout(): Response<LogoutResponse>
}