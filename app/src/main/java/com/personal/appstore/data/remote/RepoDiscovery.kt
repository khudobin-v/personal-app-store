package com.personal.appstore.data.remote

import com.personal.appstore.data.ManifestParser
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.IOException

/** Найденная витрина. */
data class DiscoveredStorefront(
    val repository: String,
    val manifestUrl: String,
    val appCount: Int,
)

@Serializable
private data class RepoDto(
    val name: String = "",
    val full_name: String = "",
    val default_branch: String = "main",
    val fork: Boolean = false,
)

/**
 * Поиск репозитория витрины по GitHub-логину — чтобы не заставлять вводить
 * длинную raw-ссылку на телефоне.
 *
 * Работает без авторизации: публичный API отдаёт список публичных
 * репозиториев. Витрина обязана быть публичной (клиент читает её без токена),
 * так что этого достаточно.
 */
class RepoDiscovery(private val client: OkHttpClient) {

    private val json = Json { ignoreUnknownKeys = true }

    suspend fun discover(login: String): List<DiscoveredStorefront> = withContext(Dispatchers.IO) {
        val user = login.trim().trim('@', '/')
        require(user.isNotEmpty()) { "пустой логин" }

        val repos = fetchRepos(user)
        // Сначала репозитории с ожидаемым именем, затем — недавно обновлённые.
        val candidates = repos
            .filter { !it.fork }
            .sortedByDescending { it.name == EXPECTED_NAME }
            .take(PROBE_LIMIT)

        candidates.mapNotNull { repo ->
            val url = rawUrl(repo.full_name, repo.default_branch)
            val apps = countApps(url) ?: return@mapNotNull null
            DiscoveredStorefront(repository = repo.full_name, manifestUrl = url, appCount = apps)
        }
    }

    private fun fetchRepos(user: String): List<RepoDto> {
        val request = Request.Builder()
            .url("https://api.github.com/users/$user/repos?per_page=100&sort=pushed")
            .header("Accept", "application/vnd.github+json")
            .build()

        client.newCall(request).execute().use { response ->
            if (response.code == 404) throw IOException("пользователь $user не найден на GitHub")
            if (response.code == 403) throw IOException("GitHub временно ограничил запросы, попробуйте позже")
            if (!response.isSuccessful) throw IOException("GitHub ответил ${response.code}")
            val body = response.body?.string().orEmpty()
            return json.decodeFromString(kotlinx.serialization.builtins.ListSerializer(RepoDto.serializer()), body)
        }
    }

    /** null, если по адресу нет разбираемого apps.json. */
    private fun countApps(url: String): Int? = try {
        val request = Request.Builder().url(url).build()
        client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) {
                null
            } else {
                val raw = response.body?.string().orEmpty()
                ManifestParser.parse(raw).apps.size
            }
        }
    } catch (_: Exception) {
        null
    }

    private fun rawUrl(fullName: String, branch: String) =
        "https://raw.githubusercontent.com/$fullName/$branch/apps.json"

    private companion object {
        const val EXPECTED_NAME = "app-store-manifest"

        /** Сколько репозиториев проверять на наличие apps.json. */
        const val PROBE_LIMIT = 12
    }
}
