package com.personal.appstore.data.local

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.first

private val Context.manifestDataStore: DataStore<Preferences> by preferencesDataStore(name = "manifest_cache")

/** Последний успешно загруженный apps.json. */
data class CachedManifest(val raw: String, val fetchedAtMillis: Long)

/** Источник данных в офлайне. Интерфейс — чтобы репозиторий тестировался без Android. */
interface ManifestCache {
    suspend fun load(): CachedManifest?
    suspend fun save(raw: String, fetchedAtMillis: Long)
}

class DataStoreManifestCache(private val context: Context) : ManifestCache {

    override suspend fun load(): CachedManifest? {
        val prefs = context.manifestDataStore.data.first()
        val raw = prefs[KEY_RAW] ?: return null
        return CachedManifest(raw = raw, fetchedAtMillis = prefs[KEY_FETCHED_AT] ?: 0L)
    }

    override suspend fun save(raw: String, fetchedAtMillis: Long) {
        context.manifestDataStore.edit { prefs ->
            prefs[KEY_RAW] = raw
            prefs[KEY_FETCHED_AT] = fetchedAtMillis
        }
    }

    private companion object {
        val KEY_RAW = stringPreferencesKey("raw_manifest")
        val KEY_FETCHED_AT = longPreferencesKey("fetched_at")
    }
}
