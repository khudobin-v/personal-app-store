package com.personal.appstore.data.remote

import com.personal.appstore.StoreConfig
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import okhttp3.CacheControl
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.IOException

/**
 * Витрина ответила, но не тем: 404 обычно означает опечатку в MANIFEST_URL
 * или ещё не сделанный первый релиз.
 */
class ManifestHttpException(val code: Int) : IOException("витрина ответила HTTP $code")

/**
 * Загрузка apps.json по raw-ссылке.
 *
 * raw.githubusercontent.com кэширует агрессивно, поэтому запрашиваем
 * без кэша: витрина должна показывать свежую версию сразу после релиза.
 */
class ManifestApi(
    private val client: OkHttpClient,
    private val retries: Int = StoreConfig.NETWORK_RETRIES,
) {

    /** [url] приходит из настроек: он может измениться без пересборки. */
    suspend fun fetch(url: String): String = withContext(Dispatchers.IO) {
        var lastError: IOException? = null

        repeat(retries) { attempt ->
            try {
                return@withContext requestOnce(url)
            } catch (e: CancellationException) {
                throw e
            } catch (e: IOException) {
                lastError = e
                if (attempt < retries - 1) {
                    // 1с, 2с, 4с — короткие сетевые сбои переживаем молча.
                    delay(1000L shl attempt)
                }
            }
        }

        throw lastError ?: IOException("не удалось загрузить манифест")
    }

    private fun requestOnce(url: String): String {
        val request = Request.Builder()
            .url(url)
            .cacheControl(CacheControl.FORCE_NETWORK)
            .header("Accept", "application/json")
            .build()

        client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) {
                throw ManifestHttpException(response.code)
            }
            return response.body?.string() ?: throw IOException("пустой ответ витрины")
        }
    }
}
