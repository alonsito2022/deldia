package com.sys4soft.deldia.retrofit

import android.content.Context
import android.content.Intent
import com.sys4soft.deldia.LoginActivity
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
        val response = chain.proceed(request)

        // Manejar el vencimiento del token (401 Unauthorized)
        if (response.code == 401) {
            // Limpiar el token para obligar a un nuevo login
            preference.saveData("TOKEN", "")

            // Redirigir al usuario al LoginActivity
            val intent = Intent(context, LoginActivity::class.java)
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
            context.startActivity(intent)
        }

        return response
    }
}