package com.personal.appstore.data

import com.personal.appstore.StoreConfig
import com.personal.appstore.data.local.CachedManifest
import com.personal.appstore.data.local.ManifestCache
import com.personal.appstore.data.remote.ManifestApi
import com.personal.appstore.data.remote.ManifestHttpException
import com.personal.appstore.domain.model.Manifest
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.io.IOException

/** Состояние витрины для UI. */
sealed interface ManifestState {

    data object Loading : ManifestState

    /**
     * Данные есть. [isStale] — показаны из кэша, свежие получить не удалось;
     * [warning] в этом случае объясняет, почему.
     */
    data class Ready(
        val manifest: Manifest,
        val fetchedAtMillis: Long,
        val isStale: Boolean,
        val warning: String? = null,
    ) : ManifestState

    /** Данных нет вообще: ни сети, ни кэша. */
    data class Failed(val message: String) : ManifestState
}

/**
 * Единственный источник витрины: сеть с ретраями + кэш последнего успешного
 * ответа. При сетевой ошибке отдаёт кэш и помечает его устаревшим — магазин
 * остаётся работоспособным офлайн.
 */
class ManifestRepository(
    private val api: ManifestApi,
    private val cache: ManifestCache,
    /** Адрес витрины: настройка на устройстве важнее значения из сборки. */
    private val manifestUrl: suspend () -> String,
    private val now: () -> Long = System::currentTimeMillis,
) {

    private val _state = MutableStateFlow<ManifestState>(ManifestState.Loading)
    val state: StateFlow<ManifestState> = _state.asStateFlow()

    private val refreshLock = Mutex()

    /**
     * Обновляет [state]. Возвращает разобранный манифест при успехе сети,
     * null — если пришлось откатиться на кэш или данных нет совсем.
     */
    suspend fun refresh(): Manifest? = refreshLock.withLock {
        // Пока идёт первая загрузка, показываем кэш, чтобы список не был пустым.
        val cached = runCatching { cache.load() }.getOrNull()
        if (_state.value is ManifestState.Loading && cached != null) {
            parseOrNull(cached.raw)?.let { manifest ->
                _state.value = ManifestState.Ready(
                    manifest = manifest,
                    fetchedAtMillis = cached.fetchedAtMillis,
                    isStale = isStale(cached.fetchedAtMillis),
                )
            }
        }

        try {
            val raw = api.fetch(manifestUrl())
            val manifest = ManifestParser.parse(raw)
            val fetchedAt = now()
            runCatching { cache.save(raw, fetchedAt) }
            _state.value = ManifestState.Ready(
                manifest = manifest,
                fetchedAtMillis = fetchedAt,
                isStale = false,
            )
            manifest
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            fallbackToCache(cached, e)
            null
        }
    }

    private fun fallbackToCache(cached: CachedManifest?, error: Exception) {
        val message = describe(error)
        val manifest = cached?.raw?.let(::parseOrNull)
        _state.value = if (manifest != null) {
            ManifestState.Ready(
                manifest = manifest,
                fetchedAtMillis = cached.fetchedAtMillis,
                isStale = true,
                warning = message,
            )
        } else {
            ManifestState.Failed(message)
        }
    }

    private fun parseOrNull(raw: String): Manifest? = runCatching { ManifestParser.parse(raw) }.getOrNull()

    private fun isStale(fetchedAtMillis: Long): Boolean =
        now() - fetchedAtMillis > StoreConfig.STALE_AFTER_MILLIS

    private fun describe(error: Exception): String = when (error) {
        is ManifestParseException -> error.message ?: "витрина повреждена"
        // 404 — почти всегда опечатка в MANIFEST_URL или ещё не было релизов.
        is ManifestHttpException -> if (error.code == 404) {
            "витрина не найдена (HTTP 404): проверьте MANIFEST_URL и что apps.json уже создан"
        } else {
            error.message ?: "витрина ответила ошибкой"
        }

        is IOException -> "нет связи с витриной"
        else -> error.message ?: "не удалось обновить витрину"
    }
}
