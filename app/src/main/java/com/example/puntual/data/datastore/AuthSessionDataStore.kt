package com.example.puntual.data.datastore

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.example.puntual.domain.model.AuthSession
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.authDataStore by preferencesDataStore(name = "puntual_auth")

@Singleton
class AuthSessionDataStore @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    private val dataStore = context.authDataStore

    val sessionFlow: Flow<AuthSession?> = dataStore.data.map { prefs ->
        val userId = prefs[Keys.USER_ID].orEmpty()
        val accessToken = prefs[Keys.ACCESS_TOKEN].orEmpty()
        val refreshToken = prefs[Keys.REFRESH_TOKEN].orEmpty()
        if (userId.isBlank() || accessToken.isBlank() || refreshToken.isBlank()) {
            null
        } else {
            AuthSession(
                userId = userId,
                email = prefs[Keys.EMAIL].orEmpty(),
                accessToken = accessToken,
                refreshToken = refreshToken,
            )
        }
    }

    suspend fun saveSession(session: AuthSession) {
        dataStore.edit { prefs ->
            prefs[Keys.USER_ID] = session.userId
            prefs[Keys.EMAIL] = session.email
            prefs[Keys.ACCESS_TOKEN] = session.accessToken
            prefs[Keys.REFRESH_TOKEN] = session.refreshToken
        }
    }

    suspend fun clearSession() {
        dataStore.edit { it.clear() }
    }

    private object Keys {
        val USER_ID = stringPreferencesKey("user_id")
        val EMAIL = stringPreferencesKey("email")
        val ACCESS_TOKEN = stringPreferencesKey("access_token")
        val REFRESH_TOKEN = stringPreferencesKey("refresh_token")
    }
}
