package com.example.puntual.data.remote.supabase

data class SupabaseConfig(
    val url: String,
    val publishableKey: String,
) {
    val isConfigured: Boolean
        get() = url.isNotBlank() && publishableKey.isNotBlank()

    val restBaseUrl: String
        get() = url.trimEnd('/') + "/rest/v1/"

    val authBaseUrl: String
        get() = url.trimEnd('/') + "/auth/v1/"
}
