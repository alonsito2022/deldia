package com.sys4soft.deldia.retrofit

import android.content.Context
import com.sys4soft.deldia.localdatabase.Preference
import okhttp3.Interceptor
import okhttp3.Response

class AuthInterceptor(private val context: Context) : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val original = chain.request()
        val requestBuilder = original.newBuilder()
            .header("Accept", "application/json")
            .header("Content-Type", "application/json")

        // Obtener el token de las preferencias
        val preference = Preference(context)
        val token = preference.getData("TOKEN")

        // Agregar token si existe
        if (token.isNotEmpty()) {
            requestBuilder.header("Authorization", "Bearer $token")
        }

        val request = requestBuilder.build()
        return chain.proceed(request)
    }
}