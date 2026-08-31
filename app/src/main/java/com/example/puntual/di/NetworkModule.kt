package com.example.puntual.di

import com.example.puntual.BuildConfig
import com.example.puntual.data.remote.ZenQuotesApi
import com.example.puntual.data.remote.supabase.PuntuallSupabaseApi
import com.example.puntual.data.remote.supabase.SupabaseAuthHeadersInterceptor
import com.example.puntual.data.remote.supabase.SupabaseConfig
import com.example.puntual.data.remote.supabase.SupabaseHeadersInterceptor
import com.example.puntual.data.remote.supabase.auth.SupabaseAuthApi
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton
import javax.inject.Named
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {

    private const val ZEN_QUOTES_BASE_URL = "https://zenquotes.io/"

    @Provides
    @Singleton
    fun provideGson(): Gson = GsonBuilder().create()

    @Provides
    @Singleton
    @Named("zenQuotes")
    fun provideZenQuotesRetrofit(gson: Gson): Retrofit =
        Retrofit.Builder()
            .baseUrl(ZEN_QUOTES_BASE_URL)
            .addConverterFactory(GsonConverterFactory.create(gson))
            .build()

    @Provides
    @Singleton
    fun provideZenQuotesApi(@Named("zenQuotes") retrofit: Retrofit): ZenQuotesApi =
        retrofit.create(ZenQuotesApi::class.java)

    @Provides
    @Singleton
    fun provideSupabaseConfig(): SupabaseConfig = SupabaseConfig(
        url = BuildConfig.SUPABASE_URL,
        publishableKey = BuildConfig.SUPABASE_PUBLISHABLE_KEY,
    )

    @Provides
    @Singleton
    @Named("supabase")
    fun provideSupabaseOkHttpClient(
        headersInterceptor: SupabaseHeadersInterceptor,
    ): OkHttpClient =
        OkHttpClient.Builder()
            .addInterceptor(headersInterceptor)
            .build()

    @Provides
    @Singleton
    @Named("supabaseAuth")
    fun provideSupabaseAuthOkHttpClient(
        headersInterceptor: SupabaseAuthHeadersInterceptor,
    ): OkHttpClient =
        OkHttpClient.Builder()
            .addInterceptor(headersInterceptor)
            .build()

    @Provides
    @Singleton
    @Named("supabase")
    fun provideSupabaseRetrofit(
        gson: Gson,
        @Named("supabase") client: OkHttpClient,
        config: SupabaseConfig,
    ): Retrofit =
        Retrofit.Builder()
            .baseUrl(config.restBaseUrl)
            .client(client)
            .addConverterFactory(GsonConverterFactory.create(gson))
            .build()

    @Provides
    @Singleton
    @Named("supabaseAuth")
    fun provideSupabaseAuthRetrofit(
        gson: Gson,
        @Named("supabaseAuth") client: OkHttpClient,
        config: SupabaseConfig,
    ): Retrofit =
        Retrofit.Builder()
            .baseUrl(config.authBaseUrl)
            .client(client)
            .addConverterFactory(GsonConverterFactory.create(gson))
            .build()

    @Provides
    @Singleton
    fun providePuntuallSupabaseApi(
        @Named("supabase") retrofit: Retrofit,
    ): PuntuallSupabaseApi =
        retrofit.create(PuntuallSupabaseApi::class.java)

    @Provides
    @Singleton
    fun provideSupabaseAuthApi(
        @Named("supabaseAuth") retrofit: Retrofit,
    ): SupabaseAuthApi =
        retrofit.create(SupabaseAuthApi::class.java)
}
