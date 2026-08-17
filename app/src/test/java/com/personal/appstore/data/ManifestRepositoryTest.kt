package com.personal.appstore.data

import com.personal.appstore.data.local.CachedManifest
import com.personal.appstore.data.local.ManifestCache
import com.personal.appstore.data.remote.ManifestApi
import kotlinx.coroutines.test.runTest
import mockwebserver3.MockResponse
import mockwebserver3.MockWebServer
import okhttp3.OkHttpClient
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class ManifestRepositoryTest {

    private lateinit var server: MockWebServer
    private val cache = FakeCache()
    private var now = 1_700_000_000_000L

    private val validManifest = """
        {
          "schemaVersion": 1,
          "updatedAt": "2026-08-16T12:00:00Z",
          "apps": [
            {
              "id": "com.example.app", "name": "My App",
              "versionCode": 2, "versionName": "1.1.0",
              "apkUrl": "https://github.com/o/r/releases/download/v1.1.0/a.apk",
              "sha256": "${"a".repeat(64)}", "apkSizeBytes": 1024,
              "changelog": "новое", "releasedAt": "2026-08-16T12:00:00Z", "versions": []
            }
          ]
        }
    """.trimIndent()

    private class FakeCache : ManifestCache {
        var entry: CachedManifest? = null
        var saveCount = 0

        override suspend fun load(): CachedManifest? = entry

        override suspend fun save(raw: String, fetchedAtMillis: Long) {
            entry = CachedManifest(raw, fetchedAtMillis)
            saveCount++
        }
    }

    @Before
    fun setUp() {
        server = MockWebServer()
        server.start()
    }

    @After
    fun tearDown() {
        server.close()
    }

    private fun repository(retries: Int = 1) = ManifestRepository(
        api = ManifestApi(OkHttpClient(), server.url("/apps.json").toString(), retries = retries),
        cache = cache,
        now = { now },
    )

    @Test
    fun `успешная загрузка отдаёт свежую витрину и кладёт её в кэш`() = runTest {
        server.enqueue(MockResponse.Builder().code(200).body(validManifest).build())
        val repository = repository()

        val manifest = repository.refresh()

        assertNotNull(manifest)
        val state = repository.state.value as ManifestState.Ready
        assertEquals(1, state.manifest.apps.size)
        assertFalse(state.isStale)
        assertEquals(now, state.fetchedAtMillis)
        assertEquals(1, cache.saveCount)
    }

    @Test
    fun `при недоступной сети показывается кэш с пометкой устаревшего`() = runTest {
        cache.entry = CachedManifest(validManifest, fetchedAtMillis = now - 60_000)
        server.enqueue(MockResponse.Builder().code(500).build())
        val repository = repository()

        val manifest = repository.refresh()

        assertEquals(null, manifest) // из сети ничего не получили
        val state = repository.state.value as ManifestState.Ready
        assertEquals(1, state.manifest.apps.size)
        assertTrue(state.isStale)
        assertNotNull(state.warning)
    }

    @Test
    fun `без сети и без кэша — состояние ошибки`() = runTest {
        server.enqueue(MockResponse.Builder().code(500).build())
        val repository = repository()

        repository.refresh()

        val state = repository.state.value
        assertTrue("ожидалось Failed, получено $state", state is ManifestState.Failed)
    }

    @Test
    fun `битый манифест не затирает рабочий кэш`() = runTest {
        cache.entry = CachedManifest(validManifest, fetchedAtMillis = now - 60_000)
        server.enqueue(MockResponse.Builder().code(200).body("{ не json").build())
        val repository = repository()

        repository.refresh()

        val state = repository.state.value as ManifestState.Ready
        assertTrue(state.isStale)
        assertEquals(validManifest, cache.entry?.raw)
    }

    @Test
    fun `временная ошибка сети переживается ретраем`() = runTest {
        server.enqueue(MockResponse.Builder().code(503).build())
        server.enqueue(MockResponse.Builder().code(200).body(validManifest).build())
        val repository = repository(retries = 2)

        val manifest = repository.refresh()

        assertNotNull(manifest)
        assertEquals(2, server.requestCount)
    }

    @Test
    fun `старый кэш помечается устаревшим`() = runTest {
        cache.entry = CachedManifest(validManifest, fetchedAtMillis = now - 48L * 60 * 60 * 1000)
        server.enqueue(MockResponse.Builder().code(500).build())
        val repository = repository()

        repository.refresh()

        assertTrue((repository.state.value as ManifestState.Ready).isStale)
    }
}
