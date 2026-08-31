package com.example.puntual.data.remote.supabase

import okhttp3.Interceptor
import okhttp3.Response
import javax.inject.Inject

class SupabaseHeadersInterceptor @Inject constructor(
    private val config: SupabaseConfig,
) : Interceptor {

    override fun intercept(chain: Interceptor.Chain): Response {
        val requestBuilder = chain.request().newBuilder()
            .header("apikey", config.publishableKey)
            .header("Authorization", "Bearer ${config.publishableKey}")
            .header("Accept-Profile", SUPABASE_SCHEMA)
            .header("Content-Profile", SUPABASE_SCHEMA)

        return chain.proceed(requestBuilder.build())
    }

    private companion object {
        const val SUPABASE_SCHEMA = "puntuall"
    }
}
