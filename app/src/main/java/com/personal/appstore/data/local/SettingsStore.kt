package com.personal.appstore.data.local

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.personal.appstore.StoreConfig
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

private val Context.settingsDataStore: DataStore<Preferences> by preferencesDataStore(name = "store_settings")

/**
 * Настройки, задаваемые на устройстве.
 *
 * Адрес витрины можно не зашивать в сборку: пользователь вводит GitHub-логин,
 * магазин сам находит репозиторий манифеста и запоминает ссылку здесь.
 * Значение из BuildConfig остаётся значением по умолчанию.
 */
class SettingsStore(private val context: Context) {

    data class Settings(
        val manifestUrl: String,
        val githubLogin: String?,
        /** true, если адрес выбран пользователем, а не взят из сборки. */
        val isCustom: Boolean,
    )

    val settings: Flow<Settings> = context.settingsDataStore.data.map { prefs ->
        val custom = prefs[KEY_MANIFEST_URL]?.takeIf { it.isNotBlank() }
        Settings(
            manifestUrl = custom ?: StoreConfig.defaultManifestUrl,
            githubLogin = prefs[KEY_GITHUB_LOGIN],
            isCustom = custom != null,
        )
    }

    suspend fun current(): Settings = settings.first()

    suspend fun setManifestUrl(url: String, githubLogin: String?) {
        context.settingsDataStore.edit { prefs ->
            prefs[KEY_MANIFEST_URL] = url.trim()
            if (githubLogin.isNullOrBlank()) prefs.remove(KEY_GITHUB_LOGIN) else prefs[KEY_GITHUB_LOGIN] = githubLogin
        }
    }

    suspend fun clearManifestUrl() {
        context.settingsDataStore.edit { prefs ->
            prefs.remove(KEY_MANIFEST_URL)
            prefs.remove(KEY_GITHUB_LOGIN)
        }
    }

    private companion object {
        val KEY_MANIFEST_URL = stringPreferencesKey("manifest_url")
        val KEY_GITHUB_LOGIN = stringPreferencesKey("github_login")
    }
}
