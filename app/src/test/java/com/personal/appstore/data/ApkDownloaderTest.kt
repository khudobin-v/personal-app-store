package com.personal.appstore.data

import com.personal.appstore.data.remote.ApkDownloader
import com.personal.appstore.data.remote.ChecksumMismatchException
import com.personal.appstore.domain.model.AppRelease
import kotlinx.coroutines.test.runTest
import mockwebserver3.MockResponse
import mockwebserver3.MockWebServer
import okhttp3.OkHttpClient
import okio.Buffer
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.IOException

/**
 * Ключевая гарантия клиента: APK не попадает в установщик без совпадения sha256.
 */
class ApkDownloaderTest {

    @get:Rule
    val temporaryFolder = TemporaryFolder()

    private lateinit var server: MockWebServer
    private lateinit var downloader: ApkDownloader

    private val payload = "APK-CONTENT-1234567890".toByteArray()
    private val payloadSha = Sha256.toHex(Sha256.newDigest().digest(payload))

    @Before
    fun setUp() {
        server = MockWebServer()
        server.start()
        downloader = ApkDownloader(OkHttpClient(), temporaryFolder.newFolder("apk"))
    }

    @After
    fun tearDown() {
        server.close()
    }

    private fun release(sha256: String = payloadSha, size: Long = payload.size.toLong()) = AppRelease(
        versionCode = 3,
        versionName = "1.2.0",
        apkUrl = server.url("/app.apk").toString(),
        sha256 = sha256,
        apkSizeBytes = size,
        changelog = "",
        releasedAt = null,
    )

    private fun enqueueApk() {
        server.enqueue(
            MockResponse.Builder()
                .code(200)
                .body(Buffer().write(payload))
                .build(),
        )
    }

    @Test
    fun `скачивает файл и отдаёт его при совпадении sha256`() = runTest {
        enqueueApk()

        val file = downloader.download("com.example.app", release(), onProgress = { _, _ -> })

        assertTrue(file.isFile)
        assertEquals(payload.size.toLong(), file.length())
        assertEquals(payloadSha, Sha256.of(file))
    }

    @Test
    fun `при несовпадении sha256 файл удаляется и бросается ошибка`() = runTest {
        enqueueApk()
        val wrongSha = "b".repeat(64)

        val error = runCatching {
            downloader.download("com.example.app", release(sha256 = wrongSha), onProgress = { _, _ -> })
        }.exceptionOrNull()

        assertTrue("ожидалась ChecksumMismatchException, получено $error", error is ChecksumMismatchException)

        val files = temporaryFolder.root.walkTopDown().filter { it.isFile }.toList()
        assertTrue("в кэше не должно остаться файлов: $files", files.isEmpty())
    }

    @Test
    fun `при несовпадении размера файл не сохраняется`() = runTest {
        enqueueApk()

        val error = runCatching {
            downloader.download(
                appId = "com.example.app",
                release = release(size = payload.size + 100L),
                onProgress = { _, _ -> },
            )
        }.exceptionOrNull()

        assertTrue("ожидалась IOException, получено $error", error is IOException)
        assertFalse(downloader.fileFor("com.example.app", release()).exists())
    }

    @Test
    fun `HTTP-ошибка не оставляет мусора`() = runTest {
        server.enqueue(MockResponse.Builder().code(404).build())

        val error = runCatching {
            downloader.download("com.example.app", release(), onProgress = { _, _ -> })
        }.exceptionOrNull()

        assertTrue("ожидалась IOException, получено $error", error is IOException)
        assertFalse(downloader.fileFor("com.example.app", release()).exists())
    }

    @Test
    fun `повторное скачивание переиспользует проверенный файл`() = runTest {
        enqueueApk()
        val first = downloader.download("com.example.app", release(), onProgress = { _, _ -> })

        // Второй запрос в очередь не кладём: если бы загрузчик пошёл в сеть, тест упал бы.
        val second = downloader.download("com.example.app", release(), onProgress = { _, _ -> })

        assertEquals(first.absolutePath, second.absolutePath)
        assertEquals(1, server.requestCount)
    }

    @Test
    fun `прогресс доходит до полного размера`() = runTest {
        enqueueApk()
        var lastDownloaded = 0L
        var lastTotal = 0L

        downloader.download(
            appId = "com.example.app",
            release = release(),
            onProgress = { downloaded, total ->
                lastDownloaded = downloaded
                lastTotal = total
            },
        )

        assertEquals(payload.size.toLong(), lastDownloaded)
        assertEquals(payload.size.toLong(), lastTotal)
    }

    @Test
    fun `перед установкой сообщается о проверке`() = runTest {
        enqueueApk()
        var verifyingReported = false

        downloader.download(
            appId = "com.example.app",
            release = release(),
            onProgress = { _, _ -> },
            onVerifying = { verifyingReported = true },
        )

        assertTrue(verifyingReported)
    }

    @Test
    fun `cleanup удаляет всё лишнее`() = runTest {
        enqueueApk()
        val keep = downloader.download("com.example.app", release(), onProgress = { _, _ -> })
        val junk = java.io.File(keep.parentFile, "old.apk").apply { writeText("junk") }

        downloader.cleanup(setOf(keep))

        assertTrue(keep.exists())
        assertFalse(junk.exists())
    }
}
