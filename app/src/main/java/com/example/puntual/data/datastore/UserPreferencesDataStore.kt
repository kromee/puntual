package com.example.puntual.data.datastore

import android.content.Context
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.example.puntual.domain.model.UserPreferences
import com.example.puntual.domain.model.sanitizeDisplayName
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore by preferencesDataStore(name = "puntual_prefs")

@Singleton
class UserPreferencesDataStore @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    private val dataStore = context.dataStore

    val preferencesFlow: Flow<UserPreferences> = dataStore.data.map { prefs ->
        val hasExpectedTime = when (prefs[Keys.EXPECTED_TIME_SET]) {
            true -> true
            false -> false
            null -> prefs[Keys.EXPECTED_HOUR] != null
        }
        UserPreferences(
            displayName = sanitizeDisplayName(prefs[Keys.DISPLAY_NAME].orEmpty()),
            hasExpectedTime = hasExpectedTime,
            expectedHour = prefs[Keys.EXPECTED_HOUR] ?: DEFAULT_HOUR,
            expectedMinute = prefs[Keys.EXPECTED_MINUTE] ?: DEFAULT_MINUTE,
            biometricUnlockEnabled = prefs[Keys.BIOMETRIC_UNLOCK_ENABLED] ?: true,
        )
    }

    suspend fun setDisplayName(name: String) {
        dataStore.edit { prefs ->
            val trimmed = sanitizeDisplayName(name)
            if (trimmed.isEmpty()) {
                prefs.remove(Keys.DISPLAY_NAME)
            } else {
                prefs[Keys.DISPLAY_NAME] = trimmed
            }
        }
    }

    suspend fun setExpectedTime(hour: Int, minute: Int) {
        dataStore.edit { prefs ->
            prefs[Keys.EXPECTED_HOUR] = hour
            prefs[Keys.EXPECTED_MINUTE] = minute
            prefs[Keys.EXPECTED_TIME_SET] = true
        }
    }

    suspend fun clearExpectedTime() {
        dataStore.edit { prefs ->
            prefs.remove(Keys.EXPECTED_HOUR)
            prefs.remove(Keys.EXPECTED_MINUTE)
            prefs[Keys.EXPECTED_TIME_SET] = false
        }
    }

    suspend fun setBiometricUnlockEnabled(enabled: Boolean) {
        dataStore.edit { prefs ->
            prefs[Keys.BIOMETRIC_UNLOCK_ENABLED] = enabled
        }
    }

    private object Keys {
        val DISPLAY_NAME = stringPreferencesKey("display_name")
        val EXPECTED_HOUR = intPreferencesKey("expected_hour")
        val EXPECTED_MINUTE = intPreferencesKey("expected_minute")
        val EXPECTED_TIME_SET = booleanPreferencesKey("expected_time_set")
        val BIOMETRIC_UNLOCK_ENABLED = booleanPreferencesKey("biometric_unlock_enabled")
    }

    companion object {
        private const val DEFAULT_HOUR = 8
        private const val DEFAULT_MINUTE = 0
    }
}
