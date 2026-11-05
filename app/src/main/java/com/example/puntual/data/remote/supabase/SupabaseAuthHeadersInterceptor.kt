package com.example.puntual.data.remote.supabase

import okhttp3.Interceptor
import okhttp3.Response
import javax.inject.Inject

class SupabaseAuthHeadersInterceptor @Inject constructor(
    private val config: SupabaseConfig,
) : Interceptor {

    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request().newBuilder()
            .header("apikey", config.publishableKey)
            .header("Authorization", "Bearer ${config.publishableKey}")
            .build()
        return chain.proceed(request)
    }
}
