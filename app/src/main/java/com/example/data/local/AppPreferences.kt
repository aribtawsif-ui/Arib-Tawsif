package com.example.data.local

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.poreyDataStore by preferencesDataStore(name = "porey_session_prefs")

data class StoredSessionPrefs(
    val isDarkMode: Boolean = false,
    val hasChosenSession: Boolean = false,
    val isGuest: Boolean = false,
    val loggedInUserId: String? = null
)

class AppPreferences(private val context: Context) {
    private object Keys {
        val IS_DARK_MODE = booleanPreferencesKey("is_dark_mode")
        val HAS_CHOSEN_SESSION = booleanPreferencesKey("has_chosen_session")
        val IS_GUEST = booleanPreferencesKey("is_guest")
        val LOGGED_IN_USER_ID = stringPreferencesKey("logged_in_user_id")
    }

    val prefsFlow: Flow<StoredSessionPrefs> = context.poreyDataStore.data.map { prefs ->
        StoredSessionPrefs(
            isDarkMode = prefs[Keys.IS_DARK_MODE] ?: false,
            hasChosenSession = prefs[Keys.HAS_CHOSEN_SESSION] ?: false,
            isGuest = prefs[Keys.IS_GUEST] ?: false,
            loggedInUserId = prefs[Keys.LOGGED_IN_USER_ID]
        )
    }

    suspend fun setDarkMode(enabled: Boolean) {
        context.poreyDataStore.edit { prefs ->
            prefs[Keys.IS_DARK_MODE] = enabled
        }
    }

    suspend fun setLoggedInUser(userId: String) {
        context.poreyDataStore.edit { prefs ->
            prefs[Keys.HAS_CHOSEN_SESSION] = true
            prefs[Keys.IS_GUEST] = false
            prefs[Keys.LOGGED_IN_USER_ID] = userId
        }
    }

    suspend fun setGuestSession() {
        context.poreyDataStore.edit { prefs ->
            prefs[Keys.HAS_CHOSEN_SESSION] = true
            prefs[Keys.IS_GUEST] = true
            prefs.remove(Keys.LOGGED_IN_USER_ID)
        }
    }

    suspend fun clearSessionToAuthGate() {
        context.poreyDataStore.edit { prefs ->
            prefs[Keys.HAS_CHOSEN_SESSION] = false
            prefs[Keys.IS_GUEST] = false
            prefs.remove(Keys.LOGGED_IN_USER_ID)
        }
    }
}
