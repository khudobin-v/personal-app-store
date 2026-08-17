package com.personal.appstore.data

import com.personal.appstore.data.remote.ManifestApi
import kotlinx.coroutines.test.runTest
import mockwebserver3.MockResponse
import mockwebserver3.MockWebServer
import okhttp3.OkHttpClient
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * Свежесть витрины: raw.githubusercontent.com кэшируется CDN на 5 минут,
 * поэтому запрос идёт через Contents API, а raw остаётся запасным.
 */
class ManifestApiTest {

    private lateinit var server: MockWebServer
    private val api = ManifestApi(OkHttpClient(), retries = 1)

    @Before
    fun setUp() {
        server = MockWebServer()
        server.start()
    }

    @After
    fun tearDown() {
        server.close()
    }

    @Test
    fun `raw-ссылка превращается в адрес Contents API`() {
        val result = api.contentsApiUrl(
            "https://raw.githubusercontent.com/khudobin-v/app-store-manifest/main/apps.json",
        )

        assertEquals(
            "https://api.github.com/repos/khudobin-v/app-store-manifest/contents/apps.json?ref=main",
            result,
        )
    }

    @Test
    fun `ветка и вложенный путь сохраняются`() {
        val result = api.contentsApiUrl(
            "https://raw.githubusercontent.com/owner/repo/release/store/apps.json",
        )

        assertTrue(result!!.endsWith("/contents/store/apps.json?ref=release"))
    }

    @Test
    fun `адрес не на GitHub остаётся как есть`() {
        assertNull(api.contentsApiUrl("https://example.org/apps.json"))
        assertNull(api.contentsApiUrl("https://raw.githubusercontent.com/owner"))
    }

    @Test
    fun `не-GitHub витрина скачивается напрямую`() = runTest {
        server.enqueue(MockResponse.Builder().code(200).body("""{"schemaVersion":1,"apps":[]}""").build())

        val raw = api.fetch(server.url("/apps.json").toString())

        assertTrue(raw.contains("schemaVersion"))
        assertEquals(1, server.requestCount)
    }
}
