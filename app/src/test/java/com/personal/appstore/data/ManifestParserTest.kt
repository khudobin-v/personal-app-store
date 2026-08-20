package com.personal.appstore.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Instant

class ManifestParserTest {

    private val sha = "a".repeat(64)

    private fun manifest(apps: String, schemaVersion: Int = 1) = """
        {
          "schemaVersion": $schemaVersion,
          "updatedAt": "2026-08-16T12:00:00Z",
          "apps": [$apps]
        }
    """.trimIndent()

    private fun app(
        id: String = "com.example.app",
        versionCode: Long = 12,
        sha256: String = sha,
        apkUrl: String = "https://github.com/o/r/releases/download/v1.4.0/com.example.app-1.4.0.apk",
        apkSizeBytes: Long = 5242880,
        versions: String = "",
    ) = """
        {
          "id": "$id",
          "name": "My App",
          "iconUrl": "https://example.org/icon.png",
          "versionCode": $versionCode,
          "versionName": "1.4.0",
          "apkUrl": "$apkUrl",
          "sha256": "$sha256",
          "apkSizeBytes": $apkSizeBytes,
          "changelog": "Тёмная тема",
          "releasedAt": "2026-08-16T12:00:00Z"
          ${if (versions.isBlank()) "" else ", \"versions\": [$versions]"}
        }
    """.trimIndent()

    @Test
    fun `разбирает приложение с полями верхнего уровня`() {
        val result = ManifestParser.parse(manifest(app()))

        assertEquals(1, result.apps.size)
        val parsed = result.apps.first()
        assertEquals("com.example.app", parsed.id)
        assertEquals("My App", parsed.name)
        assertEquals(12L, parsed.latest.versionCode)
        assertEquals("1.4.0", parsed.latest.versionName)
        assertEquals(5242880L, parsed.latest.apkSizeBytes)
        assertEquals("Тёмная тема", parsed.latest.changelog)
        assertEquals(Instant.parse("2026-08-16T12:00:00Z"), parsed.latest.releasedAt)
        assertEquals(Instant.parse("2026-08-16T12:00:00Z"), result.updatedAt)
    }

    @Test
    fun `история включает текущую версию и отсортирована от новой к старой`() {
        val older = """
            {"versionCode": 11, "versionName": "1.3.0",
             "apkUrl": "https://github.com/o/r/releases/download/v1.3.0/a.apk",
             "sha256": "${"b".repeat(64)}", "apkSizeBytes": 100, "changelog": "старое",
             "releasedAt": "2026-07-01T00:00:00Z"}
        """.trimIndent()

        val parsed = ManifestParser.parse(manifest(app(versions = older))).apps.first()

        assertEquals(listOf(12L, 11L), parsed.history.map { it.versionCode })
        assertEquals(parsed.latest.versionCode, parsed.history.first().versionCode)
    }

    @Test
    fun `дубликат версии в истории схлопывается`() {
        val duplicate = """
            {"versionCode": 12, "versionName": "1.4.0",
             "apkUrl": "https://github.com/o/r/releases/download/v1.4.0/a.apk",
             "sha256": "$sha", "apkSizeBytes": 5242880, "changelog": "Тёмная тема"}
        """.trimIndent()

        val parsed = ManifestParser.parse(manifest(app(versions = duplicate))).apps.first()

        assertEquals(1, parsed.history.size)
    }

    @Test
    fun `неизвестные поля игнорируются`() {
        val withExtra = manifest(app()).replace("\"name\": \"My App\"", "\"name\": \"My App\", \"future\": 42")
        assertEquals(1, ManifestParser.parse(withExtra).apps.size)
    }

    @Test
    fun `запись с некорректной sha256 отбрасывается`() {
        val result = ManifestParser.parse(manifest(app(sha256 = "deadbeef")))
        assertTrue(result.apps.isEmpty())
    }

    @Test
    fun `запись с http-ссылкой отбрасывается`() {
        val result = ManifestParser.parse(manifest(app(apkUrl = "http://example.org/a.apk")))
        assertTrue(result.apps.isEmpty())
    }

    @Test
    fun `запись с нулевым размером отбрасывается`() {
        assertTrue(ManifestParser.parse(manifest(app(apkSizeBytes = 0))).apps.isEmpty())
    }

    @Test
    fun `битая запись не мешает остальным`() {
        val json = manifest(app(id = "com.example.broken", sha256 = "nope") + "," + app(id = "com.example.ok"))
        val result = ManifestParser.parse(json)
        assertEquals(listOf("com.example.ok"), result.apps.map { it.id })
    }

    @Test
    fun `неподдерживаемая версия схемы — ошибка`() {
        val error = assertThrows(ManifestParseException::class.java) {
            ManifestParser.parse(manifest(app(), schemaVersion = 2))
        }
        assertTrue(error.message!!.contains("обновите магазин"))
    }

    @Test
    fun `битый JSON — ошибка`() {
        assertThrows(ManifestParseException::class.java) { ManifestParser.parse("{ не json") }
    }

    @Test
    fun `пустая витрина разбирается`() {
        val result = ManifestParser.parse("""{"schemaVersion":1,"updatedAt":null,"apps":[]}""")
        assertTrue(result.apps.isEmpty())
        assertNull(result.updatedAt)
    }

    @Test
    fun `дата со смещением приводится к Instant`() {
        val json = manifest(app()).replace("2026-08-16T12:00:00Z", "2026-08-16T15:00:00+03:00")
        val parsed = ManifestParser.parse(json).apps.first()
        assertEquals(Instant.parse("2026-08-16T12:00:00Z"), parsed.latest.releasedAt)
    }

    @Test
    fun `некорректная дата не роняет разбор`() {
        val json = manifest(app()).replace("\"2026-08-16T12:00:00Z\"", "\"вчера\"")
        val parsed = ManifestParser.parse(json).apps.first()
        assertNull(parsed.latest.releasedAt)
        assertNotNull(parsed.latest.apkUrl)
    }

    @Test
    fun `sha256 приводится к нижнему регистру`() {
        val json = manifest(app(sha256 = "A".repeat(64)))
        assertEquals("a".repeat(64), ManifestParser.parse(json).apps.first().latest.sha256)
    }

    @Test
    fun `приложения отсортированы по имени`() {
        val json = manifest(
            app(id = "com.example.b").replace("My App", "Яблоко") + "," +
                app(id = "com.example.a").replace("My App", "Арбуз"),
        )
        assertEquals(listOf("Арбуз", "Яблоко"), ManifestParser.parse(json).apps.map { it.name })
    }

    @Test
    fun `автор публикации попадает в модель`() {
        val json = manifest(app()).replace("\"changelog\":", "\"publishedBy\": \"ivan\", \"changelog\":")
        assertEquals("ivan", ManifestParser.parse(json).apps.first().author)
    }

    @Test
    fun `публикации из CI автором не считаются`() {
        val json = manifest(app()).replace("\"changelog\":", "\"publishedBy\": \"ci\", \"changelog\":")
        assertNull(ManifestParser.parse(json).apps.first().author)
    }

    @Test
    fun `не-https иконка игнорируется`() {
        val json = manifest(app()).replace("https://example.org/icon.png", "http://example.org/icon.png")
        assertNull(ManifestParser.parse(json).apps.first().iconUrl)
    }
}
