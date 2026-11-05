package com.example.puntual.data.remote.supabase

import com.example.puntual.data.datastore.AuthSessionDataStore
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import okhttp3.Interceptor
import okhttp3.Response
import javax.inject.Inject

class SupabaseHeadersInterceptor @Inject constructor(
    private val config: SupabaseConfig,
    private val sessionDataStore: AuthSessionDataStore,
) : Interceptor {

    override fun intercept(chain: Interceptor.Chain): Response {
        val accessToken = runBlocking {
            sessionDataStore.sessionFlow.first()?.accessToken
        }
        val requestBuilder = chain.request().newBuilder()
            .header("apikey", config.publishableKey)
            .header("Authorization", "Bearer ${accessToken ?: config.publishableKey}")
            .header("Accept-Profile", SUPABASE_SCHEMA)
            .header("Content-Profile", SUPABASE_SCHEMA)

        return chain.proceed(requestBuilder.build())
    }

    private companion object {
        const val SUPABASE_SCHEMA = "puntuall"
    }
}
