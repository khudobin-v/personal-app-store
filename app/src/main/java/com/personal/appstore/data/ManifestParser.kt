package com.personal.appstore.data

import com.personal.appstore.data.model.AppDto
import com.personal.appstore.data.model.ManifestDto
import com.personal.appstore.data.model.VersionDto
import com.personal.appstore.domain.model.AppRelease
import com.personal.appstore.domain.model.Manifest
import com.personal.appstore.domain.model.StoreApp
import kotlinx.serialization.json.Json
import java.time.Instant
import java.time.OffsetDateTime
import java.time.format.DateTimeParseException

/** Манифест не удалось разобрать: битый JSON или неподдерживаемая схема. */
class ManifestParseException(message: String, cause: Throwable? = null) : Exception(message, cause)

/**
 * Разбор apps.json в доменные модели.
 *
 * Записи, которые не проходят валидацию (нет корректного sha256, не-https
 * ссылка, нулевой размер), отбрасываются: клиент никогда не должен предлагать
 * к установке APK, который нельзя проверить.
 */
object ManifestParser {

    private val json = Json {
        ignoreUnknownKeys = true
        coerceInputValues = false
    }

    private val SHA256_REGEX = Regex("^[0-9a-fA-F]{64}$")

    fun parse(raw: String): Manifest {
        val dto = try {
            json.decodeFromString(ManifestDto.serializer(), raw)
        } catch (e: Exception) {
            throw ManifestParseException("не удалось разобрать apps.json: ${e.message}", e)
        }

        if (dto.schemaVersion != ManifestDto.SUPPORTED_SCHEMA_VERSION) {
            throw ManifestParseException(
                "apps.json версии ${dto.schemaVersion}, клиент понимает только " +
                    "${ManifestDto.SUPPORTED_SCHEMA_VERSION} — обновите магазин",
            )
        }

        val apps = dto.apps.mapNotNull(::toStoreApp)
        return Manifest(updatedAt = parseInstant(dto.updatedAt), apps = apps.sortedBy { it.name.lowercase() })
    }

    private fun toStoreApp(dto: AppDto): StoreApp? {
        if (dto.id.isBlank() || dto.name.isBlank()) return null

        val latest = toRelease(
            VersionDto(
                versionCode = dto.versionCode,
                versionName = dto.versionName,
                apkUrl = dto.apkUrl,
                sha256 = dto.sha256,
                apkSizeBytes = dto.apkSizeBytes,
                changelog = dto.changelog,
                releasedAt = dto.releasedAt,
            ),
        ) ?: return null

        // История: только валидные записи, от новой к старой, без дублей.
        val history = (listOf(latest) + dto.versions.mapNotNull(::toRelease))
            .distinctBy { it.versionCode }
            .sortedByDescending { it.versionCode }

        return StoreApp(
            id = dto.id,
            name = dto.name,
            iconUrl = dto.iconUrl?.takeIf { it.startsWith("https://") },
            latest = latest,
            history = history,
        )
    }

    private fun toRelease(dto: VersionDto): AppRelease? {
        if (dto.versionCode <= 0) return null
        if (dto.versionName.isBlank()) return null
        if (!dto.apkUrl.startsWith("https://")) return null
        if (!SHA256_REGEX.matches(dto.sha256)) return null
        if (dto.apkSizeBytes <= 0) return null

        return AppRelease(
            versionCode = dto.versionCode,
            versionName = dto.versionName,
            apkUrl = dto.apkUrl,
            sha256 = dto.sha256.lowercase(),
            apkSizeBytes = dto.apkSizeBytes,
            changelog = dto.changelog.trim(),
            releasedAt = parseInstant(dto.releasedAt),
        )
    }

    /** ISO-8601 как с 'Z', так и со смещением; при неудаче — null, дата не критична. */
    private fun parseInstant(value: String?): Instant? {
        val text = value?.trim().orEmpty()
        if (text.isEmpty()) return null
        return try {
            Instant.parse(text)
        } catch (_: DateTimeParseException) {
            try {
                OffsetDateTime.parse(text).toInstant()
            } catch (_: DateTimeParseException) {
                null
            }
        }
    }
}
