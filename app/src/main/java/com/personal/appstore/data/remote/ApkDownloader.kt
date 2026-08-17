package com.personal.appstore.data.remote

import com.personal.appstore.data.Sha256
import com.personal.appstore.domain.model.AppRelease
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.io.IOException

/** Скачанный APK не совпал с sha256 из витрины — файл удалён, ставить нельзя. */
class ChecksumMismatchException(val expected: String, val actual: String) :
    IOException("контрольная сумма APK не совпала (ожидалась $expected, получена $actual)")

/**
 * Скачивание APK во внутренний кэш с прогрессом и обязательной проверкой sha256.
 *
 * Файл появляется под финальным именем только после успешной проверки:
 * недокачанный или битый APK физически не может попасть в установщик.
 */
class ApkDownloader(
    private val client: OkHttpClient,
    private val apkDir: File,
) {

    suspend fun download(
        appId: String,
        release: AppRelease,
        onProgress: (downloadedBytes: Long, totalBytes: Long) -> Unit,
        onVerifying: () -> Unit = {},
    ): File = withContext(Dispatchers.IO) {
        apkDir.mkdirs()
        val target = fileFor(appId, release)

        // Уже скачан и проверен раньше (например, установка была прервана).
        if (target.isFile) onVerifying()
        if (target.isFile && Sha256.matches(Sha256.of(target), release.sha256)) {
            onProgress(target.length(), target.length())
            return@withContext target
        }
        target.delete()

        val part = File(apkDir, "${target.name}.part")
        part.delete()

        val digest = Sha256.newDigest()
        var downloaded = 0L

        val request = Request.Builder().url(release.apkUrl).build()
        client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) {
                throw IOException("не удалось скачать APK: HTTP ${response.code}")
            }
            val body = response.body ?: throw IOException("пустой ответ при скачивании APK")
            val total = body.contentLength().takeIf { it > 0 } ?: release.apkSizeBytes

            body.byteStream().use { input ->
                part.outputStream().use { output ->
                    val buffer = ByteArray(64 * 1024)
                    var lastReported = 0L
                    while (true) {
                        currentCoroutineContext().ensureActive()
                        val read = input.read(buffer)
                        if (read <= 0) break
                        output.write(buffer, 0, read)
                        digest.update(buffer, 0, read)
                        downloaded += read
                        // Не дёргаем UI на каждые 64 КБ.
                        if (downloaded - lastReported >= 128 * 1024) {
                            lastReported = downloaded
                            onProgress(downloaded, total)
                        }
                    }
                    output.flush()
                }
            }
            onProgress(downloaded, total)
        }

        onVerifying()
        val actual = Sha256.toHex(digest.digest())
        if (!Sha256.matches(actual, release.sha256)) {
            part.delete()
            throw ChecksumMismatchException(expected = release.sha256, actual = actual)
        }
        if (release.apkSizeBytes > 0 && part.length() != release.apkSizeBytes) {
            part.delete()
            throw IOException("размер APK не совпал с заявленным в витрине")
        }

        if (!part.renameTo(target)) {
            part.delete()
            throw IOException("не удалось сохранить APK в кэш")
        }
        target
    }

    fun fileFor(appId: String, release: AppRelease): File =
        File(apkDir, "$appId-${release.versionName.replace(Regex("[^A-Za-z0-9._-]"), "_")}.apk")

    /** Удаляет всё, кроме переданных файлов: кэш APK не должен расти бесконечно. */
    suspend fun cleanup(keep: Set<File>) = withContext(Dispatchers.IO) {
        val keepPaths = keep.map { it.absolutePath }.toSet()
        apkDir.listFiles()?.forEach { file ->
            if (file.absolutePath !in keepPaths) file.delete()
        }
        Unit
    }
}
