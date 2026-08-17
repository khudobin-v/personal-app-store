package com.personal.appstore.data.remote

import android.util.Log
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
 * Загрузка apps.json.
 *
 * raw.githubusercontent.com отдаёт файл через CDN с max-age=300, и этот кэш не
 * пробивается ни заголовком no-cache, ни параметром в адресе: после релиза
 * телефон до пяти минут видел бы старую витрину. Contents API отдаёт тот же
 * файл с max-age=60, поэтому сначала идём туда, а raw остаётся запасным
 * вариантом — в том числе если API упрётся в лимит запросов.
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
        val apiUrl = contentsApiUrl(url)
        if (apiUrl != null) {
            try {
                return get(apiUrl, accept = "application/vnd.github.raw")
            } catch (e: IOException) {
                // Лимит запросов или сбой API — не повод не показать витрину вовсе.
                Log.i(TAG, "Contents API недоступен (${e.message}), беру raw-ссылку")
            }
        }
        return get(url, accept = "application/json")
    }

    private fun get(url: String, accept: String): String {
        val request = Request.Builder()
            .url(url)
            .cacheControl(CacheControl.FORCE_NETWORK)
            .header("Accept", accept)
            .build()

        client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) {
                throw ManifestHttpException(response.code)
            }
            return response.body?.string() ?: throw IOException("пустой ответ витрины")
        }
    }

    /**
     * `https://raw.githubusercontent.com/OWNER/REPO/BRANCH/apps.json`
     * → `https://api.github.com/repos/OWNER/REPO/contents/apps.json?ref=BRANCH`
     *
     * null, если витрина лежит не на raw.githubusercontent.com.
     */
    internal fun contentsApiUrl(rawUrl: String): String? {
        val match = RAW_URL_REGEX.matchEntire(rawUrl.trim()) ?: return null
        val (owner, repo, branch, path) = match.destructured
        return "https://api.github.com/repos/$owner/$repo/contents/$path?ref=$branch"
    }

    private companion object {
        const val TAG = "ManifestApi"

        val RAW_URL_REGEX =
            Regex("https://raw\\.githubusercontent\\.com/([^/]+)/([^/]+)/([^/]+)/(.+)")
    }
}
